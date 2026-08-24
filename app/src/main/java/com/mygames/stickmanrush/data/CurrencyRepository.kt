package com.mygames.stickmanrush.data

import com.mygames.stickmanrush.security.CurrencySource
import kotlinx.coroutines.flow.StateFlow

/**
 * Status of a currency transaction in the verification lifecycle.
 */
enum class TransactionStatus {
    PENDING,        // Submitted on client, waiting for server handshake
    IN_FLIGHT,      // Network handshake in progress (e.g. Firebase Cloud Function / HTTPS)
    CONFIRMED,      // Authoritatively validated by server and committed to balance
    REJECTED,       // Rejected by server due to rate limit, invalid signature, or tampering
    FAILED_NETWORK  // Transient network failure, queued for retry
}

/**
 * Represents a pending or in-flight currency mutation.
 */
data class PendingCurrencyTransaction(
    val transactionId: String,
    val playerId: String,
    val amount: Int,
    val source: CurrencySource,
    val verificationToken: String?,
    val timestampMs: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.PENDING,
    val serverAuthToken: String? = null,
    val failureReason: String? = null
)

/**
 * Result returned when initiating or completing a validated currency transaction.
 */
data class CurrencyTransactionOutcome(
    val transactionId: String,
    val isApproved: Boolean,
    val status: TransactionStatus,
    val newBalance: Int,
    val message: String? = null
)

/**
 * Defines the contract for secure, server-validated currency operations.
 * Replaces unchecked client-side balance mutations with an asynchronous
 * pending transaction handshake lifecycle.
 */
interface CurrencyRepository {
    val gems: StateFlow<Int>
    val blueGems: StateFlow<Int>
    val redGems: StateFlow<Int>
    val pendingTransactions: StateFlow<List<PendingCurrencyTransaction>>

    /**
     * Enqueues an addition of gems into the 'PENDING' transaction state,
     * immediately initiates the asynchronous server-side handshake/validation flow,
     * and applies authoritative balance updates upon confirmation.
     *
     * @return The unique transaction ID for tracing.
     */
    fun addGems(
        amount: Int,
        source: CurrencySource = CurrencySource.GAMEPLAY_COLLECT,
        verificationToken: String? = null
    ): String

    /**
     * Suspending version that initiates the handshake and awaits the authoritative server verification outcome.
     */
    suspend fun addGemsAuthoritative(
        amount: Int,
        source: CurrencySource = CurrencySource.GAMEPLAY_COLLECT,
        verificationToken: String? = null
    ): CurrencyTransactionOutcome

    /**
     * Spends gems with server-side validation and local lock.
     */
    fun spendGems(amount: Int): Boolean

    /**
     * Suspending spend with server-side authorization handshake.
     */
    suspend fun spendGemsAuthoritative(
        amount: Int,
        source: CurrencySource = CurrencySource.IN_APP_PURCHASE
    ): CurrencyTransactionOutcome

    fun addBlueGems(amount: Int)
    fun spendBlueGems(amount: Int): Boolean
    fun addRedGems(amount: Int)
    fun spendRedGems(amount: Int): Boolean

    /**
     * Performs a full balance reconciliation handshake with the remote authoritative server
     * (stubbed for Firebase Cloud Functions / Firestore backend).
     */
    suspend fun syncCurrencyWithServer(): Boolean
}
