package app.finni.kids

import app.finni.kids.domain.ChangeKind
import app.finni.kids.domain.Direction
import app.finni.kids.domain.ErrorCode
import app.finni.kids.domain.GameJson
import app.finni.kids.domain.Plan
import app.finni.kids.domain.PetStat
import app.finni.kids.domain.Profile
import app.finni.kids.domain.ShortageId
import app.finni.kids.domain.TxKind
import app.finni.kids.domain.activeGoal
import app.finni.kids.domain.averageDeposit
import app.finni.kids.domain.buy
import app.finni.kids.domain.completeGoal
import app.finni.kids.domain.confirmPlan
import app.finni.kids.domain.createCustomGoal
import app.finni.kids.domain.CustomGoalInput
import app.finni.kids.domain.deposit
import app.finni.kids.domain.envelope
import app.finni.kids.domain.etaFor
import app.finni.kids.domain.goalById
import app.finni.kids.domain.previewPurchase
import app.finni.kids.domain.previewWithdraw
import app.finni.kids.domain.savingsTotal
import app.finni.kids.domain.selectGoal
import app.finni.kids.domain.toggleWishlist
import app.finni.kids.domain.withdraw
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopTest {
    private fun planned(plan: Plan = Plan(40, 30, 20)): Profile = must(confirmPlan(makeProfile(), plan)).profile

    @Test fun `обязательная покупка списывает монеты, меняет показатель и пишет историю`() {
        val r = must(buy(planned(), "meal", false, ctx()))
        val p = r.profile
        assertEquals(85, p.wallet)
        assertEquals(15, p.period.spent.must)
        assertEquals(1, p.period.needs.food)
        assertEquals(90, p.pet.stats.satiety)
        val tx = p.ledger.last()
        assertEquals(TxKind.Purchase, tx.kind); assertEquals("Обед", tx.title); assertEquals(-15, tx.amount); assertEquals(85, tx.balance); assertEquals("must", tx.dir)
        val kinds = r.report.changes.map { it.kind }
        assertTrue(kinds.containsAll(listOf(ChangeKind.Wallet, ChangeKind.Stat, ChangeKind.Plan)))
        assertTrue(r.report.changes.all { it.why.isNotEmpty() })
        assertTrue(p.pet.reason!!.text.contains("Сытость +30"))
    }

    @Test fun `необязательная покупка списывает из конверта Хочется`() {
        val p = must(buy(planned(), "ball", false, ctx())).profile
        assertEquals(12, p.period.spent.want)
        assertEquals(18, envelope(p, Direction.Want).left)
        assertEquals(90, p.pet.stats.mood)
    }

    @Test fun `до подтверждения плана покупать нельзя`() {
        assertEquals(ErrorCode.PLAN_REQUIRED, err(buy(makeProfile(), "meal", false, ctx())).code)
    }

    @Test fun `нехватка средств - покупка отклонена, баланс не меняется, есть варианты`() {
        val p0 = planned()
        val before = GameJson.encodeToString(p0)
        val e = err(buy(p0, "castle", false, ctx())) // 120 > 100
        assertEquals(ErrorCode.NOT_ENOUGH, e.code)
        assertEquals(20, e.missing)
        val ids = e.options!!.map { it.id }
        assertTrue(ids.containsAll(listOf(ShortageId.Earn, ShortageId.Cheaper, ShortageId.Wishlist, ShortageId.Wait)))
        assertEquals(before, GameJson.encodeToString(p0))
    }

    @Test fun `баланс никогда не уходит в минус`() {
        var p = planned(Plan(40, 40, 20))
        repeat(40) {
            for (it in content.items) {
                val r = buy(p, it.id, true, ctx())
                if (r is app.finni.kids.domain.Outcome.Ok) p = r.profile
                assertTrue(p.wallet >= 0)
            }
        }
    }

    @Test fun `покупка Хочется сверх плана требует подтверждения`() {
        val p = planned(Plan(40, 10, 20))
        val e = err(buy(p, "ball", false, ctx())) // 12 > 10
        assertEquals(ErrorCode.CONFIRM_OVER_PLAN, e.code)
        assertEquals(2, e.overBy)
        val ok = must(buy(p, "ball", true, ctx()))
        assertEquals(12, ok.profile.period.spent.want)
        assertTrue(ok.report.explain.contains("превышен"))
    }

    @Test fun `украшение надевается сразу и покупается один раз`() {
        val p = must(buy(planned(), "hat", false, ctx())).profile
        assertEquals("hat", p.pet.appearance.accessory)
        assertTrue("hat" in p.pet.ownedAccessories)
        assertEquals(ErrorCode.ALREADY_OWNED, err(buy(p, "hat", false, ctx())).code)
    }

    @Test fun `показатели ограничены 100, предпросмотр предупреждает о лишнем`() {
        var p = planned()
        p = must(buy(p, "meal", false, ctx())).profile // 60 → 90
        val pv = previewPurchase(p, item("meal"))
        val e = pv.effects[0]
        assertEquals(listOf(90, 100, 10, 20), listOf(e.before, e.after, e.gain, e.wasted))
        assertTrue(pv.wasteful)
        assertEquals(100, must(buy(p, "meal", false, ctx())).profile.pet.stats.satiety)
    }

    @Test fun `предпросмотр показывает цену категорию и влияние до покупки`() {
        val pv = previewPurchase(planned(), item("bath"))
        assertEquals(10, pv.price); assertEquals(Direction.Must, pv.kind); assertEquals("Нужное", pv.categoryLabel)
        assertTrue(pv.affordable); assertEquals(90, pv.balanceAfter)
        assertEquals(PetStat.Care, pv.effects[0].stat); assertEquals(30, pv.effects[0].gain)
    }

    @Test fun `список желаний`() {
        var p = must(toggleWishlist(planned(), "ball", ctx())).profile
        assertEquals(listOf("ball"), p.wishlist)
        p = must(buy(p, "ball", false, ctx())).profile
        assertEquals(emptyList<String>(), p.wishlist)
        p = must(toggleWishlist(p, "cake", ctx())).profile
        p = must(toggleWishlist(p, "cake", ctx())).profile
        assertEquals(emptyList<String>(), p.wishlist)
    }
}

class SavingsTest {
    private fun planned(): Profile = must(confirmPlan(makeProfile(), Plan(30, 20, 40))).profile

    @Test fun `взнос переводит монеты из кошелька в цель`() {
        val r = must(deposit(planned(), "house", 20))
        val p = r.profile
        assertEquals(80, p.wallet)
        assertEquals(20, goalById(p, "house")!!.saved)
        assertEquals(20, savingsTotal(p)); assertEquals(20, p.period.saved)
        val tx = p.ledger.last()
        assertEquals(TxKind.Save, tx.kind); assertEquals(-20, tx.amount); assertEquals(20, tx.savingsDelta)
        assertTrue(r.report.changes.map { it.kind }.containsAll(listOf(ChangeKind.Wallet, ChangeKind.Savings)))
    }

    @Test fun `первый взнос от 5 монет за неделю радует питомца`() {
        assertEquals(71, must(deposit(planned(), "house", 10)).profile.pet.stats.mood)
    }

    @Test fun `нельзя отложить больше чем есть или чем осталось до цели`() {
        val p = planned()
        assertEquals(ErrorCode.NOT_ENOUGH, err(deposit(p, "house", 500)).code)
        val e = err(deposit(must(deposit(p, "house", 50)).profile, "house", 20))
        assertEquals(ErrorCode.GOAL_FULL, e.code); assertEquals(10, e.max)
        assertFalse(deposit(makeProfile(), "house", 10) is app.finni.kids.domain.Outcome.Ok)
    }

    @Test fun `снятие без подтверждения невозможно и ничего не меняет`() {
        val p = must(deposit(planned(), "house", 30)).profile
        assertEquals(ErrorCode.CONFIRM_REQUIRED, err(withdraw(p, "house", 10, false)).code)
        assertEquals(30, goalById(p, "house")!!.saved)
    }

    @Test fun `снятие с подтверждением возвращает монеты в кошелёк`() {
        val p = must(deposit(planned(), "house", 30)).profile
        val r = must(withdraw(p, "house", 10, true))
        assertEquals(80, r.profile.wallet)
        assertEquals(20, goalById(r.profile, "house")!!.saved)
        assertEquals(10, r.profile.period.withdrawn)
        val tx = r.profile.ledger.last()
        assertEquals(TxKind.Withdraw, tx.kind); assertEquals(10, tx.amount); assertEquals(-10, tx.savingsDelta)
        assertFalse(withdraw(p, "house", 31, true) is app.finni.kids.domain.Outcome.Ok)
    }

    @Test fun `предпросмотр снятия показывает накопления и срок до подтверждения`() {
        val p = planned()
        val g = goalById(p, "house")!!
        g.saved = 20; g.deposits.clear(); g.deposits[1] = 20
        p.period = p.period.copy(index = 2)
        val pv = previewWithdraw(p, "house", 10)!!
        assertEquals(listOf(20, 10, 40, 50), listOf(pv.savedBefore, pv.savedAfter, pv.remainingBefore, pv.remainingAfter))
        assertEquals(2, pv.etaBefore.weeks)
        assertEquals(3, pv.etaAfter.weeks)
    }

    @Test fun `срок цели считается по среднему взносу за прошлые недели`() {
        val g = goalById(makeProfile(), "house")!!.copy(saved = 10, deposits = mutableMapOf(1 to 10, 2 to 20, 3 to 5))
        assertEquals(15, averageDeposit(g, 3))
        assertEquals(12, averageDeposit(g, 1))
        assertEquals(4, etaFor(g, 3).weeks)
        val none = etaFor(g.copy(deposits = mutableMapOf()), 1)
        assertNull(none.avg); assertNull(none.weeks)
    }

    @Test fun `исполнение мечты списывает цену даёт трофей рост и переключает цель`() {
        val p = planned()
        assertFalse(completeGoal(p, "house") is app.finni.kids.domain.Outcome.Ok)
        p.wallet = 200
        goalById(p, "house")!!.saved = 60
        val r = must(completeGoal(p, "house"))
        val g = goalById(r.profile, "house")!!
        assertTrue(g.done); assertEquals(0, g.saved)
        assertTrue("house" in r.profile.trophies)
        assertEquals(3, r.profile.pet.growth)
        assertEquals("book", r.profile.activeGoalId)
        assertFalse(completeGoal(r.profile, "house") is app.finni.kids.domain.Outcome.Ok)
    }

    @Test fun `выбор цели и создание своей`() {
        val p = planned()
        assertEquals("park", activeGoal(must(selectGoal(p, "park")).profile)!!.id)
        val c = must(createCustomGoal(p, CustomGoalInput("Подарок другу", "gift", 50), ctx())).profile
        val g = activeGoal(c)!!
        assertTrue(g.custom); assertEquals(50, g.cost); assertEquals("Подарок другу", g.title)
        assertNotNull(g)
        assertFalse(createCustomGoal(p, CustomGoalInput("Свой текст", "gift", 50), ctx()) is app.finni.kids.domain.Outcome.Ok)
        assertFalse(createCustomGoal(p, CustomGoalInput("Подарок другу", "gift", 55), ctx()) is app.finni.kids.domain.Outcome.Ok)
        assertFalse(createCustomGoal(p, CustomGoalInput("Подарок другу", "gift", 500), ctx()) is app.finni.kids.domain.Outcome.Ok)
    }
}
