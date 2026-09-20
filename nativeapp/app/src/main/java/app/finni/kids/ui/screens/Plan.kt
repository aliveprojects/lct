package app.finni.kids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.ConfirmOptions
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.app.present
import app.finni.kids.domain.DIRECTIONS
import app.finni.kids.domain.DIRECTION_LABEL
import app.finni.kids.domain.Direction
import app.finni.kids.domain.Econ
import app.finni.kids.domain.Plan
import app.finni.kids.domain.Profile
import app.finni.kids.domain.coinsText
import app.finni.kids.domain.confirmPlan
import app.finni.kids.domain.endPeriod
import app.finni.kids.domain.envelope
import app.finni.kids.domain.freeCoins
import app.finni.kids.domain.needsCost
import app.finni.kids.domain.planRemainder
import app.finni.kids.domain.planTotal
import app.finni.kids.domain.suggestPlan
import app.finni.kids.domain.topUpPlan
import app.finni.kids.domain.wishlistItems
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.Card
import app.finni.kids.ui.Coin
import app.finni.kids.ui.DIR_ICON
import app.finni.kids.ui.DirTag
import app.finni.kids.ui.ButtonRow
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Meter
import app.finni.kids.ui.MeterKind
import app.finni.kids.ui.Muted
import app.finni.kids.ui.NoteBox
import app.finni.kids.ui.Screen
import app.finni.kids.ui.Stepper
import app.finni.kids.ui.StripeCard
import app.finni.kids.ui.TermText
import app.finni.kids.ui.Tint
import app.finni.kids.ui.Tx
import app.finni.kids.ui.dirColor
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal
import kotlinx.coroutines.launch

private val ROW_HINT = mapOf(Direction.Must to "Еда и уход за питомцем", Direction.Want to "Игрушки и украшения", Direction.Save to "Откладываем на мечту")

@Composable
private fun DistributionBar(plan: Plan, total: Int) {
    val rest = maxOf(0, total - planTotal(plan))
    val pl = pal
    val desc = "Нужное ${plan.must}, хочется ${plan.want}, копилка ${plan.save}, запас $rest"
    Row(Modifier.fillMaxWidth().height(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).border(1.dp, pl.line, CircleShape).semantics { contentDescription = desc }) {
        for (d in DIRECTIONS) {
            val v = plan[d]
            if (v > 0) Box(Modifier.weight(v.toFloat()).height(34.dp).background(dirColor(pl, d)), contentAlignment = Alignment.Center) {
                if (v >= total * 0.12) Tx(v.toString(), Txt.smallBold.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF06133A))
            }
        }
        if (rest > 0) Box(Modifier.weight(rest.toFloat()).height(34.dp).background(Color.White.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
            if (rest >= total * 0.12) Tx(rest.toString(), Txt.smallBold.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
        }
    }
}

@Composable
private fun PlanEditor(p: Profile) {
    val scope = app.finni.kids.app.Ui.scope
    val content = Game.content
    val ctx = Game.ctx
    val last = p.history.lastOrNull()
    var plan by remember { mutableStateOf(Plan()) }
    val total = p.wallet
    val remainder = planRemainder(total, plan)
    val needs = needsCost(content)
    val wish = wishlistItems(p, ctx)
    val event = p.pendingEventId?.let { id -> content.events.find { it.id == id } }

    fun confirm() = scope.launch {
        val lowMust = plan.must < needs.total
        val ok = Ui.confirm(
            ConfirmOptions("Подтвердить план?", "Подтвердить", "Изменить", careful = lowMust, icon = "clip") {
                val pl = pal
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in DIRECTIONS) SummaryRow(DIR_ICON.getValue(d), DIRECTION_LABEL.getValue(d), plan[d])
                    if (remainder > 0) SummaryRow("coin", "Запас", remainder)
                }
                if (remainder > 0) Tx("Монеты в запасе останутся в кошельке — пригодятся, если случится неожиданное.")
                if (lowMust) NoteBox("На еду и уход обычно нужно около ${coinsText(needs.total)}. Можно подтвердить и так — но питомцу может не хватить.")
                Muted("После подтверждения план менять нельзя — я сравню его с тем, что получилось.")
            },
        )
        if (ok) present(Game.act { pp, _ -> confirmPlan(pp, plan) })
    }

    Screen(
        "План: неделя ${p.period.index}", hint = "Раздели свои монеты на три части. Что-то можно оставить в запасе.",
        footer = { FButton("Подтвердить план", { confirm() }, block = true, enabled = planTotal(plan) > 0) },
    ) {
        Card(tint = Tint.Gold, padding = 14.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Tx("Всего у тебя"); Coin(total, big = true)
            }
        }
        DistributionBar(plan, total)
        Tx(if (remainder == 0) "Всё разделено!" else "Осталось разделить: $remainder", Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 1.06f), color = if (remainder == 0) pal.save else pal.ink, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        for (d in DIRECTIONS) {
            StripeCard(dirColor(pal, d)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(DIR_ICON.getValue(d), 40.dp)
                    Column {
                        TermText(if (d == Direction.Must) "must" else if (d == Direction.Want) "want" else "savings", DIRECTION_LABEL.getValue(d))
                        Muted(ROW_HINT.getValue(d), style = Txt.small)
                    }
                }
                Stepper(plan[d], plan[d] + remainder, { v -> plan = plan.copy().also { it[d] = v } }, DIRECTION_LABEL.getValue(d), step = Econ.planStep)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (plan.must < needs.total && plan.must > 0) NoteBox("На еду и уход обычно нужно около ${coinsText(needs.total)}.")
            if (event != null) NoteBox("Есть непредвиденная трата: ${coinsText(event.cost)}. Оставь немного в запасе.")
            if (wish.isNotEmpty()) NoteBox("В списке желаний: ${wish.joinToString(", ") { "${it.name} (${it.price})" }}. Можно заложить на «Хочется».")
        }

        ButtonRow {
            FButton("Подсказка", { plan = suggestPlan(p, content) }, variant = BtnVariant.Secondary, icon = "bulb")
            if (last != null && planTotal(last.plan) <= total) FButton("Как в прошлый раз", { plan = last.plan.copy() }, variant = BtnVariant.Secondary, icon = "refresh")
        }
    }
}

@Composable
private fun SummaryRow(icon: String, label: String, value: Int) {
    val pl = pal
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Color.Black else pl.surface).border(1.dp, pl.line, shape).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, 28.dp)
        Tx(label, modifier = Modifier.weight(1f))
        Tx(value.toString(), Txt.bodyBold)
    }
}

private val STATUS = mapOf("none" to "Ещё не начал", "going" to "В процессе", "done" to "Выполнено", "over" to "Больше плана")

@Composable
private fun PlanTracker(p: Profile) {
    val scope = app.finni.kids.app.Ui.scope
    val free = freeCoins(p)
    val ctx = Game.ctx
    val wish = wishlistItems(p, ctx)
    val pl = pal

    fun finish() = scope.launch {
        val miss = buildList { if (p.period.needs.food == 0) add("еду"); if (p.period.needs.care == 0) add("уход") }
        val ok = Ui.confirm(
            ConfirmOptions("Завершить неделю ${p.period.index}?", "Завершить неделю", if (miss.isNotEmpty()) "Вернуться" else "Ещё не всё", careful = true, icon = "trophy") {
                if (miss.isNotEmpty()) NoteBox("Питомец ещё не получил ${miss.joinToString(" и ")}. Хочешь сначала купить?")
                Tx("Я сравню план и то, что получилось, посчитаю рост питомца и начну новую неделю. Остаток монет останется у тебя.")
            },
        )
        if (!ok) return@launch
        val r = Game.act { pp, c -> endPeriod(pp, c) }
        if (present(r)) Nav.replace("/review")
    }

    Screen(
        "Неделя ${p.period.index}: план и факт", hint = "Смотри, сколько ты запланировал и сколько уже потратил.",
        footer = { FButton("Завершить неделю", { finish() }, block = true, variant = BtnVariant.Gold, icon = "trophy") },
    ) {
        Card(tint = Tint.Gold) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Tx("В кошельке"); Coin(p.wallet, big = true) }
        }
        for (d in DIRECTIONS) {
            val e = envelope(p, d)
            val state = if (e.over > 0) "over" else if (e.used == 0) "none" else if (e.used >= e.planned) "done" else "going"
            StripeCard(dirColor(pl, d)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    DirTag(d)
                    val (fg, bg) = when (state) { "done" -> pl.save to pl.saveBg; "over" -> pl.warn to pl.warnBg; else -> pl.ink to Color.White.copy(alpha = 0.14f) }
                    Row(Modifier.clip(CircleShape).background(bg).border(1.dp, fg.copy(alpha = 0.5f), CircleShape).padding(horizontal = 10.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (state == "over") "info" else if (state == "done") "check" else "sparkle", 18.dp, tint = fg)
                        Tx(STATUS.getValue(state), Txt.smallBold, color = fg)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    NumCol("План", e.planned)
                    NumCol(if (d == Direction.Save) "Отложено" else "Потрачено", e.used)
                    NumCol("Осталось", e.left)
                }
                Meter(minOf(e.used, e.planned), maxOf(1, e.planned), "${DIRECTION_LABEL.getValue(d)}: план и факт", kind = when (d) { Direction.Must -> MeterKind.Must; Direction.Want -> MeterKind.Want; Direction.Save -> MeterKind.Ok }, showNumbers = false)
                if (e.over > 0) Muted("Больше плана на ${e.over}. Это нормально — в итогах я подскажу, как сделать лучше.", style = Txt.small)
            }
        }
        if (free > 0) {
            Card(tint = Tint.Gold) {
                Tx("Свободные монеты: $free", Txt.bodyBold)
                Muted("Их можно добавить в любой конверт — по ${Econ.planStep} монет.")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in DIRECTIONS) FButton("+${minOf(Econ.planStep, free)} ${DIRECTION_LABEL.getValue(d)}", {
                        scope.launch { present(Game.act { pp, _ -> topUpPlan(pp, d, minOf(Econ.planStep, freeCoins(pp))) }) }
                    }, small = true, variant = BtnVariant.Secondary, icon = DIR_ICON.getValue(d))
                }
            }
        } else if (free < 0) {
            Card(tint = Tint.Warn) {
                Tx("Для плана не хватает ${-free}", Txt.bodyBold)
                Tx("Ты потратил больше запланированного. Можно заработать монеты в заданиях или в конце недели составить план точнее.")
                FButton("К заданиям", { Nav.go("/tasks") }, small = true, variant = BtnVariant.Secondary)
            }
        }
        if (wish.isNotEmpty()) {
            Card {
                Tx("Список желаний", Txt.bodyBold)
                for (w in wish) Tx("${w.name} — ${coinsText(w.price)}")
            }
        }
    }
}

@Composable
private fun NumCol(label: String, value: Int) {
    Column {
        Muted(label)
        Tx(value.toString(), Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 1.15f, fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
fun PlanScreen() {
    val app = Game.app
    val p = Game.profile ?: return
    if (p.period.planConfirmed) PlanTracker(p) else PlanEditor(p)
}
