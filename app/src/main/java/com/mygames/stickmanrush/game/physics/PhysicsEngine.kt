package com.mygames.stickmanrush.game.physics

import androidx.compose.ui.graphics.Color
import com.mygames.stickmanrush.model.BossProjectile
import com.mygames.stickmanrush.model.GemData
import com.mygames.stickmanrush.model.NearMissInfo
import com.mygames.stickmanrush.model.ObstacleData
import com.mygames.stickmanrush.model.Particle
import com.mygames.stickmanrush.model.ParticleShape
import com.mygames.stickmanrush.model.PlatformData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * High-performance 2D physics and collision engine for Stickman Hero.
 * Handles rigid body kinematics, gravitational torque, bridge elasticity & spring oscillation,
 * walking sag deflection, dynamic collision detection, variable platform elevation slopes,
 * moving obstacles, boss projectiles, and particle aerodynamics.
 */
class PhysicsEngine {

    companion object {
        const val GRAVITY = 1800f // px / s^2 for falling stickman
        const val BRIDGE_BASE_TORQUE = 360f // initial rotation torque
        const val BRIDGE_GRAVITY_TORQUE = 1450f // gravitational rotational acceleration factor
        const val MAX_BRIDGE_ANGLE = 90f // horizontal landing angle
        const val FAIL_DROP_ANGLE = 180f // vertical plunge angle
        const val WALK_SPEED = 240f // px / s horizontal walk speed
        const val SCROLL_SPEED = 580f // px / s camera pan speed
        const val BULLSEYE_TOLERANCE = 12f // px radius for center red-dot perfect hit
        const val GEM_COLLECT_RADIUS = 28f // px proximity for gem pickup
        const val OBSTACLE_COLLISION_RADIUS = 24f // px proximity for obstacle hit
        const val PROJECTILE_COLLISION_RADIUS = 26f // px proximity for boss projectile hit
        
        // Elasticity & Spring Harmonic constants
        const val ELASTICITY_DAMPING = 8.5f // zeta * omega
        const val ELASTICITY_FREQUENCY = 24f // omega in rad/s
    }

    /**
     * Calculates angular acceleration for a falling bridge based on rotational torque.
     * Torque increases with sin(angle) as the center of mass moves away from the pivot:
     * tau = m * g * (L / 2) * sin(theta)
     */
    fun computeBridgeAngularAcceleration(currentAngleDegrees: Float): Float {
        val angleRad = (currentAngleDegrees * PI / 180.0).toFloat()
        val sineFactor = sin(angleRad.toDouble()).toFloat().coerceAtLeast(0.1f)
        return BRIDGE_BASE_TORQUE + BRIDGE_GRAVITY_TORQUE * sineFactor
    }

    /**
     * Computes spring harmonic oscillation (wobble) upon bridge impact.
     * Produces a physical damped bounce when the bridge strikes the platform surface.
     * angleOffset = Amplitude * e^(-damping * t) * cos(omega * t)
     */
    fun computeLandingBounceAngle(elapsedTimeSinceImpact: Float, bridgeLength: Float): Float {
        if (elapsedTimeSinceImpact < 0f || elapsedTimeSinceImpact > 0.6f) return 0f
        // Longer bridges have greater inertia and slightly higher initial amplitude
        val initialAmplitude = (bridgeLength * 0.035f).coerceIn(3.5f, 9.0f)
        val decay = exp(-ELASTICITY_DAMPING * elapsedTimeSinceImpact)
        val oscillation = cos(ELASTICITY_FREQUENCY * elapsedTimeSinceImpact)
        return initialAmplitude * decay * oscillation
    }

    /**
     * Computes vertical deflection (sag) as the stickman's weight walks along the bridge span.
     * Parabolic deflection: sag(x) = 4 * maxSag * (x / L) * (1 - x / L)
     */
    fun computeBridgeSag(stickmanProgress: Float, bridgeLength: Float): Float {
        val normalizedX = stickmanProgress.coerceIn(0f, 1f)
        val maxSag = (bridgeLength * 0.03f).coerceIn(2f, 8f)
        return 4f * maxSag * normalizedX * (1f - normalizedX)
    }

    /**
     * Evaluates collision and landing results for the dropped bridge, including variable platform elevations.
     */
    fun evaluateBridgeLanding(
        bridgeStartX: Float,
        currentHeightOffset: Float = 0f,
        stickLength: Float,
        targetPlatform: PlatformData,
        bullseyeTolerance: Float = BULLSEYE_TOLERANCE
    ): LandingResult {
        val platformStart = targetPlatform.leftX
        val platformEnd = targetPlatform.leftX + targetPlatform.width
        val platformCenter = targetPlatform.leftX + (targetPlatform.width / 2f)

        // Height difference between target platform top surface and current platform hinge
        val deltaY = targetPlatform.heightOffset - currentHeightOffset

        // If stickLength is shorter than the vertical difference, it cannot reach the target height
        val isPhysicallyLongEnough = stickLength >= abs(deltaY)

        val (bridgeTipX, landingSlopeAngle) = if (isPhysicallyLongEnough && deltaY != 0f) {
            val dx = sqrt((stickLength * stickLength - deltaY * deltaY).coerceAtLeast(0f))
            val tipX = bridgeStartX + dx
            val slopeDeg = atan2(deltaY, dx) * (180f / PI.toFloat())
            Pair(tipX, slopeDeg)
        } else {
            Pair(bridgeStartX + stickLength, 0f)
        }

        val isBullseye = abs(bridgeTipX - platformCenter) <= bullseyeTolerance
        val isSuccessful = isPhysicallyLongEnough && bridgeTipX in platformStart..platformEnd

        // Landing slope angle applies when bridge successfully connects with target platform or rests across it
        val finalLandingSlopeAngle = if (isSuccessful) {
            landingSlopeAngle
        } else if (bridgeTipX > platformEnd && isPhysicallyLongEnough) {
            landingSlopeAngle // resting across platform but overshooting
        } else {
            0f
        }

        // Psychological Near-Miss calculation (dopamine trigger)
        val nearMiss = when {
            isSuccessful -> null
            bridgeTipX < platformStart && (platformStart - bridgeTipX) <= 38f -> {
                val diff = platformStart - bridgeTipX
                NearMissInfo(
                    isNearMiss = true,
                    pixelsDifference = diff,
                    isUnderShoot = true,
                    message = "SO CLOSE! Missed by only ${diff.toInt().coerceAtLeast(1)}px!"
                )
            }
            bridgeTipX > platformEnd && (bridgeTipX - platformEnd) <= 38f -> {
                val diff = bridgeTipX - platformEnd
                NearMissInfo(
                    isNearMiss = true,
                    pixelsDifference = diff,
                    isUnderShoot = false,
                    message = "SO CLOSE! Overshot by ${diff.toInt().coerceAtLeast(1)}px!"
                )
            }
            else -> null
        }

        val targetWalk = if (isSuccessful) {
            (platformStart + (targetPlatform.width * 0.5f)).coerceAtLeast(platformStart + 20f)
        } else {
            bridgeTipX
        }

        return LandingResult(
            isSuccessful = isSuccessful,
            isBullseye = isBullseye,
            bridgeTipX = bridgeTipX,
            targetWalkX = targetWalk,
            platformCenter = platformCenter,
            landingSlopeAngle = finalLandingSlopeAngle,
            nearMiss = nearMiss
        )
    }

    /**
     * Checks if the stickman collides with a fatal obstacle hazard along the bridge span.
     * Returns true if hit (damage/fail condition), false if safely navigated/dodged.
     * If the stickman is jumping above clearance height (jumpOffsetY >= 22f), stickman safely leaps over top hazards (buzzsaws, fireballs, lasers).
     */
    fun checkObstacleCollision(
        stickmanX: Float,
        isUpsideDown: Boolean,
        obstacle: ObstacleData?,
        jumpOffsetY: Float = 0f
    ): Boolean {
        if (obstacle == null || !obstacle.isActive) return false
        if (obstacle.type == com.mygames.stickmanrush.model.ObstacleType.SLIP_PATCH) return false // Handled as slip physics

        val inRange = abs(stickmanX - obstacle.x) <= OBSTACLE_COLLISION_RADIUS
        if (!inRange) return false

        return if (obstacle.isUnderBridge) {
            // Obstacle is UNDER bridge (e.g. spike mine) -> Stickman hits it if UPSIDE-DOWN
            isUpsideDown
        } else {
            // Obstacle is ON TOP of bridge (e.g. spinning buzzsaw, fireball, laser barrier)
            // If stickman is airborne / jumping with clearance, safely clears the hazard!
            if (jumpOffsetY >= 22f) {
                false
            } else {
                !isUpsideDown
            }
        }
    }

    /**
     * Checks if the stickman steps on an ice/oil slip patch on top of the bridge.
     */
    fun checkObstacleSlip(
        stickmanX: Float,
        isUpsideDown: Boolean,
        obstacle: ObstacleData?,
        jumpOffsetY: Float = 0f
    ): Boolean {
        if (obstacle == null || !obstacle.isActive || obstacle.type != com.mygames.stickmanrush.model.ObstacleType.SLIP_PATCH) return false
        val inRange = abs(stickmanX - obstacle.x) <= 32f
        return inRange && !isUpsideDown && jumpOffsetY < 10f
    }

    /**
     * Checks if the stickman is struck by an incoming boss projectile.
     * Low/ground projectiles or fireballs can be safely jumped over (jumpOffsetY >= 22f), while high projectiles can be dodged by flipping underneath!
     */
    fun checkBossProjectileCollision(
        stickmanX: Float,
        isUpsideDown: Boolean,
        projectile: BossProjectile,
        jumpOffsetY: Float = 0f
    ): Boolean {
        if (projectile.hasHitPlayer || projectile.isDodged) return false
        val inRange = abs(stickmanX - projectile.x) <= PROJECTILE_COLLISION_RADIUS
        if (!inRange) return false

        return if (projectile.isHigh) {
            // High projectile (dragon flame, warlock beam): Player hits it if standing on top; flips under to dodge
            !isUpsideDown
        } else {
            // Low projectile (rolling boulder, ground fireball/laser):
            // Player leaps over it with a jump, or stays right-side up if projectile is underneath!
            if (jumpOffsetY >= 22f) {
                false
            } else {
                !isUpsideDown
            }
        }
    }

    /**
     * Checks if the stickman is within pickup range of a gem on the platform.
     */
    fun checkGemPickup(
        stickmanX: Float,
        isUpsideDown: Boolean,
        gem: GemData?
    ): Boolean {
        if (gem == null || gem.collected) return false
        val inRange = abs(stickmanX - gem.x) <= GEM_COLLECT_RADIUS
        val correctSide = (gem.isUnderBridge && isUpsideDown) || (!gem.isUnderBridge && !isUpsideDown)
        return inRange && correctSide
    }

    /**
     * Checks if the stickman is within pickup range of a PowerUpItem.
     */
    fun checkPowerUpPickup(
        stickmanX: Float,
        isUpsideDown: Boolean,
        powerUp: com.mygames.stickmanrush.model.PowerUpItem?
    ): Boolean {
        if (powerUp == null || powerUp.collected) return false
        val inRange = abs(stickmanX - powerUp.x) <= (GEM_COLLECT_RADIUS + 4f)
        val correctSide = (powerUp.isUnderBridge && isUpsideDown) || (!powerUp.isUnderBridge && !isUpsideDown)
        return inRange && correctSide
    }

    /**
     * Checks if the inverted stickman crashes into the side cliff wall of the destination platform.
     * Uses a tight, forgiving safety buffer.
     */
    fun checkPlatformWallCollision(
        stickmanX: Float,
        isUpsideDown: Boolean,
        platformLeftX: Float
    ): Boolean {
        return isUpsideDown && stickmanX >= (platformLeftX - 4f)
    }

    /**
     * Simulates free-fall gravity and rotational tumbling for the stickman.
     */
    fun updateStickmanFall(
        currentY: Float,
        currentVelY: Float,
        currentRot: Float,
        dt: Float
    ): StickmanFallState {
        val newVelY = currentVelY + GRAVITY * dt
        val newY = currentY + newVelY * dt
        val newRot = currentRot + 420f * dt
        return StickmanFallState(y = newY, velocityY = newVelY, rotation = newRot)
    }

    /**
     * Updates 2D particle dynamics: velocity integration, air friction, flutter harmonics, rotational tumbling, and alpha decay.
     */
    fun updateParticles(particles: MutableList<Particle>, dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.vRot * dt

            when (p.shape) {
                ParticleShape.RING_WAVE -> {
                    // Expanding shockwave ring
                    p.vx *= (1f - 2.2f * dt).coerceAtLeast(0f)
                    p.vy *= (1f - 2.2f * dt).coerceAtLeast(0f)
                }
                ParticleShape.GEM_BURST, ParticleShape.STAR -> {
                    p.vx *= (1f - 1.5f * dt).coerceAtLeast(0f)
                    p.vy += 120f * dt // gentle floating gravity
                }
                ParticleShape.CONFETTI -> {
                    p.vx *= (1f - 0.7f * dt).coerceAtLeast(0f)
                    p.vy += 160f * dt // fluttering gravity
                }
                ParticleShape.FIRE_EMBER -> {
                    // Upward floating ember with gentle horizontal sway
                    p.vy -= 180f * dt
                    p.vx *= (1f - 1.2f * dt).coerceAtLeast(0f)
                }
                ParticleShape.SPARKLE -> {
                    p.vx *= (1f - 2.5f * dt).coerceAtLeast(0f)
                    p.vy += 200f * dt
                }
                ParticleShape.NEON_ORB -> {
                    p.vx *= (1f - 1.0f * dt).coerceAtLeast(0f)
                    p.vy *= (1f - 1.0f * dt).coerceAtLeast(0f)
                }
                ParticleShape.GLOW_TRAIL -> {
                    // Smooth luminous deceleration with minimal gravity
                    p.vx *= (1f - 3.2f * dt).coerceAtLeast(0f)
                    p.vy *= (1f - 2.8f * dt).coerceAtLeast(0f)
                }
                ParticleShape.DUST -> {
                    p.vx *= (1f - 3.0f * dt).coerceAtLeast(0f)
                    p.vy *= (1f - 2.0f * dt).coerceAtLeast(0f)
                }
                else -> {
                    p.vy += 340f * dt // standard gravity
                }
            }

            p.life -= dt
            p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            if (p.life <= 0f) {
                iterator.remove()
            }
        }
    }
}

data class LandingResult(
    val isSuccessful: Boolean,
    val isBullseye: Boolean,
    val bridgeTipX: Float,
    val targetWalkX: Float,
    val platformCenter: Float,
    val landingSlopeAngle: Float = 0f,
    val nearMiss: NearMissInfo? = null
)

data class StickmanFallState(
    val y: Float,
    val velocityY: Float,
    val rotation: Float
)

