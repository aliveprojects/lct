package app.finni.kids.domain

/**
 * Демонстрационный помощник: за один вызов проигрывает разумные решения одной недели —
 * план, задания, покупки нужного и желаемого, взнос в копилку — и завершает период.
 * Использует только публичные действия движка, поэтому подчиняется тем же правилам, что и ребёнок.
 */
fun playPeriodAuto(start: Profile, ctx: Ctx): Profile {
    var p = start
    fun run(o: Outcome<*>): Boolean {
        if (o is Outcome.Ok<*>) p = o.profile
        return o is Outcome.Ok<*>
    }
    val items = ctx.content.items

    run(claimDaily(p, ctx))
    if (p.pendingEventId != null) run(resolveEvent(p, ctx))
    if (!p.period.planConfirmed) run(confirmPlan(p, suggestPlan(p, ctx.content)))

    for (t in availableTasks(p, ctx).filter { taskStars(p, it.id) < 3 }.take(2)) run(submitTask(p, t.id, solveTask(t), ctx))

    for (need in listOf("food", "care")) {
        val count = if (need == "food") p.period.needs.food else p.period.needs.care
        if (count > 0) continue
        val cheapest = items.filter { it.kind == Direction.Must && it.need == need }.sortedBy { it.price }
        for (it in cheapest) if (run(buy(p, it.id, false, ctx))) break
    }

    val wantLeft = envelope(p, Direction.Want).left
    val wants = items
        .filter { it.kind == Direction.Want && it.price <= minOf(wantLeft, p.wallet) && !(it.accessory != null && it.accessory in p.pet.ownedAccessories) }
        .sortedByDescending { it.price }
    wants.firstOrNull()?.let { run(buy(p, it.id, false, ctx)) }

    if (p.activeGoalId == null) {
        p.goals.find { !it.done }?.let { run(selectGoal(p, it.id)) }
    }
    val goal = p.activeGoalId?.let { goalById(p, it) }
    if (goal != null) {
        val amount = minOf(envelope(p, Direction.Save).left, p.wallet, goal.cost - goal.saved)
        if (amount > 0) run(deposit(p, goal.id, amount))
        val g2 = goalById(p, goal.id)!!
        if (g2.saved >= g2.cost) run(completeGoal(p, g2.id))
    }

    run(endPeriod(p, ctx))
    return p
}
