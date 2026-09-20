package app.finni.kids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.ButtonRow
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Tx
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal

/** Три типа решений — одинаково показываются в знакомстве и в подсказке «?». */
@Composable
fun ThreeChoices() {
    val p = pal
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("bowl", "Нужное" to "Еда и уход. Без этого питомцу трудно.", Triple(p.mustBg, p.must, Color(0xFFE3EEFF))),
            Triple("ball", "Хочется" to "Игрушки и украшения. Радуют, но можно подождать.", Triple(p.wantBg, p.want, Color(0xFFFFEEE7))),
            Triple("jar", "Отложить" to "Копилка для мечты. Монеты не тратятся сразу.", Triple(p.saveBg, p.save, Color(0xFFE2FFF1))),
        ).forEach { (icon, texts, colors) ->
            val shape = RoundedCornerShape(22.dp)
            Row(
                Modifier.fillMaxWidth().background(colors.first, shape).border(if (p.highContrast) 3.dp else 1.dp, if (p.highContrast) Color.White else colors.second.copy(alpha = 0.5f), shape).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, 44.dp)
                Column {
                    Tx(texts.first, Txt.bodyBold.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = if (p.highContrast) Color.White else colors.third)
                    Tx(texts.second, Txt.body, color = if (p.highContrast) Color.White else colors.third)
                }
            }
        }
    }
}

suspend fun openHelp() = Ui.sheet("Как играть") { close ->
    Tx("Ты растишь питомца. С монетами можно сделать три вещи:", Txt.lead)
    ThreeChoices()
    Tx("Монет не хватит на всё, поэтому выбирай. Ошибаться можно: ошибка — это урок. Монеты в игре не настоящие.")
    ButtonRow {
        FButton("Словарик", { close(); Nav.go("/glossary") }, variant = BtnVariant.Secondary, icon = "book")
        FButton("Знакомство ещё раз", { close(); Nav.go("/intro?again=1") }, variant = BtnVariant.Secondary, icon = "sparkle")
    }
    FButton("Понятно", close, block = true)
}
