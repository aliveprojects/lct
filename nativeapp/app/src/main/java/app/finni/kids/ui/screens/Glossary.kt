package app.finni.kids.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Ui
import app.finni.kids.ui.Card
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Muted
import app.finni.kids.ui.Screen
import app.finni.kids.ui.Tint
import app.finni.kids.ui.Tx
import app.finni.kids.ui.noRipple
import app.finni.kids.ui.theme.Txt
import app.finni.kids.ui.theme.pal

@Composable
fun GlossaryScreen() {
    Screen("Словарик", hint = "Что значат слова из игры. Нажми на слово, чтобы прочитать.") {
        for (g in Game.content.glossary) {
            var open by remember { mutableStateOf(false) }
            Card(gap = 4.dp, onClick = { open = !open }) {
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tx(g.term, Txt.bodyBold.copy(fontSize = Txt.bodyBold.fontSize * 1.03f), modifier = Modifier.weight(1f))
                    Tx(if (open) "−" else "+", Txt.h3, color = pal.primary)
                }
                if (open) {
                    Tx(g.short)
                    g.example?.let { ex -> Column { Tx("Например: $ex", color = pal.ink2) } }
                }
            }
        }
    }
}

/** Слово из словарика: объясняет термин простыми словами. */
suspend fun openTerm(id: String) {
    val t = Game.content.glossary.find { it.id == id } ?: return
    Ui.sheet(t.term) { close ->
        Tx(t.short, Txt.lead)
        t.example?.let { Card(tint = Tint.Gold) { Tx("Например: $it") } }
        FButton("Понятно", close, block = true)
    }
}
