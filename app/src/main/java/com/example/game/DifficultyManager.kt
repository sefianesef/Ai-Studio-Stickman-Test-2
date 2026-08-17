package com.example.game

import kotlin.random.Random

enum class DifficultyTier(
    val tierLevel: Int,
    val title: String,
    val minScore: Int,
    val minWidth: Float,
    val maxWidth: Float,
    val minGap: Float,
    val maxGap: Float,
    val bullseyeTolerance: Float,
    val gemSpawnRate: Float,
    val doubleGemChance: Float,
    val movingPlatformChance: Float,
    val growthSpeedFactor: Float,
    val badgeColorHex: Long
) {
    // Early Easy Levels (Scores 0-10): Super forgiving, wide platforms, high bullseye hits, dopamine & confidence building!
    NOVICE_TRAINING(
        tierLevel = 1,
        title = "RECRUIT",
        minScore = 0,
        minWidth = 140f,
        maxWidth = 190f,
        minGap = 75f,
        maxGap = 150f,
        bullseyeTolerance = 24f,
        gemSpawnRate = 0.65f,
        doubleGemChance = 0.05f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.88f,
        badgeColorHex = 0xFF10B981 // Emerald Green
    ),
    APPRENTICE(
        tierLevel = 2,
        title = "APPRENTICE",
        minScore = 11,
        minWidth = 115f,
        maxWidth = 165f,
        minGap = 90f,
        maxGap = 190f,
        bullseyeTolerance = 20f,
        gemSpawnRate = 0.70f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.0f,
        growthSpeedFactor = 0.94f,
        badgeColorHex = 0xFF38BDF8 // Sky Blue
    ),
    ADEPT(
        tierLevel = 3,
        title = "ADEPT",
        minScore = 22,
        minWidth = 90f,
        maxWidth = 135f,
        minGap = 115f,
        maxGap = 240f,
        bullseyeTolerance = 16f,
        gemSpawnRate = 0.78f,
        doubleGemChance = 0.25f,
        movingPlatformChance = 0.08f,
        growthSpeedFactor = 1.0f,
        badgeColorHex = 0xFFA855F7 // Purple
    ),
    EXPERT(
        tierLevel = 4,
        title = "EXPERT",
        minScore = 35,
        minWidth = 65f,
        maxWidth = 100f,
        minGap = 145f,
        maxGap = 295f,
        bullseyeTolerance = 12.5f,
        gemSpawnRate = 0.85f,
        doubleGemChance = 0.35f,
        movingPlatformChance = 0.22f,
        growthSpeedFactor = 1.06f,
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    MASTER(
        tierLevel = 5,
        title = "MASTER",
        minScore = 52,
        minWidth = 48f,
        maxWidth = 75f,
        minGap = 170f,
        maxGap = 350f,
        bullseyeTolerance = 10f,
        gemSpawnRate = 0.92f,
        doubleGemChance = 0.45f,
        movingPlatformChance = 0.38f,
        growthSpeedFactor = 1.12f,
        badgeColorHex = 0xFFEC4899 // Pink Neon
    ),
    GRANDMASTER(
        tierLevel = 6,
        title = "LEGEND",
        minScore = 75,
        minWidth = 32f,
        maxWidth = 55f,
        minGap = 195f,
        maxGap = 410f,
        bullseyeTolerance = 8f,
        gemSpawnRate = 0.98f,
        doubleGemChance = 0.55f,
        movingPlatformChance = 0.55f,
        growthSpeedFactor = 1.18f,
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

    fun getTier(score: Int): DifficultyTier = DifficultyTier.getTierForScore(score)

    fun generatePlatformWidth(score: Int): Float {
        val tier = getTier(score)
        return Random.nextFloat() * (tier.maxWidth - tier.minWidth) + tier.minWidth
    }

    fun generatePlatformGap(score: Int): Float {
        val tier = getTier(score)
        return Random.nextFloat() * (tier.maxGap - tier.minGap) + tier.minGap
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
