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
    // Levels 1 to 2 (Scores 0-7): Pure Learning & Onboarding
    NOVICE_TRAINING(
        tierLevel = 1,
        title = "RECRUIT",
        minLevel = 1,
        minScore = 0,
        minWidth = 135f,
        maxWidth = 175f,
        bullseyeTolerance = 24f,
        gemSpawnRate = 0.45f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.86f,
        badgeColorHex = 0xFF10B981
    ),
    // Level 3 (Scores 8-11): Slight Challenge Intro
    APPRENTICE(
        tierLevel = 2,
        title = "CHALLENGER",
        minLevel = 3,
        minScore = 8,
        minWidth = 100f,
        maxWidth = 140f,
        bullseyeTolerance = 18f,
        gemSpawnRate = 0.38f,
        doubleGemChance = 0.12f,
        movingPlatformChance = 0.08f,
        growthSpeedFactor = 0.94f,
        badgeColorHex = 0xFF38BDF8
    ),
    // Levels 4 to 5 (Scores 12-21): Engaging Balanced Challenge
    ADEPT(
        tierLevel = 3,
        title = "ADEPT",
        minLevel = 4,
        minScore = 12,
        minWidth = 80f,
        maxWidth = 115f,
        bullseyeTolerance = 14f,
        gemSpawnRate = 0.32f,
        doubleGemChance = 0.18f,
        movingPlatformChance = 0.15f,
        growthSpeedFactor = 1.00f,
        badgeColorHex = 0xFF8B5CF6
    ),
    // Level 6+ (Scores 22+): Master Stage
    MASTER(
        tierLevel = 4,
        title = "MASTER",
        minLevel = 6,
        minScore = 22,
        minWidth = 55f,
        maxWidth = 85f,
        bullseyeTolerance = 10f,
        gemSpawnRate = 0.25f,
        doubleGemChance = 0.22f,
        movingPlatformChance = 0.22f,
        growthSpeedFactor = 1.10f,
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
            level <= 2 -> {
                // Levels 1-2: Easy steady gap
                val minGap = 120f
                val maxGap = 160f.coerceAtMost(maxAvailableGap)
                Random.nextFloat() * (maxGap - minGap) + minGap
            }
            level == 3 -> {
                // Level 3: Mild gap variation
                val minGap = 135f
                val maxGap = 190f.coerceAtMost(maxAvailableGap)
                Random.nextFloat() * (maxGap - minGap) + minGap
            }
            level in 4..5 -> {
                // Level 4 & 5 (Boss Stage): Good canyon jumps
                val minGap = 145f
                val maxGap = 225f.coerceAtMost(maxAvailableGap)
                Random.nextFloat() * (maxGap - minGap) + minGap
            }
            else -> {
                // Level 6+: Dynamic scaling
                val minGap = 160f
                val maxGap = (235f + (level * 5f)).coerceIn(250f, maxAvailableGap)
                Random.nextFloat() * (maxGap - minGap) + minGap
            }
        }
    }

    fun generatePlatformHeightOffset(score: Int, level: Int): Float {
        // Level 1 to 3: Flat ground for smooth learning
        if (level <= 3) return 0f

        // Level 4 onwards: Mild height steps up/down (+/- 18px to 28px)
        if (Random.nextFloat() < 0.38f) {
            val direction = if (Random.nextBoolean()) -1f else 1f
            val variance = if (level in 4..5) 20f else 32f
            return direction * (Random.nextFloat() * variance + 10f)
        }
        return 0f
    }

    fun generateObstacle(spanStart: Float, spanEnd: Float, score: Int, level: Int, isBossLevel: Boolean): com.mygames.stickmanrush.model.ObstacleData? {
        if (level <= 3 && !isBossLevel) return null
        val spanWidth = spanEnd - spanStart
        if (spanWidth < 140f) return null

        val spawnChance = when {
            isBossLevel -> 0.12f
            level in 4..5 -> 0.25f
            else -> 0.38f
        }
        if (Random.nextFloat() > spawnChance) return null

        val posX = spanStart + (spanWidth * (Random.nextFloat() * 0.4f + 0.3f))
        val chosenType = com.mygames.stickmanrush.model.ObstacleType.FIRE_BALL

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

        val powerUpChance = if (level % 5 == 0 || level <= 3) 0.45f else 0.28f
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
        if (level <= 3) {
            return MovingPlatformConfig(isMoving = false, amplitude = 0f, speed = 0f, isVertical = false)
        }
        val movingChance = if (level in 4..5) 0.15f else 0.25f
        val isMoving = Random.nextFloat() < movingChance
        return MovingPlatformConfig(
            isMoving = isMoving,
            amplitude = if (isMoving) 18f else 0f,
            speed = if (isMoving) 1.35f else 0f,
            isVertical = false
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
