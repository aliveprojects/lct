package app.finni.kids.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.finni.kids.app.ConfirmOptions
import app.finni.kids.app.Game
import app.finni.kids.app.Nav
import app.finni.kids.app.Ui
import app.finni.kids.app.present
import app.finni.kids.domain.ADULT_BONUS_REASONS
import app.finni.kids.domain.Econ
import app.finni.kids.domain.Mode
import app.finni.kids.domain.adultBonusLeft
import app.finni.kids.domain.coinsText
import app.finni.kids.domain.grantAdultBonus
import app.finni.kids.domain.resetDemoProfile
import app.finni.kids.domain.resetProgress
import app.finni.kids.domain.savingsTotal
import app.finni.kids.domain.setMode
import app.finni.kids.domain.withProfile
import app.finni.kids.platform.Storage
import app.finni.kids.platform.TRACKS
import app.finni.kids.ui.BtnVariant
import app.finni.kids.ui.Card
import app.finni.kids.ui.FButton
import app.finni.kids.ui.Icon
import app.finni.kids.ui.Meter
import app.finni.kids.ui.MeterKind
import app.finni.kids.ui.Muted
import app.finni.kids.ui.Screen
import app.finni.kids.ui.Tx
import app.finni.kids.ui.theme.Txt
import kotlinx.coroutines.launch

private fun makeProblem(): Triple<Int, Int, Int> {
    val a = 12 + (Math.random() * 8).toInt()
    val b = 6 + (Math.random() * 4).toInt()
    return Triple(a, b, a * b)
}

@Composable
private fun Gate(onPass: () -> Unit) {
    var problem by remember { mutableStateOf(makeProblem()) }
    var value by remember { mutableStateOf("") }
    fun check() {
        if (value.toIntOrNull() == problem.third) onPass()
        else {
            Ui.toast("Не получилось. Этот раздел для взрослых — попробуйте другой пример.")
            problem = makeProblem()
            value = ""
        }
    }
    Screen("Для взрослых", help = false, hint = "Раздел для родителей и учителей. Дети сюда обычно не заходят.") {
        Card(gap = 12.dp) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Icon("key", 64.dp) }
            Tx("Чтобы войти, решите пример:", Txt.lead, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Tx("${problem.first} × ${problem.second} = ?", Txt.huge.copy(fontSize = Txt.huge.fontSize * 0.98f), align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { NumberField(value, { value = it }, "Ответ на пример", onDone = { if (value.isNotEmpty()) check() }) }
            FButton("Войти", { check() }, block = true, enabled = value.isNotEmpty())
        }
    }
}

@Composable
private fun Dashboard() {
    val app = Game.app
    val p = Game.profile ?: return
    val content = Game.content
    val context = LocalContext.current
    val left = adultBonusLeft(p)
    val done = p.tasks.values.count { it.stars > 0 }
    val practiced = content.tasks.filter { (p.tasks[it.id]?.stars ?: 0) > 0 }.flatMap { it.competencies }.toSet()
    val stage = content.pet.stages.first { it.id == p.pet.stage }
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }

    fun reset() = Ui.scope.launch {
        val ok = Ui.confirm(ConfirmOptions("Сбросить прогресс?", "Сбросить прогресс", "Не надо", careful = true, icon = "refresh") {
            Tx("Игра начнётся заново: монеты, копилка, задания и рост питомца обнулятся.")
            Tx("Имя игрока и питомец останутся.")
        })
        if (!ok) return@launch
        Game.setState(withProfile(Game.app, resetProgress(p, Game.ctx)))
        Ui.toast("Прогресс сброшен")
        Nav.reset("/")
    }

    fun remove() = Ui.scope.launch {
        val ok = Ui.confirm(ConfirmOptions("Удалить профиль?", "Удалить профиль", "Оставить", careful = true, icon = "trash") {
            Tx("Будут удалены все данные профиля с этого устройства: имя игрока, питомец, монеты, задания.")
            Tx("Вернуть их будет нельзя.", Txt.bodyBold)
        })
        if (!ok) return@launch
        Game.setState(withProfile(Game.app, null))
        Storage.clearBackup()
        Ui.toast("Профиль удалён")
        Nav.reset("/welcome")
    }

    Screen("Для взрослых", help = false, hint = "Здесь видно, чему учится ребёнок. Оценок и сравнений нет.") {
        Card(gap = 12.dp) {
            Tx("Для чего это приложение", Txt.h3)
            Tx("Ребёнок 7–11 лет ухаживает за питомцем и принимает простые решения с игровыми монетами: планирует неделю, делит монеты на нужное и желаемое, копит на мечту и решает ситуации в заданиях. Настоящих денег, покупок и рекламы нет.")
            Muted("Ваша роль — быть рядом и обсуждать. Решения ребёнок принимает сам.")
        }
        Card(gap = 12.dp) {
            Tx("Чему учимся", Txt.h3)
            for (c in content.competencies) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(if (c.id in practiced) "check" else "sparkle", 26.dp)
                Column { Tx(c.text); Muted(if (c.id in practiced) "уже пробовали в заданиях" else "впереди", style = Txt.small) }
            }
        }
        Card(gap = 12.dp) {
            Tx("Общий прогресс", Txt.h3)
            listOf(
                "Игровое имя" to p.playerName,
                "Питомец" to "${p.pet.name}, ${stage.name}",
                "Недель сыграно" to p.history.size.toString(),
                "Заданий пройдено" to "$done из ${content.tasks.size}",
                "В копилке" to coinsText(savingsTotal(p)),
                "Мечт исполнено" to p.goals.count { it.done }.toString(),
            ).forEach { (k, v) -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Muted(k); Tx(v, Txt.bodyBold) } }
            for (t in content.topics) {
                val tasks = content.tasks.filter { it.topic == t.id }
                val stars = tasks.sumOf { p.tasks[it.id]?.stars ?: 0 }
                Meter(stars, tasks.size * 3, t.title, kind = MeterKind.Ok, text = "${t.title}: $stars/${tasks.size * 3}")
            }
        }
        Card(gap = 12.dp) {
            Tx("Поговорите вместе", Txt.h3)
            Tx("Почему ты решил(а) положить столько монет в копилку?")
            Tx("Чем «нужное» отличается от «хочется»? Приведи пример из жизни.")
            Tx("Что бы ты сделал(а) по-другому на следующей неделе?")
        }
        Card(gap = 12.dp) {
            Tx("Бонус от взрослого", Txt.h3)
            Row { Tx("Можно добавить игровые монеты за помощь или хорошую идею. Не больше ${Econ.adultBonusCap} за неделю. Это не деньги: их нельзя обменять на призы. Осталось на этой неделе: $left.") }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in ADULT_BONUS_REASONS) FButton("+${Econ.adultBonusStep} · $r", {
                    Ui.scope.launch { present(Game.act { pp, _ -> grantAdultBonus(pp, Econ.adultBonusStep, r) }) }
                }, small = true, variant = BtnVariant.Secondary, enabled = left > 0)
            }
        }
        Card(gap = 12.dp) { Tx("Доступность", Txt.h3); SettingsForm() }
        Card(gap = 12.dp) {
            Tx("Данные и приватность", Txt.h3)
            Tx("Приложение работает без интернета, без аккаунта и не собирает персональные данные. Игровое имя, питомец и прогресс хранятся только на этом устройстве. Разрешений Android не требуется.")
            FButton("Сбросить прогресс", { reset() }, block = true, variant = BtnVariant.Secondary, icon = "refresh")
            FButton("Удалить профиль", { remove() }, block = true, variant = BtnVariant.Secondary, icon = "trash")
        }
        Card(gap = 12.dp) {
            Tx("Режим для экспертов", Txt.h3)
            if (app.mode == Mode.Demo) {
                Tx("Сейчас включён демо-режим с тестовым профилем.")
                FButton("Панель проверки", { Ui.scope.launch { openDemoPanel(Ui.scope) } }, block = true, variant = BtnVariant.Secondary)
                FButton("Выйти из демо-режима", { Game.mutate { setMode(it, Mode.Normal) }; Nav.reset("/") }, block = true, variant = BtnVariant.Secondary)
                FButton("Сбросить тестовый профиль", {
                    Ui.scope.launch {
                        val ok = Ui.confirm(ConfirmOptions("Сбросить тестовый профиль?", "Сбросить", "Отмена", careful = true, icon = "refresh") { Tx("Тестовый профиль вернётся к исходному состоянию. Игровой профиль не изменится.") })
                        if (ok) { Game.mutate { resetDemoProfile(it) }; Nav.reset("/intro") }
                    }
                }, block = true, variant = BtnVariant.Secondary)
            } else {
                Tx("Тестовый профиль позволяет быстро пройти игровой цикл: все задания открыты, недели переключаются без ожидания. Игровой профиль при этом не меняется.")
                FButton("Включить демо-режим", { Game.mutate { setMode(it, Mode.Demo) }; Nav.reset("/") }, block = true, variant = BtnVariant.Secondary, icon = "key")
            }
        }
        Card(gap = 12.dp) {
            Tx("Полезно для семьи", Txt.h3)
            Tx("Материалы по финансовой грамотности для детей и родителей: раздел «Финансовая грамотность» на портале «Открытый бюджет города Москвы» и просветительский ресурс Банка России «Финансовая культура». В приложении нет внешних ссылок — найдите их через поиск.")
        }
        Muted("Музыка: " + TRACKS.joinToString("; ") { "«${it.title}» — ${it.artist}" } + ".", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Muted("Финни · версия $version · нативное приложение (Kotlin, Jetpack Compose) · шрифт Rubik (лицензия OFL)", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun AdultScreen() {
    var ok by remember { mutableStateOf(false) }
    if (ok) Dashboard() else Gate { ok = true }
}
