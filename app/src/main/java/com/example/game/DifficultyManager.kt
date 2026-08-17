package com.example.game

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
    // Levels 1 to 2 (Scores 0-7): Friendly onboarding, wide platforms, forgiving bullseye
    NOVICE_TRAINING(
        tierLevel = 1,
        title = "RECRUIT",
        minLevel = 1,
        minScore = 0,
        minWidth = 135f,
        maxWidth = 175f,
        bullseyeTolerance = 24f,
        gemSpawnRate = 0.85f,
        doubleGemChance = 0.10f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.85f,
        badgeColorHex = 0xFF10B981 // Emerald Green
    ),
    // Level 3 (Scores 8-11): Stepping into genuine challenge - platform widths shrink, gap dynamics begin, tighter tolerance
    APPRENTICE(
        tierLevel = 2,
        title = "CHALLENGER",
        minLevel = 3,
        minScore = 8,
        minWidth = 100f,
        maxWidth = 135f,
        bullseyeTolerance = 18f,
        gemSpawnRate = 0.65f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.95f,
        badgeColorHex = 0xFF38BDF8 // Sky Blue
    ),
    // Level 4 (Scores 12-16): Dynamic bridge physics, high canyon gaps & narrow platforms
    ADEPT(
        tierLevel = 3,
        title = "ADEPT",
        minLevel = 4,
        minScore = 12,
        minWidth = 78f,
        maxWidth = 110f,
        bullseyeTolerance = 14f,
        gemSpawnRate = 0.55f,
        doubleGemChance = 0.20f,
        movingPlatformChance = 0.08f,
        growthSpeedFactor = 1.00f,
        badgeColorHex = 0xFFA855F7 // Purple
    ),
    // Level 5 (Scores 17-21): Expert precision testing
    EXPERT(
        tierLevel = 4,
        title = "EXPERT",
        minLevel = 5,
        minScore = 17,
        minWidth = 65f,
        maxWidth = 92f,
        bullseyeTolerance = 11f,
        gemSpawnRate = 0.45f,
        doubleGemChance = 0.25f,
        movingPlatformChance = 0.14f,
        growthSpeedFactor = 1.05f,
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    // Level 6 to 9 (Scores 22-51): Master tier
    MASTER(
        tierLevel = 5,
        title = "MASTER",
        minLevel = 6,
        minScore = 22,
        minWidth = 52f,
        maxWidth = 78f,
        bullseyeTolerance = 9f,
        gemSpawnRate = 0.38f,
        doubleGemChance = 0.30f,
        movingPlatformChance = 0.20f,
        growthSpeedFactor = 1.10f,
        badgeColorHex = 0xFFEC4899 // Pink Neon
    ),
    // Level 10+ (Scores 52+): Legendary grandmaster tier
    GRANDMASTER(
        tierLevel = 6,
        title = "LEGEND",
        minLevel = 10,
        minScore = 52,
        minWidth = 42f,
        maxWidth = 64f,
        bullseyeTolerance = 7f,
        gemSpawnRate = 0.30f,
        doubleGemChance = 0.35f,
        movingPlatformChance = 0.28f,
        growthSpeedFactor = 1.15f,
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
     * - Levels 1-2: Easy, comfortable, predictable platform distances (105-145px) with wide platforms so beginners learn bridge timing.
     * - Levels 3+: Dynamic bridge physics where distances vary dynamically (sometimes very small hops ~55-105px, 
     *   sometimes medium bridges ~130-210px, sometimes long canyon stretches ~220-330px).
     */
    fun generatePlatformGap(score: Int, level: Int, screenWidth: Float): Float {
        val maxAvailableGap = (screenWidth * 0.52f).coerceAtLeast(290f)

        if (level <= 2) {
            // First 2 levels: steady, easy-to-judge comfortable distance
            val minGap = 100f
            val maxGap = 145f.coerceAtMost(maxAvailableGap)
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
                // Small distance: quick precision tap
                Pair(55f, 105f)
            }
            1 -> {
                // Medium distance: standard bridge stretch
                Pair(130f, 210f)
            }
            else -> {
                // Bigger distance: thrilling canyon crossing with dynamic scaling
                val maxCanyon = (230f + (level * 4f)).coerceIn(240f, maxAvailableGap)
                Pair(220f, maxCanyon)
            }
        }

        return Random.nextFloat() * (maxGap - minGap) + minGap
    }

    fun shouldSpawnGem(score: Int): Boolean {
        val tier = getTier(score)
        return Random.nextFloat() < tier.gemSpawnRate
    }

    fun isMovingPlatform(score: Int): Boolean {
        val tier = getTier(score)
        return Random.nextFloat() < tier.movingPlatformChance
    }
}
