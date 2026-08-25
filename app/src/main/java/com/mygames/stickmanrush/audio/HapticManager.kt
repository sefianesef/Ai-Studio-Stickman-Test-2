package com.mygames.stickmanrush.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * High-performance tactile haptic feedback manager for Stickman Hero.
 * Provides custom vibration patterns for bridge growth ticks, bridge placement,
 * perfect bullseye hits, gem pickups, flip maneuvers, and game over impacts.
 */
class HapticManager(private val context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Throwable) {
        null
    }

    private val hasHardwareVibrator: Boolean = try {
        vibrator?.hasVibrator() == true
    } catch (_: Throwable) {
        false
    }

    var isEnabled: Boolean = true

    /**
     * Progressive micro-click during bridge stretching that scales dynamically
     * with the difficulty tier and level tension!
     */
    fun tick(tierLevel: Int = 1) {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = (5 + tierLevel * 2).toLong().coerceIn(6L, 18L)
                val amplitude = when (tierLevel) {
                    1 -> 45   // Gentle, satisfying, effortless for early levels 1-10
                    2 -> 80   // Crisp, engaging
                    3 -> 125  // Firm tactile response
                    4 -> 175  // Tense, energetic
                    5 -> 220  // High stakes master pulse
                    else -> 255 // Maximum tension legend vibration
                }
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate((6 + tierLevel * 2).toLong())
            }
        } catch (_: Throwable) {}
    }

    /**
     * Solid progressive impact when the bridge lands onto a platform
     */
    fun bridgePlaced(tierLevel: Int = 1) {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = (18 + tierLevel * 4).toLong().coerceIn(18L, 42L)
                val amplitude = (120 + tierLevel * 25).coerceIn(120, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Progressive celebratory rhythmic pulse for a precision center red-dot bullseye!
     */
    fun perfectHit(tierLevel: Int = 1) {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val baseAmp = (180 + tierLevel * 15).coerceIn(180, 255)
                val timings = longArrayOf(0, 25, 35, 45, 35, 60)
                val amplitudes = intArrayOf(0, (baseAmp * 0.75f).toInt(), 0, baseAmp, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 40, 50), -1)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Subtle pulse warning when aligning timing on dynamic moving pillars
     */
    fun movingPillarWarning() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 90))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Crisp high-frequency tactile sparkle chime when collecting a gem.
     * Scales dynamically with combo multipliers (2x, 3x, 4x+) into celebratory multi-pulse sparkle bursts!
     */
    fun gemCollect(comboMultiplier: Int = 1) {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when {
                    comboMultiplier >= 3 -> {
                        // 4-pulse ascending sparkle fanfare for mega combos
                        val timings = longArrayOf(0, 8, 12, 10, 12, 14, 14, 24)
                        val amplitudes = intArrayOf(0, 140, 0, 175, 0, 215, 0, 255)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    comboMultiplier == 2 -> {
                        // Triple ascending energetic chime for 2x combo
                        val timings = longArrayOf(0, 10, 14, 12, 14, 20)
                        val amplitudes = intArrayOf(0, 130, 0, 180, 0, 235)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    else -> {
                        // Crisp double-pulse diamond pop for single gem pickup
                        val timings = longArrayOf(0, 12, 14, 18)
                        val amplitudes = intArrayOf(0, 120, 0, 200)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when {
                    comboMultiplier >= 3 -> vibrator.vibrate(longArrayOf(0, 10, 15, 12, 15, 20), -1)
                    comboMultiplier == 2 -> vibrator.vibrate(longArrayOf(0, 10, 15, 16), -1)
                    else -> vibrator.vibrate(longArrayOf(0, 12, 15, 18), -1)
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Grounded, solid tactile footfall impact pattern when the stickman successfully
     * crosses the bridge and plants both feet onto the destination platform.
     */
    fun stickmanLand(
        tierLevel: Int = 1,
        isElevated: Boolean = false,
        isSliding: Boolean = false
    ) {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when {
                    isSliding -> {
                        // Slick ripple sensation for ice platforms
                        val timings = longArrayOf(0, 10, 14, 12, 14, 18)
                        val amplitudes = intArrayOf(0, 90, 0, 140, 0, 190)
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    isElevated -> {
                        // Punchier landing impact for incline / cliff platforms
                        val timings = longArrayOf(0, 20, 22, 30)
                        val amplitudes = intArrayOf(
                            0,
                            (160 + tierLevel * 15).coerceIn(160, 240),
                            0,
                            (210 + tierLevel * 10).coerceIn(210, 255)
                        )
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    else -> {
                        // Solid, satisfying step-and-plant double tap
                        val timings = longArrayOf(0, 16, 20, 26)
                        val amplitudes = intArrayOf(
                            0,
                            (130 + tierLevel * 15).coerceIn(130, 220),
                            0,
                            (170 + tierLevel * 15).coerceIn(170, 255)
                        )
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                if (isElevated) {
                    vibrator.vibrate(longArrayOf(0, 20, 25, 30), -1)
                } else {
                    vibrator.vibrate(longArrayOf(0, 16, 20, 24), -1)
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Quick flutter pulse when flipping upside down / upright
     */
    fun flip() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, 100))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Crisp upward elastic spring pulse when performing a jump
     */
    fun jump() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 10, 12, 18)
                val amplitudes = intArrayOf(0, 110, 0, 190)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Heavy rumble vibration on game over fall or wall crash
     */
    fun gameOver() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 70, 50, 110)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 70, 50, 110), -1)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Light UI interaction click
     */
    fun uiClick() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 80))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Subtle pulse on near-miss close call
     */
    fun nearMiss() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 20, 30, 60)
                val amplitudes = intArrayOf(0, 180, 0, 240)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 20, 30, 60), -1)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Exciting celebratory burst on leveling up and milestone reached
     */
    fun levelUp() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 35, 45, 45, 45, 75)
                val amplitudes = intArrayOf(0, 160, 0, 210, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 35, 45, 45, 45, 75), -1)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Fanfare buzz when a daily mission is completed or claimed
     */
    fun missionClaim() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 40, 50, 40, 50, 80)
                val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 40, 50, 40, 50, 80), -1)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Crispy rhythmic surge when collecting a tactical Power-Up item (Magnet / Shield / 2X)
     */
    fun powerUpPickup() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 25, 30, 25, 60)
                val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 25, 30, 25, 60), -1)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Heavy shockwave tactile punch when Invincibility Shield shatters and absorbs a fatal collision
     */
    fun shieldShatter() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 60, 40, 90)
                val amplitudes = intArrayOf(0, 255, 0, 220)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Subtle magnetic vibration pulse as gems are attracted into the stickman vortex
     */
    fun magnetPulse() {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(14L, 110))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(14L)
            }
        } catch (_: Throwable) {}
    }
}
