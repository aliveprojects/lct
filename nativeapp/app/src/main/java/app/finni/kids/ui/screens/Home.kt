package app.finni.kids.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.app.present
import app.finni.kids.domain.Mode
import app.finni.kids.domain.STATS
import app.finni.kids.domain.STAT_LABEL
import app.finni.kids.domain.TextScale
import app.finni.kids.domain.activeGoal
import app.finni.kids.domain.canClaimDaily
import app.finni.kids.domain.claimDaily
import app.finni.kids.domain.coinsText
import app.finni.kids.domain.ExpressionCode
import app.finni.kids.domain.nextActiveTask
import app.finni.kids.domain.petExpression
import app.finni.kids.domain.resolveEvent
import app.finni.kids.domain.savingsTotal
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.IconBtn
import app.finni.kids.ui.Meter
import app.finni.kids.ui.MeterKind
import app.finni.kids.ui.STAT_ICON
import app.finni.kids.ui.STAT_SHORT
import app.finni.kids.ui.Tx
import app.finni.kids.ui.pet.PetOf
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal
import kotlinx.coroutines.launch

private class NavItem(val route: String, val icon: String, val label: String, val aria: String)

private val NAV = listOf(
    NavItem("/plan", "clip", "План", "План личного бюджета"),
    NavItem("/tasks", "star", "Задания", "Задания"),
    NavItem("/shop", "bag", "Магазин", "Покупки"),
    NavItem("/goals", "jar", "Копилка", "Накопления и цели"),
    NavItem("/progress", "trophy", "Успехи", "Прогресс питомца"),
)

@Composable
fun HomeScreen() {
    val app = Game.app
    val p = (if (app.mode == Mode.Demo) app.demo else app.main) ?: return
    val scope = Ui.scope
    val pl = pal
    val ctx = Game.ctx
    val content = Game.content
    val expr = petExpression(p.pet.stats)
    val goal = activeGoal(p)
    val task = nextActiveTask(p, ctx)
    val gift = canClaimDaily(p, ctx)
    val event = p.pendingEventId?.let { id -> content.events.find { it.id == id } }
    val stage = content.pet.stages.first { it.id == p.pet.stage }
    val bubble = p.pet.reason?.text ?: expr.hint
    val needPlan = !p.period.planConfirmed
    val bigText = app.settings.textScale != TextScale.Normal

    val body: @Composable ColumnScope.(Modifier) -> Unit = { petMod ->
        // верхняя строка: кошелёк, копилка, замок, настройки, справка
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("coin", p.wallet.toString(), "Баланс: ${coinsText(p.wallet)}. Открыть историю", pl.gold.copy(alpha = 0.18f), pl.gold.copy(alpha = 0.6f)) { scope.launch { openLedger() } }
            Chip("jar", savingsTotal(p).toString(), "Накопления: ${coinsText(savingsTotal(p))}", Color(0x2EFF82BE), Color(0x8CFF96C8)) { Nav.go("/goals") }
            Spacer(Modifier.weight(1f))
            IconBtn("lock", "Раздел для взрослого", { Nav.go("/adult") })
            IconBtn("settings", "Настройки", { scope.launch { openSettings() } })
            IconBtn("help", "Подсказка: как играть", { scope.launch { openHelp() } })
        }

        if (app.mode == Mode.Demo) {
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(Color(0xCC060928)).border(1.5.dp, pl.gold, shape).clickable(role = Role.Button) { scope.launch { openDemoPanel(scope) } }.heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon("key", 22.dp)
                Tx("Демо-режим · неделя ${p.period.index}", Txt.bodyBold.copy(fontWeight = FontWeight.SemiBold), color = Color.White, modifier = Modifier.weight(1f))
                Tx("Панель проверки", Txt.bodyBold, color = pl.gold)
            }
        }

        if (gift || event != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (event != null) Banner("vet", event.title, event.short, "Помочь", Color(0x2BFFC93C), Color(0x99FFC93C), Color(0xFFFFF1C9)) {
                    scope.launch { present(Game.act { pp, c -> resolveEvent(pp, c) }) }
                }
                if (gift) Banner("gift", "Подарок дня", "+5 монет", "Забрать", Color(0x33FF82BE), Color(0x99FF96C8), Color(0xFFFFE6F1)) {
                    scope.launch { present(Game.act { pp, c -> claimDaily(pp, c) }) }
                }
            }
        }

        PetScene(petMod, bubble, onBubble = { scope.launch { openPetFeelings() } }, name = { PetName(p.pet.name, stage.name, expr.label, expr.code) }) {
            PetOf(p, Modifier.fillMaxHeight().widthIn(max = 280.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!bigText) {
                for (s in STATS) StatBox(s, p.pet.stats[s], Modifier.weight(1f))
            }
        }
        if (bigText) for (s in STATS) StatBox(s, p.pet.stats[s], Modifier.fillMaxWidth(), wide = true)

        if (goal != null) {
            GoalCard(goal.icon, "Мечта: ${goal.title}") {
                Meter(goal.saved, goal.cost, "Накоплено на «${goal.title}»", kind = MeterKind.Ok, text = "${goal.saved} из ${goal.cost}")
            }
        } else {
            GoalCard("star", "Выбери новую мечту") { Tx("Нажми, чтобы выбрать цель для копилки", Txt.small, color = pl.ink2) }
        }

        when {
            needPlan -> TaskCard("clip", "План недели ${p.period.index}", "Раздели ${coinsText(p.wallet)}", Color(0x2BFFC93C), Color(0x99FFC93C), "К плану", BtnVariant.Primary) { Nav.go("/plan") }
            task != null -> TaskCard("star", "Задание: ${task.title}", "до ${coinsText(task.rewards.three)}", pl.mustBg, pl.must.copy(alpha = 0.5f), "Играть", BtnVariant.Primary) { Nav.go("/task/${task.id}") }
            else -> TaskCard("trophy", "Все задания освоены!", "Повтори любое для практики", pl.mustBg, pl.must.copy(alpha = 0.5f), "Задания", BtnVariant.Secondary) { Nav.go("/tasks") }
        }
    }

    val tabs: @Composable () -> Unit = {
        val cols = if (bigText) 3 else 5
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            NAV.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { n -> Tab(n, needPlan && n.route == "/plan", Modifier.weight(1f)) }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        if (bigText) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                body(Modifier.height(190.dp))
                tabs()
            }
        } else {
            Column(Modifier.weight(1f).padding(horizontal = 14.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                body(Modifier.weight(1f).heightIn(min = 120.dp))
            }
            Box(Modifier.padding(horizontal = 14.dp)) { tabs() }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(8.dp))
    }
}

@Composable
private fun Chip(icon: String, text: String, label: String, bg: Color, border: Color, onClick: () -> Unit) {
    Row(
        Modifier.heightIn(min = 48.dp).clip(CircleShape).background(bg).border(1.5.dp, if (pal.highContrast) Color.White else border, CircleShape)
            .semantics { contentDescription = label }.clickable(role = Role.Button) { onClick() }.padding(start = 5.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, 28.dp)
        Tx(text, Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold, fontSize = Txt.bodyBold.fontSize * 1.02f))
    }
}

@Composable
private fun Banner(icon: String, title: String, sub: String, action: String, bg: Color, border: Color, fg: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg).border(1.5.dp, if (pal.highContrast) Color.White else border, shape).clickable(role = Role.Button) { onClick() }.heightIn(min = 50.dp).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, 34.dp)
        Column(Modifier.weight(1f)) {
            Tx(title, Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 0.95f), color = fg, maxLines = 1)
            Tx(sub, Txt.small.copy(fontSize = Txt.small.fontSize * 0.95f), color = fg, maxLines = 1)
        }
        Box(Modifier.clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFFE07A), Color(0xFFFFAB1F)))).padding(horizontal = 14.dp, vertical = 8.dp)) {
            Tx(action, Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF2E1D00))
        }
    }
}

/** Сцена питомца: небо, планета под лапами, облачко с репликой. */
@Composable
private fun PetScene(modifier: Modifier, bubble: String, onBubble: () -> Unit, name: @Composable () -> Unit, pet: @Composable () -> Unit) {
    val pl = pal
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(if (pl.highContrast) Brush.verticalGradient(listOf(Color.Black, Color.Black)) else Brush.verticalGradient(listOf(Color(0xFF171C67), Color(0xFF0C1043))))
            .border(if (pl.highContrast) 3.dp else 1.dp, pl.line, shape),
    ) {
        if (!pl.highContrast) Canvas(Modifier.fillMaxSize()) {
            // планета: широкий овал внизу
            val w = size.width * 1.6f
            val top = size.height * 0.82f
            val topLeft = Offset(-(w - size.width) / 2f, top)
            val sz = Size(w, size.height * 1.04f)
            drawOval(
                Brush.radialGradient(0f to Color(0xFF7A63FF), 0.38f to Color(0xFF4739C9), 0.74f to Color(0xFF1C1A78), center = Offset(size.width / 2f, top), radius = w / 2f),
                topLeft, sz,
            )
        }
        Row(Modifier.fillMaxSize().padding(top = 6.dp)) {
            Spacer(Modifier.weight(0.42f))
            Box(Modifier.weight(0.58f).fillMaxHeight().padding(bottom = 30.dp), contentAlignment = Alignment.Center) { pet() }
        }
        Box(
            Modifier.padding(10.dp).widthIn(max = 170.dp).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)).background(if (pl.highContrast) Color.Black else Color(0xFFF4F6FF))
                .then(if (pl.highContrast) Modifier.border(3.dp, Color.White, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)) else Modifier)
                .semantics { contentDescription = "Почему питомец так себя чувствует" }.clickable(role = Role.Button) { onBubble() }.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Tx(bubble, Txt.small.copy(fontSize = Txt.small.fontSize * 0.98f, fontWeight = FontWeight.SemiBold, lineHeight = Txt.small.fontSize * 1.25f), color = if (pl.highContrast) Color.White else Color(0xFF0E1240), maxLines = 3)
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) { name() }
    }
}

@Composable
private fun PetName(name: String, stage: String, mood: String, code: ExpressionCode) {
    val pl = pal
    val sad = code == ExpressionCode.Hungry || code == ExpressionCode.Dirty || code == ExpressionCode.Bored
    Row(Modifier.fillMaxWidth().padding(top = 0.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Tx(name, Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold, fontSize = Txt.bodyBold.fontSize * 1.05f))
        Tx(stage, Txt.body.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFFE9EDFF))
        Box(
            Modifier.clip(CircleShape).background(if (sad) Color(0xFF4A2338) else Color(0xFF12365A))
                .border(1.dp, if (sad) Color(0xB3FFAB8C) else Color(0x9978F4B8), CircleShape).padding(horizontal = 12.dp, vertical = 3.dp),
        ) { Tx(mood, Txt.smallBold.copy(fontSize = Txt.smallBold.fontSize * 0.98f), color = if (sad) Color(0xFFFFC0A8) else pl.save) }
    }
}

@Composable
private fun StatBox(stat: app.finni.kids.domain.PetStat, value: Int, modifier: Modifier, wide: Boolean = false) {
    val pl = pal
    val shape = RoundedCornerShape(16.dp)
    val meter = @Composable { m: Modifier -> Meter(value, label = STAT_LABEL.getValue(stat), modifier = m, kind = if (value < 40) MeterKind.Want else MeterKind.Ok, text = value.toString()) }
    if (wide) {
        Row(modifier.clip(shape).background(if (pl.highContrast) Color.Black else pl.surface).border(1.dp, pl.line, shape).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(STAT_ICON.getValue(stat), 22.dp)
            Tx(STAT_SHORT.getValue(stat), Txt.bodyBold, modifier = Modifier.widthIn(min = 120.dp))
            meter(Modifier.weight(1f))
        }
    } else {
        Column(modifier.clip(shape).background(if (pl.highContrast) Color.Black else pl.surface).border(1.dp, pl.line, shape).padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(STAT_ICON.getValue(stat), 22.dp)
                Tx(STAT_SHORT.getValue(stat), Txt.tiny.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp), maxLines = 1)
            }
            meter(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun GoalCard(icon: String, title: String, content: @Composable () -> Unit) {
    val pl = pal
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Color.Black else pl.saveBg).border(if (pl.highContrast) 3.dp else 1.dp, if (pl.highContrast) Color.White else pl.save.copy(alpha = 0.5f), shape)
            .clickable(role = Role.Button) { Nav.go("/goals") }.heightIn(min = 60.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Tx(title, Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 0.96f))
            content()
        }
    }
}

@Composable
private fun TaskCard(icon: String, title: String, sub: String, bg: Color, border: Color, action: String, variant: BtnVariant, onClick: () -> Unit) {
    val pl = pal
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (pl.highContrast) Color.Black else bg).border(if (pl.highContrast) 3.dp else 1.dp, if (pl.highContrast) Color.White else border, shape).heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, 40.dp)
        Column(Modifier.weight(1f)) {
            Tx(title, Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 0.96f))
            Tx(sub, Txt.small, color = pl.ink2)
        }
        FButton(action, onClick, variant = variant, small = true)
    }
}

@Composable
private fun Tab(n: NavItem, dot: Boolean, modifier: Modifier) {
    val pl = pal
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.heightIn(min = 60.dp).clip(shape).background(pl.surface2).border(1.5.dp, pl.line, shape)
            .semantics { contentDescription = n.aria }.clickable(role = Role.Button) { Nav.go(n.route) }.padding(horizontal = 1.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(n.icon, 34.dp)
            Tx(n.label, Txt.tiny.copy(fontWeight = FontWeight.Bold, fontSize = Txt.tiny.fontSize * 0.98f), maxLines = 1)
        }
        if (dot) Box(Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 8.dp).background(Color(0xFFFF7A59), CircleShape).border(2.dp, pl.sky1, CircleShape).height(12.dp).widthIn(min = 12.dp))
    }
}
