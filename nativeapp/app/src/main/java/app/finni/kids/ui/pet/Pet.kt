package app.finni.kids.ui.pet

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.finni.kids.app.Game
import app.finni.kids.content.ColorDef
import app.finni.kids.domain.ExpressionCode
import app.finni.kids.domain.PetAppearance
import app.finni.kids.domain.Profile
import app.finni.kids.domain.Stats
import app.finni.kids.domain.petExpression
import app.finni.kids.ui.SvgCanvas
import app.finni.kids.ui.Svgs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Питомец рисуется из параметров: вид × цвет × украшение × стадия × выражение.
// Все фигуры — собственные, векторные, без внешних файлов (SVG собирается из текста и рисуется AndroidSVG).

private const val INK = "#2B2540"
private const val PINK = "#F7B8C6"
private val STAGE_SCALE = mapOf(1 to 0.8, 2 to 0.9, 3 to 0.96, 4 to 1.0)

private fun back(species: String, c: ColorDef): String = when (species) {
    "cat" -> """<g><path d="M136 160C170 162 174 128 152 122" stroke="${c.dark}" stroke-width="11" fill="none" stroke-linecap="round"/><path d="M60 68L52 26L94 50Z" fill="${c.body}"/><path d="M65 61L60 38L83 51Z" fill="$PINK"/><path d="M140 68L148 26L106 50Z" fill="${c.body}"/><path d="M135 61L140 38L117 51Z" fill="$PINK"/></g>"""
    "dog" -> """<g><path d="M136 154C156 148 160 132 150 120" stroke="${c.dark}" stroke-width="10" fill="none" stroke-linecap="round"/><ellipse cx="54" cy="94" rx="15" ry="28" fill="${c.dark}" transform="rotate(10 54 94)"/><ellipse cx="146" cy="94" rx="15" ry="28" fill="${c.dark}" transform="rotate(-10 146 94)"/></g>"""
    "bunny" -> """<g><circle cx="140" cy="166" r="11" fill="#fff"/><ellipse cx="76" cy="36" rx="13" ry="35" fill="${c.body}" transform="rotate(-8 76 36)"/><ellipse cx="76" cy="38" rx="6.5" ry="24" fill="$PINK" transform="rotate(-8 76 36)"/><ellipse cx="124" cy="36" rx="13" ry="35" fill="${c.body}" transform="rotate(8 124 36)"/><ellipse cx="124" cy="38" rx="6.5" ry="24" fill="$PINK" transform="rotate(8 124 36)"/></g>"""
    else -> """<g><path d="M134 158C158 158 172 170 178 184C160 184 146 180 130 176Z" fill="${c.body}"/><path d="M150 162l4-9 5 10M162 168l5-8 4 10" fill="${c.dark}"/><path d="M78 56L86 36L95 52Z" fill="${c.dark}"/><path d="M93 50L100 28L107 50Z" fill="${c.dark}"/><path d="M105 52L114 36L122 56Z" fill="${c.dark}"/></g>"""
}

private fun face(species: String, c: ColorDef): String = when (species) {
    "cat" -> """<g><path d="M96 100L104 100L100 105Z" fill="#E86F8A"/><path d="M72 104L50 100M72 110L50 113M128 104L150 100M128 110L150 113" stroke="$INK" stroke-width="2.2" stroke-linecap="round" opacity=".55"/><path d="M92 60l2 8M100 58v9M108 60l-2 8" stroke="${c.dark}" stroke-width="3" stroke-linecap="round"/></g>"""
    "dog" -> """<g><ellipse cx="100" cy="108" rx="21" ry="15" fill="${c.belly}"/><ellipse cx="100" cy="99" rx="7.5" ry="5.5" fill="$INK"/><ellipse cx="98" cy="97.5" rx="2.2" ry="1.3" fill="#fff" opacity=".7"/></g>"""
    "bunny" -> """<ellipse cx="100" cy="101" rx="4" ry="3" fill="#E86F8A"/>"""
    else -> """<g><ellipse cx="100" cy="106" rx="22" ry="14" fill="${c.belly}"/><circle cx="93" cy="101" r="2" fill="${c.dark}"/><circle cx="107" cy="101" r="2" fill="${c.dark}"/></g>"""
}

private fun round(x: Double) = """<g><circle cx="$x" cy="90" r="7.5" fill="$INK"/><circle cx="${x + 2.5}" cy="87" r="2.6" fill="#fff"/></g>"""

private fun eyes(e: ExpressionCode): String = when (e) {
    ExpressionCode.Happy -> """<g stroke="$INK" stroke-width="4" fill="none" stroke-linecap="round"><path d="M73 92Q82 81 91 92"/><path d="M109 92Q118 81 127 92"/></g>"""
    ExpressionCode.Bored -> """<g stroke="$INK" stroke-width="4" fill="none" stroke-linecap="round"><path d="M74 91Q82 96 90 91"/><path d="M110 91Q118 96 126 91"/><path d="M72 82l16 3M128 82l-16 3" stroke-width="3" opacity=".6"/></g>"""
    else -> "<g>" + round(82.0) + round(118.0) + (if (e == ExpressionCode.Hungry) """<path d="M72 80l15 4M128 80l-15 4" stroke="$INK" stroke-width="3" stroke-linecap="round" opacity=".6"/>""" else "") + "</g>"
}

private fun mouth(e: ExpressionCode): String = when (e) {
    ExpressionCode.Happy -> """<g><path d="M89 109Q100 128 111 109Z" fill="$INK"/><path d="M94 117Q100 124 106 117Q100 113 94 117Z" fill="#F27C8E"/></g>"""
    ExpressionCode.Hungry -> """<g><ellipse cx="100" cy="117" rx="6.5" ry="7.5" fill="$INK"/><ellipse cx="100" cy="121" rx="3.6" ry="2.8" fill="#F27C8E"/></g>"""
    ExpressionCode.Bored -> """<path d="M92 118H108" stroke="$INK" stroke-width="3.5" stroke-linecap="round"/>"""
    ExpressionCode.Dirty -> """<path d="M92 114Q100 120 108 114" stroke="$INK" stroke-width="3.5" fill="none" stroke-linecap="round"/>"""
    ExpressionCode.Content -> """<path d="M91 113Q100 122 109 113" stroke="$INK" stroke-width="3.5" fill="none" stroke-linecap="round"/>"""
}

private fun accessory(id: String): String = when (id) {
    "bow" -> """<g transform="translate(140 60)"><path d="M0 0L-17 -11L-17 11Z" fill="#F2647A"/><path d="M0 0L17 -11L17 11Z" fill="#F2647A"/><circle r="5.5" fill="#D9425A"/></g>"""
    "helmet" -> """<g><circle cx="100" cy="92" r="55" fill="rgba(170,215,255,.1)" stroke="#d7e9ff" stroke-width="4"/><path d="M62 66Q74 48 96 44" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity=".75"/><ellipse cx="100" cy="137" rx="44" ry="8.5" fill="#8fa4ff" stroke="#d7e9ff" stroke-width="3"/><circle cx="66" cy="137" r="3" fill="#ffe066"/><circle cx="134" cy="137" r="3" fill="#ffe066"/></g>"""
    "glasses" -> """<g fill="rgba(255,255,255,.35)" stroke="$INK" stroke-width="3.2"><circle cx="82" cy="90" r="15"/><circle cx="118" cy="90" r="15"/><path d="M97 90H103" fill="none"/></g>"""
    "hat" -> """<g transform="translate(100 56)"><rect x="-19" y="-36" width="38" height="36" rx="4" fill="#3B3B6B"/><rect x="-19" y="-13" width="38" height="8" fill="#E4534B"/><ellipse cx="0" cy="0" rx="32" ry="7.5" fill="#4A4A80"/></g>"""
    "scarf" -> """<g><path d="M62 128Q100 148 138 128L136 144Q100 164 64 144Z" fill="#E4534B"/><path d="M116 146l12 28h-15l-5-26Z" fill="#E4534B"/><path d="M76 136v12M90 141v12M104 141v12M118 137v12" stroke="#fff" stroke-width="3" opacity=".75" stroke-linecap="round"/></g>"""
    "flower" -> {
        val petals = listOf(0, 72, 144, 216, 288).joinToString("") { a ->
            val r = a * PI / 180
            """<circle cx="${cos(r) * 8}" cy="${sin(r) * 8}" r="6.5" fill="#F98CA0"/>"""
        }
        """<g transform="translate(66 60)">$petals<circle r="5" fill="#F5B301"/></g>"""
    }
    else -> ""
}

private fun star(cx: Double, cy: Double, r: Double, fill: String): String {
    val pts = (0 until 10).joinToString(" ") { i ->
        val rad = if (i % 2 == 0) r else r * 0.45
        val a = (PI / 5) * i - PI / 2
        "%.1f,%.1f".format(java.util.Locale.US, cx + cos(a) * rad, cy + sin(a) * rad)
    }
    return """<polygon points="$pts" fill="$fill" stroke="#D18F00" stroke-width="1.5" stroke-linejoin="round"/>"""
}

fun petSvg(species: String, c: ColorDef, accessory: String, stage: Int, expression: ExpressionCode): String {
    val s = STAGE_SCALE.getValue(stage)
    val sb = StringBuilder()
    sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">""")
    sb.append("""<defs><radialGradient id="aura"><stop offset="0" stop-color="#FFE07A" stop-opacity=".75"/><stop offset="1" stop-color="#FFE07A" stop-opacity="0"/></radialGradient></defs>""")
    if (stage == 4) sb.append("""<circle cx="100" cy="112" r="92" fill="url(#aura)"/>""")
    sb.append("""<ellipse cx="100" cy="188" rx="52" ry="6.5" fill="#050826" opacity=".4"/>""")
    sb.append("""<g transform="translate(100 184) scale($s) translate(-100 -184)">""")
    if (stage >= 3) sb.append("""<path d="M62 122Q52 164 46 184Q100 198 154 184Q148 164 138 122Z" fill="#D9425A"/>""")
    sb.append(back(species, c))
    sb.append("""<ellipse cx="100" cy="146" rx="38" ry="35" fill="${c.body}"/>""")
    sb.append("""<ellipse cx="100" cy="153" rx="24" ry="22" fill="${c.belly}"/>""")
    sb.append("""<ellipse cx="64" cy="148" rx="8" ry="15" fill="${c.body}" transform="rotate(18 64 148)"/>""")
    sb.append("""<ellipse cx="136" cy="148" rx="8" ry="15" fill="${c.body}" transform="rotate(-18 136 148)"/>""")
    sb.append("""<ellipse cx="76" cy="180" rx="16" ry="8.5" fill="${c.dark}"/>""")
    sb.append("""<ellipse cx="124" cy="180" rx="16" ry="8.5" fill="${c.dark}"/>""")
    if (expression == ExpressionCode.Hungry) sb.append("""<path d="M84 152q4-5 8 0t8 0 8 0" stroke="${c.dark}" stroke-width="2.6" fill="none" stroke-linecap="round" opacity=".7"/>""")
    if (stage >= 2) sb.append(star(100.0, 152.0, 11.0, "#F5B301"))
    sb.append("""<circle cx="100" cy="92" r="45" fill="${c.body}"/>""")
    sb.append("""<circle cx="66" cy="106" r="8.5" fill="#FF8FA3" opacity=".45"/>""")
    sb.append("""<circle cx="134" cy="106" r="8.5" fill="#FF8FA3" opacity=".45"/>""")
    if (expression == ExpressionCode.Dirty) sb.append("""<g fill="#8E6B4A" opacity=".55"><ellipse cx="74" cy="76" rx="6" ry="4"/><ellipse cx="128" cy="72" rx="4.5" ry="3.2"/><ellipse cx="112" cy="140" rx="7" ry="4.5"/><ellipse cx="80" cy="158" rx="4.5" ry="3"/></g>""")
    sb.append(face(species, c))
    sb.append(eyes(expression))
    sb.append(mouth(expression))
    sb.append(accessory(accessory))
    if (expression == ExpressionCode.Bored) sb.append("""<text x="142" y="62" font-size="20" font-weight="800" fill="$INK" opacity=".55">z</text>""")
    if (expression == ExpressionCode.Happy) sb.append("<g>" + star(40.0, 60.0, 7.0, "#FFD24D") + star(162.0, 52.0, 5.5, "#FFD24D") + "</g>")
    sb.append("</g>")
    if (stage == 4) sb.append("<g>" + star(28.0, 100.0, 7.0, "#FFD24D") + star(176.0, 108.0, 6.0, "#FFD24D") + star(160.0, 36.0, 5.0, "#FFD24D") + "</g>")
    sb.append("</svg>")
    return sb.toString()
}

/** Питомец: качается на месте (если анимации включены). Размер задаёт вызывающий. */
@Composable
fun PetView(
    appearance: PetAppearance,
    stage: Int,
    modifier: Modifier = Modifier,
    stats: Stats? = null,
    expression: ExpressionCode? = null,
    label: String? = null,
) {
    val content = Game.content
    val color = content.pet.colors.find { it.id == appearance.color } ?: content.pet.colors.first()
    val expr = expression ?: stats?.let { petExpression(it).code } ?: ExpressionCode.Content
    val svg = remember(appearance.species, color.id, appearance.accessory, stage, expr) {
        Svgs.parse("pet|${appearance.species}|${color.id}|${appearance.accessory}|$stage|$expr") { petSvg(appearance.species, color, appearance.accessory, stage, expr) }
    }
    val animate = Game.app.settings.animations
    var m = modifier.aspectRatio(1f)
    if (animate) {
        val t = rememberInfiniteTransition(label = "bob")
        val v = t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bobv")
        m = m.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 0.93f)
            translationY = -0.02f * size.height * v.value
            scaleX = 1f + 0.01f * v.value
            scaleY = 1f - 0.01f * v.value
        }
    }
    if (label != null) m = m.semantics { contentDescription = label }
    SvgCanvas(svg, m)
}

@Composable
fun PetOf(profile: Profile, modifier: Modifier = Modifier) =
    PetView(profile.pet.appearance, profile.pet.stage, modifier, stats = profile.pet.stats, label = "${profile.pet.name}: ${petExpression(profile.pet.stats).label}")

@Composable
fun PetSized(appearance: PetAppearance, stage: Int, size: Dp, expression: ExpressionCode = ExpressionCode.Content, label: String? = null) =
    PetView(appearance, stage, Modifier.size(size), expression = expression, label = label)
