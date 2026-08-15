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
    val badgeColorHex: Long
) {
    APPRENTICE(
        tierLevel = 1,
        title = "NOVICE",
        minScore = 0,
        minWidth = 115f,
        maxWidth = 155f,
        minGap = 110f,
        maxGap = 210f,
        bullseyeTolerance = 14f,
        gemSpawnRate = 0.40f,
        doubleGemChance = 0.0f,
        movingPlatformChance = 0.0f,
        badgeColorHex = 0xFF10B981 // Emerald
    ),
    ADEPT(
        tierLevel = 2,
        title = "ADEPT",
        minScore = 5,
        minWidth = 90f,
        maxWidth = 130f,
        minGap = 140f,
        maxGap = 290f,
        bullseyeTolerance = 12f,
        gemSpawnRate = 0.55f,
        doubleGemChance = 0.10f,
        movingPlatformChance = 0.0f,
        badgeColorHex = 0xFF38BDF8 // Sky Blue
    ),
    EXPERT(
        tierLevel = 3,
        title = "EXPERT",
        minScore = 12,
        minWidth = 70f,
        maxWidth = 105f,
        minGap = 170f,
        maxGap = 350f,
        bullseyeTolerance = 11f,
        gemSpawnRate = 0.70f,
        doubleGemChance = 0.20f,
        movingPlatformChance = 0.25f,
        badgeColorHex = 0xFFA855F7 // Purple
    ),
    MASTER(
        tierLevel = 4,
        title = "MASTER",
        minScore = 22,
        minWidth = 52f,
        maxWidth = 88f,
        minGap = 190f,
        maxGap = 410f,
        bullseyeTolerance = 9.5f,
        gemSpawnRate = 0.85f,
        doubleGemChance = 0.35f,
        movingPlatformChance = 0.45f,
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    GRANDMASTER(
        tierLevel = 5,
        title = "LEGEND",
        minScore = 35,
        minWidth = 42f,
        maxWidth = 72f,
        minGap = 220f,
        maxGap = 460f,
        bullseyeTolerance = 8.5f,
        gemSpawnRate = 0.95f,
        doubleGemChance = 0.50f,
        movingPlatformChance = 0.65f,
        badgeColorHex = 0xFFEF4444 // Red
    );

    companion object {
        fun getTierForScore(score: Int): DifficultyTier {
            return when {
                score >= GRANDMASTER.minScore -> GRANDMASTER
                score >= MASTER.minScore -> MASTER
                score >= EXPERT.minScore -> EXPERT
                score >= ADEPT.minScore -> ADEPT
                else -> APPRENTICE
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
