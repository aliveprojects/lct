package app.finni.kids.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.finni.kids.app.Game
import app.finni.kids.ui.theme.pal

private class Star(val x: Float, val y: Float, val r: Float, val color: Long)

// Положения звёзд взяты из веб-версии: два слоя — постоянный и мерцающий.
private val STARS_STATIC = listOf(
    Star(.08f, .12f, 1.3f, 0xFFFFFFFF), Star(.22f, .06f, 1f, 0xFFCFE0FF), Star(.37f, .15f, 1.1f, 0xFFFFFFFF), Star(.58f, .05f, 1.4f, 0xFFFFE9B0),
    Star(.74f, .11f, 1f, 0xFFFFFFFF), Star(.91f, .07f, 1.3f, 0xFFCFE0FF), Star(.05f, .38f, 1f, 0xFFFFFFFF), Star(.95f, .34f, 1.2f, 0xFFFFFFFF),
    Star(.12f, .62f, 1.1f, 0xFFCFE0FF), Star(.88f, .58f, 1.2f, 0xFFFFE9B0), Star(.30f, .84f, 1f, 0xFFFFFFFF), Star(.66f, .90f, 1.3f, 0xFFFFFFFF),
    Star(.93f, .82f, 1f, 0xFFCFE0FF), Star(.48f, .46f, .9f, 0xFFFFFFFF),
)
private val STARS_TWINKLE = listOf(
    Star(.16f, .24f, 1.6f, 0xFFFFFFFF), Star(.46f, .09f, 1.5f, 0xFFFFFFFF), Star(.82f, .22f, 1.7f, 0xFFFFE9B0),
    Star(.07f, .74f, 1.5f, 0xFFFFFFFF), Star(.70f, .70f, 1.6f, 0xFFCFE0FF), Star(.40f, .94f, 1.5f, 0xFFFFFFFF),
)

private fun DrawScope.glow(cx: Float, cy: Float, radiusDp: Float, c: Color) {
    val center = Offset(cx * size.width, cy * size.height)
    val r = radiusDp * density
    drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center, r), r, center)
}

private fun DrawScope.stars(list: List<Star>, alpha: Float) {
    for (s in list) drawCircle(Color(s.color).copy(alpha = alpha), s.r * density * 1.25f, Offset(s.x * size.width, s.y * size.height))
}

/** Небо: туманности и звёзды. В режиме высокого контраста — просто чёрный фон. */
@Composable
fun SpaceBackground(modifier: Modifier = Modifier.fillMaxSize()) {
    if (pal.highContrast) {
        Box(modifier.background(Color.Black))
        return
    }
    val twinkle = if (Game.app.settings.animations) {
        rememberInfiniteTransition(label = "twinkle").animateFloat(1f, 0.3f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "tw").value
    } else 1f
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(0f to Color(0xFF0A0D35), 0.65f to Color(0xFF141A5B), 1f to Color(0xFF1B2072)))
        glow(0.12f, -0.02f, 300f, Color(0x80_8462FF))
        glow(1.05f, 0.22f, 260f, Color(0x57_2696FF))
        glow(-0.10f, 0.86f, 240f, Color(0x33_FF5CAA))
        stars(STARS_STATIC, 0.85f)
        stars(STARS_TWINKLE, twinkle)
    }
}
