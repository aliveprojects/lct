package app.finni.kids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.domain.DIRECTION_LABEL
import app.finni.kids.domain.Direction
import app.finni.kids.domain.Econ
import app.finni.kids.domain.ExpressionCode
import app.finni.kids.domain.PeriodResult
import app.finni.kids.domain.PetStat
import app.finni.kids.domain.Profile
import app.finni.kids.domain.ReviewStatus
import app.finni.kids.domain.STAT_LABEL
import app.finni.kids.domain.coinsText
import app.finni.kids.domain.growthToNext
import app.finni.kids.domain.pointsText
import app.finni.kids.domain.savingsTotal
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.Card
import app.finni.kids.ui.Confetti
import app.finni.kids.ui.DIR_ICON
import app.finni.kids.ui.ButtonRow
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Meter
import app.finni.kids.ui.MeterKind
import app.finni.kids.ui.Muted
import app.finni.kids.ui.STAT_ICON
import app.finni.kids.ui.Screen
import app.finni.kids.ui.StarsRow
import app.finni.kids.ui.Tint
import app.finni.kids.ui.Tx
import app.finni.kids.ui.Tx2
import app.finni.kids.ui.pet.PetOf
import app.finni.kids.ui.pet.PetView
import app.finni.kids.ui.signed
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal
import kotlinx.coroutines.launch

@Composable
private fun StageTrack(p: Profile) {
    val pl = pal
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (s in Game.content.pet.stages) {
            val reached = p.pet.stage >= s.id
            val now = p.pet.stage == s.id
            Column(Modifier.weight(1f).alpha(if (reached) 1f else 0.6f), horizontalAlignment = Alignment.CenterHorizontally) {
                val shape = RoundedCornerShape(18.dp)
                Box(
                    Modifier.fillMaxWidth().then(if (now) Modifier.clip(shape).background(pl.goldBg).border(1.5.dp, pl.gold.copy(alpha = 0.7f), shape) else Modifier).padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PetView(p.pet.appearance, s.id, Modifier.size(64.dp), expression = ExpressionCode.Content)
                    if (!reached) Box(Modifier.align(Alignment.BottomEnd).size(26.dp).clip(CircleShape).background(Color(0xFF2A2F8A)).border(1.dp, pl.line, CircleShape), contentAlignment = Alignment.Center) { Icon("lock", 18.dp) }
                }
                Tx(s.name, Txt.tiny.copy(fontWeight = FontWeight.Bold, fontSize = Txt.tiny.fontSize * 0.98f, lineHeight = Txt.tiny.fontSize * 1.1f), align = TextAlign.Center)
                Muted(if (reached) (if (now) "сейчас" else "пройдено") else "от ${Econ.stageFrom[s.id - 1]} очков", style = Txt.tiny.copy(fontSize = Txt.tiny.fontSize * 0.92f), align = TextAlign.Center)
            }
        }
    }
}

@Composable
fun PlanFactRows(r: PeriodResult) {
    val pl = pal
    class Row3(val d: Direction, val plan: Int, val fact: Int, val ok: Boolean, val verb: String)
    val rows = listOf(
        Row3(Direction.Must, r.plan.must, r.fact.must, r.checks.mustWithin, "потрачено"),
        Row3(Direction.Want, r.plan.want, r.fact.want, r.checks.wantWithin, "потрачено"),
        Row3(Direction.Save, r.plan.save, r.fact.save, r.checks.savedPlan, "отложено"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (x in rows) PfRow(DIR_ICON.getValue(x.d), DIRECTION_LABEL.getValue(x.d), "план ${x.plan} · ${x.verb} ${x.fact}", x.ok, if (x.ok) "по плану" else if (x.d == Direction.Save) "меньше плана" else if (x.fact == 0) "не тратил" else "больше плана", if (x.ok) "check" else "info")
        if (r.fact.event > 0) PfRow("vet", "Непредвиденное", "потрачено ${r.fact.event} · в оценку плана не входит", true, "", "check")
    }
}

@Composable
private fun PfRow(icon: String, title: String, sub: String, ok: Boolean, mark: String, markIcon: String) {
    val pl = pal
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Color.Black else Color(0x73060928)).border(1.dp, pl.line, shape).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, 34.dp)
        Column(Modifier.weight(1f)) { Tx(title, Txt.bodyBold); Muted(sub, style = Txt.small) }
        if (mark.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            val c = if (ok) pl.save else pl.warn
            Icon(markIcon, 20.dp, tint = c); Tx(mark, Txt.tiny.copy(fontWeight = FontWeight.Bold, fontSize = Txt.tiny.fontSize * 1.08f), color = c, align = TextAlign.End)
        }
    }
}

@Composable
fun ProgressScreen() {
    val p = Game.profile ?: return
    val content = Game.content
    val stage = content.pet.stages.first { it.id == p.pet.stage }
    val toNext = growthToNext(p.pet.growth)
    val from = Econ.stageFrom[p.pet.stage - 1]
    val nextFrom = Econ.stageFrom.getOrNull(p.pet.stage)
    val nextName = content.pet.stages.find { it.id == p.pet.stage + 1 }?.name
    val last = p.history.lastOrNull()
    val doneTasks = p.tasks.values.count { it.stars > 0 }
    val scope = app.finni.kids.app.Ui.scope
    val pl = pal
    var opsOpen by remember { mutableStateOf(false) }

    Screen("Успехи", hint = "Смотри, как растёт питомец и что уже получилось.") {
        val shape = RoundedCornerShape(22.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Brush.verticalGradient(listOf(Color.Black, Color.Black)) else Brush.radialGradient(listOf(Color(0x738C6EFF), Color(0x333240BE)), radius = 600f)).border(1.dp, if (pl.highContrast) Color.White else Color(0x80B79BFF), shape).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            PetOf(p, Modifier.size(130.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Tx(p.pet.name, Txt.h2); Tx(stage.name, Txt.bodyBold); Muted(stage.blurb, style = Txt.small)
            }
        }
        Card {
            Tx("Очки роста: ${p.pet.growth}", Txt.bodyBold)
            if (toNext != null && nextFrom != null) {
                Meter(p.pet.growth - from, nextFrom - from, "До следующей стадии", kind = MeterKind.Ok, text = "ещё $toNext")
                Muted("Ещё ${pointsText(toNext)} до стадии «$nextName». Очки не пропадают.")
            } else Muted("Это самая большая стадия. Питомец вырос!")
            StageTrack(p)
        }
        Card {
            Tx("Как растёт питомец", Txt.bodyBold)
            HowTo("bowl", "Нужное — до 3 очков.", " Купи и еду, и уход за неделю.")
            HowTo("clip", "План — до 3 очков.", " Не превышай план на нужное и на желаемое, отложи, сколько задумал.")
            HowTo("jar", "Копилка — до 2 очков.", " Откладывай каждую неделю.")
        }
        if (last != null) Card {
            Tx("Итоги недели ${last.index}", Txt.bodyBold)
            Tx(last.summary)
            PlanFactRows(last)
            FButton("Все подробности", { Nav.go("/review") }, block = true, variant = BtnVariant.Secondary)
        } else Card(tint = Tint.Gold) {
            Tx2("Итоги появятся после первой недели. ", "Составь план, сделай покупки и заверши неделю.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tx("Задания: $doneTasks из ${content.tasks.size}", Txt.h3)
            for (t in content.topics) Card {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(t.icon, 36.dp); Tx(t.title, Txt.bodyBold) }
                for (x in content.tasks.filter { it.topic == t.id }) Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Tx(x.title, modifier = Modifier.weight(1f)); StarsRow(p.tasks[x.id]?.stars ?: 0)
                }
            }
        }
        Card {
            Tx("Мечты", Txt.bodyBold)
            Tx("Накоплено: ${coinsText(savingsTotal(p))}. Исполнено мечт: ${p.goals.count { it.done }}.")
            FButton("К копилке", { Nav.go("/goals") }, small = true, variant = BtnVariant.Secondary)
        }
        if (p.history.size > 1) Card {
            Tx("Все недели", Txt.bodyBold)
            for (h in p.history.asReversed()) Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Tx("Неделя ${h.index}: +${pointsText(h.score.total)}"); Muted(content.pet.stages.find { it.id == h.stageAfter }?.name ?: "", style = Txt.small)
            }
        }
        ButtonRow {
            FButton("История монет", { scope.launch { openLedger() } }, variant = BtnVariant.Secondary, icon = "history")
            FButton("Словарик", { Nav.go("/glossary") }, variant = BtnVariant.Secondary, icon = "book")
        }
        Card(onClick = { opsOpen = !opsOpen }) {
            Tx("Последние операции ${if (opsOpen) "−" else "+"}", Txt.bodyBold)
            if (opsOpen) LedgerList(p.ledger.takeLast(8))
        }
    }
}

@Composable
private fun HowTo(icon: String, bold: String, rest: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, 30.dp)
        Column { Tx(bold, Txt.bodyBold); Tx(rest.trim()) }
    }
}

@Composable
fun ReviewScreen() {
    val p = Game.profile ?: return
    val r = p.history.lastOrNull()
    val pl = pal
    if (r == null) {
        Screen("Итоги недели") {
            Card(tint = Tint.Gold) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Icon("trophy", 64.dp) }
                Tx("Итогов пока нет", Txt.bodyBold, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Tx("Составь план, сделай покупки и заверши неделю — тогда я покажу, что получилось.", align = TextAlign.Center)
                FButton("К плану", { Nav.go("/plan") })
            }
        }
        return
    }
    val up = r.stageAfter > r.stageBefore
    val stage = Game.content.pet.stages.first { it.id == r.stageAfter }
    Box(Modifier.fillMaxSize()) {
        Screen(
            "Итоги недели ${r.index}", hint = "Вот что получилось и почему питомец растёт.",
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FButton("На главный", { Nav.reset("/") }, Modifier.weight(1f), variant = BtnVariant.Secondary)
                    FButton("К плану", { Nav.replace("/plan") }, Modifier.weight(1f), icon = "clip")
                }
            },
        ) {
            val shape = RoundedCornerShape(22.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Brush.verticalGradient(listOf(Color.Black, Color.Black)) else Brush.radialGradient(listOf(Color(if (up) 0x80FFC93C else 0x47FFC93C), Color(0x14FFC93C)), radius = 700f)).border(if (pl.highContrast) 3.dp else 1.dp, if (pl.highContrast) Color.White else pl.gold.copy(alpha = 0.5f), shape).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                    PetView(p.pet.appearance, r.stageAfter, Modifier.height(190.dp), stats = r.statsAfter, label = "${p.pet.name}: ${stage.name}")
                }
                Tx(if (up) "Новая стадия: ${stage.name}!" else stage.name, Txt.h2, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Tx(r.summary, Txt.lead, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Meter(r.score.total, 8, "Очки роста за неделю", kind = MeterKind.Ok, text = "+${r.score.total} из 8")
            }
            Card { Tx("План и факт", Txt.bodyBold); PlanFactRows(r) }
            Card {
                Tx("Почему столько очков", Txt.bodyBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (l in r.lines) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            val (fg, bg) = when (l.status) { ReviewStatus.Ok -> pl.save to pl.saveBg; ReviewStatus.Part -> pl.warn to pl.warnBg; else -> pl.ink to Color.White.copy(alpha = 0.16f) }
                            Box(Modifier.size(30.dp).clip(CircleShape).background(bg).border(1.dp, fg.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(if (l.status == ReviewStatus.Ok) "check" else if (l.status == ReviewStatus.Part) "info" else "sparkle", 22.dp, tint = fg)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Tx(l.text)
                                l.tip?.let { tip -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) { Icon("bulb", 18.dp, tint = pl.warn); Tx(tip, Txt.small.copy(fontWeight = FontWeight.SemiBold), color = pl.warn) } }
                            }
                            Tx(if ((l.points ?: 0) > 0) "+${l.points}" else "0", Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold), align = TextAlign.End, modifier = Modifier.widthIn(min = 20.dp))
                        }
                    }
                }
            }
            Card {
                Tx("Питомец после недели", Txt.bodyBold)
                for (s in listOf(PetStat.Satiety, PetStat.Care, PetStat.Mood)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(STAT_ICON.getValue(s), 30.dp)
                    Tx2("${STAT_LABEL.getValue(s)}: ${r.statsBefore[s]} → ${r.statsAfter[s]} ", "(${signed(r.statsAfter[s] - r.statsBefore[s])})", modifier = Modifier.weight(1f))
                }
                Muted("Каждую неделю показатели чуть снижаются. Это нормально — заботься о питомце снова.")
            }
        }
        if (up && Game.app.settings.animations) Confetti()
    }
}
