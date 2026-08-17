package com.example.audio

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

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isEnabled: Boolean = true

    /**
     * Progressive micro-click during bridge stretching that scales dynamically
     * with the difficulty tier and level tension!
     */
    fun tick(tierLevel: Int = 1) {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
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
        } catch (_: Exception) {}
    }

    /**
     * Solid progressive impact when the bridge lands onto a platform
     */
    fun bridgePlaced(tierLevel: Int = 1) {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = (18 + tierLevel * 4).toLong().coerceIn(18L, 42L)
                val amplitude = (120 + tierLevel * 25).coerceIn(120, 255)
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    /**
     * Progressive celebratory rhythmic pulse for a precision center red-dot bullseye!
     */
    fun perfectHit(tierLevel: Int = 1) {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
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
        } catch (_: Exception) {}
    }

    /**
     * Subtle pulse warning when aligning timing on dynamic moving pillars
     */
    fun movingPillarWarning() {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 90))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    /**
     * Crisp light pop when collecting a gem
     */
    fun gemCollect() {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(14, 150))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(14)
            }
        } catch (_: Exception) {}
    }

    /**
     * Quick flutter pulse when flipping upside down / upright
     */
    fun flip() {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, 100))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    /**
     * Heavy rumble vibration on game over fall or wall crash
     */
    fun gameOver() {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 70, 50, 110)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 70, 50, 110), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Light UI interaction click
     */
    fun uiClick() {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 80))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    /**
     * Subtle pulse on near-miss close call
     */
    fun nearMiss() {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 20, 30, 60)
                val amplitudes = intArrayOf(0, 180, 0, 240)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 20, 30, 60), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Exciting celebratory burst on leveling up and milestone reached
     */
    fun levelUp() {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 35, 45, 45, 45, 75)
                val amplitudes = intArrayOf(0, 160, 0, 210, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 35, 45, 45, 45, 75), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Fanfare buzz when a daily mission is completed or claimed
     */
    fun missionClaim() {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 40, 50, 40, 50, 80)
                val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 40, 50, 40, 50, 80), -1)
            }
        } catch (_: Exception) {}
    }
}
