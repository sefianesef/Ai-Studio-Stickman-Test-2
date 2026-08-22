package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.HapticManager
import com.example.audio.SoundManager
import com.example.data.GameRepository
import com.example.game.physics.PhysicsEngine
import com.example.model.*
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

    fun dismissLevelVictory() {
        _activeLevelVictory.value = null
    }

    fun dismissChallengeDialog() {
        _activeChallengeDialog.value = null
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
        val targetStartLevel = startLevel.coerceAtLeast(1)
        val initialScore = ((targetStartLevel - 1) * 3).coerceAtLeast(0)
        _score.value = initialScore
        _currentLevel.value = targetStartLevel
        _isNewHighScore.value = false
        _revivalsUsed.value = 0
        justLeveledUp = false
        _difficultyTier.value = difficultyManager.getTier(initialScore)
        val equippedTheme = repository.selectedTheme.value
        _currentStage.value = StageThemes.getThemeForScore(initialScore, equippedTheme)
        _levelVictoryCelebration.value = null
        _activeLevelVictory.value = null
        gemStateManager.resetRun()

        currentPlatform = PlatformData(id = 1L, leftX = 60f, width = 160f)
        spawnNextPlatform()

        bridgeStartX = currentPlatform.leftX + currentPlatform.width
        stickLength = 0f
        bridgeAngle = 0f
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
        walkPhase = 0f
        parallaxOffset = 0f

        particles.clear()
        floatingTexts.clear()

        if (!initial) {
            repository.recordGamePlayed()
        }

        _gameState.value = if (initial) GameState.START else GameState.IDLE
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
        soundManager.playPerfectHit()
        hapticManager?.perfectHit()
        _revivalsUsed.value += 1

        // Reset bridge & stickman to start of current platform
        stickLength = 0f
        bridgeAngle = 0f
        bridgeAngularVel = 0f
        bridgeImpactTime = 0f
        bridgeBounceOffset = 0f
        bridgeSagOffset = 0f
        growTickCounter = 0

        stickmanX = currentPlatform.leftX + currentPlatform.width - 40f
        stickmanY = floorY
        stickmanFallVel = 0f
        stickmanRotation = 0f
        isUpsideDown = false
        walkPhase = 0f

        // Golden revival shockwave & celebratory particles
        spawnConfetti(stickmanX, floorY - 60f, count = 25)
        addFloatingText("SECOND CHANCE! ✨", stickmanX, floorY - 100f, Color(0xFFFFD700), scale = 1.4f)

        _gameState.value = GameState.IDLE
    }

    /**
     * Procedurally generates the next platform using DifficultyManager.
     */
    fun generateNextPlatform(currentScore: Int, screenW: Float): PlatformData {
        val tier = difficultyManager.getTier(currentScore)
        _difficultyTier.value = tier

        val level = computeLevelForScore(currentScore)
        val gap = difficultyManager.generatePlatformGap(currentScore, level, screenW, isFirstBridgeOfLevel = justLeveledUp)
        justLeveledUp = false

        val width = difficultyManager.generatePlatformWidth(currentScore).coerceIn(35f, 160f)
        val nextLeft = currentPlatform.leftX + currentPlatform.width + gap

        // Procedural Gem Placement via GemStateManager
        val spanStart = currentPlatform.leftX + currentPlatform.width
        val spanEnd = nextLeft
        val gem = gemStateManager.createGemForSpan(spanStart, spanEnd, tier)

        return PlatformData(
            id = nextPlatform.id + 1,
            leftX = nextLeft,
            width = width,
            hasRedDot = true,
            gem = gem
        )
    }

    private fun spawnNextPlatform() {
        nextPlatform = generateNextPlatform(_score.value, screenWidth)
    }

    // Touch input handlers
    fun onTouchDown() {
        when (_gameState.value) {
            GameState.START, GameState.IDLE -> {
                soundManager.playButton()
                hapticManager?.uiClick()
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
                // Instant responsive tap to toggle flip upside-down / right-side up
                isUpsideDown = !isUpsideDown
                soundManager.playFlip()
                hapticManager?.flip()
                if (isUpsideDown) {
                    repository.trackMissionProgress("FLIP_WALK", 1)
                    repository.trackWeeklyMissionProgress("FLIP_WALK", 1)
                }
                spawnFlipAcrobaticsEffects(stickmanX, if (isUpsideDown) floorY + 30f else floorY)
            }
            else -> {}
        }
    }

    fun onTouchUp() {
        if (_gameState.value == GameState.GROWING) {
            if (stickLength > 10f) {
                _gameState.value = GameState.FALLING_BRIDGE
                bridgeAngularVel = 0f
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

        when (_gameState.value) {
            GameState.GROWING -> {
                // Growth speed scales gently with current difficulty tier for easy early control
                val tier = difficultyManager.getTier(_score.value)
                val speed = (310f + (stickLength * 0.40f)) * tier.growthSpeedFactor
                val prevLength = stickLength
                stickLength += speed * clampedDt

                // Sound & haptic ticks
                if ((stickLength / 22f).toInt() > (prevLength / 22f).toInt()) {
                    growTickCounter++
                    soundManager.playGrowTick(growTickCounter)
                    hapticManager?.tick(tier.tierLevel)

                    // Themed sparks at the growing bridge tip
                    if (particles.size < 80) {
                        val stickSkin = repository.availableAccessories.find { it.id == repository.selectedStick.value }
                        spawnTipSparks(bridgeStartX, floorY - stickLength, stickSkin)
                    }
                }
            }

            GameState.FALLING_BRIDGE -> {
                // Physics-based gravitational angular acceleration
                val torque = physicsEngine.computeBridgeAngularAcceleration(bridgeAngle)
                bridgeAngularVel += torque * clampedDt
                bridgeAngle += bridgeAngularVel * clampedDt

                if (bridgeAngle >= PhysicsEngine.MAX_BRIDGE_ANGLE) {
                    bridgeAngle = PhysicsEngine.MAX_BRIDGE_ANGLE
                    bridgeAngularVel = 0f
                    bridgeImpactTime = 0f

                    // Evaluate landing with PhysicsEngine collision geometry and difficulty tolerance
                    val tier = difficultyManager.getTier(_score.value)
                    soundManager.playBridgePlaced()
                    hapticManager?.bridgePlaced(tier.tierLevel)

                    val landingResult = physicsEngine.evaluateBridgeLanding(
                        bridgeStartX = bridgeStartX,
                        stickLength = stickLength,
                        targetPlatform = nextPlatform,
                        bullseyeTolerance = tier.bullseyeTolerance
                    )

                    isSuccessfulLanding = landingResult.isSuccessful
                    isPerfectHit = landingResult.isBullseye
                    targetStickmanWalkX = landingResult.targetWalkX
                    _lastNearMiss.value = landingResult.nearMiss

                    if (landingResult.isSuccessful) {
                        spawnLandingEffects(landingResult.bridgeTipX, floorY, landingResult.isBullseye)

                        if (landingResult.isBullseye) {
                            soundManager.playPerfectHit()
                            hapticManager?.perfectHit(tier.tierLevel)
                            repository.recordPerfectHit()
                            repository.trackMissionProgress("PERFECT_HITS", 1)
                            repository.trackWeeklyMissionProgress("PERFECT_HITS", 1)
                            repository.trackContestProgress("PERFECT_HITS", 1)
                            addFloatingText(
                                "PERFECT! +2",
                                landingResult.platformCenter,
                                floorY - 90f,
                                Color(0xFFFFD700),
                                scale = 1.3f
                            )
                            _score.value += 1 // Bonus +1 for bullseye
                            repository.addGems(1)
                            repository.recordGemsHarvested(1)
                        }

                        _gameState.value = GameState.WALKING
                    } else {
                        landingResult.nearMiss?.let { nearMiss ->
                            hapticManager?.nearMiss()
                            addFloatingText(
                                nearMiss.message,
                                landingResult.bridgeTipX,
                                floorY - 80f,
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

                val stepX = PhysicsEngine.WALK_SPEED * clampedDt
                stickmanX += stepX
                parallaxOffset += stepX
                walkPhase += clampedDt * 16f

                // Bridge sag calculation when walking on span
                val spanWidth = (targetStickmanWalkX - bridgeStartX).coerceAtLeast(10f)
                val walkProgress = ((stickmanX - bridgeStartX) / spanWidth).coerceIn(0f, 1f)
                bridgeSagOffset = physicsEngine.computeBridgeSag(walkProgress, stickLength)

                // Footstep sound & subtle dust puff
                if (sin(walkPhase) > 0.95f) {
                    soundManager.playWalkStep()
                    if (particles.size < 70) {
                        spawnFootstepDust(stickmanX, if (isUpsideDown) floorY - 20f else floorY)
                    }
                }

                // Check Gem Pickup along the bridge using PhysicsEngine & GemStateManager
                nextPlatform.gem?.let { gem ->
                    if (physicsEngine.checkGemPickup(stickmanX, isUpsideDown, gem)) {
                        gem.collected = true
                        val event = gemStateManager.onGemCollected(gem, floorY)
                        soundManager.playGemCollect()
                        hapticManager?.gemCollect()
                        repository.trackMissionProgress("COLLECT_GEMS", event.amount)
                        repository.trackWeeklyMissionProgress("COLLECT_GEMS", event.amount)
                        repository.trackContestProgress("COLLECT_GEMS", event.amount)
                        repository.recordGemsHarvested(event.amount)

                        val comboLabel = if (event.comboMultiplier > 1) "💎 +${event.amount} (${event.comboMultiplier}x COMBO!)" else "💎 +${event.amount}"
                        addFloatingText(comboLabel, event.x, event.y - 30f, if (event.comboMultiplier > 1) Color(0xFFFFD700) else Color(0xFF38BDF8), scale = if (event.comboMultiplier > 1) 1.25f else 1.0f)
                        spawnGemCollectEffects(event.x, event.y)
                    }
                }

                // Safe Auto-Flip Assist: If stickman is inverted, the bridge successfully landed, and player reaches destination platform edge
                if (isSuccessfulLanding && isUpsideDown && stickmanX >= (nextPlatform.leftX - 12f)) {
                    isUpsideDown = false
                    soundManager.playFlip()
                    hapticManager?.flip()
                    spawnFlipAcrobaticsEffects(stickmanX, floorY)
                    addFloatingText("SAFE FLIP! 🥷", stickmanX, floorY - 50f, Color(0xFF38BDF8), scale = 1.15f)
                }

                // Obstacle wall collision check (only fails if bridge was NOT landed safely and stickman walked into obstacle or fell off bridge end)
                if (!isSuccessfulLanding && physicsEngine.checkPlatformWallCollision(stickmanX, isUpsideDown, nextPlatform.leftX)) {
                    fallElapsedTime = 0f
                    hasSpawnedMidFallReaction = false
                    soundManager.playStickmanFall()
                    hapticManager?.gameOver()
                    spawnDust(stickmanX, floorY + 30f, count = 14)
                    stickmanFallVel = 50f
                    val funnyQuotes = listOf(
                        "WHOOOPS! 🍌",
                        "AALLL THE WAY DOWNNN! 😱",
                        "WHEEEEEEE! 🪂",
                        "GRAVITY: 1, STICKMAN: 0 💀",
                        "MY ANKLES! 💥",
                        "SEE YA! 🕳️",
                        "I CAN'T FLY! 🦅"
                    )
                    addFloatingText(funnyQuotes.random(), stickmanX, floorY - 30f, Color(0xFFFB7185), scale = 1.25f)
                    _gameState.value = GameState.DROPPING_FAIL
                    return
                }

                // Walk destination reached check
                if (isSuccessfulLanding) {
                    if (stickmanX >= targetStickmanWalkX) {
                        stickmanX = targetStickmanWalkX
                        if (isUpsideDown) {
                            isUpsideDown = false
                        }
                        soundManager.playStickmanLand()

                        // Turn complete
                        val previousLevel = computeLevelForScore(_score.value)
                        _score.value += 1
                        repository.trackMissionProgress("REACH_SCORE", _score.value)
                        repository.trackWeeklyMissionProgress("REACH_SCORE", _score.value)
                        repository.trackContestProgress("REACH_SCORE", _score.value)
                        val newLevel = computeLevelForScore(_score.value)
                        val updatedHigh = repository.updateHighScore(_score.value)
                        if (updatedHigh && _score.value > 1) {
                            _isNewHighScore.value = true
                            addFloatingText("NEW BEST!", stickmanX, floorY - 110f, Color(0xFFFBBF24), scale = 1.4f)
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
                            repository.addGems(bonusGems)
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
                                _activeChallengeDialog.value = ChallengeDialogData(
                                    levelNumber = newLevel,
                                    title = "STICKMAN BOSS CHALLENGE: LEVEL $newLevel",
                                    message = "I challenge you: You cannot clear Level $newLevel!\nThe canyon winds are fierce and the bridges require supreme mastery. Prove me wrong!",
                                    type = ChallengeDialogType.PRE_LEVEL_TAUNT,
                                    rewardGems = bonusGems + 10,
                                    buttonText = "I ACCEPT THE CHALLENGE! ⚔️"
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
                        val newStage = StageThemes.getThemeForScore(_score.value, equippedTheme)
                        if (newStage.stageNumber != _currentStage.value.stageNumber || _currentStage.value.name != newStage.name) {
                            _currentStage.value = newStage
                            addFloatingText(
                                "STAGE ${newStage.stageNumber}: ${newStage.name.uppercase()}",
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
                        soundManager.playStickmanFall()
                        hapticManager?.gameOver()
                        val funnyQuotes = listOf(
                            "WHOOOPS! 🍌",
                            "AALLL THE WAY DOWNNN! 😱",
                            "GRAVITY: 1, STICKMAN: 0 💀",
                            "WHEEEEEEE! 🪂",
                            "MY ANKLES! 💥",
                            "SEE YA! 🕳️",
                            "I CAN'T FLY! 🦅"
                        )
                        addFloatingText(funnyQuotes.random(), stickmanX, floorY - 30f, Color(0xFFFB7185), scale = 1.30f)
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

                // Mid-air comedic floating reaction & panic dust at ~0.65s (slide whistle descending into trombone)
                if (fallElapsedTime >= 0.65f && !hasSpawnedMidFallReaction) {
                    hasSpawnedMidFallReaction = true
                    val midFallQuotes = listOf(
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
                        scale = 1.25f
                    )
                    spawnDust(stickmanX, stickmanY.coerceAtMost(screenHeight - 40f), count = 8)
                }

                // Allow the full cartoon sound sequence (slide whistle + sad trombone + spring boing) to play before game over
                if (fallElapsedTime >= 2.10f && stickmanY > screenHeight + 50f) {
                    // Deduct 1 life when stickman falls via persistent repository
                    repository.consumeLife()

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

                if (scrollCurrentX >= scrollTargetX) {
                    val overshoot = scrollCurrentX - scrollTargetX
                    currentPlatform.leftX += overshoot
                    nextPlatform.leftX += overshoot
                    bridgeStartX += overshoot
                    stickmanX += overshoot
                    nextPlatform.gem?.let { it.x += overshoot }

                    currentPlatform = nextPlatform.copy(leftX = 60f)
                    stickmanX = currentPlatform.leftX + currentPlatform.width - 35f
                    bridgeStartX = currentPlatform.leftX + currentPlatform.width
                    stickLength = 0f
                    bridgeAngle = 0f
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

    private fun spawnTipSparks(tipX: Float, tipY: Float, stickSkin: com.example.model.AccessoryItem?) {
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
        spawnDust(x, y, count = if (isBullseye) 18 else 10)

        // Ring shockwave
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = if (isBullseye) Color(0xFFFFD700) else Color(0xFF60A5FA),
                radius = if (isBullseye) 10f else 5f,
                maxLife = 0.42f,
                life = 0.42f,
                shape = ParticleShape.RING_WAVE
            )
        )

        if (isBullseye) {
            // Secondary Crimson shockwave ring
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = 0f,
                    vy = 0f,
                    color = Color(0xFFEF4444),
                    radius = 5f,
                    maxLife = 0.32f,
                    life = 0.32f,
                    shape = ParticleShape.RING_WAVE
                )
            )
        }

        val burstCount = if (isBullseye) 36 else 16
        val burstColor = if (isBullseye) Color(0xFFFFD700) else Color(0xFF93C5FD)
        for (i in 0 until burstCount) {
            val angle = -Math.PI.toFloat() * (Random.nextFloat() * 0.85f + 0.08f)
            val speed = Random.nextFloat() * 240f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = if (isBullseye && i % 2 == 0) Color(0xFFEF4444) else burstColor,
                    radius = Random.nextFloat() * 4.5f + 2f,
                    maxLife = Random.nextFloat() * 0.35f + 0.45f,
                    life = 0.8f,
                    shape = if (isBullseye) (if (i % 3 == 0) ParticleShape.STAR else ParticleShape.SPARKLE) else ParticleShape.CIRCLE,
                    rotation = Random.nextFloat() * 360f,
                    vRot = Random.nextFloat() * 450f - 225f
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

    fun dismissVictoryCelebration() {
        _levelVictoryCelebration.value = null
    }
}
