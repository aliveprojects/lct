package app.finni.kids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.app.present
import app.finni.kids.domain.CustomGoalInput
import app.finni.kids.domain.Direction
import app.finni.kids.domain.Econ
import app.finni.kids.domain.Goal
import app.finni.kids.domain.Profile
import app.finni.kids.domain.activeGoal
import app.finni.kids.domain.clamp
import app.finni.kids.domain.completeGoal
import app.finni.kids.domain.createCustomGoal
import app.finni.kids.domain.deposit
import app.finni.kids.domain.envelope
import app.finni.kids.domain.etaFor
import app.finni.kids.domain.previewWithdraw
import app.finni.kids.domain.savingsTotal
import app.finni.kids.domain.selectGoal
import app.finni.kids.domain.weeksAt
import app.finni.kids.domain.weeksText
import app.finni.kids.domain.withdraw
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.Card
import app.finni.kids.ui.Coin
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Meter
import app.finni.kids.ui.MeterKind
import app.finni.kids.ui.Muted
import app.finni.kids.ui.NoteBox
import app.finni.kids.ui.Screen
import app.finni.kids.ui.Stepper
import app.finni.kids.ui.Tint
import app.finni.kids.ui.Tx
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal
import kotlinx.coroutines.launch

@Composable
private fun KVRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Muted(label); Tx(value, Txt.bodyBold) }
}

@Composable
private fun WithdrawSheet(goalId: String, close: () -> Unit) {
    val p = Game.profile ?: return
    val goal = p.goals.first { it.id == goalId }
    var amt by remember { mutableIntStateOf(minOf(goal.saved, Econ.planStep)) }
    val pv = previewWithdraw(p, goalId, amt) ?: return
    Tx("Копилка — это монеты на мечту. Если заберёшь их, мечта станет дальше. Проверь, как всё изменится.", Txt.lead)
    Stepper(amt, goal.saved, { amt = it }, "Сколько забрать", min = 1, step = Econ.planStep, unit = " монет")
    Card {
        KVRow("Накоплено", "${pv.savedBefore} → ${pv.savedAfter}")
        KVRow("До мечты останется", "${pv.remainingBefore} → ${pv.remainingAfter}")
        KVRow("Кошелёк", "${pv.walletBefore} → ${pv.walletAfter}")
        KVRow("Срок", if (pv.etaBefore.weeks == null) "пока нельзя посчитать" else "${weeksText(pv.etaBefore.weeks)} → ${weeksText(pv.etaAfter.weeks ?: 0)}")
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FButton("Оставить в копилке", close, Modifier.fillMaxWidth(), block = true, variant = BtnVariant.Secondary)
        FButton("Забрать $amt", {
            Ui.scope.launch {
                val r = Game.act { pp, _ -> withdraw(pp, goalId, amt, true) }
                close()
                present(r)
            }
        }, Modifier.fillMaxWidth(), block = true, variant = BtnVariant.Gold)
    }
}

@Composable
private fun CustomGoalSheet(close: () -> Unit) {
    var pick by remember { mutableIntStateOf(0) }
    var cost by remember { mutableIntStateOf(50) }
    val presets = Game.content.customGoal.presets
    val pl = pal
    Tx("Выбери мечту и назначь ей цену.", Txt.lead)
    Grid(presets.withIndex().toList(), 3) { (i, pr), m ->
        val shape = RoundedCornerShape(18.dp)
        val on = i == pick
        Column(
            m.heightIn(min = 84.dp).clip(shape).background(if (on) pl.primarySoft else pl.surface).border(2.5.dp, if (on) pl.primary else pl.line, shape).clickable { pick = i }.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(pr.icon, 48.dp)
            Tx(pr.title, Txt.tiny.copy(fontSize = Txt.tiny.fontSize * 1.05f), align = TextAlign.Center)
        }
    }
    Stepper(cost, Econ.customMax, { cost = it }, "Цена мечты", min = Econ.customMin, step = Econ.customStep, unit = " монет")
    Muted("Цена — от ${Econ.customMin} до ${Econ.customMax} монет.")
    FButton("Создать мечту", {
        Ui.scope.launch {
            val r = Game.act { pp, c -> createCustomGoal(pp, CustomGoalInput(presets[pick].title, presets[pick].icon, cost), c) }
            close()
            present(r)
        }
    }, block = true)
}

@Composable
private fun ActiveGoal(p: Profile, goal: Goal) {
    val scope = app.finni.kids.app.Ui.scope
    val pl = pal
    val remaining = goal.cost - goal.saved
    val env = envelope(p, Direction.Save)
    val max = minOf(p.wallet, remaining)
    val suggested = if (env.left > 0) minOf(env.left, max) else minOf(10, max)
    var raw by remember(goal.id) { mutableStateOf<Int?>(null) }
    val amount = clamp(raw ?: suggested, 0, max)
    val eta = etaFor(goal, p.period.index)
    val reached = goal.saved >= goal.cost
    val pct = Math.round(goal.saved.toDouble() / goal.cost * 100).toInt()
    val shape = RoundedCornerShape(22.dp)

    Column(
        Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Brush.verticalGradient(listOf(Color.Black, Color.Black)) else Brush.verticalGradient(listOf(Color(0x4778F4B8), Color(0x1A3CDC96)))).border(if (pl.highContrast) 3.dp else 1.dp, if (pl.highContrast) Color.White else pl.save.copy(alpha = 0.5f), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Icon(goal.icon, 96.dp) }
        Tx(goal.title, Txt.h2, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) { Tx("Цена мечты: "); Coin(goal.cost) }
        Meter(goal.saved, goal.cost, "Накоплено на «${goal.title}»", kind = MeterKind.Ok, text = "$pct%")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((label, v) in listOf("Накоплено" to goal.saved, "Осталось" to remaining, "Цена" to goal.cost)) {
                val s = RoundedCornerShape(14.dp)
                Column(Modifier.weight(1f).clip(s).background(Color(0x80060928)).border(1.dp, pl.line, s).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Muted(label, style = Txt.tiny.copy(fontSize = Txt.tiny.fontSize * 1.05f))
                    Tx(v.toString(), Txt.big.copy(fontSize = Txt.big.fontSize * 0.98f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon("history", 22.dp)
            Tx(eta.text, Txt.bodyBold.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), modifier = Modifier.weight(1f))
        }
        if (eta.avg == null && remaining > 0) Muted("Например: если откладывать по 10 монет в неделю, это ${weeksText(weeksAt(remaining, 10))}.")

        if (reached) {
            FButton("Исполнить мечту!", { scope.launch { present(Game.act { pp, _ -> completeGoal(pp, goal.id) }) } }, block = true, variant = BtnVariant.Gold, icon = "trophy")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!p.period.planConfirmed) NoteBox("Откладывать можно после плана недели.")
                Stepper(amount, max, { raw = it }, "Сколько отложить", step = Econ.planStep, unit = " монет")
                if (p.period.planConfirmed && env.planned > 0) Muted("В плане на копилку: ${env.planned}, уже отложено: ${env.used}.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                FButton("Отложить ${if (amount > 0) amount else ""}", {
                    scope.launch { if (present(Game.act { pp, _ -> deposit(pp, goal.id, amount) })) raw = null }
                }, block = true, variant = BtnVariant.Good, icon = "jar", enabled = amount > 0)
                if (!p.period.planConfirmed) FButton("Составить план", { Nav.go("/plan") }, variant = BtnVariant.Secondary)
            }
        }
        if (goal.saved > 0) FButton("Забрать монеты из копилки", { scope.launch { Ui.sheet("Забрать из копилки") { close -> WithdrawSheet(goal.id, close) } } }, block = true, variant = BtnVariant.Secondary, icon = "refresh")
    }
}

@Composable
fun GoalsScreen() {
    val p = Game.profile ?: return
    val scope = app.finni.kids.app.Ui.scope
    val goal = activeGoal(p)
    val others = p.goals.filter { !it.done && it.id != goal?.id }
    val done = p.goals.filter { it.done }
    val pl = pal

    Screen("Копилка", hint = "Выбери мечту и откладывай на неё монеты.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f), padding = 10.dp) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Tx("Кошелёк", Txt.body.copy(fontSize = Txt.body.fontSize * 0.92f), maxLines = 1); Coin(p.wallet) } }
            Card(Modifier.weight(1f), padding = 10.dp) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Tx("В копилке", Txt.body.copy(fontSize = Txt.body.fontSize * 0.92f), maxLines = 1); Coin(savingsTotal(p)) } }
        }
        if (goal != null) ActiveGoal(p, goal)
        else Card(tint = Tint.Gold) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Icon("trophy", 64.dp) }
            Tx("Все мечты исполнены! Выбери новую.", Txt.bodyBold, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        if (others.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tx("Другие мечты", Txt.h3)
            for (g in others) {
                val shape = RoundedCornerShape(22.dp)
                Row(Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Color.Black else pl.surface).border(1.dp, pl.line, shape).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(g.icon, 48.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Tx(g.title, Txt.bodyBold)
                        Meter(g.saved, g.cost, "Накоплено на «${g.title}»", kind = MeterKind.Ok, text = "${g.saved} из ${g.cost}")
                    }
                    FButton("Выбрать", { scope.launch { present(Game.act { pp, _ -> selectGoal(pp, g.id) }) } }, small = true, variant = BtnVariant.Secondary)
                }
            }
        }
        FButton("Создать свою мечту", { scope.launch { Ui.sheet("Своя мечта") { close -> CustomGoalSheet(close) } } }, block = true, variant = BtnVariant.Secondary, icon = "plus")

        if (done.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tx("Исполненные мечты", Txt.h3)
            for (g in done) Row(Modifier.clip(CircleShape).background(pl.goldBg).border(1.dp, pl.gold.copy(alpha = 0.6f), CircleShape).padding(start = 8.dp, end = 14.dp, top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(g.icon, 36.dp); Tx(g.title, Txt.bodyBold)
            }
        }
    }
}
