package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * SoundPool Manager that synthesizes and loads low-latency sound effects for
 * bridge placement, stickman landing, gem collection, and arcade interactions.
 */
class SoundManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var soundPool: SoundPool? = null

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    // Sound effect IDs for SoundPool
    private var soundBridgePlaceId: Int = 0
    private var soundStickmanLandId: Int = 0
    private var soundGemCollectId: Int = 0
    private var soundGrowTickId: Int = 0
    private var soundBridgeFallId: Int = 0
    private var soundWalkStepId: Int = 0
    private var soundPerfectHitId: Int = 0
    private var soundFlipId: Int = 0
    private var soundGameOverId: Int = 0
    private var soundButtonId: Int = 0
    private var soundVictoryMusicId: Int = 0
    private var soundStickmanFallId: Int = 0
    private var soundPurchaseSuccessId: Int = 0
    private var soundComboStreakId: Int = 0

    private val loadedSounds = mutableSetOf<Int>()

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedSounds.add(sampleId)
                }
            }
            soundPool = pool
            generateAndLoadSounds()
        } catch (_: Exception) {}
    }

    private fun generateAndLoadSounds() {
        scope.launch {
            try {
                val soundDir = File(context.cacheDir, "game_sounds").apply { mkdirs() }

                // 1. Bridge Placed (Thud + resonance)
                soundBridgePlaceId = loadWavSound(soundDir, "bridge_placed.wav") {
                    generateWavData(durationMs = 120, sampleRate = 22050) { t, frac ->
                        val decay = (1.0 - frac * frac)
                        (sin(2.0 * PI * 120.0 * t) * 0.7 + sin(2.0 * PI * 65.0 * t) * 0.5) * decay
                    }
                }

                // 2. Stickman Landing (Satisfying platform step / contact tone)
                soundStickmanLandId = loadWavSound(soundDir, "stickman_land.wav") {
                    generateWavData(durationMs = 100, sampleRate = 22050) { t, frac ->
                        val decay = (1.0 - frac)
                        (sin(2.0 * PI * 440.0 * t) * 0.6 + sin(2.0 * PI * 880.0 * t) * 0.4) * decay
                    }
                }

                // 3. Gem Collect (Crisp bright dual crystal chime)
                soundGemCollectId = loadWavSound(soundDir, "gem_collect.wav") {
                    generateWavData(durationMs = 160, sampleRate = 22050) { t, frac ->
                        val freq = if (frac < 0.35) 987.77 else 1479.98 // B5 -> F#6
                        val decay = (1.0 - frac)
                        sin(2.0 * PI * freq * t) * decay * 0.85
                    }
                }

                // 4. Grow Tick (Short percussive click)
                soundGrowTickId = loadWavSound(soundDir, "grow_tick.wav") {
                    generateWavData(durationMs = 25, sampleRate = 22050) { t, frac ->
                        val decay = 1.0 - frac
                        sin(2.0 * PI * 520.0 * t) * decay * 0.4
                    }
                }

                // 5. Bridge Fall (Whistle descending)
                soundBridgeFallId = loadWavSound(soundDir, "bridge_fall.wav") {
                    generateWavData(durationMs = 90, sampleRate = 22050) { t, frac ->
                        val freq = 240.0 - frac * 120.0
                        val decay = 1.0 - frac
                        sin(2.0 * PI * freq * t) * decay * 0.6
                    }
                }

                // 6. Walk Step (Subtle light tap)
                soundWalkStepId = loadWavSound(soundDir, "walk_step.wav") {
                    generateWavData(durationMs = 20, sampleRate = 22050) { t, frac ->
                        val decay = 1.0 - frac
                        sin(2.0 * PI * 600.0 * t) * decay * 0.25
                    }
                }

                // 7. Perfect Bullseye Fanfare (Bright quad arpeggio)
                soundPerfectHitId = loadWavSound(soundDir, "perfect_hit.wav") {
                    generateWavData(durationMs = 280, sampleRate = 22050) { t, frac ->
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
                }

                // 8. Stickman Flip
                soundFlipId = loadWavSound(soundDir, "flip.wav") {
                    generateWavData(durationMs = 50, sampleRate = 22050) { t, frac ->
                        val freq = 450.0 + frac * 400.0
                        val decay = 1.0 - frac
                        sin(2.0 * PI * freq * t) * decay * 0.5
                    }
                }

                // 9. Game Over Defeat Chime
                soundGameOverId = loadWavSound(soundDir, "game_over.wav") {
                    generateWavData(durationMs = 380, sampleRate = 22050) { t, frac ->
                        val freq = when {
                            frac < 0.3 -> 349.23  // F4
                            frac < 0.6 -> 311.13  // Eb4
                            else -> 220.00        // A3
                        }
                        val decay = 1.0 - frac * 0.8
                        sin(2.0 * PI * freq * t) * decay * 0.85
                    }
                }

                // 10. UI Button Click
                soundButtonId = loadWavSound(soundDir, "btn_click.wav") {
                    generateWavData(durationMs = 35, sampleRate = 22050) { t, frac ->
                        val decay = 1.0 - frac
                        sin(2.0 * PI * 700.0 * t) * decay * 0.4
                    }
                }

                // 11. Level Victory Fanfare / Music (Glorious multi-note triumph)
                soundVictoryMusicId = loadWavSound(soundDir, "level_victory.wav") {
                    generateWavData(durationMs = 650, sampleRate = 22050) { t, frac ->
                        val freq = when {
                            frac < 0.15 -> 523.25  // C5
                            frac < 0.30 -> 659.25  // E5
                            frac < 0.45 -> 783.99  // G5
                            frac < 0.65 -> 1046.50 // C6
                            else -> 1318.51        // E6 (Triumphant high flourish!)
                        }
                        val segmentFrac = (frac % 0.15) / 0.15
                        val decay = 1.0 - (segmentFrac * 0.45)
                        // Add rich brass/chime 2nd harmonic
                        (sin(2.0 * PI * freq * t) * 0.75 + sin(2.0 * PI * freq * 2.0 * t) * 0.35) * decay
                    }
                }

                // 12. Stickman Fall Down (Cartoon slide whistle drop down to hilarious thud)
                soundStickmanFallId = loadWavSound(soundDir, "stickman_fall.wav") {
                    generateWavData(durationMs = 550, sampleRate = 22050) { t, frac ->
                        if (frac < 0.82) {
                            // Descending slide whistle with comedic vibrato
                            val fallFrac = frac / 0.82
                            val baseFreq = 780.0 - (fallFrac * fallFrac * 620.0) // 780Hz -> 160Hz
                            val vibrato = sin(2.0 * PI * 18.0 * t) * 25.0
                            val decay = 1.0 - fallFrac * 0.3
                            sin(2.0 * PI * (baseFreq + vibrato) * t) * decay * 0.85
                        } else {
                            // Comedic cartoon thud / splash impact
                            val impactFrac = (frac - 0.82) / 0.18
                            val decay = 1.0 - impactFrac
                            (sin(2.0 * PI * 85.0 * t) * 0.8 + sin(2.0 * PI * 42.0 * t) * 0.5) * decay
                        }
                    }
                }

                // 13. Gem Purchase / Real Money Triumph Fanfare
                soundPurchaseSuccessId = loadWavSound(soundDir, "purchase_success.wav") {
                    generateWavData(durationMs = 500, sampleRate = 22050) { t, frac ->
                        val freq = when {
                            frac < 0.2 -> 659.25   // E5
                            frac < 0.4 -> 880.00   // A5
                            frac < 0.6 -> 1174.66  // D6
                            else -> 1760.00        // A6 (Crystal sparkle)
                        }
                        val decay = 1.0 - frac * 0.7
                        (sin(2.0 * PI * freq * t) * 0.8 + sin(2.0 * PI * (freq * 1.5) * t) * 0.3) * decay
                    }
                }

                // 14. Combo Streak / On Fire Chime
                soundComboStreakId = loadWavSound(soundDir, "combo_streak.wav") {
                    generateWavData(durationMs = 240, sampleRate = 22050) { t, frac ->
                        val freq = 580.0 + (frac * 600.0)
                        val decay = 1.0 - frac
                        sin(2.0 * PI * freq * t) * decay * 0.8
                    }
                }
            } catch (_: Exception) {
                // Gracefully fallback
            }
        }
    }

    private fun loadWavSound(
        dir: File,
        fileName: String,
        generator: () -> ByteArray
    ): Int {
        val file = File(dir, fileName)
        if (!file.exists() || file.length() == 0L) {
            val bytes = generator()
            FileOutputStream(file).use { it.write(bytes) }
        }
        val pool = soundPool ?: return 0
        return pool.load(file.absolutePath, 1)
    }

    private fun generateWavData(
        durationMs: Int,
        sampleRate: Int = 22050,
        waveFn: (t: Double, frac: Double) -> Double
    ): ByteArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val dataSize = numSamples * 2
        val totalSize = 44 + dataSize

        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())

        // fmt chunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size (16 for PCM)
        buffer.putShort(1) // AudioFormat (1 for PCM)
        buffer.putShort(1) // NumChannels (1 = mono)
        buffer.putInt(sampleRate) // SampleRate
        buffer.putInt(sampleRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
        buffer.putShort(2) // BlockAlign (NumChannels * BitsPerSample/8)
        buffer.putShort(16) // BitsPerSample

        // data chunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val frac = i.toDouble() / numSamples
            val sampleVal = (waveFn(t, frac) * 32767.0).toInt().coerceIn(-32768, 32767)
            buffer.putShort(sampleVal.toShort())
        }

        return buffer.array()
    }

    private fun playSound(soundId: Int, volume: Float = 1.0f, rate: Float = 1.0f) {
        if (!soundEnabled || soundId == 0) return
        try {
            soundPool?.play(soundId, volume, volume, 1, 0, rate)
        } catch (_: Exception) {}
    }

    // --- Sound Effects Trigger Methods ---

    fun playBridgePlaced() {
        playSound(soundBridgePlaceId, volume = 0.8f)
    }

    fun playBridgeLand() {
        playBridgePlaced()
    }

    fun playStickmanLand() {
        playSound(soundStickmanLandId, volume = 0.7f)
    }

    fun playGemCollect() {
        playSound(soundGemCollectId, volume = 0.9f)
    }

    fun playGrowTick(pitchIndex: Int) {
        val rate = (1.0f + (pitchIndex % 15) * 0.04f).coerceIn(0.8f, 1.8f)
        playSound(soundGrowTickId, volume = 0.4f, rate = rate)
    }

    fun playBridgeFall() {
        playSound(soundBridgeFallId, volume = 0.6f)
    }

    fun playWalkStep() {
        playSound(soundWalkStepId, volume = 0.3f)
    }

    fun playPerfectHit() {
        playSound(soundPerfectHitId, volume = 0.95f)
    }

    fun playFlip() {
        playSound(soundFlipId, volume = 0.65f)
    }

    fun playGameOver() {
        playSound(soundGameOverId, volume = 0.9f)
    }

    fun playButton() {
        playSound(soundButtonId, volume = 0.5f)
    }

    fun playVictoryMusic() {
        playSound(soundVictoryMusicId, volume = 1.0f)
    }

    fun playStickmanFall() {
        playSound(soundStickmanFallId, volume = 0.95f)
    }

    fun playBuyGemsSuccess() {
        playSound(soundPurchaseSuccessId, volume = 1.0f)
    }

    fun playComboStreak() {
        playSound(soundComboStreakId, volume = 0.9f)
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
        } catch (_: Exception) {}
    }
}

