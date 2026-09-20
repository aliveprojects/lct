package app.finni.kids.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.finni.kids.ui.theme.pal
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import java.util.concurrent.ConcurrentHashMap

/** Векторные картинки (иконки и питомец) — SVG-описания, которые рисует AndroidSVG. Файлы лежат в assets/icons. */
object Svgs {
    private val parsed = ConcurrentHashMap<String, SVG>()
    private val files = ConcurrentHashMap<String, String>()

    private fun hex(c: Color): String = "#%06X".format(c.toArgb() and 0xFFFFFF)

    /** Разбирает SVG один раз и кэширует. `currentColor` заменяется на нужный цвет. */
    fun parse(key: String, source: () -> String): SVG =
        parsed.getOrPut(key) {
            SVG.getFromString(source()).also {
                it.setDocumentWidth("100%")
                it.setDocumentHeight("100%")
            }
        }

    fun icon(context: Context, name: String, tint: Color): SVG {
        val fileName = if (files.containsKey(name) || assetExists(context, name)) name else "star"
        val raw = files.getOrPut(fileName) { context.assets.open("icons/$fileName.svg").bufferedReader().use { it.readText() } }
        val hexTint = hex(tint)
        return parse("$fileName|$hexTint") { raw.replace("currentColor", hexTint) }
    }

    private fun assetExists(context: Context, name: String): Boolean =
        try { context.assets.open("icons/$name.svg").close(); true } catch (e: Exception) { false }
}

/** Рисует SVG, вписывая его в размер элемента. */
@Composable
fun SvgCanvas(svg: SVG, modifier: Modifier) {
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            svg.renderToCanvas(canvas.nativeCanvas, RenderOptions.create().viewPort(0f, 0f, size.width, size.height))
        }
    }
}

/** Иконка по имени. Контурные иконки берут цвет `tint` (по умолчанию — цвет текста), цветные — свой. */
@Composable
fun Icon(name: String, size: Dp = 24.dp, tint: Color? = null, description: String? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val color = tint ?: pal.ink
    val svg = remember(name, color) { Svgs.icon(context, name, color) }
    val m = modifier.size(size).let { if (description != null) it.semantics { contentDescription = description } else it }
    SvgCanvas(svg, m)
}

