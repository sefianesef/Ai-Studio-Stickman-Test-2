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
    // Levels 1 to 5 (Scores 0-9): Super forgiving, wide platforms, high bullseye hits, dopamine & confidence building!
    NOVICE_TRAINING(
        tierLevel = 1,
        title = "RECRUIT",
        minLevel = 1,
        minScore = 0,
        minWidth = 140f,
        maxWidth = 185f,
        bullseyeTolerance = 24f,
        gemSpawnRate = 0.65f,
        doubleGemChance = 0.05f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.85f,
        badgeColorHex = 0xFF10B981 // Emerald Green
    ),
    // Levels 6 to 8 (Scores 10-17): Gentle, slow progression
    APPRENTICE(
        tierLevel = 2,
        title = "APPRENTICE",
        minLevel = 6,
        minScore = 10,
        minWidth = 125f,
        maxWidth = 160f,
        bullseyeTolerance = 20f,
        gemSpawnRate = 0.70f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.90f,
        badgeColorHex = 0xFF38BDF8 // Sky Blue
    ),
    // Levels 9 to 12 (Scores 18-27): Smooth intermediate tier
    ADEPT(
        tierLevel = 3,
        title = "ADEPT",
        minLevel = 9,
        minScore = 18,
        minWidth = 105f,
        maxWidth = 140f,
        bullseyeTolerance = 17f,
        gemSpawnRate = 0.76f,
        doubleGemChance = 0.25f,
        movingPlatformChance = 0.05f,
        growthSpeedFactor = 0.95f,
        badgeColorHex = 0xFFA855F7 // Purple
    ),
    // Levels 13 to 17 (Scores 28-39): Skilled adventurer pacing
    EXPERT(
        tierLevel = 4,
        title = "EXPERT",
        minLevel = 13,
        minScore = 28,
        minWidth = 85f,
        maxWidth = 120f,
        bullseyeTolerance = 14f,
        gemSpawnRate = 0.82f,
        doubleGemChance = 0.35f,
        movingPlatformChance = 0.10f,
        growthSpeedFactor = 1.00f,
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    // Levels 18 to 24 (Scores 40-54): Master tier
    MASTER(
        tierLevel = 5,
        title = "MASTER",
        minLevel = 18,
        minScore = 40,
        minWidth = 70f,
        maxWidth = 100f,
        bullseyeTolerance = 11f,
        gemSpawnRate = 0.88f,
        doubleGemChance = 0.45f,
        movingPlatformChance = 0.18f,
        growthSpeedFactor = 1.05f,
        badgeColorHex = 0xFFEC4899 // Pink Neon
    ),
    // Level 25+ (Scores 55+): Legendary tier
    GRANDMASTER(
        tierLevel = 6,
        title = "LEGEND",
        minLevel = 25,
        minScore = 55,
        minWidth = 55f,
        maxWidth = 80f,
        bullseyeTolerance = 9f,
        gemSpawnRate = 0.95f,
        doubleGemChance = 0.55f,
        movingPlatformChance = 0.25f,
        growthSpeedFactor = 1.10f,
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
     * - Levels 1-3: Easy, comfortable, predictable platform distances (105-145px) with wide platforms so beginners learn bridge timing.
     * - Levels 4-5+: Rich "bridge physics" variation where distances vary dynamically (sometimes very small hops ~60-100px, 
     *   sometimes medium bridges ~140-210px, sometimes long canyon stretches ~220-320px).
     */
    fun generatePlatformGap(score: Int, level: Int, screenWidth: Float): Float {
        val maxAvailableGap = (screenWidth * 0.52f).coerceAtLeast(290f)

        if (level <= 3) {
            // First 3 levels: steady, easy-to-judge comfortable distance
            val minGap = 100f
            val maxGap = 145f.coerceAtMost(maxAvailableGap)
            return Random.nextFloat() * (maxGap - minGap) + minGap
        }

        // Levels 4, 5 and beyond: dynamic bridge distance variations (small, medium, large swings)
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
