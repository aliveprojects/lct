package app.finni.kids

import app.finni.kids.domain.ADULT_BONUS_REASONS
import app.finni.kids.domain.AppState
import app.finni.kids.domain.Direction
import app.finni.kids.domain.ErrorCode
import app.finni.kids.domain.GameJson
import app.finni.kids.domain.Mode
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.Plan
import app.finni.kids.domain.activeGoal
import app.finni.kids.domain.activeProfile
import app.finni.kids.domain.advanceDemoDay
import app.finni.kids.domain.availableTasks
import app.finni.kids.domain.buy
import app.finni.kids.domain.confirmPlan
import app.finni.kids.domain.deposit
import app.finni.kids.domain.endPeriod
import app.finni.kids.domain.envelope
import app.finni.kids.domain.freeCoins
import app.finni.kids.domain.grantAdultBonus
import app.finni.kids.domain.initialApp
import app.finni.kids.domain.makeCtx
import app.finni.kids.domain.nextActiveTask
import app.finni.kids.domain.parseState
import app.finni.kids.domain.playPeriodAuto
import app.finni.kids.domain.resetDemoProfile
import app.finni.kids.domain.resetProgress
import app.finni.kids.domain.selectGoal
import app.finni.kids.domain.serializeState
import app.finni.kids.domain.setMode
import app.finni.kids.domain.solveTask
import app.finni.kids.domain.submitTask
import app.finni.kids.domain.suggestPlan
import app.finni.kids.domain.todayKey
import app.finni.kids.domain.withProfile
import app.finni.kids.domain.withdraw
import app.finni.kids.domain.worstAnswer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Убирает поле key из объекта по пути path — так проверяем разбор данных «старых версий». */
private fun removeKey(raw: String, path: List<String>, key: String): String {
    fun rec(e: JsonElement, p: List<String>): JsonElement =
        if (p.isEmpty()) JsonObject(e.jsonObject - key)
        else JsonObject(e.jsonObject.mapValues { (k, v) -> if (k == p[0]) rec(v, p.drop(1)) else v })
    return rec(Json.parseToJsonElement(raw), path).toString()
}

class StateTest {
    @Test fun `профиль баланс покупки цель и прогресс переживают перезапуск`() {
        var s = initialApp().copy(introSeen = true)
        var p = makeProfile()
        p = playPeriodAuto(p, ctx())
        p = playPeriodAuto(p, ctx())
        s = withProfile(s, p)
        val r = parseState(serializeState(s))
        assertFalse(r.recovered)
        assertEquals(s, r.state)
        val q = activeProfile(r.state)!!
        assertEquals(p.wallet, q.wallet); assertEquals(p.ledger, q.ledger); assertEquals(p.goals, q.goals); assertEquals(p.tasks, q.tasks); assertEquals(p.pet.growth, q.pet.growth)
    }

    @Test fun `фоновая музыка включена по умолчанию и выключается настройкой`() {
        assertTrue(initialApp().settings.music)
        val off = withProfile(initialApp().copy(settings = initialApp().settings.copy(music = false)), makeProfile())
        assertFalse(parseState(serializeState(off)).state.settings.music)
        val raw = removeKey(serializeState(off), listOf("settings"), "music")
        assertTrue(parseState(raw).state.settings.music)
    }

    @Test fun `пустое хранилище - чистое начало`() {
        assertEquals(initialApp(), parseState(null).state); assertFalse(parseState(null).recovered)
        assertEquals(initialApp(), parseState("").state); assertFalse(parseState("").recovered)
    }

    @Test fun `повреждённые данные не роняют приложение`() {
        for (raw in listOf("{oops", "\"строка\"", "[]", "{\"schema\":1,\"main\":{\"wallet\":\"много\"}}", "{\"schema\":99}", "null")) {
            val r = parseState(raw)
            assertTrue(raw, r.recovered)
            assertEquals(initialApp(), r.state)
        }
    }

    @Test fun `данные без новых полей достраиваются значениями по умолчанию`() {
        val s = withProfile(initialApp(), makeProfile())
        var raw = serializeState(s)
        raw = removeKey(raw, listOf("main"), "wishlist")
        raw = removeKey(raw, listOf("main"), "trophies")
        raw = removeKey(raw, listOf("settings"), "highContrast")
        val r = parseState(raw)
        assertFalse(r.recovered)
        assertEquals(emptyList<String>(), r.state.main!!.wishlist)
        assertEquals(emptyList<String>(), r.state.main!!.trophies)
        assertFalse(r.state.settings.highContrast)
    }

    @Test fun `игровой и тестовый профили хранятся раздельно`() {
        var s = withProfile(initialApp(), makeProfile(false))
        s = setMode(s, Mode.Demo)
        assertNull(activeProfile(s))
        s = withProfile(s, makeProfile(true))
        assertFalse(s.main!!.isDemo); assertTrue(s.demo!!.isDemo)
        s = resetDemoProfile(advanceDemoDay(s))
        assertNull(s.demo); assertEquals(0, s.demoDayOffset)
        assertNotNull(s.main)
    }

    @Test fun `демо-календарь можно прокрутить без ожидания`() {
        var s: AppState = setMode(initialApp(), Mode.Demo)
        val now = LocalDate.of(2026, 9, 19)
        assertEquals("2026-09-19", makeCtx(s, content, now).today)
        s = advanceDemoDay(s)
        assertEquals("2026-09-20", makeCtx(s, content, now).today)
        assertEquals("2026-09-19", makeCtx(s.copy(mode = Mode.Normal), content, now).today)
        assertEquals("2027-01-01", todayKey(1, LocalDate.of(2026, 12, 31)))
    }

    @Test fun `сброс прогресса оставляет питомца и имя но начинает игру заново`() {
        val played = playPeriodAuto(playPeriodAuto(makeProfile(), ctx()), ctx())
        assertEquals(2, played.history.size)
        val fresh = resetProgress(played, ctx())
        assertEquals(played.playerName, fresh.playerName)
        assertEquals(played.pet.name, fresh.pet.name)
        assertEquals(played.pet.appearance.species, fresh.pet.appearance.species)
        assertEquals(100, fresh.wallet); assertTrue(fresh.history.isEmpty()); assertTrue(fresh.tasks.isEmpty()); assertTrue(fresh.trophies.isEmpty())
        assertEquals(0, fresh.pet.growth)
    }
}

class ScenarioTest {
    @Test fun `сквозной сценарий Приложения А шаги 2 - 12`() {
        // 2–3. Локальный профиль без имени/телефона/e-mail, питомец с именем
        var p = makeProfile()
        assertEquals("Тестик", p.pet.name)
        assertFalse(Regex("@|\\+7|phone|email", RegexOption.IGNORE_CASE).containsMatchIn(GameJson.encodeToString(p)))

        // 4. Стартовый бюджет, текущая цель и доступные задания
        assertEquals(100, p.wallet)
        assertEquals("house", activeGoal(p)!!.id)
        assertTrue(availableTasks(p, ctx()).size >= 3)
        assertNotNull(nextActiveTask(p, ctx()))

        // 5. Распределение: нельзя больше бюджета, остаток виден, потом подтверждение
        assertFalse(confirmPlan(p, Plan(70, 30, 20)) is Outcome.Ok)
        val suggestion = suggestPlan(p, content)
        p = must(confirmPlan(p, suggestion)).profile
        assertEquals(100 - suggestion.must - suggestion.want - suggestion.save, freeCoins(p))

        // 6. Задание: ошибочный, затем правильный вариант — оба с объяснением
        val t = nextActiveTask(p, ctx())!!
        val wrong = must(submitTask(p, t.id, worstAnswer(t), ctx()))
        assertTrue(wrong.extra.evaluation.details.isNotEmpty())
        val right = must(submitTask(wrong.profile, t.id, solveTask(t), ctx()))
        p = right.profile
        assertEquals(100 + t.rewards.three, p.wallet)

        // 7. Обязательная и необязательная покупка + попытка покупки при нехватке средств
        p = must(buy(p, "meal", false, ctx())).profile
        p = must(buy(p, "ball", false, ctx())).profile
        val short = err(buy(p, "castle", false, ctx()))
        assertTrue(short.message.contains("Не хватает"))
        assertTrue(short.options!!.size > 1)
        assertEquals(listOf("must", "want"), p.ledger.filter { it.kind == app.finni.kids.domain.TxKind.Purchase }.map { it.dir })

        // 8. Выбор цели и пополнение накоплений, снятие — только с подтверждением
        p = must(selectGoal(p, "book")).profile
        p = must(deposit(p, "book", 20)).profile
        val g = activeGoal(p)!!
        assertEquals("book", g.id); assertEquals(20, g.saved)
        assertFalse(withdraw(p, "book", 5, false) is Outcome.Ok)

        // 9. Обратная связь: баланс, план, состояние питомца
        assertEquals(20, envelope(p, Direction.Save).used)
        assertTrue(p.pet.reason!!.text.isNotEmpty())

        // 10. Переход в новый период; изменение прогресса
        val before = p.pet.growth
        val end = must(endPeriod(p, ctx()))
        p = end.profile
        assertTrue(end.extra.summary.isNotEmpty())
        assertTrue(p.pet.growth > before)
        assertEquals(2, p.period.index)

        // 11. Закрытие и повторный запуск: всё сохраняется
        val saved = serializeState(withProfile(initialApp(), p))
        val reopened = activeProfile(parseState(saved).state)!!
        assertEquals(p, reopened)

        // 12. Раздел взрослого: бонус, сброс
        val bonus = must(grantAdultBonus(reopened, 10, ADULT_BONUS_REASONS[0])).profile
        assertEquals(reopened.wallet + 10, bonus.wallet)
        val reset = resetProgress(bonus, ctx())
        assertEquals(100, reset.wallet); assertTrue(reset.history.isEmpty())
    }
}

class DemoModeTest {
    private fun play(): Pair<app.finni.kids.domain.Profile, List<app.finni.kids.domain.Profile>> {
        var p = makeProfile(true)
        val snaps = mutableListOf(p)
        for (i in 0 until 5) { p = playPeriodAuto(p, ctx("2026-09-${20 + i}")); snaps += p }
        return p to snaps
    }

    @Test fun `5 периодов подряд - рост объясним и не убывает`() {
        val (p, snaps) = play()
        assertEquals(5, p.history.size); assertEquals(6, p.period.index)
        val growth = snaps.map { it.pet.growth }
        for (i in 1 until growth.size) assertTrue(growth[i] >= growth[i - 1])
        assertTrue(p.history.all { it.summary.isNotEmpty() && it.lines.isNotEmpty() })
        for (h in p.history) assertEquals(h.score.total, h.lines.sumOf { it.points ?: 0 })
    }

    @Test fun `питомец меняет стадию как результат серии решений`() {
        val (p, _) = play()
        assertTrue(p.pet.stage >= 3)
        assertTrue((listOf(1) + p.history.map { it.stageAfter }).toSet().size >= 3)
        assertTrue(p.history.any { it.stageAfter > it.stageBefore })
    }

    @Test fun `все девять заданий выполнены и хотя бы одна мечта исполнена`() {
        var (p, _) = play()
        for (t in content.tasks) p = must(submitTask(p, t.id, solveTask(t), ctx())).profile
        assertTrue(p.tasks.values.all { it.stars == 3 })
        assertTrue(p.trophies.size >= 1)
    }

    @Test fun `история операций целостна`() {
        val (p, _) = play()
        var bal = 0
        for (tx in p.ledger) {
            bal += tx.amount
            assertEquals(bal, tx.balance)
            assertTrue(tx.balance >= 0)
            assertTrue(tx.title.isNotEmpty())
        }
        assertEquals(p.wallet, bal)
    }

    @Test fun `детерминирован - два прогона дают одинаковый результат`() {
        val a = play().first.copy(id = "", createdAt = "")
        val b = play().first.copy(id = "", createdAt = "")
        assertEquals(GameJson.encodeToString(a), GameJson.encodeToString(b))
    }

    @Test fun `код ошибки при повторном плане`() {
        val p = must(confirmPlan(makeProfile(), Plan(40, 30, 20))).profile
        assertEquals(ErrorCode.PLAN_ALREADY_CONFIRMED, err(confirmPlan(p, Plan(1, 1, 1))).code)
    }
}
