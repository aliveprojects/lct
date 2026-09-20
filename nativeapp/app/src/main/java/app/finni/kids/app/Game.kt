package app.finni.kids.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.finni.kids.content.Content
import app.finni.kids.content.ContentSource
import app.finni.kids.content.loadContent
import app.finni.kids.domain.AppState
import app.finni.kids.domain.Ctx
import app.finni.kids.domain.ErrorCode
import app.finni.kids.domain.GameError
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.Profile
import app.finni.kids.domain.activeProfile
import app.finni.kids.domain.initialApp
import app.finni.kids.domain.makeCtx
import app.finni.kids.domain.parseState
import app.finni.kids.domain.serializeState
import app.finni.kids.domain.withProfile
import app.finni.kids.platform.Music
import app.finni.kids.platform.Sound
import app.finni.kids.platform.Storage

/** Читает контент из assets приложения. */
private class AssetSource(private val context: Context) : ContentSource {
    override fun read(path: String): String = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    override fun list(dir: String): List<String> = context.assets.list(dir)?.sorted() ?: emptyList()
}

/**
 * Единственный источник состояния приложения. Изменять профиль можно только через [act]: движок возвращает
 * новый профиль (или ошибку), хранилище его сохраняет, а экран показывает объяснение из результата.
 */
object Game {
    lateinit var content: Content
        private set

    var app: AppState by mutableStateOf(initialApp())
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var pending: AppState? = null
    private var ready = false

    fun init(context: Context) {
        if (ready) return
        Storage.init(context)
        content = loadContent(AssetSource(context))
        val raw = Storage.load()
        val parsed = parseState(raw)
        if (parsed.recovered && raw != null) Storage.backup(raw)
        app = parsed.state
        Sound.setEnabled(app.settings.sound)
        Music.init(context, app.settings.music)
        ready = true
    }

    fun setState(next: AppState) {
        app = next
        pending = next
        handler.removeCallbacks(writer)
        handler.postDelayed(writer, 120)
    }

    fun mutate(fn: (AppState) -> AppState) = setState(fn(app))

    private val writer = Runnable { write() }

    private fun write() {
        val s = pending ?: return
        pending = null
        try { Storage.save(serializeState(s)) } catch (_: Exception) { /* прогресс остаётся в памяти */ }
    }

    /** Немедленно записывает всё, что не успело сохраниться (сворачивание, закрытие). */
    fun flush() {
        handler.removeCallbacks(writer)
        write()
    }

    val ctx: Ctx get() = makeCtx(app, content)

    val profile: Profile? get() = activeProfile(app)

    /** Применяет действие движка к профилю. Успех сохраняется, ошибка ничего не меняет. */
    fun <T> act(fn: (Profile, Ctx) -> Outcome<T>): Outcome<T> {
        val p = profile ?: return Outcome.Err(GameError(ErrorCode.NO_PROFILE, "Профиль ещё не создан."))
        val res = fn(p, ctx)
        if (res is Outcome.Ok) setState(withProfile(app, res.profile))
        return res
    }
}
