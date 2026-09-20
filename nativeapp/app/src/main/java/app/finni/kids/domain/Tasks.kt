package app.finni.kids.domain

import app.finni.kids.content.AllocateTask
import app.finni.kids.content.CalcTask
import app.finni.kids.content.CartTask
import app.finni.kids.content.SimTask
import app.finni.kids.content.SortTask
import app.finni.kids.content.StoryEnd
import app.finni.kids.content.StoryTask
import app.finni.kids.content.TaskDef

// ---------- Ответы ----------

sealed class TaskAnswer {
    data class Sort(val groups: Map<String, String>) : TaskAnswer()
    data class Cart(val picked: List<String>) : TaskAnswer()
    data class Allocate(val amounts: Plan) : TaskAnswer()
    data class Calc(val values: Map<String, Int?>) : TaskAnswer()
    data class Sim(val weekly: Int) : TaskAnswer()
    data class Story(val path: List<Int>) : TaskAnswer()
}

/** ok: true — верно, false — неверно, null — просто информация. */
data class TaskEvalDetail(val ok: Boolean?, val text: String, val sub: String? = null)

data class TaskEval(
    val stars: Int,
    val headline: String,
    val summary: String,
    val details: List<TaskEvalDetail>,
    val recovery: String? = null,
    val learn: String,
)

private fun headline(stars: Int) = when (stars) {
    3 -> "Отлично!"
    2 -> "Хорошо!"
    else -> "Есть над чем подумать"
}

private const val RETRY = "Попробуй ещё раз — за лучший результат дадут ещё монет."

/** Все верно — 3 звезды; 60% и больше — 2; иначе 1 (с разбором и возможностью повторить). */
private fun starsFromRatio(ok: Int, total: Int): Int = if (ok == total) 3 else if (ok.toDouble() / total >= 0.6) 2 else 1

// ---------- Оценка ----------

fun evaluateTask(def: TaskDef, answer: TaskAnswer): TaskEval = when {
    def is SortTask && answer is TaskAnswer.Sort -> evalSort(def, answer)
    def is CartTask && answer is TaskAnswer.Cart -> evalCart(def, answer)
    def is AllocateTask && answer is TaskAnswer.Allocate -> evalAllocate(def, answer)
    def is CalcTask && answer is TaskAnswer.Calc -> evalCalc(def, answer)
    def is SimTask && answer is TaskAnswer.Sim -> evalSim(def, answer)
    def is StoryTask && answer is TaskAnswer.Story -> evalStory(def, answer)
    else -> throw IllegalArgumentException("Ответ ${answer::class.simpleName} не подходит заданию ${def.id}")
}

private fun evalSort(def: SortTask, a: TaskAnswer.Sort): TaskEval {
    fun label(id: String) = def.groups.find { it.id == id }?.label ?: id
    val details = def.items.map { it ->
        val ok = a.groups[it.id] == it.group
        TaskEvalDetail(ok, it.label, (if (ok) "Верно. " else "Лучше в «${label(it.group)}». ") + it.why)
    }
    val correct = details.count { it.ok == true }
    val stars = starsFromRatio(correct, details.size)
    return TaskEval(stars, headline(stars), "Верно: $correct из ${details.size}.", details, if (stars < 3) RETRY else null, def.learn)
}

private fun evalCart(def: CartTask, a: TaskAnswer.Cart): TaskEval {
    val picked = def.items.filter { it.id in a.picked }
    val total = picked.sumOf { it.price }
    val left = def.budget - total
    val missing = def.items.filter { it.need && it.id !in a.picked }
    val details = picked.map { TaskEvalDetail(null, "${it.label} — ${coinsText(it.price)}", it.why) }.toMutableList()
    details += TaskEvalDetail(left >= 0, "Итого: ${coinsText(total)} из ${def.budget}", if (left >= 0) "Осталось ${coinsText(left)}." else "Не хватает ${coinsText(-left)}.")

    val stars: Int
    val summary: String
    var recovery: String? = null
    when {
        left < 0 -> {
            stars = 1
            summary = "В корзине на ${coinsText(-left)} больше, чем у тебя есть."
            recovery = "Убери что-нибудь необязательное — и оплата пройдёт."
        }
        missing.isNotEmpty() -> {
            stars = 1
            summary = "В корзине нет: ${missing.joinToString(", ") { it.label.lowercase() }}. Без этого питомцу будет трудно."
            recovery = "Сначала кладём нужное. На него монет хватит."
        }
        left >= def.minLeft -> {
            stars = 3
            summary = "Нужное куплено, и ещё осталось про запас. Так и надо!"
        }
        else -> {
            stars = 2
            summary = "Нужное куплено, но монет почти не осталось. Хорошо бы оставлять хоть немного."
            recovery = "Попробуй убрать одну мелочь из необязательного."
        }
    }
    return TaskEval(stars, headline(stars), summary, details, recovery, def.learn)
}

private fun evalAllocate(def: AllocateTask, a: TaskAnswer.Allocate): TaskEval {
    val used = DIRECTIONS.sumOf { a.amounts[it] }
    val details = def.rules.map { r ->
        val v = a.amounts[r.bucket]
        val ok = if (r.op == "min") v >= r.value else v <= r.value
        TaskEvalDetail(ok, if (ok) r.ok else r.fail)
    }
    val passes = details.count { it.ok == true }
    if (used > def.total) {
        return TaskEval(1, headline(1), "Ты разложил $used, а всего монет ${def.total}. Так нельзя: расходы не должны быть больше доходов.", details, RETRY, def.learn)
    }
    val stars = if (passes == def.rules.size) 3 else if (passes == def.rules.size - 1) 2 else 1
    return TaskEval(
        stars, headline(stars),
        if (stars == 3) "Монет хватает на всё: и на нужное, и на копилку." else "Получилось не всё. Посмотри, что можно поправить.",
        details, if (stars < 3) RETRY else null, def.learn,
    )
}

private fun evalCalc(def: CalcTask, a: TaskAnswer.Calc): TaskEval {
    val details = def.questions.map { q ->
        val v = a.values[q.id]
        val ok = v == q.answer
        TaskEvalDetail(ok, q.text, if (ok) "Верно! ${q.explain}" else "Твой ответ: ${v?.toString() ?: "—"}. Правильно: ${q.answer}. ${q.explain}")
    }
    val correct = details.count { it.ok == true }
    val stars = starsFromRatio(correct, details.size)
    return TaskEval(stars, headline(stars), "Верных ответов: $correct из ${details.size}.", details, if (stars < 3) RETRY else null, def.learn)
}

fun simWeeks(def: SimTask, weekly: Int): Int? = if (weekly > 0) Math.ceil((def.goalCost - def.start).toDouble() / weekly).toInt() else null

private fun evalSim(def: SimTask, a: TaskAnswer.Sim): TaskEval {
    val weekly = minOf(maxOf(0, a.weekly), def.freePerWeek)
    val weeks = simWeeks(def, weekly)
    val stars: Int
    val summary: String
    when {
        weeks == null -> { stars = 1; summary = def.texts.none }
        weeks <= def.targetWeeks -> { stars = 3; summary = def.texts.great }
        weeks <= def.targetWeeks * 2 -> { stars = 2; summary = def.texts.good }
        else -> { stars = 1; summary = def.texts.slow }
    }
    val details = if (weeks == null) listOf(TaskEvalDetail(false, "Ты решил не откладывать", "Копилка так и останется пустой."))
    else listOf(
        TaskEvalDetail(null, "Откладываешь по ${coinsText(weekly)} в неделю", "Цель в ${coinsText(def.goalCost)} накопится за ${weeksText(weeks)}."),
        TaskEvalDetail(null, "На радости остаётся ${coinsText(def.freePerWeek - weekly)} в неделю", "Это монеты, которые можно потратить на желаемое."),
    )
    return TaskEval(stars, headline(stars), summary, details, if (stars < 3) RETRY else null, def.learn)
}

data class StoryStep(val text: String, val choice: String)

/** Проходит историю по выбранным вариантам и возвращает концовку. */
fun walkStory(def: StoryTask, path: List<Int>): Pair<StoryEnd?, List<StoryStep>> {
    var node = def.nodes[def.start]
    val steps = mutableListOf<StoryStep>()
    for (idx in path) {
        val n = node ?: break
        val choice = n.choices.getOrNull(idx) ?: break
        steps += StoryStep(n.text, choice.text)
        if (choice.end != null) return choice.end to steps
        node = def.nodes[choice.next ?: ""]
    }
    return null to steps
}

private fun evalStory(def: StoryTask, a: TaskAnswer.Story): TaskEval {
    val (end, steps) = walkStory(def, a.path)
    end ?: throw IllegalStateException("История не дошла до концовки")
    val details = steps.map { TaskEvalDetail(null, it.text, "Ты выбрал: «${it.choice}»") }.toMutableList()
    details += TaskEvalDetail(if (end.stars == 3) true else null, "Почему так?", end.explain)
    return TaskEval(end.stars, headline(end.stars), end.consequence, details, end.recovery, def.learn)
}

// ---------- Решатели (для тестов и демо-режима) ----------

/** Лучший ответ на задание. */
fun solveTask(def: TaskDef): TaskAnswer = when (def) {
    is SortTask -> TaskAnswer.Sort(def.items.associate { it.id to it.group })
    is CartTask -> {
        val picked = def.items.filter { it.need }.map { it.id }.toMutableList()
        var total = def.items.filter { it.need }.sumOf { it.price }
        for (it in def.items.filter { !it.need }.sortedBy { it.price }) {
            if (def.budget - (total + it.price) >= def.minLeft) {
                picked += it.id
                total += it.price
                break
            }
        }
        TaskAnswer.Cart(picked)
    }
    is AllocateTask -> {
        val amounts = Plan()
        for (r in def.rules) if (r.op == "min") amounts[r.bucket] = maxOf(amounts[r.bucket], r.value)
        var rest = def.total - DIRECTIONS.sumOf { amounts[it] }
        val order = listOf(Direction.Must, Direction.Save, Direction.Want)
        var i = 0
        while (rest >= def.step && i < 1000) {
            val d = order[i % order.size]
            val cap = def.rules.find { it.bucket == d && it.op == "max" }?.value ?: Int.MAX_VALUE
            if (amounts[d] + def.step <= cap) {
                amounts[d] = amounts[d] + def.step
                rest -= def.step
            }
            i++
        }
        TaskAnswer.Allocate(amounts)
    }
    is CalcTask -> TaskAnswer.Calc(def.questions.associate { it.id to it.answer })
    is SimTask -> {
        var w = def.step
        var found: Int? = null
        while (w <= def.freePerWeek) {
            if ((simWeeks(def, w) ?: Int.MAX_VALUE) <= def.targetWeeks) { found = w; break }
            w += def.step
        }
        TaskAnswer.Sim(found ?: def.freePerWeek)
    }
    is StoryTask -> TaskAnswer.Story(findStoryPath(def) { it == 3 })
}

/** Заведомо неудачный ответ — нужен, чтобы проверять «ошибочный вариант» и путь восстановления. */
fun worstAnswer(def: TaskDef): TaskAnswer = when (def) {
    is SortTask -> TaskAnswer.Sort(def.items.associate { it.id to (if (it.group == "need") "want" else "need") })
    is CartTask -> {
        val picked = mutableListOf<String>()
        var total = 0
        for (it in def.items.filter { !it.need }.sortedBy { it.price }) {
            if (total + it.price <= def.budget) {
                picked += it.id
                total += it.price
            }
        }
        TaskAnswer.Cart(picked)
    }
    is AllocateTask -> TaskAnswer.Allocate(Plan(want = def.total))
    is CalcTask -> TaskAnswer.Calc(def.questions.associate { it.id to it.answer + 1 })
    is SimTask -> TaskAnswer.Sim(0)
    is StoryTask -> TaskAnswer.Story(findStoryPath(def) { it == 1 })
}

private fun findStoryPath(def: StoryTask, want: (Int) -> Boolean): List<Int> {
    fun search(nodeId: String, path: List<Int>): List<Int>? {
        val node = def.nodes.getValue(nodeId)
        node.choices.forEachIndexed { i, c ->
            if (c.end != null && want(c.end.stars)) return path + i
            if (c.next != null) search(c.next, path + i)?.let { return it }
        }
        return null
    }
    return search(def.start, emptyList()) ?: throw IllegalStateException("В задании ${def.id} нет подходящей концовки")
}

// ---------- Доступность и выдача наград ----------

fun isUnlocked(def: TaskDef, p: Profile): Boolean = p.isDemo || def.unlockPeriod <= p.period.index

fun availableTasks(p: Profile, ctx: Ctx): List<TaskDef> = ctx.content.tasks.filter { isUnlocked(it, p) }

fun taskStars(p: Profile, id: String): Int = p.tasks[id]?.stars ?: 0

/** Активное задание для главного экрана: сначала новое, потом то, где можно лучше. */
fun nextActiveTask(p: Profile, ctx: Ctx): TaskDef? {
    val open = availableTasks(p, ctx).filter { taskStars(p, it.id) < 3 }
    return open.find { taskStars(p, it.id) == 0 } ?: open.firstOrNull()
}

data class SubmitResult(val evaluation: TaskEval, val extra: Int, val improved: Boolean)

fun submitTask(p0: Profile, taskId: String, answer: TaskAnswer, ctx: Ctx): Outcome<SubmitResult> {
    val def = ctx.content.tasks.find { it.id == taskId } ?: return fail(ErrorCode.TASK_NOT_FOUND, "Такого задания нет.")
    if (!isUnlocked(def, p0)) return fail(ErrorCode.TASK_LOCKED, "Это задание откроется на ${def.unlockPeriod}-й неделе.")

    val evaluation = evaluateTask(def, answer)
    val p = p0.deepCopy()
    val prog = p.tasks[taskId] ?: TaskProgress()
    prog.attempts += 1
    val improved = evaluation.stars > prog.stars
    var extra = 0
    if (improved) {
        extra = maxOf(0, def.rewards[evaluation.stars] - prog.paid)
        prog.paid += extra
        prog.stars = evaluation.stars
    }
    p.tasks[taskId] = prog

    val before = p.wallet
    if (extra > 0) {
        p.wallet += extra
        p.period.earned += extra
        addTx(p, TxKind.Task, "Задание «${def.title}»", extra)
        p.pet.reason = PetReason("Ты справился с заданием — молодец! Питомцу интересно учиться вместе с тобой.")
    }
    return done(
        p,
        Report(
            title = "Задание: ${def.title}",
            tone = if (evaluation.stars == 3) Tone.Good else Tone.Info,
            changes = if (extra > 0) listOf(Change(ChangeKind.Wallet, "Монеты", delta = extra, before = before, after = p.wallet, why = "Награда за задание: ${"★".repeat(evaluation.stars)} из 3.")) else emptyList(),
            explain = evaluation.summary,
        ),
        SubmitResult(evaluation, extra, improved),
    )
}
