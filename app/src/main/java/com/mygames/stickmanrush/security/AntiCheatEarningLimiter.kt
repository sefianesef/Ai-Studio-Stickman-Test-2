package com.mygames.stickmanrush.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Anomaly Record for security audits and telemetry reporting.
 */
data class CheatIncident(
    val timestampMs: Long,
    val reason: String,
    val attemptedAmount: Int,
    val sourceName: String,
    val velocityInWindow: Int
)

/**
 * Evaluation Result from the Anti-Cheat Velocity Engine.
 */
data class EarningEvaluationResult(
    val isAllowed: Boolean,
    val sanitizedAmount: Int,
    val rejectionReason: String? = null,
    val isCheaterFlagged: Boolean = false
)

/**
 * Abnormal Earning Limits & Anti-Cheat Velocity Engine
 *
 * Enforces strict mathematical ceilings on currency gains derived from normal gameplay mechanics.
 * Blocks and flags players attempting memory injection (GameGuardian/CheatEngine),
 * Frida script speedhacks, or modified APK transaction injections.
 *
 * Mechanical Gameplay Physics Limits:
 * 1. Bridge Crossing Speed: ~1.5 - 2.0s per bridge minimum. Max ~1-2 gems per bridge.
 * 2. 10-Second Burst Limit: Max 15 gameplay gems.
 * 3. 60-Second Window Limit: Max 40 gameplay gems, Max 150 total non-IAP gems.
 * 4. 1-Hour Rolling Window Limit: Max 600 total non-IAP gems.
 * 5. Single Transaction Limits: Strictly capped by [CurrencySource.maxAllowedAmount].
 */
class AntiCheatEarningLimiter(private val context: Context) {

    companion object {
        private const val TAG = "AntiCheatLimiter"
        private const val PREFS_NAME = "stickman_anticheat_vault"
        private const val KEY_IS_CHEAT_FLAGGED = "KEY_IS_CHEAT_FLAGGED"
        private const val KEY_VIOLATION_COUNT = "KEY_VIOLATION_COUNT"
        private const val KEY_LAST_FLAG_TIMESTAMP = "KEY_LAST_FLAG_TIMESTAMP"

        // Rate-limiting constants based on game physics
        const val MAX_GAMEPLAY_GEMS_BURST_10S = 15
        const val MAX_GAMEPLAY_GEMS_PER_MINUTE = 40
        const val MAX_TOTAL_NON_IAP_GEMS_PER_MINUTE = 160
        const val MAX_TOTAL_NON_IAP_GEMS_PER_HOUR = 700
    }

    private val prefs: SharedPreferences =
        EncryptedSaveStorage.createEncryptedSharedPreferences(context, PREFS_NAME)

    private val _isCheaterFlagged = MutableStateFlow(prefs.getBoolean(KEY_IS_CHEAT_FLAGGED, false))
    val isCheaterFlagged: StateFlow<Boolean> = _isCheaterFlagged.asStateFlow()

    // Rolling window transaction records: Pair(timestampMs, amount)
    private val recentTransactions = ConcurrentLinkedQueue<Pair<Long, Int>>()
    private val incidentHistory = ConcurrentLinkedQueue<CheatIncident>()

    /**
     * Evaluates a proposed currency grant against all mechanical velocity limits.
     */
    @Synchronized
    fun evaluateGemGain(
        amount: Int,
        source: CurrencySource,
        isVerifiedIap: Boolean = false
    ): EarningEvaluationResult {
        // 1. Negative or zero amount is an anomaly
        if (amount <= 0) {
            return EarningEvaluationResult(
                isAllowed = false,
                sanitizedAmount = 0,
                rejectionReason = "INVALID_AMOUNT_NON_POSITIVE"
            )
        }

        // 2. Verified Google Play In-App Purchases bypass gameplay velocity limits
        if (isVerifiedIap && source == CurrencySource.IN_APP_PURCHASE) {
            return EarningEvaluationResult(
                isAllowed = true,
                sanitizedAmount = amount.coerceAtMost(source.maxAllowedAmount)
            )
        }

        // 3. Single-transaction ceiling check
        if (amount > source.maxAllowedAmount) {
            flagViolation(
                reason = "SINGLE_TX_CEILING_EXCEEDED",
                attemptedAmount = amount,
                source = source,
                velocityInWindow = amount
            )
            return EarningEvaluationResult(
                isAllowed = false,
                sanitizedAmount = 0,
                rejectionReason = "EXCEEDS_SOURCE_CEILING_${source.maxAllowedAmount}",
                isCheaterFlagged = true
            )
        }

        val now = System.currentTimeMillis()
        pruneOldRecords(now)

        // 4. Calculate rolling window velocities
        val gemsInLast10s = recentTransactions.filter { it.first >= now - 10_000L }.sumOf { it.second }
        val gemsInLast60s = recentTransactions.filter { it.first >= now - 60_000L }.sumOf { it.second }
        val gemsInLastHour = recentTransactions.filter { it.first >= now - 3600_000L }.sumOf { it.second }

        // 5. Check 10-Second Burst Limit for in-run gameplay pickups
        if (source == CurrencySource.GAMEPLAY_COLLECT || source == CurrencySource.PERFECT_BULLSEYE) {
            if (gemsInLast10s + amount > MAX_GAMEPLAY_GEMS_BURST_10S) {
                flagViolation(
                    reason = "GAMEPLAY_10S_BURST_LIMIT_EXCEEDED",
                    attemptedAmount = amount,
                    source = source,
                    velocityInWindow = gemsInLast10s + amount
                )
                return EarningEvaluationResult(
                    isAllowed = false,
                    sanitizedAmount = 0,
                    rejectionReason = "BURST_RATE_LIMIT_EXCEEDED",
                    isCheaterFlagged = true
                )
            }
        }

        // 6. Check 60-Second Window Limit
        if (source == CurrencySource.GAMEPLAY_COLLECT) {
            if (gemsInLast60s + amount > MAX_GAMEPLAY_GEMS_PER_MINUTE) {
                flagViolation(
                    reason = "GAMEPLAY_60S_VELOCITY_EXCEEDED",
                    attemptedAmount = amount,
                    source = source,
                    velocityInWindow = gemsInLast60s + amount
                )
                return EarningEvaluationResult(
                    isAllowed = false,
                    sanitizedAmount = 0,
                    rejectionReason = "MINUTE_RATE_LIMIT_EXCEEDED",
                    isCheaterFlagged = true
                )
            }
        }

        if (gemsInLast60s + amount > MAX_TOTAL_NON_IAP_GEMS_PER_MINUTE) {
            flagViolation(
                reason = "TOTAL_NON_IAP_60S_VELOCITY_EXCEEDED",
                attemptedAmount = amount,
                source = source,
                velocityInWindow = gemsInLast60s + amount
            )
            return EarningEvaluationResult(
                isAllowed = false,
                sanitizedAmount = 0,
                rejectionReason = "TOTAL_VELOCITY_EXCEEDED",
                isCheaterFlagged = true
            )
        }

        // 7. Check 1-Hour Rolling Window Limit
        if (gemsInLastHour + amount > MAX_TOTAL_NON_IAP_GEMS_PER_HOUR) {
            flagViolation(
                reason = "HOURLY_VELOCITY_CEILING_BREACHED",
                attemptedAmount = amount,
                source = source,
                velocityInWindow = gemsInLastHour + amount
            )
            return EarningEvaluationResult(
                isAllowed = false,
                sanitizedAmount = 0,
                rejectionReason = "HOURLY_CAP_EXCEEDED",
                isCheaterFlagged = true
            )
        }

        // Transaction approved within normal mechanical gameplay parameters
        recentTransactions.add(Pair(now, amount))
        return EarningEvaluationResult(
            isAllowed = true,
            sanitizedAmount = amount
        )
    }

    private fun pruneOldRecords(now: Long) {
        val oneHourAgo = now - 3600_000L
        while (true) {
            val head = recentTransactions.peek() ?: break
            if (head.first < oneHourAgo) {
                recentTransactions.poll()
            } else {
                break
            }
        }
    }

    private fun flagViolation(
        reason: String,
        attemptedAmount: Int,
        source: CurrencySource,
        velocityInWindow: Int
    ) {
        val now = System.currentTimeMillis()
        val incident = CheatIncident(
            timestampMs = now,
            reason = reason,
            attemptedAmount = attemptedAmount,
            sourceName = source.name,
            velocityInWindow = velocityInWindow
        )
        incidentHistory.add(incident)
        _isCheaterFlagged.value = true

        val currentCount = prefs.getInt(KEY_VIOLATION_COUNT, 0) + 1
        prefs.edit()
            .putBoolean(KEY_IS_CHEAT_FLAGGED, true)
            .putInt(KEY_VIOLATION_COUNT, currentCount)
            .putLong(KEY_LAST_FLAG_TIMESTAMP, now)
            .apply()

        Log.e(TAG, "ANTI-CHEAT ALERT: Account flagged! Violation: $reason, Amount: $attemptedAmount, Source: ${source.name}, Velocity: $velocityInWindow")
    }

    fun isCheater(): Boolean = _isCheaterFlagged.value

    fun getViolationCount(): Int = prefs.getInt(KEY_VIOLATION_COUNT, 0)

    fun getIncidentLog(): List<CheatIncident> = incidentHistory.toList()
}
