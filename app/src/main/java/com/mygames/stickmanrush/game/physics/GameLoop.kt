package com.mygames.stickmanrush.game.physics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-precision Game Loop controller supporting fixed-timestep simulation
 * and variable delta clamping. Prevents tunneling and frame stutter across
 * 60Hz, 90Hz, and 120Hz displays.
 */
class GameLoop(
    private val onTick: (Float) -> Unit,
    val targetFps: Int = 60
) {
    private var isRunning = false
    private var lastTimeNanos: Long = 0L
    private val fixedDeltaSeconds = 1.0f / targetFps.toFloat()
    private var accumulator = 0f
    private val maxFrameTime = 0.1f // Max clamp to prevent spiral of death

    /**
     * Executes a single frame cycle from Compose or external render clock.
     */
    fun onFrame(frameTimeNanos: Long) {
        if (!isRunning) {
            lastTimeNanos = frameTimeNanos
            isRunning = true
            return
        }

        if (lastTimeNanos == 0L) {
            lastTimeNanos = frameTimeNanos
            return
        }

        val elapsedNanos = frameTimeNanos - lastTimeNanos
        lastTimeNanos = frameTimeNanos

        var frameTimeSeconds = (elapsedNanos / 1_000_000_000f).coerceIn(0.0001f, maxFrameTime)
        accumulator += frameTimeSeconds

        // Fixed timestep sub-steps for determinism
        var steps = 0
        while (accumulator >= fixedDeltaSeconds && steps < 4) {
            onTick(fixedDeltaSeconds)
            accumulator -= fixedDeltaSeconds
            steps++
        }

        // Remainder step if sub-stepping finished
        if (steps == 0 && accumulator > 0.001f) {
            onTick(accumulator)
            accumulator = 0f
        }
    }

    fun pause() {
        isRunning = false
        lastTimeNanos = 0L
        accumulator = 0f
    }

    fun resume() {
        lastTimeNanos = 0L
        accumulator = 0f
        isRunning = true
    }

    fun reset() {
        pause()
    }
}
