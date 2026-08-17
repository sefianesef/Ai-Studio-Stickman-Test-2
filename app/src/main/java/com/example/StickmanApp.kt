package com.example

import android.app.Application
import android.util.Log

class StickmanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Global safety net for low-end devices and custom OEM OS (MIUI, ColorOS, etc.)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("StickmanApp", "Caught fatal exception on thread ${thread.name}", throwable)
            try {
                // If it's an audio, vibration, or coroutine cancellation issue, don't crash
                val message = throwable.message.orEmpty()
                val className = throwable.javaClass.name
                if (className.contains("Audio") || 
                    className.contains("Vibrat") || 
                    className.contains("Cancellation") ||
                    message.contains("AudioTrack") ||
                    message.contains("Vibrator")
                ) {
                    Log.w("StickmanApp", "Suppressed non-critical hardware crash: $message")
                    return@setDefaultUncaughtExceptionHandler
                }
            } catch (_: Throwable) {}
            
            // Pass to system handler if unrecoverable
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
