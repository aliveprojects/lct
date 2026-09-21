package app.finni.kids

import app.finni.kids.content.SimTask
import app.finni.kids.content.SortTask
import app.finni.kids.domain.ADULT_BONUS_REASONS
import app.finni.kids.domain.ChangeKind
import app.finni.kids.domain.Direction
import app.finni.kids.domain.Econ
import app.finni.kids.domain.ErrorCode
import app.finni.kids.domain.ExpressionCode
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.Plan
import app.finni.kids.domain.Profile
import app.finni.kids.domain.ReviewStatus
import app.finni.kids.domain.ShortageId
import app.finni.kids.domain.Stats
import app.finni.kids.domain.TaskAnswer
import app.finni.kids.domain.TxKind
import app.finni.kids.domain.adultBonusLeft
import app.finni.kids.domain.buy
import app.finni.kids.domain.claimDaily
import app.finni.kids.domain.confirmPlan
import app.finni.kids.domain.deposit
import app.finni.kids.domain.endPeriod
import app.finni.kids.domain.evaluateTask
import app.finni.kids.domain.grantAdultBonus
import app.finni.kids.domain.isUnlocked
import app.finni.kids.domain.nextActiveTask
import app.finni.kids.domain.petExpression
import app.finni.kids.domain.resolveEvent
import app.finni.kids.domain.simWeeks
import app.finni.kids.domain.solveTask
import app.finni.kids.domain.stageForGrowth
import app.finni.kids.domain.submitTask
import app.finni.kids.domain.worstAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

private val GOOD_BUYS = listOf("meal", "brush", "ball")

private fun playWeek(p0: Profile, plan: Plan = Plan(35, 25, 30), buys: List<String> = emptyList(), save: Int = 0): Profile {
    var p = p0
    if (!p.period.planConfirmed) p = must(confirmPlan(p, plan)).profile
    for (id in buys) p = must(buy(p, id, true, ctx())).profile
    val room = p.goals.first { it.id == "house" }
    val amount = minOf(save, room.cost - room.saved)
    if (amount > 0) p = must(deposit(p, "house", amount)).profile
    return must(endPeriod(p, ctx())).profile
}

private fun goodWeek(p: Profile) = playWeek(p, buys = GOOD_BUYS, save = 30)

class PeriodTest {
    @Test fun `завершить неделю без подтверждённого плана нельзя`() {
        assertEquals(ErrorCode.PLAN_REQUIRED, err(endPeriod(makeProfile(), ctx())).code)
    }

    @Test fun `хорошая неделя - очки считаются по формуле и объясняются построчно`() {
        var p = must(confirmPlan(makeProfile(), Plan(35, 25, 30))).profile
        for (id in GOOD_BUYS) p = must(buy(p, id, false, ctx())).profile
        p = must(deposit(p, "house", 30)).profile
        val r = must(endPeriod(p, ctx()))
        val s = r.extra.score
        assertEquals(listOf(3, 3, 1, 7), listOf(s.needs, s.plan, s.saving, s.total))
        assertEquals(7, r.extra.lines.sumOf { it.points ?: 0 })
        assertEquals(listOf(22, 12, 30), listOf(r.extra.fact.must, r.extra.fact.want, r.extra.fact.save))
        assertEquals(7, r.profile.pet.growth)
        assertEquals(1, r.profile.history.size)
        assertEquals(2, r.profile.period.index); assertFalse(r.profile.period.planConfirmed)
        assertEquals(36 + Econ.weeklyAllowance, r.profile.wallet)
        val last = r.profile.ledger.last()
        assertEquals(TxKind.Allowance, last.kind); assertEquals(Econ.weeklyAllowance, last.amount)
        assertTrue(r.report.changes.any { it.kind == ChangeKind.Growth })
    }

    @Test fun `регулярность - вторая неделя с копилкой подряд даёт очко и стадию`() {
        var p = goodWeek(makeProfile())
        assertFalse(p.history[0].checks.regular)
        assertEquals(1, p.pet.stage)
        p = goodWeek(p)
        assertTrue(p.history[1].checks.regular)
        assertEquals(8, p.history[1].score.total)
        assertEquals(15, p.pet.growth)
        assertEquals(2, p.pet.stage)
        assertEquals(1, p.history[1].stageBefore); assertEquals(2, p.history[1].stageAfter)
        assertTrue(p.history[1].summary.contains("Подросток"))
    }

    @Test fun `пустая неделя не обнуляет прогресс`() {
        var p = goodWeek(makeProfile())
        val growth = p.pet.growth
        repeat(12) {
            p = playWeek(p, plan = Plan(30, 20, 20))
            assertTrue(p.pet.growth >= growth)
            for (v in listOf(p.pet.stats.satiety, p.pet.stats.care, p.pet.stats.mood)) assertTrue(v >= Econ.statFloor)
        }
        assertEquals(0, p.history.last().score.total)
        assertTrue(p.history.last().lines.any { it.tip != null })
    }

    @Test fun `перерасход Хочется замечается но объясняется мягко`() {
        val p = playWeek(makeProfile(), plan = Plan(35, 10, 30), buys = listOf("meal", "brush", "mouse"), save = 30)
        val h = p.history[0]
        assertFalse(h.checks.wantWithin)
        assertEquals(2, h.score.plan)
        val line = h.lines.first { it.id == "want" }
        assertEquals(ReviewStatus.Part, line.status)
        assertTrue(line.tip!!.contains("не ошибка"))
    }

    @Test fun `без еды и ухода питомец просит заботы но не болеет`() {
        val p = playWeek(makeProfile(), plan = Plan(30, 20, 20))
        assertEquals(false, p.history[0].needs.food); assertEquals(false, p.history[0].needs.care)
        assertEquals(Stats(30, 40, 50), p.pet.stats)
        val e = petExpression(p.pet.stats)
        assertEquals(ExpressionCode.Hungry, e.code)
        assertTrue(e.label.isNotEmpty()); assertTrue(e.hint.contains("магазин"))
    }

    @Test fun `стадии роста монотонны`() {
        assertTrue(Econ.stageFrom.size >= 3)
        var prev = 0
        for (g in 0..60) { val s = stageForGrowth(g); assertTrue(s >= prev); prev = s }
        assertEquals(listOf(1, 2, 3, 4), listOf(0, 8, 20, 36).map { stageForGrowth(it) })
    }

    @Test fun `непредвиденное событие приходит на 3-й неделе`() {
        var p = goodWeek(goodWeek(makeProfile()))
        assertEquals("vet", p.pendingEventId)
        val care = p.pet.stats.care
        val wallet = p.wallet
        val r = must(resolveEvent(p, ctx()))
        p = r.profile
        assertEquals(wallet - 15, p.wallet)
        assertEquals(15, p.period.spent.event)
        assertEquals(minOf(100, care + 20), p.pet.stats.care)
        assertNull(p.pendingEventId)
        assertTrue(r.report.explain.isNotEmpty())
        assertFalse(resolveEvent(p, ctx()) is Outcome.Ok)
    }

    @Test fun `событие при нехватке монет не блокирует игру`() {
        val p = goodWeek(goodWeek(makeProfile()))
        p.wallet = 3
        val e = err(resolveEvent(p, ctx()))
        assertTrue(e.options!!.map { it.id }.contains(ShortageId.Earn))
    }

    @Test fun `трата на событие не считается нарушением плана`() {
        var p = goodWeek(goodWeek(makeProfile()))
        p = must(resolveEvent(p, ctx())).profile
        p = goodWeek(p)
        assertEquals(15, p.history[2].fact.event)
        assertTrue(p.history[2].checks.mustWithin)
    }

    @Test fun `подарок дня - один раз в сутки без штрафов за пропуск`() {
        var p = makeProfile()
        p = must(claimDaily(p, ctx("2026-09-19"))).profile
        assertEquals(100 + Econ.dailyGift, p.wallet)
        assertEquals(TxKind.Daily, p.ledger.last().kind)
        assertFalse(claimDaily(p, ctx("2026-09-19")) is Outcome.Ok)
        assertTrue(claimDaily(p, ctx("2026-09-25")) is Outcome.Ok)
    }

    @Test fun `бонус взрослого - шаг 5 и не больше 20 за неделю`() {
        var p = makeProfile()
        assertEquals(20, adultBonusLeft(p))
        p = must(grantAdultBonus(p, 10, ADULT_BONUS_REASONS[0])).profile
        assertFalse(grantAdultBonus(p, 15, ADULT_BONUS_REASONS[0]) is Outcome.Ok)
        assertFalse(grantAdultBonus(p, 7, ADULT_BONUS_REASONS[0]) is Outcome.Ok)
        p = must(grantAdultBonus(p, 10, ADULT_BONUS_REASONS[1])).profile
        assertEquals(0, adultBonusLeft(p))
        assertEquals(120, p.wallet)
        assertTrue(p.ledger.last().title.contains("Бонус от взрослого"))
        assertEquals(20, adultBonusLeft(goodWeek(p)))
    }
}

class TasksTest {
    @Test fun `каждое задание - правильный вариант даёт 3 звезды, ошибочный даёт объяснение и путь исправления`() {
        for (def in content.tasks) {
            val good = evaluateTask(def, solveTask(def))
            assertEquals("${def.id}: правильный", 3, good.stars)
            assertTrue(good.summary.isNotEmpty() && good.details.isNotEmpty() && good.details.all { it.text.isNotEmpty() })
            assertEquals(def.learn, good.learn)
            val bad = evaluateTask(def, worstAnswer(def))
            assertEquals("${def.id}: ошибочный", 1, bad.stars)
            assertTrue(bad.summary.isNotEmpty() && bad.details.isNotEmpty())
            assertTrue("${def.id}: нет recovery", !bad.recovery.isNullOrEmpty())
        }
    }

    @Test fun `частично верный ответ - не провал`() {
        val two = evaluateTask(task("calc-change"), TaskAnswer.Calc(mapOf("q1" to 24, "q2" to 7, "q3" to 5)))
        assertEquals(2, two.stars)
        assertTrue(two.details[1].sub!!.contains("Правильно: 6"))
        val sort = task("sort-need-want") as SortTask
        val mostly = TaskAnswer.Sort(sort.items.withIndex().associate { (k, i) -> i.id to (if (k < 5) i.group else if (i.group == "need") "want" else "need") })
        assertEquals(2, evaluateTask(sort, mostly).stars)
    }

    @Test fun `тип ответа должен совпадать с типом задания`() {
        assertThrows(IllegalArgumentException::class.java) { evaluateTask(task("calc-weeks"), TaskAnswer.Sim(5)) }
    }

    @Test fun `награда выдаётся один раз а за улучшение доплачивается разница`() {
        val def = task("calc-weeks")
        var p = makeProfile(true)
        val start = p.wallet
        val bad = must(submitTask(p, def.id, worstAnswer(def), ctx()))
        p = bad.profile
        assertEquals(def.rewards.one, bad.extra.extra)
        assertEquals(1, bad.extra.evaluation.stars)
        assertEquals(start + def.rewards.one, p.wallet)

        val good = must(submitTask(p, def.id, solveTask(def), ctx()))
        p = good.profile
        assertTrue(good.extra.improved)
        assertEquals(def.rewards.three - def.rewards.one, good.extra.extra)
        assertEquals(start + def.rewards.three, p.wallet)
        val prog = p.tasks.getValue(def.id)
        assertEquals(listOf(3, 2, def.rewards.three), listOf(prog.stars, prog.attempts, prog.paid))

        val again = must(submitTask(p, def.id, solveTask(def), ctx()))
        assertEquals(0, again.extra.extra)
        assertEquals(p.wallet, again.profile.wallet)
        assertEquals(2, again.profile.ledger.count { it.kind == TxKind.Task })
    }

    @Test fun `начисление за задание попадает в историю`() {
        val def = task("sort-need-want")
        val p = must(submitTask(makeProfile(true), def.id, solveTask(def), ctx())).profile
        val tx = p.ledger.last()
        assertEquals(TxKind.Task, tx.kind); assertEquals("Задание «${def.title}»", tx.title)
        assertEquals(def.rewards.three, tx.amount); assertEquals(p.wallet, tx.balance)
    }

    @Test fun `задания открываются по неделям а в демо-режиме сразу все`() {
        val normal = makeProfile(false)
        val demo = makeProfile(true)
        val late = content.tasks.filter { it.unlockPeriod > 1 }
        assertTrue(late.isNotEmpty())
        for (t in late) { assertFalse(isUnlocked(t, normal)); assertTrue(isUnlocked(t, demo)) }
        assertEquals(ErrorCode.TASK_LOCKED, err(submitTask(normal, late[0].id, solveTask(late[0]), ctx())).code)
        assertTrue(content.tasks.count { isUnlocked(it, normal) } >= 3)
    }

    @Test fun `активное задание - сначала новое потом то где можно лучше`() {
        var p = makeProfile(true)
        val first = nextActiveTask(p, ctx())!!
        p = must(submitTask(p, first.id, solveTask(first), ctx())).profile
        assertTrue(nextActiveTask(p, ctx())!!.id != first.id)
        for (t in content.tasks) p = must(submitTask(p, t.id, solveTask(t), ctx())).profile
        assertNull(nextActiveTask(p, ctx()))
    }

    @Test fun `сумма в распредели не может превышать доступное`() {
        val ev = evaluateTask(task("budget-week"), TaskAnswer.Allocate(Plan(40, 30, 30)))
        assertEquals(1, ev.stars); assertTrue(ev.summary.contains("больше"))
    }

    @Test fun `корзина - нельзя оплатить больше бюджета и забыть нужное`() {
        val def = task("cart-zoo")
        assertEquals(1, evaluateTask(def, TaskAnswer.Cart(listOf("food", "soap", "ball", "mouse"))).stars)
        assertEquals(1, evaluateTask(def, TaskAnswer.Cart(listOf("ball"))).stars)
        assertEquals(2, evaluateTask(def, TaskAnswer.Cart(listOf("food", "soap", "ball"))).stars)
    }

    @Test fun `симуляция накоплений - срок это осталось на взнос`() {
        val def = task("sim-house") as SimTask
        assertEquals(4, simWeeks(def, 15)); assertEquals(6, simWeeks(def, 10)); assertNull(simWeeks(def, 0))
        assertEquals(2, evaluateTask(def, TaskAnswer.Sim(10)).stars)
        assertEquals(1, evaluateTask(def, TaskAnswer.Sim(5)).stars)
    }

    @Test fun `задание не меняет конверты`() {
        val p0 = must(confirmPlan(makeProfile(true), Plan(40, 30, 20))).profile
        val def = task("calc-change")
        val p = must(submitTask(p0, def.id, solveTask(def), ctx())).profile
        assertEquals(p0.period.plan, p.period.plan)
        assertEquals(Direction.Must, Direction.entries.first())
    }
}
