package com.mygames.stickmanrush.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mygames.stickmanrush.game.StickmanGameEngine
import com.mygames.stickmanrush.game.StageThemes
import com.mygames.stickmanrush.game.physics.GameLoop
import com.mygames.stickmanrush.model.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StickmanGameCanvas(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val engine = viewModel.engine
    val gameState by engine.gameState.collectAsState()
    val currentStage by engine.currentStage.collectAsState()
    val equippedHat = viewModel.getEquippedHat()
    val equippedScarf = viewModel.getEquippedScarf()
    val equippedStick = viewModel.getEquippedStick()
    val equippedSkin = viewModel.getEquippedSkin()

    var gameTimeSeconds by remember { mutableFloatStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()

    val gameLoop = remember {
        GameLoop(
            onTick = { dt ->
                if (!viewModel.isShopOpen.value && !viewModel.isPauseMenuOpen.value) {
                    engine.update(dt)
                    gameTimeSeconds += dt
                }
            }
        )
    }

    // High frequency game loop driven by Compose frame clock
    LaunchedEffect(gameLoop) {
        while (true) {
            withFrameNanos { frameNanos ->
                gameLoop.onFrame(frameNanos)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("stickman_game_canvas")
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (viewModel.isPauseMenuOpen.value || viewModel.isShopOpen.value ||
                            viewModel.isDailyRewardOpen.value || viewModel.isHowToPlayOpen.value
                        ) {
                            continue
                        }

                        engine.onTouchDown()

                        // Track pointer until finger is released or touch is cancelled
                        val pointerId = down.id
                        var isPressed = true
                        while (isPressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                isPressed = false
                            }
                        }

                        engine.onTouchUp()
                    }
                }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        LaunchedEffect(widthPx, heightPx) {
            engine.setScreenDimensions(widthPx, heightPx)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Draw dynamic background sky & multi-layer parallax environment
            drawGameBackground(engine, currentStage, gameTimeSeconds)

            // 2. Draw platforms
            drawPlatforms(engine, currentStage)

            // 3. Draw gems
            drawGems(engine, gameTimeSeconds)

            // 4. Draw Bridge / Stick
            drawBridge(engine, equippedStick)

            // 5. Draw Stickman Hero with cosmetics
            drawHeroStickman(
                engine = engine,
                hat = equippedHat,
                scarf = equippedScarf,
                skin = equippedSkin,
                gameTime = gameTimeSeconds
            )

            // 6. Draw particles
            drawParticles(engine.particles)

            // 7. Draw floating popups
            drawFloatingTexts(engine.floatingTexts, textMeasurer)

            // 8. Draw prompt hint when waiting for player to stretch bridge
            if (engine.gameState.value == GameState.IDLE) {
                drawIdleHoldPrompt(engine, textMeasurer, gameTimeSeconds)
            }

            // 9. Draw prompt hint when walking inverted under the bridge
            if (engine.gameState.value == GameState.WALKING && engine.isUpsideDown) {
                drawInvertedFlipPrompt(engine, textMeasurer, gameTimeSeconds)
            }
        }
    }
}

private fun DrawScope.drawGameBackground(engine: StickmanGameEngine, stage: StageTheme, time: Float) {
    val screenW = size.width
    val screenH = size.height
    val parallax = engine.parallaxOffset

    // 1. Base Sky Gradient
    val skyBrush = Brush.verticalGradient(
        colors = listOf(stage.bgTopColor, stage.bgBottomColor),
        startY = 0f,
        endY = screenH
    )
    drawRect(brush = skyBrush)

    // 2. Distant Starfield & Atmospheric Specks (Subtlest Parallax: 0.02x)
    val starParallax = (parallax * 0.02f)
    for (i in 0 until 35) {
        val seedX = (i * 137.5f) % screenW
        val starX = (seedX - starParallax).mod(screenW)
        val starY = ((i * 73.3f) % (screenH * 0.45f)) + 15f
        val radius = if (i % 5 == 0) 2.2f else if (i % 2 == 0) 1.5f else 1.0f
        val alpha = (0.35f + 0.45f * sin((time * 2.5f) + i.toFloat())).coerceIn(0.2f, 0.9f)
        
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius.dp.toPx(),
            center = Offset(starX, starY)
        )
    }

    // 3. Celestial Body / Sky Phenomenon (Parallax: 0.04x)
    val celestialWrapW = screenW + 200f
    val baseCelestialX = (screenW * 0.82f - parallax * 0.04f).mod(celestialWrapW) - 50f
    val celestialY = screenH * 0.20f

    when (stage.celestialType) {
        CelestialType.MOON -> {
            // Glowing Moon with Crater Depth
            drawCircle(
                color = Color(0x25FFFFFF),
                radius = 48.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = 34.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            // Moon crater shadow
            drawCircle(
                color = stage.bgTopColor.copy(alpha = 0.88f),
                radius = 28.dp.toPx(),
                center = Offset(baseCelestialX - 10.dp.toPx(), celestialY - 6.dp.toPx())
            )
        }
        CelestialType.SUN -> {
            // Radiant Golden Sun
            drawCircle(
                color = Color(0x33F59E0B),
                radius = 62.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = 38.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
        }
        CelestialType.NEON_PLANET -> {
            // Ringed Sci-Fi Planet
            drawCircle(
                color = Color(0x33A855F7),
                radius = 50.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFFC084FC),
                radius = 32.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawOval(
                color = Color(0xFF22D3EE),
                topLeft = Offset(baseCelestialX - 52.dp.toPx(), celestialY - 9.dp.toPx()),
                size = Size(104.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        CelestialType.AURORA -> {
            // Dynamic Waving Aurora Borealis
            val auroraPath = Path()
            val startY = screenH * 0.16f
            auroraPath.moveTo(0f, startY)
            for (x in 0..screenW.toInt() step 30) {
                val y = startY + sin((x * 0.012f) + time * 1.6f - (parallax * 0.002f)) * 26.dp.toPx()
                auroraPath.lineTo(x.toFloat(), y)
            }
            auroraPath.lineTo(screenW, 0f)
            auroraPath.lineTo(0f, 0f)
            auroraPath.close()

            drawPath(
                path = auroraPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0x00000000), Color(0x4034D399), Color(0x00000000)),
                    startY = 0f,
                    endY = screenH * 0.36f
                )
            )
        }
        CelestialType.SAKURA_BLOOM -> {
            // Floating Sakura Blossoms & Ethereal Pink Moon
            drawCircle(
                color = Color(0x40F472B6),
                radius = 54.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFFFBCFE8),
                radius = 36.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            // Floating Sakura Petals
            for (p in 0 until 18) {
                val petX = (p * 55f + sin(time * 1.8f + p) * 30f - parallax * 0.08f).mod(screenW)
                val petY = (p * 40f + time * 28f + cos(time + p) * 15f).mod(screenH * 0.7f)
                drawOval(
                    color = Color(0xCCF472B6),
                    topLeft = Offset(petX, petY),
                    size = Size(10.dp.toPx(), 5.dp.toPx())
                )
            }
        }
        CelestialType.DEEP_ABYSS -> {
            // Glowing Bioluminescent Deep Sea Moon & Floating Orbs
            drawCircle(
                color = Color(0x3306B6D4),
                radius = 52.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFF22D3EE),
                radius = 32.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            for (j in 0 until 15) {
                val jX = (j * 70f + cos(time * 1.2f + j) * 20f - parallax * 0.06f).mod(screenW)
                val jY = (screenH * 0.7f - (j * 35f + time * 20f).mod(screenH * 0.6f))
                val jAlpha = (0.3f + 0.4f * sin(time * 2f + j)).coerceIn(0.1f, 0.8f)
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = jAlpha),
                    radius = (4 + (j % 4)).dp.toPx(),
                    center = Offset(jX, jY)
                )
            }
        }
        CelestialType.MATRIX_CASCADE -> {
            // Cyber Matrix Digital Grid Lines
            drawCircle(
                color = Color(0x3310B981),
                radius = 48.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFF34D399),
                radius = 28.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            for (col in 0 until 16) {
                val colX = (col * 28.dp.toPx() - parallax * 0.05f).mod(screenW)
                val dropY = ((time * 120f + col * 45f) % (screenH * 0.6f))
                drawLine(
                    color = Color(0x8810B981),
                    start = Offset(colX, dropY),
                    end = Offset(colX, dropY + 25.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        CelestialType.CELESTIAL_SHRINE -> {
            // High Star Shrines & Prismatic Blue Moon
            drawCircle(
                color = Color(0x4460A5FA),
                radius = 60.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFF93C5FD),
                radius = 36.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawOval(
                color = Color(0xAA38BDF8),
                topLeft = Offset(baseCelestialX - 58.dp.toPx(), celestialY - 8.dp.toPx()),
                size = Size(116.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
        CelestialType.DRAGON_EMBER -> {
            // Blood-Red Dragon Moon & Floating Lava Embers
            drawCircle(
                color = Color(0x44EF4444),
                radius = 58.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFFF87171),
                radius = 36.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            for (e in 0 until 20) {
                val embX = (e * 60f + sin(time * 2f + e) * 25f - parallax * 0.08f).mod(screenW)
                val embY = (screenH * 0.7f - (e * 30f + time * 35f).mod(screenH * 0.55f))
                val embAlpha = (0.4f + 0.5f * sin(time * 3f + e)).coerceIn(0.2f, 0.9f)
                drawCircle(
                    color = Color(0xFFF97316).copy(alpha = embAlpha),
                    radius = (2 + (e % 3)).dp.toPx(),
                    center = Offset(embX, embY)
                )
            }
        }
        CelestialType.CRYSTAL_PRISM -> {
            // Prismatic Grandmaster Cosmic Crystals
            drawCircle(
                color = Color(0x44EC4899),
                radius = 62.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            drawCircle(
                color = Color(0xFFF472B6),
                radius = 38.dp.toPx(),
                center = Offset(baseCelestialX, celestialY)
            )
            for (c in 0 until 12) {
                val cX = (c * 80f + cos(time * 1.5f + c) * 35f - parallax * 0.06f).mod(screenW)
                val cY = ((c * 50f + time * 18f) % (screenH * 0.5f)) + 30f
                drawCircle(
                    color = if (c % 2 == 0) Color(0xFFC084FC) else Color(0xFF38BDF8),
                    radius = (3 + (c % 3)).dp.toPx(),
                    center = Offset(cX, cY)
                )
            }
        }
    }

    // 4. Distant Parallax Mountain Range (Slow Speed: 0.10x)
    val farPeriod = 500.dp.toPx()
    val farShift = (parallax * 0.10f).mod(farPeriod)
    val farMountainPath = Path()
    val farBaseY = screenH * 0.62f
    farMountainPath.moveTo(-farPeriod, screenH)

    var currentX = -farPeriod - farShift
    while (currentX <= screenW + farPeriod) {
        val peak1X = currentX + farPeriod * 0.28f
        val peak1Y = farBaseY - 90.dp.toPx()
        val valleyX = currentX + farPeriod * 0.55f
        val valleyY = farBaseY - 30.dp.toPx()
        val peak2X = currentX + farPeriod * 0.82f
        val peak2Y = farBaseY - 110.dp.toPx()
        val nextX = currentX + farPeriod
        val nextY = farBaseY - 45.dp.toPx()

        farMountainPath.lineTo(currentX, farBaseY - 45.dp.toPx())
        farMountainPath.lineTo(peak1X, peak1Y)
        farMountainPath.lineTo(valleyX, valleyY)
        farMountainPath.lineTo(peak2X, peak2Y)
        farMountainPath.lineTo(nextX, nextY)

        currentX += farPeriod
    }
    farMountainPath.lineTo(screenW + farPeriod, screenH)
    farMountainPath.close()

    drawPath(
        path = farMountainPath,
        color = stage.mountainColor.copy(alpha = 0.40f)
    )

    // 5. Midground Mountain Ridge & Plateaus (Medium Speed: 0.26x)
    val midPeriod = 420.dp.toPx()
    val midShift = (parallax * 0.26f).mod(midPeriod)
    val midRidgePath = Path()
    val midBaseY = screenH * 0.66f
    midRidgePath.moveTo(-midPeriod, screenH)

    var midX = -midPeriod - midShift
    while (midX <= screenW + midPeriod) {
        val midPeakX = midX + midPeriod * 0.45f
        val midPeakY = midBaseY - 70.dp.toPx()
        val midSaddleX = midX + midPeriod * 0.75f
        val midSaddleY = midBaseY - 25.dp.toPx()
        val nextMidX = midX + midPeriod

        midRidgePath.lineTo(midX, midBaseY - 35.dp.toPx())
        midRidgePath.lineTo(midPeakX, midPeakY)
        midRidgePath.lineTo(midSaddleX, midSaddleY)
        midRidgePath.lineTo(nextMidX, midBaseY - 35.dp.toPx())

        midX += midPeriod
    }
    midRidgePath.lineTo(screenW + midPeriod, screenH)
    midRidgePath.close()

    drawPath(
        path = midRidgePath,
        color = stage.mountainColor.copy(alpha = 0.70f)
    )

    // 6. Near Foothills & Atmospheric Scenery (Fast Speed: 0.50x)
    val nearPeriod = 320.dp.toPx()
    val nearShift = (parallax * 0.50f).mod(nearPeriod)
    val nearRidgePath = Path()
    val nearBaseY = screenH * 0.70f
    nearRidgePath.moveTo(-nearPeriod, screenH)

    var nearX = -nearPeriod - nearShift
    while (nearX <= screenW + nearPeriod) {
        val nearPeakX = nearX + nearPeriod * 0.35f
        val nearPeakY = nearBaseY - 40.dp.toPx()
        val nearValleyX = nearX + nearPeriod * 0.70f
        val nearValleyY = nearBaseY - 10.dp.toPx()
        val nextNearX = nearX + nearPeriod

        nearRidgePath.lineTo(nearX, nearBaseY - 15.dp.toPx())
        nearRidgePath.lineTo(nearPeakX, nearPeakY)
        nearRidgePath.lineTo(nearValleyX, nearValleyY)
        nearRidgePath.lineTo(nextNearX, nearBaseY - 15.dp.toPx())

        nearX += nearPeriod
    }
    nearRidgePath.lineTo(screenW + nearPeriod, screenH)
    nearRidgePath.close()

    drawPath(
        path = nearRidgePath,
        color = stage.mountainColor.copy(alpha = 0.95f)
    )

    // 7. Ambient Drifting Clouds & Mist Layers (Wind + Parallax)
    val cloudCount = 4
    for (c in 0 until cloudCount) {
        val cloudSpeed = 16f + c * 8f
        val cloudParallaxSpeed = 0.15f + c * 0.10f
        val cloudWidth = (120 + c * 40).dp.toPx()
        val cloudX = (-(time * cloudSpeed + parallax * cloudParallaxSpeed) + (c * screenW * 0.35f)).mod(screenW + cloudWidth * 2) - cloudWidth
        val cloudY = screenH * (0.28f + c * 0.09f)

        // Draw soft cloud puff cluster
        val cloudColor = Color.White.copy(alpha = 0.08f + c * 0.03f)
        drawCircle(
            color = cloudColor,
            radius = 24.dp.toPx(),
            center = Offset(cloudX + cloudWidth * 0.3f, cloudY)
        )
        drawCircle(
            color = cloudColor,
            radius = 34.dp.toPx(),
            center = Offset(cloudX + cloudWidth * 0.5f, cloudY - 8.dp.toPx())
        )
        drawCircle(
            color = cloudColor,
            radius = 22.dp.toPx(),
            center = Offset(cloudX + cloudWidth * 0.7f, cloudY + 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawPlatforms(engine: StickmanGameEngine, stage: StageTheme) {
    val floorY = engine.floorY
    val platColor = stage.platformColor
    val highlightColor = stage.platformHighlightColor

    // Current Platform
    val p1 = engine.currentPlatform
    if (p1.leftX + p1.width > 0f) {
        drawRoundRect(
            color = platColor,
            topLeft = Offset(p1.leftX, floorY),
            size = Size(p1.width, size.height - floorY + 100f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Top highlight line
        drawLine(
            color = highlightColor,
            start = Offset(p1.leftX, floorY),
            end = Offset(p1.leftX + p1.width, floorY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Next Platform
    val p2 = engine.nextPlatform
    if (p2.leftX < size.width) {
        drawRoundRect(
            color = platColor,
            topLeft = Offset(p2.leftX, floorY),
            size = Size(p2.width, size.height - floorY + 100f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Top highlight line
        drawLine(
            color = highlightColor,
            start = Offset(p2.leftX, floorY),
            end = Offset(p2.leftX + p2.width, floorY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center Red Bullseye Dot (Bonus point marker)
        if (p2.hasRedDot) {
            val centerDotX = p2.leftX + (p2.width / 2f)
            drawRoundRect(
                color = Color(0xFFEF4444),
                topLeft = Offset(centerDotX - 5.dp.toPx(), floorY - 1.dp.toPx()),
                size = Size(10.dp.toPx(), 5.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            // Glowing core
            drawCircle(
                color = Color(0xFFFEE2E2),
                radius = 2.dp.toPx(),
                center = Offset(centerDotX, floorY + 1.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawGems(engine: StickmanGameEngine, time: Float) {
    val gem = engine.nextPlatform.gem ?: return
    if (gem.collected) return

    val bobbing = sin(time * 4f) * 4.dp.toPx()
    val gemY = if (gem.isUnderBridge) {
        engine.floorY + 28.dp.toPx() + bobbing
    } else {
        engine.floorY - 24.dp.toPx() + bobbing
    }

    // Draw diamond gem
    val gemSize = 10.dp.toPx()
    val gemPath = Path().apply {
        moveTo(gem.x, gemY - gemSize)
        lineTo(gem.x + gemSize * 0.8f, gemY)
        lineTo(gem.x, gemY + gemSize)
        lineTo(gem.x - gemSize * 0.8f, gemY)
        close()
    }

    // Gem glow
    drawCircle(
        color = Color(0x5538BDF8),
        radius = gemSize * 1.5f,
        center = Offset(gem.x, gemY)
    )

    // Diamond body
    drawPath(
        path = gemPath,
        color = Color(0xFF38BDF8)
    )

    // Inner facet highlight
    val facetPath = Path().apply {
        moveTo(gem.x, gemY - gemSize * 0.7f)
        lineTo(gem.x + gemSize * 0.4f, gemY)
        lineTo(gem.x, gemY + gemSize * 0.5f)
        close()
    }
    drawPath(path = facetPath, color = Color(0xFFBAE6FD))
}

private fun DrawScope.drawBridge(engine: StickmanGameEngine, stickSkin: AccessoryItem) {
    if (engine.stickLength <= 0f) return

    val startX = engine.bridgeStartX
    val startY = engine.floorY
    val angle = engine.bridgeAngle + engine.bridgeBounceOffset

    rotate(degrees = angle, pivot = Offset(startX, startY)) {
        // Pointing straight up by default (angle = 0), so it goes from (startX, startY) to (startX, startY - length)
        val endX = startX
        val endY = startY - engine.stickLength

        val primaryColor = Color(stickSkin.primaryColor)
        val secondaryColor = Color(stickSkin.secondaryColor)

        when (stickSkin.id) {
            "stick_laser" -> {
                // Neon glow layer
                drawLine(
                    color = primaryColor.copy(alpha = 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Core beam
                drawLine(
                    color = Color.White,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            "stick_dark" -> {
                // Pulsing antimatter purple void beam
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFF3E8FF),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            "stick_lava" -> {
                // Molten magma girder with glowing core
                drawLine(
                    color = primaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            "stick_rainbow" -> {
                // Shifting spectrum beam
                drawLine(
                    color = Color(0xFFEC4899),
                    start = Offset(startX - 1.5.dp.toPx(), startY),
                    end = Offset(endX - 1.5.dp.toPx(), endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFBBF24),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset(startX + 1.5.dp.toPx(), startY),
                    end = Offset(endX + 1.5.dp.toPx(), endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            "stick_cyber" -> {
                // Emerald cyber data stream
                drawLine(
                    color = primaryColor.copy(alpha = 0.45f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Square
                )
            }
            "stick_candy" -> {
                // Base red
                drawLine(
                    color = primaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // White stripes
                val segmentCount = (engine.stickLength / 16.dp.toPx()).toInt()
                for (i in 0 until segmentCount step 2) {
                    val segStartY = startY - (i * 16.dp.toPx())
                    val segEndY = (segStartY - 8.dp.toPx()).coerceAtLeast(endY)
                    drawLine(
                        color = secondaryColor,
                        start = Offset(startX, segStartY),
                        end = Offset(endX, segEndY),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Butt
                    )
                }
            }
            "stick_gold" -> {
                // Golden rod with shimmering tip
                drawLine(
                    color = secondaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(endX, endY)
                )
            }
            else -> {
                // Classic wood / staff
                drawLine(
                    color = primaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Glowing energetic charge at the tip while holding to extend
        if (engine.gameState.value == GameState.GROWING) {
            val glowRadius = (4.dp.toPx() + sin(engine.stickLength * 0.15f) * 1.5.dp.toPx()).coerceAtLeast(2.dp.toPx())
            drawCircle(
                color = Color(0xFF60A5FA).copy(alpha = 0.5f),
                radius = glowRadius * 2f,
                center = Offset(endX, endY)
            )
            drawCircle(
                color = Color.White,
                radius = glowRadius,
                center = Offset(endX, endY)
            )
        }
    }
}

private fun DrawScope.drawHeroStickman(
    engine: StickmanGameEngine,
    hat: AccessoryItem,
    scarf: AccessoryItem,
    skin: AccessoryItem,
    gameTime: Float
) {
    val x = engine.stickmanX
    val y = engine.stickmanY + (if (engine.gameState.value == GameState.WALKING) engine.bridgeSagOffset else 0f)
    val isUpsideDown = engine.isUpsideDown
    val walkPhase = engine.walkPhase
    val rotation = engine.stickmanRotation
    val isWalking = engine.gameState.value == GameState.WALKING

    val bodyColor = Color(skin.primaryColor)
    val hatColor = Color(hat.primaryColor)
    val hatSecondary = Color(hat.secondaryColor)
    val scarfColor = Color(scarf.primaryColor)

    // Base pivot is at stickman feet
    rotate(degrees = rotation + (if (isUpsideDown) 180f else 0f), pivot = Offset(x, y)) {
        val headRadius = 7.dp.toPx()
        val headCenterY = y - 28.dp.toPx()
        val neckY = y - 21.dp.toPx()
        val hipY = y - 11.dp.toPx()

        // 2. Head & Torso Bob calculation
        val bob = if (isWalking) -abs(sin(walkPhase)) * 2.dp.toPx() else 0f
        val currentHeadY = headCenterY + bob
        val currentNeckY = neckY + bob
        val currentHipY = hipY + bob * 0.7f

        // 1. Scarf / Cape (Drawn behind body)
        drawHeroCape(scarf, scarfColor, x, currentNeckY, isWalking, walkPhase, gameTime)

        // 2. Spine / Torso (with slight forward lean during walking)
        val torsoLean = if (isWalking) 1.5.dp.toPx() else 0f
        drawLine(
            color = bodyColor,
            start = Offset(x + torsoLean, currentNeckY),
            end = Offset(x, currentHipY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 3. Legs: 2-Segment Articulated Kinematics (Hip -> Knee -> Foot)
        if (isWalking) {
            // Left Leg (Lead Phase)
            val leftLegPhase = walkPhase
            val leftFootX = x + sin(leftLegPhase) * 8.dp.toPx()
            val leftFootY = y - (cos(leftLegPhase).coerceAtLeast(0f) * 4.dp.toPx())
            val leftKneeX = (x + leftFootX) / 2f + (if (sin(leftLegPhase) > 0) 2.5.dp.toPx() else -1.dp.toPx())
            val leftKneeY = (currentHipY + leftFootY) / 2f - 1.5.dp.toPx()

            // Thigh
            drawLine(
                color = bodyColor,
                start = Offset(x, currentHipY),
                end = Offset(leftKneeX, leftKneeY),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Shin & Foot
            drawLine(
                color = bodyColor,
                start = Offset(leftKneeX, leftKneeY),
                end = Offset(leftFootX, leftFootY),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Right Leg (Opposing Phase: + PI)
            val rightLegPhase = walkPhase + PI.toFloat()
            val rightFootX = x + sin(rightLegPhase) * 8.dp.toPx()
            val rightFootY = y - (cos(rightLegPhase).coerceAtLeast(0f) * 4.dp.toPx())
            val rightKneeX = (x + rightFootX) / 2f + (if (sin(rightLegPhase) > 0) 2.5.dp.toPx() else -1.dp.toPx())
            val rightKneeY = (currentHipY + rightFootY) / 2f - 1.5.dp.toPx()

            // Thigh
            drawLine(
                color = bodyColor,
                start = Offset(x, currentHipY),
                end = Offset(rightKneeX, rightKneeY),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Shin & Foot
            drawLine(
                color = bodyColor,
                start = Offset(rightKneeX, rightKneeY),
                end = Offset(rightFootX, rightFootY),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            // Standing Idle Legs
            drawLine(
                color = bodyColor,
                start = Offset(x, currentHipY),
                end = Offset(x - 3.5.dp.toPx(), y),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = bodyColor,
                start = Offset(x, currentHipY),
                end = Offset(x + 3.5.dp.toPx(), y),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 4. Arms: 2-Segment Articulated Kinematics (Shoulder -> Elbow -> Hand)
        if (isWalking) {
            val shoulderY = currentNeckY + 2.dp.toPx()
            val shoulderX = x + torsoLean

            // Left Arm (Swings opposite to left leg)
            val leftArmPhase = walkPhase + PI.toFloat()
            val leftHandX = shoulderX + sin(leftArmPhase) * 7.dp.toPx()
            val leftHandY = shoulderY + 8.dp.toPx() - (abs(sin(leftArmPhase)) * 2.5.dp.toPx())
            val leftElbowX = (shoulderX + leftHandX) / 2f + 1.5.dp.toPx()
            val leftElbowY = (shoulderY + leftHandY) / 2f - 1.dp.toPx()

            drawLine(
                color = bodyColor,
                start = Offset(shoulderX, shoulderY),
                end = Offset(leftElbowX, leftElbowY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = bodyColor,
                start = Offset(leftElbowX, leftElbowY),
                end = Offset(leftHandX, leftHandY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Right Arm (Swings with left leg)
            val rightArmPhase = walkPhase
            val rightHandX = shoulderX + sin(rightArmPhase) * 7.dp.toPx()
            val rightHandY = shoulderY + 8.dp.toPx() - (abs(sin(rightArmPhase)) * 2.5.dp.toPx())
            val rightElbowX = (shoulderX + rightHandX) / 2f + 1.5.dp.toPx()
            val rightElbowY = (shoulderY + rightHandY) / 2f - 1.dp.toPx()

            drawLine(
                color = bodyColor,
                start = Offset(shoulderX, shoulderY),
                end = Offset(rightElbowX, rightElbowY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = bodyColor,
                start = Offset(rightElbowX, rightElbowY),
                end = Offset(rightHandX, rightHandY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            // Standing Idle Arms
            val shoulderY = currentNeckY + 2.dp.toPx()
            drawLine(
                color = bodyColor,
                start = Offset(x, shoulderY),
                end = Offset(x + 5.dp.toPx(), shoulderY + 8.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = bodyColor,
                start = Offset(x, shoulderY),
                end = Offset(x - 5.dp.toPx(), shoulderY + 8.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 5. Head
        drawCircle(
            color = bodyColor,
            radius = headRadius,
            center = Offset(x + torsoLean, currentHeadY)
        )

        // Eye / Visor
        if (skin.id == "skin_shadow") {
            // White glowing shinobi eyes
            drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = Offset(x + torsoLean + 3.dp.toPx(), currentHeadY - 1.dp.toPx()))
        } else {
            // Cute dark eye facing right
            drawCircle(color = Color(0xFF0F172A), radius = 1.2.dp.toPx(), center = Offset(x + torsoLean + 3.dp.toPx(), currentHeadY - 1.dp.toPx()))
        }

        // 6. Hat / Headgear
        drawHeroHat(hat, hatColor, hatSecondary, x + torsoLean, currentHeadY, headRadius)

        // 7. Scarf Knot around neck
        drawCircle(
            color = scarfColor,
            radius = 3.dp.toPx(),
            center = Offset(x + torsoLean - 1.dp.toPx(), currentNeckY)
        )
    }
}

private fun DrawScope.drawHeroCape(
    scarf: AccessoryItem,
    color: Color,
    x: Float,
    neckY: Float,
    isWalking: Boolean,
    walkPhase: Float,
    time: Float
) {
    val flutter = sin((if (isWalking) walkPhase * 2f else time * 6f)) * 3.dp.toPx()
    val capeLength = 16.dp.toPx()

    val capePath = Path().apply {
        moveTo(x - 2.dp.toPx(), neckY)
        quadraticTo(
            x - 10.dp.toPx(), neckY + 6.dp.toPx() + flutter,
            x - 14.dp.toPx(), neckY + capeLength + flutter
        )
        lineTo(x - 6.dp.toPx(), neckY + capeLength - 2.dp.toPx() + flutter)
        quadraticTo(
            x - 3.dp.toPx(), neckY + 8.dp.toPx(),
            x - 1.dp.toPx(), neckY + 2.dp.toPx()
        )
        close()
    }

    drawPath(path = capePath, color = color)
}

private fun DrawScope.drawHeroHat(
    hat: AccessoryItem,
    primaryColor: Color,
    secondaryColor: Color,
    x: Float,
    headY: Float,
    headRadius: Float
) {
    when (hat.id) {
        "hat_crown" -> {
            val crownWidth = 14.dp.toPx()
            val crownHeight = 9.dp.toPx()
            val crownBottom = headY - headRadius + 2.dp.toPx()
            val crownPath = Path().apply {
                moveTo(x - crownWidth / 2f, crownBottom)
                lineTo(x - crownWidth / 2f, crownBottom - crownHeight)
                lineTo(x - crownWidth / 4f, crownBottom - crownHeight * 0.5f)
                lineTo(x, crownBottom - crownHeight * 1.1f)
                lineTo(x + crownWidth / 4f, crownBottom - crownHeight * 0.5f)
                lineTo(x + crownWidth / 2f, crownBottom - crownHeight)
                lineTo(x + crownWidth / 2f, crownBottom)
                close()
            }
            drawPath(path = crownPath, color = primaryColor)
            // Jewel dots
            drawCircle(color = secondaryColor, radius = 1.2.dp.toPx(), center = Offset(x, crownBottom - crownHeight * 0.9f))
            drawCircle(color = Color(0xFFEF4444), radius = 1.dp.toPx(), center = Offset(x - crownWidth / 2f + 1.dp.toPx(), crownBottom - crownHeight * 0.8f))
            drawCircle(color = Color(0xFF38BDF8), radius = 1.dp.toPx(), center = Offset(x + crownWidth / 2f - 1.dp.toPx(), crownBottom - crownHeight * 0.8f))
        }

        "hat_wizard" -> {
            val brimWidth = 18.dp.toPx()
            val coneHeight = 16.dp.toPx()
            val hatBottom = headY - headRadius + 1.5.dp.toPx()
            // Brim
            drawOval(
                color = secondaryColor,
                topLeft = Offset(x - brimWidth / 2f, hatBottom - 2.dp.toPx()),
                size = Size(brimWidth, 4.dp.toPx())
            )
            // Cone
            val conePath = Path().apply {
                moveTo(x - brimWidth * 0.35f, hatBottom)
                lineTo(x - 2.dp.toPx(), hatBottom - coneHeight)
                lineTo(x + brimWidth * 0.35f, hatBottom)
                close()
            }
            drawPath(path = conePath, color = primaryColor)
            // Star on hat
            drawCircle(color = Color(0xFFFFD700), radius = 1.8.dp.toPx(), center = Offset(x - 1.dp.toPx(), hatBottom - coneHeight * 0.6f))
        }

        "hat_ninja" -> {
            // Black ninja headband with trailing dual ribbons
            val bandHeight = 4.dp.toPx()
            val bandTop = headY - headRadius * 0.5f
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(x - headRadius - 1.dp.toPx(), bandTop),
                size = Size((headRadius * 2f) + 2.dp.toPx(), bandHeight),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            // Trailing red ribbons
            val ribbon1 = Path().apply {
                moveTo(x - headRadius, bandTop + 2.dp.toPx())
                quadraticTo(x - headRadius - 7.dp.toPx(), bandTop + 4.dp.toPx(), x - headRadius - 12.dp.toPx(), bandTop + 10.dp.toPx())
            }
            drawPath(ribbon1, color = secondaryColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        "hat_viking" -> {
            val helmetBottom = headY - headRadius + 3.dp.toPx()
            // Helmet dome
            drawArc(
                color = primaryColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(x - headRadius, headY - headRadius * 1.5f),
                size = Size(headRadius * 2f, headRadius * 1.5f)
            )
            // Left horn
            val leftHorn = Path().apply {
                moveTo(x - headRadius, helmetBottom - 4.dp.toPx())
                quadraticTo(x - headRadius - 8.dp.toPx(), helmetBottom - 6.dp.toPx(), x - headRadius - 6.dp.toPx(), helmetBottom - 14.dp.toPx())
            }
            drawPath(leftHorn, color = secondaryColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            // Right horn
            val rightHorn = Path().apply {
                moveTo(x + headRadius, helmetBottom - 4.dp.toPx())
                quadraticTo(x + headRadius + 8.dp.toPx(), helmetBottom - 6.dp.toPx(), x + headRadius + 6.dp.toPx(), helmetBottom - 14.dp.toPx())
            }
            drawPath(rightHorn, color = secondaryColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        }

        "hat_cyber" -> {
            // Neon visor
            val visorY = headY - 1.dp.toPx()
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(x - 2.dp.toPx(), visorY - 2.5.dp.toPx()),
                size = Size(10.dp.toPx(), 5.dp.toPx()),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
            drawCircle(
                color = secondaryColor,
                radius = 1.2.dp.toPx(),
                center = Offset(x + 4.dp.toPx(), visorY)
            )
        }

        "hat_halo" -> {
            // Floating golden halo above head
            val haloY = headY - headRadius - 6.dp.toPx()
            drawOval(
                color = primaryColor,
                topLeft = Offset(x - 8.dp.toPx(), haloY),
                size = Size(16.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(x - 6.dp.toPx(), haloY + 0.5.dp.toPx()),
                size = Size(12.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        "hat_pirate" -> {
            // Tricorn pirate captain hat
            val tricornBottom = headY - headRadius + 2.dp.toPx()
            val tricornPath = Path().apply {
                moveTo(x - 12.dp.toPx(), tricornBottom)
                lineTo(x - 10.dp.toPx(), tricornBottom - 7.dp.toPx())
                lineTo(x, tricornBottom - 11.dp.toPx())
                lineTo(x + 10.dp.toPx(), tricornBottom - 7.dp.toPx())
                lineTo(x + 12.dp.toPx(), tricornBottom)
                close()
            }
            drawPath(tricornPath, color = primaryColor)
            // Gold skull emblem dot
            drawCircle(color = secondaryColor, radius = 1.5.dp.toPx(), center = Offset(x, tricornBottom - 5.dp.toPx()))
        }

        else -> {
            // Default Red Band
            val bandTop = headY - headRadius * 0.4f
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(x - headRadius - 1.dp.toPx(), bandTop),
                size = Size((headRadius * 2f) + 2.dp.toPx(), 3.5.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawParticles(particles: List<Particle>) {
    particles.forEach { p ->
        val pColor = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f))
        when (p.shape) {
            ParticleShape.RING_WAVE -> {
                // Expanding shockwave ripple ring
                val progress = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
                val expandingRadius = p.radius + (progress * 32.dp.toPx())
                val ringStroke = (3f * (1f - progress)).coerceAtLeast(0.5f).dp.toPx()
                drawCircle(
                    color = pColor,
                    radius = expandingRadius,
                    center = Offset(p.x, p.y),
                    style = Stroke(width = ringStroke)
                )
            }
            ParticleShape.GEM_BURST -> {
                // Faceted diamond gem shard with sparkle core
                val progress = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
                val dynamicRadius = p.radius * (1f + progress * 0.35f)
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    val path = Path().apply {
                        moveTo(p.x, p.y - dynamicRadius * 1.3f)
                        lineTo(p.x + dynamicRadius * 0.8f, p.y)
                        lineTo(p.x, p.y + dynamicRadius * 1.3f)
                        lineTo(p.x - dynamicRadius * 0.8f, p.y)
                        close()
                    }
                    drawPath(path = path, color = pColor)
                    // Center white twinkle core
                    drawCircle(
                        color = Color.White.copy(alpha = (p.alpha * 0.9f).coerceIn(0f, 1f)),
                        radius = dynamicRadius * 0.35f,
                        center = Offset(p.x, p.y)
                    )
                }
            }
            ParticleShape.STAR -> {
                // 4-Point Diamond Sparkle Star with rotation
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    val path = Path().apply {
                        moveTo(p.x, p.y - p.radius * 1.4f)
                        lineTo(p.x + p.radius * 0.35f, p.y)
                        lineTo(p.x, p.y + p.radius * 1.4f)
                        lineTo(p.x - p.radius * 0.35f, p.y)
                        close()
                    }
                    val pathH = Path().apply {
                        moveTo(p.x - p.radius * 1.4f, p.y)
                        lineTo(p.x, p.y + p.radius * 0.35f)
                        lineTo(p.x + p.radius * 1.4f, p.y)
                        lineTo(p.x, p.y - p.radius * 0.35f)
                        close()
                    }
                    drawPath(path = path, color = pColor)
                    drawPath(path = pathH, color = pColor)
                    drawCircle(
                        color = Color.White.copy(alpha = (p.alpha * 0.85f).coerceIn(0f, 1f)),
                        radius = p.radius * 0.4f,
                        center = Offset(p.x, p.y)
                    )
                }
            }
            ParticleShape.CONFETTI -> {
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    drawRoundRect(
                        color = pColor,
                        topLeft = Offset(p.x - p.radius, p.y - p.radius * 0.6f),
                        size = Size(p.radius * 2f, p.radius * 1.2f),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }
            }
            ParticleShape.FIRE_EMBER -> {
                // Warm glowing fire ember with core
                drawCircle(
                    color = pColor.copy(alpha = (p.alpha * 0.4f).coerceIn(0f, 1f)),
                    radius = p.radius * 1.8f,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = pColor,
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = (p.alpha * 0.8f).coerceIn(0f, 1f)),
                    radius = p.radius * 0.4f,
                    center = Offset(p.x, p.y)
                )
            }
            ParticleShape.SPARKLE -> {
                // High-intensity micro spark
                drawCircle(
                    color = pColor,
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
                drawLine(
                    color = Color.White.copy(alpha = p.alpha),
                    start = Offset(p.x - p.radius * 1.6f, p.y),
                    end = Offset(p.x + p.radius * 1.6f, p.y),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = p.alpha),
                    start = Offset(p.x, p.y - p.radius * 1.6f),
                    end = Offset(p.x, p.y + p.radius * 1.6f),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            ParticleShape.NEON_ORB -> {
                // Pulsing glowing orb
                drawCircle(
                    color = pColor.copy(alpha = (p.alpha * 0.35f).coerceIn(0f, 1f)),
                    radius = p.radius * 2.2f,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = pColor,
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = (p.alpha * 0.9f).coerceIn(0f, 1f)),
                    radius = p.radius * 0.45f,
                    center = Offset(p.x, p.y)
                )
            }
            ParticleShape.DUST -> {
                // Soft billowing dust puff
                drawCircle(
                    color = pColor,
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
            }
            ParticleShape.BALLOON_POP -> {
                // Balloon fragment/shard: tear-drop / rubber petal curve with glossy highlight
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    val progress = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
                    val stretch = p.radius * (1f + progress * 0.4f)
                    val path = Path().apply {
                        moveTo(p.x, p.y - stretch * 1.5f)
                        cubicTo(
                            p.x + stretch * 1.2f, p.y - stretch * 0.5f,
                            p.x + stretch * 0.8f, p.y + stretch * 1.2f,
                            p.x, p.y + stretch * 1.4f
                        )
                        cubicTo(
                            p.x - stretch * 0.8f, p.y + stretch * 1.2f,
                            p.x - stretch * 1.2f, p.y - stretch * 0.5f,
                            p.x, p.y - stretch * 1.5f
                        )
                        close()
                    }
                    drawPath(path = path, color = pColor)
                    // Curved specular shine
                    drawCircle(
                        color = Color.White.copy(alpha = (p.alpha * 0.75f).coerceIn(0f, 1f)),
                        radius = stretch * 0.35f,
                        center = Offset(p.x - stretch * 0.25f, p.y - stretch * 0.4f)
                    )
                }
            }
            ParticleShape.RIBBON -> {
                // Spiraling celebratory streamer ribbon
                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    val width = p.radius * 2.6f
                    val height = p.radius * 0.8f
                    drawRoundRect(
                        color = pColor,
                        topLeft = Offset(p.x - width / 2f, p.y - height / 2f),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
            else -> {
                drawCircle(
                    color = pColor,
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

private fun DrawScope.drawFloatingTexts(
    texts: List<FloatingPopupText>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    texts.forEach { item ->
        val style = TextStyle(
            fontSize = (18 * item.scale).sp,
            fontWeight = FontWeight.Black,
            color = item.color.copy(alpha = item.alpha),
            shadow = Shadow(
                color = Color.Black.copy(alpha = item.alpha * 0.8f),
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        )
        val textLayout = textMeasurer.measure(item.text, style)
        val posX = item.x - (textLayout.size.width / 2f)
        val posY = item.y - (textLayout.size.height / 2f)

        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(posX, posY)
        )
    }
}

private fun DrawScope.drawIdleHoldPrompt(
    engine: StickmanGameEngine,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    time: Float
) {
    val pulse = (sin(time * 4.5f) * 0.15f + 0.85f).coerceIn(0.6f, 1f)
    val text = if (engine.score.value == 0) "HOLD SCREEN TO GROW BRIDGE" else "HOLD TO STRETCH"
    val style = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFDE047).copy(alpha = pulse),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(1.5f, 1.5f),
            blurRadius = 3f
        )
    )
    val textLayout = textMeasurer.measure(text, style)
    val promptX = (engine.bridgeStartX + (engine.nextPlatform.leftX - engine.bridgeStartX) / 2f).coerceIn(
        120f,
        size.width - 120f
    )
    val promptY = engine.floorY - 140f + (sin(time * 3f) * 6f)

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(promptX - (textLayout.size.width / 2f), promptY)
    )
}

private fun DrawScope.drawInvertedFlipPrompt(
    engine: StickmanGameEngine,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    time: Float
) {
    val pulse = (sin(time * 8f) * 0.25f + 0.75f).coerceIn(0.5f, 1f)
    val text = "TAP TO FLIP UP ⬆️"
    val style = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFF38BDF8).copy(alpha = pulse),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.9f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )
    val textLayout = textMeasurer.measure(text, style)
    val promptX = engine.stickmanX
    val promptY = engine.floorY + 65f + (sin(time * 6f) * 4f)

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(promptX - (textLayout.size.width / 2f), promptY)
    )
}

