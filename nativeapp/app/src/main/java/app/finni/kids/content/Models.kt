package app.finni.kids.content

import app.finni.kids.domain.Direction
import app.finni.kids.domain.PetStat
import app.finni.kids.domain.Topic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Учебный контент лежит в assets/content/*.json. Новое задание — новый файл в tasks/, код менять не нужно.

@Serializable
data class Item(
    val id: String,
    val name: String,
    val kind: Direction,
    /** food / care — только у обязательных товаров. */
    val need: String? = null,
    val price: Int,
    val icon: String,
    val effects: Map<PetStat, Int> = emptyMap(),
    val accessory: String? = null,
    val blurb: String = "",
    val note: String = "",
)

@Serializable
data class GoalDef(val id: String, val title: String, val icon: String, val cost: Int, val blurb: String = "")

@Serializable
data class EventDef(
    val id: String,
    val period: Int,
    val title: String,
    /** Короткая строка для баннера на главном экране. */
    val short: String,
    val text: String,
    val cost: Int,
    val effects: Map<PetStat, Int> = emptyMap(),
    val tip: String,
)

@Serializable
data class GlossaryEntry(val id: String, val term: String, val short: String, val example: String? = null)

@Serializable
data class Competency(val id: Int, val text: String)

@Serializable
data class TopicDef(val id: Topic, val title: String, val blurb: String, val icon: String)

@Serializable
data class SpeciesDef(val id: String, val name: String)

@Serializable
data class ColorDef(val id: String, val name: String, val body: String, val belly: String, val dark: String)

@Serializable
data class AccessoryDef(val id: String, val name: String, val atStart: Boolean)

@Serializable
data class StageDef(val id: Int, val name: String, val blurb: String)

@Serializable
data class PetOptions(
    val species: List<SpeciesDef>,
    val colors: List<ColorDef>,
    val accessories: List<AccessoryDef>,
    val stages: List<StageDef>,
    /** Имя питомца-талисмана: подставляется по умолчанию, ребёнок может ввести своё. */
    val defaultPetName: String,
    val petNames: List<String>,
    val playerNames: List<String>,
)

@Serializable
data class CustomPreset(val icon: String, val title: String)

@Serializable
data class CustomGoalOptions(val presets: List<CustomPreset>)

// ---------- Задания ----------

@Serializable
data class Rewards(@SerialName("1") val one: Int, @SerialName("2") val two: Int, @SerialName("3") val three: Int) {
    operator fun get(stars: Int): Int = when (stars) {
        1 -> one
        2 -> two
        else -> three
    }
}

@Serializable
sealed class TaskDef {
    abstract val id: String
    abstract val topic: Topic
    abstract val title: String
    abstract val skill: String
    abstract val competencies: List<Int>
    abstract val unlockPeriod: Int
    abstract val intro: String
    abstract val rewards: Rewards
    abstract val learn: String
}

@Serializable
data class SortGroup(val id: String, val label: String, val hint: String)

@Serializable
data class SortItem(val id: String, val label: String, val icon: String, val group: String, val why: String)

@Serializable
@SerialName("sort")
data class SortTask(
    override val id: String, override val topic: Topic, override val title: String, override val skill: String,
    override val competencies: List<Int>, override val unlockPeriod: Int, override val intro: String,
    override val rewards: Rewards, override val learn: String,
    val groups: List<SortGroup>, val items: List<SortItem>,
) : TaskDef()

@Serializable
data class CartItem(val id: String, val label: String, val icon: String, val price: Int, val need: Boolean, val why: String)

@Serializable
@SerialName("cart")
data class CartTask(
    override val id: String, override val topic: Topic, override val title: String, override val skill: String,
    override val competencies: List<Int>, override val unlockPeriod: Int, override val intro: String,
    override val rewards: Rewards, override val learn: String,
    val budget: Int, val minLeft: Int, val items: List<CartItem>,
) : TaskDef()

@Serializable
data class Bucket(val id: Direction, val label: String, val hint: String)

@Serializable
data class AllocRule(val id: String, val bucket: Direction, val op: String, val value: Int, val ok: String, val fail: String)

@Serializable
@SerialName("allocate")
data class AllocateTask(
    override val id: String, override val topic: Topic, override val title: String, override val skill: String,
    override val competencies: List<Int>, override val unlockPeriod: Int, override val intro: String,
    override val rewards: Rewards, override val learn: String,
    val total: Int, val step: Int, val buckets: List<Bucket>, val rules: List<AllocRule>,
) : TaskDef()

@Serializable
data class CalcQuestion(val id: String, val text: String, val answer: Int, val unit: String = "", val explain: String)

@Serializable
@SerialName("calc")
data class CalcTask(
    override val id: String, override val topic: Topic, override val title: String, override val skill: String,
    override val competencies: List<Int>, override val unlockPeriod: Int, override val intro: String,
    override val rewards: Rewards, override val learn: String,
    val questions: List<CalcQuestion>,
) : TaskDef()

@Serializable
data class SimTexts(val none: String, val slow: String, val good: String, val great: String)

@Serializable
@SerialName("sim")
data class SimTask(
    override val id: String, override val topic: Topic, override val title: String, override val skill: String,
    override val competencies: List<Int>, override val unlockPeriod: Int, override val intro: String,
    override val rewards: Rewards, override val learn: String,
    val goalCost: Int, val start: Int, val freePerWeek: Int, val step: Int, val targetWeeks: Int, val texts: SimTexts,
) : TaskDef()

@Serializable
data class StoryEnd(val stars: Int, val consequence: String, val explain: String, val recovery: String? = null)

@Serializable
data class StoryChoice(val text: String, val next: String? = null, val end: StoryEnd? = null)

@Serializable
data class StoryNode(val text: String, val choices: List<StoryChoice>)

@Serializable
@SerialName("story")
data class StoryTask(
    override val id: String, override val topic: Topic, override val title: String, override val skill: String,
    override val competencies: List<Int>, override val unlockPeriod: Int, override val intro: String,
    override val rewards: Rewards, override val learn: String,
    val start: String, val nodes: Map<String, StoryNode>,
) : TaskDef()

data class Content(
    val items: List<Item>,
    val goals: List<GoalDef>,
    val customGoal: CustomGoalOptions,
    val tasks: List<TaskDef>,
    val events: List<EventDef>,
    val glossary: List<GlossaryEntry>,
    val competencies: List<Competency>,
    val pet: PetOptions,
    val topics: List<TopicDef>,
)
