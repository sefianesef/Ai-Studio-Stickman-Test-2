package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.GameRepository
import com.example.model.GemData
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

    fun resetRun() {
        _collectedInRun.value = 0
        _currentCombo.value = 0
        lastGemCollectTime = 0L
    }

    /**
     * Generates a gem entity located between the start and destination platform.
     */
    fun createGemForSpan(
        spanStartX: Float,
        spanEndX: Float,
        difficultyTier: DifficultyTier
    ): GemData? {
        val spanWidth = spanEndX - spanStartX
        if (spanWidth < 90f) return null

        if (Random.nextFloat() > difficultyTier.gemSpawnRate) return null

        // Position gem in the first 25% - 60% of the bridge span so player always has ample space to flip up safely
        val minX = spanStartX + (spanWidth * 0.25f)
        val maxX = (spanStartX + (spanWidth * 0.60f)).coerceAtMost(spanEndX - 55f)
        val gemX = if (maxX > minX) minX + Random.nextFloat() * (maxX - minX) else (spanStartX + spanEndX) / 2f

        // 70% under the bridge (requiring flip), 30% on top of bridge
        val isUnder = Random.nextFloat() < 0.70f

        return GemData(
            id = System.nanoTime(),
            x = gemX,
            isUnderBridge = isUnder,
            collected = false,
            floatOffset = 0f
        )
    }

    /**
     * Handles gem pickup, combo multiplier calculation, and repository persistence.
     */
    fun onGemCollected(gem: GemData, floorY: Float): GemPickupEvent {
        val now = System.currentTimeMillis()
        val isQuickChain = (now - lastGemCollectTime) < 12000L // within 12 seconds
        lastGemCollectTime = now

        val newCombo = if (isQuickChain) (_currentCombo.value + 1).coerceAtMost(5) else 1
        _currentCombo.value = newCombo

        // Multiplier bonus: combo 1 = 1 gem, combo 2 = 2 gems, combo 3+ = 3 gems
        val gemReward = when {
            newCombo >= 4 -> 3
            newCombo >= 2 -> 2
            else -> 1
        }

        _collectedInRun.value += gemReward
        repository.addGems(gemReward)

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
