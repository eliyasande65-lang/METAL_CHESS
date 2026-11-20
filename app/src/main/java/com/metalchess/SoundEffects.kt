package com.metalchess

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Small, offline synthesized piece-drop sounds. No network or audio files required.
 */
object SoundEffects {
    @Volatile private var enabled = true
    private const val SAMPLE_RATE = 22050

    fun setEnabled(value: Boolean) { enabled = value }

    fun play(context: Context, capture: Boolean, check: Boolean) {
        if (!enabled || !context.getSharedPreferences("chess_saves", Context.MODE_PRIVATE)
                .getBoolean("move_sounds", true)) return
        val frequency = when {
            check -> 720.0
            capture -> 185.0
            else -> 310.0
        }
        val duration = if (capture) 0.22 else 0.16
        val count = (SAMPLE_RATE * duration).toInt()
        val pcm = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * if (capture) 19.0 else 25.0)
            val fundamental = sin(2.0 * PI * frequency * t)
            val body = sin(2.0 * PI * frequency * 2.37 * t) * 0.32
            val click = sin(2.0 * PI * 1450.0 * t) * exp(-t * 95.0) * 0.20
            val value = (fundamental * 0.62 + body * 0.28 + click) * envelope
            pcm[i] = (value.coerceIn(-1.0, 1.0) * 10500).toInt().toShort()
        }
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setNotificationMarkerPosition(pcm.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(player: AudioTrack) {
                    try { player.stop(); player.release() } catch (_: Exception) { }
                }
                override fun onPeriodicNotification(player: AudioTrack) = Unit
            })
            track.play()
        } catch (_: Exception) { }
    }
}
