package app.finni.kids.platform

import android.content.Context
import android.content.SharedPreferences

/** Хранилище прогресса: SharedPreferences внутри приложения. Наружу ничего не отправляется. */
object Storage {
    private const val KEY = "state.v1"
    private const val BACKUP_KEY = "state.backup"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("finni", Context.MODE_PRIVATE)
    }

    fun load(): String? = prefs.getString(KEY, null)

    fun save(raw: String) = prefs.edit().putString(KEY, raw).apply()

    /** Сохраняет копию повреждённых данных, чтобы их можно было разобрать вручную. */
    fun backup(raw: String) = prefs.edit().putString(BACKUP_KEY, raw).apply()

    /** Удаляет резервную копию повреждённых данных (при удалении профиля не должно остаться ничего). */
    fun clearBackup() = prefs.edit().remove(BACKUP_KEY).apply()

    fun clear() = prefs.edit().remove(KEY).remove(BACKUP_KEY).apply()
}
