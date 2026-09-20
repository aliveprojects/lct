package app.finni.kids.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.finni.kids.app.ConfirmOptions
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.app.present
import app.finni.kids.domain.Mode
import app.finni.kids.domain.NextLink
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.Report
import app.finni.kids.domain.Tone
import app.finni.kids.domain.advanceDemoDay
import app.finni.kids.domain.playPeriodAuto
import app.finni.kids.domain.resetDemoProfile
import app.finni.kids.domain.setMode
import app.finni.kids.domain.withProfile
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.Card
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Muted
import app.finni.kids.ui.Tint
import app.finni.kids.ui.Tx
import app.finni.kids.ui.theme.Txt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Панель проверки: помогает экспертам пройти обязательный сценарий без ожидания календарных сроков. */
suspend fun openDemoPanel(scope: CoroutineScope) = Ui.sheet("Демо-режим") { close ->
    val p = Game.profile
    val stage = p?.let { pr -> Game.content.pet.stages.find { it.id == pr.pet.stage }?.name } ?: ""
    Card(tint = Tint.Gold) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon("key", 22.dp); Tx("Тестовый профиль", Txt.bodyBold) }
        Tx("Хранится отдельно от игрового. " + (p?.let { "Сейчас: неделя ${it.period.index}, $stage, очков роста ${it.pet.growth}." } ?: ""))
        Muted("Все задания открыты сразу. Недели переключаются без ожидания.")
    }
    FButton("Пропустить день (подарок дня)", {
        Game.mutate { advanceDemoDay(it) }
        Ui.toast("Наступил следующий день: подарок дня снова доступен")
        close()
    }, block = true, variant = BtnVariant.Secondary, icon = "gift")
    FButton("Сыграть неделю автоматически", {
        val cur = Game.profile
        if (cur != null) {
            close()
            val after = playPeriodAuto(cur, Game.ctx)
            Game.setState(withProfile(Game.app, after))
            val last = after.history.lastOrNull()
            if (last != null) scope.launch {
                present(Outcome.Ok(after, Report("Неделя ${last.index} сыграна", Tone.Good, emptyList(), "${last.summary} Так бы выглядела разумная неделя: план, задания, покупки, копилка.", NextLink("Смотреть итоги", "/review")), Unit))
            }
        }
    }, block = true, variant = BtnVariant.Secondary, icon = "sparkle")
    FButton("Итоги последней недели", { close(); Nav.go("/review") }, block = true, variant = BtnVariant.Secondary, icon = "trophy")
    FButton("Сбросить тестовый профиль", {
        scope.launch {
            val ok = Ui.confirm(
                ConfirmOptions("Сбросить тестовый профиль?", "Сбросить", "Отмена", careful = true, icon = "refresh") {
                    Tx("Тестовый профиль вернётся к исходному состоянию, и можно пройти сценарий заново. Игровой профиль не изменится.")
                },
            )
            if (ok) {
                Game.mutate { resetDemoProfile(it) }
                close()
                Nav.reset("/intro")
            }
        }
    }, block = true, variant = BtnVariant.Gold, icon = "refresh")
    FButton("Выйти из демо-режима", {
        Game.mutate { setMode(it, Mode.Normal) }
        close()
        Nav.reset("/")
    }, block = true, variant = BtnVariant.Secondary)
    FButton("Закрыть", close, Modifier.fillMaxWidth(), block = true, variant = BtnVariant.Ghost)
}
