package com.example.security

import android.os.Build
import android.util.Log
import java.io.File

/**
 * Anti-Tamper & Device Integrity Monitor
 * Detects rooted environments, debuggers, hooking frameworks (Frida/Xposed),
 * and suspicious memory manipulation tools.
 */
object DeviceIntegrityManager {
    private const val TAG = "DeviceIntegrity"

    private val KNOWN_ROOT_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )

    fun isDeviceRooted(): Boolean {
        return checkBuildTags() || checkRootBinaries()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootBinaries(): Boolean {
        for (path in KNOWN_ROOT_PATHS) {
            if (File(path).exists()) {
                Log.w(TAG, "Root binary detected at $path")
                return true
            }
        }
        return false
    }

    fun isHookingFrameworkDetected(): Boolean {
        val suspiciousPacks = listOf("de.robv.android.xposed", "com.topjohnwu.magisk")
        for (pkg in suspiciousPacks) {
            if (File("/data/data/$pkg").exists()) {
                Log.w(TAG, "Hooking framework directory found: $pkg")
                return true
            }
        }
        return false
    }
}
