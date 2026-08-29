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
    NOVICE_TRAINING(
        tierLevel = 1,
        title = "RECRUIT",
        minLevel = 1,
        minScore = 0,
        minWidth = 130f,
        maxWidth = 170f,
        bullseyeTolerance = 24f,
        gemSpawnRate = 0.45f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.85f,
        badgeColorHex = 0xFF10B981
    ),
    APPRENTICE(
        tierLevel = 2,
        title = "CHALLENGER",
        minLevel = 6,
        minScore = 22,
        minWidth = 75f,
        maxWidth = 105f,
        bullseyeTolerance = 14f,
        gemSpawnRate = 0.30f,
        doubleGemChance = 0.12f,
        movingPlatformChance = 0.10f,
        growthSpeedFactor = 1.00f,
        badgeColorHex = 0xFF38BDF8
    ),
    ADEPT(
        tierLevel = 3,
        title = "ADEPT",
        minLevel = 10,
        minScore = 52,
        minWidth = 60f,
        maxWidth = 85f,
        bullseyeTolerance = 11f,
        gemSpawnRate = 0.25f,
        doubleGemChance = 0.18f,
        movingPlatformChance = 0.18f,
        growthSpeedFactor = 1.08f,
        badgeColorHex = 0xFF8B5CF6
    ),
    EXPERT(
        tierLevel = 4,
        title = "EXPERT",
        minLevel = 15,
        minScore = 80,
        minWidth = 52f,
        maxWidth = 72f,
        bullseyeTolerance = 9f,
        gemSpawnRate = 0.22f,
        doubleGemChance = 0.20f,
        movingPlatformChance = 0.22f,
        growthSpeedFactor = 1.12f,
        badgeColorHex = 0xFFF59E0B
    ),
    MASTER(
        tierLevel = 5,
        title = "MASTER",
        minLevel = 20,
        minScore = 110,
        minWidth = 45f,
        maxWidth = 65f,
        bullseyeTolerance = 8f,
        gemSpawnRate = 0.20f,
        doubleGemChance = 0.25f,
        movingPlatformChance = 0.25f,
        growthSpeedFactor = 1.15f,
        badgeColorHex = 0xFFEF4444
    ),
    GRANDMASTER(
        tierLevel = 6,
        title = "GRANDMASTER",
        minLevel = 25,
        minScore = 150,
        minWidth = 35f,
        maxWidth = 55f,
        bullseyeTolerance = 6f,
        gemSpawnRate = 0.18f,
        doubleGemChance = 0.30f,
        movingPlatformChance = 0.30f,
        growthSpeedFactor = 1.20f,
        badgeColorHex = 0xFFEC4899
    );

    companion object {
        fun getTierForScore(score: Int): DifficultyTier {
            return when {
                score >= GRANDMASTER.minScore -> GRANDMASTER
                score >= MASTER.minScore -> MASTER
                score >= EXPERT.minScore -> EXPERT
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
        return Random.nextFloat() * (tier.maxWidth - tier.minWidth) + tier.minWidth
    }

    fun generatePlatformGap(score: Int, level: Int, screenWidth: Float, isFirstBridgeOfLevel: Boolean = false): Float {
        val maxAvailableGap = (screenWidth * 0.50f).coerceAtLeast(280f)
        if (level <= 5) {
            val minGap = 120f
            val maxGap = 160f.coerceAtMost(maxAvailableGap)
            return Random.nextFloat() * (maxGap - minGap) + minGap
        }
        val minGap = 150f
        val maxGap = (220f + (level * 5f)).coerceIn(240f, maxAvailableGap)
        return Random.nextFloat() * (maxGap - minGap) + minGap
    }

    fun generatePlatformHeightOffset(score: Int, level: Int): Float {
        if (level <= 5) return 0f
        if (Random.nextFloat() < 0.35f) {
            val direction = if (Random.nextBoolean()) -1f else 1f
            return direction * (Random.nextFloat() * 25f + 15f)
        }
        return 0f
    }

    fun generateObstacle(spanStart: Float, spanEnd: Float, score: Int, level: Int, isBossLevel: Boolean): com.mygames.stickmanrush.model.ObstacleData? {
        if (level <= 5 && !isBossLevel) return null
        val spanWidth = spanEnd - spanStart
        if (spanWidth < 140f) return null
        if (Random.nextFloat() > 0.30f) return null

        val posX = spanStart + (spanWidth * 0.5f)
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
        
        val powerUpChance = if (level <= 5 || level % 5 == 0) 0.45f else 0.25f
        if (Random.nextFloat() > powerUpChance) return null

        val minX = spanStart + (spanWidth * 0.25f)
        val maxX = spanStart + (spanWidth * 0.75f)
        val posX = minX + Random.nextFloat() * (maxX - minX)

        val chosenType = when (Random.nextFloat()) {
            in 0.0f..0.45f -> com.mygames.stickmanrush.model.PowerUpType.INVINCIBILITY_SHIELD
            in 0.45f..0.80f -> com.mygames.stickmanrush.model.PowerUpType.MAGNET
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
        val isMoving = Random.nextFloat() < 0.20f
        return MovingPlatformConfig(
            isMoving = isMoving,
            amplitude = if (isMoving) 20f else 0f,
            speed = if (isMoving) 1.5f else 0f,
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
