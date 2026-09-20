package app.finni.kids.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import app.finni.kids.R
import app.finni.kids.domain.TextScale

// Тема «Космическое приключение»: тёмное звёздное небо, светящиеся акценты, стеклянные карточки.
// Значения совпадают с веб-версией (см. finny/src/styles/app.css). Контраст текста ≥ 4,5 : 1.

@OptIn(ExperimentalTextApi::class)
val Rubik = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.rubik, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

private fun rgba(r: Int, g: Int, b: Int, a: Double) = Color(r / 255f, g / 255f, b / 255f, a.toFloat())

@Immutable
class Palette(
    val highContrast: Boolean,
    val sky0: Color,
    val sky1: Color,
    val ink: Color,
    val ink2: Color,
    val surface: Color,
    val surface2: Color,
    val solid: Color,
    val line: Color,
    val primary: Color,
    val primary2: Color,
    val primaryInk: Color,
    val primarySoft: Color,
    val accent: Color,
    val must: Color,
    val mustBg: Color,
    val want: Color,
    val wantBg: Color,
    val save: Color,
    val saveBg: Color,
    val gold: Color,
    val goldBg: Color,
    val goldInk: Color,
    val warn: Color,
    val warnBg: Color,
    val sheetTop: Color,
    val btnText2: Color,
)

val NormalPalette = Palette(
    highContrast = false,
    sky0 = Color(0xFF070A2B), sky1 = Color(0xFF0E1240),
    ink = Color(0xFFF4F6FF), ink2 = Color(0xFFBCC4F5),
    surface = rgba(30, 36, 112, 0.84), surface2 = rgba(54, 60, 152, 0.9), solid = Color(0xFF1A1F6E),
    line = rgba(170, 185, 255, 0.32),
    primary = Color(0xFF4EECDA), primary2 = Color(0xFF4AA8FF), primaryInk = Color(0xFF06133A),
    primarySoft = rgba(78, 236, 218, 0.16), accent = Color(0xFFB79BFF),
    must = Color(0xFF8DBDFF), mustBg = rgba(90, 140, 255, 0.22),
    want = Color(0xFFFFAB8C), wantBg = rgba(255, 125, 85, 0.2),
    save = Color(0xFF78F4B8), saveBg = rgba(60, 220, 150, 0.18),
    gold = Color(0xFFFFC93C), goldBg = rgba(255, 201, 60, 0.17), goldInk = Color(0xFF2E1D00),
    warn = Color(0xFFFFD88F), warnBg = rgba(255, 190, 60, 0.17),
    sheetTop = Color(0xFF232883), btnText2 = Color(0xFF8FF5E9),
)

/** Высокий контраст: чёрный фон, белый текст, жёлтые действия, чёткие рамки. */
val HighContrastPalette = Palette(
    highContrast = true,
    sky0 = Color.Black, sky1 = Color.Black,
    ink = Color.White, ink2 = Color.White,
    surface = Color.Black, surface2 = Color(0xFF111111), solid = Color.Black,
    line = Color.White,
    primary = Color(0xFFFFE600), primary2 = Color(0xFFFFE600), primaryInk = Color.Black,
    primarySoft = Color.Black, accent = Color(0xFFB79BFF),
    must = Color(0xFF9FC8FF), mustBg = Color.Black,
    want = Color(0xFFFFB59B), wantBg = Color.Black,
    save = Color(0xFF8DFFC7), saveBg = Color.Black,
    gold = Color(0xFFFFE600), goldBg = Color.Black, goldInk = Color.Black,
    warn = Color(0xFFFFE600), warnBg = Color.Black,
    sheetTop = Color.Black, btnText2 = Color(0xFFFFE600),
)

val LocalPalette = staticCompositionLocalOf { NormalPalette }

val pal: Palette @Composable get() = LocalPalette.current

/** Готовые стили текста. Размеры — как в веб-версии (базовые 17 sp). */
object Txt {
    private fun s(size: Double, weight: Int, lh: Double = 1.38) = TextStyle(fontFamily = Rubik, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = (size * lh).sp)
    val body = s(17.0, 500)
    val bodyBold = s(17.0, 700)
    val lead = s(18.0, 600)
    val h1 = s(27.2, 800, 1.15)
    val h2 = s(23.0, 800, 1.2)
    val h3 = s(19.0, 700)
    val small = s(14.6, 500)
    val smallBold = s(14.6, 700)
    val tiny = s(12.0, 600, 1.2)
    val topbar = s(22.0, 800, 1.15)
    val button = s(17.7, 800, 1.25)
    val big = s(27.0, 800, 1.1)
    val huge = s(45.0, 800, 1.0)
}

/** Оборачивает приложение: палитра и масштаб текста из настроек (поверх системного размера шрифта). */
@Composable
fun FinniTheme(highContrast: Boolean, scale: TextScale, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    val factor = when (scale) {
        TextScale.Normal -> 1f
        TextScale.Large -> 1.147f
        TextScale.XLarge -> 1.294f
    }
    CompositionLocalProvider(
        LocalPalette provides (if (highContrast) HighContrastPalette else NormalPalette),
        LocalDensity provides Density(d.density, d.fontScale * factor),
        content = content,
    )
}
