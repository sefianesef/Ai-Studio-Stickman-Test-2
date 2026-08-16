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

    private val _currentStage = MutableStateFlow(StageThemes.stages[0])
    val currentStage: StateFlow<StageTheme> = _currentStage.asStateFlow()

    private val _difficultyTier = MutableStateFlow(DifficultyTier.APPRENTICE)
    val difficultyTier: StateFlow<DifficultyTier> = _difficultyTier.asStateFlow()

    private val _isNewHighScore = MutableStateFlow(false)
    val isNewHighScore: StateFlow<Boolean> = _isNewHighScore.asStateFlow()

    private val _lastNearMiss = MutableStateFlow<NearMissInfo?>(null)
    val lastNearMiss: StateFlow<NearMissInfo?> = _lastNearMiss.asStateFlow()

    // Level milestone & victory celebration state
    private val _levelVictoryCelebration = MutableStateFlow<String?>(null)
    val levelVictoryCelebration: StateFlow<String?> = _levelVictoryCelebration.asStateFlow()

    val gemsCollectedRun: StateFlow<Int> = gemStateManager.collectedInRun
    val gemCombo: StateFlow<Int> = gemStateManager.currentCombo

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

    fun resetGame(initial: Boolean = false) {
        _score.value = 0
        _isNewHighScore.value = false
        _difficultyTier.value = DifficultyTier.APPRENTICE
        val equippedTheme = repository.selectedTheme.value
        _currentStage.value = StageThemes.getThemeForScore(0, equippedTheme)
        _levelVictoryCelebration.value = null
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
        isUpsideDown = false
        walkPhase = 0f
        parallaxOffset = 0f

        particles.clear()
        floatingTexts.clear()

        _gameState.value = if (initial) GameState.START else GameState.IDLE
    }

    fun startGame() {
        soundManager.playButton()
        hapticManager?.uiClick()
        resetGame(initial = false)
        _gameState.value = GameState.IDLE
    }

    /**
     * Loss Aversion / Revive Mechanic: Revives stickman right back on the platform with full score preserved.
     */
    fun reviveRun() {
        soundManager.playPerfectHit()
        hapticManager?.perfectHit()

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

        val baseMinGap = tier.minGap
        val maxAvailableGap = (screenW * 0.48f).coerceIn(tier.minGap + 40f, tier.maxGap)
        val gap = Random.nextFloat() * (maxAvailableGap - baseMinGap) + baseMinGap

        val width = difficultyManager.generatePlatformWidth(currentScore).coerceIn(36f, 160f)
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
                }
                spawnDust(stickmanX, if (isUpsideDown) floorY + 30f else floorY, count = 6)
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
            } else {
                // Finger released too quickly; reset back to IDLE
                stickLength = 0f
                _gameState.value = GameState.IDLE
            }
        }
    }

    // Main Game Update Loop (deltaTime in seconds)
    fun update(dt: Float) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Update active particles & floating texts
        updateEffects(clampedDt)

        when (_gameState.value) {
            GameState.GROWING -> {
                // Growth speed accelerates slightly over time
                val speed = 340f + (stickLength * 0.45f)
                val prevLength = stickLength
                stickLength += speed * clampedDt

                // Sound & haptic ticks
                if ((stickLength / 22f).toInt() > (prevLength / 22f).toInt()) {
                    growTickCounter++
                    soundManager.playGrowTick(growTickCounter)
                    hapticManager?.tick()

                    // Small upward spark at the growing tip
                    if (particles.size < 60) {
                        particles.add(
                            Particle(
                                x = bridgeStartX + (Random.nextFloat() * 6f - 3f),
                                y = floorY - stickLength,
                                vx = Random.nextFloat() * 40f - 20f,
                                vy = -Random.nextFloat() * 50f - 20f,
                                color = Color(0xFF67E8F9),
                                radius = 2f,
                                maxLife = 0.25f,
                                life = 0.25f,
                                shape = ParticleShape.STAR
                            )
                        )
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
                    soundManager.playBridgePlaced()
                    hapticManager?.bridgePlaced()

                    // Evaluate landing with PhysicsEngine collision geometry and difficulty tolerance
                    val tier = difficultyManager.getTier(_score.value)
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
                            hapticManager?.perfectHit()
                            repository.recordPerfectHit()
                            repository.trackMissionProgress("PERFECT_HITS", 1)
                            addFloatingText(
                                "PERFECT! +2",
                                landingResult.platformCenter,
                                floorY - 90f,
                                Color(0xFFFFD700),
                                scale = 1.3f
                            )
                            _score.value += 1 // Bonus +1 for bullseye
                            repository.addGems(1)
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

                // Footstep sound
                if (sin(walkPhase) > 0.95f) {
                    soundManager.playWalkStep()
                }

                // Check Gem Pickup along the bridge using PhysicsEngine & GemStateManager
                nextPlatform.gem?.let { gem ->
                    if (physicsEngine.checkGemPickup(stickmanX, isUpsideDown, gem)) {
                        gem.collected = true
                        val event = gemStateManager.onGemCollected(gem, floorY)
                        soundManager.playGemCollect()
                        hapticManager?.gemCollect()
                        repository.trackMissionProgress("COLLECT_GEMS", event.amount)

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
                    spawnDust(stickmanX, floorY, count = 8)
                    addFloatingText("SAFE FLIP! 🥷", stickmanX, floorY - 50f, Color(0xFF38BDF8), scale = 1.15f)
                }

                // Obstacle wall collision check (only fails if bridge was NOT landed safely and stickman walked into obstacle or fell off bridge end)
                if (!isSuccessfulLanding && physicsEngine.checkPlatformWallCollision(stickmanX, isUpsideDown, nextPlatform.leftX)) {
                    soundManager.playGameOver()
                    hapticManager?.gameOver()
                    spawnDust(stickmanX, floorY + 30f, count = 12)
                    stickmanFallVel = 50f
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
                        val previousLevel = (_score.value / 5) + 1
                        _score.value += 1
                        val newLevel = (_score.value / 5) + 1
                        val updatedHigh = repository.updateHighScore(_score.value)
                        if (updatedHigh && _score.value > 1) {
                            _isNewHighScore.value = true
                            addFloatingText("NEW BEST!", stickmanX, floorY - 110f, Color(0xFFFBBF24), scale = 1.4f)
                            spawnConfetti(screenWidth / 2f, screenHeight * 0.4f, count = 30)
                        }

                        // Victory Celebration & Level Progression
                        if (newLevel > previousLevel) {
                            val celebrationText = "🎉 LEVEL $previousLevel COMPLETE! 🎉\nCongratulations! You are going to Level $newLevel!"
                            _levelVictoryCelebration.value = celebrationText
                            soundManager.playPerfectHit()
                            hapticManager?.levelUp()
                            spawnConfetti(screenWidth / 2f, screenHeight * 0.35f, count = 45)
                            addFloatingText(
                                "VICTORY! NEXT LEVEL!",
                                screenWidth / 2f,
                                screenHeight * 0.30f,
                                Color(0xFFFFD700),
                                scale = 1.45f
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
                        soundManager.playGameOver()
                        hapticManager?.gameOver()
                        _gameState.value = GameState.DROPPING_FAIL
                    }
                }
            }

            GameState.DROPPING_FAIL -> {
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

                if (stickmanY > screenHeight + 100f) {
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

    private fun spawnDust(x: Float, y: Float, count: Int = 6) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * Math.PI.toFloat()
            val speed = Random.nextFloat() * 80f + 20f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = -kotlin.math.sin(angle) * speed * 0.6f,
                    color = Color.White.copy(alpha = 0.6f),
                    radius = Random.nextFloat() * 3.5f + 2f,
                    maxLife = 0.5f,
                    life = 0.5f,
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

        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = Color(0xFF38BDF8),
                radius = 6f,
                maxLife = 0.45f,
                life = 0.45f,
                shape = ParticleShape.RING_WAVE
            )
        )

        for (i in 0 until 24) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 180f + 40f
            val shape = if (i % 2 == 0) ParticleShape.GEM_BURST else ParticleShape.STAR
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 30f,
                    color = gemColors[Random.nextInt(gemColors.size)],
                    radius = Random.nextFloat() * 4.5f + 2.5f,
                    maxLife = Random.nextFloat() * 0.4f + 0.5f,
                    life = 0.8f,
                    shape = shape
                )
            )
        }
    }

    private fun spawnLandingEffects(x: Float, y: Float, isBullseye: Boolean) {
        spawnDust(x, y, count = if (isBullseye) 16 else 10)

        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 0f,
                vy = 0f,
                color = if (isBullseye) Color(0xFFFFD700) else Color(0xFF60A5FA),
                radius = if (isBullseye) 8f else 5f,
                maxLife = 0.4f,
                life = 0.4f,
                shape = ParticleShape.RING_WAVE
            )
        )

        val burstCount = if (isBullseye) 28 else 14
        val burstColor = if (isBullseye) Color(0xFFFFD700) else Color(0xFF93C5FD)
        for (i in 0 until burstCount) {
            val angle = -Math.PI.toFloat() * (Random.nextFloat() * 0.8f + 0.1f)
            val speed = Random.nextFloat() * 200f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    color = if (isBullseye && i % 2 == 0) Color(0xFFEF4444) else burstColor,
                    radius = Random.nextFloat() * 3.5f + 2f,
                    maxLife = Random.nextFloat() * 0.3f + 0.45f,
                    life = 0.75f,
                    shape = if (isBullseye) ParticleShape.STAR else ParticleShape.CIRCLE
                )
            )
        }
    }

    private fun spawnConfetti(x: Float, y: Float, count: Int = 30) {
        val colors = listOf(
            Color(0xFFFFD700),
            Color(0xFFEF4444),
            Color(0xFF38BDF8),
            Color(0xFF4ADE80),
            Color(0xFFA855F7),
            Color(0xFFF43F5E)
        )
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 260f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed - 120f,
                    color = colors[Random.nextInt(colors.size)],
                    radius = Random.nextFloat() * 5f + 3f,
                    maxLife = 1.4f,
                    life = 1.4f,
                    shape = ParticleShape.CONFETTI
                )
            )
        }
    }

    fun dismissVictoryCelebration() {
        _levelVictoryCelebration.value = null
    }
}
