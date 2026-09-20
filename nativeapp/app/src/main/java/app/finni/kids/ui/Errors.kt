package app.finni.kids.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.finni.kids.app.ConfirmOptions
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.domain.ErrorCode
import app.finni.kids.domain.GameError
import app.finni.kids.platform.Beep
import app.finni.kids.platform.Sound
import app.finni.kids.ui.theme.Txt

/** Ошибка действия — не тупик: объясняем, что случилось, и предлагаем варианты. */
suspend fun explainError(e: GameError) {
    Sound.play(Beep.Oops)
    if (e.code == ErrorCode.PLAN_REQUIRED) {
        val go = Ui.confirm(
            ConfirmOptions(
                title = "Сначала — план недели", confirmLabel = "Составить план", cancelLabel = "Позже", icon = "clip",
                body = { Tx(e.message) },
            ),
        )
        if (go) Nav.go("/plan")
        return
    }
    Ui.sheet("Так пока нельзя") { close ->
        Tx(e.message, Txt.lead)
        val options = e.options
        if (!options.isNullOrEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Tx("Что можно сделать:", Txt.bodyBold)
                for (o in options) {
                    Card(gap = 6.dp) {
                        Tx(o.label, Txt.bodyBold)
                        Muted(o.hint)
                        o.route?.let { r -> FButton("Перейти", { close(); Nav.go(r) }, variant = BtnVariant.Secondary, small = true) }
                    }
                }
            }
        }
        FButton("Понятно", close, Modifier.fillMaxWidth(), block = true)
    }
}
