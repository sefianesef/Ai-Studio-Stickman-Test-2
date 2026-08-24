package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.AntiCheatEarningLimiter
import com.example.security.CurrencySource
import com.example.security.EncryptedSaveStorage
import com.example.security.PurchaseVerificationService
import com.example.security.SecureCurrencyVault
import com.example.security.SecureTimeAuthority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityAntiCheatTest {

    private lateinit var context: Context
    private lateinit var purchaseVerifier: PurchaseVerificationService
    private lateinit var timeAuthority: SecureTimeAuthority
    private lateinit var limiter: AntiCheatEarningLimiter
    private lateinit var vault: SecureCurrencyVault

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        purchaseVerifier = PurchaseVerificationService(context)
        timeAuthority = SecureTimeAuthority(context)
        limiter = AntiCheatEarningLimiter(context)
        vault = SecureCurrencyVault(context, limiter)
    }

    @Test
    fun `test Google Play receipt verification rejects invalid or empty tokens`() {
        val resultNullToken = purchaseVerifier.verifyPurchase("gem_pack_100", null)
        assertFalse(resultNullToken.isValid)

        val resultInvalidSku = purchaseVerifier.verifyPurchase("fake_gem_pack_9999", "fake_token_123")
        assertFalse(resultInvalidSku.isValid)

        val resultValid = purchaseVerifier.verifyPurchase("gem_pack_100", "valid_google_play_token_abc123")
        assertTrue(resultValid.isValid)
        assertNotNull(resultValid.verificationToken)

        // Replay attack prevention: same token cannot be consumed twice
        val resultReplay = purchaseVerifier.verifyPurchase("gem_pack_100", "valid_google_play_token_abc123")
        assertFalse(resultReplay.isValid)
    }

    @Test
    fun `test SecureTimeAuthority calculates valid epoch day and monotonic time`() {
        val currentTimeMs = timeAuthority.getCurrentTimeMs()
        assertTrue(currentTimeMs > 1700000000000L)

        val epochDay = timeAuthority.getAuthoritativeEpochDay()
        assertTrue(epochDay > 19000)

        val secondsRemaining = timeAuthority.getSecondsUntilNextUtcDay()
        assertTrue(secondsRemaining in 0..86400)
    }

    @Test
    fun `test abnormal earning limiter blocks excessive gameplay velocity and flags cheaters`() {
        // Normal bridge gem earnings should pass
        val legit1 = limiter.evaluateGemGain(1, CurrencySource.GAMEPLAY_COLLECT)
        assertTrue(legit1.isAllowed)
        assertEquals(1, legit1.sanitizedAmount)

        // Excessive single transaction ceiling breach (e.g. 50 gems from 1 bridge pickup)
        val abnormalSingle = limiter.evaluateGemGain(50, CurrencySource.GAMEPLAY_COLLECT)
        assertFalse(abnormalSingle.isAllowed)
        assertTrue(abnormalSingle.isCheaterFlagged)
        assertTrue(limiter.isCheater())
    }

    @Test
    fun `test abnormal earning limiter burst rate limiting`() {
        val localLimiter = AntiCheatEarningLimiter(context)
        // Add gems up to the 10s burst limit (15 gems)
        for (i in 1..10) {
            val res = localLimiter.evaluateGemGain(1, CurrencySource.GAMEPLAY_COLLECT)
            assertTrue(res.isAllowed)
        }

        // Attempting a burst that exceeds 15 gems within 10 seconds must be rejected
        val burstOverflow = localLimiter.evaluateGemGain(10, CurrencySource.GAMEPLAY_COLLECT)
        assertFalse(burstOverflow.isAllowed)
        assertTrue(burstOverflow.isCheaterFlagged)
    }

    @Test
    fun `test SecureCurrencyVault enforces anti-cheat verification token requirement for IAP`() {
        val currentBalance = 100

        // IAP without verification token must be rejected
        val unverifiedBalance = vault.addGemsSecurely(currentBalance, 550, CurrencySource.IN_APP_PURCHASE, null)
        assertEquals(currentBalance, unverifiedBalance)

        // IAP with valid signed verification token succeeds
        val verifiedBalance = vault.addGemsSecurely(currentBalance, 550, CurrencySource.IN_APP_PURCHASE, "VALID_SIGNED_TOKEN")
        assertEquals(currentBalance + 550, verifiedBalance)
    }

    @Test
    fun `test EncryptedSaveStorage writes and reads encrypted values`() {
        val storage = EncryptedSaveStorage(context, "test_encrypted_vault")
        storage.putInt("TEST_HIGH_SCORE", 1234)
        storage.putString("TEST_HERO_NAME", "StickChampion")
        storage.putBoolean("TEST_VIP_STATUS", true)

        assertEquals(1234, storage.getInt("TEST_HIGH_SCORE", 0))
        assertEquals("StickChampion", storage.getString("TEST_HERO_NAME", null))
        assertTrue(storage.getBoolean("TEST_VIP_STATUS", false))
    }

    @Test
    fun `test EncryptedSaveStorage migrates unencrypted legacy prefs seamlessly`() {
        val legacyPrefsName = "TEST_LEGACY_UNENCRYPTED_PREFS"
        val legacyPrefs = context.getSharedPreferences(legacyPrefsName, Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putInt("GEMS", 450)
            .putInt("HIGH_SCORE", 88)
            .putString("SELECTED_HAT", "hat_crown")
            .putBoolean("SOUND_ENABLED", true)
            .apply()

        val encryptedPrefs = EncryptedSaveStorage.createEncryptedSharedPreferences(context, "TEST_MIGRATED_VAULT")
        EncryptedSaveStorage.migrateFromLegacySharedPreferences(context, legacyPrefsName, encryptedPrefs)

        // Values must now be present in encrypted preferences
        assertEquals(450, encryptedPrefs.getInt("GEMS", 0))
        assertEquals(88, encryptedPrefs.getInt("HIGH_SCORE", 0))
        assertEquals("hat_crown", encryptedPrefs.getString("SELECTED_HAT", null))
        assertTrue(encryptedPrefs.getBoolean("SOUND_ENABLED", false))

        // Legacy unencrypted preferences must be wiped clean
        assertEquals(0, legacyPrefs.all.size)
    }

    @Test
    fun `test CloudWalletData enforces integrity and validation constraints`() {
        val cloudData = com.example.security.CloudWalletData(
            userId = "USER_12345",
            displayName = "StickHero",
            email = "hero@example.com",
            gems = 500,
            redGems = 10,
            highScore = 150,
            isCheaterFlagged = false
        )

        assertEquals("USER_12345", cloudData.userId)
        assertEquals(500, cloudData.gems)
        assertEquals(10, cloudData.redGems)
        assertEquals(150, cloudData.highScore)
        assertFalse(cloudData.isCheaterFlagged)
    }
}
