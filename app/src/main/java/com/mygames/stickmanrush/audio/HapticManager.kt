package com.mygames.stickmanrush.audio

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * High-performance tactile haptic feedback manager for Stickman Hero.
 * Built using Android's modern [VibratorManager] API (Android 12+ / API 31+) with backward compatibility
 * to [Vibrator] and [VibrationEffect].
 *
 * Provides dedicated utility methods to trigger vibration effects for:
 * - 'BridgeExtend' (growth start, continuous tension ticks, elastic spring resistance)
 * - 'LandingSuccess' (structural bridge impact, precision red-dot bullseyes, and cliff landings)
 * - Additional arcade tactile events (gem combos, flips, jumps, power-ups, game over).
 */
class HapticManager(private val context: Context) {

    // Modern Android 12+ (API 31+) VibratorManager
    private val vibratorManager: VibratorManager? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        } else {
            null
        }
    } catch (_: Throwable) {
        null
    }

    // Default hardware vibrator handle
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Throwable) {
        null
    }

    val hasHardwareVibrator: Boolean = try {
        vibrator?.hasVibrator() == true
    } catch (_: Throwable) {
        false
    }

    var isEnabled: Boolean = true

    /**
     * Dispatches vibration using [VibratorManager] on Android S+ or [Vibrator] on older APIs.
     */
    private fun playVibrationEffect(effect: VibrationEffect, fallbackDurationMs: Long = 20L) {
        if (!isEnabled || !hasHardwareVibrator) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && vibratorManager != null) {
                // Use modern VibratorManager API with parallel CombinedVibration
                vibratorManager.vibrate(CombinedVibration.createParallel(effect))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator != null) {
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(fallbackDurationMs)
            }
        } catch (_: Throwable) {}
    }

    // =========================================================================
    // PRIMARY GAME EVENT UTILITY METHODS: 'BridgeExtend' & 'LandingSuccess'
    // =========================================================================

    /**
     * Utility method to trigger vibration for the 'BridgeExtend' game event.
     * Generates dynamic tactile micro-pulses whose amplitude and duration scale
     * with bridge extension length and current difficulty tier.
     *
     * @param stretchRatio Current bridge stretch ratio (0.0f to 1.0f).
     * @param tierLevel Current difficulty tier (1 to 5+).
     */
    fun triggerBridgeExtend(stretchRatio: Float = 0f, tierLevel: Int = 1) {
        bridgeGrowTick(stretchRatio, tierLevel)
    }

    /**
     * Convenience alias for 'BridgeExtend' game event.
     */
    fun bridgeExtend(stretchRatio: Float = 0f, tierLevel: Int = 1) {
        triggerBridgeExtend(stretchRatio, tierLevel)
    }

    /**
     * Utility method to trigger vibration for the 'LandingSuccess' game event.
     * Plays a crisp structural landing waveform, with distinct celebratory patterns
     * for precision bullseyes and cliff platform heights.
     *
     * @param tierLevel Current difficulty tier.
     * @param isBullseye True if bridge hit the precision center red dot.
     * @param isElevated True if landing on a cliff or incline platform.
     * @param isNearMiss True if the bridge landed on the edge.
     */
    fun triggerLandingSuccess(
        tierLevel: Int = 1,
        isBullseye: Boolean = false,
        isElevated: Boolean = false,
        isNearMiss: Boolean = false
    ) {
        if (isBullseye) {
            perfectHit(tierLevel)
        } else {
            bridgePlaced(tierLevel = tierLevel, isElevated = isElevated, isNearMiss = isNearMiss)
        }
    }

    /**
     * Convenience alias for 'LandingSuccess' game event.
     */
    fun landingSuccess(
        tierLevel: Int = 1,
        isBullseye: Boolean = false,
        isElevated: Boolean = false,
        isNearMiss: Boolean = false
    ) {
        triggerLandingSuccess(tierLevel, isBullseye, isElevated, isNearMiss)
    }

    // =========================================================================
    // UNIFIED GAME EVENT DISPATCHER
    // =========================================================================

    /**
     * Strongly typed game haptic events for clean event-driven architectures.
     */
    sealed interface GameHapticEvent {
        data class BridgeExtend(val stretchRatio: Float = 0f, val tierLevel: Int = 1) : GameHapticEvent
        data class LandingSuccess(
            val tierLevel: Int = 1,
            val isBullseye: Boolean = false,
            val isElevated: Boolean = false,
            val isNearMiss: Boolean = false
        ) : GameHapticEvent
        object BridgeStart : GameHapticEvent
        object BridgeRelease : GameHapticEvent
        object BridgeFail : GameHapticEvent
        data class GemCollect(val comboMultiplier: Int = 1) : GameHapticEvent
        object GameOver : GameHapticEvent
        object Flip : GameHapticEvent
        object Jump : GameHapticEvent
        object JumpLanding : GameHapticEvent
        object UiClick : GameHapticEvent
        object LevelUp : GameHapticEvent
    }

    /**
     * Dispatch haptic effects by [GameHapticEvent].
     */
    fun trigger(event: GameHapticEvent) {
        when (event) {
            is GameHapticEvent.BridgeExtend -> triggerBridgeExtend(event.stretchRatio, event.tierLevel)
            is GameHapticEvent.LandingSuccess -> triggerLandingSuccess(event.tierLevel, event.isBullseye, event.isElevated, event.isNearMiss)
            GameHapticEvent.BridgeStart -> bridgeGrowStart()
            GameHapticEvent.BridgeRelease -> bridgeRelease()
            GameHapticEvent.BridgeFail -> bridgeFail()
            is GameHapticEvent.GemCollect -> gemCollect(event.comboMultiplier)
            GameHapticEvent.GameOver -> gameOver()
            GameHapticEvent.Flip -> flip()
            GameHapticEvent.Jump -> jump()
            GameHapticEvent.JumpLanding -> jumpLanding()
            GameHapticEvent.UiClick -> uiClick()
            GameHapticEvent.LevelUp -> levelUp()
        }
    }

    // =========================================================================
    // DETAILED BRIDGE & GAMEPLAY TACTILE EFFECTS
    // =========================================================================

    /**
     * Crisp contact impulse when the player first touches the screen to begin stretching the bridge.
     */
    fun bridgeGrowStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playVibrationEffect(VibrationEffect.createOneShot(10L, 85), 10L)
        } else {
            playVibrationEffectLegacy(10L)
        }
    }

    /**
     * Progressive micro-click during bridge stretching that scales dynamically
     * with length tension and difficulty tier, creating realistic mechanical resistance.
     */
    fun bridgeGrowTick(stretchRatio: Float = 0f, tierLevel: Int = 1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = (6 + (stretchRatio * 8f) + (tierLevel * 1.5f)).toLong().coerceIn(6L, 16L)
            val baseAmp = when (tierLevel) {
                1 -> 50
                2 -> 85
                3 -> 120
                4 -> 160
                5 -> 200
                else -> 230
            }
            val tensionBoost = (stretchRatio * 65f).toInt()
            val finalAmp = (baseAmp + tensionBoost).coerceIn(40, 255)
            playVibrationEffect(VibrationEffect.createOneShot(duration, finalAmp), (6 + tierLevel * 2).toLong())
        } else {
            playVibrationEffectLegacy((6 + tierLevel * 2).toLong())
        }
    }

    /**
     * Legacy alias for backward compatibility.
     */
    fun tick(tierLevel: Int = 1) {
        bridgeGrowTick(0f, tierLevel)
    }

    /**
     * Tactile release snap when the player lifts their finger and the bridge drops forward.
     */
    fun bridgeRelease() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playVibrationEffect(VibrationEffect.createOneShot(14L, 110), 12L)
        } else {
            playVibrationEffectLegacy(12L)
        }
    }

    /**
     * Solid progressive physical impact when the bridge slams onto the target platform.
     * Incorporates structural vibration waveform for authentic physical weight.
     */
    fun bridgePlaced(
        tierLevel: Int = 1,
        isElevated: Boolean = false,
        isNearMiss: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val baseAmp = (140 + tierLevel * 20).coerceIn(140, 240)
            val (timings, amplitudes) = if (isNearMiss) {
                // Stuttering vibration warning of close-call tip landing
                longArrayOf(0, 18, 16, 28) to intArrayOf(0, baseAmp, 0, 230)
            } else if (isElevated) {
                // Heavy slanted cliff impact waveform
                longArrayOf(0, 24, 18, 16) to intArrayOf(0, (baseAmp + 25).coerceAtMost(255), 0, (baseAmp * 0.6f).toInt())
            } else {
                // Crisp structural impact + dampening echo
                longArrayOf(0, 20, 14, 12) to intArrayOf(0, baseAmp, 0, (baseAmp * 0.5f).toInt())
            }
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 28L)
        } else {
            playVibrationEffectLegacy(28L)
        }
    }

    /**
     * Deep hollow failure thud when bridge falls into the abyss (too short or too long).
     */
    fun bridgeFail() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 35, 25, 45)
            val amplitudes = intArrayOf(0, 180, 0, 130)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 40L)
        } else {
            playVibrationEffectLegacy(40L)
        }
    }

    /**
     * Progressive celebratory rhythmic pulse for a precision center red-dot bullseye!
     */
    fun perfectHit(tierLevel: Int = 1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val baseAmp = (180 + tierLevel * 15).coerceIn(180, 255)
            val timings = longArrayOf(0, 25, 35, 45, 35, 60)
            val amplitudes = intArrayOf(0, (baseAmp * 0.75f).toInt(), 0, baseAmp, 0, 255)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 50L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    vibrator?.vibrate(longArrayOf(0, 30, 40, 50), -1)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Crisp high-frequency tactile sparkle chime when collecting a gem.
     * Scales dynamically with combo multipliers (2x, 3x, 4x+) into celebratory multi-pulse sparkle bursts!
     */
    fun gemCollect(comboMultiplier: Int = 1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val (timings, amplitudes) = when {
                comboMultiplier >= 3 -> {
                    longArrayOf(0, 8, 12, 10, 12, 14, 14, 24) to intArrayOf(0, 140, 0, 175, 0, 215, 0, 255)
                }
                comboMultiplier == 2 -> {
                    longArrayOf(0, 10, 14, 12, 14, 20) to intArrayOf(0, 130, 0, 180, 0, 235)
                }
                else -> {
                    longArrayOf(0, 12, 14, 18) to intArrayOf(0, 120, 0, 200)
                }
            }
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 20L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    when {
                        comboMultiplier >= 3 -> vibrator?.vibrate(longArrayOf(0, 10, 15, 12, 15, 20), -1)
                        comboMultiplier == 2 -> vibrator?.vibrate(longArrayOf(0, 10, 15, 16), -1)
                        else -> vibrator?.vibrate(longArrayOf(0, 12, 15, 18), -1)
                    }
                }
            } catch (_: Throwable) {}
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val (timings, amplitudes) = when {
                isSliding -> {
                    longArrayOf(0, 10, 14, 12, 14, 18) to intArrayOf(0, 90, 0, 140, 0, 190)
                }
                isElevated -> {
                    longArrayOf(0, 20, 22, 30) to intArrayOf(
                        0,
                        (160 + tierLevel * 15).coerceIn(160, 240),
                        0,
                        (210 + tierLevel * 10).coerceIn(210, 255)
                    )
                }
                else -> {
                    longArrayOf(0, 16, 20, 26) to intArrayOf(
                        0,
                        (130 + tierLevel * 15).coerceIn(130, 220),
                        0,
                        (170 + tierLevel * 15).coerceIn(170, 255)
                    )
                }
            }
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 24L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    if (isElevated) {
                        vibrator?.vibrate(longArrayOf(0, 20, 25, 30), -1)
                    } else {
                        vibrator?.vibrate(longArrayOf(0, 16, 20, 24), -1)
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Quick flutter pulse when flipping upside down / upright
     */
    fun flip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playVibrationEffect(VibrationEffect.createOneShot(12, 100), 12L)
        } else {
            playVibrationEffectLegacy(12L)
        }
    }

    /**
     * Crisp upward elastic spring pulse when performing a jump
     */
    fun jump() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 10, 12, 18)
            val amplitudes = intArrayOf(0, 110, 0, 190)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 18L)
        } else {
            playVibrationEffectLegacy(18L)
        }
    }

    /**
     * Cushion spring impact when landing back onto the bridge after an aerial jump
     */
    fun jumpLanding() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 12, 10, 16)
            val amplitudes = intArrayOf(0, 95, 0, 150)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 15L)
        } else {
            playVibrationEffectLegacy(15L)
        }
    }

    /**
     * Heavy rumble vibration on game over fall or wall crash
     */
    fun gameOver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 70, 50, 110)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 100L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    vibrator?.vibrate(longArrayOf(0, 70, 50, 110), -1)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Epic celebratory multi-burst pulse when reaching high streak milestones (10, 20, 30+).
     */
    fun streakBonus(streak: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = when {
                streak >= 30 -> longArrayOf(0, 30, 20, 40, 20, 60, 20, 90)
                streak >= 20 -> longArrayOf(0, 25, 20, 35, 20, 50)
                else -> longArrayOf(0, 20, 20, 30)
            }
            val amplitudes = when {
                streak >= 30 -> intArrayOf(0, 160, 0, 200, 0, 230, 0, 255)
                streak >= 20 -> intArrayOf(0, 140, 0, 190, 0, 240)
                else -> intArrayOf(0, 120, 0, 180)
            }
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 80L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    vibrator?.vibrate(longArrayOf(0, 30, 30, 50, 30, 80), -1)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Light UI interaction click
     */
    fun uiClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playVibrationEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), 10L)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playVibrationEffect(VibrationEffect.createOneShot(10, 80), 10L)
        } else {
            playVibrationEffectLegacy(10L)
        }
    }

    /**
     * Subtle pulse on near-miss close call
     */
    fun nearMiss() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 20, 30, 60)
            val amplitudes = intArrayOf(0, 180, 0, 240)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 60L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    vibrator?.vibrate(longArrayOf(0, 20, 30, 60), -1)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Exciting celebratory burst on leveling up and milestone reached
     */
    fun levelUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 35, 45, 45, 45, 75)
            val amplitudes = intArrayOf(0, 160, 0, 210, 0, 255)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 75L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    vibrator?.vibrate(longArrayOf(0, 35, 45, 45, 45, 75), -1)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Fanfare buzz when a daily mission is completed or claimed
     */
    fun missionClaim() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 40, 50, 40, 50, 80)
            val amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 80L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    vibrator?.vibrate(longArrayOf(0, 40, 50, 40, 50, 80), -1)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Crispy rhythmic surge when collecting a tactical Power-Up item (Magnet / Shield / 2X)
     */
    fun powerUpPickup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 30, 25, 30, 25, 60)
            val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 60L)
        } else {
            @Suppress("DEPRECATION")
            try {
                if (isEnabled && hasHardwareVibrator) {
                    vibrator?.vibrate(longArrayOf(0, 30, 25, 30, 25, 60), -1)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Heavy shockwave tactile punch when Invincibility Shield shatters and absorbs a fatal collision
     */
    fun shieldShatter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 60, 40, 90)
            val amplitudes = intArrayOf(0, 255, 0, 220)
            playVibrationEffect(VibrationEffect.createWaveform(timings, amplitudes, -1), 80L)
        } else {
            playVibrationEffectLegacy(80L)
        }
    }

    /**
     * Subtle magnetic vibration pulse as gems are attracted into the stickman vortex
     */
    fun magnetPulse() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playVibrationEffect(VibrationEffect.createOneShot(14L, 110), 14L)
        } else {
            playVibrationEffectLegacy(14L)
        }
    }

    /**
     * Cancel any active vibration pattern immediately.
     */
    fun cancel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && vibratorManager != null) {
                vibratorManager.cancel()
            } else {
                @Suppress("DEPRECATION")
                vibrator?.cancel()
            }
        } catch (_: Throwable) {}
    }

    private fun playVibrationEffectLegacy(durationMs: Long) {
        if (!isEnabled || !hasHardwareVibrator || vibrator == null) return
        try {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        } catch (_: Throwable) {}
    }
}
