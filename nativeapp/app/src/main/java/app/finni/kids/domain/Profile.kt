package app.finni.kids.domain

import app.finni.kids.content.Content

val DIRECTION_LABEL: Map<Direction, String> = mapOf(
    Direction.Must to "Нужное",
    Direction.Want to "Хочется",
    Direction.Save to "Копилка",
)

val STAT_LABEL: Map<PetStat, String> = mapOf(
    PetStat.Satiety to "Сытость",
    PetStat.Care to "Чистота и здоровье",
    PetStat.Mood to "Настроение",
)

fun freshPeriod(index: Int) = PeriodState(index = index)

fun newGoal(id: String, title: String, icon: String, cost: Int, custom: Boolean = false) =
    Goal(id = id, title = title, icon = icon, cost = cost, custom = custom)

data class NewProfileInput(val playerName: String, val petName: String, val appearance: PetAppearance, val isDemo: Boolean = false)

fun newProfile(input: NewProfileInput, ctx: Ctx): Profile {
    val content = ctx.content
    val owned = if (input.appearance.accessory != "none") mutableListOf(input.appearance.accessory) else mutableListOf()
    val pet = Pet(
        name = input.petName,
        appearance = input.appearance.copy(),
        stats = Econ.startStats(),
        growth = 0,
        stage = 1,
        ownedAccessories = owned,
        reason = PetReason("Привет! Меня зовут ${input.petName}. Давай дружить и учиться вместе!"),
    )
    val profile = Profile(
        id = "p-${java.lang.Long.toString(System.currentTimeMillis(), 36)}",
        playerName = input.playerName,
        createdAt = ctx.today,
        isDemo = input.isDemo,
        pet = pet,
        wallet = Econ.startBalance,
        goals = content.goals.map { newGoal(it.id, it.title, it.icon, it.cost) }.toMutableList(),
        activeGoalId = content.goals.firstOrNull()?.id,
        period = freshPeriod(1),
    )
    addTx(profile, TxKind.Start, "Стартовый подарок", Econ.startBalance)
    scheduleEvent(profile, content)
    return profile
}

/** Записывает операцию в историю. Баланс profile.wallet уже должен быть обновлён. */
fun addTx(p: Profile, kind: TxKind, title: String, amount: Int, savingsDelta: Int? = null, dir: String? = null, itemId: String? = null): Tx {
    val rec = Tx(id = p.nextTxId++, period = p.period.index, kind = kind, title = title, amount = amount, savingsDelta = savingsDelta, balance = p.wallet, dir = dir, itemId = itemId)
    p.ledger.add(rec)
    return rec
}

/** Выставляет непредвиденное событие, если оно назначено на текущий период. */
fun scheduleEvent(p: Profile, content: Content) {
    if (p.pendingEventId != null) return
    val ev = content.events.find { it.period == p.period.index && it.id !in p.resolvedEvents }
    if (ev != null) p.pendingEventId = ev.id
}

// ---------- Селекторы ----------

fun usedInDirection(p: Profile, dir: Direction): Int {
    val per = p.period
    return when (dir) {
        Direction.Must -> per.spent.must
        Direction.Want -> per.spent.want
        Direction.Save -> maxOf(0, per.saved - per.withdrawn)
    }
}

data class EnvelopeView(val dir: Direction, val planned: Int, val used: Int, val left: Int, val over: Int)

fun envelope(p: Profile, dir: Direction): EnvelopeView {
    val planned = p.period.plan[dir]
    val used = usedInDirection(p, dir)
    return EnvelopeView(dir, planned, used, left = maxOf(0, planned - used), over = maxOf(0, used - planned))
}

fun envelopes(p: Profile): List<EnvelopeView> = DIRECTIONS.map { envelope(p, it) }

/** Монеты, не закреплённые за конвертами (запас). Может быть < 0, если план «не сходится». */
fun freeCoins(p: Profile): Int {
    if (!p.period.planConfirmed) return p.wallet
    return p.wallet - envelopes(p).sumOf { it.left }
}

fun savingsTotal(p: Profile): Int = p.goals.sumOf { it.saved }

fun activeGoal(p: Profile): Goal? = p.goals.find { it.id == p.activeGoalId && !it.done }

fun goalById(p: Profile, id: String): Goal? = p.goals.find { it.id == id }

data class StatApply(val before: Int, val after: Int, val gain: Int, val wasted: Int)

fun applyStat(pet: Pet, stat: PetStat, delta: Int): StatApply {
    val before = pet.stats[stat]
    val after = clamp(before + delta, 0, Econ.statMax)
    pet.stats[stat] = after
    return StatApply(before, after, gain = after - before, wasted = maxOf(0, delta - (after - before)))
}

fun stageOf(p: Profile): Int = stageForGrowth(p.pet.growth)

enum class ExpressionCode { Happy, Content, Hungry, Dirty, Bored }

data class Expression(val code: ExpressionCode, val label: String, val hint: String, val need: PetStat? = null)

/** По показателям выбираем выражение мордочки. Всегда есть текст — цвет не единственный признак. */
fun petExpression(stats: Stats): Expression {
    val low = STATS.filter { stats[it] < Econ.statLow }.sortedBy { stats[it] }.firstOrNull()
    return when (low) {
        PetStat.Satiety -> Expression(ExpressionCode.Hungry, "Хочет кушать", "Я проголодался! Обед и ужин — в магазине, в разделе «Нужное».", PetStat.Satiety)
        PetStat.Care -> Expression(ExpressionCode.Dirty, "Хочет помыться", "Хочу быть чистым! Купание и расчёска — в магазине, в «Нужном».", PetStat.Care)
        PetStat.Mood -> Expression(ExpressionCode.Bored, "Скучает", "Мне скучно. Игрушка из раздела «Хочется» меня развеселит.", PetStat.Mood)
        null -> {
            val avg = (stats.satiety + stats.care + stats.mood) / 3.0
            if (avg >= 80) Expression(ExpressionCode.Happy, "Счастлив!", "Мне так хорошо! Спасибо за заботу.")
            else Expression(ExpressionCode.Content, "Всё хорошо", "У меня всё хорошо. Что будем делать?")
        }
    }
}
