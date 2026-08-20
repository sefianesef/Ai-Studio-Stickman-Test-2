package com.example

import com.example.data.GameRepository
import com.example.game.StageThemes
import com.example.game.physics.PhysicsEngine
import com.example.model.GemData
import com.example.model.GameState
import com.example.model.PlatformData
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testStageThemesProgression() {
    val stage0 = StageThemes.getThemeForScore(0)
    assertEquals(1, stage0.stageNumber)

    val stage5 = StageThemes.getThemeForScore(5)
    assertEquals(2, stage5.stageNumber)

    val stage12 = StageThemes.getThemeForScore(12)
    assertEquals(3, stage12.stageNumber)
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
    val targetPlatform = PlatformData(id = 2L, leftX = 300f, width = 100f) // spans [300, 400], center = 350

    // Perfect hit: bridgeStartX = 150, length = 200 -> tip = 350
    val perfectHit = physics.evaluateBridgeLanding(150f, 200f, targetPlatform)
    assertTrue(perfectHit.isSuccessful)
    assertTrue(perfectHit.isBullseye)
    assertEquals(350f, perfectHit.bridgeTipX, 0.01f)

    // Normal safe hit: tip = 320 (in [300, 400])
    val safeHit = physics.evaluateBridgeLanding(150f, 170f, targetPlatform)
    assertTrue(safeHit.isSuccessful)
    assertFalse(safeHit.isBullseye)

    // Too short: tip = 280
    val tooShort = physics.evaluateBridgeLanding(150f, 130f, targetPlatform)
    assertFalse(tooShort.isSuccessful)

    // Overshoot: tip = 430
    val overshoot = physics.evaluateBridgeLanding(150f, 280f, targetPlatform)
    assertFalse(overshoot.isSuccessful)
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


