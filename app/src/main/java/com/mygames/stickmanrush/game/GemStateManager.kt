package com.mygames.stickmanrush.game

import androidx.compose.ui.graphics.Color
import com.mygames.stickmanrush.data.GameRepository
import com.mygames.stickmanrush.model.GemData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class GemPickupEvent(
    val amount: Int,
    val comboMultiplier: Int,
    val x: Float,
    val y: Float,
    val isBonusCombo: Boolean
)

class GemStateManager(
    private val repository: GameRepository
) {
    private val _collectedInRun = MutableStateFlow(0)
    val collectedInRun: StateFlow<Int> = _collectedInRun.asStateFlow()

    private val _currentCombo = MutableStateFlow(0)
    val currentCombo: StateFlow<Int> = _currentCombo.asStateFlow()

    private var lastGemCollectTime = 0L

    // Track gem IDs actually spawned this run, and which have been collected
    private val spawnedGemIds = mutableSetOf<Long>()
    private val collectedGemIds = mutableSetOf<Long>()

    fun resetRun() {
        _collectedInRun.value = 0
        _currentCombo.value = 0
        lastGemCollectTime = 0L
        spawnedGemIds.clear()
        collectedGemIds.clear()
    }

    /**
     * Generates a gem entity located between the start and destination platform.
     * As levels increase:
     * - Gem spawn rates become more selective and exclusive.
     * - Gem positioning on higher levels tests acrobatic timing (tight flip windows requiring precise reflexes).
     */
    fun createGemForSpan(
        spanStartX: Float,
        spanEndX: Float,
        difficultyTier: DifficultyTier
    ): GemData? {
        val spanWidth = spanEndX - spanStartX
        if (spanWidth < 85f) return null

        if (Random.nextFloat() > difficultyTier.gemSpawnRate) return null

        // In higher tiers (Adept, Master), gems are placed in trickier positions on the bridge
        val (minFrac, maxFrac) = when (difficultyTier) {
            DifficultyTier.NOVICE_TRAINING -> Pair(0.30f, 0.60f)
            DifficultyTier.APPRENTICE -> Pair(0.25f, 0.65f)
            DifficultyTier.ADEPT -> Pair(0.20f, 0.70f)
            DifficultyTier.MASTER -> Pair(0.20f, 0.75f)
        }

        val minX = spanStartX + (spanWidth * minFrac)
        val maxX = (spanStartX + (spanWidth * maxFrac)).coerceAtMost(spanEndX - 45f)
        val gemX = if (maxX > minX) minX + Random.nextFloat() * (maxX - minX) else (spanStartX + spanEndX) / 2f

        // Higher difficulty tiers have 80% flip under-bridge challenge
        val flipThreshold = when (difficultyTier) {
            DifficultyTier.NOVICE_TRAINING -> 0.60f
            DifficultyTier.APPRENTICE -> 0.70f
            else -> 0.80f
        }
        val isUnder = Random.nextFloat() < flipThreshold

        val gem = GemData(
            id = System.nanoTime(),
            x = gemX,
            isUnderBridge = isUnder,
            collected = false,
            floatOffset = 0f
        )
        spawnedGemIds.add(gem.id)
        return gem
    }

    /**
     * Handles gem pickup, combo multiplier calculation, and repository persistence.
     * Rejects anything not spawned legitimately or already collected.
     */
    fun onGemCollected(gem: GemData, floorY: Float): GemPickupEvent? {
        if (gem.id !in spawnedGemIds || gem.id in collectedGemIds) {
            return null
        }
        collectedGemIds.add(gem.id)

        val now = System.currentTimeMillis()
        val isQuickChain = (now - lastGemCollectTime) < 12000L // within 12 seconds
        lastGemCollectTime = now

        val newCombo = if (isQuickChain) (_currentCombo.value + 1).coerceAtMost(5) else 1
        _currentCombo.value = newCombo

        // Multiplier bonus: combo 1 = 1 gem, combo 3+ = 2 gems (competitive high-skill ceiling)
        val gemReward = when {
            newCombo >= 3 -> 2
            else -> 1
        }

        _collectedInRun.value += gemReward
        repository.addGems(gemReward, com.mygames.stickmanrush.security.CurrencySource.GAMEPLAY_COLLECT)

        val gemY = if (gem.isUnderBridge) floorY + 40f else floorY - 20f

        return GemPickupEvent(
            amount = gemReward,
            comboMultiplier = newCombo,
            x = gem.x,
            y = gemY,
            isBonusCombo = newCombo > 1
        )
    }
}
