package app.finni.kids.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

// Фоновая музыка: два трека по кругу, тихо, с плавным включением. Работает офлайн (файлы лежат в приложении).
// Не играет, когда приложение свёрнуто, и выключается переключателем в настройках.
// Важная информация никогда не передаётся только музыкой.

data class Track(val id: String, val title: String, val artist: String, val file: String)

/** Треки и их авторы (показываются в разделе для взрослых). Чтобы заменить музыку, поменяйте файлы в assets/music и этот список. */
val TRACKS = listOf(
    Track("sky", "Sensai Masopi Sky", "DEX 1200", "music/sky.m4a"),
    Track("metamorphosis", "Metamorphosis", "Laura Platt", "music/metamorphosis.m4a"),
)

fun nextIndex(i: Int, n: Int = TRACKS.size): Int = (i + 1) % n

object Music {
    private const val VOLUME = 0.22f
    private const val FADE_MS = 700L

    private lateinit var appContext: Context
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var index = 0
    private var enabled = false
    private var background = true
    private var focusLost = false
    private var fadeToken = 0
    private var focusRequest: AudioFocusRequest? = null

    /** Текущее состояние: playing / paused / off — для проверок и отладки. */
    val status: String get() = if (!enabled) "off" else if (player?.isPlaying == true) "playing" else "paused"

    private val audioManager get() = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                focusLost = true
                player?.takeIf { it.isPlaying }?.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (focusLost) { focusLost = false; play() }
            }
        }
    }

    fun init(context: Context, on: Boolean) {
        appContext = context.applicationContext
        index = (Math.random() * TRACKS.size).toInt()
        enabled = on
    }

    private fun load(): MediaPlayer {
        player?.let { return it }
        val mp = MediaPlayer()
        val fd = appContext.assets.openFd(TRACKS[index].file)
        mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        fd.close()
        mp.setVolume(0f, 0f)
        mp.setOnCompletionListener {
            it.release()
            player = null
            index = nextIndex(index)
            play()
        }
        mp.prepare()
        player = mp
        return mp
    }

    private fun requestFocus(): Boolean {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener(focusListener, handler)
            .build()
        focusRequest = req
        return audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun ramp(to: Float, then: (() -> Unit)? = null) {
        val mp = player ?: return
        val token = ++fadeToken
        val steps = (FADE_MS / 50).toInt().coerceAtLeast(1)
        // плавно идём от 0 к цели (включение) или от штатной громкости к 0 (выключение)
        val from = if (to > 0f) 0f else VOLUME
        var n = 0
        val tick = object : Runnable {
            override fun run() {
                if (token != fadeToken) return
                n++
                val v = (from + (to - from) * n / steps).coerceIn(0f, 1f)
                try { mp.setVolume(v, v) } catch (_: Exception) { return }
                if (n >= steps) then?.invoke() else handler.postDelayed(this, 50)
            }
        }
        handler.post(tick)
    }

    private fun play() {
        if (!enabled || background) return
        try {
            val mp = load()
            if (mp.isPlaying) return
            if (!requestFocus()) return
            focusLost = false
            mp.start()
            ramp(VOLUME)
        } catch (_: Exception) {
            // музыка не критична: приложение работает и без неё
        }
    }

    private fun pauseNow() {
        fadeToken++
        try { player?.takeIf { it.isPlaying }?.pause() } catch (_: Exception) {}
        abandonFocus()
    }

    /** Включает или выключает музыку (переключатель в настройках). */
    fun setEnabled(on: Boolean) {
        enabled = on
        if (on) {
            play()
        } else {
            val mp = player
            if (mp != null && mp.isPlaying) ramp(0f) { pauseNow() } else pauseNow()
        }
    }

    /** Приложение свёрнуто или вернулось: музыка не играет в фоне. */
    fun setBackgrounded(isBackground: Boolean) {
        background = isBackground
        if (isBackground) pauseNow() else play()
    }

    fun release() {
        fadeToken++
        player?.release()
        player = null
        abandonFocus()
    }
}
