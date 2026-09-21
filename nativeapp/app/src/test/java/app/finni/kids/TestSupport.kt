package app.finni.kids

import app.finni.kids.content.Content
import app.finni.kids.content.FileContentSource
import app.finni.kids.content.Item
import app.finni.kids.content.TaskDef
import app.finni.kids.content.loadContent
import app.finni.kids.domain.Ctx
import app.finni.kids.domain.NewProfileInput
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.PetAppearance
import app.finni.kids.domain.Profile
import app.finni.kids.domain.newProfile
import org.junit.Assert.fail
import java.io.File

/** Тот же контент, что и в приложении: assets/content. */
val content: Content by lazy { loadContent(FileContentSource(File("src/main/assets"))) }

fun ctx(today: String = "2026-09-19") = Ctx(content, today)

fun makeProfile(isDemo: Boolean = false): Profile =
    newProfile(NewProfileInput("Тест", "Тестик", PetAppearance("cat", "peach", "none"), isDemo), ctx())

/** Достаёт успешный результат или падает с понятным сообщением. */
fun <T> must(o: Outcome<T>): Outcome.Ok<T> = when (o) {
    is Outcome.Ok -> o
    is Outcome.Err -> { fail("Ожидался успех, а вышла ошибка ${o.error.code}: ${o.error.message}"); throw IllegalStateException() }
}

fun err(o: Outcome<*>): app.finni.kids.domain.GameError = when (o) {
    is Outcome.Err -> o.error
    is Outcome.Ok -> { fail("Ожидалась ошибка, а действие прошло"); throw IllegalStateException() }
}

fun item(id: String): Item = content.items.first { it.id == id }

fun task(id: String): TaskDef = content.tasks.first { it.id == id }
