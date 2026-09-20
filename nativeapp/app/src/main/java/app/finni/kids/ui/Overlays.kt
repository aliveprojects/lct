package app.finni.kids.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Overlay
import app.finni.kids.app.SheetTone
import app.finni.kids.app.Ui
import app.finni.kids.domain.ChangeKind
import app.finni.kids.domain.Tone
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal
import kotlin.math.cos
import kotlin.math.sin

/** Окно снизу: затемнение, «ручка», заголовок и содержимое. Нажатие вне окна закрывает его. */
@Composable
fun SheetFrame(title: String?, onClose: () -> Unit, tone: SheetTone = SheetTone.Normal, actions: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = pal
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.92f).dp
    val top = when (tone) {
        SheetTone.Good -> Color(0xFF155C66)
        SheetTone.Care -> Color(0xFF6A4A1A)
        SheetTone.Normal -> p.sheetTop
    }
    Box(
        Modifier.fillMaxSize().background(Color(0xB803051A)).pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(shown, enter = slideInVertically(tween(220)) { it / 4 } + fadeIn(tween(200))) {
            val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxH)
                    .background(if (p.highContrast) Brush.verticalGradient(listOf(Color.Black, Color.Black)) else Brush.verticalGradient(0f to top, 0.22f to Color(0xFF141A5E), 1f to Color(0xFF101450)), shape)
                    .border(if (p.highContrast) 3.dp else 1.dp, p.line, shape)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .semantics { if (title != null) contentDescription = title }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding(),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 10.dp).height(5.dp).widthIn(min = 44.dp).background(Color.White.copy(alpha = 0.35f), CircleShape))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = if (actions == null) 18.dp else 8.dp)) {
                    if (title != null) Tx(title, Txt.h2, modifier = Modifier.padding(bottom = 12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                }
                if (actions != null) Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = actions)
            }
        }
    }
}

@Composable
fun OverlayHost() {
    for (o in Ui.stack.toList()) {
        androidx.compose.runtime.key(o.id) {
            when (o) {
                is Overlay.ReportView -> ReportSheet(o)
                is Overlay.Confirm -> {
                    val opts = o.opts
                    val stacked = maxOf(opts.cancelLabel.length, opts.confirmLabel.length) > 10
                    SheetFrame(
                        opts.title, onClose = { o.done.complete(false) }, tone = if (opts.careful) SheetTone.Care else SheetTone.Normal,
                        actions = {
                            val confirmBtn = @Composable { m: Modifier -> FButton(opts.confirmLabel, { o.done.complete(true) }, m, variant = if (opts.careful) BtnVariant.Gold else BtnVariant.Primary) }
                            val cancelBtn = @Composable { m: Modifier -> FButton(opts.cancelLabel, { o.done.complete(false) }, m, variant = BtnVariant.Secondary) }
                            if (stacked) {
                                confirmBtn(Modifier.fillMaxWidth())
                                cancelBtn(Modifier.fillMaxWidth())
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    cancelBtn(Modifier.weight(1f))
                                    confirmBtn(Modifier.weight(1f))
                                }
                            }
                        },
                    ) {
                        if (opts.icon != null) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Icon(opts.icon, 56.dp) }
                        opts.body?.invoke(this)
                    }
                }
                is Overlay.Sheet -> SheetFrame(
                    o.title, onClose = { o.done.complete(Unit) }, tone = o.tone,
                    actions = o.actions?.let { a -> { a(this) { o.done.complete(Unit) } } },
                ) {
                    o.content(this) { o.done.complete(Unit) }
                }
            }
        }
    }
    Ui.toast?.let { t ->
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(16.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.widthIn(max = 528.dp).fillMaxWidth().background(Color(0xFFF4F6FF), RoundedCornerShape(16.dp)).padding(16.dp, 14.dp)
                    .semantics { contentDescription = t.text },
            ) { Tx(t.text, Txt.bodyBold, color = Color(0xFF0E1240)) }
        }
    }
}

@Composable
private fun ReportSheet(o: Overlay.ReportView) {
    val report = o.report
    val celebrate = report.tone == Tone.Good && Game.app.settings.animations &&
        report.changes.any { it.kind == ChangeKind.Growth || it.kind == ChangeKind.Savings || (it.kind == ChangeKind.Wallet && it.delta > 0) }
    Box {
        val next = report.next
        SheetFrame(
            report.title, onClose = { o.done.complete(Unit) }, tone = when (report.tone) { Tone.Good -> SheetTone.Good; Tone.Care -> SheetTone.Care; else -> SheetTone.Normal },
            actions = {
                if (next != null) FButton(next.label, { o.done.complete(Unit); Nav.go(next.route) }, block = true)
                FButton("Понятно", { o.done.complete(Unit) }, block = true, variant = if (next != null) BtnVariant.Secondary else BtnVariant.Primary)
            },
        ) {
            ChangeList(report.changes)
            Explain(report.explain)
        }
        if (celebrate) Confetti()
    }
}

private val CONFETTI = listOf(Color(0xFFFFC93C), Color(0xFFFF8FC0), Color(0xFF63F3E0), Color(0xFFB79BFF))

/** Звёздочки-конфетти: падают один раз сверху вниз. */
@Composable
fun Confetti() {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1800, easing = LinearEasing)) }
    if (t.value >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val delays = floatArrayOf(0f, .15f, .05f, .3f, .1f, .25f, .02f, .2f, .12f, .35f, .08f, .22f, .4f, .45f, .38f, .5f)
        for (i in 0 until 16) {
            val local = ((t.value * 2.3f - delays[i]) / 1.0f).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val x = size.width * (0.06f + 0.058f * ((i * 7) % 16))
            val y = -10f + local * size.height * 0.7f
            val r = 5.5.dp.toPx()
            val rot = local * 9.4f
            val path = Path()
            for (k in 0 until 8) {
                val rad = if (k % 2 == 0) r else r * 0.4f
                val a = Math.PI.toFloat() / 4f * k + rot
                val px = x + cos(a) * rad
                val py = y + sin(a) * rad
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(path, CONFETTI[i % 4].copy(alpha = 1f - local * 0.9f))
        }
    }
}

