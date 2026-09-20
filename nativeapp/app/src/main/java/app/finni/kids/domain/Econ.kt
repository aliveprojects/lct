package app.finni.kids.domain

/** Все числа игровой экономики — в одном месте. Они описаны в docs/ECONOMY.md. */
object Econ {
    const val startBalance = 100
    const val weeklyAllowance = 70
    const val dailyGift = 5

    const val statMax = 100

    /** Ниже этого значения питомцу «не хватает» чего-то — он просит заботы. */
    const val statLow = 40

    /** Выше этого значения покупка почти ничего не даёт — подсказываем об этом. */
    const val statHigh = 85

    /** Питомец никогда не опускается ниже этого значения: игра не наказывает. */
    const val statFloor = 25
    fun startStats() = Stats(satiety = 60, care = 60, mood = 65)

    /** Насколько показатели снижаются по итогам периода. */
    fun periodDecay(s: PetStat): Int = when (s) {
        PetStat.Satiety -> 30
        PetStat.Care -> 20
        PetStat.Mood -> 15
    }

    const val planStep = 5
    const val minSaving = 5
    const val firstSavingMood = 6
    const val planKeptMood = 8
    const val goalMood = 40
    const val goalGrowth = 3

    /** Очки роста, с которых начинается стадия 1, 2, 3, 4. */
    val stageFrom = listOf(0, 8, 20, 36)

    const val adultBonusStep = 5
    const val adultBonusCap = 20

    const val customMin = 20
    const val customMax = 200
    const val customStep = 10
    const val customLimit = 4
    const val ledgerView = 30
}

val DIRECTIONS = listOf(Direction.Must, Direction.Want, Direction.Save)
val STATS = listOf(PetStat.Satiety, PetStat.Care, PetStat.Mood)

fun clamp(n: Int, lo: Int, hi: Int): Int = minOf(hi, maxOf(lo, n))

fun stageForGrowth(growth: Int): Int {
    var stage = 1
    Econ.stageFrom.forEachIndexed { i, from -> if (growth >= from) stage = i + 1 }
    return stage
}

/** Сколько очков роста не хватает до следующей стадии (null — стадия последняя). */
fun growthToNext(growth: Int): Int? {
    val stage = stageForGrowth(growth)
    val next = Econ.stageFrom.getOrNull(stage) ?: return null
    return next - growth
}

/** Склонение по-русски: plural(2, "монета", "монеты", "монет") → «монеты». */
fun plural(n: Int, one: String, few: String, many: String): String {
    val a = Math.abs(n) % 100
    val b = a % 10
    return when {
        a in 11..19 -> many
        b in 2..4 -> few
        b == 1 -> one
        else -> many
    }
}

fun coinsText(n: Int): String = "$n ${plural(n, "монета", "монеты", "монет")}"
fun weeksText(n: Int): String = "$n ${plural(n, "неделя", "недели", "недель")}"
fun pointsText(n: Int): String = "$n ${plural(n, "очко", "очка", "очков")}"

fun roundDown5(n: Int): Int = Math.floorDiv(n, 5) * 5
fun roundUp5(n: Int): Int = -Math.floorDiv(-n, 5) * 5

/** Подстановка {имя} в шаблон. */
fun fmt(template: String, vars: Map<String, Any> = emptyMap()): String =
    Regex("\\{(\\w+)\\}").replace(template) { m -> vars[m.groupValues[1]]?.toString() ?: m.value }
