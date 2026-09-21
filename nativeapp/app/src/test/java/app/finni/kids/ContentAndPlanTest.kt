package app.finni.kids

import app.finni.kids.content.validateContent
import app.finni.kids.domain.Direction
import app.finni.kids.domain.ErrorCode
import app.finni.kids.domain.Econ
import app.finni.kids.domain.Plan
import app.finni.kids.domain.PlanProblem
import app.finni.kids.domain.TxKind
import app.finni.kids.domain.confirmPlan
import app.finni.kids.domain.envelope
import app.finni.kids.domain.freeCoins
import app.finni.kids.domain.needsCost
import app.finni.kids.domain.suggestPlan
import app.finni.kids.domain.topUpPlan
import app.finni.kids.domain.validatePlan
import app.finni.kids.domain.GameJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTest {
    @Test fun `проходит проверку целостности`() = assertEquals(emptyList<String>(), validateContent(content))

    @Test fun `минимальный объём демонстрационного контента из ТЗ`() {
        val must = content.items.filter { it.kind == Direction.Must }
        val want = content.items.filter { it.kind == Direction.Want }
        assertTrue(content.items.size >= 8 && must.isNotEmpty() && want.isNotEmpty())
        assertTrue(content.goals.size >= 3)
        assertTrue(content.tasks.size >= 6)
        val topics = content.tasks.map { it.topic }.toSet()
        assertEquals(3, topics.size)
        assertEquals(content.topics.map { it.id }.toSet(), topics)
        assertTrue(content.pet.stages.size >= 3)
    }

    @Test fun `не менее 9 комбинаций питомца`() {
        val starts = content.pet.accessories.count { it.atStart }
        assertTrue(content.pet.species.size * content.pet.colors.size * starts >= 9)
    }

    @Test fun `задания не ограничиваются выбором ответа из списка`() {
        val types = content.tasks.map { it::class.simpleName }.toSet()
        assertTrue(types.size >= 4)
        assertTrue(types.any { it != "StoryTask" })
    }

    @Test fun `питомец-талисман Финни`() {
        assertEquals("Финни", content.pet.defaultPetName)
        assertTrue("Финни" in content.pet.petNames)
        assertTrue(content.pet.defaultPetName.length in 2..12)
    }

    @Test fun `у обязательных товаров есть еда и уход`() {
        val needs = content.items.filter { it.kind == Direction.Must }.map { it.need }.toSet()
        assertTrue("food" in needs && "care" in needs)
    }

    @Test fun `валидатор действительно ловит ошибки`() {
        val broken = content.copy(items = content.items + content.items[0])
        assertTrue(validateContent(broken).any { it.contains("повторяется") })
    }
}

class PlanTest {
    private val plan = Plan(40, 30, 20)

    @Test fun `стартовый профиль получает стартовый бюджет`() {
        val p = makeProfile()
        assertEquals(Econ.startBalance, p.wallet)
        val tx = p.ledger[0]
        assertEquals(TxKind.Start, tx.kind)
        assertEquals("Стартовый подарок", tx.title)
        assertEquals(Econ.startBalance, tx.amount)
        assertEquals(Econ.startBalance, tx.balance)
    }

    @Test fun `validatePlan показывает остаток и не даёт превысить бюджет`() {
        assertEquals(true to 10, validatePlan(100, Plan(40, 30, 20)).let { it.ok to it.remainder })
        assertEquals(PlanProblem.Over, validatePlan(100, Plan(60, 30, 20)).problem)
        assertEquals(-10, validatePlan(100, Plan(60, 30, 20)).remainder)
        assertEquals(PlanProblem.Empty, validatePlan(100, Plan(0, 0, 0)).problem)
        assertEquals(PlanProblem.Invalid, validatePlan(100, Plan(-5, 30, 20)).problem)
    }

    @Test fun `confirmPlan подтверждает и фиксирует стартовый баланс периода`() {
        val p = must(confirmPlan(makeProfile(), plan)).profile
        assertTrue(p.period.planConfirmed)
        assertEquals(100, p.period.startBalance)
        assertEquals(Plan(40, 30, 20), p.period.plan)
        assertEquals(10, freeCoins(p))
    }

    @Test fun `превышение бюджета отклоняется и сообщает на сколько`() {
        val e = err(confirmPlan(makeProfile(), Plan(80, 30, 20)))
        assertEquals(ErrorCode.PLAN_OVER_BUDGET, e.code)
        assertEquals(30, e.overBy)
    }

    @Test fun `план нельзя переподтвердить`() {
        val p = must(confirmPlan(makeProfile(), plan)).profile
        assertEquals(ErrorCode.PLAN_ALREADY_CONFIRMED, err(confirmPlan(p, Plan(10, 10, 10))).code)
    }

    @Test fun `confirmPlan не изменяет исходный профиль`() {
        val p0 = makeProfile()
        val snapshot = GameJson.encodeToString(p0)
        must(confirmPlan(p0, plan))
        assertEquals(snapshot, GameJson.encodeToString(p0))
    }

    @Test fun `suggestPlan укладывается в бюджет и покрывает минимум нужного`() {
        val p = makeProfile()
        val s = suggestPlan(p, content)
        assertTrue(s.must + s.want + s.save <= p.wallet)
        assertTrue(s.must >= needsCost(content).total)
        assertTrue(validatePlan(p.wallet, s).ok)
    }

    @Test fun `topUpPlan добавляет свободные монеты в конверт, больше нельзя`() {
        val p = must(confirmPlan(makeProfile(), plan)).profile // запас 10
        val t = must(topUpPlan(p, Direction.Want, 5)).profile
        assertEquals(35, t.period.plan.want)
        assertEquals(35, envelope(t, Direction.Want).left)
        assertEquals(ErrorCode.NOT_ENOUGH, err(topUpPlan(p, Direction.Want, 15)).code)
        assertFalse(topUpPlan(makeProfile(), Direction.Want, 5) is app.finni.kids.domain.Outcome.Ok)
        assertNotEquals(p.period.plan, t.period.plan)
    }
}
