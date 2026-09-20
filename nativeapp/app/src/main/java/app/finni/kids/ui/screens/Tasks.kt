package app.finni.kids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.content.AllocateTask
import app.finni.kids.content.CalcTask
import app.finni.kids.content.CartTask
import app.finni.kids.content.SimTask
import app.finni.kids.content.SortTask
import app.finni.kids.content.StoryTask
import app.finni.kids.content.TaskDef
import app.finni.kids.domain.DIRECTIONS
import app.finni.kids.domain.Direction
import app.finni.kids.domain.Mode
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.Plan
import app.finni.kids.domain.SubmitResult
import app.finni.kids.domain.TaskAnswer
import app.finni.kids.domain.TaskEval
import app.finni.kids.domain.coinsText
import app.finni.kids.domain.isUnlocked
import app.finni.kids.domain.simWeeks
import app.finni.kids.domain.submitTask
import app.finni.kids.domain.walkStory
import app.finni.kids.domain.weeksText
import app.finni.kids.platform.Beep
import app.finni.kids.platform.Sound
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.Card
import app.finni.kids.ui.Coin
import app.finni.kids.ui.Confetti
import app.finni.kids.ui.DIR_ICON
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Muted
import app.finni.kids.ui.NoteBox
import app.finni.kids.ui.Screen
import app.finni.kids.ui.StarsRow
import app.finni.kids.ui.Stepper
import app.finni.kids.ui.StripeCard
import app.finni.kids.ui.Tint
import app.finni.kids.ui.Tx
import app.finni.kids.ui.dirColor
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal

// ---------- Список заданий ----------

@Composable
fun TasksScreen() {
    val app = Game.app
    val p = Game.profile ?: return
    val pl = pal
    Screen("Задания", hint = "Играй в ситуации, выбирай и считай. За задания дают монеты.") {
        if (app.mode == Mode.Demo) NoteBox("Демо-режим: все задания открыты сразу.")
        for (t in Game.content.topics) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(t.icon, 44.dp)
                    Column { Tx(t.title, Txt.h3); Muted(t.blurb, style = Txt.small) }
                }
                for (task in Game.content.tasks.filter { it.topic == t.id }) {
                    val open = isUnlocked(task, p)
                    val stars = p.tasks[task.id]?.stars ?: 0
                    val shape = RoundedCornerShape(22.dp)
                    Row(
                        Modifier.fillMaxWidth().alpha(if (open) 1f else 0.6f).clip(shape).background(if (pl.highContrast) Color.Black else pl.surface).border(1.dp, pl.line, shape)
                            .then(if (open) Modifier.clickable(role = Role.Button) { Sound.play(Beep.Tap); Nav.go("/task/${task.id}") } else Modifier)
                            .heightIn(min = 72.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (open) "star" else "locked", 44.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Tx(task.title, Txt.bodyBold)
                            Muted(if (open) task.skill else "Откроется на ${task.unlockPeriod}-й неделе", style = Txt.small)
                            if (open) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { StarsRow(stars); Muted("до ${coinsText(task.rewards.three)}", style = Txt.small) }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Числовое поле ----------

@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLen: Int = 4, onDone: (() -> Unit)? = null) {
    val pl = pal
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value, { onChange(it.filter(Char::isDigit).take(maxLen)) }, singleLine = true,
        textStyle = Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 1.4f, color = pl.ink, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(pl.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus(); onDone?.invoke() }),
        modifier = modifier.widthIn(max = 160.dp).semantics { contentDescription = label },
        decorationBox = { inner -> Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(Color(0x99060928)).border(2.5.dp, pl.line, shape).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { inner() } },
    )
}

// ---------- Игровые формы ----------

@Composable
private fun SortPlay(def: SortTask, submit: (TaskAnswer) -> Unit) {
    val ans: SnapshotStateMap<String, String> = remember { mutableStateMapOf() }
    val left = def.items.count { it.id !in ans }
    val pl = pal
    Muted("Нажми «Нужное» или «Хочется» у каждой покупки.")
    for (it in def.items) {
        val shape = RoundedCornerShape(14.dp)
        Column(Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Color.Black else pl.surface).border(1.dp, pl.line, shape).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(it.icon, 44.dp); Tx(it.label, Txt.bodyBold) }
            app.finni.kids.ui.Segmented(ans[it.id], def.groups.map { g -> app.finni.kids.ui.SegOpt<String?>(g.id, g.label) }, { v -> if (v != null) ans[it.id] = v })
        }
    }
    FButton(if (left > 0) "Осталось выбрать: $left" else "Проверить", { submit(TaskAnswer.Sort(ans.toMap())) }, block = true, enabled = left == 0)
}

@Composable
private fun CartPlay(def: CartTask, submit: (TaskAnswer) -> Unit) {
    val picked: SnapshotStateList<String> = remember { mutableStateListOf() }
    val total = def.items.filter { it.id in picked }.sumOf { it.price }
    val left = def.budget - total
    val pl = pal
    Card(tint = Tint.Gold) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Tx("Твои монеты"); Coin(def.budget, big = true) } }
    Grid(def.items, 2, gap = 10.dp) { it, m ->
        val on = it.id in picked
        val shape = RoundedCornerShape(22.dp)
        Column(
            m.heightIn(min = 148.dp).clip(shape).background(if (on) pl.saveBg else pl.surface).border(if (on) 2.dp else 1.5.dp, if (on) pl.save else pl.line, shape)
                .semantics { contentDescription = it.label + if (on) ", в корзине" else "" }
                .clickable(role = Role.Checkbox) { Sound.play(Beep.Tap); if (on) picked.remove(it.id) else picked.add(it.id) }.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(it.icon, 56.dp)
            Tx(it.label, Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 0.96f), align = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Icon("coin", 20.dp); Tx(it.price.toString(), Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold)) }
            if (on) Row(Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(pl.saveBg).border(1.dp, pl.save.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape).padding(horizontal = 8.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon("check", 16.dp, tint = pl.save); Tx("В корзине", Txt.tiny.copy(fontWeight = FontWeight.Bold), color = pl.save)
            }
        }
    }
    Card(tint = if (left < 0) Tint.Warn else Tint.Plain) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Muted("В корзине"); Tx("$total из ${def.budget}", Txt.bodyBold) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Muted(if (left >= 0) "Останется" else "Не хватает"); Tx(Math.abs(left).toString(), Txt.bodyBold) }
    }
    FButton(if (left < 0) "Убери что-нибудь" else "Оплатить", { submit(TaskAnswer.Cart(picked.toList())) }, block = true, icon = "bag", enabled = picked.isNotEmpty() && left >= 0)
}

@Composable
private fun AllocatePlay(def: AllocateTask, submit: (TaskAnswer) -> Unit) {
    var amounts by remember { mutableStateOf(Plan()) }
    val used = DIRECTIONS.sumOf { amounts[it] }
    val rest = def.total - used
    val pl = pal
    Card(tint = Tint.Gold) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Tx("Всего монет"); Coin(def.total, big = true) } }
    Tx(if (rest == 0) "Всё разложено!" else "Осталось разложить: $rest", Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 1.06f), color = if (rest == 0) pl.save else pl.ink, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    for (b in def.buckets) {
        StripeCard(dirColor(pl, b.id)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(DIR_ICON.getValue(b.id), 40.dp)
                Column { Tx(b.label, Txt.bodyBold); Muted(b.hint, style = Txt.small) }
            }
            Stepper(amounts[b.id], amounts[b.id] + rest, { v -> amounts = amounts.copy().also { it[b.id] = v } }, b.label, step = def.step)
        }
    }
    FButton("Готово", { submit(TaskAnswer.Allocate(amounts.copy())) }, block = true, enabled = used > 0)
}

@Composable
private fun CalcPlay(def: CalcTask, submit: (TaskAnswer) -> Unit) {
    val vals: SnapshotStateMap<String, String> = remember { mutableStateMapOf() }
    val filled = def.questions.all { !vals[it.id].isNullOrEmpty() }
    val pl = pal
    for ((i, q) in def.questions.withIndex()) {
        val shape = RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Color.Black else pl.surface).border(1.dp, pl.line, shape).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row { Tx("${i + 1}. ", Txt.bodyBold); Tx(q.text, Txt.body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(vals[q.id] ?: "", { vals[q.id] = it }, "Ответ на вопрос ${i + 1}")
                Muted(q.unit, style = Txt.small)
            }
        }
    }
    FButton("Проверить", { submit(TaskAnswer.Calc(def.questions.associate { it.id to vals[it.id]?.toIntOrNull() })) }, block = true, enabled = filled)
}

@Composable
private fun SimPlay(def: SimTask, submit: (TaskAnswer) -> Unit) {
    var weekly by remember { mutableIntStateOf(def.step) }
    val weeks = simWeeks(def, weekly)
    val shown = minOf(12, weeks ?: 12)
    val target = def.goalCost - def.start
    val pl = pal
    Card(tint = Tint.Gold) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Tx("Мечта стоит"); Coin(def.goalCost, big = true) } }
    Muted("Каждую неделю у тебя свободно ${coinsText(def.freePerWeek)}. Сколько отложишь?", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Stepper(weekly, def.freePerWeek, { weekly = it }, "Откладываю в неделю", step = def.step, unit = " в неделю")
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().height(130.dp).clip(shape).background(Color(0x80060928)).border(1.dp, pl.line, shape).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
            .semantics { contentDescription = if (weeks == null) "Копилка не растёт" else "Мечта накопится за ${weeksText(weeks)}" },
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 0 until 12) {
            val saved = minOf(target, weekly * (i + 1))
            val on = i < shown && weekly > 0
            val frac = if (on) maxOf(8f, saved.toFloat() / target * 100f) / 100f else 0.06f
            val full = saved >= target
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(frac).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(
                        if (!on) SolidColor(Color.White.copy(alpha = 0.18f)) else if (full) Brush.verticalGradient(listOf(Color(0xFFFFE07A), Color(0xFFFFAB1F))) else Brush.verticalGradient(listOf(Color(0xFF78F4B8), Color(0xFF2FD08C))),
                    ))
                }
                Muted((i + 1).toString(), style = Txt.tiny.copy(fontSize = Txt.tiny.fontSize * 0.85f))
            }
        }
    }
    if (weeks == null) Tx("Копилка не растёт — мечта не накопится.", Txt.bodyBold, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    else Tx("Мечта накопится за ${weeksText(weeks)}. На радости останется ${def.freePerWeek - weekly} в неделю.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    FButton("Готово", { submit(TaskAnswer.Sim(weekly)) }, block = true)
}

@Composable
private fun StoryPlay(def: StoryTask, submit: (TaskAnswer) -> Unit) {
    var node by remember { mutableStateOf(def.start) }
    var path by remember { mutableStateOf(listOf<Int>()) }
    val cur = def.nodes.getValue(node)
    val (_, steps) = walkStory(def, path)
    val pl = pal
    for (s in steps) {
        val shape = RoundedCornerShape(14.dp)
        Column(Modifier.fillMaxWidth().clip(shape).background(Color.White.copy(alpha = 0.07f)).border(1.dp, pl.line, shape).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Muted(s.text, style = Txt.small)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Icon("check", 18.dp); Tx(s.choice, Txt.bodyBold) }
        }
    }
    Card(tint = Tint.Must) { Tx(cur.text, Txt.lead) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cur.choices.forEachIndexed { i, c ->
            FButton(c.text, {
                val np = path + i
                if (c.end != null) submit(TaskAnswer.Story(np)) else { path = np; node = c.next!! }
            }, block = true, variant = BtnVariant.Secondary)
        }
    }
}

// ---------- Экран задания и результат ----------

@Composable
private fun ResultView(def: TaskDef, ev: TaskEval, extra: Int, retry: () -> Unit) {
    val pl = pal
    val good = ev.stars == 3
    val (bg, border) = when (ev.stars) { 3 -> pl.saveBg to pl.save.copy(alpha = 0.55f); 2 -> pl.goldBg to pl.gold.copy(alpha = 0.55f); else -> pl.warnBg to pl.warn.copy(alpha = 0.5f) }
    val shape = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Color.Black else bg).border(if (pl.highContrast) 3.dp else 1.dp, if (pl.highContrast) Color.White else border, shape).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StarsRow(ev.stars, size = 34.dp)
        Tx(ev.headline, Txt.h2, align = TextAlign.Center)
        Tx(ev.summary, Txt.lead, align = TextAlign.Center)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (d in ev.details) {
            val s = RoundedCornerShape(14.dp)
            Row(Modifier.fillMaxWidth().clip(s).background(if (pl.highContrast) Color.Black else pl.surface).border(1.dp, pl.line, s).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(if (d.ok == true) "check" else if (d.ok == false) "cross" else "info", 22.dp, tint = if (d.ok == true) pl.save else if (d.ok == false) pl.want else pl.ink)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Tx(d.text); d.sub?.let { Muted(it, style = Txt.small) } }
            }
        }
    }
    ev.recovery?.let { Card(tint = Tint.Gold) { Tx("Как исправить", Txt.bodyBold); Tx(it) } }
    Card(tint = Tint.Save) { Tx("Запомни", Txt.bodyBold); Tx(ev.learn) }
    Card(tint = Tint.Gold) {
        if (extra > 0) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Coin(extra); Tx("Награда за задание «${def.title}». Монеты уже в кошельке.", modifier = Modifier.weight(1f)) }
        else Tx("Награда за это задание уже получена. Повторяй его для практики — монеты дадут, только если получится лучше.")
    }
    if (!good) FButton("Попробовать ещё", retry, block = true, variant = BtnVariant.Secondary, icon = "refresh")
    FButton("К заданиям", { Nav.back() }, block = true)
}

@Composable
fun TaskPlayerScreen(id: String) {
    val def = Game.content.tasks.find { it.id == id }
    var attempt by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Pair<TaskEval, Int>?>(null) }
    if (def == null) {
        Screen("Задание") { Tx("Такого задания нет.") }
        return
    }
    val submit: (TaskAnswer) -> Unit = { answer ->
        val r = Game.act { pp, c -> submitTask(pp, def.id, answer, c) }
        if (r is Outcome.Ok<SubmitResult>) {
            Sound.play(if (r.extra.evaluation.stars == 3) Beep.Good else Beep.Oops)
            result = r.extra.evaluation to r.extra.extra
        }
    }
    Box(Modifier.fillMaxSize()) {
        Screen(def.title, hint = if (result == null) def.intro else null) {
            val res = result
            if (res != null) ResultView(def, res.first, res.second) { result = null; attempt++ }
            else key(attempt) {
                when (def) {
                    is SortTask -> SortPlay(def, submit)
                    is CartTask -> CartPlay(def, submit)
                    is AllocateTask -> AllocatePlay(def, submit)
                    is CalcTask -> CalcPlay(def, submit)
                    is SimTask -> SimPlay(def, submit)
                    is StoryTask -> StoryPlay(def, submit)
                }
            }
        }
        val res = result
        if (res != null && res.first.stars == 3 && Game.app.settings.animations) Confetti()
    }
}
