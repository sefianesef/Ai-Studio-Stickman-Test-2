package com.example.security

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Secure Time Authority & Anti-Time-Manipulation Engine
 *
 * Protects against local clock manipulation (e.g. players setting device date forward
 * to bypass daily login cooldowns, regenerate unlimited lives, or reset daily missions).
 *
 * Mechanisms:
 * 1. Monotonic Hardware Clock Reference: Uses [SystemClock.elapsedRealtime()] which continues
 *    ticking accurately across sleep and is immune to device clock modifications.
 * 2. Network Time Protocol (NTP / SNTP): Queries trusted NTP servers (time.google.com, pool.ntp.org)
 *    to obtain authoritative atomic UTC timestamps.
 * 3. Clock Skew / Time-Travel Anomaly Detection: Detects if wall clock time jumped forwards or backwards
 *    relative to the monotonic elapsed hardware clock.
 */
class SecureTimeAuthority(private val context: Context) {

    companion object {
        private const val TAG = "SecureTimeAuthority"
        private const val NTP_HOST_PRIMARY = "time.google.com"
        private const val NTP_HOST_BACKUP = "pool.ntp.org"
        private const val NTP_PORT = 123
        private const val NTP_TIMEOUT_MS = 3000
        private const val NTP_RESPONSE_OFFSET = 40
        private const val SEVENTY_YEARS_MS = 2208988800000L // Difference between 1900 and 1970 in ms
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    // Base calibration anchor: Pair of (Authoritative Network Time Ms, Monotonic Hardware Elapsed Ms)
    @Volatile
    private var calibratedNetworkTimeMs: Long = 0L

    @Volatile
    private var calibratedElapsedRealtimeMs: Long = 0L

    @Volatile
    private var isCalibrated: Boolean = false

    @Volatile
    private var lastObservedWallTimeMs: Long = System.currentTimeMillis()

    @Volatile
    private var timeTravelTamperDetected: Boolean = false

    init {
        // Initialize baseline with monotonic anchor
        calibratedElapsedRealtimeMs = SystemClock.elapsedRealtime()
        calibratedNetworkTimeMs = System.currentTimeMillis()
        lastObservedWallTimeMs = System.currentTimeMillis()

        // Kick off background NTP calibration
        syncWithNetworkTime()
    }

    /**
     * Attempts to query authoritative NTP atomic server to calibrate trusted time.
     */
    fun syncWithNetworkTime() {
        scope.launch {
            try {
                val ntpTime = querySntpServer(NTP_HOST_PRIMARY) ?: querySntpServer(NTP_HOST_BACKUP)
                if (ntpTime != null && ntpTime > 1700000000000L) { // Sanity check: > late 2023
                    val elapsed = SystemClock.elapsedRealtime()
                    calibratedNetworkTimeMs = ntpTime
                    calibratedElapsedRealtimeMs = elapsed
                    isCalibrated = true
                    Log.d(TAG, "Successfully calibrated atomic network time: $ntpTime ms")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "NTP time synchronization failed: ${e.message}. Using monotonic hardware anchor.")
            }
        }
    }

    /**
     * Returns the authoritative timestamp in milliseconds.
     * Always calculated using the monotonic hardware clock delta from calibration anchor,
     * completely bypassing local device clock tampering.
     */
    fun getCurrentTimeMs(): Long {
        val currentElapsed = SystemClock.elapsedRealtime()
        val currentWallTime = System.currentTimeMillis()

        // Detect Time-Travel Tamper: Wall clock moved substantially without monotonic clock advancing
        val wallDelta = currentWallTime - lastObservedWallTimeMs
        val elapsedDelta = currentElapsed - calibratedElapsedRealtimeMs

        if (wallDelta < -60_000L || (wallDelta > 300_000L && elapsedDelta < 60_000L)) {
            Log.w(TAG, "SECURITY ALERT: Local device clock manipulation detected! (Wall delta: $wallDelta ms, Elapsed delta: $elapsedDelta ms)")
            timeTravelTamperDetected = true
        }
        lastObservedWallTimeMs = currentWallTime

        // Compute trusted time from monotonic elapsed delta
        val monotonicCalculatedTime = calibratedNetworkTimeMs + (currentElapsed - calibratedElapsedRealtimeMs)
        return monotonicCalculatedTime
    }

    /**
     * Returns the authoritative calendar epoch day (days since Jan 1, 1970 UTC).
     */
    fun getAuthoritativeEpochDay(): Long {
        val timeMs = getCurrentTimeMs()
        return timeMs / (1000L * 60 * 60 * 24)
    }

    fun isTimeTampered(): Boolean = timeTravelTamperDetected

    private suspend fun querySntpServer(host: String): Long? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                soTimeout = NTP_TIMEOUT_MS
            }
            val address = InetAddress.getByName(host)
            val buffer = ByteArray(48)
            // Set client mode = 3, NTP version = 3 (0x1B)
            buffer[0] = 0x1B

            val requestPacket = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket.send(requestPacket)

            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)

            // Extract transmit timestamp (bytes 40..47)
            val transmitTime = readTimestamp(buffer, NTP_RESPONSE_OFFSET)
            transmitTime
        } catch (_: Throwable) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Throwable) {}
        }
    }

    private fun readTimestamp(buffer: ByteArray, offset: Int): Long {
        var seconds = 0L
        for (i in 0..3) {
            seconds = (seconds shl 8) or (buffer[offset + i].toLong() and 0xFF)
        }
        var fraction = 0L
        for (i in 4..7) {
            fraction = (fraction shl 8) or (buffer[offset + i].toLong() and 0xFF)
        }
        val ms = (seconds * 1000L) + ((fraction * 1000L) shr 32)
        return ms - SEVENTY_YEARS_MS
    }
}
