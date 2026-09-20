package app.finni.kids.content

/** Проверяет целостность учебного контента. Возвращает список проблем (пусто — всё хорошо). */
fun validateContent(c: Content): List<String> {
    val errs = mutableListOf<String>()
    fun dup(label: String, ids: List<String>) {
        val seen = mutableSetOf<String>()
        for (id in ids) {
            if (!seen.add(id)) errs += "$label: повторяется id «$id»"
        }
    }
    dup("items", c.items.map { it.id })
    dup("goals", c.goals.map { it.id })
    dup("tasks", c.tasks.map { it.id })
    dup("glossary", c.glossary.map { it.id })
    dup("events", c.events.map { it.id })

    val accessoryIds = c.pet.accessories.map { it.id }.toSet()
    for (i in c.items) {
        if (i.price <= 0) errs += "item ${i.id}: цена должна быть целым положительным числом"
        if (i.name.isEmpty() || i.note.isEmpty() || i.icon.isEmpty()) errs += "item ${i.id}: не заполнены name/note/icon"
        if (i.kind == app.finni.kids.domain.Direction.Must && i.need == null) errs += "item ${i.id}: у обязательного товара нужно указать need (food/care)"
        if (i.effects.isEmpty()) errs += "item ${i.id}: нет эффектов"
        if (i.accessory != null && i.accessory !in accessoryIds) errs += "item ${i.id}: неизвестное украшение ${i.accessory}"
    }
    for (g in c.goals) if (g.cost <= 0 || g.cost % 5 != 0) errs += "goal ${g.id}: цена должна быть кратна 5"
    for (e in c.events) if (e.period < 1) errs += "event ${e.id}: неверный период"
    for (t in c.tasks) errs += validateTask(t)
    return errs
}

private fun validateTask(t: TaskDef): List<String> {
    val errs = mutableListOf<String>()
    fun e(m: String) { errs += "task ${t.id}: $m" }
    if (t.title.isEmpty() || t.intro.isEmpty() || t.learn.isEmpty() || t.skill.isEmpty()) e("не заполнены title/intro/learn/skill")
    if (t.competencies.isEmpty()) e("не указаны компетенции")
    if (!(t.rewards.one > 0 && t.rewards.two >= t.rewards.one && t.rewards.three >= t.rewards.two)) e("награды должны расти: 1 ≤ 2 ≤ 3")
    when (t) {
        is SortTask -> {
            if (t.items.size < 4) e("слишком мало предметов")
            for (g in listOf("need", "want")) if (t.items.none { it.group == g }) e("нет предметов группы $g")
            for (i in t.items) if (i.why.isEmpty()) e("предмет ${i.id} без объяснения")
        }
        is CartTask -> {
            val needs = t.items.filter { it.need }.sumOf { it.price }
            if (needs + t.minLeft > t.budget) e("на нужное не хватает бюджета")
            if (t.items.none { !it.need }) e("нет необязательных товаров")
        }
        is AllocateTask -> {
            val mins = t.rules.filter { it.op == "min" }.sumOf { it.value }
            if (mins > t.total) e("правила невыполнимы: минимумы больше суммы")
            for (r in t.rules) if (t.buckets.none { it.id == r.bucket }) e("правило ${r.id}: неизвестный конверт")
        }
        is CalcTask -> {
            if (t.questions.isEmpty()) e("нет вопросов")
            for (q in t.questions) if (q.explain.isEmpty()) e("вопрос ${q.id}: нужен числовой ответ и объяснение")
        }
        is SimTask -> if (t.freePerWeek < t.step) e("свободных монет меньше шага")
        is StoryTask -> {
            if (t.nodes[t.start] == null) e("нет стартового узла")
            var hasBad = false
            var hasGood = false
            for ((id, n) in t.nodes) {
                if (n.choices.size < 2) e("узел $id: нужно минимум 2 варианта")
                for (c in n.choices) {
                    if (c.end == null && (c.next == null || t.nodes[c.next] == null)) e("узел $id: вариант «${c.text}» ведёт в никуда")
                    c.end?.let { end ->
                        if (end.stars == 1) hasBad = true
                        if (end.stars == 3) hasGood = true
                        if (end.explain.isEmpty() || end.consequence.isEmpty()) e("узел $id: у концовки нет объяснения")
                    }
                }
            }
            if (!hasBad || !hasGood) e("нужна и удачная, и неудачная концовка")
        }
    }
    return errs
}
