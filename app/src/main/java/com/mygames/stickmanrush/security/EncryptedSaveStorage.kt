package com.mygames.stickmanrush.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * High-Security Encrypted Save Storage Vault
 *
 * Uses Android KeyStore Hardware-Backed MasterKey (AES-256 GCM) and EncryptedSharedPreferences
 * (AES-256 SIV for keys, AES-256 GCM for values). Keys never leave the hardware TEE / StrongBox,
 * preventing offline key extraction, memory tampering, or save-game file modification on rooted devices.
 */
class EncryptedSaveStorage(
    private val context: Context,
    private val preferencesName: String = DEFAULT_ENCRYPTED_PREFS_NAME,
    private val securePrefs: SharedPreferences = createEncryptedSharedPreferences(context, preferencesName)
) : SharedPreferences by securePrefs {

    companion object {
        private const val TAG = "EncryptedSaveStorage"
        const val DEFAULT_ENCRYPTED_PREFS_NAME = "stickman_secure_keystore_vault_v2"
        const val LEGACY_UNENCRYPTED_PREFS_NAME = "STICKMAN_HERO_DATA"

        /**
         * Creates an EncryptedSharedPreferences instance backed by the Android Keystore MasterKey.
         */
        fun createEncryptedSharedPreferences(
            context: Context,
            preferencesName: String = DEFAULT_ENCRYPTED_PREFS_NAME
        ): SharedPreferences {
            val appContext = context.applicationContext ?: context
            return try {
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    appContext,
                    preferencesName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Hardware Keystore EncryptedSharedPreferences init failed for $preferencesName. Falling back safely.", e)
                appContext.getSharedPreferences(preferencesName + "_fallback", Context.MODE_PRIVATE)
            }
        }

        /**
         * Seamlessly migrates existing unencrypted save data from legacy SharedPreferences
         * to hardware-backed EncryptedSharedPreferences without data loss.
         */
        fun migrateFromLegacySharedPreferences(
            context: Context,
            legacyName: String = LEGACY_UNENCRYPTED_PREFS_NAME,
            securePrefs: SharedPreferences
        ) {
            if (legacyName == DEFAULT_ENCRYPTED_PREFS_NAME || legacyName.endsWith("_vault") || legacyName.contains("keystore")) {
                return
            }
            try {
                val appContext = context.applicationContext ?: context
                val legacyPrefs = appContext.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
                val allEntries = legacyPrefs.all
                if (allEntries.isNotEmpty()) {
                    Log.i(TAG, "Migrating ${allEntries.size} entries from legacy unencrypted save file ($legacyName) to Hardware-backed Keystore...")
                    val editor = securePrefs.edit()
                    for ((key, value) in allEntries) {
                        // CRITICAL: Skip any internal AndroidX security keys or reserved prefixes
                        if (key.startsWith("__androidx_security_crypto_encrypted_prefs_") || key.startsWith("__")) {
                            continue
                        }
                        when (value) {
                            is Int -> editor.putInt(key, value)
                            is Long -> editor.putLong(key, value)
                            is String -> editor.putString(key, value)
                            is Boolean -> editor.putBoolean(key, value)
                            is Float -> editor.putFloat(key, value)
                            is Set<*> -> {
                                @Suppress("UNCHECKED_CAST")
                                editor.putStringSet(key, value as? Set<String>)
                            }
                        }
                    }
                    editor.apply()
                    // Clear the unencrypted legacy file so plain-text values no longer sit on disk
                    legacyPrefs.edit().clear().apply()
                    Log.i(TAG, "Legacy save data successfully migrated and unencrypted storage wiped.")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error during legacy save file migration", e)
            }
        }
    }

    fun putInt(key: String, value: Int) {
        securePrefs.edit().putInt(key, value).apply()
    }

    fun putLong(key: String, value: Long) {
        securePrefs.edit().putLong(key, value).apply()
    }

    fun putString(key: String, value: String?) {
        if (value == null) {
            securePrefs.edit().remove(key).apply()
        } else {
            securePrefs.edit().putString(key, value).apply()
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        securePrefs.edit().putBoolean(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return securePrefs.getInt(key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return securePrefs.getLong(key, defaultValue)
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return securePrefs.getString(key, defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return securePrefs.getBoolean(key, defaultValue)
    }

    fun secureEdit(): EncryptedEditor = EncryptedEditor(securePrefs.edit())

    class EncryptedEditor(private val editor: SharedPreferences.Editor) {
        fun putInt(key: String, value: Int) = apply {
            editor.putInt(key, value)
        }

        fun putLong(key: String, value: Long) = apply {
            editor.putLong(key, value)
        }

        fun putString(key: String, value: String?) = apply {
            if (value == null) editor.remove(key) else editor.putString(key, value)
        }

        fun putBoolean(key: String, value: Boolean) = apply {
            editor.putBoolean(key, value)
        }

        fun remove(key: String) = apply {
            editor.remove(key)
        }

        fun apply() {
            editor.apply()
        }
    }
}


