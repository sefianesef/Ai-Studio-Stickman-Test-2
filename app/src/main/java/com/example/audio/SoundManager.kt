package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ultra-resilient, low-latency SoundManager powered directly by in-memory raw PCM waveforms
 * and non-blocking streaming AudioTracks.
 * Safe across all Android devices, handles hardware limitations gracefully, and never crashes.
 */
class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    // Standard game sample rate
    private val standardSampleRate = 22050

    // Audio attributes
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // Pre-synthesized PCM waveform tables (stored purely in JVM heap)
    private val soundBuffers = ConcurrentHashMap<String, ShortArray>()

    // Thread-safe pool of streaming AudioTracks
    private val streamingTracks = CopyOnWriteArrayList<AudioTrack>()
    private val trackIndex = AtomicInteger(0)

    init {
        scope.launch {
            try {
                initAllSounds()
            } catch (_: Throwable) {}
        }
    }

    private fun getOrCreateTrack(): AudioTrack? {
        val existing = streamingTracks.firstOrNull()
        if (existing != null && existing.state == AudioTrack.STATE_INITIALIZED) {
            return existing
        }
        return synchronized(streamingTracks) {
            val trackCheck = streamingTracks.firstOrNull()
            if (trackCheck != null && trackCheck.state == AudioTrack.STATE_INITIALIZED) {
                trackCheck
            } else {
                try {
                    val minBuf = AudioTrack.getMinBufferSize(
                        standardSampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ).coerceAtLeast(4096)

                    val format = AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(standardSampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()

                    val track = AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(minBuf)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        track.play()
                        streamingTracks.add(track)
                        track
                    } else {
                        null
                    }
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }

    private fun initAllSounds() {
        // 1. Bridge Placed (Thud + deep resonance)
        registerSound("bridge_place", durationMs = 120) { t, frac ->
            val decay = (1.0 - frac * frac)
            (sin(2.0 * PI * 120.0 * t) * 0.7 + sin(2.0 * PI * 65.0 * t) * 0.5) * decay
        }

        // 2. Stickman Landing (Satisfying platform step / contact tone)
        registerSound("stickman_land", durationMs = 100) { t, frac ->
            val decay = (1.0 - frac)
            (sin(2.0 * PI * 440.0 * t) * 0.6 + sin(2.0 * PI * 880.0 * t) * 0.4) * decay
        }

        // 3. Gem Collect (Crisp bright dual crystal chime)
        registerSound("gem_collect", durationMs = 160) { t, frac ->
            val freq = if (frac < 0.35) 987.77 else 1479.98
            val decay = (1.0 - frac)
            sin(2.0 * PI * freq * t) * decay * 0.85
        }

        // 4. Grow Tick (Short percussive click)
        registerSound("grow_tick", durationMs = 25) { t, frac ->
            val decay = 1.0 - frac
            sin(2.0 * PI * 520.0 * t) * decay * 0.45
        }

        // 5. Bridge Fall (Whistle descending)
        registerSound("bridge_fall", durationMs = 90) { t, frac ->
            val freq = 240.0 - frac * 120.0
            val decay = 1.0 - frac
            sin(2.0 * PI * freq * t) * decay * 0.6
        }

        // 6. Walk Step (Subtle light tap)
        registerSound("walk_step", durationMs = 20) { t, frac ->
            val decay = 1.0 - frac
            sin(2.0 * PI * 600.0 * t) * decay * 0.25
        }

        // 7. Perfect Bullseye Fanfare (Bright quad arpeggio)
        registerSound("perfect_hit", durationMs = 280) { t, frac ->
            val freq = when {
                frac < 0.25 -> 523.25 // C5
                frac < 0.50 -> 659.25 // E5
                frac < 0.75 -> 783.99 // G5
                else -> 1046.50       // C6
            }
            val noteFrac = (frac % 0.25) / 0.25
            val decay = 1.0 - noteFrac * 0.7
            sin(2.0 * PI * freq * t) * decay * 0.9
        }

        // 8. Stickman Flip
        registerSound("flip", durationMs = 50) { t, frac ->
            val freq = 450.0 + frac * 400.0
            val decay = 1.0 - frac
            sin(2.0 * PI * freq * t) * decay * 0.5
        }

        // 9. Game Over Defeat Chime
        registerSound("game_over", durationMs = 380) { t, frac ->
            val freq = when {
                frac < 0.3 -> 349.23  // F4
                frac < 0.6 -> 311.13  // Eb4
                else -> 220.00        // A3
            }
            val decay = 1.0 - frac * 0.8
            sin(2.0 * PI * freq * t) * decay * 0.85
        }

        // 10. UI Button Click
        registerSound("button_click", durationMs = 35) { t, frac ->
            val decay = 1.0 - frac
            sin(2.0 * PI * 700.0 * t) * decay * 0.4
        }

        // 11. Level Victory Fanfare / Music (Glorious multi-note triumph)
        registerSound("level_victory", durationMs = 650) { t, frac ->
            val freq = when {
                frac < 0.15 -> 523.25  // C5
                frac < 0.30 -> 659.25  // E5
                frac < 0.45 -> 783.99  // G5
                frac < 0.65 -> 1046.50 // C6
                else -> 1318.51        // E6
            }
            val segmentFrac = (frac % 0.15) / 0.15
            val decay = 1.0 - (segmentFrac * 0.45)
            (sin(2.0 * PI * freq * t) * 0.75 + sin(2.0 * PI * freq * 2.0 * t) * 0.35) * decay
        }

        // 12. Stickman Fall Down (Cartoon slide whistle drop down to hilarious thud)
        registerSound("stickman_fall", durationMs = 550) { t, frac ->
            if (frac < 0.82) {
                val fallFrac = frac / 0.82
                val baseFreq = 780.0 - (fallFrac * fallFrac * 620.0)
                val vibrato = sin(2.0 * PI * 18.0 * t) * 25.0
                val decay = 1.0 - fallFrac * 0.3
                sin(2.0 * PI * (baseFreq + vibrato) * t) * decay * 0.85
            } else {
                val impactFrac = (frac - 0.82) / 0.18
                val decay = 1.0 - impactFrac
                (sin(2.0 * PI * 85.0 * t) * 0.8 + sin(2.0 * PI * 42.0 * t) * 0.5) * decay
            }
        }

        // 13. Gem Purchase / Real Money Triumph Fanfare
        registerSound("purchase_success", durationMs = 500) { t, frac ->
            val freq = when {
                frac < 0.2 -> 659.25   // E5
                frac < 0.4 -> 880.00   // A5
                frac < 0.6 -> 1174.66  // D6
                else -> 1760.00        // A6
            }
            val decay = 1.0 - frac * 0.7
            (sin(2.0 * PI * freq * t) * 0.8 + sin(2.0 * PI * (freq * 1.5) * t) * 0.3) * decay
        }

        // 14. Combo Streak / On Fire Chime
        registerSound("combo_streak", durationMs = 240) { t, frac ->
            val freq = 580.0 + (frac * 600.0)
            val decay = 1.0 - frac
            sin(2.0 * PI * freq * t) * decay * 0.8
        }
    }

    private fun registerSound(
        key: String,
        durationMs: Int,
        sampleRate: Int = standardSampleRate,
        waveFn: (t: Double, frac: Double) -> Double
    ) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
            val pcmData = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val frac = i.toDouble() / numSamples
                val sampleVal = (waveFn(t, frac) * 32767.0).toInt().coerceIn(-32768, 32767)
                pcmData[i] = sampleVal.toShort()
            }

            soundBuffers[key] = pcmData
        } catch (_: Throwable) {}
    }

    private fun play(key: String, volume: Float = 1.0f, pitchMultiplier: Float = 1.0f) {
        if (!soundEnabled) return
        val rawPcm = soundBuffers[key] ?: return

        scope.launch(Dispatchers.Default) {
            try {
                val track = getOrCreateTrack() ?: return@launch
                if (track.state != AudioTrack.STATE_INITIALIZED) return@launch

                // Pitch scaling via linear interpolation if modified
                val pitchedPcm = if (pitchMultiplier != 1.0f && pitchMultiplier > 0.1f) {
                    val newLength = (rawPcm.size / pitchMultiplier).toInt().coerceAtLeast(1)
                    val resampled = ShortArray(newLength)
                    for (i in 0 until newLength) {
                        val srcIdx = i * pitchMultiplier
                        val i0 = srcIdx.toInt().coerceIn(0, rawPcm.size - 1)
                        val i1 = (i0 + 1).coerceIn(0, rawPcm.size - 1)
                        val frac = srcIdx - i0
                        resampled[i] = ((1.0 - frac) * rawPcm[i0] + frac * rawPcm[i1]).toInt().toShort()
                    }
                    resampled
                } else {
                    rawPcm
                }

                // Volume scaling
                val clampedVolume = volume.coerceIn(0f, 1f)
                val finalPcm = if (clampedVolume < 0.98f) {
                    ShortArray(pitchedPcm.size) { i ->
                        (pitchedPcm[i] * clampedVolume).toInt().coerceIn(-32768, 32767).toShort()
                    }
                } else {
                    pitchedPcm
                }

                // Write to streaming track non-blockingly
                track.write(finalPcm, 0, finalPcm.size, AudioTrack.WRITE_NON_BLOCKING)
            } catch (_: Throwable) {}
        }
    }

    // --- Sound Effects Trigger Methods ---

    fun playBridgePlaced() {
        play("bridge_place", volume = 0.8f)
    }

    fun playBridgeLand() {
        playBridgePlaced()
    }

    fun playStickmanLand() {
        play("stickman_land", volume = 0.7f)
    }

    fun playGemCollect() {
        play("gem_collect", volume = 0.9f)
    }

    fun playGrowTick(pitchIndex: Int) {
        val rate = (1.0f + (pitchIndex % 15) * 0.04f).coerceIn(0.8f, 1.8f)
        play("grow_tick", volume = 0.4f, pitchMultiplier = rate)
    }

    fun playBridgeFall() {
        play("bridge_fall", volume = 0.6f)
    }

    fun playWalkStep() {
        play("walk_step", volume = 0.3f)
    }

    fun playPerfectHit() {
        play("perfect_hit", volume = 0.95f)
    }

    fun playFlip() {
        play("flip", volume = 0.65f)
    }

    fun playGameOver() {
        play("game_over", volume = 0.9f)
    }

    fun playButton() {
        play("button_click", volume = 0.5f)
    }

    fun playVictoryMusic() {
        play("level_victory", volume = 1.0f)
    }

    fun playStickmanFall() {
        play("stickman_fall", volume = 0.95f)
    }

    fun playBuyGemsSuccess() {
        play("purchase_success", volume = 1.0f)
    }

    fun playComboStreak() {
        play("combo_streak", volume = 0.9f)
    }

    fun release() {
        try {
            streamingTracks.forEach { track ->
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            }
            streamingTracks.clear()
            soundBuffers.clear()
        } catch (_: Throwable) {}
    }
}


