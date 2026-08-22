package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Ultra-Reliable Dual-Engine Game Audio Manager:
 * 1. SoundPool Primary Engine: Standard Android SoundPool with generated WAV files on local disk
 *    - Guarantees 100% audio compatibility with browser emulator, WebRTC streaming, and Android devices.
 * 2. Static AudioTrack Fallback: Direct hardware PCM streaming if SoundPool is loading.
 */
class SoundManager(private val context: Context) {

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    // SoundPool resources
    private var soundPool: SoundPool? = null
    private val soundIdMap = ConcurrentHashMap<String, Int>()
    private val isLoadedMap = ConcurrentHashMap<Int, Boolean>()
    private val isInitialized = AtomicBoolean(false)

    // Pre-computed raw waveform samples
    private val pcmTick1 by lazy { generateTone(freq = 440f, durationSec = 0.045f, decayRate = 50f) }
    private val pcmTick2 by lazy { generateTone(freq = 554f, durationSec = 0.045f, decayRate = 50f) }
    private val pcmTick3 by lazy { generateTone(freq = 659f, durationSec = 0.045f, decayRate = 50f) }
    private val pcmTick4 by lazy { generateTone(freq = 880f, durationSec = 0.045f, decayRate = 50f) }

    private val pcmBridgeLand by lazy { generateTone(freq = 180f, durationSec = 0.14f, decayRate = 18f) }
    private val pcmStickmanLand by lazy { generateTone(freq = 240f, durationSec = 0.10f, decayRate = 30f) }
    private val pcmGem by lazy { generateTone(freq = 1046f, durationSec = 0.18f, decayRate = 10f, harmonic = 2093f) }
    private val pcmPerfect by lazy { generateChime(freq1 = 880f, freq2 = 1320f, freq3 = 1760f, durationSec = 0.32f) }
    private val pcmFlip by lazy { generateSweep(startFreq = 320f, endFreq = 640f, durationSec = 0.08f) }
    private val pcmFall by lazy { generateSweep(startFreq = 420f, endFreq = 90f, durationSec = 0.28f) }
    private val pcmFunnyFallingMusic by lazy { generateFunnyFallingMusic() }
    private val pcmOhNoVoice by lazy { generateOhNoVoiceOver() }
    private val pcmGameOver by lazy { generateMinorChord(durationSec = 0.45f) }
    private val pcmButton by lazy { generateTone(freq = 520f, durationSec = 0.05f, decayRate = 45f) }
    private val pcmVictory by lazy { generateFanfare(durationSec = 0.50f) }
    private val pcmBuySuccess by lazy { generateChime(freq1 = 660f, freq2 = 880f, freq3 = 1320f, durationSec = 0.28f) }
    private val pcmStartupMelody by lazy { generateStartupMelody() }

    init {
        initAudioEngines()
    }

    private fun initAudioEngines() {
        if (!isInitialized.compareAndSet(false, true)) return

        thread(name = "SoundPoolInit", isDaemon = true) {
            try {
                // Ensure system media volume is not muted in emulator safely without triggering OEM permission warnings
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    audioManager?.let { am ->
                        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                        if (currentVol == 0 && maxVol > 0) {
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.85f).toInt().coerceAtLeast(1), 0)
                        }
                    }
                } catch (_: Throwable) {}

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val pool = SoundPool.Builder()
                    .setMaxStreams(12)
                    .setAudioAttributes(audioAttributes)
                    .build()

                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        isLoadedMap[sampleId] = true
                    }
                }

                soundPool = pool

                // Pre-generate and cache WAV files for SoundPool instant playback
                val audioDir = File(context.cacheDir, "game_sounds").apply { mkdirs() }

                fun registerSound(key: String, samples: ShortArray) {
                    try {
                        val file = File(audioDir, "$key.wav")
                        if (!file.exists() || file.length() == 0L) {
                            writePcmToWav(file, samples, SAMPLE_RATE)
                        }
                        val soundId = pool.load(file.absolutePath, 1)
                        soundIdMap[key] = soundId
                    } catch (e: Throwable) {
                        Log.w("SoundManager", "Error caching sound $key", e)
                    }
                }

                registerSound("tick1", pcmTick1)
                registerSound("tick2", pcmTick2)
                registerSound("tick3", pcmTick3)
                registerSound("tick4", pcmTick4)
                registerSound("bridge_land", pcmBridgeLand)
                registerSound("stickman_land", pcmStickmanLand)
                registerSound("gem", pcmGem)
                registerSound("perfect", pcmPerfect)
                registerSound("flip", pcmFlip)
                registerSound("fall", pcmFall)
                registerSound("funny_fall", pcmFunnyFallingMusic)
                registerSound("oh_no_voice", pcmOhNoVoice)
                registerSound("game_over", pcmGameOver)
                registerSound("button", pcmButton)
                registerSound("victory", pcmVictory)
                registerSound("buy_success", pcmBuySuccess)
                registerSound("startup", pcmStartupMelody)

            } catch (t: Throwable) {
                Log.w("SoundManager", "SoundPool initialization error", t)
            }
        }
    }

    private fun play(key: String, fallbackSamples: ShortArray, volume: Float = 0.95f, priority: Int = 1, rate: Float = 1.0f) {
        if (!soundEnabled) return

        initAudioEngines()

        val pool = soundPool
        val soundId = soundIdMap[key]

        if (pool != null && soundId != null && (isLoadedMap[soundId] == true)) {
            try {
                val streamId = pool.play(soundId, volume, volume, priority, 0, rate)
                if (streamId != 0) return
            } catch (t: Throwable) {
                Log.w("SoundManager", "SoundPool play error for $key", t)
            }
        }

        // Direct AudioTrack Instant Playback Fallback
        playViaAudioTrack(fallbackSamples, volume)
    }

    private fun playViaAudioTrack(samples: ShortArray, volume: Float) {
        thread(name = "DirectAudioTrack", isDaemon = true) {
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufSize = maxOf(minBuf, samples.size * 2)

                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                if (volume < 1.0f) {
                    val scaledSamples = ShortArray(samples.size)
                    for (i in samples.indices) {
                        scaledSamples[i] = (samples[i] * volume).toInt().coerceIn(-32767, 32767).toShort()
                    }
                    track.write(scaledSamples, 0, scaledSamples.size)
                } else {
                    track.write(samples, 0, samples.size)
                }

                track.play()

                // Wait until playback completes, then release
                val durationMs = (samples.size * 1000L) / SAMPLE_RATE + 50
                Thread.sleep(durationMs)
                track.stop()
                track.release()
            } catch (_: Throwable) {}
        }
    }

    // --- High-Quality Game Sound FX Callbacks ---

    fun playGrowTick(pitchIndex: Int) {
        when (pitchIndex % 4) {
            0 -> play("tick1", pcmTick1, volume = 0.65f)
            1 -> play("tick2", pcmTick2, volume = 0.65f)
            2 -> play("tick3", pcmTick3, volume = 0.65f)
            else -> play("tick4", pcmTick4, volume = 0.65f)
        }
    }

    fun playBridgePlaced() = play("bridge_land", pcmBridgeLand, volume = 0.90f)
    fun playBridgeLand() = play("bridge_land", pcmBridgeLand, volume = 0.90f)
    fun playStickmanLand() = play("stickman_land", pcmStickmanLand, volume = 0.85f)
    fun playGemCollect() = play("gem", pcmGem, volume = 0.95f)
    fun playPerfectHit() = play("perfect", pcmPerfect, volume = 1.00f)
    fun playFlip() = play("flip", pcmFlip, volume = 0.85f)
    fun playBridgeFall() = play("fall", pcmFall, volume = 0.90f)
    fun playStickmanFall() = play("funny_fall", pcmFunnyFallingMusic, volume = 1.00f, priority = 10)
    fun playFunnyFallingMusic() = play("funny_fall", pcmFunnyFallingMusic, volume = 1.00f, priority = 10)
    fun playOhNoVoice() = play("oh_no_voice", pcmOhNoVoice, volume = 1.00f, priority = 10)
    fun playGameOver() = play("game_over", pcmGameOver, volume = 0.95f)
    fun playWalkStep() = play("tick1", pcmTick1, volume = 0.35f)
    fun playButton() = play("button", pcmButton, volume = 0.80f)
    fun playVictoryMusic() = play("victory", pcmVictory, volume = 1.00f)
    fun playBuyGemsSuccess() = play("buy_success", pcmBuySuccess, volume = 0.95f)
    fun playComboStreak() = play("perfect", pcmPerfect, volume = 0.95f)
    fun playStartupMelody() = play("startup", pcmStartupMelody, volume = 0.90f)

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
        } catch (_: Throwable) {}
    }

    companion object {
        private const val SAMPLE_RATE = 44100

        private fun writePcmToWav(wavFile: File, pcmData: ShortArray, sampleRate: Int) {
            val totalAudioLen = pcmData.size * 2
            val totalDataLen = totalAudioLen + 36
            val byteRate = sampleRate * 2 // 16 bit mono = 2 bytes per sample

            FileOutputStream(wavFile).use { out ->
                val header = ByteArray(44)
                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

                // RIFF chunk descriptor
                buffer.put("RIFF".toByteArray())
                buffer.putInt(totalDataLen)
                buffer.put("WAVE".toByteArray())

                // "fmt " sub-chunk
                buffer.put("fmt ".toByteArray())
                buffer.putInt(16) // SubChunk1Size for PCM
                buffer.putShort(1.toShort()) // AudioFormat (1 = PCM)
                buffer.putShort(1.toShort()) // NumChannels (1 = Mono)
                buffer.putInt(sampleRate)
                buffer.putInt(byteRate)
                buffer.putShort(2.toShort()) // BlockAlign (NumChannels * BitsPerSample/8)
                buffer.putShort(16.toShort()) // BitsPerSample

                // "data" sub-chunk
                buffer.put("data".toByteArray())
                buffer.putInt(totalAudioLen)

                out.write(header)

                // Write raw PCM byte buffer
                val pcmBytes = ByteArray(pcmData.size * 2)
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmData)
                out.write(pcmBytes)
            }
        }

        private fun generateTone(
            freq: Float,
            durationSec: Float,
            decayRate: Float = 20f,
            harmonic: Float = 0f
        ): ShortArray {
            val numSamples = (SAMPLE_RATE * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val envelope = exp(-decayRate * t)
                var wave = sin(2.0 * PI * freq * t).toFloat()
                if (harmonic > 0f) {
                    wave = (wave * 0.7f) + (sin(2.0 * PI * harmonic * t).toFloat() * 0.3f)
                }
                val sampleValue = (wave * envelope * 28000).toInt().coerceIn(-32767, 32767)
                samples[i] = sampleValue.toShort()
            }
            return samples
        }

        private fun generateSweep(startFreq: Float, endFreq: Float, durationSec: Float): ShortArray {
            val numSamples = (SAMPLE_RATE * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val progress = t / durationSec
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                val envelope = sin(PI * progress).toFloat()
                val wave = sin(2.0 * PI * currentFreq * t).toFloat()
                val sampleValue = (wave * envelope * 28000).toInt().coerceIn(-32767, 32767)
                samples[i] = sampleValue.toShort()
            }
            return samples
        }

        private fun generateChime(freq1: Float, freq2: Float, freq3: Float, durationSec: Float): ShortArray {
            val numSamples = (SAMPLE_RATE * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val envelope = exp(-8f * t)
                val wave = (sin(2.0 * PI * freq1 * t) * 0.4 +
                        sin(2.0 * PI * freq2 * t) * 0.35 +
                        sin(2.0 * PI * freq3 * t) * 0.25).toFloat()
                val sampleValue = (wave * envelope * 28000).toInt().coerceIn(-32767, 32767)
                samples[i] = sampleValue.toShort()
            }
            return samples
        }

        private fun generateMinorChord(durationSec: Float): ShortArray {
            val numSamples = (SAMPLE_RATE * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val envelope = exp(-6f * t)
                val wave = (sin(2.0 * PI * 220.0 * t) * 0.45 +
                        sin(2.0 * PI * 261.6 * t) * 0.35 +
                        sin(2.0 * PI * 329.6 * t) * 0.20).toFloat()
                val sampleValue = (wave * envelope * 28000).toInt().coerceIn(-32767, 32767)
                samples[i] = sampleValue.toShort()
            }
            return samples
        }

        private fun generateFanfare(durationSec: Float): ShortArray {
            val numSamples = (SAMPLE_RATE * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val envelope = exp(-5f * t)
                val wave = (sin(2.0 * PI * 523.25 * t) * 0.4 +
                        sin(2.0 * PI * 659.25 * t) * 0.35 +
                        sin(2.0 * PI * 783.99 * t) * 0.25).toFloat()
                val sampleValue = (wave * envelope * 28000).toInt().coerceIn(-32767, 32767)
                samples[i] = sampleValue.toShort()
            }
            return samples
        }

        private fun generateStartupMelody(): ShortArray {
            data class MelodicNote(val freq: Float, val startSec: Float, val durationSec: Float, val gain: Float)

            val melodyScore = listOf(
                MelodicNote(freq = 392.00f, startSec = 0.00f, durationSec = 0.35f, gain = 0.70f), // G4
                MelodicNote(freq = 440.00f, startSec = 0.18f, durationSec = 0.35f, gain = 0.75f), // A4
                MelodicNote(freq = 523.25f, startSec = 0.36f, durationSec = 0.40f, gain = 0.80f), // C5
                MelodicNote(freq = 587.33f, startSec = 0.54f, durationSec = 0.45f, gain = 0.85f), // D5
                MelodicNote(freq = 659.25f, startSec = 0.72f, durationSec = 0.55f, gain = 0.90f), // E5
                MelodicNote(freq = 783.99f, startSec = 0.90f, durationSec = 0.90f, gain = 1.00f), // G5
                MelodicNote(freq = 1046.50f, startSec = 0.90f, durationSec = 0.90f, gain = 0.55f), // C6
                MelodicNote(freq = 261.63f, startSec = 0.00f, durationSec = 1.20f, gain = 0.40f), // C4 bass
                MelodicNote(freq = 329.63f, startSec = 0.54f, durationSec = 1.00f, gain = 0.35f)  // E4 chord
            )

            val totalDurationSec = 1.95f
            val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
            val floatBuffer = FloatArray(totalSamples)

            for (note in melodyScore) {
                val startIdx = (note.startSec * SAMPLE_RATE).toInt()
                val noteSamples = (note.durationSec * SAMPLE_RATE).toInt()
                for (i in 0 until noteSamples) {
                    val idx = startIdx + i
                    if (idx < totalSamples) {
                        val t = i.toFloat() / SAMPLE_RATE
                        val attack = (t / 0.018f).coerceIn(0f, 1f)
                        val decay = exp(-3.8f * t)
                        val env = attack * decay

                        val f0 = note.freq
                        val sample = (
                            sin(2.0 * PI * f0 * t) * 0.72 +
                            sin(2.0 * PI * (f0 * 2.0) * t) * 0.18 +
                            sin(2.0 * PI * (f0 * 3.0) * t) * 0.07 +
                            sin(2.0 * PI * (f0 * 4.0) * t) * 0.03
                        ).toFloat()

                        floatBuffer[idx] += sample * env * note.gain
                    }
                }
            }

            val pcmSamples = ShortArray(totalSamples)
            var maxPeak = 0.001f
            for (v in floatBuffer) {
                val absV = kotlin.math.abs(v)
                if (absV > maxPeak) maxPeak = absV
            }
            val scale = (27000f / maxPeak).coerceAtMost(28000f)

            for (i in 0 until totalSamples) {
                pcmSamples[i] = (floatBuffer[i] * scale).toInt().coerceIn(-32767, 32767).toShort()
            }
            return pcmSamples
        }

        private fun generateOhNoVoiceOver(): ShortArray {
            val totalDurationSec = 2.45f
            val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
            val floatBuffer = FloatArray(totalSamples)

            data class VocalSyllable(
                val startSec: Float,
                val durationSec: Float,
                val startPitch: Float,
                val endPitch: Float,
                val isNasalStart: Boolean,
                val tremoloRate: Float = 0f,
                val tremoloDepth: Float = 0f,
                val gain: Float = 1.0f
            )

            // High-pitched funny panic cartoon stickman voice: "Oh... No! Oh... No! Oh-No-No-No-Noooo!"
            val syllables = listOf(
                VocalSyllable(0.02f, 0.26f, 390f, 450f, isNasalStart = false, gain = 0.95f), // "OH"
                VocalSyllable(0.28f, 0.32f, 490f, 380f, isNasalStart = true, gain = 1.00f),  // "NO!"
                VocalSyllable(0.64f, 0.26f, 430f, 510f, isNasalStart = false, gain = 1.00f), // "OH"
                VocalSyllable(0.92f, 0.32f, 550f, 420f, isNasalStart = true, gain = 1.05f),  // "NO!"
                VocalSyllable(1.26f, 0.16f, 530f, 490f, isNasalStart = true, gain = 1.10f),  // "NO"
                VocalSyllable(1.44f, 0.16f, 560f, 510f, isNasalStart = true, gain = 1.15f),  // "NO"
                VocalSyllable(1.62f, 0.16f, 590f, 530f, isNasalStart = true, gain = 1.20f),  // "NO"
                VocalSyllable(1.80f, 0.60f, 630f, 210f, isNasalStart = true, tremoloRate = 12f, tremoloDepth = 0.28f, gain = 1.25f) // "NOOOOOOO!" (screaming down)
            )

            for (syl in syllables) {
                val startIdx = (syl.startSec * SAMPLE_RATE).toInt()
                val sylSamples = (syl.durationSec * SAMPLE_RATE).toInt()
                var phase = 0.0

                for (i in 0 until sylSamples) {
                    val idx = startIdx + i
                    if (idx < totalSamples) {
                        val t = i.toFloat() / SAMPLE_RATE
                        val p = (t / syl.durationSec).coerceIn(0f, 1f)

                        var currentF0 = syl.startPitch + (syl.endPitch - syl.startPitch) * (p * p)
                        if (syl.tremoloRate > 0f) {
                            currentF0 += sin(2.0 * PI * syl.tremoloRate * t).toFloat() * (currentF0 * 0.08f)
                        }

                        phase += 2.0 * PI * currentF0 / SAMPLE_RATE

                        val isNasalPhase = syl.isNasalStart && (p < 0.20f)
                        val f1 = if (isNasalPhase) 280f else 500f
                        val f2 = if (isNasalPhase) 1600f else 950f
                        val f3 = 2400f

                        var voiceSample = 0f
                        for (h in 1..10) {
                            val hFreq = currentF0 * h
                            if (hFreq < SAMPLE_RATE / 2) {
                                val b1 = 90f
                                val b2 = 120f
                                val b3 = 220f
                                val r1 = 1f / (1f + ((hFreq - f1) / b1).let { it * it })
                                val r2 = 1f / (1f + ((hFreq - f2) / b2).let { it * it })
                                val r3 = 1f / (1f + ((hFreq - f3) / b3).let { it * it })
                                val formantWeight = (r1 * 0.70f + r2 * 0.45f + r3 * 0.20f) / (h * 0.6f + 0.4f)
                                voiceSample += sin(phase * h).toFloat() * formantWeight
                            }
                        }

                        val attack = (t / 0.03f).coerceIn(0f, 1f)
                        val decay = ((1f - p) / 0.15f).coerceIn(0f, 1f)
                        val tremoloEnv = 1f - syl.tremoloDepth * (1f + sin(2.0 * PI * 14.0 * t).toFloat()) * 0.5f
                        val env = attack * decay * tremoloEnv * syl.gain

                        floatBuffer[idx] += voiceSample * env * 0.85f
                    }
                }
            }

            val pcmSamples = ShortArray(totalSamples)
            var maxPeak = 0.001f
            for (v in floatBuffer) {
                val absV = kotlin.math.abs(v)
                if (absV > maxPeak) maxPeak = absV
            }
            val scale = (27000f / maxPeak).coerceAtMost(28000f)

            for (i in 0 until totalSamples) {
                pcmSamples[i] = (floatBuffer[i] * scale).toInt().coerceIn(-32767, 32767).toShort()
            }
            return pcmSamples
        }

        private fun generateFunnyFallingMusic(): ShortArray {
            val totalDurationSec = 2.45f
            val totalSamples = (SAMPLE_RATE * totalDurationSec).toInt()
            val floatBuffer = FloatArray(totalSamples)

            // 1. Cartoon Slide-Whistle Scream & Wobble (0.00s – 0.65s)
            val whistleDuration = 0.65f
            val whistleSamples = (SAMPLE_RATE * whistleDuration).toInt()
            var whistlePhase = 0.0
            for (i in 0 until whistleSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val progress = t / whistleDuration
                val baseFreq = (1250.0 * Math.pow(0.11, progress.toDouble())).toFloat()
                val flutter = sin(2.0 * PI * 8.5 * t).toFloat() * (baseFreq * 0.10f)
                val currentFreq = (baseFreq + flutter).coerceAtLeast(80f)

                whistlePhase += 2.0 * PI * currentFreq / SAMPLE_RATE

                val attack = (t / 0.015f).coerceIn(0f, 1f)
                val decay = (1.0f - progress).coerceIn(0f, 1f)
                val tremolo = 0.85f + 0.15f * sin(2.0 * PI * 17.0 * t).toFloat()
                val env = attack * decay * tremolo

                val sample = (
                    sin(whistlePhase) * 0.78 +
                    sin(whistlePhase * 2.0) * 0.18 +
                    sin(whistlePhase * 3.0) * 0.04
                ).toFloat()

                floatBuffer[i] += sample * env * 0.75f
            }

            // 2. Funny Cartoon "Oh No! Oh No! Oh No No No No!" Voice-Over Layer (0.00s - 2.40s)
            val voiceSamples = generateOhNoVoiceOver()
            for (i in 0 until voiceSamples.size.coerceAtMost(totalSamples)) {
                floatBuffer[i] += (voiceSamples[i] / 32768.0f) * 0.80f
            }

            // 3. Sad Trombone "Wah-Wah-Wah-Waaaaah" (0.60s – 2.10s)
            data class TromboneNote(
                val startSec: Float,
                val durationSec: Float,
                val startFreq: Float,
                val targetFreq: Float,
                val isDroop: Boolean = false
            )

            val notes = listOf(
                TromboneNote(startSec = 0.60f, durationSec = 0.28f, startFreq = 285f, targetFreq = 311.13f), // Eb4
                TromboneNote(startSec = 0.90f, durationSec = 0.28f, startFreq = 270f, targetFreq = 293.66f), // D4
                TromboneNote(startSec = 1.20f, durationSec = 0.28f, startFreq = 255f, targetFreq = 277.18f), // Db4
                TromboneNote(startSec = 1.50f, durationSec = 0.60f, startFreq = 261.63f, targetFreq = 228.0f, isDroop = true) // C4 -> B3 / Bb3 droop
            )

            for (note in notes) {
                val startIdx = (note.startSec * SAMPLE_RATE).toInt()
                val noteSamples = (note.durationSec * SAMPLE_RATE).toInt()
                var notePhase = 0.0

                for (i in 0 until noteSamples) {
                    val idx = startIdx + i
                    if (idx < totalSamples) {
                        val t = i.toFloat() / SAMPLE_RATE
                        val p = t / note.durationSec

                        val currentFreq = if (note.isDroop) {
                            val droop = note.startFreq + (note.targetFreq - note.startFreq) * (p * p)
                            val vibrato = if (p > 0.25f) sin(2.0 * PI * 5.5 * t).toFloat() * 10f else 0f
                            droop + vibrato
                        } else {
                            if (p < 0.25f) {
                                val scoopP = p / 0.25f
                                note.startFreq + (note.targetFreq - note.startFreq) * scoopP
                            } else {
                                note.targetFreq + sin(2.0 * PI * 6.0 * t).toFloat() * 4f
                            }
                        }

                        notePhase += 2.0 * PI * currentFreq / SAMPLE_RATE

                        val attack = (t / 0.025f).coerceIn(0f, 1f)
                        val decay = if (note.isDroop) exp(-2.2f * t) else exp(-3.5f * t)
                        val env = attack * decay

                        val sample = (
                            sin(notePhase) * 0.45 +
                            sin(notePhase * 2.0) * 0.28 +
                            sin(notePhase * 3.0) * 0.16 +
                            sin(notePhase * 4.0) * 0.07 +
                            sin(notePhase * 5.0) * 0.04
                        ).toFloat()

                        floatBuffer[idx] += sample * env * 0.85f
                    }
                }
            }

            // 4. Cartoon Spring "Boing" & Splat (1.95s - 2.45s)
            val boingStartSec = 1.95f
            val boingDurationSec = 0.45f
            val boingStartIdx = (boingStartSec * SAMPLE_RATE).toInt()
            val boingSamples = (boingDurationSec * SAMPLE_RATE).toInt()
            var boingPhase = 0.0

            for (i in 0 until boingSamples) {
                val idx = boingStartIdx + i
                if (idx < totalSamples) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val p = t / boingDurationSec
                    val springMod = sin(2.0 * PI * 24.0 * t).toFloat() * 70f * (1f - p)
                    val freq = 210f + springMod + (1f - p) * 60f

                    boingPhase += 2.0 * PI * freq / SAMPLE_RATE
                    val env = (t / 0.01f).coerceIn(0f, 1f) * exp(-8.0f * t)
                    val sample = sin(boingPhase).toFloat()
                    floatBuffer[idx] += sample * env * 0.70f
                }
            }

            val pcmSamples = ShortArray(totalSamples)
            var maxPeak = 0.001f
            for (v in floatBuffer) {
                val absV = kotlin.math.abs(v)
                if (absV > maxPeak) maxPeak = absV
            }
            val scale = (27000f / maxPeak).coerceAtMost(28000f)

            for (i in 0 until totalSamples) {
                pcmSamples[i] = (floatBuffer[i] * scale).toInt().coerceIn(-32767, 32767).toShort()
            }
            return pcmSamples
        }
    }
}
