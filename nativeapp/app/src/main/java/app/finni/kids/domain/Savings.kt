package app.finni.kids.domain

data class EtaInfo(
    /** Средний взнос за неделю (null — пока не было взносов). */
    val avg: Int?,
    /** Сколько недель осталось при таком взносе (null — посчитать нельзя). */
    val weeks: Int?,
    val remaining: Int,
    val text: String,
)

/**
 * Средний регулярный взнос: среднее по прошлым неделям, где были взносы.
 * Если завершённых недель ещё нет — берём текущую. Снятия на среднее не влияют.
 */
fun averageDeposit(goal: Goal, currentPeriod: Int): Int? {
    val entries = goal.deposits.entries.map { it.key to it.value }.filter { it.second > 0 }
    val past = entries.filter { it.first < currentPeriod }.map { it.second }
    val pool = if (past.isNotEmpty()) past else entries.map { it.second }
    if (pool.isEmpty()) return null
    return maxOf(1, Math.round(pool.sum().toDouble() / pool.size).toInt())
}

fun etaFor(goal: Goal, currentPeriod: Int, savedOverride: Int? = null): EtaInfo {
    val saved = savedOverride ?: goal.saved
    val remaining = maxOf(0, goal.cost - saved)
    val avg = averageDeposit(goal, currentPeriod)
    if (remaining == 0) return EtaInfo(avg, 0, remaining, "Цель накоплена!")
    if (avg == null || avg == 0) return EtaInfo(null, null, remaining, "Срок появится, когда ты отложишь первые монеты.")
    val weeks = Math.ceil(remaining.toDouble() / avg).toInt()
    return EtaInfo(avg, weeks, remaining, "Примерно ${weeksText(weeks)}: ты откладываешь в среднем ${coinsText(avg)} в неделю.")
}

/** «Если откладывать по N в неделю» — подсказка без истории. */
fun weeksAt(remaining: Int, perWeek: Int): Int = if (perWeek > 0) Math.ceil(remaining.toDouble() / perWeek).toInt() else 0

fun deposit(p0: Profile, goalId: String, amount: Int): Res {
    if (!p0.period.planConfirmed) return fail(ErrorCode.PLAN_REQUIRED, "Сначала составь план недели — в нём ты решаешь, сколько отложить.")
    val goal = goalById(p0, goalId) ?: return fail(ErrorCode.GOAL_NOT_FOUND, "Такой цели нет.")
    if (goal.done) return fail(ErrorCode.GOAL_DONE, "Эта мечта уже исполнена.")
    if (amount <= 0) return fail(ErrorCode.INVALID_AMOUNT, "Выбери, сколько монет отложить.")
    if (amount > p0.wallet) return fail(ErrorCode.NOT_ENOUGH, "В кошельке только ${coinsText(p0.wallet)}.", missing = amount - p0.wallet)
    val remaining = goal.cost - goal.saved
    if (amount > remaining) return fail(ErrorCode.GOAL_FULL, "До цели осталось всего ${coinsText(remaining)}.", max = remaining)

    val p = p0.deepCopy()
    val g = goalById(p, goalId)!!
    val walletBefore = p.wallet
    val savedBefore = g.saved
    val periodSavedBefore = p.period.saved
    p.wallet -= amount
    g.saved += amount
    g.deposits[p.period.index] = (g.deposits[p.period.index] ?: 0) + amount
    p.period.saved += amount
    addTx(p, TxKind.Save, "В копилку: ${g.title}", -amount, savingsDelta = amount)

    val changes = mutableListOf(
        walletChange("Монеты", walletBefore, p.wallet, "Эти монеты переехали из кошелька в копилку."),
        Change(ChangeKind.Savings, "Копилка", delta = amount, before = savedBefore, after = g.saved, why = "Копится на мечту «${g.title}»."),
    )
    if (periodSavedBefore < Econ.minSaving && p.period.saved >= Econ.minSaving) {
        val r = applyStat(p.pet, PetStat.Mood, Econ.firstSavingMood)
        changes += Change(ChangeKind.Stat, STAT_LABEL.getValue(PetStat.Mood), delta = r.gain, before = r.before, after = r.after, stat = PetStat.Mood, why = "Питомец гордится: ты начал копить на этой неделе.")
        p.pet.reason = PetReason("Я горжусь тобой: ты копишь на мечту!", PetStat.Mood, r.gain)
    } else {
        p.pet.reason = PetReason("Копилка растёт — мечта всё ближе!")
    }

    val eta = etaFor(g, p.period.index)
    val reached = g.saved >= g.cost
    return done(
        p,
        Report(
            title = if (reached) "Мечта накоплена!" else "Отложено!",
            tone = Tone.Good,
            changes = changes,
            explain = if (reached) "Ты накопил всю сумму. Теперь мечту можно исполнить!" else "Осталось накопить ${coinsText(eta.remaining)}. ${eta.text}",
            next = if (reached) NextLink("Исполнить мечту", "/goals") else null,
        ),
    )
}

data class WithdrawPreview(
    val goalId: String,
    val amount: Int,
    val savedBefore: Int,
    val savedAfter: Int,
    val walletBefore: Int,
    val walletAfter: Int,
    val remainingBefore: Int,
    val remainingAfter: Int,
    val etaBefore: EtaInfo,
    val etaAfter: EtaInfo,
)

/** Показывает последствия снятия ДО подтверждения: накопления и срок цели. */
fun previewWithdraw(p: Profile, goalId: String, amount: Int): WithdrawPreview? {
    val g = goalById(p, goalId) ?: return null
    val amt = minOf(maxOf(0, amount), g.saved)
    val savedAfter = g.saved - amt
    return WithdrawPreview(
        goalId, amt, g.saved, savedAfter, p.wallet, p.wallet + amt,
        remainingBefore = g.cost - g.saved, remainingAfter = g.cost - savedAfter,
        etaBefore = etaFor(g, p.period.index), etaAfter = etaFor(g, p.period.index, savedAfter),
    )
}

fun withdraw(p0: Profile, goalId: String, amount: Int, confirmed: Boolean): Res {
    val goal = goalById(p0, goalId) ?: return fail(ErrorCode.GOAL_NOT_FOUND, "Такой цели нет.")
    if (goal.done) return fail(ErrorCode.GOAL_DONE, "Эта мечта уже исполнена.")
    if (amount <= 0) return fail(ErrorCode.INVALID_AMOUNT, "Выбери, сколько монет забрать.")
    if (amount > goal.saved) return fail(ErrorCode.NOT_ENOUGH, "В копилке только ${coinsText(goal.saved)}.", max = goal.saved)
    if (!confirmed) return fail(ErrorCode.CONFIRM_REQUIRED, "Забрать монеты из копилки можно только после подтверждения.")

    val pv = previewWithdraw(p0, goalId, amount)!!
    val p = p0.deepCopy()
    val g = goalById(p, goalId)!!
    p.wallet += amount
    g.saved -= amount
    p.period.withdrawn += amount
    addTx(p, TxKind.Withdraw, "Из копилки: ${g.title}", amount, savingsDelta = -amount)
    p.pet.reason = PetReason("Ты забрал монеты из копилки. Иногда так нужно — главное, чтобы это было осознанно.")

    val slower = pv.etaBefore.weeks != null && pv.etaAfter.weeks != null && pv.etaAfter.weeks > pv.etaBefore.weeks
    return done(
        p,
        Report(
            title = "Монеты забраны из копилки",
            tone = Tone.Info,
            changes = listOf(
                Change(ChangeKind.Savings, "Копилка", delta = -amount, before = pv.savedBefore, after = pv.savedAfter, why = "Цель «${g.title}» теперь дальше на ${coinsText(amount)}."),
                walletChange("Монеты", pv.walletBefore, pv.walletAfter, "Монеты вернулись в кошелёк."),
            ),
            explain = if (slower) "Срок цели вырос: было ${weeksText(pv.etaBefore.weeks!!)}, стало ${weeksText(pv.etaAfter.weeks!!)}. Верни монеты в копилку, когда сможешь."
            else "Копилка стала меньше, поэтому мечта чуть дальше.",
        ),
    )
}

fun completeGoal(p0: Profile, goalId: String): Res {
    val goal = goalById(p0, goalId) ?: return fail(ErrorCode.GOAL_NOT_FOUND, "Такой цели нет.")
    if (goal.done) return fail(ErrorCode.GOAL_DONE, "Эта мечта уже исполнена.")
    if (goal.saved < goal.cost) return fail(ErrorCode.GOAL_NOT_REACHED, "Ещё не хватает ${coinsText(goal.cost - goal.saved)}.", missing = goal.cost - goal.saved)

    val p = p0.deepCopy()
    val g = goalById(p, goalId)!!
    val savedBefore = g.saved
    g.saved -= g.cost
    g.done = true
    p.trophies.add(g.id)
    val growthBefore = p.pet.growth
    p.pet.growth += Econ.goalGrowth
    val stageBefore = p.pet.stage
    p.pet.stage = stageForGrowth(p.pet.growth)
    val mood = applyStat(p.pet, PetStat.Mood, Econ.goalMood)
    addTx(p, TxKind.Goal, "Мечта исполнена: ${g.title}", 0, savingsDelta = -g.cost)
    p.activeGoalId = p.goals.find { !it.done }?.id
    p.pet.reason = PetReason("Ура! Мечта «${g.title}» исполнена — ты накопил её сам!", PetStat.Mood, mood.gain)

    val changes = listOf(
        Change(ChangeKind.Savings, "Копилка", delta = -g.cost, before = savedBefore, after = g.saved, why = "Монеты потрачены на мечту «${g.title}»."),
        Change(ChangeKind.Stat, STAT_LABEL.getValue(PetStat.Mood), delta = mood.gain, before = mood.before, after = mood.after, stat = PetStat.Mood, why = "Исполнить мечту — большая радость!"),
        Change(ChangeKind.Growth, "Очки роста", delta = Econ.goalGrowth, before = growthBefore, after = p.pet.growth, why = "Ты долго и регулярно копил — питомец растёт."),
    )
    return done(
        p,
        Report(
            title = "Мечта исполнена!",
            tone = Tone.Good,
            changes = changes,
            explain = if (stageBefore != p.pet.stage) "Питомец перешёл на новую стадию. Это результат твоих решений!" else "Ты выбрал цель, копил и дошёл до конца. Так работают накопления.",
            next = if (p.activeGoalId != null) NextLink("Выбрать новую цель", "/goals") else null,
        ),
    )
}

fun selectGoal(p0: Profile, goalId: String): Res {
    val goal = goalById(p0, goalId) ?: return fail(ErrorCode.GOAL_NOT_FOUND, "Такой цели нет.")
    if (goal.done) return fail(ErrorCode.GOAL_DONE, "Эта мечта уже исполнена.")
    val p = p0.deepCopy()
    p.activeGoalId = goalId
    return done(p, Report("Цель: ${goal.title}", Tone.Info, emptyList(), "Теперь копим на «${goal.title}». Уже накопленные монеты по другим целям никуда не делись."))
}

data class CustomGoalInput(val title: String, val icon: String, val cost: Int)

fun createCustomGoal(p0: Profile, input: CustomGoalInput, ctx: Ctx): Res {
    ctx.content.customGoal.presets.find { it.icon == input.icon && it.title == input.title } ?: return fail(ErrorCode.INVALID_AMOUNT, "Выбери мечту из списка.")
    if (input.cost < Econ.customMin || input.cost > Econ.customMax || input.cost % Econ.customStep != 0)
        return fail(ErrorCode.INVALID_AMOUNT, "Цена мечты — от ${Econ.customMin} до ${Econ.customMax} монет, шаг ${Econ.customStep}.")
    if (p0.goals.count { it.custom && !it.done } >= Econ.customLimit)
        return fail(ErrorCode.INVALID_AMOUNT, "Своих мечт может быть не больше ${Econ.customLimit}. Исполни одну из них.")

    val p = p0.deepCopy()
    val n = p.goals.count { it.custom } + 1
    val g = newGoal("custom-$n", input.title, input.icon, input.cost, custom = true)
    p.goals.add(g)
    p.activeGoalId = g.id
    return done(p, Report("Новая мечта!", Tone.Good, emptyList(), "«${g.title}» стоит ${coinsText(g.cost)}. Теперь это твоя текущая цель."))
}
