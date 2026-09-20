package app.finni.kids.domain

private const val DECAY_WHY = "С каждой неделей питомец немного устаёт и хочет есть — так бывает."

data class PeriodScore(
    val net: Int,
    val food: Boolean,
    val care: Boolean,
    val mustWithin: Boolean,
    val wantWithin: Boolean,
    val savedSome: Boolean,
    val savedPlan: Boolean,
    val regular: Boolean,
    val score: Score,
)

/** Считает итоги периода: А (0–3) + Б (0–3) + В (0–2) = до 8 очков роста. Формулы — в docs/ECONOMY.md. */
fun scorePeriod(p: Profile): PeriodScore {
    val per = p.period
    val net = maxOf(0, per.saved - per.withdrawn)
    val food = per.needs.food > 0
    val care = per.needs.care > 0
    val prev = p.history.lastOrNull()

    // A. Обязательное: еда и уход.
    val needs = if (food && care) 3 else if (food || care) 1 else 0
    // B. План и факт. Очки даём только если неделя не пустая: подтвердить план и ничего не делать — не решение.
    val active = per.spent.must + per.spent.want + net > 0
    val mustWithin = per.spent.must > 0 && per.spent.must <= per.plan.must
    val wantWithin = active && per.spent.want <= per.plan.want
    val savedPlan = per.plan.save > 0 && net >= per.plan.save
    val plan = if (active) (if (mustWithin) 1 else 0) + (if (wantWithin) 1 else 0) + (if (savedPlan) 1 else 0) else 0
    // C. Регулярные накопления.
    val savedSome = net >= Econ.minSaving
    val regular = savedSome && prev != null && prev.fact.save >= Econ.minSaving
    val saving = (if (savedSome) 1 else 0) + (if (regular) 1 else 0)

    return PeriodScore(net, food, care, mustWithin, wantWithin, savedSome, savedPlan, regular, Score(needs, plan, saving, needs + plan + saving))
}

private fun buildLines(p: Profile, s: PeriodScore): List<ReviewLine> {
    val per = p.period
    val lines = mutableListOf<ReviewLine>()
    if (s.food && s.care) lines += ReviewLine("needs", ReviewStatus.Ok, "Еда и уход куплены. Питомец сыт и ухожен.", points = 3)
    else if (s.food || s.care)
        lines += ReviewLine(
            "needs", ReviewStatus.Part,
            "Куплено только: ${if (s.food) "еда" else "уход"}. ${if (s.food) "Уход" else "Еда"} тоже нужен.",
            points = 1,
            tip = "Начни новую неделю с ${if (s.food) "ухода (купание, расчёска)" else "еды (обед, ужин)"}.",
        )
    else lines += ReviewLine("needs", ReviewStatus.Todo, "На этой неделе не было ни еды, ни ухода.", points = 0, tip = "Начни новую неделю с нужного: оно есть в магазине.")

    lines += when {
        s.mustWithin -> ReviewLine("must", ReviewStatus.Ok, "Нужное: потрачено ${per.spent.must} из ${per.plan.must}. В плане!", points = 1)
        per.spent.must == 0 -> ReviewLine("must", ReviewStatus.Todo, "На нужное ничего не потрачено.", points = 0, tip = "Питомцу нужны еда и уход — они есть в магазине.")
        else -> ReviewLine("must", ReviewStatus.Part, "На нужное ушло ${per.spent.must}, а по плану было ${per.plan.must}.", points = 0, tip = "В следующий раз заложи на нужное побольше.")
    }
    lines += if (s.wantWithin)
        ReviewLine(
            "want", ReviewStatus.Ok,
            if (per.spent.want == 0) "Хочется: ты не потратил лишнего. Это тоже хороший выбор!" else "Хочется: потрачено ${per.spent.want} из ${per.plan.want}. В плане!",
            points = 1,
        )
    else ReviewLine("want", ReviewStatus.Part, "На «Хочется» ушло ${per.spent.want} при плане ${per.plan.want}.", points = 0, tip = "В следующий раз выбери подешевле или отложи покупку на потом — это не ошибка.")

    lines += when {
        per.plan.save == 0 -> ReviewLine("save-plan", ReviewStatus.Todo, "В плане не было копилки.", points = 0, tip = "Попробуй заложить в план хоть немного на мечту.")
        s.savedPlan -> ReviewLine("save-plan", ReviewStatus.Ok, "Копилка: по плану ${per.plan.save}, отложено ${s.net}. Как задумано!", points = 1)
        else -> ReviewLine("save-plan", ReviewStatus.Part, "По плану нужно было отложить ${per.plan.save}, а отложено ${s.net}.", points = 0, tip = "Откладывай сразу после плана, пока монеты не потрачены.")
    }
    lines += if (s.savedSome) ReviewLine("save", ReviewStatus.Ok, "В копилку отложено ${coinsText(s.net)}.", points = 1)
    else ReviewLine("save", ReviewStatus.Todo, "В копилку ничего не отложено.", points = 0, tip = "Даже ${Econ.minSaving} монет в неделю — уже привычка.")
    lines += if (s.regular) ReviewLine("regular", ReviewStatus.Ok, "Ты копишь уже несколько недель подряд — это привычка!", points = 1)
    else ReviewLine("regular", ReviewStatus.Part, "Регулярность: чем чаще ты копишь неделя за неделей, тем быстрее растёт питомец.", points = 0)
    return lines
}

fun endPeriod(p0: Profile, ctx: Ctx): Outcome<PeriodResult> {
    if (!p0.period.planConfirmed) return fail(ErrorCode.PLAN_REQUIRED, "Неделю можно завершить после плана: сначала составь и подтверди его.")

    val p = p0.deepCopy()
    val per = p.period
    val s = scorePeriod(p)
    val stageBefore = p.pet.stage
    val growthBefore = p.pet.growth
    val statsBefore = p.pet.stats.copy()
    val walletBefore = p.wallet

    // 1. Показатели питомца немного снижаются, но не ниже нижней границы.
    for (stat in STATS) {
        val cur = p.pet.stats[stat]
        if (cur > Econ.statFloor) p.pet.stats[stat] = maxOf(Econ.statFloor, cur - Econ.periodDecay(stat))
    }
    // 2. Держался плана — питомец гордится (настроение растёт).
    val kept = s.mustWithin && s.wantWithin
    if (kept) applyStat(p.pet, PetStat.Mood, Econ.planKeptMood)
    val statsAfter = p.pet.stats.copy()

    // 3. Рост питомца копится и никогда не уменьшается.
    p.pet.growth += s.score.total
    p.pet.stage = stageForGrowth(p.pet.growth)
    fun stageName(st: Int) = ctx.content.pet.stages.find { it.id == st }?.name ?: ""
    val stageUp = p.pet.stage != stageBefore

    val lines = buildLines(p0, s)
    val head = when {
        stageUp -> "Питомец вырос! Теперь он «${stageName(p.pet.stage)}»."
        s.score.total >= 6 -> "Отличная неделя!"
        s.score.total >= 3 -> "Хорошая неделя. Есть что улучшить."
        else -> "Ничего страшного — на новой неделе всё получится."
    }
    val summary = "$head Ты получил ${pointsText(s.score.total)} роста."

    val result = PeriodResult(
        index = per.index,
        plan = per.plan.copy(),
        fact = Fact(per.spent.must, per.spent.want, s.net, per.spent.event, per.earned),
        needs = NeedsDone(s.food, s.care),
        checks = Checks(s.mustWithin, s.wantWithin, s.savedSome, s.savedPlan, s.regular),
        score = s.score,
        growthBefore = growthBefore,
        growthAfter = p.pet.growth,
        stageBefore = stageBefore,
        stageAfter = p.pet.stage,
        balanceEnd = p.wallet,
        statsBefore = statsBefore,
        statsAfter = statsAfter,
        lines = lines,
        summary = summary,
    )
    p.history.add(result)

    // 4. Новая неделя: карманные монеты, свежий план, возможное событие.
    p.period = freshPeriod(per.index + 1)
    p.wallet += Econ.weeklyAllowance
    addTx(p, TxKind.Allowance, "Карманные монеты на неделю", Econ.weeklyAllowance)
    scheduleEvent(p, ctx.content)

    val expr = petExpression(p.pet.stats)
    p.pet.reason = PetReason("Началась новая неделя! ${expr.hint}")

    val changes = mutableListOf(
        Change(ChangeKind.Growth, "Очки роста", delta = s.score.total, before = growthBefore, after = p.pet.growth, why = "Очки за заботу, план и копилку. Они не пропадают."),
    )
    for (stat in STATS) {
        if (statsBefore[stat] != statsAfter[stat])
            changes += Change(
                ChangeKind.Stat, STAT_LABEL.getValue(stat),
                delta = statsAfter[stat] - statsBefore[stat], before = statsBefore[stat], after = statsAfter[stat], stat = stat,
                why = if (kept && stat == PetStat.Mood) "Ты держался плана — питомец гордится (+8), но неделя всё равно немного утомила." else DECAY_WHY,
            )
    }
    changes += walletChange("Монеты", walletBefore, p.wallet, "Карманные монеты на новую неделю. Остаток ${coinsText(walletBefore)} тоже остался у тебя.")

    return done(
        p,
        Report(
            title = "Неделя ${per.index} завершена",
            tone = Tone.Good,
            changes = changes,
            explain = summary,
            next = NextLink("Смотреть итоги", "/review"),
        ),
        result,
    )
}

/** Ответить на непредвиденное событие: заплатить из кошелька. */
fun resolveEvent(p0: Profile, ctx: Ctx): Res {
    val ev = ctx.content.events.find { it.id == p0.pendingEventId } ?: return fail(ErrorCode.NO_EVENT, "Сейчас непредвиденных трат нет.")
    if (p0.wallet < ev.cost) {
        val options = mutableListOf(ShortageOption(ShortageId.Earn, "Заработать монеты", "Выполни задание — и вернись.", route = "/tasks"))
        if (savingsTotal(p0) > 0)
            options += ShortageOption(ShortageId.Savings, "Взять из копилки", "Для таких случаев копилка и нужна. Но сначала подумай.", route = "/goals")
        return fail(ErrorCode.NOT_ENOUGH, "Не хватает ${coinsText(ev.cost - p0.wallet)}.", missing = ev.cost - p0.wallet, options = options)
    }
    val p = p0.deepCopy()
    val before = p.wallet
    p.wallet -= ev.cost
    p.period.spent.event += ev.cost
    p.pendingEventId = null
    p.resolvedEvents.add(ev.id)
    addTx(p, TxKind.Event, ev.title, -ev.cost, dir = "event")

    val changes = mutableListOf(walletChange("Монеты", before, p.wallet, "Непредвиденная трата: ${ev.title.lowercase()}."))
    for (stat in STATS) {
        val eff = ev.effects[stat] ?: continue
        if (eff == 0) continue
        val r = applyStat(p.pet, stat, eff)
        changes += Change(ChangeKind.Stat, STAT_LABEL.getValue(stat), delta = r.gain, before = r.before, after = r.after, stat = stat, why = "Врач помог — питомец в порядке.")
    }
    p.pet.reason = PetReason("Спасибо, что позаботился! Мне уже лучше.", stat = PetStat.Care)
    return done(p, Report("Питомцу помогли", Tone.Care, changes, "${ev.tip} Эта трата не считается ошибкой плана."))
}
