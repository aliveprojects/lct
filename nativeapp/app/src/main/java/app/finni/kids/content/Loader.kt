package app.finni.kids.content

import app.finni.kids.domain.GameJson
import kotlinx.serialization.serializer
import java.io.File

/** Откуда читать файлы контента: из assets приложения или с диска (в тестах). */
interface ContentSource {
    fun read(path: String): String
    fun list(dir: String): List<String>
}

class FileContentSource(private val root: File) : ContentSource {
    override fun read(path: String): String = File(root, path).readText(Charsets.UTF_8)
    override fun list(dir: String): List<String> = File(root, dir).list()?.sorted() ?: emptyList()
}

private inline fun <reified T> ContentSource.load(path: String): T = GameJson.decodeFromString(serializer<T>(), read(path))

// Каждый файл в content/tasks — одно задание: чтобы добавить новое, достаточно положить сюда JSON.
fun loadContent(src: ContentSource): Content {
    val topics: List<TopicDef> = src.load("content/topics.json")
    val order = topics.map { it.id }
    val tasks = src.list("content/tasks")
        .filter { it.endsWith(".json") }
        .map { GameJson.decodeFromString(TaskDef.serializer(), src.read("content/tasks/$it")) }
        .sortedWith(compareBy<TaskDef> { it.unlockPeriod }.thenBy { order.indexOf(it.topic) }.thenBy { it.id })
    return Content(
        items = src.load("content/items.json"),
        goals = src.load("content/goals.json"),
        customGoal = src.load("content/custom-goal.json"),
        tasks = tasks,
        events = src.load("content/events.json"),
        glossary = src.load("content/glossary.json"),
        competencies = src.load("content/competencies.json"),
        pet = src.load("content/pet.json"),
        topics = topics,
    )
}

