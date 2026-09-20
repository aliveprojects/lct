package app.finni.kids.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.sin

/** Короткие звуки-сигналы синтезируются на лету, без аудиофайлов. Можно отключить в настройках. */
enum class Beep { Tap, Coin, Good, Oops, Level }

object Sound {
    private const val RATE = 22050
    private var enabled = true
    private val handler = Handler(Looper.getMainLooper())

    private class Note(val f: Double, val t: Double, val d: Double, val wave: Char = 's')

    private val seq: Map<Beep, List<Note>> = mapOf(
        Beep.Tap to listOf(Note(520.0, 0.0, 0.05, 't')),
        Beep.Coin to listOf(Note(880.0, 0.0, 0.07, 'q'), Note(1320.0, 0.07, 0.16, 'q')),
        Beep.Good to listOf(Note(523.0, 0.0, 0.10, 't'), Note(659.0, 0.10, 0.10, 't'), Note(784.0, 0.20, 0.18, 't')),
        Beep.Oops to listOf(Note(330.0, 0.0, 0.12), Note(262.0, 0.12, 0.18)),
        Beep.Level to listOf(Note(523.0, 0.0, 0.10, 't'), Note(659.0, 0.10, 0.10, 't'), Note(784.0, 0.20, 0.10, 't'), Note(1047.0, 0.30, 0.30, 't')),
    )

    private val buffers = HashMap<Beep, ShortArray>()

    fun setEnabled(on: Boolean) { enabled = on }

    private fun render(kind: Beep): ShortArray = buffers.getOrPut(kind) {
        val notes = seq.getValue(kind)
        val total = notes.maxOf { it.t + it.d } + 0.03
        val out = DoubleArray((total * RATE).toInt())
        for (n in notes) {
            val start = (n.t * RATE).toInt()
            val len = (n.d * RATE).toInt()
            for (i in 0 until len) {
                val x = i.toDouble() / RATE
                val phase = 2 * PI * n.f * x
                val w = when (n.wave) {
                    'q' -> if (sin(phase) >= 0) 1.0 else -1.0
                    't' -> 2.0 / PI * Math.asin(sin(phase))
                    else -> sin(phase)
                }
                val env = minOf(1.0, i / (0.01 * RATE)) * (1.0 - i.toDouble() / len)
                if (start + i < out.size) out[start + i] += w * env * 0.09
            }
        }
        ShortArray(out.size) { (out[it].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }
    }

    fun play(kind: Beep) {
        if (!enabled) return
        try {
            val data = render(kind)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(data, 0, data.size)
            track.play()
            handler.postDelayed({ try { track.release() } catch (_: Exception) {} }, (data.size * 1000L / RATE) + 200)
        } catch (_: Exception) {
            // звук не критичен
        }
    }
}
