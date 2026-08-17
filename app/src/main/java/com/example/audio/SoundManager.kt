package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Ultra-Low-Latency, Real-Time Streaming Audio Mixer.
 * Uses a single AudioTrack in MODE_STREAM with fast low-latency performance mode.
 * Memory is managed completely on JVM heap without static ashmem allocations.
 */
class SoundManager(context: Context) {

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    // Pre-computed raw waveform samples in JVM heap
    private val pcmTick1 by lazy { generateTone(freq = 440f, durationSec = 0.035f, decayRate = 60f) }
    private val pcmTick2 by lazy { generateTone(freq = 554f, durationSec = 0.035f, decayRate = 60f) }
    private val pcmTick3 by lazy { generateTone(freq = 659f, durationSec = 0.035f, decayRate = 60f) }
    private val pcmTick4 by lazy { generateTone(freq = 880f, durationSec = 0.035f, decayRate = 60f) }

    private val pcmBridgeLand by lazy { generateTone(freq = 180f, durationSec = 0.12f, decayRate = 20f) }
    private val pcmStickmanLand by lazy { generateTone(freq = 240f, durationSec = 0.08f, decayRate = 35f) }
    private val pcmGem by lazy { generateTone(freq = 1046f, durationSec = 0.15f, decayRate = 12f, harmonic = 2093f) }
    private val pcmPerfect by lazy { generateChime(freq1 = 880f, freq2 = 1320f, freq3 = 1760f, durationSec = 0.30f) }
    private val pcmFlip by lazy { generateSweep(startFreq = 300f, endFreq = 600f, durationSec = 0.06f) }
    private val pcmFall by lazy { generateSweep(startFreq = 400f, endFreq = 100f, durationSec = 0.25f) }
    private val pcmGameOver by lazy { generateMinorChord(durationSec = 0.40f) }
    private val pcmButton by lazy { generateTone(freq = 520f, durationSec = 0.04f, decayRate = 50f) }
    private val pcmVictory by lazy { generateFanfare(durationSec = 0.45f) }
    private val pcmBuySuccess by lazy { generateChime(freq1 = 660f, freq2 = 880f, freq3 = 1320f, durationSec = 0.25f) }

    @Volatile private var audioTrack: AudioTrack? = null
    private val isRunning = AtomicBoolean(true)
    private val isInitialized = AtomicBoolean(false)
    private val activeSounds = ConcurrentLinkedQueue<ActiveVoice>()

    private class ActiveVoice(
        val samples: ShortArray,
        var position: Int = 0,
        val volume: Float = 1.0f
    )

    private fun ensureAudioEngine() {
        if (!isInitialized.compareAndSet(false, true)) return

        thread(name = "AudioEngineInit", isDaemon = true) {
            try {
                val minBufSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(1024)

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
                    .setBufferSizeInBytes(minBufSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()

                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.play()
                    audioTrack = track

                    // Audio pump loop
                    val frameSize = 256
                    val mixBuffer = ShortArray(frameSize)

                    while (isRunning.get()) {
                        if (activeSounds.isEmpty()) {
                            try {
                                Thread.sleep(12)
                            } catch (_: InterruptedException) {
                                break
                            }
                            continue
                        }

                        mixBuffer.fill(0)
                        var hasPlayingVoices = false

                        val iterator = activeSounds.iterator()
                        while (iterator.hasNext()) {
                            val voice = iterator.next()
                            hasPlayingVoices = true
                            val remaining = voice.samples.size - voice.position
                            val count = minOf(frameSize, remaining)

                            for (i in 0 until count) {
                                val sample = (voice.samples[voice.position + i] * voice.volume).toInt()
                                val mixed = mixBuffer[i] + sample
                                mixBuffer[i] = mixed.coerceIn(-32767, 32767).toShort()
                            }

                            voice.position += count
                            if (voice.position >= voice.samples.size) {
                                iterator.remove()
                            }
                        }

                        if (hasPlayingVoices && isRunning.get()) {
                            audioTrack?.write(mixBuffer, 0, frameSize)
                        }
                    }
                } else {
                    track.release()
                }
            } catch (t: Throwable) {
                Log.w("SoundManager", "Audio initialization gracefully bypassed", t)
            }
        }
    }

    private fun enqueue(samples: ShortArray, volume: Float = 0.85f) {
        if (!soundEnabled) return
        ensureAudioEngine()
        try {
            if (activeSounds.size < 6) {
                activeSounds.add(ActiveVoice(samples, 0, volume.coerceIn(0f, 1f)))
            }
        } catch (_: Throwable) {}
    }

    // --- High-Quality Game Sound FX Callbacks ---

    fun playGrowTick(pitchIndex: Int) {
        val sound = when (pitchIndex % 4) {
            0 -> pcmTick1
            1 -> pcmTick2
            2 -> pcmTick3
            else -> pcmTick4
        }
        enqueue(sound, volume = 0.55f)
    }

    fun playBridgePlaced() = enqueue(pcmBridgeLand, volume = 0.85f)
    fun playBridgeLand() = enqueue(pcmBridgeLand, volume = 0.85f)
    fun playStickmanLand() = enqueue(pcmStickmanLand, volume = 0.75f)
    fun playGemCollect() = enqueue(pcmGem, volume = 0.90f)
    fun playPerfectHit() = enqueue(pcmPerfect, volume = 0.95f)
    fun playFlip() = enqueue(pcmFlip, volume = 0.75f)
    fun playBridgeFall() = enqueue(pcmFall, volume = 0.80f)
    fun playStickmanFall() = enqueue(pcmFall, volume = 0.85f)
    fun playGameOver() = enqueue(pcmGameOver, volume = 0.90f)
    fun playWalkStep() = enqueue(pcmTick1, volume = 0.25f)
    fun playButton() = enqueue(pcmButton, volume = 0.70f)
    fun playVictoryMusic() = enqueue(pcmVictory, volume = 0.95f)
    fun playBuyGemsSuccess() = enqueue(pcmBuySuccess, volume = 0.90f)
    fun playComboStreak() = enqueue(pcmPerfect, volume = 0.90f)

    fun release() {
        isRunning.set(false)
        activeSounds.clear()
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Throwable) {}
    }

    companion object {
        private const val SAMPLE_RATE = 44100

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
    }
}
