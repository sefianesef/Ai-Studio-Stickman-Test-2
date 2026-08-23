package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Anti-Tamper & Device Integrity Monitor
 * Detects rooted environments, attached debuggers (JDWP), hooking frameworks (Frida/Xposed),
 * memory injectors (GameGuardian), and debug builds.
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

    /**
     * Checks if a debugger is currently attached to the process.
     */
    fun isDebuggerAttached(): Boolean {
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            Log.w(TAG, "Debugger attachment detected via Android Debug API!")
            return true
        }
        return isTracerPidNonZero()
    }

    /**
     * Reads /proc/self/status to check if TracerPid is non-zero (indicating ptraced by GDB / LLDB / Frida).
     */
    private fun isTracerPidNonZero(): Boolean {
        return try {
            val file = File("/proc/self/status")
            if (file.exists()) {
                val reader = BufferedReader(FileReader(file))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.startsWith("TracerPid:") == true) {
                        val pid = line?.substringAfter("TracerPid:")?.trim()?.toIntOrNull() ?: 0
                        if (pid > 0) {
                            Log.w(TAG, "Process is being traced! TracerPid = $pid")
                            reader.close()
                            return true
                        }
                    }
                }
                reader.close()
            }
            false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks whether the application is running with debuggable flag active.
     */
    fun isAppDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

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
        val suspiciousPacks = listOf(
            "de.robv.android.xposed",
            "com.topjohnwu.magisk",
            "com.chelpus.luckypatcher",
            "catch_.me_.if_.you_.can_"
        )
        for (pkg in suspiciousPacks) {
            if (File("/data/data/$pkg").exists()) {
                Log.w(TAG, "Suspicious framework directory found: $pkg")
                return true
            }
        }
        return false
    }
}

