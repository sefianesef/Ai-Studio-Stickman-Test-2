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
        minWidth = 100f,
        maxWidth = 145f,
        minGap = 100f,
        maxGap = 190f,
        bullseyeTolerance = 14f,
        gemSpawnRate = 0.50f,
        doubleGemChance = 0.0f,
        movingPlatformChance = 0.0f,
        badgeColorHex = 0xFF10B981 // Emerald
    ),
    ADEPT(
        tierLevel = 2,
        title = "ADEPT",
        minScore = 5,
        minWidth = 75f,
        maxWidth = 110f,
        minGap = 130f,
        maxGap = 260f,
        bullseyeTolerance = 12f,
        gemSpawnRate = 0.65f,
        doubleGemChance = 0.15f,
        movingPlatformChance = 0.0f,
        badgeColorHex = 0xFF38BDF8 // Sky Blue
    ),
    EXPERT(
        tierLevel = 3,
        title = "EXPERT",
        minScore = 12,
        minWidth = 52f,
        maxWidth = 85f,
        minGap = 160f,
        maxGap = 320f,
        bullseyeTolerance = 10f,
        gemSpawnRate = 0.80f,
        doubleGemChance = 0.25f,
        movingPlatformChance = 0.20f,
        badgeColorHex = 0xFFA855F7 // Purple
    ),
    MASTER(
        tierLevel = 4,
        title = "MASTER",
        minScore = 22,
        minWidth = 38f,
        maxWidth = 65f,
        minGap = 180f,
        maxGap = 370f,
        bullseyeTolerance = 8.5f,
        gemSpawnRate = 0.90f,
        doubleGemChance = 0.40f,
        movingPlatformChance = 0.40f,
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    GRANDMASTER(
        tierLevel = 5,
        title = "LEGEND",
        minScore = 35,
        minWidth = 26f,
        maxWidth = 48f,
        minGap = 200f,
        maxGap = 420f,
        bullseyeTolerance = 7.5f,
        gemSpawnRate = 0.98f,
        doubleGemChance = 0.55f,
        movingPlatformChance = 0.60f,
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
