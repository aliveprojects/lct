package app.finni.kids

import app.finni.kids.domain.ADULT_BONUS_REASONS
import app.finni.kids.domain.Direction
import app.finni.kids.domain.Econ
import app.finni.kids.domain.GameJson
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.Plan
import app.finni.kids.domain.Profile
import app.finni.kids.domain.TxKind
import app.finni.kids.domain.CustomGoalInput
import app.finni.kids.domain.buy
import app.finni.kids.domain.claimDaily
import app.finni.kids.domain.completeGoal
import app.finni.kids.domain.confirmPlan
import app.finni.kids.domain.createCustomGoal
import app.finni.kids.domain.deposit
import app.finni.kids.domain.endPeriod
import app.finni.kids.domain.grantAdultBonus
import app.finni.kids.domain.resolveEvent
import app.finni.kids.domain.selectGoal
import app.finni.kids.domain.solveTask
import app.finni.kids.domain.stageForGrowth
import app.finni.kids.domain.submitTask
import app.finni.kids.domain.toggleWishlist
import app.finni.kids.domain.topUpPlan
import app.finni.kids.domain.withdraw
import app.finni.kids.domain.worstAnswer
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Детерминированный генератор (mulberry32): тест воспроизводим, как и в веб-версии. */
private class Rng(seed: Int) {
    private var a = seed
    fun next(): Double {
        a += 0x6d2b79f5
        var t = a
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return ((t xor (t ushr 14)).toLong() and 0xffffffffL).toDouble() / 4294967296.0
    }
}

private fun invariants(p: Profile, prevGrowth: Int) {
    assertTrue(p.wallet >= 0)
    for (g in p.goals) { assertTrue(g.saved >= 0); assertTrue(g.saved <= g.cost) }
    for (v in listOf(p.pet.stats.satiety, p.pet.stats.care, p.pet.stats.mood)) assertTrue(v in 0..Econ.statMax)
    assertTrue(p.pet.growth >= prevGrowth)
    assertEquals(stageForGrowth(p.pet.growth), p.pet.stage)
    // история монет: баланс = сумма всех изменений, каждая запись согласована с предыдущей
    var bal = 0
    for (tx in p.ledger) { bal += tx.amount; assertEquals(bal, tx.balance); assertTrue(tx.title.isNotEmpty()) }
    assertEquals(bal, p.wallet)
    // монеты не появляются из воздуха
    val income = p.ledger.filter { it.kind in listOf(TxKind.Start, TxKind.Allowance, TxKind.Daily, TxKind.Task, TxKind.Bonus) }.sumOf { it.amount }
    val spent = -p.ledger.filter { it.kind == TxKind.Purchase || it.kind == TxKind.Event }.sumOf { it.amount }
    val goalsDone = p.goals.filter { it.done }.sumOf { it.cost }
    val savings = p.goals.sumOf { it.saved }
    assertEquals(income - spent, p.wallet + savings + goalsDone)
    val per = p.period
    for (v in listOf(per.spent.must, per.spent.want, per.spent.event, per.saved, per.withdrawn, per.earned, per.plan.must, per.plan.want, per.plan.save)) assertTrue(v >= 0)
}

class FuzzTest {
    @Test fun `150 случайных последовательностей по 120 действий не ломают экономику`() {
        val okByKind = IntArray(14)
        val items = content.items
        val tasks = content.tasks

        for (seed in 1..150) {
            val r = Rng(seed)
            fun <T> pick(xs: List<T>): T = xs[Math.floor(r.next() * xs.size).toInt()]
            fun int(lo: Int, hi: Int): Int = lo + Math.floor(r.next() * (hi - lo + 1)).toInt()
            var p = makeProfile(r.next() < 0.5)
            var day = 19

            for (step in 0 until 120) {
                val before = GameJson.encodeToString(p)
                val prevGrowth = p.pet.growth
                val c = ctx("2026-09-${(1 + (day % 28)).toString().padStart(2, '0')}")
                val goal = pick(p.goals)
                val kind = int(0, 13)
                val out: Outcome<*> = when (kind) {
                    0 -> claimDaily(p, ctx("2026-10-${(1 + (day++ % 28)).toString().padStart(2, '0')}"))
                    1 -> confirmPlan(p, Plan(int(0, 8) * 5, int(0, 6) * 5, int(0, 6) * 5))
                    2 -> topUpPlan(p, pick(listOf(Direction.Must, Direction.Want, Direction.Save)), int(-1, 6) * 5)
                    3, 4 -> buy(p, pick(items).id, r.next() < 0.5, c)
                    5 -> toggleWishlist(p, pick(items).id, c)
                    6 -> deposit(p, goal.id, int(-1, 12) * 5)
                    7 -> withdraw(p, goal.id, int(-1, 8) * 5, r.next() < 0.7)
                    8 -> completeGoal(p, goal.id)
                    9 -> if (r.next() < 0.5) selectGoal(p, goal.id) else createCustomGoal(p, CustomGoalInput("Подарок другу", "gift", int(1, 21) * 10), c)
                    10 -> { val t = pick(tasks); submitTask(p, t.id, if (r.next() < 0.6) solveTask(t) else worstAnswer(t), c) }
                    11 -> resolveEvent(p, c)
                    12 -> grantAdultBonus(p, int(0, 5) * 5, ADULT_BONUS_REASONS[0])
                    else -> if (r.next() < 0.6) endPeriod(p, c) else claimDaily(p, c)
                }
                when (out) {
                    is Outcome.Ok<*> -> {
                        okByKind[kind]++
                        p = out.profile
                        invariants(p, prevGrowth)
                        assertTrue(out.report.title.isNotEmpty())
                    }
                    is Outcome.Err -> {
                        // отклонённое действие ничего не меняет и объясняется человеку
                        assertEquals("seed $seed step $step kind $kind", before, GameJson.encodeToString(p))
                        assertTrue(out.error.message.isNotEmpty())
                    }
                }
            }
        }

        // тест бесполезен, если почти все действия отклоняются: каждый вид действий должен регулярно проходить
        fun n(k: Int) = okByKind[k]
        assertTrue("подарок дня ${n(0)}", n(0) > 50)
        assertTrue("план ${n(1)}", n(1) > 100)
        assertTrue("покупки ${n(3) + n(4)}", n(3) + n(4) > 500)
        assertTrue("взносы ${n(6)}", n(6) > 300)
        assertTrue("снятия ${n(7)}", n(7) > 50)
        assertTrue("мечты ${n(8)}", n(8) > 3)
        assertTrue("задания ${n(10)}", n(10) > 500)
        assertTrue("недели ${n(13)}", n(13) > 300)
    }
}
