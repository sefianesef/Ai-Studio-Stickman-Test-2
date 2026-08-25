package com.mygames.stickmanrush.ads

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production-ready Ad Frequency Manager.
 * Prevents ad fatigue and maximizes player retention by enforcing four core rules:
 *
 * 1. Minimum Time Cooldown: Real-time seconds (default 90s) required between forced interstitials.
 * 2. Minimum Attempt Threshold: Minimum attempts/completions (default 3) required before an interstitial can trigger.
 * 3. Onboarding Protection: Forced interstitials disabled for early onboarding levels (Levels 1 to 3; minLevel = 4).
 * 4. Rewarded Ad Immunity Grace Period: Completing a voluntary rewarded ad grants 120s grace period and resets attempt counter.
 *
 * Also honors IAP "No Ads" and VIP Season Pass exemptions.
 */
class AdFrequencyManager private constructor() {

    companion object {
        private const val TAG = "AdFrequencyManager"

        @Volatile
        private var instance: AdFrequencyManager? = null

        fun getInstance(): AdFrequencyManager {
            return instance ?: synchronized(this) {
                instance ?: AdFrequencyManager().also { instance = it }
            }
        }
    }

    // Pacing configurations (in seconds and attempt counts)
    var minInterstitialCooldownSec: Float = 90f
    var minAttemptsBetweenAds: Int = 3
    var rewardedAdGracePeriodSec: Float = 120f
    var minLevelToEnableForcedAds: Int = 4

    // State tracking
    private var lastAdTimestampSec: Float = getUptimeSeconds()
    private var attemptsSinceLastAd: Int = 0

    private val _interstitialDisplayEvents = MutableStateFlow<Long>(0L)
    val interstitialDisplayEvents: StateFlow<Long> = _interstitialDisplayEvents.asStateFlow()

    private fun getUptimeSeconds(): Float {
        return SystemClock.elapsedRealtime() / 1000f
    }

    /**
     * Evaluates if all conditions pass to display an interstitial ad.
     * @param currentLevel Current level of the player (1-indexed)
     * @param hasNoAdsPurchased Whether the player owns No-Ads or VIP Pass
     */
    fun canShowInterstitial(currentLevel: Int, hasNoAdsPurchased: Boolean = false): Boolean {
        // VIP / No-Ads purchase exemption
        if (hasNoAdsPurchased) {
            Log.d(TAG, "Interstitial suppressed: Player has No-Ads or VIP Pass")
            return false
        }

        // 1. Protect early onboarding retention (Levels 1 to 3)
        if (currentLevel < minLevelToEnableForcedAds) {
            Log.d(TAG, "Interstitial suppressed: Onboarding protection active (Level $currentLevel < $minLevelToEnableForcedAds)")
            return false
        }

        // 2. Check if player has completed enough attempts since the last ad
        if (attemptsSinceLastAd < minAttemptsBetweenAds) {
            Log.d(TAG, "Interstitial suppressed: Attempt threshold not met ($attemptsSinceLastAd / $minAttemptsBetweenAds)")
            return false
        }

        // 3. Check elapsed real-time since last interstitial or rewarded ad
        val elapsed = getUptimeSeconds() - lastAdTimestampSec
        if (elapsed < minInterstitialCooldownSec) {
            Log.d(TAG, "Interstitial suppressed: Cooldown active (${elapsed.toInt()}s / ${minInterstitialCooldownSec.toInt()}s)")
            return false
        }

        return true
    }

    /**
     * Call whenever a level ends (Win, Loss, or Restart).
     * @param currentLevel Current level index
     * @param hasNoAdsPurchased Whether player has No-Ads
     * @param triggerInterstitial Lambda invoked if all 4 frequency conditions are satisfied
     */
    fun handleLevelEnd(
        currentLevel: Int,
        hasNoAdsPurchased: Boolean = false,
        triggerInterstitial: () -> Unit
    ) {
        attemptsSinceLastAd++
        Log.d(TAG, "Level ended. Level: $currentLevel, Attempts since last ad: $attemptsSinceLastAd")

        if (canShowInterstitial(currentLevel, hasNoAdsPurchased)) {
            Log.d(TAG, "Triggering interstitial ad.")
            triggerInterstitial()
            resetInterstitialCooldown()
        }
    }

    /**
     * Call immediately when a voluntary rewarded ad successfully finishes.
     * Resets attempt count and extends immunity so the player is rewarded for engagement.
     */
    fun onRewardedAdCompleted() {
        attemptsSinceLastAd = 0
        val bonusImmunity = maxOf(0f, rewardedAdGracePeriodSec - minInterstitialCooldownSec)
        lastAdTimestampSec = getUptimeSeconds() + bonusImmunity
        Log.d(TAG, "Rewarded ad completed. Immunity grace period applied: +${bonusImmunity}s bonus.")
    }

    /**
     * Resets timers and attempt counters after an interstitial displays successfully.
     */
    fun resetInterstitialCooldown() {
        lastAdTimestampSec = getUptimeSeconds()
        attemptsSinceLastAd = 0
        _interstitialDisplayEvents.value = System.currentTimeMillis()
    }

    /**
     * Get remaining cooldown seconds for telemetry/debug inspection.
     */
    fun getRemainingCooldownSeconds(): Float {
        val elapsed = getUptimeSeconds() - lastAdTimestampSec
        return (minInterstitialCooldownSec - elapsed).coerceAtLeast(0f)
    }

    fun getAttemptsSinceLastAd(): Int = attemptsSinceLastAd
}
