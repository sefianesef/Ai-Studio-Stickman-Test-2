package com.mygames.stickmanrush

import com.mygames.stickmanrush.data.GameRepository
import com.mygames.stickmanrush.game.StageThemes
import com.mygames.stickmanrush.game.physics.PhysicsEngine
import com.mygames.stickmanrush.model.GemData
import com.mygames.stickmanrush.model.GameState
import com.mygames.stickmanrush.model.PlatformData
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testStageThemesProgression() {
    val stageLevel1 = StageThemes.getThemeForLevel(1)
    assertEquals(1, stageLevel1.stageNumber)

    val stageLevel2 = StageThemes.getThemeForLevel(2)
    assertEquals(2, stageLevel2.stageNumber)

    val stageLevel7 = StageThemes.getThemeForLevel(7)
    assertEquals(7, stageLevel7.stageNumber)

    val stageLevel13 = StageThemes.getThemeForLevel(13)
    assertEquals(13, stageLevel13.stageNumber)

    // Score-based mappings to levels
    val stageScore0 = StageThemes.getThemeForScore(0) // Level 1
    assertEquals(1, stageScore0.stageNumber)

    val stageScore5 = StageThemes.getThemeForScore(5) // Level 2
    assertEquals(2, stageScore5.stageNumber)
  }

  @Test
  fun testDailyRewardAmounts() {
    assertEquals(7, GameRepository.DAILY_REWARD_AMOUNTS.size)
    assertEquals(3, GameRepository.DAILY_REWARD_AMOUNTS[0])
    assertEquals(35, GameRepository.DAILY_REWARD_AMOUNTS[6])
  }

  @Test
  fun testGameStateEnumValues() {
    assertTrue(GameState.values().contains(GameState.START))
    assertTrue(GameState.values().contains(GameState.IDLE))
    assertTrue(GameState.values().contains(GameState.GROWING))
    assertTrue(GameState.values().contains(GameState.FALLING_BRIDGE))
    assertTrue(GameState.values().contains(GameState.WALKING))
    assertTrue(GameState.values().contains(GameState.SCROLLING))
    assertTrue(GameState.values().contains(GameState.GAMEOVER))
  }

  @Test
  fun testPhysicsEngineBridgeLanding() {
    val physics = PhysicsEngine()
    val targetPlatform = PlatformData(id = 2L, leftX = 300f, width = 100f, heightOffset = 0f) // spans [300, 400], center = 350

    // Perfect hit: bridgeStartX = 150, currentHeightOffset = 0f, length = 200 -> tip = 350
    val perfectHit = physics.evaluateBridgeLanding(150f, 0f, 200f, targetPlatform)
    assertTrue(perfectHit.isSuccessful)
    assertTrue(perfectHit.isBullseye)
    assertEquals(350f, perfectHit.bridgeTipX, 0.01f)

    // Normal safe hit: tip = 320 (in [300, 400])
    val safeHit = physics.evaluateBridgeLanding(150f, 0f, 170f, targetPlatform)
    assertTrue(safeHit.isSuccessful)
    assertFalse(safeHit.isBullseye)

    // Too short: tip = 280
    val tooShort = physics.evaluateBridgeLanding(150f, 0f, 130f, targetPlatform)
    assertFalse(tooShort.isSuccessful)

    // Overshoot: tip = 430
    val overshoot = physics.evaluateBridgeLanding(150f, 0f, 280f, targetPlatform)
    assertFalse(overshoot.isSuccessful)

    // Elevated Platform Landing (e.g. Level 7 height variation)
    val elevatedPlatform = PlatformData(id = 3L, leftX = 300f, width = 100f, heightOffset = -30f) // higher cliff
    val elevatedHit = physics.evaluateBridgeLanding(150f, 0f, 202.24f, elevatedPlatform)
    assertTrue(elevatedHit.isSuccessful)
    assertTrue(elevatedHit.landingSlopeAngle < 0f) // negative slope upwards

    // Depressed Platform Landing
    val depressedPlatform = PlatformData(id = 4L, leftX = 300f, width = 100f, heightOffset = 30f) // lower ledge
    val depressedHit = physics.evaluateBridgeLanding(150f, 0f, 202.24f, depressedPlatform)
    assertTrue(depressedHit.isSuccessful)
    assertTrue(depressedHit.landingSlopeAngle > 0f) // positive slope downwards
  }

  @Test
  fun testPhysicsEngineObstacleJumpClearance() {
    val physics = PhysicsEngine()
    val obstacle = com.mygames.stickmanrush.model.ObstacleData(
        id = 10L,
        x = 220f,
        y = 600f,
        type = com.mygames.stickmanrush.model.ObstacleType.SPINNING_BLADE
    )

    // Walking right through obstacle without jumping -> collision!
    val hitWhileWalking = physics.checkObstacleCollision(
        stickmanX = 220f,
        isUpsideDown = false,
        obstacle = obstacle,
        jumpOffsetY = 0f
    )
    assertTrue(hitWhileWalking)

    // Jumping high over obstacle (jumpOffsetY = 35f) -> cleared safely!
    val clearedWhileJumping = physics.checkObstacleCollision(
        stickmanX = 220f,
        isUpsideDown = false,
        obstacle = obstacle,
        jumpOffsetY = 35f
    )
    assertFalse(clearedWhileJumping)
  }

  @Test
  fun testPhysicsEngineGemPickup() {
    val physics = PhysicsEngine()
    val gem = GemData(id = 1L, x = 250f, isUnderBridge = true)

    // Inverted stickman right on gem
    val canPickup = physics.checkGemPickup(250f, isUpsideDown = true, gem = gem)
    assertTrue(canPickup)

    // Upright stickman shouldn't reach under-bridge gem
    val cannotPickupWrongSide = physics.checkGemPickup(250f, isUpsideDown = false, gem = gem)
    assertFalse(cannotPickupWrongSide)

    // Far away stickman
    val tooFar = physics.checkGemPickup(100f, isUpsideDown = true, gem = gem)
    assertFalse(tooFar)
  }
}


