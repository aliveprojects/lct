package app.finni.kids.domain

import app.finni.kids.content.Content
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Типы игровой модели. Здесь нет ни интерфейса, ни хранилища — только данные.
// Профиль — «простые» изменяемые данные: действия движка сначала копируют его (deepCopy), потом меняют копию.

@Serializable
enum class Direction(val key: String) {
    @SerialName("must") Must("must"),
    @SerialName("want") Want("want"),
    @SerialName("save") Save("save"),
}

@Serializable
enum class PetStat {
    @SerialName("satiety") Satiety,
    @SerialName("care") Care,
    @SerialName("mood") Mood,
}

@Serializable
enum class Topic {
    @SerialName("budget") Budget,
    @SerialName("saving") Saving,
    @SerialName("purchases") Purchases,
}

/** Три числа по конвертам плана: нужное, хочется, копилка. */
@Serializable
data class Plan(var must: Int = 0, var want: Int = 0, var save: Int = 0) {
    operator fun get(d: Direction): Int = when (d) {
        Direction.Must -> must
        Direction.Want -> want
        Direction.Save -> save
    }

    operator fun set(d: Direction, v: Int) {
        when (d) {
            Direction.Must -> must = v
            Direction.Want -> want = v
            Direction.Save -> save = v
        }
    }
}

/** Показатели питомца. */
@Serializable
data class Stats(var satiety: Int = 0, var care: Int = 0, var mood: Int = 0) {
    operator fun get(s: PetStat): Int = when (s) {
        PetStat.Satiety -> satiety
        PetStat.Care -> care
        PetStat.Mood -> mood
    }

    operator fun set(s: PetStat, v: Int) {
        when (s) {
            PetStat.Satiety -> satiety = v
            PetStat.Care -> care = v
            PetStat.Mood -> mood = v
        }
    }
}

@Serializable
data class PetAppearance(var species: String, var color: String, var accessory: String)

@Serializable
data class PetReason(val text: String, val stat: PetStat? = null, val delta: Int? = null)

@Serializable
data class Pet(
    var name: String,
    var appearance: PetAppearance,
    var stats: Stats,
    /** Очки роста: копятся по итогам периодов и никогда не уменьшаются. */
    var growth: Int = 0,
    var stage: Int = 1,
    var ownedAccessories: MutableList<String> = mutableListOf(),
    var reason: PetReason? = null,
)

@Serializable
enum class TxKind {
    @SerialName("start") Start,
    @SerialName("allowance") Allowance,
    @SerialName("daily") Daily,
    @SerialName("task") Task,
    @SerialName("bonus") Bonus,
    @SerialName("purchase") Purchase,
    @SerialName("event") Event,
    @SerialName("save") Save,
    @SerialName("withdraw") Withdraw,
    @SerialName("goal") Goal,
}

/** Запись в истории. amount — изменение кошелька; savingsDelta — изменение накоплений. dir: must/want/save/event. */
@Serializable
data class Tx(
    val id: Int,
    val period: Int,
    val kind: TxKind,
    val title: String,
    val amount: Int,
    val savingsDelta: Int? = null,
    val balance: Int,
    val dir: String? = null,
    val itemId: String? = null,
)

@Serializable
data class Goal(
    val id: String,
    val title: String,
    val icon: String,
    val cost: Int,
    var saved: Int = 0,
    /** Сколько положено в цель в каждом периоде (без учёта снятий). */
    val deposits: MutableMap<Int, Int> = mutableMapOf(),
    val custom: Boolean = false,
    var done: Boolean = false,
)

@Serializable
data class Spent(var must: Int = 0, var want: Int = 0, var event: Int = 0)

@Serializable
data class NeedsCount(var food: Int = 0, var care: Int = 0)

@Serializable
data class PeriodState(
    val index: Int,
    var planConfirmed: Boolean = false,
    var startBalance: Int = 0,
    var plan: Plan = Plan(),
    val spent: Spent = Spent(),
    val needs: NeedsCount = NeedsCount(),
    var saved: Int = 0,
    var withdrawn: Int = 0,
    var earned: Int = 0,
    var adultBonus: Int = 0,
)

@Serializable
enum class ReviewStatus {
    @SerialName("ok") Ok,
    @SerialName("part") Part,
    @SerialName("todo") Todo,
}

@Serializable
data class ReviewLine(val id: String, val status: ReviewStatus, val text: String, val points: Int? = null, val tip: String? = null)

@Serializable
data class Fact(val must: Int, val want: Int, val save: Int, val event: Int, val earned: Int)

@Serializable
data class NeedsDone(val food: Boolean, val care: Boolean)

@Serializable
data class Checks(val mustWithin: Boolean, val wantWithin: Boolean, val savedSome: Boolean, val savedPlan: Boolean, val regular: Boolean)

@Serializable
data class Score(val needs: Int, val plan: Int, val saving: Int, val total: Int)

@Serializable
data class PeriodResult(
    val index: Int,
    val plan: Plan,
    val fact: Fact,
    val needs: NeedsDone,
    val checks: Checks,
    val score: Score,
    val growthBefore: Int,
    val growthAfter: Int,
    val stageBefore: Int,
    val stageAfter: Int,
    val balanceEnd: Int,
    val statsBefore: Stats,
    val statsAfter: Stats,
    val lines: List<ReviewLine>,
    val summary: String,
)

@Serializable
data class TaskProgress(var stars: Int = 0, var attempts: Int = 0, var paid: Int = 0)

@Serializable
data class Profile(
    val id: String,
    val playerName: String,
    val createdAt: String,
    val isDemo: Boolean = false,
    var pet: Pet,
    var wallet: Int,
    val goals: MutableList<Goal> = mutableListOf(),
    var activeGoalId: String? = null,
    var period: PeriodState,
    val history: MutableList<PeriodResult> = mutableListOf(),
    val ledger: MutableList<Tx> = mutableListOf(),
    var nextTxId: Int = 1,
    val tasks: MutableMap<String, TaskProgress> = mutableMapOf(),
    var wishlist: MutableList<String> = mutableListOf(),
    var dailyGiftDate: String? = null,
    var pendingEventId: String? = null,
    val resolvedEvents: MutableList<String> = mutableListOf(),
    val trophies: MutableList<String> = mutableListOf(),
)

@Serializable
enum class TextScale {
    @SerialName("normal") Normal,
    @SerialName("large") Large,
    @SerialName("xlarge") XLarge,
}

@Serializable
data class Settings(
    /** Фоновая музыка (отдельно от звуков-сигналов). */
    val music: Boolean = true,
    val sound: Boolean = true,
    val animations: Boolean = true,
    val textScale: TextScale = TextScale.Normal,
    val highContrast: Boolean = false,
)

@Serializable
enum class Mode {
    @SerialName("normal") Normal,
    @SerialName("demo") Demo,
}

@Serializable
data class AppState(
    val schema: Int = 1,
    val settings: Settings = Settings(),
    val introSeen: Boolean = false,
    val mode: Mode = Mode.Normal,
    val main: Profile? = null,
    val demo: Profile? = null,
    /** Виртуальный сдвиг календаря в демо-режиме (дни), чтобы не ждать «подарок дня». */
    val demoDayOffset: Int = 0,
)

// ---------- Результат действий ----------

enum class ErrorCode {
    PLAN_REQUIRED, PLAN_ALREADY_CONFIRMED, PLAN_INVALID, PLAN_OVER_BUDGET, NOT_ENOUGH, CONFIRM_OVER_PLAN,
    CONFIRM_REQUIRED, ALREADY_OWNED, GOAL_NOT_FOUND, GOAL_FULL, GOAL_NOT_REACHED, GOAL_DONE, INVALID_AMOUNT,
    TASK_LOCKED, TASK_NOT_FOUND, ITEM_NOT_FOUND, ALREADY_TODAY, BONUS_LIMIT, NO_EVENT, NO_PROFILE,
}

enum class ShortageId { Earn, Cheaper, Wishlist, Savings, Wait }

data class ShortageOption(
    val id: ShortageId,
    val label: String,
    val hint: String,
    val route: String? = null,
    val itemIds: List<String>? = null,
)

data class GameError(
    val code: ErrorCode,
    val message: String,
    val missing: Int? = null,
    val overBy: Int? = null,
    val max: Int? = null,
    val options: List<ShortageOption>? = null,
)

enum class ChangeKind { Wallet, Savings, Stat, Growth, Plan }

data class Change(
    val kind: ChangeKind,
    val label: String,
    val delta: Int,
    val before: Int? = null,
    val after: Int? = null,
    val stat: PetStat? = null,
    val why: String,
)

enum class Tone { Good, Info, Care }

data class NextLink(val label: String, val route: String)

data class Report(
    val title: String,
    val tone: Tone,
    val changes: List<Change>,
    val explain: String,
    val next: NextLink? = null,
)

/** Итог действия: успех (новый профиль + понятный отчёт + дополнительные данные) или объяснимая ошибка. */
sealed class Outcome<out T> {
    data class Ok<T>(val profile: Profile, val report: Report, val extra: T) : Outcome<T>()
    data class Err(val error: GameError) : Outcome<Nothing>()
}

typealias Res = Outcome<Unit>

data class Ctx(
    val content: Content,
    /** Дата в формате ГГГГ-ММ-ДД (в демо-режиме может быть сдвинута). */
    val today: String,
)
