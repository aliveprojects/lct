package app.finni.kids.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Nav
import app.finni.kids.domain.Change
import app.finni.kids.domain.ChangeKind
import app.finni.kids.domain.Direction
import app.finni.kids.domain.PetStat
import app.finni.kids.platform.Beep
import app.finni.kids.platform.Sound
import app.finni.kids.ui.screens.openHelp
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal
import kotlinx.coroutines.launch

const val MINUS = "−"

fun signed(n: Int): String = if (n > 0) "+$n" else if (n < 0) "$MINUS${Math.abs(n)}" else "0"

val STAT_ICON = mapOf(PetStat.Satiety to "bowl", PetStat.Care to "bath", PetStat.Mood to "smile")
val STAT_SHORT = mapOf(PetStat.Satiety to "Сытость", PetStat.Care to "Чистота", PetStat.Mood to "Настроение")
val DIR_ICON = mapOf(Direction.Must to "bowl", Direction.Want to "ball", Direction.Save to "jar")
val DIR_NAME = mapOf(Direction.Must to "Нужное", Direction.Want to "Хочется", Direction.Save to "Копилка")

// ---------- Текст ----------

@Composable
fun Tx(
    text: String,
    style: TextStyle = Txt.body,
    modifier: Modifier = Modifier,
    color: Color = pal.ink,
    align: TextAlign? = null,
    underline: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    weight: FontWeight? = null,
) {
    Text(
        text = text, modifier = modifier, color = color, textAlign = align, maxLines = maxLines,
        style = style.let { if (weight != null) it.copy(fontWeight = weight) else it },
        textDecoration = if (underline) TextDecoration.Underline else null,
    )
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, style: TextStyle = Txt.body, align: TextAlign? = null) =
    Tx(text, style, modifier, pal.ink2, align)

fun Modifier.noRipple(enabled: Boolean = true, role: Role? = null, onClick: () -> Unit): Modifier =
    clickable(interactionSource = null, indication = null, enabled = enabled, role = role, onClick = onClick)

// ---------- Кнопки ----------

enum class BtnVariant { Primary, Secondary, Ghost, Good, Gold }

@Composable
fun FButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BtnVariant = BtnVariant.Primary,
    icon: String? = null,
    block: Boolean = false,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val p = pal
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val hc = p.highContrast
    val raised = variant == BtnVariant.Primary || variant == BtnVariant.Good || variant == BtnVariant.Gold
    val brush: Brush = when {
        variant == BtnVariant.Ghost -> SolidColor(Color.Transparent)
        hc && raised -> SolidColor(p.primary)
        variant == BtnVariant.Primary -> Brush.linearGradient(listOf(Color(0xFF63F3E0), Color(0xFF3EA6FF)))
        variant == BtnVariant.Good -> Brush.linearGradient(listOf(Color(0xFF7BF6BD), Color(0xFF2FD08C)))
        variant == BtnVariant.Gold -> Brush.linearGradient(listOf(Color(0xFFFFE07A), Color(0xFFFFAB1F)))
        else -> SolidColor(if (hc) Color.Black else Color.White.copy(alpha = 0.06f))
    }
    val textColor = when {
        variant == BtnVariant.Ghost -> p.ink
        variant == BtnVariant.Secondary -> p.btnText2
        variant == BtnVariant.Gold && !hc -> p.goldInk
        else -> p.primaryInk
    }
    val shadow = when (variant) {
        BtnVariant.Primary -> Color(0x8C031446)
        BtnVariant.Good -> Color(0x99033C28)
        BtnVariant.Gold -> Color(0x995A3200)
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .then(if (block) Modifier.fillMaxWidth() else Modifier)
            .padding(bottom = if (raised && !hc) 4.dp else 0.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .graphicsLayer { translationY = if (pressed && enabled && raised) 3.dp.toPx() else 0f }
            .drawBehind {
                if (raised && !hc) drawRoundRect(shadow, Offset(0f, 4.dp.toPx()), size, CornerRadius(size.height / 2))
            }
            .clip(shape)
            .background(brush)
            .then(if (variant == BtnVariant.Secondary) Modifier.border(2.5.dp, p.primary, shape) else Modifier)
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button) {
                Sound.play(Beep.Tap)
                onClick()
            }
            .heightIn(min = if (small) 48.dp else 56.dp)
            .padding(horizontal = if (small) 14.dp else 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, if (small) 22.dp else 26.dp, tint = textColor)
            Tx(text, if (small) Txt.button.copy(fontSize = Txt.button.fontSize * 0.94f) else Txt.button, color = textColor, align = TextAlign.Center, underline = variant == BtnVariant.Ghost, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
fun IconBtn(icon: String, label: String, onClick: () -> Unit, badge: Boolean = false, modifier: Modifier = Modifier) {
    val p = pal
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(p.surface2)
            .border(1.5.dp, p.line, CircleShape)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button) { Sound.play(Beep.Tap); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, 26.dp)
        if (badge) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(11.dp).clip(CircleShape).background(Color(0xFFFF7A59)).border(2.dp, p.sky1, CircleShape))
    }
}

// ---------- Деньги и метки ----------

@Composable
fun Coin(n: Int, big: Boolean = false, color: Color = pal.ink) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon("coin", if (big) 30.dp else 22.dp)
        Tx(n.toString(), if (big) Txt.big else Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold), color = color)
    }
}

@Composable
fun DirTag(dir: Direction, text: String? = null) {
    val p = pal
    val (fg, bg) = when (dir) {
        Direction.Must -> p.must to p.mustBg
        Direction.Want -> p.want to p.wantBg
        Direction.Save -> p.save to p.saveBg
    }
    Row(
        Modifier.clip(CircleShape).background(bg).border(1.dp, fg.copy(alpha = 0.4f), CircleShape).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(DIR_ICON.getValue(dir), 18.dp)
        Tx(text ?: DIR_NAME.getValue(dir), Txt.smallBold, color = fg)
    }
}

// ---------- Карточки ----------

enum class Tint { Plain, Gold, Warn, Save, Must, Want, Primary }

@Composable
fun Card(
    modifier: Modifier = Modifier,
    tint: Tint = Tint.Plain,
    gap: Dp = 8.dp,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = pal
    val (bg, border) = when (tint) {
        Tint.Plain -> p.surface to p.line
        Tint.Gold -> p.goldBg to p.gold.copy(alpha = 0.45f)
        Tint.Warn -> p.warnBg to p.warn.copy(alpha = 0.4f)
        Tint.Save -> p.saveBg to p.save.copy(alpha = 0.4f)
        Tint.Must -> p.mustBg to p.must.copy(alpha = 0.5f)
        Tint.Want -> p.wantBg to p.want.copy(alpha = 0.5f)
        Tint.Primary -> p.primarySoft to p.primary.copy(alpha = 0.35f)
    }
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (p.highContrast) Color.Black else bg)
            .border(if (p.highContrast) 3.dp else 1.dp, if (p.highContrast) Color.White else border, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button) { Sound.play(Beep.Tap); onClick() } else Modifier)
            .padding(horizontal = padding + 2.dp, vertical = padding),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** Жёлтая подсказка-предупреждение. */
@Composable
fun NoteBox(text: String, modifier: Modifier = Modifier) {
    val p = pal
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(p.warnBg).border(1.dp, p.warn.copy(alpha = 0.35f), shape).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon("info", 24.dp, tint = p.warn)
        Tx(text, Txt.bodyBold.copy(fontWeight = FontWeight.SemiBold), color = p.warn, modifier = Modifier.weight(1f))
    }
}

/** Бирюзовый блок «понятное объяснение». */
@Composable
fun Explain(text: String, modifier: Modifier = Modifier, icon: String = "bulb") {
    val p = pal
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(p.primarySoft).border(1.dp, p.primary.copy(alpha = 0.35f), shape).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, 26.dp, tint = p.primary)
        Tx(text, Txt.bodyBold.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
    }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { Sound.play(Beep.Tap); onClick() }.padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Tx(text, Txt.bodyBold, color = pal.primary, underline = true)
    }
}

// ---------- Показатели ----------

enum class MeterKind { Primary, Ok, Must, Want }

@Composable
fun Meter(
    value: Int,
    max: Int = 100,
    label: String,
    modifier: Modifier = Modifier,
    kind: MeterKind = MeterKind.Primary,
    showNumbers: Boolean = true,
    text: String? = null,
) {
    val p = pal
    val pct = (value.toFloat() / max).coerceIn(0f, 1f)
    val colors = when (kind) {
        MeterKind.Primary -> listOf(Color(0xFF4EECDA), Color(0xFF4AA8FF))
        MeterKind.Ok -> listOf(Color(0xFF5CF0B0), Color(0xFFB3F76A))
        MeterKind.Must -> listOf(Color(0xFF6AA6FF), Color(0xFF9FC8FF))
        MeterKind.Want -> listOf(Color(0xFFFF9A70), Color(0xFFFFC38C))
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.weight(1f).height(14.dp).clip(CircleShape).background(if (p.highContrast) Color.Black else Color.White.copy(alpha = 0.15f))
                .then(if (p.highContrast) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                .semantics { contentDescription = "$label: $value из $max" },
        ) {
            if (pct > 0f) Box(Modifier.fillMaxSize().fillMaxWidthFraction(pct).clip(CircleShape).background(if (p.highContrast) SolidColor(p.primary) else Brush.horizontalGradient(colors)))
        }
        if (showNumbers) Tx(text ?: "$value/$max", Txt.smallBold.copy(fontSize = Txt.smallBold.fontSize * 0.96f))
    }
}

private fun Modifier.fillMaxWidthFraction(f: Float): Modifier = this.then(Modifier.fillMaxWidth(f))

@Composable
fun Stepper(value: Int, max: Int, onChange: (Int) -> Unit, label: String, modifier: Modifier = Modifier, min: Int = 0, step: Int = 5, unit: String? = null) {
    Row(modifier.fillMaxWidth().semantics { contentDescription = "$label: $value" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepBtn("minus", "Меньше: $label", value > min) { onChange(maxOf(min, value - step)) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Tx(value.toString(), Txt.big.copy(fontSize = Txt.big.fontSize * 1.05f))
            if (unit != null) Tx(unit, Txt.small, color = pal.ink2, modifier = Modifier.padding(start = 4.dp, bottom = 5.dp))
        }
        StepBtn("plus", "Больше: $label", value < max) { onChange(minOf(max, value + step)) }
    }
}

@Composable
private fun StepBtn(icon: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val p = pal
    Box(
        Modifier.size(56.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape).background(if (p.highContrast) Color.Black else p.primary.copy(alpha = 0.1f))
            .border(2.5.dp, p.primary, CircleShape).semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button) { Sound.play(Beep.Tap); onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, 26.dp, tint = p.btnText2) }
}

data class SegOpt<T>(val id: T, val label: String, val icon: String? = null)

@Composable
fun <T> Segmented(value: T, options: List<SegOpt<T>>, onChange: (T) -> Unit, modifier: Modifier = Modifier, columns: Int = options.size) {
    val p = pal
    val outer = RoundedCornerShape(28.dp)
    Column(
        modifier.fillMaxWidth().clip(outer).background(Color.White.copy(alpha = 0.09f)).border(1.dp, p.line, outer).padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { o ->
                    val on = o.id == value
                    val shape = RoundedCornerShape(22.dp)
                    Row(
                        Modifier.weight(1f).heightIn(min = 48.dp).clip(shape)
                            .then(if (on) Modifier.background(if (p.highContrast) SolidColor(p.primary) else Brush.linearGradient(listOf(Color(0xFF63F3E0), Color(0xFF3EA6FF)))) else Modifier)
                            .semantics { contentDescription = o.label + if (on) ", выбрано" else "" }
                            .clickable(role = Role.RadioButton) { Sound.play(Beep.Tap); onChange(o.id) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (o.icon != null) Icon(o.icon, 22.dp, tint = if (on) p.primaryInk else p.ink)
                        Tx(o.label, Txt.bodyBold.copy(fontSize = if (columns >= 3) Txt.bodyBold.fontSize * 0.86f else Txt.bodyBold.fontSize, lineHeight = Txt.bodyBold.fontSize * 1.15f), color = if (on) p.primaryInk else p.ink, align = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
                    }
                }
                // выравниваем неполный ряд
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null, modifier: Modifier = Modifier) {
    val p = pal
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).background(if (p.highContrast) Color.Black else p.surface).border(1.dp, p.line, shape)
            .semantics { contentDescription = "$label: ${if (checked) "включено" else "выключено"}" }
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Tx(label, Txt.bodyBold)
            if (hint != null) Tx(hint, Txt.small, color = p.ink2)
        }
        Text(if (checked) "Вкл" else "Выкл", Modifier.widthIn2(46.dp), color = p.ink, textAlign = TextAlign.End, maxLines = 1, softWrap = false, style = Txt.bodyBold)
        Box(
            Modifier.size(52.dp, 30.dp).clip(CircleShape).background(if (checked) p.primary else Color.White.copy(alpha = 0.28f)).padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) { Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White)) }
    }
}

private fun Modifier.widthIn2(w: Dp): Modifier = this.then(Modifier.width(w))

// ---------- Каркас экрана ----------

/** Единый каркас: кнопка «назад» всегда слева, подсказка «?» — справа. */
@Composable
fun Screen(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    back: Boolean = true,
    help: Boolean = true,
    right: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = app.finni.kids.app.Ui.scope
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (back) IconBtn("back", "Назад", { if (!Nav.back()) Nav.reset("/") }) else Spacer(Modifier.width(12.dp))
            Tx(title, Txt.topbar, modifier = Modifier.weight(1f))
            right?.invoke(this)
            if (help) IconBtn("help", "Подсказка: как играть", { scope.launch { openHelp() } })
        }
        if (hint != null) Tx(hint, Txt.body, color = pal.ink2, modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 4.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        if (footer != null) {
            Column(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(0f to Color.Transparent, 0.4f to Color(0xF50A0D35), 1f to Color(0xF50A0D35)))
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                content = footer,
            )
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

// ---------- «Что изменилось и почему» ----------

private fun changeIcon(c: Change): String = when {
    c.kind == ChangeKind.Wallet -> "coin"
    c.kind == ChangeKind.Savings -> "jar"
    c.kind == ChangeKind.Growth -> "star"
    c.kind == ChangeKind.Stat && c.stat != null -> STAT_ICON.getValue(c.stat)
    else -> "clip"
}

@Composable
fun ChangeList(changes: List<Change>) {
    if (changes.isEmpty()) return
    val p = pal
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (c in changes) {
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(if (p.highContrast) Color.Black else p.surface).border(1.dp, p.line, shape).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(changeIcon(c), 34.dp)
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Tx(c.label, Txt.bodyBold, modifier = Modifier.weight(1f))
                        val col = if (c.delta > 0) p.save else if (c.delta < 0) p.want else p.ink
                        if (c.kind == ChangeKind.Plan && c.before == null) {
                            Tx(c.delta.toString(), Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (c.before != null && c.after != null) Tx("${c.before} → ${c.after}", Txt.body, color = p.ink2, modifier = Modifier.padding(end = 6.dp))
                                Tx(signed(c.delta), Txt.bodyBold.copy(fontWeight = FontWeight.ExtraBold), color = col)
                            }
                        }
                    }
                    Tx(c.why, Txt.small.copy(fontSize = Txt.small.fontSize * 1.0f), color = p.ink2)
                }
            }
        }
    }
}

@Composable
fun StarsRow(count: Int, of: Int = 3, size: Dp = 22.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.semantics { contentDescription = "Звёзд: $count из $of" }) {
        repeat(of) { i -> Box(Modifier.alpha(if (i < count) 1f else 0.3f)) { Icon("star", size) } }
    }
}


/** Слово с подчёркиванием: по нажатию объясняет термин простыми словами. */
@Composable
fun TermText(id: String, text: String, style: TextStyle = Txt.bodyBold, color: Color = pal.ink) {
    Tx(text, style, color = color, underline = true, modifier = Modifier.clickable(role = Role.Button) { Sound.play(Beep.Tap); app.finni.kids.app.Ui.scope.launch { app.finni.kids.ui.screens.openTerm(id) } })
}

/** Карточка с цветной полосой слева (конверты плана). */
@Composable
fun StripeCard(stripe: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = pal
    val shape = RoundedCornerShape(22.dp)
    Row(modifier.fillMaxWidth().clip(shape).background(if (p.highContrast) Color.Black else p.surface).border(if (p.highContrast) 3.dp else 1.dp, p.line, shape).height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Box(Modifier.width(10.dp).fillMaxSize().background(stripe))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

fun dirColor(p: app.finni.kids.ui.theme.Palette, d: Direction): Color = when (d) { Direction.Must -> p.must; Direction.Want -> p.want; Direction.Save -> p.save }

/** Строка с жирным началом: «Жирное. Обычное продолжение». */
@Composable
fun Tx2(bold: String, rest: String, modifier: Modifier = Modifier, color: Color = pal.ink) {
    val text = androidx.compose.ui.text.buildAnnotatedString {
        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
        append(bold)
        pop()
        append(rest)
    }
    Text(text, modifier, color = color, style = Txt.body)
}

/** Ряд кнопок: если не помещаются — переносятся на следующую строку (слова внутри кнопок не разрываются). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ButtonRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}
