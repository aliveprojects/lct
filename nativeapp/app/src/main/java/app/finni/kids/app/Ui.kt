package app.finni.kids.app

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ColumnScope
import app.finni.kids.domain.Outcome
import app.finni.kids.domain.Report
import app.finni.kids.domain.ChangeKind
import app.finni.kids.domain.GameError
import app.finni.kids.domain.Tone
import app.finni.kids.platform.Beep
import app.finni.kids.platform.Sound
import app.finni.kids.ui.explainError
import kotlinx.coroutines.CompletableDeferred

class ConfirmOptions(
    val title: String,
    val confirmLabel: String,
    val cancelLabel: String = "Отмена",
    /** Действие заметно меняет прогресс: подсвечиваем аккуратнее. */
    val careful: Boolean = false,
    val icon: String? = null,
    val body: (@Composable ColumnScope.() -> Unit)? = null,
)

enum class SheetTone { Normal, Good, Care }

sealed class Overlay(val id: Int) {
    class Confirm(id: Int, val opts: ConfirmOptions, val done: CompletableDeferred<Boolean>) : Overlay(id)
    class ReportView(id: Int, val report: Report, val done: CompletableDeferred<Unit>) : Overlay(id)
    class Sheet(id: Int, val title: String?, val tone: SheetTone, val actions: (@Composable ColumnScope.(close: () -> Unit) -> Unit)?, val content: @Composable ColumnScope.(close: () -> Unit) -> Unit, val done: CompletableDeferred<Unit>) : Overlay(id)

    fun dismiss() {
        when (this) {
            is Confirm -> done.complete(false)
            is ReportView -> done.complete(Unit)
            is Sheet -> done.complete(Unit)
        }
    }
}

class ToastData(val id: Int, val text: String)

/** Окна поверх экрана. Каждое окно — «обещание»: экран ждёт ответа и продолжает работу после закрытия. */
object Ui {
    /** Долгоживущий scope: продолжает работу, даже когда окно, из которого всё началось, уже закрыто. */
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    private var seq = 1
    val stack = mutableStateListOf<Overlay>()
    var toast by mutableStateOf<ToastData?>(null)
        private set
    private val handler = Handler(Looper.getMainLooper())
    private val hideToast = Runnable { toast = null }

    private suspend fun <T> show(o: Overlay, done: CompletableDeferred<T>): T {
        stack.add(o)
        try {
            return done.await()
        } finally {
            stack.remove(o)
        }
    }

    suspend fun confirm(opts: ConfirmOptions): Boolean {
        val d = CompletableDeferred<Boolean>()
        return show(Overlay.Confirm(seq++, opts, d), d)
    }

    /** Показывает «что изменилось и почему». */
    suspend fun report(report: Report) {
        val d = CompletableDeferred<Unit>()
        show(Overlay.ReportView(seq++, report, d), d)
    }

    /** Окно снизу. Кнопки из [actions] закреплены внизу и не прокручиваются вместе с содержимым. */
    suspend fun sheet(title: String? = null, tone: SheetTone = SheetTone.Normal, actions: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null, content: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
        val d = CompletableDeferred<Unit>()
        show(Overlay.Sheet(seq++, title, tone, actions, content, d), d)
    }

    fun toast(text: String) {
        toast = ToastData(seq++, text)
        handler.removeCallbacks(hideToast)
        handler.postDelayed(hideToast, 3200)
    }

    /** Закрывает верхнее окно (для системной кнопки «назад»). Возвращает true, если было что закрывать. */
    fun closeTop(): Boolean {
        val top = stack.lastOrNull() ?: return false
        top.dismiss()
        return true
    }
}

/**
 * Показывает результат действия движка. Успех — окно «что изменилось и почему».
 * Ошибка — понятное объяснение и варианты, а не тупик. Возвращает true, если действие удалось.
 */
suspend fun <T> present(r: Outcome<T>): Boolean {
    when (r) {
        is Outcome.Err -> {
            explainError(r.error)
            return false
        }
        is Outcome.Ok -> {
            val gained = r.report.changes.any { it.kind == ChangeKind.Wallet && it.delta > 0 }
            val grew = r.report.changes.any { it.kind == ChangeKind.Growth && it.delta > 0 }
            Sound.play(if (grew) Beep.Level else if (gained) Beep.Coin else if (r.report.tone == Tone.Good) Beep.Good else Beep.Tap)
            Ui.report(r.report)
            return true
        }
    }
}
