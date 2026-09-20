package app.finni.kids.domain

import app.finni.kids.content.Content

private val DIR_WHY = mapOf(
    Direction.Must to "На еду и уход за питомцем.",
    Direction.Want to "На игрушки и украшения.",
    Direction.Save to "Столько ты хочешь отложить на мечту.",
)

data class NeedsCost(val food: Int, val care: Int, val total: Int)

/** Самая дешёвая еда + самый дешёвый уход: минимум на нужное за неделю. */
fun needsCost(content: Content): NeedsCost {
    fun cheapest(need: String) = content.items.filter { it.kind == Direction.Must && it.need == need }.minOf { it.price }
    val food = cheapest("food")
    val care = cheapest("care")
    return NeedsCost(food, care, food + care)
}

fun planTotal(plan: Plan): Int = DIRECTIONS.sumOf { plan[it] }

fun planRemainder(available: Int, plan: Plan): Int = available - planTotal(plan)

enum class PlanProblem { Invalid, Empty, Over }

data class PlanCheck(val ok: Boolean, val remainder: Int, val problem: PlanProblem? = null)

/** Проверка плана до подтверждения: сумма не больше доступного бюджета. */
fun validatePlan(available: Int, plan: Plan): PlanCheck {
    val remainder = planRemainder(available, plan)
    if (!DIRECTIONS.all { plan[it] >= 0 }) return PlanCheck(false, remainder, PlanProblem.Invalid)
    if (planTotal(plan) == 0) return PlanCheck(false, remainder, PlanProblem.Empty)
    if (remainder < 0) return PlanCheck(false, remainder, PlanProblem.Over)
    return PlanCheck(true, remainder)
}

/** Пример разделения для кнопки «Подсказка». Ребёнок может изменить любое число. */
fun suggestPlan(p: Profile, content: Content): Plan {
    val avail = p.wallet
    val event = p.pendingEventId?.let { id -> content.events.find { it.id == id } }
    val reserve = event?.cost ?: 0
    var must = maxOf(roundUp5(needsCost(content).total), roundDown5(Math.floor(avail * 0.35).toInt()))
    var save = roundDown5(Math.floor(avail * 0.3).toInt())
    var want = roundDown5(Math.floor(avail * 0.25).toInt())
    val room = avail - reserve
    while (must + save + want > room && want > 0) want -= Econ.planStep
    while (must + save + want > room && save > 0) save -= Econ.planStep
    must = minOf(must, maxOf(0, room - save - want))
    return Plan(must, want, save)
}

fun confirmPlan(p0: Profile, plan: Plan): Res {
    if (p0.period.planConfirmed) return fail(ErrorCode.PLAN_ALREADY_CONFIRMED, "План этой недели уже подтверждён.")
    val check = validatePlan(p0.wallet, plan)
    if (check.problem == PlanProblem.Over)
        return fail(ErrorCode.PLAN_OVER_BUDGET, "В плане на ${coinsText(-check.remainder)} больше, чем у тебя есть.", overBy = -check.remainder)
    if (!check.ok) return fail(ErrorCode.PLAN_INVALID, "Раздели хотя бы несколько монет по конвертам.")

    val p = p0.deepCopy()
    p.period.plan = Plan(plan.must, plan.want, plan.save)
    p.period.planConfirmed = true
    p.period.startBalance = p.wallet

    val changes = DIRECTIONS.map { d ->
        Change(ChangeKind.Plan, DIRECTION_LABEL.getValue(d), delta = plan[d], after = plan[d], why = DIR_WHY.getValue(d))
    }.toMutableList()
    if (check.remainder > 0)
        changes += Change(ChangeKind.Plan, "Запас", delta = check.remainder, after = check.remainder, why = "Эти монеты остаются в кошельке — на неожиданный случай.")
    return done(
        p,
        Report(
            title = "План на неделю готов!",
            tone = Tone.Good,
            changes = changes,
            explain = "Теперь можно покупать и откладывать. В конце недели я сравню план с тем, что получилось.",
            next = NextLink("В магазин", "/shop"),
        ),
    )
}

/** Новые монеты (заработанные уже после плана) можно добавить в любой конверт. */
fun topUpPlan(p0: Profile, dir: Direction, amount: Int): Res {
    if (!p0.period.planConfirmed) return fail(ErrorCode.PLAN_REQUIRED, "Сначала составь план недели.")
    if (amount <= 0) return fail(ErrorCode.INVALID_AMOUNT, "Выбери, сколько монет добавить.")
    val free = freeCoins(p0)
    if (amount > free) return fail(ErrorCode.NOT_ENOUGH, "Свободных монет только $free.", missing = amount - maxOf(0, free))

    val p = p0.deepCopy()
    p.period.plan[dir] = p.period.plan[dir] + amount
    return done(
        p,
        Report(
            title = "Добавлено в план",
            tone = Tone.Info,
            changes = listOf(
                Change(ChangeKind.Plan, DIRECTION_LABEL.getValue(dir), delta = amount, before = p0.period.plan[dir], after = p.period.plan[dir], why = "Новые монеты ты сам разложил по конвертам."),
            ),
            explain = "Каждая монета получила своё дело. Так проще не потратить лишнего.",
        ),
    )
}
