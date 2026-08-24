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
    // Levels 1 to 2 (Scores 0-7): Friendly onboarding, comfortable platforms
    NOVICE_TRAINING(
        tierLevel = 1,
        title = "RECRUIT",
        minLevel = 1,
        minScore = 0,
        minWidth = 120f,
        maxWidth = 155f,
        bullseyeTolerance = 22f,
        gemSpawnRate = 0.50f,
        doubleGemChance = 0.05f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.88f,
        badgeColorHex = 0xFF10B981 // Emerald Green
    ),
    // Level 3 (Scores 8-11): Stepping into genuine challenge - platform widths shrink, gap dynamics begin
    APPRENTICE(
        tierLevel = 2,
        title = "CHALLENGER",
        minLevel = 3,
        minScore = 8,
        minWidth = 85f,
        maxWidth = 115f,
        bullseyeTolerance = 16f,
        gemSpawnRate = 0.40f,
        doubleGemChance = 0.10f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.96f,
        badgeColorHex = 0xFF38BDF8 // Sky Blue
    ),
    // Level 4 (Scores 12-16): Dynamic bridge physics, high canyon gaps & narrow platforms
    ADEPT(
        tierLevel = 3,
        title = "ADEPT",
        minLevel = 4,
        minScore = 12,
        minWidth = 70f,
        maxWidth = 95f,
        bullseyeTolerance = 13f,
        gemSpawnRate = 0.35f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.08f,
        growthSpeedFactor = 1.02f,
        badgeColorHex = 0xFFA855F7 // Purple
    ),
    // Level 5 (Scores 17-21): Expert precision testing
    EXPERT(
        tierLevel = 4,
        title = "EXPERT",
        minLevel = 5,
        minScore = 17,
        minWidth = 55f,
        maxWidth = 78f,
        bullseyeTolerance = 10f,
        gemSpawnRate = 0.30f,
        doubleGemChance = 0.20f,
        movingPlatformChance = 0.14f,
        growthSpeedFactor = 1.08f,
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    // Level 6 to 9 (Scores 22-51): Master tier
    MASTER(
        tierLevel = 5,
        title = "MASTER",
        minLevel = 6,
        minScore = 22,
        minWidth = 44f,
        maxWidth = 65f,
        bullseyeTolerance = 8f,
        gemSpawnRate = 0.26f,
        doubleGemChance = 0.25f,
        movingPlatformChance = 0.20f,
        growthSpeedFactor = 1.14f,
        badgeColorHex = 0xFFEC4899 // Pink Neon
    ),
    // Level 10+ (Scores 52+): Legendary grandmaster tier
    GRANDMASTER(
        tierLevel = 6,
        title = "LEGEND",
        minLevel = 10,
        minScore = 52,
        minWidth = 35f,
        maxWidth = 52f,
        bullseyeTolerance = 6f,
        gemSpawnRate = 0.22f,
        doubleGemChance = 0.30f,
        movingPlatformChance = 0.28f,
        growthSpeedFactor = 1.20f,
        badgeColorHex = 0xFFEF4444 // Crimson Red
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

    private var previousGapCategory: Int = 0 // 0 = short, 1 = medium, 2 = long

    fun getTier(score: Int): DifficultyTier = DifficultyTier.getTierForScore(score)

    fun generatePlatformWidth(score: Int): Float {
        val tier = getTier(score)
        return Random.nextFloat() * (tier.maxWidth - tier.minWidth) + tier.minWidth
    }

    /**
     * Generates platform gap distance based on the user's level progression curve:
     * - Levels 1-2: Comfortable, predictable platform distances (120-170px).
     * - Levels 3+: Dynamic bridge physics with varied spans (Short: 120-165px, Medium: 175-245px, Canyon: 255-360px).
     * - Guarantees platform gap is never awkwardly close (< 115px).
     */
    fun generatePlatformGap(score: Int, level: Int, screenWidth: Float, isFirstBridgeOfLevel: Boolean = false): Float {
        val maxAvailableGap = (screenWidth * 0.54f).coerceAtLeast(300f)

        if (isFirstBridgeOfLevel) {
            // First bridge after level-up: majestic, open canyon distance
            val firstMin = 155f
            val firstMax = 225f.coerceAtMost(maxAvailableGap)
            return Random.nextFloat() * (firstMax - firstMin) + firstMin
        }

        if (level <= 2) {
            // First 2 levels: steady, easy-to-judge comfortable distance
            val minGap = 120f
            val maxGap = 165f.coerceAtMost(maxAvailableGap)
            return Random.nextFloat() * (maxGap - minGap) + minGap
        }

        // Levels 3 and beyond: dynamic bridge distance variations (small, medium, large canyon swings)
        val gapCategory = when (Random.nextFloat()) {
            in 0.0f..0.33f -> if (previousGapCategory == 0) 1 else 0
            in 0.33f..0.67f -> if (previousGapCategory == 1) 2 else 1
            else -> if (previousGapCategory == 2) 0 else 2
        }
        previousGapCategory = gapCategory

        val (minGap, maxGap) = when (gapCategory) {
            0 -> {
                // Short precision hop: clean, well-spaced (never cramped)
                Pair(115f, 160f)
            }
            1 -> {
                // Medium distance: standard bridge stretch
                Pair(170f, 245f)
            }
            else -> {
                // Thrilling canyon crossing with dynamic scaling
                val maxCanyon = (260f + (level * 5f)).coerceIn(270f, maxAvailableGap)
                Pair(250f, maxCanyon)
            }
        }

        return Random.nextFloat() * (maxGap - minGap) + minGap
    }

    /**
     * Generates dynamic platform height variations (elevated cliffs, stepping plateaus).
     * - Levels 1-2: Level flat terrain for intuitive learning.
     * - Level 3+: Dynamic height variance (steps up to -45px or down to +40px).
     */
    fun generatePlatformHeightOffset(score: Int, level: Int): Float {
        if (level <= 2) return 0f

        // 45% chance of dynamic elevation change on Level 3+
        if (Random.nextFloat() < 0.45f) {
            val maxVariance = when (level) {
                3 -> 25f
                4 -> 35f
                else -> 48f
            }
            // Step up or step down randomly
            val direction = if (Random.nextBoolean()) -1f else 1f
            return direction * (Random.nextFloat() * (maxVariance - 15f) + 15f)
        }
        return 0f
    }

    /**
     * Generates physical moving hazards & obstacles (spinning saws, spike mines, laser barriers).
     * - Levels 1-2: Safe spans for onboarding.
     * - Level 3+: Procedurally places hazards requiring tactical flips (upside-down or right-side up).
     */
    fun generateObstacle(spanStart: Float, spanEnd: Float, score: Int, level: Int, isBossLevel: Boolean): com.mygames.stickmanrush.model.ObstacleData? {
        if (level <= 2) return null
        val spanWidth = spanEnd - spanStart
        if (spanWidth < 140f) return null // Only spawn on medium/wide spans

        val obstacleChance = when {
            isBossLevel -> 0.20f // Boss already has projectiles
            level == 3 -> 0.35f
            level == 4 -> 0.45f
            else -> 0.55f
        }

        if (Random.nextFloat() > obstacleChance) return null

        // Position hazard midway along the span
        val posX = spanStart + (spanWidth * (Random.nextFloat() * 0.4f + 0.3f))
        
        // Choose obstacle type
        val chosenType = when (Random.nextInt(5)) {
            0 -> com.mygames.stickmanrush.model.ObstacleType.SPINNING_BLADE // Top of bridge
            1 -> com.mygames.stickmanrush.model.ObstacleType.SPIKE_MINE // Under bridge
            2 -> com.mygames.stickmanrush.model.ObstacleType.LASER_BARRIER // Pulsing laser
            3 -> com.mygames.stickmanrush.model.ObstacleType.SLIP_PATCH // Slippery ice patch
            else -> com.mygames.stickmanrush.model.ObstacleType.MOVING_SPIKE_BALL // Hovering orb
        }

        val isUnderBridge = chosenType == com.mygames.stickmanrush.model.ObstacleType.SPIKE_MINE || 
                           (chosenType == com.mygames.stickmanrush.model.ObstacleType.MOVING_SPIKE_BALL && Random.nextBoolean())

        return com.mygames.stickmanrush.model.ObstacleData(
            id = System.currentTimeMillis() + Random.nextInt(1000),
            x = posX,
            y = 0f, // Initialized relative to bridge floor in engine
            type = chosenType,
            isUnderBridge = isUnderBridge
        )
    }

    fun shouldSpawnGem(score: Int): Boolean {
        val tier = getTier(score)
        return Random.nextFloat() < tier.gemSpawnRate
    }

    fun isMovingPlatform(score: Int, level: Int): Boolean {
        if (level <= 2) return false
        val tier = getTier(score)
        val chance = when {
            level >= 5 -> 0.35f
            level >= 3 -> 0.25f
            else -> tier.movingPlatformChance
        }
        return Random.nextFloat() < chance
    }

    data class MovingPlatformConfig(
        val isMoving: Boolean,
        val amplitude: Float,
        val speed: Float,
        val isVertical: Boolean
    )

    fun generateMovingConfig(score: Int, level: Int): MovingPlatformConfig {
        if (!isMovingPlatform(score, level)) {
            return MovingPlatformConfig(isMoving = false, amplitude = 0f, speed = 0f, isVertical = false)
        }
        val isVertical = Random.nextFloat() < 0.40f
        val amplitude = if (isVertical) Random.nextFloat() * 18f + 14f else Random.nextFloat() * 24f + 16f
        val speed = Random.nextFloat() * 1.2f + 1.6f
        return MovingPlatformConfig(isMoving = true, amplitude = amplitude, speed = speed, isVertical = isVertical)
    }
}
