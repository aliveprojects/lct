package app.finni.kids.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Ui
import app.finni.kids.domain.Econ
import app.finni.kids.domain.Tx as Transaction
import app.finni.kids.domain.TxKind
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.MINUS
import app.finni.kids.ui.Muted
import app.finni.kids.ui.Tx
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal

private val KIND_ICON = mapOf(
    TxKind.Start to "gift", TxKind.Allowance to "coin", TxKind.Daily to "gift", TxKind.Task to "star", TxKind.Bonus to "smile",
    TxKind.Purchase to "bag", TxKind.Event to "vet", TxKind.Save to "jar", TxKind.Withdraw to "jar", TxKind.Goal to "trophy",
)

private val KIND_TEXT = mapOf(
    TxKind.Start to "Подарок", TxKind.Allowance to "Доход", TxKind.Daily to "Доход", TxKind.Task to "Доход", TxKind.Bonus to "Доход",
    TxKind.Purchase to "Покупка", TxKind.Event to "Непредвиденное", TxKind.Save to "В копилку", TxKind.Withdraw to "Из копилки", TxKind.Goal to "Мечта",
)

private fun sgn(n: Int) = if (n > 0) "+$n" else "$MINUS${Math.abs(n)}"

@Composable
fun LedgerList(tx: List<Transaction>, empty: String = "Пока ничего не происходило.") {
    if (tx.isEmpty()) {
        Muted(empty)
        return
    }
    val p = pal
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (t in tx.asReversed()) {
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().background(if (p.highContrast) Color.Black else p.surface, shape).border(1.dp, p.line, shape).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(KIND_ICON.getValue(t.kind), 34.dp)
                Column(Modifier.weight(1f)) {
                    Tx(t.title, Txt.bodyBold)
                    val dir = when (t.dir) { "must" -> " · нужное"; "want" -> " · хочется"; else -> "" }
                    Muted("${KIND_TEXT.getValue(t.kind)} · неделя ${t.period}$dir", style = Txt.small)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (t.amount != 0) Tx(sgn(t.amount), Txt.bodyBold, color = if (t.amount > 0) p.save else p.want)
                    val sd = t.savingsDelta
                    if (sd != null && sd != 0) Muted("копилка ${sgn(sd)}", style = Txt.tiny)
                    Muted("баланс ${t.balance}", style = Txt.tiny)
                }
            }
        }
    }
}

/** «Откуда монеты?»: каждое изменение баланса с источником и суммой. */
suspend fun openLedger() {
    val p = Game.profile ?: return
    Ui.sheet("История монет") { close ->
        Muted("Здесь видно, откуда пришли монеты и куда ушли. Баланс не меняется без объяснения.")
        LedgerList(p.ledger.takeLast(Econ.ledgerView))
        FButton("Закрыть", close, Modifier.fillMaxWidth(), block = true)
    }
}
