package com.mygames.stickmanrush.game

import kotlin.random.Random

enum class DifficultyTier(
    val tierLevel: Int,
    val title: String,
    val minLevel: Int,
    val minScore: Int,
    val minWidth: Float,
    val maxWidth: Float,
    val bullseyeTolerance: Float,
    val gemSpawnRate: Float,
    val doubleGemChance: Float,
    val movingPlatformChance: Float,
    val growthSpeedFactor: Float,
    val badgeColorHex: Long
) {
    // Levels 1 to 5 (Scores 0-21): Comfortable, Easy & Forgiving onboarding
    NOVICE_TRAINING(
        tierLevel = 1,
        title = "RECRUIT",
        minLevel = 1,
        minScore = 0,
        minWidth = 125f,
        maxWidth = 165f,
        bullseyeTolerance = 22f,
        gemSpawnRate = 0.45f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.88f,
        badgeColorHex = 0xFF10B981
    ),
    // Levels 6 to 8 (Scores 22-42): Moderate Step-up in challenge
    APPRENTICE(
        tierLevel = 2,
        title = "CHALLENGER",
        minLevel = 6,
        minScore = 22,
        minWidth = 85f,
        maxWidth = 120f,
        bullseyeTolerance = 16f,
        gemSpawnRate = 0.35f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.12f,
        growthSpeedFactor = 0.98f,
        badgeColorHex = 0xFF38BDF8
    ),
    // Levels 9 to 10 (Scores 43-61): Genuine Precision Challenge
    ADEPT(
        tierLevel = 3,
        title = "ADEPT",
        minLevel = 9,
        minScore = 43,
        minWidth = 65f,
        maxWidth = 95f,
        bullseyeTolerance = 12f,
        gemSpawnRate = 0.28f,
        doubleGemChance = 0.20f,
        movingPlatformChance = 0.20f,
        growthSpeedFactor = 1.05f,
        badgeColorHex = 0xFF8B5CF6
    ),
    // Level 11+ (Scores 62+): Master Tier
    MASTER(
        tierLevel = 4,
        title = "MASTER",
        minLevel = 11,
        minScore = 62,
        minWidth = 45f,
        maxWidth = 70f,
        bullseyeTolerance = 8f,
        gemSpawnRate = 0.22f,
        doubleGemChance = 0.25f,
        movingPlatformChance = 0.28f,
        growthSpeedFactor = 1.15f,
        badgeColorHex = 0xFFEF4444
    );

    companion object {
        fun getTierForScore(score: Int): DifficultyTier {
            return when {
                score >= MASTER.minScore -> MASTER
                score >= ADEPT.minScore -> ADEPT
                score >= APPRENTICE.minScore -> APPRENTICE
                else -> NOVICE_TRAINING
            }
        }
    }
}

class DifficultyManager {
    fun getTier(score: Int): DifficultyTier = DifficultyTier.getTierForScore(score)

    fun generatePlatformWidth(score: Int, streak: Int = 0): Float {
        val tier = getTier(score)
        val baseWidth = Random.nextFloat() * (tier.maxWidth - tier.minWidth) + tier.minWidth
        return baseWidth.coerceAtLeast(35f)
    }

    fun generatePlatformGap(score: Int, level: Int, screenWidth: Float, isFirstBridgeOfLevel: Boolean = false): Float {
        val maxAvailableGap = (screenWidth * 0.52f).coerceAtLeast(280f)
        return when {
            level <= 5 -> {
                // Levels 1-5: Steady & predictable gaps
                val minGap = 120f
                val maxGap = 165f.coerceAtMost(maxAvailableGap)
                Random.nextFloat() * (maxGap - minGap) + minGap
            }
            level in 6..8 -> {
                // Levels 6-8: Moderate variation
                val minGap = 140f
                val maxGap = 210f.coerceAtMost(maxAvailableGap)
                Random.nextFloat() * (maxGap - minGap) + minGap
            }
            else -> {
                // Levels 9-10+: Thrilling canyon crossings
                val minGap = 160f
                val maxGap = (230f + (level * 6f)).coerceIn(250f, maxAvailableGap)
                Random.nextFloat() * (maxGap - minGap) + minGap
            }
        }
    }

    fun generatePlatformHeightOffset(score: Int, level: Int): Float {
        if (level <= 5) return 0f // Level 1-5 flat ground
        // Level 6 onwards dynamic steps & cliffs
        if (Random.nextFloat() < 0.40f) {
            val direction = if (Random.nextBoolean()) -1f else 1f
            val variance = (15f + (level * 2.5f)).coerceAtMost(38f)
            return direction * (Random.nextFloat() * variance + 10f)
        }
        return 0f
    }

    fun generateObstacle(spanStart: Float, spanEnd: Float, score: Int, level: Int, isBossLevel: Boolean): com.mygames.stickmanrush.model.ObstacleData? {
        if (level <= 5 && !isBossLevel) return null
        val spanWidth = spanEnd - spanStart
        if (spanWidth < 140f) return null

        val spawnChance = when {
            isBossLevel -> 0.15f
            level in 6..8 -> 0.28f
            else -> 0.42f
        }
        if (Random.nextFloat() > spawnChance) return null

        val posX = spanStart + (spanWidth * (Random.nextFloat() * 0.4f + 0.3f))
        val chosenType = if (level >= 9 && Random.nextBoolean()) {
            com.mygames.stickmanrush.model.ObstacleType.MOVING_SPIKE_BALL
        } else {
            com.mygames.stickmanrush.model.ObstacleType.FIRE_BALL
        }

        return com.mygames.stickmanrush.model.ObstacleData(
            id = System.currentTimeMillis() + Random.nextInt(1000),
            x = posX,
            y = 0f,
            type = chosenType,
            isUnderBridge = false
        )
    }

    fun generatePowerUp(
        spanStart: Float,
        spanEnd: Float,
        score: Int,
        level: Int,
        hasObstacle: Boolean
    ): com.mygames.stickmanrush.model.PowerUpItem? {
        val spanWidth = spanEnd - spanStart
        if (spanWidth < 80f) return null

        val powerUpChance = when {
            level % 5 == 0 -> 0.50f // Boss level par support pickups
            level <= 5 -> 0.40f
            else -> 0.30f
        }
        if (Random.nextFloat() > powerUpChance) return null

        val minX = spanStart + (spanWidth * 0.25f)
        val maxX = spanStart + (spanWidth * 0.75f)
        val posX = minX + Random.nextFloat() * (maxX - minX)

        val chosenType = when (Random.nextFloat()) {
            in 0.0f..0.45f -> com.mygames.stickmanrush.model.PowerUpType.INVINCIBILITY_SHIELD
            in 0.45f..0.78f -> com.mygames.stickmanrush.model.PowerUpType.MAGNET
            else -> com.mygames.stickmanrush.model.PowerUpType.GEM_DOUBLER
        }

        return com.mygames.stickmanrush.model.PowerUpItem(
            id = System.nanoTime() + Random.nextInt(500),
            x = posX,
            y = 0f,
            type = chosenType,
            isUnderBridge = false,
            collected = false,
            floatOffset = 0f
        )
    }

    fun generateMovingConfig(score: Int, level: Int, streak: Int = 0): MovingPlatformConfig {
        if (level <= 5) {
            return MovingPlatformConfig(isMoving = false, amplitude = 0f, speed = 0f, isVertical = false)
        }
        val movingChance = if (level >= 9) 0.30f else 0.18f
        val isMoving = Random.nextFloat() < movingChance
        return MovingPlatformConfig(
            isMoving = isMoving,
            amplitude = if (isMoving) (14f + (level * 1.5f)).coerceAtMost(28f) else 0f,
            speed = if (isMoving) (1.2f + (level * 0.08f)).coerceAtMost(2.0f) else 0f,
            isVertical = Random.nextFloat() < 0.25f
        )
    }

    fun getWindDriftForce(streak: Int): Float = 0f

    data class MovingPlatformConfig(
        val isMoving: Boolean,
        val amplitude: Float,
        val speed: Float,
        val isVertical: Boolean
    )
}
