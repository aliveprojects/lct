package app.finni.kids.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.finni.kids.app.Game
import app.finni.kids.app.Ui
import app.finni.kids.domain.Settings
import app.finni.kids.domain.TextScale
import app.finni.kids.platform.Beep
import app.finni.kids.platform.Music
import app.finni.kids.platform.Sound
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Muted
import app.finni.kids.ui.SegOpt
import app.finni.kids.ui.Segmented
import app.finni.kids.ui.SwitchRow
import app.finni.kids.ui.Tx
import app.finni.kids.ui.theme.Txt

/** Меняет настройки и сразу применяет их: звуки и музыка включаются или выключаются без перезапуска. */
fun updateSettings(patch: (Settings) -> Settings) {
    Game.mutate { s ->
        val settings = patch(s.settings)
        Sound.setEnabled(settings.sound)
        Music.setEnabled(settings.music)
        s.copy(settings = settings)
    }
}

@Composable
fun SettingsForm() {
    val st = Game.app.settings
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SwitchRow("Фоновая музыка", st.music, { v -> updateSettings { it.copy(music = v) } }, "Спокойная музыка во время игры. Её можно выключить в любой момент.")
        SwitchRow("Звуки", st.sound, { v -> updateSettings { it.copy(sound = v) }; if (v) Sound.play(Beep.Good) }, "Короткие сигналы при действиях. Важное всегда есть и в тексте.")
        SwitchRow("Анимации", st.animations, { v -> updateSettings { it.copy(animations = v) } }, "Движение питомца и конфетти.")
        SwitchRow("Высокий контраст", st.highContrast, { v -> updateSettings { it.copy(highContrast = v) } }, "Чёрный фон, белый текст, жёлтые кнопки и чёткие рамки.")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tx("Размер текста", Txt.bodyBold)
            Segmented(
                st.textScale,
                listOf(SegOpt(TextScale.Normal, "Обычный"), SegOpt(TextScale.Large, "Крупный"), SegOpt(TextScale.XLarge, "Очень крупный")),
                { v -> updateSettings { it.copy(textScale = v) } },
            )
            Muted("Приложение также подстраивается под размер шрифта в настройках телефона.", style = Txt.small)
        }
    }
}

suspend fun openSettings() = Ui.sheet("Настройки", actions = { close -> FButton("Готово", close, Modifier.fillMaxWidth(), block = true) }) {
    SettingsForm()
}

