package app.finni.kids.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.domain.Econ
import app.finni.kids.domain.PetStat
import app.finni.kids.domain.STATS
import app.finni.kids.domain.STAT_LABEL
import app.finni.kids.domain.petExpression
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Meter
import app.finni.kids.ui.MeterKind
import app.finni.kids.ui.Muted
import app.finni.kids.ui.STAT_ICON
import app.finni.kids.ui.Tx
import app.finni.kids.ui.theme.Txt

private class Why(val low: String, val ok: String, val fix: String)

private val WHY = mapOf(
    PetStat.Satiety to Why("Давно не было еды.", "Питомец сыт.", "Еда — в магазине, раздел «Нужное»."),
    PetStat.Care to Why("Питомцу не хватает ухода.", "Питомец чистый и здоровый.", "Купание и расчёска — в «Нужном»."),
    PetStat.Mood to Why("Питомцу скучно.", "Питомцу весело.", "Игрушки — в разделе «Хочется». Копить и держаться плана тоже радует."),
)

/** Объясняет, почему питомец так себя чувствует, и что можно сделать. */
suspend fun openPetFeelings() {
    val p = Game.profile ?: return
    val expr = petExpression(p.pet.stats)
    Ui.sheet("${p.pet.name}: ${expr.label.lowercase()}") { close ->
        Tx(p.pet.reason?.text ?: expr.hint, Txt.lead)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (s in STATS) {
                val low = p.pet.stats[s] < Econ.statLow
                val why = WHY.getValue(s)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(STAT_ICON.getValue(s), 36.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Tx("${STAT_LABEL.getValue(s)}: ${p.pet.stats[s]} из 100", Txt.bodyBold)
                        Meter(p.pet.stats[s], label = STAT_LABEL.getValue(s), kind = if (low) MeterKind.Want else MeterKind.Ok, showNumbers = false)
                        Tx(if (low) "${why.low} ${why.fix}" else why.ok)
                    }
                }
            }
        }
        Muted("Каждую неделю показатели чуть снижаются — так бывает у всех. Питомец никогда не болеет из-за твоих ошибок.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FButton("В магазин", { close(); Nav.go("/shop") }, Modifier.weight(1f), variant = BtnVariant.Secondary)
            FButton("Понятно", close, Modifier.weight(1f))
        }
    }
}
