package com.mygames.stickmanrush.game

import androidx.compose.ui.graphics.Color
import com.mygames.stickmanrush.audio.HapticManager
import com.mygames.stickmanrush.audio.SoundManager
import com.mygames.stickmanrush.data.GameRepository
import com.mygames.stickmanrush.game.physics.LandingResult
import com.mygames.stickmanrush.game.physics.PhysicsEngine
import com.mygames.stickmanrush.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class StickmanGameEngine(
    private val repository: GameRepository,
    private val soundManager: SoundManager,
    val hapticManager: HapticManager? = null,
    val physicsEngine: PhysicsEngine = PhysicsEngine(),
    val difficultyManager: DifficultyManager = DifficultyManager(),
    val gemStateManager: GemStateManager = GemStateManager(repository)
) {
    var onGemCollectedListener: ((Int) -> Unit)? = null

    val isFlipped: Boolean get() = isUpsideDown

    fun flipStickman() {
        triggerFlip()
    }

    fun startStretchingBridge() {
        if (_gameState.value == GameState.START || _gameState.value == GameState.IDLE) {
            onTouchDown()
        }
    }

    fun stopStretchingAndDrop() {
        if (_gameState.value == GameState.GROWING) {
            onTouchUp()
        }
    }

    fun checkGemCollision(x: Float, y: Float, gem: GemData): Boolean {
        return physicsEngine.checkGemPickup(x, isUpsideDown, gem)
    }
    // Game state
    private val _gameState = MutableStateFlow(GameState.START)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _currentLevel = MutableStateFlow(1)
    val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()

    private val _currentStage = MutableStateFlow(StageThemes.stages[0])
    val currentStage: StateFlow<StageTheme> = _currentStage.asStateFlow()

    private val _difficultyTier = MutableStateFlow(DifficultyTier.APPRENTICE)
    val difficultyTier: StateFlow<DifficultyTier> = _difficultyTier.asStateFlow()

    private val _isNewHighScore = MutableStateFlow(false)
    val isNewHighScore: StateFlow<Boolean> = _isNewHighScore.asStateFlow()

    private val _lastNearMiss = MutableStateFlow<NearMissInfo?>(null)
    val lastNearMiss: StateFlow<NearMissInfo?> = _lastNearMiss.asStateFlow()

    // Lives System (5 lives max, regenerating 1 life every 20 minutes)
    val lives: StateFlow<Int> = repository.lives
    val maxLives: Int = GameRepository.MAX_LIVES

    // Challenge Dialog State (Every 5 levels challenge popup: Level 5, 10, 15...)
    private val _activeChallengeDialog = MutableStateFlow<ChallengeDialogData?>(null)
    val activeChallengeDialog: StateFlow<ChallengeDialogData?> = _activeChallengeDialog.asStateFlow()

    // Level milestone & victory celebration state
    private val _levelVictoryCelebration = MutableStateFlow<String?>(null)
    val levelVictoryCelebration: StateFlow<String?> = _levelVictoryCelebration.asStateFlow()

    private val _activeLevelVictory = MutableStateFlow<LevelVictoryData?>(null)
    val activeLevelVictory: StateFlow<LevelVictoryData?> = _activeLevelVictory.asStateFlow()

    // Out of Wood Planks Dialog State
    private val _isOutOfPlanksDialog = MutableStateFlow(false)
    val isOutOfPlanksDialog: StateFlow<Boolean> = _isOutOfPlanksDialog.asStateFlow()

    fun dismissOutOfPlanksDialog() {
        _isOutOfPlanksDialog.value = false
    }

    fun watchAdForPlanks() {
        repository.addWoodPlanks(10)
        _isOutOfPlanksDialog.value = false
    }

    fun buyPlanksWithGems(): Boolean {
        if (repository.spendGems(20)) {
            repository.addWoodPlanks(20)
            _isOutOfPlanksDialog.value = false
            return true
        }
        return false
    }

    fun dismissLevelVictory() {
        _activeLevelVictory.value = null
    }

    fun dismissChallengeDialog() {
        _activeChallengeDialog.value = null
    }

    fun setStageTheme(stageTheme: StageTheme) {
        _currentStage.value = stageTheme
        val themeId = StageThemes.getThemeIdForStage(stageTheme.stageNumber)
        repository.equip(
            AccessoryItem(
                id = themeId,
                name = stageTheme.name,
                type = AccessoryType.THEME,
                cost = 0,
                primaryColor = stageTheme.platformHighlightColor.value.toLong(),
                secondaryColor = stageTheme.bgTopColor.value.toLong(),
                description = stageTheme.ambientDescription,
                iconSymbol = StageThemes.getThemeIcon(stageTheme)
            )
        )
        soundManager.playButton()
        hapticManager?.uiClick()
        addFloatingText(
            "THEME: ${stageTheme.name.uppercase()}",
            screenWidth / 2f,
            screenHeight * 0.35f,
            stageTheme.platformHighlightColor,
            scale = 1.25f
        )
        spawnConfetti(screenWidth / 2f, screenHeight * 0.38f, count = 20)
    }

    fun cycleNextTheme() {
        val allThemes = StageThemes.stages
        val currentIndex = allThemes.indexOfFirst { it.stageNumber == _currentStage.value.stageNumber }
        val nextIndex = if (currentIndex == -1 || currentIndex >= allThemes.size - 1) 0 else currentIndex + 1
        val nextTheme = allThemes[nextIndex]
        setStageTheme(nextTheme)
    }

    fun addLives(count: Int) {
        repository.addLives(count)
    }

    fun refillMaxLives() {
        repository.refillMaxLives()
    }

    val gemsCollectedRun: StateFlow<Int> = gemStateManager.collectedInRun
    val gemCombo: StateFlow<Int> = gemStateManager.currentCombo

    private val _revivalsUsed = MutableStateFlow(0)
    val revivalsUsed: StateFlow<Int> = _revivalsUsed.asStateFlow()

    // Active Tactical Power-Up State
    private val _activeMagnetTime = MutableStateFlow(0f)
    val activeMagnetTime: StateFlow<Float> = _activeMagnetTime.asStateFlow()

    // Second-Chance Revive (1 per run when falling near goal >70% track progress)
    private val _activeSecondChancePrompt = MutableStateFlow(false)
    val activeSecondChancePrompt: StateFlow<Boolean> = _activeSecondChancePrompt.asStateFlow()

    private val _secondChanceProgressPercent = MutableStateFlow(75)
    val secondChanceProgressPercent: StateFlow<Int> = _secondChanceProgressPercent.asStateFlow()

    private var secondChancePromptUsedThisRun = false

    private var pendingSecondChanceEligible = false
    private var pendingSecondChanceProgress = 70

    private val _hasInvincibilityShield = MutableStateFlow(false)
    val hasInvincibilityShield: StateFlow<Boolean> = _hasInvincibilityShield.asStateFlow()

    private val _activeGemDoublerTime = MutableStateFlow(0f)
    val activeGemDoublerTime: StateFlow<Float> = _activeGemDoublerTime.asStateFlow()

    private val _activeSlowMoTime = MutableStateFlow(0f)
    val activeSlowMoTime: StateFlow<Float> = _activeSlowMoTime.asStateFlow()

    private val _shieldShatterFx = MutableStateFlow(0f)
    val shieldShatterFx: StateFlow<Float> = _shieldShatterFx.asStateFlow()

    private val _activeBossState = MutableStateFlow<BossState?>(null)
    val activeBossState: StateFlow<BossState?> = _activeBossState.asStateFlow()

    private val _currentWindDrift = MutableStateFlow(0f)
    val currentWindDrift: StateFlow<Float> = _currentWindDrift.asStateFlow()

    fun getReviveCost(): Int {
        return when (_revivalsUsed.value) {
            0 -> 5
            1 -> 15
            2 -> 35
            else -> 75
        }
    }

    private var justLeveledUp = false

    // Physics & Layout coordinates (in DP/Canvas virtual pixels)
    var screenWidth = 1080f
    var screenHeight = 1920f
    var floorY = 1400f

    // Platforms
    var currentPlatform = PlatformData(id = 1L, leftX = 60f, width = 160f)
    var nextPlatform = PlatformData(id = 2L, leftX = 400f, width = 140f)

    // Bridge
    var bridgeStartX = 220f // right edge of current platform
    var stickLength = 0f
    var bridgeAngle = 0f // 0 = straight up, 90 = horizontal flat, 180 = dropped down
    var bridgeLandingSlope = 0f // Incline or decline angle for sloped platforms
    var targetBridgeTipX = 0f
    var bridgeAngularVel = 0f
    var bridgeImpactTime = 0f // Timer tracking spring oscillation wobble
    var bridgeBounceOffset = 0f
    var bridgeSagOffset = 0f
    private var growTickCounter = 0

    // Stickman
    var stickmanX = 120f
    var stickmanY = 1400f
    var stickmanFallVel = 0f
    var stickmanRotation = 0f
    var isUpsideDown = false
    var isJumping = false
    var jumpOffsetY = 0f
    var jumpVelocityY = 0f
    var jumpRotation = 0f
    var isSlipping = false
    var slipTimer = 0f
    var walkPhase = 0f

    // Parallax tracking
    var parallaxOffset = 0f

    // Scroll interpolation
    private var scrollTargetX = 0f
    private var scrollCurrentX = 0f

    // Results of bridge drop
    private var isSuccessfulLanding = false
    private var isPerfectHit = false
    private var targetStickmanWalkX = 0f
    private var fallElapsedTime = 0f
    private var hasSpawnedMidFallReaction = false

    // Visual effects
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingPopupText>()
    private var nextTextId = 0L

    // Theme transition alpha
    var themeTransitionProgress = 1f

    init {
        resetGame(initial = true)
    }

    fun setScreenDimensions(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        screenWidth = w
        screenHeight = h
        floorY = h * 0.72f
        stickmanY = floorY

        if (_gameState.value == GameState.START) {
            currentPlatform.leftX = 60f
            currentPlatform.width = 160f
            currentPlatform.gem = null
            spawnNextPlatform()
            stickmanX = currentPlatform.leftX + currentPlatform.width - 40f
            bridgeStartX = currentPlatform.leftX + currentPlatform.width
        }
    }

    fun resetGame(initial: Boolean = false, startLevel: Int = 1) {
        if (!initial) {
            repository.onRunCompleted()
        }
        soundManager.stopFallingSound()
        val targetStartLevel = startLevel.coerceAtLeast(1)
        val initialScore = ((targetStartLevel - 1) * 3).coerceAtLeast(0)
        _score.value = initialScore
        _currentLevel.value = targetStartLevel
        _isNewHighScore.value = false
        _revivalsUsed.value = 0
        secondChancePromptUsedThisRun = false
        _activeSecondChancePrompt.value = false
        justLeveledUp = false
        _difficultyTier.value = difficultyManager.getTier(initialScore)
        val equippedTheme = repository.selectedTheme.value
        val startLevel = computeLevelForScore(initialScore)
        _currentStage.value = StageThemes.getThemeForLevel(startLevel, equippedTheme)
        _levelVictoryCelebration.value = null
        _activeLevelVictory.value = null
        gemStateManager.resetRun()

        _activeMagnetTime.value = 0f
        _hasInvincibilityShield.value = false
        _activeGemDoublerTime.value = 0f
        _activeSlowMoTime.value = 0f
        _shieldShatterFx.value = 0f

        currentPlatform = PlatformData(id = 1L, leftX = 60f, width = 160f, heightOffset = 0f)
        checkOrSpawnBoss(targetStartLevel)
        spawnNextPlatform()

        bridgeStartX = currentPlatform.leftX + currentPlatform.width
        stickLength = 0f
        bridgeAngle = 0f
        bridgeLandingSlope = 0f
        targetBridgeTipX = 0f
        bridgeAngularVel = 0f
        bridgeImpactTime = 0f
        bridgeBounceOffset = 0f
        bridgeSagOffset = 0f
        growTickCounter = 0

        stickmanX = currentPlatform.leftX + currentPlatform.width - 40f
        stickmanY = floorY
        stickmanFallVel = 0f
        stickmanRotation = 0f
        fallElapsedTime = 0f
        hasSpawnedMidFallReaction = false
        isUpsideDown = false
        isJumping = false
        jumpOffsetY = 0f
        jumpVelocityY = 0f
        jumpRotation = 0f
        isSlipping = false
        slipTimer = 0f
        walkPhase = 0f
        parallaxOffset = 0f

        particles.clear()
        floatingTexts.clear()

        if (!initial) {
            repository.recordGamePlayed()
        }

        _gameState.value = if (initial) GameState.START else GameState.IDLE
    }

    private fun checkOrSpawnBoss(level: Int) {
        if (level % 5 == 0) {
            val bossType = when (level) {
                5 -> BossType.STONE_TITAN
                10 -> BossType.INFERNO_DRAGON
                15 -> BossType.CYBER_GOLEM
                else -> BossType.VOID_REAPER
            }
            _activeBossState.value = BossState(type = bossType)
        } else {
            _activeBossState.value = null
        }
    }

    fun startGame(startLevel: Int = 1) {
        soundManager.playButton()
        hapticManager?.uiClick()
        resetGame(initial = false, startLevel = startLevel)
        _gameState.value = GameState.IDLE

        // ⚔️ Start of Game / Checkpoint Provocative Challenge Prompt
        val nextMilestone = if (startLevel % 5 == 0) startLevel + 5 else ((startLevel / 5) + 1) * 5
        _activeChallengeDialog.value = ChallengeDialogData(
            levelNumber = startLevel,
            title = if (startLevel == 1) "I CHALLENGE YOU: REACH LEVEL 5!" else "CHECKPOINT: LEVEL $startLevel!",
            message = if (startLevel == 1) {
                "Think you have ninja precision? I challenge you: You can't even clear the first 5 levels!\nBuild bridges, balance your stickman, and prove your skills!"
            } else {
                "Starting from Checkpoint Level $startLevel! Can you beat the odds and reach Level $nextMilestone?"
            },
            type = ChallengeDialogType.PRE_LEVEL_TAUNT,
            rewardGems = if (startLevel == 1) 15 else 20,
            buttonText = "I ACCEPT THE CHALLENGE! ⚔️"
        )
    }

    /**
     * Loss Aversion / Revive Mechanic: Revives stickman right back on the platform with full score preserved.
     */
    fun reviveRun() {
        soundManager.stopFallingSound()
        soundManager.playPerfectHit()
        hapticManager?.perfectHit()
        _revivalsUsed.value += 1

        // Reset bridge & stickman to start of current platform
        stickLength = 0f
        bridgeAngle = 0f
        bridgeLandingSlope = 0f
        targetBridgeTipX = 0f
        bridgeAngularVel = 0f
        bridgeImpactTime = 0f
        bridgeBounceOffset = 0f
        bridgeSagOffset = 0f
        growTickCounter = 0

        stickmanX = currentPlatform.leftX + currentPlatform.width - 40f
        stickmanY = floorY + currentPlatform.heightOffset
        stickmanFallVel = 0f
        stickmanRotation = 0f
        isUpsideDown = false
        walkPhase = 0f

        // Clear boss projectiles for a clean second chance
        _activeBossState.value?.projectiles?.clear()

        // Golden revival shockwave & celebratory particles
        spawnConfetti(stickmanX, floorY - 60f, count = 25)
        addFloatingText("SECOND CHANCE! ✨", stickmanX, floorY - 100f, Color(0xFFFFD700), scale = 1.4f)

        _gameState.value = GameState.IDLE
    }

    fun acceptSecondChanceRevive(isRewardedAd: Boolean = true) {
        if (isRewardedAd) {
            repository.recordRewardedAdWatched()
        }
        secondChancePromptUsedThisRun = true
        _activeSecondChancePrompt.value = false
        reviveRun()
    }

    fun declineSecondChanceRevive() {
        _activeSecondChancePrompt.value = false
        repository.consumeLife()
        repository.updateHighScore(_score.value)

        val lvl = _currentLevel.value
        _activeChallengeDialog.value = ChallengeDialogData(
            levelNumber = lvl,
            title = if (lvl < 5) "CHALLENGE FAILED: YOU CAN'T CLEAR LEVEL 5!" else "CHALLENGE FAILED: LEVEL $lvl!",
            message = if (lvl < 5) {
                "I challenged you to clear Level 5 and you fell at Level $lvl! You can't clear it, better try again!"
            } else {
                "I told you that you can't clear Level $lvl! Better practice your bridge timing and try again!"
            },
            type = ChallengeDialogType.POST_LEVEL_FAIL,
            rewardGems = 0,
            buttonText = "TRY AGAIN & PROVE IT! 🔥"
        )
        _gameState.value = GameState.GAMEOVER
    }

    /**
     * Procedurally generates the next platform using DifficultyManager.
     */
    fun generateNextPlatform(currentScore: Int, screenW: Float): PlatformData {
        val tier = difficultyManager.getTier(currentScore)
        _difficultyTier.value = tier

        val level = computeLevelForScore(currentScore)
        val isBoss = (level % 5 == 0)
        val gap = difficultyManager.generatePlatformGap(currentScore, level, screenW, isFirstBridgeOfLevel = justLeveledUp)
        justLeveledUp = false

        val streak = currentScore
        val width = difficultyManager.generatePlatformWidth(currentScore, streak = streak).coerceIn(28f, 160f)
        val nextLeft = currentPlatform.leftX + currentPlatform.width + gap

        // Streak Milestone 30+: Update Wind Drift Force
        _currentWindDrift.value = difficultyManager.getWindDriftForce(streak)

        // Procedural Height Variation (Elevations & Depressions)
        val heightOffset = difficultyManager.generatePlatformHeightOffset(currentScore, level)

        // Procedural Gem Placement via GemStateManager
        val spanStart = currentPlatform.leftX + currentPlatform.width
        val spanEnd = nextLeft
        val gem = gemStateManager.createGemForSpan(spanStart, spanEnd, tier)

        // Procedural Physical Obstacle Hazard (Buzzsaws, Spike Mines, Laser Barriers, Ice Slip)
        val obstacle = difficultyManager.generateObstacle(spanStart, spanEnd, currentScore, level, isBossLevel = isBoss)

        // Procedural Tactical Power-Up (Magnet, Aegis Shield, Gem Doubler, Chrono Slow-Mo)
        val powerUp = difficultyManager.generatePowerUp(spanStart, spanEnd, currentScore, level, hasObstacle = (obstacle != null))

        // Procedural Moving Platform Config with Streak Escalation
        val movingConfig = difficultyManager.generateMovingConfig(currentScore, level, streak = streak)

        return PlatformData(
            id = nextPlatform.id + 1,
            leftX = nextLeft,
            width = width,
            hasRedDot = true,
            heightOffset = heightOffset,
            gem = gem,
            powerUp = powerUp,
            obstacle = obstacle,
            isMoving = movingConfig.isMoving,
            moveAmplitude = movingConfig.amplitude,
            moveSpeed = movingConfig.speed,
            moveVertical = movingConfig.isVertical,
            baseLeftX = nextLeft,
            baseHeightOffset = heightOffset
        )
    }

    private fun spawnNextPlatform() {
        nextPlatform = generateNextPlatform(_score.value, screenWidth)
    }

    // Actions for Jumping & Flipping
    fun triggerJump() {
        if (_gameState.value == GameState.WALKING) {
            if (!isJumping) {
                if (isUpsideDown) {
                    // Quick flip back up to bridge surface before leaping
                    isUpsideDown = false
                    soundManager.playFlip()
                    hapticManager?.flip()
                }
                isJumping = true
                jumpVelocityY = 520f // Peak jump arc ~90px
                jumpOffsetY = 4f
                jumpRotation = 0f
                soundManager.playJump()
                hapticManager?.jump()
                spawnJumpTakeoffEffects(stickmanX, stickmanY)
                addFloatingText("JUMP! 🦘", stickmanX, stickmanY - 50f, Color(0xFF38BDF8), scale = 1.15f)
                repository.trackMissionProgress("JUMP_HAZARD", 1)
            }
        }
    }

    fun triggerFlip() {
        if (_gameState.value == GameState.WALKING) {
            // If in mid-air jump, landing back onto bridge
            if (isJumping && jumpOffsetY > 15f) {
                // Ignore flip during high air jump
                return
            }
            if (isJumping) {
                isJumping = false
                jumpOffsetY = 0f
                jumpVelocityY = 0f
                jumpRotation = 0f
            }

            isUpsideDown = !isUpsideDown
            soundManager.playFlip()
            hapticManager?.flip()
            if (isUpsideDown) {
                repository.trackMissionProgress("FLIP_WALK", 1)
                repository.trackWeeklyMissionProgress("FLIP_WALK", 1)
            }
            spawnFlipAcrobaticsEffects(stickmanX, if (isUpsideDown) floorY + 30f else floorY)
        }
    }

    // Touch input handlers
    fun onTouchDown(touchX: Float = 0f, touchY: Float = 0f) {
        when (_gameState.value) {
            GameState.START, GameState.IDLE -> {
                val currentPlanks = repository.firestoreWoodPlanks.value
                if (currentPlanks <= 0) {
                    soundManager.playGameOver()
                    hapticManager?.uiClick()
                    _isOutOfPlanksDialog.value = true
                    return
                }
                soundManager.playButton()
                hapticManager?.bridgeGrowStart()
                stickLength = 0f
                bridgeAngle = 0f
                bridgeAngularVel = 0f
                bridgeImpactTime = 0f
                bridgeBounceOffset = 0f
                bridgeSagOffset = 0f
                growTickCounter = 0
                _gameState.value = GameState.GROWING
            }
            GameState.GAMEOVER -> {
                soundManager.playButton()
                hapticManager?.uiClick()
                resetGame(initial = false)
                _gameState.value = GameState.IDLE
            }
            GameState.GROWING -> {
                // Already growing while finger is pressed down
            }
            GameState.WALKING -> {
                if (isUpsideDown) {
                    // Tap flips upright back onto bridge
                    triggerFlip()
                } else {
                    // If tap is in bottom area, flip underneath; otherwise perform athletic jump
                    if (touchY > 0f && touchY > (floorY + 30f)) {
                        triggerFlip()
                    } else {
                        triggerJump()
                    }
                }
            }
            else -> {}
        }
    }

    fun onTouchUp() {
        if (_gameState.value == GameState.GROWING) {
            if (stickLength > 10f) {
                _gameState.value = GameState.FALLING_BRIDGE
                bridgeAngularVel = 0f
                hapticManager?.bridgeRelease()

                // Pre-evaluate landing result to determine target landing angle (incline or decline)
                val tier = difficultyManager.getTier(_score.value)
                val landingResult = physicsEngine.evaluateBridgeLanding(
                    bridgeStartX = bridgeStartX,
                    currentHeightOffset = currentPlatform.heightOffset,
                    stickLength = stickLength,
                    targetPlatform = nextPlatform,
                    bullseyeTolerance = tier.bullseyeTolerance
                )
                // If the bridge will successfully land on the target platform, land at the slope angle
                bridgeLandingSlope = landingResult.landingSlopeAngle

                soundManager.playBridgeFall()
                repository.recordBridgeBuilt()
                repository.trackMissionProgress("BUILD_BRIDGES", 1)
                repository.trackWeeklyMissionProgress("BUILD_BRIDGES", 1)
                repository.trackContestProgress("BUILD_BRIDGES", 1)
            } else {
                // Finger released too quickly; reset back to IDLE
                stickLength = 0f
                _gameState.value = GameState.IDLE
            }
        }
    }

    /**
     * Level Progression Curve:
     * - Level 1 (Score 0-3): 4 introductory bridges (easy & wide platforms)
     * - Level 2 (Score 4-7): 4 bridges (predictable comfortable distances)
     * - Level 3 (Score 8-11): 4 bridges (solid mastery)
     * - Level 4 (Score 12-16): 5 bridges (dynamic bridge physics & varying distances: small hops, medium bridges, long gaps)
     * - Level 5 (Score 17-21): 5 bridges (rich bridge physics variations)
     * - Level 6+ (Score 22+): Scaled progression with continuous endless arcade flow
     */
    fun computeLevelForScore(score: Int): Int {
        return when {
            score < 4 -> 1
            score < 8 -> 2
            score < 12 -> 3
            score < 17 -> 4
            score < 22 -> 5
            score < 28 -> 6
            score < 35 -> 7
            score < 43 -> 8
            score < 52 -> 9
            score < 62 -> 10
            else -> 10 + ((score - 62) / 10) + 1
        }
    }

    // Main Game Update Loop (deltaTime in seconds)
    fun update(dt: Float) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Update active particles & floating texts
        updateEffects(clampedDt)

        // Update Power-Up Active Timers
        if (_activeMagnetTime.value > 0f) {
            _activeMagnetTime.value = (_activeMagnetTime.value - clampedDt).coerceAtLeast(0f)
        }
        if (_activeGemDoublerTime.value > 0f) {
            _activeGemDoublerTime.value = (_activeGemDoublerTime.value - clampedDt).coerceAtLeast(0f)
        }
        if (_activeSlowMoTime.value > 0f) {
            _activeSlowMoTime.value = (_activeSlowMoTime.value - clampedDt).coerceAtLeast(0f)
        }
        if (_shieldShatterFx.value > 0f) {
            _shieldShatterFx.value = (_shieldShatterFx.value - clampedDt * 2.5f).coerceAtLeast(0f)
        }

        // Active Magnet Dynamic Attraction of Bridge Gems
        if (_activeMagnetTime.value > 0f) {
            nextPlatform.gem?.let { gem ->
                if (!gem.collected) {
                    val dist = kotlin.math.abs(stickmanX - gem.x)
                    if (dist < 600f) {
                        val dir = if (stickmanX > gem.x) 1f else -1f
                        val pullSpeed = (450f + (600f - dist) * 0.9f) * clampedDt
                        gem.x += dir * pullSpeed

                        if (particles.size < 120 && kotlin.random.Random.nextFloat() < 0.25f) {
                            spawnMagnetAttractSparks(gem.x, stickmanY - 20f, stickmanX, stickmanY - 20f)
                        }

                        if (dist < 36f) {
                            gem.collected = true
                            val multiplier = if (_activeGemDoublerTime.value > 0f) 2 else 1
                            gemStateManager.onGemCollected(gem, stickmanY)?.let { event ->
                                val finalAmt = event.amount * multiplier
                                if (multiplier > 1) {
                                    repository.addGems(event.amount, com.mygames.stickmanrush.security.CurrencySource.GAMEPLAY_COLLECT)
                                }
                                soundManager.playGemCollect()
                                soundManager.playMagnetAttract()
                                hapticManager?.magnetPulse()
                                repository.trackMissionProgress("COLLECT_GEMS", finalAmt)
                                repository.trackWeeklyMissionProgress("COLLECT_GEMS", finalAmt)
                                repository.trackContestProgress("COLLECT_GEMS", finalAmt)
                                repository.recordGemsHarvested(finalAmt)
                                val label = if (multiplier > 1) "🧲 2X 💎 +$finalAmt" else "🧲 💎 +$finalAmt"
                                addFloatingText(label, stickmanX, stickmanY - 35f, Color(0xFF38BDF8), scale = 1.3f)
                                spawnGemCollectEffects(stickmanX, stickmanY)
                                spawnHeroGemCollectAura(stickmanX, stickmanY, multiplier > 1, 1)
                            }
                        }
                    }
                }
            }
        }

        // Update Obstacle animations
        nextPlatform.obstacle?.let { obs ->
            obs.animPhase += clampedDt * 5f
            if (obs.type == ObstacleType.MOVING_SPIKE_BALL) {
                obs.y = sin(obs.animPhase) * 18f
            }
        }

        // Update Tactical Power-Up Slanted Bridge Tracking & Floating Bobbing
        nextPlatform.powerUp?.let { pUp ->
            if (!pUp.collected) {
                val spanW = (nextPlatform.leftX - bridgeStartX).coerceAtLeast(10f)
                val prog = ((pUp.x - bridgeStartX) / spanW).coerceIn(0.15f, 0.85f)
                val startY = floorY + currentPlatform.heightOffset
                val endY = floorY + nextPlatform.heightOffset
                val bY = startY + prog * (endY - startY)
                pUp.y = if (pUp.isUnderBridge) bY + 22f else bY - 20f
                pUp.floatOffset = sin(System.currentTimeMillis() * 0.005f + pUp.x) * 4.5f
            }
        }

        // Update Boss Mechanics & Projectiles
        _activeBossState.value?.let { boss ->
            if (!boss.isDefeated && _gameState.value != GameState.GAMEOVER && _gameState.value != GameState.START) {
                boss.attackTimer += clampedDt
                if (boss.attackTimer >= boss.attackInterval && (_gameState.value == GameState.IDLE || _gameState.value == GameState.GROWING || _gameState.value == GameState.WALKING)) {
                    boss.attackTimer = 0f
                    // Boss launches a projectile
                    val isHigh = (kotlin.random.Random.nextFloat() < 0.65f)
                    val spawnX = (nextPlatform.leftX + nextPlatform.width + 120f).coerceAtMost(screenWidth + 60f)
                    val spawnY = floorY + nextPlatform.heightOffset - (if (isHigh) 28f else 0f)
                    boss.projectiles.add(
                        BossProjectile(
                            id = System.currentTimeMillis() + kotlin.random.Random.nextInt(1000),
                            x = spawnX,
                            y = spawnY,
                            vx = -(190f + (_currentLevel.value * 5f)),
                            colorHex = boss.type.primaryColorHex,
                            isHigh = isHigh
                        )
                    )
                    spawnTipSparks(spawnX, spawnY)
                }

                // Update Projectiles
                val pIter = boss.projectiles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.vx * clampedDt
                    // Spawn fiery tracer particles
                    if (particles.size < 85 && kotlin.random.Random.nextFloat() < 0.35f) {
                        particles.add(
                            Particle(
                                x = p.x,
                                y = p.y,
                                vx = kotlin.random.Random.nextFloat() * 20f,
                                vy = (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
                                color = Color(p.colorHex),
                                radius = 4.5f,
                                maxLife = 0.35f,
                                life = 0.35f,
                                shape = ParticleShape.FIRE_EMBER
                            )
                        )
                    }
                    if (p.x < -60f) {
                        pIter.remove()
                    }
                }
            }
        }

        // Procedural Moving Platform Oscillation
        if (nextPlatform.isMoving && (_gameState.value == GameState.IDLE || _gameState.value == GameState.GROWING || _gameState.value == GameState.FALLING_BRIDGE)) {
            nextPlatform.movePhase += clampedDt * nextPlatform.moveSpeed
            if (nextPlatform.moveVertical) {
                nextPlatform.heightOffset = nextPlatform.baseHeightOffset + sin(nextPlatform.movePhase) * nextPlatform.moveAmplitude
            } else {
                nextPlatform.leftX = nextPlatform.baseLeftX + sin(nextPlatform.movePhase) * nextPlatform.moveAmplitude
            }
        }

        // Procedural Obstacle Hazard Slanted Bridge Tracking
        nextPlatform.obstacle?.let { obs ->
            obs.animPhase += clampedDt * 6f
            val spanW = (nextPlatform.leftX - bridgeStartX).coerceAtLeast(10f)
            val prog = ((obs.x - bridgeStartX) / spanW).coerceIn(0.15f, 0.85f)
            val startY = floorY + currentPlatform.heightOffset
            val endY = floorY + nextPlatform.heightOffset
            val bY = startY + prog * (endY - startY)
            obs.y = if (obs.isUnderBridge) bY + 22f else bY - 20f
        }

        when (_gameState.value) {
            GameState.GROWING -> {
                // Growth speed scales gently with current difficulty tier for easy early control
                val tier = difficultyManager.getTier(_score.value)
                val speed = (310f + (stickLength * 0.40f)) * tier.growthSpeedFactor
                val prevLength = stickLength
                stickLength += speed * clampedDt

                // Sound & haptic ticks
                val tickStep = 20f
                if ((stickLength / tickStep).toInt() > (prevLength / tickStep).toInt()) {
                    growTickCounter++
                    soundManager.playGrowTick(growTickCounter)
                    val stretchRatio = (stickLength / screenWidth.coerceAtLeast(300f)).coerceIn(0f, 1f)
                    hapticManager?.triggerBridgeExtend(stretchRatio = stretchRatio, tierLevel = tier.tierLevel)

                    // Themed sparks at the growing bridge tip
                    if (particles.size < 80) {
                        val stickSkin = repository.availableAccessories.find { it.id == repository.selectedStick.value }
                        spawnTipSparks(bridgeStartX, (floorY + currentPlatform.heightOffset) - stickLength, stickSkin)
                    }
                }
            }

            GameState.FALLING_BRIDGE -> {
                // Physics-based gravitational angular acceleration (scaled when Chrono Slow-Mo is active)
                val slowMoFactor = if (_activeSlowMoTime.value > 0f) 0.60f else 1.0f
                val torque = physicsEngine.computeBridgeAngularAcceleration(bridgeAngle) * slowMoFactor
                bridgeAngularVel += torque * clampedDt
                bridgeAngle += bridgeAngularVel * clampedDt

                val tier = difficultyManager.getTier(_score.value)
                val rawLandingResult = physicsEngine.evaluateBridgeLanding(
                    bridgeStartX = bridgeStartX,
                    currentHeightOffset = currentPlatform.heightOffset,
                    stickLength = stickLength,
                    targetPlatform = nextPlatform,
                    bullseyeTolerance = tier.bullseyeTolerance
                )

                var landingResult = rawLandingResult
                if (!landingResult.isSuccessful && _hasInvincibilityShield.value) {
                    _hasInvincibilityShield.value = false
                    _shieldShatterFx.value = 1f
                    soundManager.playShieldShatter()
                    hapticManager?.shieldShatter()
                    spawnShieldShatterShockwave(bridgeStartX + stickLength, floorY + nextPlatform.heightOffset)

                    val platformStart = nextPlatform.leftX
                    val platformEnd = nextPlatform.leftX + nextPlatform.width
                    val platformCenter = nextPlatform.leftX + (nextPlatform.width / 2f)

                    val adjustedWalkX = if (rawLandingResult.bridgeTipX < platformStart) {
                        platformStart + 25f // short bridge -> walk onto platform safely
                    } else {
                        platformEnd - 25f // long bridge -> walk back onto platform safely
                    }

                    val rescueMsg = if (rawLandingResult.bridgeTipX < platformStart) {
                        "SHIELD RESCUE: SHORT BRIDGE WALK! 🛡️"
                    } else {
                        "SHIELD RESCUE: WALKING BACK! 🛡️"
                    }
                    addFloatingText(rescueMsg, platformCenter, floorY + nextPlatform.heightOffset - 90f, Color(0xFF38BDF8), scale = 1.35f)

                    landingResult = LandingResult(
                        isSuccessful = true,
                        isBullseye = false,
                        bridgeTipX = adjustedWalkX,
                        targetWalkX = adjustedWalkX,
                        platformCenter = platformCenter,
                        landingSlopeAngle = 0f,
                        nearMiss = null
                    )
                }

                bridgeLandingSlope = landingResult.landingSlopeAngle
                val targetLandingAngle = PhysicsEngine.MAX_BRIDGE_ANGLE + bridgeLandingSlope

                if (bridgeAngle >= targetLandingAngle) {
                    bridgeAngle = targetLandingAngle
                    bridgeAngularVel = 0f
                    bridgeImpactTime = 0f

                    soundManager.playBridgePlaced()

                    isSuccessfulLanding = landingResult.isSuccessful
                    isPerfectHit = landingResult.isBullseye
                    targetStickmanWalkX = landingResult.targetWalkX
                    targetBridgeTipX = landingResult.bridgeTipX
                    bridgeLandingSlope = landingResult.landingSlopeAngle
                    _lastNearMiss.value = landingResult.nearMiss

                    // Trigger tactile bridge landing vibration
                    if (landingResult.isSuccessful) {
                        hapticManager?.triggerLandingSuccess(
                            tierLevel = tier.tierLevel,
                            isBullseye = landingResult.isBullseye,
                            isElevated = nextPlatform.heightOffset != currentPlatform.heightOffset,
                            isNearMiss = landingResult.nearMiss != null
                        )
                    } else {
                        if (landingResult.nearMiss != null) {
                            hapticManager?.nearMiss()
                        } else {
                            hapticManager?.bridgeFail()
                        }
                    }

                    if (landingResult.isSuccessful) {
                        val impactY = floorY + nextPlatform.heightOffset
                        spawnLandingEffects(landingResult.bridgeTipX, impactY, landingResult.isBullseye)

                        // Boss damage if boss active
                        _activeBossState.value?.let { boss ->
                            if (!boss.isDefeated) {
                                val damage = if (landingResult.isBullseye) 2 else 1
                                boss.currentHp = (boss.currentHp - damage).coerceAtLeast(0)
                                soundManager.playPerfectHit()
                                hapticManager?.levelUp()
                                spawnLandingEffects(nextPlatform.leftX + (nextPlatform.width / 2f), impactY - 50f, true)
                                if (landingResult.isBullseye) {
                                    addFloatingText("CRITICAL HIT! -${damage} HP 💥", nextPlatform.leftX + (nextPlatform.width / 2f), impactY - 90f, Color(0xFFFF4500), scale = 1.35f)
                                } else {
                                    addFloatingText("STRIKE! -${damage} HP ⚔️", nextPlatform.leftX + (nextPlatform.width / 2f), impactY - 75f, Color(0xFFFFD700), scale = 1.2f)
                                }

                                if (boss.currentHp <= 0) {
                                    boss.isDefeated = true
                                    boss.projectiles.clear()
                                    repository.addGems(boss.type.gemReward, com.mygames.stickmanrush.security.CurrencySource.LEVEL_MILESTONE)
                                    repository.recordGemsHarvested(boss.type.gemReward)
                                    spawnConfetti(screenWidth / 2f, screenHeight * 0.35f, count = 60)
                                    soundManager.playVictoryMusic()
                                    addFloatingText("BOSS DEFEATED! 🏆 +${boss.type.gemReward} GEMS", screenWidth / 2f, screenHeight * 0.32f, Color(0xFFFFD700), scale = 1.5f)
                                }
                            }
                        }

                        if (landingResult.isBullseye) {
                            soundManager.playPerfectHit()
                            repository.recordPerfectHit()
                            repository.trackMissionProgress("PERFECT_HITS", 1)
                            repository.trackWeeklyMissionProgress("PERFECT_HITS", 1)
                            repository.trackContestProgress("PERFECT_HITS", 1)
                            addFloatingText(
                                "PERFECT! +2",
                                landingResult.platformCenter,
                                impactY - 90f,
                                Color(0xFFFFD700),
                                scale = 1.3f
                            )
                            _score.value += 1 // Bonus +1 for bullseye
                            repository.addGems(1, com.mygames.stickmanrush.security.CurrencySource.PERFECT_BULLSEYE)
                            repository.recordGemsHarvested(1)
                        }

                        _gameState.value = GameState.WALKING
                    } else {
                        landingResult.nearMiss?.let { nearMiss ->
                            addFloatingText(
                                nearMiss.message,
                                landingResult.bridgeTipX,
                                floorY + nextPlatform.heightOffset - 80f,
                                Color(0xFFF43F5E),
                                scale = 1.25f
                            )
                        }
                        _gameState.value = GameState.WALKING
                    }
                }
            }

            GameState.WALKING -> {
                bridgeImpactTime += clampedDt
                bridgeBounceOffset = physicsEngine.computeLandingBounceAngle(bridgeImpactTime, stickLength)

                // 1. Jump Physics Update & Airborne Trail
                if (isJumping) {
                    jumpOffsetY += jumpVelocityY * clampedDt
                    jumpVelocityY -= 1400f * clampedDt // Gravity
                    jumpRotation += 360f * clampedDt // Aerial somersault spin

                    if (particles.size < 130) {
                        spawnJumpAirborneTrail(stickmanX, stickmanY - jumpOffsetY)
                    }

                    if (jumpOffsetY <= 0f) {
                        // Landed safely back onto bridge surface
                        jumpOffsetY = 0f
                        isJumping = false
                        jumpVelocityY = 0f
                        jumpRotation = 0f
                        soundManager.playStickmanLand()
                        hapticManager?.jumpLanding()
                        spawnFootstepDust(stickmanX, stickmanY)
                    }
                }

                // Slip hazard physics check
                if (physicsEngine.checkObstacleSlip(stickmanX, isUpsideDown, nextPlatform.obstacle, jumpOffsetY)) {
                    if (!isSlipping) {
                        isSlipping = true
                        slipTimer = 0.55f
                        soundManager.playFlip()
                        addFloatingText("WHOOSH! SLIPPING! ⛸️", stickmanX, stickmanY - 45f, Color(0xFF38BDF8), scale = 1.25f)
                    }
                }
                if (slipTimer > 0f) {
                    slipTimer -= clampedDt
                    if (slipTimer <= 0f) isSlipping = false
                }
                if (isSlipping && particles.size < 75) {
                    spawnFootstepDust(stickmanX, stickmanY)
                }

                val currentWalkSpeed = if (isSlipping) PhysicsEngine.WALK_SPEED * 1.85f else PhysicsEngine.WALK_SPEED
                val stepX = currentWalkSpeed * clampedDt
                stickmanX += stepX
                parallaxOffset += stepX
                walkPhase += clampedDt * (if (isSlipping) 26f else 16f)

                // Bridge sag & slope calculation when walking on span
                val startY = floorY + currentPlatform.heightOffset
                val targetY = floorY + (if (isSuccessfulLanding) nextPlatform.heightOffset else currentPlatform.heightOffset)

                if (isSuccessfulLanding) {
                    val bridgeTip = if (targetBridgeTipX > bridgeStartX) targetBridgeTipX else nextPlatform.leftX
                    if (stickmanX < nextPlatform.leftX) {
                        val bridgeSpan = (bridgeTip - bridgeStartX).coerceAtLeast(10f)
                        val bridgeProgress = ((stickmanX - bridgeStartX) / bridgeSpan).coerceIn(0f, 1f)
                        bridgeSagOffset = physicsEngine.computeBridgeSag(bridgeProgress, stickLength)
                        stickmanY = startY + (bridgeProgress * (targetY - startY)) + bridgeSagOffset
                    } else {
                        bridgeSagOffset = 0f
                        stickmanY = targetY
                    }
                } else {
                    val spanWidth = (targetStickmanWalkX - bridgeStartX).coerceAtLeast(10f)
                    val walkProgress = ((stickmanX - bridgeStartX) / spanWidth).coerceIn(0f, 1f)
                    bridgeSagOffset = physicsEngine.computeBridgeSag(walkProgress, stickLength)
                    val endY = if (bridgeLandingSlope != 0f) targetY else startY
                    stickmanY = startY + (walkProgress * (endY - startY)) + bridgeSagOffset
                }

                // Footstep sound, dust puff & glowing speed trail behind stickman
                if (!isJumping && particles.size < 120) {
                    spawnRunningTrail(
                        stickmanX = stickmanX,
                        stickmanY = stickmanY,
                        isUpsideDown = isUpsideDown,
                        isSlipping = isSlipping,
                        skinId = repository.selectedSkin.value
                    )
                }

                if (!isJumping && sin(walkPhase) > 0.95f) {
                    soundManager.playWalkStep()
                    if (particles.size < 90) {
                        spawnFootstepDust(stickmanX, if (isUpsideDown) stickmanY - 20f else stickmanY)
                    }
                }

                // Check Gem Pickup along the bridge using PhysicsEngine & GemStateManager
                nextPlatform.gem?.let { currentGem ->
                    if (isFlipped && checkGemCollision(stickmanX, stickmanY, currentGem)) {
                        if (!currentGem.collected) {
                            currentGem.collected = true
                            soundManager.playGemCollectSound()
                            onGemCollectedListener?.invoke(1)
                        }
                    } else if (physicsEngine.checkGemPickup(stickmanX, isUpsideDown, currentGem)) {
                        if (!currentGem.collected) {
                            currentGem.collected = true
                            val multiplier = if (_activeGemDoublerTime.value > 0f) 2 else 1
                            gemStateManager.onGemCollected(currentGem, stickmanY)?.let { event ->
                                val finalAmt = event.amount * multiplier
                                if (multiplier > 1) {
                                    repository.addGems(event.amount, com.mygames.stickmanrush.security.CurrencySource.GAMEPLAY_COLLECT)
                                }
                                soundManager.playGemCollect()
                                soundManager.playGemCollectSound()
                                onGemCollectedListener?.invoke(finalAmt)
                                hapticManager?.gemCollect(event.comboMultiplier)
                                repository.trackMissionProgress("COLLECT_GEMS", finalAmt)
                                repository.trackWeeklyMissionProgress("COLLECT_GEMS", finalAmt)
                                repository.trackContestProgress("COLLECT_GEMS", finalAmt)
                                repository.recordGemsHarvested(finalAmt)

                                val comboLabel = if (multiplier > 1) {
                                    "✨ 2X 💎 +$finalAmt"
                                } else if (event.comboMultiplier > 1) {
                                    "💎 +${event.amount} (${event.comboMultiplier}x COMBO!)"
                                } else {
                                    "💎 +${event.amount}"
                                }
                                addFloatingText(comboLabel, event.x, event.y - 30f, if (event.comboMultiplier > 1 || multiplier > 1) Color(0xFFFFD700) else Color(0xFF38BDF8), scale = if (event.comboMultiplier > 1 || multiplier > 1) 1.25f else 1.0f)
                                spawnGemCollectEffects(event.x, event.y)
                                spawnHeroGemCollectAura(stickmanX, stickmanY, multiplier > 1, event.comboMultiplier)
                            }
                        }
                    }
                }

                // Check Tactical Power-Up Pickup (Magnet, Shield, Gem Doubler, Slow Motion)
                nextPlatform.powerUp?.let { pUp ->
                    if (!pUp.collected && physicsEngine.checkPowerUpPickup(stickmanX, isUpsideDown, pUp)) {
                        pUp.collected = true
                        when (pUp.type) {
                            PowerUpType.MAGNET -> {
                                _activeMagnetTime.value = pUp.type.durationSeconds
                                addFloatingText("🧲 MAGNET EQUIPPED! 14s", pUp.x, stickmanY - 45f, Color(pUp.type.primaryColorHex), scale = 1.35f)
                            }
                            PowerUpType.INVINCIBILITY_SHIELD -> {
                                _hasInvincibilityShield.value = true
                                addFloatingText("🛡️ AEGIS SHIELD READY!", pUp.x, stickmanY - 45f, Color(pUp.type.primaryColorHex), scale = 1.35f)
                            }
                            PowerUpType.GEM_DOUBLER -> {
                                _activeGemDoublerTime.value = pUp.type.durationSeconds
                                addFloatingText("✨ 2X GEMS ACTIVATED! 15s", pUp.x, stickmanY - 45f, Color(pUp.type.primaryColorHex), scale = 1.35f)
                            }
                            PowerUpType.SLOW_MOTION -> {
                                _activeSlowMoTime.value = pUp.type.durationSeconds
                                addFloatingText("⏱️ CHRONO SLOW-MO! 12s", pUp.x, stickmanY - 45f, Color(pUp.type.primaryColorHex), scale = 1.35f)
                            }
                        }
                        soundManager.playPowerUpPickup()
                        hapticManager?.powerUpPickup()
                        spawnPowerUpBurst(pUp.x, stickmanY, Color(pUp.type.primaryColorHex))
                    }
                }

                // Check Obstacle Hazard Collision along the bridge span (Supports Jumping Clearance & Shield Defense)
                nextPlatform.obstacle?.let { obstacle ->
                    if (physicsEngine.checkObstacleCollision(stickmanX, isUpsideDown, obstacle, jumpOffsetY)) {
                        if (_hasInvincibilityShield.value) {
                            // 🛡️ Aegis Shield absorbs the fatal impact!
                            _hasInvincibilityShield.value = false
                            _shieldShatterFx.value = 1f
                            obstacle.isActive = false
                            obstacle.isDodged = true
                            soundManager.playShieldShatter()
                            hapticManager?.shieldShatter()
                            spawnShieldShatterShockwave(stickmanX, stickmanY)
                            addFloatingText("SHIELD DEFENSE! 🛡️💥", stickmanX, stickmanY - 45f, Color(0xFF38BDF8), scale = 1.4f)
                        } else {
                            fallElapsedTime = 0f
                            hasSpawnedMidFallReaction = false
                            soundManager.playStickmanFall()
                            hapticManager?.gameOver()
                            spawnDust(stickmanX, stickmanY, count = 16)
                            stickmanFallVel = 60f
                            val obstacleHits = listOf("OUCH! 🪚", "ZAPPED! ⚡", "SPIKED! 💥", "HIT HAZARD! 💀", "BURNED! 🔥")
                            addFloatingText(obstacleHits.random(), stickmanX, stickmanY - 30f, Color(0xFFEF4444), scale = 1.35f)
                            _gameState.value = GameState.DROPPING_FAIL
                            return
                        }
                    } else if (!obstacle.isDodged && stickmanX > obstacle.x + 25f) {
                        obstacle.isDodged = true
                        soundManager.playFlip()
                        if (isJumping || jumpOffsetY > 12f) {
                            addFloatingText("VAULT OVER HAZARD! 🦘 +50", stickmanX, stickmanY - 45f, Color(0xFF38BDF8), scale = 1.25f)
                            repository.trackMissionProgress("JUMP_HAZARD", 1)
                            repository.trackWeeklyMissionProgress("JUMP_HAZARD", 1)
                            repository.addGems(1, com.mygames.stickmanrush.security.CurrencySource.PERFECT_BULLSEYE)
                        } else {
                            addFloatingText("HAZARD DODGED! 🥷", stickmanX, stickmanY - 40f, Color(0xFF10B981), scale = 1.15f)
                            repository.addGems(1, com.mygames.stickmanrush.security.CurrencySource.PERFECT_BULLSEYE)
                        }
                    }
                }

                // Check Boss Projectile Collision (Supports Jumping Clearance & Shield Defense)
                _activeBossState.value?.let { boss ->
                    if (!boss.isDefeated) {
                        for (proj in boss.projectiles) {
                            if (physicsEngine.checkBossProjectileCollision(stickmanX, isUpsideDown, proj, jumpOffsetY)) {
                                if (_hasInvincibilityShield.value) {
                                    // 🛡️ Aegis Shield absorbs the boss blast!
                                    _hasInvincibilityShield.value = false
                                    _shieldShatterFx.value = 1f
                                    proj.hasHitPlayer = true
                                    proj.isDodged = true
                                    soundManager.playShieldShatter()
                                    hapticManager?.shieldShatter()
                                    spawnShieldShatterShockwave(stickmanX, stickmanY)
                                    addFloatingText("SHIELD DEFENSE! 🛡️💥", stickmanX, stickmanY - 45f, Color(0xFF38BDF8), scale = 1.4f)
                                } else {
                                    proj.hasHitPlayer = true
                                    fallElapsedTime = 0f
                                    hasSpawnedMidFallReaction = false
                                    soundManager.playStickmanFall()
                                    hapticManager?.gameOver()
                                    spawnDust(stickmanX, stickmanY, count = 16)
                                    stickmanFallVel = 60f
                                    addFloatingText("BOSS BLAST! 🔥", stickmanX, stickmanY - 30f, Color(0xFFEF4444), scale = 1.35f)
                                    _gameState.value = GameState.DROPPING_FAIL
                                    return
                                }
                            } else if (!proj.isDodged && stickmanX > proj.x + 30f) {
                                proj.isDodged = true
                                if (isJumping || jumpOffsetY > 12f) {
                                    addFloatingText("FIREBALL CLEARED! 🔥🦘", stickmanX, stickmanY - 45f, Color(0xFFFBBF24), scale = 1.25f)
                                    repository.trackMissionProgress("JUMP_HAZARD", 1)
                                    repository.addGems(2, com.mygames.stickmanrush.security.CurrencySource.PERFECT_BULLSEYE)
                                } else {
                                    addFloatingText("BLAST DODGED! 🥷", stickmanX, stickmanY - 45f, Color(0xFF38BDF8), scale = 1.15f)
                                }
                            }
                        }
                    }
                }

                // Safe Auto-Flip Assist: If stickman is inverted, the bridge successfully landed, and player reaches destination platform edge
                if (isSuccessfulLanding && isUpsideDown && stickmanX >= (nextPlatform.leftX - 12f)) {
                    isUpsideDown = false
                    soundManager.playFlip()
                    hapticManager?.flip()
                    spawnFlipAcrobaticsEffects(stickmanX, stickmanY)
                    addFloatingText("SAFE FLIP! 🥷", stickmanX, stickmanY - 50f, Color(0xFF38BDF8), scale = 1.15f)
                }

                // Obstacle wall collision check (only fails if bridge was NOT landed safely and stickman walked into obstacle or fell off bridge end)
                if (!isSuccessfulLanding && physicsEngine.checkPlatformWallCollision(stickmanX, isUpsideDown, nextPlatform.leftX)) {
                    fallElapsedTime = 0f
                    hasSpawnedMidFallReaction = false
                    soundManager.playStickmanFall()
                    hapticManager?.gameOver()
                    spawnDust(stickmanX, stickmanY + 30f, count = 14)
                    stickmanFallVel = 50f
                    val funnyQuotes = listOf(
                        "OH NO! OH NO! 😱",
                        "OH NO NO NO NO! 🏃‍♂️💨",
                        "WHOOOPS! 🍌",
                        "AALLL THE WAY DOWNNN! 😱",
                        "WHEEEEEEE! 🪂",
                        "GRAVITY: 1, STICKMAN: 0 💀",
                        "MY ANKLES! 💥",
                        "SEE YA! 🕳️",
                        "I CAN'T FLY! 🦅"
                    )
                    addFloatingText(funnyQuotes.random(), stickmanX, stickmanY - 30f, Color(0xFFFB7185), scale = 1.35f)
                    _gameState.value = GameState.DROPPING_FAIL
                    return
                }

                // Walk destination reached check
                if (isSuccessfulLanding) {
                    if (stickmanX >= targetStickmanWalkX) {
                        stickmanX = targetStickmanWalkX
                        stickmanY = floorY + nextPlatform.heightOffset
                        if (isUpsideDown) {
                            isUpsideDown = false
                        }
                        soundManager.playStickmanLand()
                        val currentTier = difficultyManager.getTier(_score.value)
                        hapticManager?.stickmanLand(
                            tierLevel = currentTier.tierLevel,
                            isElevated = nextPlatform.heightOffset != currentPlatform.heightOffset,
                            isSliding = isSlipping
                        )
                        spawnHeroCrossingLandingEffects(
                            x = stickmanX,
                            y = stickmanY,
                            isBullseye = isPerfectHit,
                            score = _score.value,
                            skinId = repository.selectedSkin.value
                        )

                        // Turn complete
                        val previousLevel = computeLevelForScore(_score.value)
                        _score.value += 1
                        val currentStreak = _score.value
                        repository.trackMissionProgress("REACH_SCORE", _score.value)
                        repository.trackWeeklyMissionProgress("REACH_SCORE", _score.value)
                        repository.trackContestProgress("REACH_SCORE", _score.value)

                        // Environmental Streak Milestones
                        when (currentStreak) {
                            10 -> {
                                addFloatingText("🔥 10 STREAK: NARROW PLATFORMS!", screenWidth / 2f, screenHeight * 0.26f, Color(0xFFF59E0B), scale = 1.35f)
                                soundManager.playPerfectHit()
                                hapticManager?.streakBonus(10)
                            }
                            20 -> {
                                addFloatingText("🌪️ 20 STREAK: MOVING PILLARS!", screenWidth / 2f, screenHeight * 0.26f, Color(0xFFA855F7), scale = 1.4f)
                                soundManager.playPerfectHit()
                                hapticManager?.streakBonus(20)
                            }
                            30 -> {
                                addFloatingText("💨 30 STREAK: WIND DRIFT GALE!", screenWidth / 2f, screenHeight * 0.26f, Color(0xFF38BDF8), scale = 1.45f)
                                soundManager.playPerfectHit()
                                hapticManager?.streakBonus(30)
                            }
                        }

                        val newLevel = computeLevelForScore(_score.value)
                        val updatedHigh = repository.updateHighScore(_score.value)
                        if (updatedHigh && _score.value > 1) {
                            _isNewHighScore.value = true
                            addFloatingText("NEW BEST!", stickmanX, stickmanY - 110f, Color(0xFFFBBF24), scale = 1.4f)
                            spawnConfetti(screenWidth / 2f, screenHeight * 0.4f, count = 35)
                        }

                        // Victory Celebration & Level Progression with Music Fanfare
                        if (newLevel > previousLevel) {
                            _currentLevel.value = newLevel
                            justLeveledUp = true
                            val isMajorMilestone = (newLevel == 5 || newLevel == 10 || newLevel == 15 || newLevel == 20)
                            val isChallengeLevel = (previousLevel % 5 == 0) // Just cleared Level 5, 10, 15, etc.
                            val bonusGems = when {
                                previousLevel == 10 -> 25
                                previousLevel % 5 == 0 -> 20
                                else -> 2
                            }
                            repository.addGems(bonusGems, com.mygames.stickmanrush.security.CurrencySource.LEVEL_MILESTONE)
                            repository.addWoodPlanks(5)
                            addFloatingText("+5 WOOD PLANKS 🪵", screenWidth / 2f, screenHeight * 0.36f, Color(0xFF10B981), scale = 1.2f)
                            repository.saveProgressLevel(newLevel)
                            val title = when (previousLevel) {
                                1 -> "Novice Stickman"
                                2 -> "Starter Stickman"
                                3 -> "Precision Stickman"
                                4 -> "Acrobat Stickman"
                                5 -> "Rookie Stickman"
                                6 -> "Shinobi Stickman"
                                7 -> "Sky Hopper Stickman"
                                8 -> "Canyon Stickman"
                                9 -> "Elite Stickman"
                                10 -> "Expert Stickman"
                                15 -> "Master Stickman"
                                20 -> "Champion Stickman"
                                25 -> "Grandmaster Stickman"
                                30 -> "Legendary Stickman"
                                else -> "Level $previousLevel Stickman"
                            }
                            
                            // Check / Spawn Boss for new level if applicable
                            checkOrSpawnBoss(newLevel)

                            // Pop up challenge motivational victory dialog when clearing level 5, 10, 15, etc.
                            if (isChallengeLevel) {
                                _activeChallengeDialog.value = ChallengeDialogData(
                                    levelNumber = previousLevel,
                                    title = "CONGRATULATIONS! YOU CLEARED LEVEL $previousLevel!",
                                    message = "Incredible precision! You have proven your skills and earned the honorary rank of $title. Keep conquering higher heights!",
                                    type = ChallengeDialogType.POST_LEVEL_VICTORY,
                                    awardedTitle = title,
                                    rewardGems = bonusGems,
                                    buttonText = "CLAIM $title RANK! 👑"
                                )
                            } else if (isMajorMilestone) {
                                val celebrationText = "🎉 LEVEL $previousLevel MILESTONE CLEARED! 🎉\n+$bonusGems Bonus Gems! Advancing to Level $newLevel!"
                                _levelVictoryCelebration.value = celebrationText
                                _activeLevelVictory.value = LevelVictoryData(
                                    levelNumber = previousLevel,
                                    nextLevelNumber = newLevel,
                                    bonusGems = bonusGems,
                                    title = title,
                                    milestoneReward = "+$bonusGems Gems & Title: $title"
                                )
                            }
                            
                            // Trigger challenging provocation before entering Level 5, 10, 15, etc.
                            if (newLevel % 5 == 0) {
                                val bossTitle = when (newLevel) {
                                    5 -> "GOLIAS - THE ROCK TITAN 🗿"
                                    10 -> "IGNIS - INFERNO WYRM 🐉"
                                    15 -> "NEXUS - CYBER GOLEM 🤖"
                                    else -> "MALOK - VOID REAPER ⚔️"
                                }
                                _activeChallengeDialog.value = ChallengeDialogData(
                                    levelNumber = newLevel,
                                    title = "BOSS BATTLE: $bossTitle",
                                    message = "WARNING: Boss detected!\nBuild precise bridges to strike the boss and flip under your bridge to dodge incoming attacks!",
                                    type = ChallengeDialogType.PRE_LEVEL_TAUNT,
                                    rewardGems = bonusGems + 15,
                                    buttonText = "CONFRONT THE BOSS! ⚔️"
                                )
                            }
                            
                            soundManager.playVictoryMusic()
                            hapticManager?.levelUp()
                            spawnBalloonBurst(screenWidth / 2f, screenHeight * 0.35f, balloonCount = if (isMajorMilestone) 5 else 3)
                            spawnConfetti(screenWidth / 2f, screenHeight * 0.35f, count = 55)
                            addFloatingText(
                                "LEVEL $newLevel REACHED! +$bonusGems 💎",
                                screenWidth / 2f,
                                screenHeight * 0.30f,
                                Color(0xFFFFD700),
                                scale = 1.5f
                            )
                        }

                        // Update difficulty tier & stage theme
                        val newTier = difficultyManager.getTier(_score.value)
                        if (newTier.tierLevel != _difficultyTier.value.tierLevel) {
                            _difficultyTier.value = newTier
                            addFloatingText(
                                "RANK UP: ${newTier.title}!",
                                screenWidth / 2f,
                                screenHeight * 0.28f,
                                Color(newTier.badgeColorHex),
                                scale = 1.35f
                            )
                        }

                        val equippedTheme = repository.selectedTheme.value
                        val newStage = StageThemes.getThemeForLevel(newLevel, equippedTheme)
                        if (newStage.stageNumber != _currentStage.value.stageNumber || _currentStage.value.name != newStage.name) {
                            _currentStage.value = newStage
                            addFloatingText(
                                "LEVEL $newLevel: ${newStage.name.uppercase()}",
                                screenWidth / 2f,
                                screenHeight * 0.35f,
                                Color.White,
                                scale = 1.2f
                            )
                        }

                        // Begin camera scrolling
                        scrollTargetX = nextPlatform.leftX - 60f
                        scrollCurrentX = 0f
                        _gameState.value = GameState.SCROLLING
                    }
                } else {
                    // Tip of failed bridge reached
                    if (stickmanX >= targetStickmanWalkX) {
                        stickmanX = targetStickmanWalkX
                        bridgeAngularVel = 0f
                        stickmanFallVel = 0f
                        fallElapsedTime = 0f
                        hasSpawnedMidFallReaction = false

                        // Calculate bridge progress towards next platform gap
                        val gapDist = (nextPlatform.leftX - bridgeStartX).coerceAtLeast(1f)
                        val stickProgress = (stickLength / gapDist).coerceIn(0f, 1.5f)
                        val walkProgress = ((stickmanX - bridgeStartX) / gapDist).coerceIn(0f, 1.5f)
                        val maxProg = maxOf(stickProgress, walkProgress)
                        val progressPercent = (maxProg * 100).toInt().coerceIn(0, 99)

                        pendingSecondChanceEligible = maxProg >= 0.70f && !secondChancePromptUsedThisRun && _revivalsUsed.value == 0
                        pendingSecondChanceProgress = progressPercent

                        soundManager.playStickmanFall()
                        hapticManager?.gameOver()
                        val funnyQuotes = listOf(
                            "OH NO! OH NO! 😱",
                            "OH NO NO NO NO! 🏃‍♂️💨",
                            "WHOOOPS! 🍌",
                            "AALLL THE WAY DOWNNN! 😱",
                            "GRAVITY: 1, STICKMAN: 0 💀",
                            "WHEEEEEEE! 🪂",
                            "MY ANKLES! 💥",
                            "SEE YA! 🕳️",
                            "I CAN'T FLY! 🦅"
                        )
                        addFloatingText(funnyQuotes.random(), stickmanX, stickmanY - 30f, Color(0xFFFB7185), scale = 1.35f)
                        _gameState.value = GameState.DROPPING_FAIL
                    }
                }
            }

            GameState.DROPPING_FAIL -> {
                fallElapsedTime += clampedDt

                if (bridgeAngle < PhysicsEngine.FAIL_DROP_ANGLE) {
                    bridgeAngle += 360f * clampedDt
                    if (bridgeAngle > PhysicsEngine.FAIL_DROP_ANGLE) bridgeAngle = PhysicsEngine.FAIL_DROP_ANGLE
                }

                val fallState = physicsEngine.updateStickmanFall(
                    currentY = stickmanY,
                    currentVelY = stickmanFallVel,
                    currentRot = stickmanRotation,
                    dt = clampedDt
                )
                stickmanY = fallState.y
                stickmanFallVel = fallState.velocityY
                stickmanRotation = fallState.rotation

                // Mid-air comedic floating reaction & panic dust at ~0.65s (voice-over crescendos into oh-no-no-no plunge)
                if (fallElapsedTime >= 0.65f && !hasSpawnedMidFallReaction) {
                    hasSpawnedMidFallReaction = true
                    val midFallQuotes = listOf(
                        "OH NO NO NO NO! 😱",
                        "AALLL THE WAY DOWNNN! 😱",
                        "GRAVITY: 1, STICKMAN: 0 💀",
                        "WHEEEEEEE! 🪂",
                        "WHOOOPS! 🍌",
                        "NOT AGAIN! 😭"
                    )
                    addFloatingText(
                        midFallQuotes.random(),
                        stickmanX,
                        (stickmanY - 30f).coerceAtMost(screenHeight - 140f),
                        Color(0xFFFBBF24),
                        scale = 1.30f
                    )
                    spawnDust(stickmanX, stickmanY.coerceAtMost(screenHeight - 40f), count = 8)
                }

                // Allow the full cartoon sound sequence to play before game over or revive prompt (~2.75s delay)
                if (fallElapsedTime >= 2.75f && stickmanY > screenHeight + 50f) {
                    if (pendingSecondChanceEligible) {
                        pendingSecondChanceEligible = false
                        _secondChanceProgressPercent.value = pendingSecondChanceProgress
                        _activeSecondChancePrompt.value = true
                        _gameState.value = GameState.SECOND_CHANCE_REVIVE
                        soundManager.playButton()
                        hapticManager?.nearMiss()
                    } else {
                        // Deduct 1 life when stickman falls via persistent repository
                        repository.consumeLife()

                        // Authoritative High Score sync to Firestore and local Room DB
                        repository.updateHighScore(_score.value)

                        // Psychology: Provocative challenge failure dialog on fail
                        val lvl = _currentLevel.value
                        _activeChallengeDialog.value = ChallengeDialogData(
                            levelNumber = lvl,
                            title = if (lvl < 5) "CHALLENGE FAILED: YOU CAN'T CLEAR LEVEL 5!" else "CHALLENGE FAILED: LEVEL $lvl!",
                            message = if (lvl < 5) {
                                "I challenged you to clear Level 5 and you fell at Level $lvl! You can't clear it, better try again!"
                            } else {
                                "I told you that you can't clear Level $lvl! Better practice your bridge timing and try again!"
                            },
                            type = ChallengeDialogType.POST_LEVEL_FAIL,
                            rewardGems = 0,
                            buttonText = "TRY AGAIN & PROVE IT! 🔥"
                        )

                        _gameState.value = GameState.GAMEOVER
                    }
                }
            }

            GameState.SCROLLING -> {
                val step = PhysicsEngine.SCROLL_SPEED * clampedDt
                scrollCurrentX += step
                parallaxOffset += step

                // Move all platforms & stickman left
                currentPlatform.leftX -= step
                nextPlatform.leftX -= step
                bridgeStartX -= step
                stickmanX -= step
                nextPlatform.gem?.let { it.x -= step }
                nextPlatform.obstacle?.let { it.x -= step }

                _activeBossState.value?.projectiles?.forEach { it.x -= step }

                if (scrollCurrentX >= scrollTargetX) {
                    val overshoot = scrollCurrentX - scrollTargetX
                    currentPlatform.leftX += overshoot
                    nextPlatform.leftX += overshoot
                    bridgeStartX += overshoot
                    stickmanX += overshoot
                    nextPlatform.gem?.let { it.x += overshoot }
                    nextPlatform.obstacle?.let { it.x += overshoot }

                    currentPlatform = nextPlatform.copy(leftX = 60f)
                    stickmanX = currentPlatform.leftX + currentPlatform.width - 35f
                    stickmanY = floorY + currentPlatform.heightOffset
                    bridgeStartX = currentPlatform.leftX + currentPlatform.width
                    stickLength = 0f
                    bridgeAngle = 0f
                    bridgeLandingSlope = 0f
                    targetBridgeTipX = 0f
                    bridgeAngularVel = 0f
                    bridgeImpactTime = 0f
                    bridgeBounceOffset = 0f
                    bridgeSagOffset = 0f
                    growTickCounter = 0

                    spawnNextPlatform()
                    _gameState.value = GameState.IDLE
                }
            }

            else -> {}
        }
    }

    private fun updateEffects(dt: Float) {
        physicsEngine.updateParticles(particles, dt)

        val tIter = floatingTexts.iterator()
        while (tIter.hasNext()) {
            val t = tIter.next()
            t.y -= 45f * dt
            t.lifeTime -= dt
            t.alpha = (t.lifeTime / 1.0f).coerceIn(0f, 1f)
            if (t.lifeTime <= 0f) {
                tIter.remove()
            }
        }
    }

    private fun addFloatingText(text: String, x: Float, y: Float, color: Color, scale: Float = 1f) {
        floatingTexts.add(
            FloatingPopupText(
                id = ++nextTextId,
                text = text,
                x = x,
                y = y,
                color = color,
                alpha = 1f,
                scale = scale,
                lifeTime = 1.2f
            )
        )
    }

    private fun spawnTipSparks(tipX: Float, tipY: Float, stickSkin: com.mygames.stickmanrush.model.AccessoryItem? = null) {
        val (pColor, shape) = when (stickSkin?.id) {
            "stick_laser" -> Pair(Color(0xFF22D3EE), ParticleShape.SPARKLE)
            "stick_lava" -> Pair(Color(0xFFEA580C), ParticleShape.FIRE_EMBER)
            "stick_dark" -> Pair(Color(0xFFA855F7), ParticleShape.NEON_ORB)
            "stick_rainbow" -> {
                val rainbow = listOf(Color(0xFFEC4899), Color(0xFFFBBF24), Color(0xFF38BDF8), Color(0xFF4ADE80))
                Pair(rainbow[Random.nextInt(rainbow.size)], ParticleShape.STAR)
            }
            "stick_gold" -> Pair(Color(0xFFFFD700), ParticleShape.STAR)
            "stick_cyber" -> Pair(Color(0xFF10B981), ParticleShape.SPARKLE)
            else -> Pair(Color(0xFF67E8F9), ParticleShape.SPARKLE)
        }

        particles.add(
            Particle(
                x = tipX + (Random.nextFloat() * 6f - 3f),
                y = tipY,
                vx = Random.nextFloat() * 50f - 25f,
                vy = -Random.nextFloat() * 60f - 20f,
                color = pColor,
                radius = Random.nextFloat() * 2.5f + 1.5f,
                maxLife = 0.35f,
                life = 0.35f,
                shape = shape,
                rotation = Random.nextFloat() * 360f,
                vRot = Random.nextFloat() * 300f - 150f
            )
        )
    }

    private fun spawnRunningTrail(
        stickmanX: Float,
        stickmanY: Float,
        isUpsideDown: Boolean,
        isSlipping: Boolean,
        skinId: String = "skin_white"
    ) {
        val trailColors = when {
            isSlipping -> listOf(Color(0xFF38BDF8), Color(0xFFE0F2FE), Color(0xFF67E8F9), Color.White)
            isUpsideDown -> listOf(Color(0xFFEC4899), Color(0xFFF472B6), Color(0xFFC084FC), Color.White)
            else -> when {
                skinId.contains("laser") || skinId.contains("neon") -> listOf(Color(0xFF00E5FF), Color(0xFF38BDF8), Color(0xFF67E8F9), Color.White)
                skinId.contains("lava") || skinId.contains("fire") -> listOf(Color(0xFFFF6B00), Color(0xFFEA580C), Color(0xFFFBBF24), Color(0xFFEF4444))
                skinId.contains("dark") || skinId.contains("ninja") || skinId.contains("shadow") -> listOf(Color(0xFFA855F7), Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFFDDD6FE))
                skinId.contains("rainbow") -> listOf(Color(0xFFEC4899), Color(0xFFFBBF24), Color(0xFF38BDF8), Color(0xFF4ADE80))
                skinId.contains("gold") || skinId.contains("king") || skinId.contains("champion") -> listOf(Color(0xFFFFD700), Color(0xFFFDE047), Color(0xFFF59E0B), Color.White)
                skinId.contains("cyber") || skinId.contains("matrix") -> listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF6EE7B7), Color.White)
                else -> listOf(Color(0xFF38BDF8), Color(0xFF60A5FA), Color(0xFF93C5FD), Color.White)
            }
        }

        val offsetY = if (isUpsideDown) -12f else -8f
        val particleShape = when {
            isSlipping -> if (Random.nextBoolean()) ParticleShape.STAR else ParticleShape.GLOW_TRAIL
            isUpsideDown -> if (Random.nextBoolean()) ParticleShape.SPARKLE else ParticleShape.GLOW_TRAIL
            else -> ParticleShape.GLOW_TRAIL
        }

        // Primary glowing speed trail orb
        particles.add(
            Particle(
                x = stickmanX - 12f + (Random.nextFloat() * 6f - 3f),
                y = stickmanY + offsetY + (Random.nextFloat() * 10f - 5f),
                vx = -Random.nextFloat() * 45f - 15f,
                vy = (Random.nextFloat() * 24f - 12f) + (if (isUpsideDown) -18f else 6f),
                color = trailColors[Random.nextInt(trailColors.size)],
                radius = if (isSlipping) Random.nextFloat() * 3.5f + 2.5f else Random.nextFloat() * 3.0f + 1.8f,
                maxLife = 0.32f,
                life = 0.32f,
                shape = particleShape,
                rotation = Random.nextFloat() * 360f,
                vRot = Random.nextFloat() * 240f - 120f
            )
        )

        // Micro sparkle fleck
        if (Random.nextFloat() < 0.45f) {
            particles.add(
                Particle(
                    x = stickmanX - 10f,
                    y = stickmanY + offsetY,
                    vx = -Random.nextFloat() * 55f - 20f,
                    vy = Random.nextFloat() * 30f - 15f,
                    color = Color.White,
                    radius = Random.nextFloat() * 1.8f + 1.0f,
                    maxLife = 0.22f,
                    life = 0.22f,
                    shape = ParticleShape.SPARKLE
                )
            )
        }
    }

    private fun spawnFootstepDust(x: Float, y: Float) {
        particles.add(
            Particle(
                x = x - 6f + Random.nextFloat() * 4f,
                y = y + Random.nextFloat() * 2f,
                vx = -Random.nextFloat() * 25f - 10f,
                vy = -Random.nextFloat() * 15f,
                color = Color.White.copy(alpha = 0.45f),
                radius = Random.nextFloat() * 2.5f + 1.2f,
                maxLife = 0.28f,
                life = 0.28f,
                shape = ParticleShape.DUST
            )
        )
    }

    private fun spawnJumpTakeoffEffects(x: Float, y: Float) {
        spawnDust(x, y, count = 8)
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = Color(0xFF38BDF8),
                radius = 8f,
                maxLife = 0.30f,
                life = 0.30f,
                shape = ParticleShape.RING_WAVE
            )
        )
        for (i in 0 until 6) {
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() * 16f - 8f),
                    y = y,
                    vx = Random.nextFloat() * 60f - 30f,
                    vy = -Random.nextFloat() * 120f - 40f,
                    color = if (i % 2 == 0) Color(0xFF67E8F9) else Color(0xFFFDE047),
                    radius = Random.nextFloat() * 2.5f + 1.5f,
                    maxLife = 0.35f,
                    life = 0.35f,
                    shape = ParticleShape.SPARKLE,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 300f - 150f
                )
            )
        }
    }

    private fun spawnJumpAirborneTrail(x: Float, y: Float) {
        val trailColors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF67E8F9), Color.White)
        particles.add(
            Particle(
                x = x - 12f + (Random.nextFloat() * 4f - 2f),
                y = y + (Random.nextFloat() * 6f - 3f),
                vx = -Random.nextFloat() * 40f - 10f,
                vy = Random.nextFloat() * 20f - 10f,
                color = trailColors[Random.nextInt(trailColors.size)],
                radius = Random.nextFloat() * 3.0f + 1.5f,
                maxLife = 0.25f,
                life = 0.25f,
                shape = ParticleShape.SPARKLE,
                rotation = Random.nextFloat() * 360f,
                vRot = Random.nextFloat() * 360f - 180f
            )
        )
    }

    private fun spawnFlipAcrobaticsEffects(x: Float, y: Float) {
        val flipColors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFC084FC), Color.White)
        for (i in 0 until 10) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 120f + 40f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = flipColors[Random.nextInt(flipColors.size)],
                    radius = Random.nextFloat() * 3.5f + 1.8f,
                    maxLife = 0.35f,
                    life = 0.35f,
                    shape = if (i % 2 == 0) ParticleShape.STAR else ParticleShape.SPARKLE,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 400f - 200f
                )
            )
        }
    }

    private fun spawnDust(x: Float, y: Float, count: Int = 6) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * Math.PI.toFloat()
            val speed = Random.nextFloat() * 90f + 20f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = -kotlin.math.sin(angle) * speed * 0.7f,
                    color = Color.White.copy(alpha = 0.65f),
                    radius = Random.nextFloat() * 4f + 2f,
                    maxLife = 0.45f,
                    life = 0.45f,
                    shape = ParticleShape.DUST
                )
            )
        }
    }

    private fun spawnGemCollectEffects(x: Float, y: Float) {
        val gemColors = listOf(
            Color(0xFF38BDF8),
            Color(0xFF00E5FF),
            Color(0xFF67E8F9),
            Color(0xFFE0F2FE),
            Color(0xFFA855F7),
            Color(0xFFFFD700)
        )

        // Expanding shockwave ripple
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = Color(0xFF38BDF8),
                radius = 8f,
                maxLife = 0.45f,
                life = 0.45f,
                shape = ParticleShape.RING_WAVE
            )
        )

        // Sparkling faceted diamond shards and stars
        for (i in 0 until 28) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 210f + 50f
            val shape = when {
                i % 3 == 0 -> ParticleShape.GEM_BURST
                i % 3 == 1 -> ParticleShape.STAR
                else -> ParticleShape.SPARKLE
            }
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 35f,
                    color = gemColors[Random.nextInt(gemColors.size)],
                    radius = Random.nextFloat() * 4.5f + 2.5f,
                    maxLife = Random.nextFloat() * 0.35f + 0.5f,
                    life = 0.85f,
                    shape = shape,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 500f - 250f
                )
            )
        }
    }

    private fun spawnLandingEffects(x: Float, y: Float, isBullseye: Boolean) {
        spawnDust(x, y, count = if (isBullseye) 20 else 12)

        // Primary Ring Shockwave
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = if (isBullseye) Color(0xFFFFD700) else Color(0xFF38BDF8),
                radius = if (isBullseye) 12f else 6f,
                maxLife = 0.44f,
                life = 0.44f,
                shape = ParticleShape.RING_WAVE
            )
        )

        // Secondary inner shockwave ring
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = if (isBullseye) Color(0xFFEF4444) else Color(0xFFE0F2FE),
                radius = if (isBullseye) 6f else 3f,
                maxLife = 0.32f,
                life = 0.32f,
                shape = ParticleShape.RING_WAVE
            )
        )

        // Brilliant burst of sparks with upward velocity, radial spray, and rotational flare
        val sparkColors = if (isBullseye) {
            listOf(Color(0xFFFFD700), Color(0xFFFDE047), Color(0xFFFF6B00), Color(0xFFEF4444), Color.White)
        } else {
            listOf(Color(0xFFFFD700), Color(0xFFF59E0B), Color(0xFF38BDF8), Color(0xFF67E8F9), Color.White)
        }

        val burstCount = if (isBullseye) 44 else 28
        for (i in 0 until burstCount) {
            // Upward arcing fountain angles (-170 deg to -10 deg)
            val angle = -Math.PI.toFloat() * (Random.nextFloat() * 0.88f + 0.06f)
            val speed = if (isBullseye) Random.nextFloat() * 280f + 80f else Random.nextFloat() * 220f + 60f
            val shape = when {
                i % 4 == 0 -> ParticleShape.SPARKLE
                i % 4 == 1 -> ParticleShape.STAR
                i % 4 == 2 -> ParticleShape.GLOW_TRAIL
                else -> ParticleShape.FIRE_EMBER
            }

            particles.add(
                Particle(
                    x = x + (Random.nextFloat() * 10f - 5f),
                    y = y + (Random.nextFloat() * 6f - 3f),
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - (Random.nextFloat() * 50f + 20f),
                    color = sparkColors[Random.nextInt(sparkColors.size)],
                    radius = if (shape == ParticleShape.STAR || shape == ParticleShape.SPARKLE) {
                        Random.nextFloat() * 4.2f + 2.0f
                    } else {
                        Random.nextFloat() * 3.0f + 1.5f
                    },
                    maxLife = Random.nextFloat() * 0.35f + 0.45f,
                    life = 0.8f,
                    shape = shape,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 500f - 250f
                )
            )
        }
    }

    private fun spawnBalloonBurst(centerX: Float, centerY: Float, balloonCount: Int = 3) {
        val balloonThemes = listOf(
            Pair(Color(0xFFEF4444), listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFFB91C1C))), // Red
            Pair(Color(0xFF3B82F6), listOf(Color(0xFF93C5FD), Color(0xFF3B82F6), Color(0xFF1D4ED8))), // Blue
            Pair(Color(0xFF10B981), listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF047857))), // Emerald
            Pair(Color(0xFFF59E0B), listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706))), // Gold
            Pair(Color(0xFFA855F7), listOf(Color(0xFFE9D5FF), Color(0xFFA855F7), Color(0xFF6B21A8))), // Purple
            Pair(Color(0xFFEC4899), listOf(Color(0xFFFBCFE8), Color(0xFFEC4899), Color(0xFFBE185D)))  // Hot Pink
        )

        for (b in 0 until balloonCount) {
            val offsetX = (b - (balloonCount - 1) / 2f) * 110f + (Random.nextFloat() * 30f - 15f)
            val offsetY = (Random.nextFloat() * 50f - 25f)
            val bx = centerX + offsetX
            val by = centerY + offsetY
            val theme = balloonThemes[Random.nextInt(balloonThemes.size)]

            // Shockwave pop ring
            particles.add(
                Particle(
                    x = bx,
                    y = by,
                    vx = 0f,
                    vy = 0f,
                    color = theme.first,
                    radius = 12f,
                    maxLife = 0.5f,
                    life = 0.5f,
                    shape = ParticleShape.RING_WAVE
                )
            )

            // Rubber balloon shards bursting outwards
            for (i in 0 until 18) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 260f + 90f
                particles.add(
                    Particle(
                        x = bx,
                        y = by,
                        vx = kotlin.math.cos(angle) * speed,
                        vy = kotlin.math.sin(angle) * speed - 60f,
                        color = theme.second[Random.nextInt(theme.second.size)],
                        radius = Random.nextFloat() * 6f + 3.5f,
                        maxLife = Random.nextFloat() * 0.4f + 0.9f,
                        life = 1.3f,
                        shape = ParticleShape.BALLOON_POP,
                        rotation = Random.nextFloat() * 360f,
                        vRot = Random.nextFloat() * 600f - 300f
                    )
                )
            }

            // Confetti and streamer ribbons fluttering from inside balloon
            for (i in 0 until 12) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 200f + 60f
                particles.add(
                    Particle(
                        x = bx,
                        y = by,
                        vx = kotlin.math.cos(angle) * speed,
                        vy = kotlin.math.sin(angle) * speed - 110f,
                        color = theme.second[Random.nextInt(theme.second.size)],
                        radius = Random.nextFloat() * 5f + 3f,
                        maxLife = 1.5f,
                        life = 1.5f,
                        shape = if (i % 2 == 0) ParticleShape.RIBBON else ParticleShape.CONFETTI,
                        rotation = Random.nextFloat() * 360f,
                        vRot = Random.nextFloat() * 500f - 250f
                    )
                )
            }

            // Sparkles & Stars
            for (i in 0 until 8) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 180f + 40f
                particles.add(
                    Particle(
                        x = bx,
                        y = by,
                        vx = kotlin.math.cos(angle) * speed,
                        vy = kotlin.math.sin(angle) * speed - 40f,
                        color = Color.White,
                        radius = Random.nextFloat() * 3.5f + 2f,
                        maxLife = 0.7f,
                        life = 0.7f,
                        shape = ParticleShape.STAR,
                        rotation = Random.nextFloat() * 360f,
                        vRot = Random.nextFloat() * 400f - 200f
                    )
                )
            }
        }
    }

    private fun spawnPowerUpBurst(x: Float, y: Float, color: Color) {
        // Shockwave expansion ring
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = color,
                radius = 14f,
                maxLife = 0.42f,
                life = 0.42f,
                shape = ParticleShape.RING_WAVE
            )
        )
        // Particle burst
        val burstColors = listOf(color, Color.White, Color(0xFFFFD700), Color(0xFF67E8F9))
        for (i in 0 until 18) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 200f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 50f,
                    color = burstColors[Random.nextInt(burstColors.size)],
                    radius = Random.nextFloat() * 3.5f + 2.0f,
                    maxLife = 0.45f,
                    life = 0.45f,
                    shape = if (i % 2 == 0) ParticleShape.STAR else ParticleShape.SPARKLE,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 360f - 180f
                )
            )
        }
    }

    private fun spawnShieldShatterShockwave(x: Float, y: Float) {
        // High-energy shield shatter shockwave
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = Color(0xFF38BDF8),
                radius = 24f,
                maxLife = 0.45f,
                life = 0.45f,
                shape = ParticleShape.RING_WAVE
            )
        )
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = Color(0xFF818CF8),
                radius = 12f,
                maxLife = 0.35f,
                life = 0.35f,
                shape = ParticleShape.RING_WAVE
            )
        )
        val shatterColors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFE0F2FE), Color.White, Color(0xFFFFD700))
        for (i in 0 until 24) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 260f + 80f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = shatterColors[Random.nextInt(shatterColors.size)],
                    radius = Random.nextFloat() * 4.0f + 2.5f,
                    maxLife = 0.50f,
                    life = 0.50f,
                    shape = ParticleShape.STAR,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 480f - 240f
                )
            )
        }
    }

    private fun spawnMagnetAttractSparks(gemX: Float, gemY: Float, stickmanX: Float, stickmanY: Float) {
        val t = Random.nextFloat()
        val sparkX = gemX + t * (stickmanX - gemX) + (Random.nextFloat() * 12f - 6f)
        val sparkY = gemY + t * (stickmanY - gemY) + (Random.nextFloat() * 12f - 6f)
        particles.add(
            Particle(
                x = sparkX,
                y = sparkY,
                vx = (stickmanX - gemX) * 0.4f + (Random.nextFloat() * 30f - 15f),
                vy = (Random.nextFloat() * 40f - 20f),
                color = if (Random.nextBoolean()) Color(0xFF38BDF8) else Color(0xFFFDE047),
                radius = Random.nextFloat() * 2.8f + 1.2f,
                maxLife = 0.22f,
                life = 0.22f,
                shape = ParticleShape.SPARKLE,
                rotation = Random.nextFloat() * 360f,
                vRot = Random.nextFloat() * 300f - 150f
            )
        )
    }

    private fun spawnConfetti(x: Float, y: Float, count: Int = 35) {
        val colors = listOf(
            Color(0xFFFFD700),
            Color(0xFFEF4444),
            Color(0xFF38BDF8),
            Color(0xFF4ADE80),
            Color(0xFFA855F7),
            Color(0xFFF43F5E),
            Color(0xFFFDE047)
        )
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 280f + 70f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 140f,
                    color = colors[Random.nextInt(colors.size)],
                    radius = Random.nextFloat() * 5.5f + 3f,
                    maxLife = 1.6f,
                    life = 1.6f,
                    shape = ParticleShape.CONFETTI,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 500f - 250f
                )
            )
        }
    }

    private fun spawnHeroGemCollectAura(heroX: Float, heroY: Float, isDoubler: Boolean, comboMultiplier: Int) {
        val heroCenterY = heroY - 18f
        val auraColors = if (isDoubler || comboMultiplier >= 3) {
            listOf(Color(0xFFFFD700), Color(0xFFFDE047), Color(0xFF38BDF8), Color(0xFF00E5FF), Color(0xFFEC4899), Color.White)
        } else {
            listOf(Color(0xFF38BDF8), Color(0xFF00E5FF), Color(0xFF67E8F9), Color(0xFFE0F2FE), Color.White)
        }

        // Torso shockwave pulse ring
        particles.add(
            Particle(
                x = heroX,
                y = heroCenterY,
                vx = 0f,
                vy = 0f,
                color = if (isDoubler) Color(0xFFFFD700) else Color(0xFF38BDF8),
                radius = 10f,
                maxLife = 0.38f,
                life = 0.38f,
                shape = ParticleShape.RING_WAVE
            )
        )

        // Radiant upward burst of sparkles, diamond stars, and glowing gem shards
        val particleCount = if (isDoubler || comboMultiplier >= 3) 22 else 14
        for (i in 0 until particleCount) {
            val angle = -Math.PI.toFloat() * (Random.nextFloat() * 0.90f + 0.05f) // Upward fountain arc
            val speed = Random.nextFloat() * 160f + 50f
            val shape = when {
                i % 3 == 0 -> ParticleShape.GEM_BURST
                i % 3 == 1 -> ParticleShape.STAR
                else -> ParticleShape.SPARKLE
            }
            particles.add(
                Particle(
                    x = heroX + (Random.nextFloat() * 12f - 6f),
                    y = heroCenterY + (Random.nextFloat() * 10f - 5f),
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - (Random.nextFloat() * 60f + 30f),
                    color = auraColors[Random.nextInt(auraColors.size)],
                    radius = Random.nextFloat() * 3.8f + 1.8f,
                    maxLife = Random.nextFloat() * 0.3f + 0.45f,
                    life = 0.75f,
                    shape = shape,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 450f - 225f
                )
            )
        }

        // Ascending neon orbs
        for (i in 0 until 4) {
            particles.add(
                Particle(
                    x = heroX + (Random.nextFloat() * 16f - 8f),
                    y = heroCenterY + (Random.nextFloat() * 8f - 4f),
                    vx = Random.nextFloat() * 20f - 10f,
                    vy = -Random.nextFloat() * 70f - 30f,
                    color = if (isDoubler) Color(0xFFFFD700) else Color(0xFF38BDF8),
                    radius = Random.nextFloat() * 2.5f + 1.2f,
                    maxLife = 0.45f,
                    life = 0.45f,
                    shape = ParticleShape.NEON_ORB
                )
            )
        }
    }

    private fun spawnHeroCrossingLandingEffects(
        x: Float,
        y: Float,
        isBullseye: Boolean,
        score: Int,
        skinId: String
    ) {
        // 1. Bilateral footstep dust plumes
        spawnDust(x - 8f, y, count = 6)
        spawnDust(x + 8f, y, count = 6)

        // 2. Ground impact expanding shockwaves at stickman's feet
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = if (isBullseye) Color(0xFFFFD700) else Color(0xFF38BDF8),
                radius = if (isBullseye) 14f else 8f,
                maxLife = 0.42f,
                life = 0.42f,
                shape = ParticleShape.RING_WAVE
            )
        )

        // 3. Themed skin victory burst colors
        val themeColors = when {
            skinId.contains("laser") || skinId.contains("neon") -> listOf(Color(0xFF00E5FF), Color(0xFF38BDF8), Color(0xFF67E8F9), Color.White)
            skinId.contains("lava") || skinId.contains("fire") -> listOf(Color(0xFFFF6B00), Color(0xFFEA580C), Color(0xFFFBBF24), Color(0xFFEF4444))
            skinId.contains("dark") || skinId.contains("ninja") || skinId.contains("shadow") -> listOf(Color(0xFFA855F7), Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFFDDD6FE))
            skinId.contains("rainbow") -> listOf(Color(0xFFEC4899), Color(0xFFFBBF24), Color(0xFF38BDF8), Color(0xFF4ADE80))
            skinId.contains("gold") || skinId.contains("king") || skinId.contains("champion") -> listOf(Color(0xFFFFD700), Color(0xFFFDE047), Color(0xFFF59E0B), Color.White)
            skinId.contains("cyber") || skinId.contains("matrix") -> listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF6EE7B7), Color.White)
            else -> listOf(Color(0xFFFFD700), Color(0xFF38BDF8), Color(0xFF4ADE80), Color(0xFFFBBF24), Color.White)
        }

        // 4. Upward victory fountain burst of stars, sparkles, and ribbons around hero
        val burstCount = if (isBullseye) 28 else 18
        for (i in 0 until burstCount) {
            val angle = -Math.PI.toFloat() * (Random.nextFloat() * 0.85f + 0.08f)
            val speed = Random.nextFloat() * 200f + 60f
            val shape = when {
                i % 4 == 0 -> ParticleShape.STAR
                i % 4 == 1 -> ParticleShape.SPARKLE
                i % 4 == 2 -> ParticleShape.RIBBON
                else -> ParticleShape.CONFETTI
            }
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() * 10f - 5f),
                    y = y - 10f + (Random.nextFloat() * 8f - 4f),
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - (Random.nextFloat() * 80f + 30f),
                    color = themeColors[Random.nextInt(themeColors.size)],
                    radius = Random.nextFloat() * 4.2f + 2.0f,
                    maxLife = Random.nextFloat() * 0.4f + 0.55f,
                    life = 0.95f,
                    shape = shape,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 480f - 240f
                )
            )
        }
    }

    fun dismissVictoryCelebration() {
        _levelVictoryCelebration.value = null
    }
}
