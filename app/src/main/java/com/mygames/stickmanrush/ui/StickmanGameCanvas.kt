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

                        engine.onTouchDown(touchX = down.position.x, touchY = down.position.y)

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

            // 2. Draw platforms (with variable height offsets)
            drawPlatforms(engine, currentStage)

            // 3. Draw Boss entity if active
            drawBoss(engine, gameTimeSeconds)

            // 4. Draw Obstacles (Buzzsaws, Spike Mines, Laser Barriers, Spike Orbs)
            drawObstacles(engine, gameTimeSeconds)

            // 5. Draw gems
            drawGems(engine, gameTimeSeconds)

            // 5b. Draw tactical power-up items (Magnet, Aegis Shield, Gem Doubler, Chrono Slow-Mo)
            drawPowerUps(engine, gameTimeSeconds)

            // 6. Draw Bridge / Stick
            drawBridge(engine, equippedStick)

            // 7. Draw Boss Projectiles
            drawBossProjectiles(engine, gameTimeSeconds)

            // 8. Draw Stickman Hero with cosmetics
            drawHeroStickman(
                engine = engine,
                hat = equippedHat,
                scarf = equippedScarf,
                skin = equippedSkin,
                gameTime = gameTimeSeconds
            )

            // 9. Draw Boss Health HUD on top
            drawBossHud(engine, textMeasurer, gameTimeSeconds)

            // 9b. Draw Active Tactical Power-Up Status HUD & Timers
            drawActivePowerUpHud(engine, textMeasurer, gameTimeSeconds)

            // 10. Draw particles
            drawParticles(engine.particles)

            // 11. Draw floating popups
            drawFloatingTexts(engine.floatingTexts, textMeasurer)

            // 12. Draw prompt hint when waiting for player to stretch bridge
            if (engine.gameState.value == GameState.IDLE) {
                drawIdleHoldPrompt(engine, textMeasurer, gameTimeSeconds)
            }

            // 13. Draw prompt hint when walking inverted under the bridge
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


}

private fun DrawScope.drawPlatforms(engine: StickmanGameEngine, stage: StageTheme) {
    val floorY = engine.floorY
    val platColor = stage.platformColor
    val highlightColor = stage.platformHighlightColor

    // Current Platform
    val p1 = engine.currentPlatform
    val p1TopY = floorY + p1.heightOffset
    if (p1.leftX + p1.width > 0f) {
        drawRoundRect(
            color = platColor,
            topLeft = Offset(p1.leftX, p1TopY),
            size = Size(p1.width, size.height - p1TopY + 100f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Top highlight line
        drawLine(
            color = highlightColor,
            start = Offset(p1.leftX, p1TopY),
            end = Offset(p1.leftX + p1.width, p1TopY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Next Platform
    val p2 = engine.nextPlatform
    val p2TopY = floorY + p2.heightOffset
    if (p2.leftX < size.width) {
        drawRoundRect(
            color = platColor,
            topLeft = Offset(p2.leftX, p2TopY),
            size = Size(p2.width, size.height - p2TopY + 100f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        // Top highlight line
        drawLine(
            color = highlightColor,
            start = Offset(p2.leftX, p2TopY),
            end = Offset(p2.leftX + p2.width, p2TopY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center Red Bullseye Dot (Bonus point marker)
        if (p2.hasRedDot) {
            val centerDotX = p2.leftX + (p2.width / 2f)
            drawRoundRect(
                color = Color(0xFFEF4444),
                topLeft = Offset(centerDotX - 5.dp.toPx(), p2TopY - 1.dp.toPx()),
                size = Size(10.dp.toPx(), 5.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            // Glowing core
            drawCircle(
                color = Color(0xFFFEE2E2),
                radius = 2.dp.toPx(),
                center = Offset(centerDotX, p2TopY + 1.dp.toPx())
            )
        }

        // Moving Platform Thrusters & Directional Indicators
        if (p2.isMoving) {
            val thrusterY = p2TopY + 10.dp.toPx()
            val pulse = (sin(p2.movePhase * 4f) * 0.3f + 0.7f).coerceIn(0.3f, 1f)
            // Thrusters on left & right
            drawCircle(color = Color(0xFF0284C7), radius = 5.dp.toPx(), center = Offset(p2.leftX + 8.dp.toPx(), thrusterY))
            drawCircle(color = Color(0xFF38BDF8).copy(alpha = pulse), radius = 3.dp.toPx(), center = Offset(p2.leftX + 8.dp.toPx(), thrusterY))
            drawCircle(color = Color(0xFF0284C7), radius = 5.dp.toPx(), center = Offset(p2.leftX + p2.width - 8.dp.toPx(), thrusterY))
            drawCircle(color = Color(0xFF38BDF8).copy(alpha = pulse), radius = 3.dp.toPx(), center = Offset(p2.leftX + p2.width - 8.dp.toPx(), thrusterY))

            // Directional motion arrow indicator
            val arrowMidX = p2.leftX + (p2.width / 2f)
            val arrowY = p2TopY + 16.dp.toPx()
            val arrowOffset = sin(p2.movePhase) * 5.dp.toPx()
            if (p2.moveVertical) {
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                    start = Offset(arrowMidX, arrowY - 5.dp.toPx() + arrowOffset),
                    end = Offset(arrowMidX, arrowY + 5.dp.toPx() + arrowOffset),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            } else {
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.85f),
                    start = Offset(arrowMidX - 7.dp.toPx() + arrowOffset, arrowY),
                    end = Offset(arrowMidX + 7.dp.toPx() + arrowOffset, arrowY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawGems(engine: StickmanGameEngine, time: Float) {
    val gem = engine.nextPlatform.gem ?: return
    if (gem.collected) return

    val bobbing = sin(time * 4f) * 4.dp.toPx()
    val spanW = (engine.nextPlatform.leftX - engine.bridgeStartX).coerceAtLeast(10f)
    val prog = ((gem.x - engine.bridgeStartX) / spanW).coerceIn(0f, 1f)
    val startY = engine.floorY + engine.currentPlatform.heightOffset
    val endY = engine.floorY + engine.nextPlatform.heightOffset
    val baseY = startY + prog * (endY - startY)
    val gemY = if (gem.isUnderBridge) {
        baseY + 28.dp.toPx() + bobbing
    } else {
        baseY - 24.dp.toPx() + bobbing
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

private fun DrawScope.drawPowerUps(engine: StickmanGameEngine, time: Float) {
    val pUp = engine.nextPlatform.powerUp ?: return
    if (pUp.collected) return
    if (pUp.x < -50f || pUp.x > size.width + 50f) return

    val bobbing = sin(time * 5f) * 4.dp.toPx()
    val spanW = (engine.nextPlatform.leftX - engine.bridgeStartX).coerceAtLeast(10f)
    val prog = ((pUp.x - engine.bridgeStartX) / spanW).coerceIn(0f, 1f)
    val startY = engine.floorY + engine.currentPlatform.heightOffset
    val endY = engine.floorY + engine.nextPlatform.heightOffset
    val baseY = startY + prog * (endY - startY)
    val pUpY = baseY + pUp.floatOffset + bobbing

    val pColor = Color(pUp.type.primaryColorHex)
    val sColor = Color(pUp.type.secondaryColorHex)
    val pulse = (sin(time * 6f) * 0.2f + 0.8f)

    when (pUp.type) {
        PowerUpType.MAGNET -> {
            // 🧲 Horseshoe Magnet with Magnetic Flux Waves
            val magnetRadius = 14.dp.toPx()

            // Outer magnetic field wave
            drawCircle(
                color = pColor.copy(alpha = 0.25f * pulse),
                radius = magnetRadius * 1.8f,
                center = Offset(pUp.x, pUpY)
            )
            drawCircle(
                color = sColor.copy(alpha = 0.35f),
                radius = magnetRadius * 1.35f,
                center = Offset(pUp.x, pUpY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Red/Blue Horseshoe U-Shape
            val uPath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = pUp.x - magnetRadius,
                        top = pUpY - magnetRadius,
                        right = pUp.x + magnetRadius,
                        bottom = pUpY + magnetRadius
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
            }
            drawPath(
                path = uPath,
                color = pColor,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Silver pole tips
            drawRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(pUp.x - magnetRadius - 3.dp.toPx(), pUpY - 1.dp.toPx()),
                size = Size(6.dp.toPx(), 6.dp.toPx())
            )
            drawRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(pUp.x + magnetRadius - 3.dp.toPx(), pUpY - 1.dp.toPx()),
                size = Size(6.dp.toPx(), 6.dp.toPx())
            )

            // Sparkle lightning arcs
            val sparkAng = (time * 8f) % (2f * PI.toFloat())
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 2.dp.toPx(),
                center = Offset(pUp.x + cos(sparkAng) * magnetRadius * 1.2f, pUpY + sin(sparkAng) * magnetRadius * 1.2f)
            )
        }

        PowerUpType.INVINCIBILITY_SHIELD -> {
            // 🛡️ Aegis Radiant Crest Shield
            val shieldRadius = 15.dp.toPx()

            // Energy aura
            drawCircle(
                color = pColor.copy(alpha = 0.3f * pulse),
                radius = shieldRadius * 1.6f,
                center = Offset(pUp.x, pUpY)
            )

            // Heraldic Shield Crest Path
            val shieldPath = Path().apply {
                moveTo(pUp.x, pUpY - shieldRadius)
                lineTo(pUp.x + shieldRadius * 0.85f, pUpY - shieldRadius * 0.6f)
                cubicTo(
                    pUp.x + shieldRadius * 0.9f, pUpY + shieldRadius * 0.2f,
                    pUp.x + shieldRadius * 0.4f, pUpY + shieldRadius * 0.85f,
                    pUp.x, pUpY + shieldRadius
                )
                cubicTo(
                    pUp.x - shieldRadius * 0.4f, pUpY + shieldRadius * 0.85f,
                    pUp.x - shieldRadius * 0.9f, pUpY + shieldRadius * 0.2f,
                    pUp.x - shieldRadius * 0.85f, pUpY - shieldRadius * 0.6f
                )
                close()
            }
            drawPath(path = shieldPath, color = pColor)

            // Inner heraldic inlay
            val innerPath = Path().apply {
                moveTo(pUp.x, pUpY - shieldRadius * 0.65f)
                lineTo(pUp.x + shieldRadius * 0.55f, pUpY - shieldRadius * 0.35f)
                lineTo(pUp.x, pUpY + shieldRadius * 0.65f)
                lineTo(pUp.x - shieldRadius * 0.55f, pUpY - shieldRadius * 0.35f)
                close()
            }
            drawPath(path = innerPath, color = sColor)

            // Golden star emblem
            drawCircle(color = Color(0xFFFFD700), radius = 3.dp.toPx(), center = Offset(pUp.x, pUpY - 1.dp.toPx()))
        }

        PowerUpType.GEM_DOUBLER -> {
            // ✨ Dual Glowing 2X Crystals
            val crystalRadius = 13.dp.toPx()

            drawCircle(
                color = pColor.copy(alpha = 0.35f * pulse),
                radius = crystalRadius * 1.7f,
                center = Offset(pUp.x, pUpY)
            )

            // Left Emerald Diamond
            val leftDiamond = Path().apply {
                moveTo(pUp.x - 5.dp.toPx(), pUpY - crystalRadius)
                lineTo(pUp.x + 1.dp.toPx(), pUpY)
                lineTo(pUp.x - 5.dp.toPx(), pUpY + crystalRadius)
                lineTo(pUp.x - 11.dp.toPx(), pUpY)
                close()
            }
            drawPath(path = leftDiamond, color = pColor)

            // Right Amber Diamond
            val rightDiamond = Path().apply {
                moveTo(pUp.x + 5.dp.toPx(), pUpY - crystalRadius * 0.9f)
                lineTo(pUp.x + 11.dp.toPx(), pUpY)
                lineTo(pUp.x + 5.dp.toPx(), pUpY + crystalRadius * 0.9f)
                lineTo(pUp.x - 1.dp.toPx(), pUpY)
                close()
            }
            drawPath(path = rightDiamond, color = sColor)

            // Center golden "2" spark glint
            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(pUp.x, pUpY))
        }

        PowerUpType.SLOW_MOTION -> {
            // ⏱️ Chrono Hourglass & Temporal Waves
            val clockRadius = 14.dp.toPx()

            // Temporal distortion rings
            drawCircle(
                color = pColor.copy(alpha = 0.28f * pulse),
                radius = clockRadius * 1.6f,
                center = Offset(pUp.x, pUpY)
            )
            drawCircle(
                color = sColor.copy(alpha = 0.6f),
                radius = clockRadius,
                center = Offset(pUp.x, pUpY),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Hourglass silhouette
            val hgPath = Path().apply {
                moveTo(pUp.x - 6.dp.toPx(), pUpY - 8.dp.toPx())
                lineTo(pUp.x + 6.dp.toPx(), pUpY - 8.dp.toPx())
                lineTo(pUp.x, pUpY)
                lineTo(pUp.x + 6.dp.toPx(), pUpY + 8.dp.toPx())
                lineTo(pUp.x - 6.dp.toPx(), pUpY + 8.dp.toPx())
                lineTo(pUp.x, pUpY)
                close()
            }
            drawPath(path = hgPath, color = pColor)

            // Chrono ticking hand
            val handAng = (time * 4f)
            drawLine(
                color = Color.White,
                start = Offset(pUp.x, pUpY),
                end = Offset(pUp.x + cos(handAng) * 6.dp.toPx(), pUpY + sin(handAng) * 6.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawObstacles(engine: StickmanGameEngine, time: Float) {
    val obs = engine.nextPlatform.obstacle ?: return
    if (obs.x < -50f || obs.x > size.width + 50f) return

    when (obs.type) {
        ObstacleType.SPINNING_BLADE -> {
            // High hazard: Sharp spinning circular buzzsaw with warning teeth
            val bladeRadius = obs.width / 2f
            rotate(degrees = (time * 720f) % 360f, pivot = Offset(obs.x, obs.y)) {
                // Steel saw body
                drawCircle(
                    color = Color(0xFFE2E8F0),
                    radius = bladeRadius,
                    center = Offset(obs.x, obs.y)
                )
                drawCircle(
                    color = Color(0xFF94A3B8),
                    radius = bladeRadius * 0.7f,
                    center = Offset(obs.x, obs.y)
                )
                // Red danger core
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = bladeRadius * 0.35f,
                    center = Offset(obs.x, obs.y)
                )

                // 8 Serrated saw teeth
                for (i in 0 until 8) {
                    val toothAngle = (i * 45.0 * PI / 180.0).toFloat()
                    val tx = obs.x + cos(toothAngle) * (bladeRadius + 4.dp.toPx())
                    val ty = obs.y + sin(toothAngle) * (bladeRadius + 4.dp.toPx())
                    drawCircle(
                        color = Color(0xFFF1F5F9),
                        radius = 2.5.dp.toPx(),
                        center = Offset(tx, ty)
                    )
                }
            }
            // Warning ring
            drawCircle(
                color = Color(0x44EF4444),
                radius = bladeRadius * 1.4f,
                center = Offset(obs.x, obs.y),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        ObstacleType.SPIKE_MINE -> {
            // Low hazard: Spiked proximity mine hanging under bridge
            val mineRadius = obs.width / 2f
            // Dark iron spiked shell
            drawCircle(
                color = Color(0xFF1E293B),
                radius = mineRadius,
                center = Offset(obs.x, obs.y)
            )
            // Flashing proximity LED
            val flash = (sin(time * 12f) * 0.5f + 0.5f).coerceIn(0f, 1f)
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = flash),
                radius = mineRadius * 0.45f,
                center = Offset(obs.x, obs.y)
            )
            // Metal spikes pointing downward
            for (i in 0 until 5) {
                val spikeAngle = ((i * 35.0 + 35.0) * PI / 180.0).toFloat()
                val sx = obs.x + cos(spikeAngle) * (mineRadius + 5.dp.toPx())
                val sy = obs.y + sin(spikeAngle) * (mineRadius + 5.dp.toPx())
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(obs.x, obs.y),
                    end = Offset(sx, sy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        ObstacleType.LASER_BARRIER -> {
            // High hazard: Vertical pulsating laser tripwire
            val pulseAlpha = (sin(time * 14f) * 0.3f + 0.7f).coerceIn(0.4f, 1f)
            // Neon red outer aura
            drawLine(
                color = Color(0xFFEF4444).copy(alpha = pulseAlpha * 0.4f),
                start = Offset(obs.x, obs.y - 25.dp.toPx()),
                end = Offset(obs.x, obs.y + 25.dp.toPx()),
                strokeWidth = 9.dp.toPx(),
                cap = StrokeCap.Round
            )
            // White-hot core beam
            drawLine(
                color = Color.White.copy(alpha = pulseAlpha),
                start = Offset(obs.x, obs.y - 25.dp.toPx()),
                end = Offset(obs.x, obs.y + 25.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Emitter cap top and bottom
            drawCircle(color = Color(0xFF334155), radius = 4.dp.toPx(), center = Offset(obs.x, obs.y - 25.dp.toPx()))
            drawCircle(color = Color(0xFF334155), radius = 4.dp.toPx(), center = Offset(obs.x, obs.y + 25.dp.toPx()))
        }

        ObstacleType.MOVING_SPIKE_BALL -> {
            // Floating oscillating spike orb
            val orbRadius = obs.width / 2f
            drawCircle(
                color = Color(0xFF581C87),
                radius = orbRadius,
                center = Offset(obs.x, obs.y)
            )
            drawCircle(
                color = Color(0xFFA855F7),
                radius = orbRadius * 0.5f,
                center = Offset(obs.x, obs.y)
            )
            // 6 Dark energy spikes
            for (i in 0 until 6) {
                val sAng = (i * 60.0 * PI / 180.0).toFloat()
                val sx = obs.x + cos(sAng) * (orbRadius + 4.dp.toPx())
                val sy = obs.y + sin(sAng) * (orbRadius + 4.dp.toPx())
                drawLine(
                    color = Color(0xFFC084FC),
                    start = Offset(obs.x, obs.y),
                    end = Offset(sx, sy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        ObstacleType.SLIP_PATCH -> {
            // Slick ice / oil slip hazard on bridge
            val patchWidth = obs.width
            val patchLeft = obs.x - patchWidth / 2f
            val patchTop = obs.y - 4.dp.toPx()

            // Translucent cyan icy surface bar
            drawRoundRect(
                color = Color(0xDD38BDF8),
                topLeft = Offset(patchLeft, patchTop),
                size = Size(patchWidth, 9.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Frost glow outline
            drawRoundRect(
                color = Color(0xFFE0F2FE),
                topLeft = Offset(patchLeft, patchTop),
                size = Size(patchWidth, 9.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Sparkling ice glints
            val glintX = patchLeft + 6.dp.toPx() + ((time * 30f) % (patchWidth - 12.dp.toPx()))
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(glintX, patchTop + 4.5.dp.toPx())
            )
            // Chevron slide guides indicating slick zone
            for (c in 0 until 3) {
                val cx = patchLeft + 10.dp.toPx() + (c * 12.dp.toPx())
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx, patchTop + 2.dp.toPx()),
                    end = Offset(cx + 4.dp.toPx(), patchTop + 4.5.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx + 4.dp.toPx(), patchTop + 4.5.dp.toPx()),
                    end = Offset(cx, patchTop + 7.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        ObstacleType.FIRE_BALL -> {
            // Roaring fiery blazing fireball hovering on the bridge track
            val fireRadius = obs.width / 2f
            val flamePulse = (sin(time * 16f) * 0.15f + 0.85f)
            // Outer heat bloom aura
            drawCircle(
                color = Color(0xFFFF6B00).copy(alpha = 0.35f),
                radius = fireRadius * 1.8f * flamePulse,
                center = Offset(obs.x, obs.y)
            )
            // Fiery orange body
            drawCircle(
                color = Color(0xFFEA580C),
                radius = fireRadius * flamePulse,
                center = Offset(obs.x, obs.y)
            )
            // Golden magma layer
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = fireRadius * 0.65f,
                center = Offset(obs.x, obs.y)
            )
            // White-hot core
            drawCircle(
                color = Color.White,
                radius = fireRadius * 0.32f,
                center = Offset(obs.x, obs.y)
            )
            // Orbiting flame embers
            for (i in 0 until 6) {
                val fAng = ((i * 60f + time * 320f) * PI / 180.0).toFloat()
                val fx = obs.x + cos(fAng) * (fireRadius * 1.15f)
                val fy = obs.y + sin(fAng) * (fireRadius * 1.15f)
                drawCircle(
                    color = if (i % 2 == 0) Color(0xFFFFD700) else Color(0xFFEF4444),
                    radius = (2.5.dp.toPx() + sin(time * 20f + i) * 1.dp.toPx()).coerceAtLeast(1.dp.toPx()),
                    center = Offset(fx, fy)
                )
            }
        }
    }
}

private fun DrawScope.drawBoss(engine: StickmanGameEngine, time: Float) {
    val boss = engine.activeBossState.value ?: return
    if (boss.isDefeated) return

    val p2 = engine.nextPlatform
    val bossX = (p2.leftX + p2.width / 2f).coerceAtLeast(p2.leftX + 25f)
    val floatBob = sin(time * 3.5f) * 6.dp.toPx()
    val bossY = engine.floorY + p2.heightOffset - 48.dp.toPx() + floatBob

    val primaryColor = Color(boss.type.primaryColorHex)
    val secondaryColor = Color(boss.type.secondaryColorHex)

    when (boss.type) {
        BossType.STONE_TITAN -> {
            // Heavy Stone Golem Boss with Glowing Rune Eyes
            // Stone Torso
            drawRoundRect(
                color = Color(0xFF475569),
                topLeft = Offset(bossX - 18.dp.toPx(), bossY - 18.dp.toPx()),
                size = Size(36.dp.toPx(), 44.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            // Stone Shoulders
            drawCircle(color = Color(0xFF334155), radius = 12.dp.toPx(), center = Offset(bossX - 22.dp.toPx(), bossY - 8.dp.toPx()))
            drawCircle(color = Color(0xFF334155), radius = 12.dp.toPx(), center = Offset(bossX + 22.dp.toPx(), bossY - 8.dp.toPx()))
            // Stone Head
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(bossX - 14.dp.toPx(), bossY - 42.dp.toPx()),
                size = Size(28.dp.toPx(), 22.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Glowing Amber Rune Eyes
            val eyeGlow = (sin(time * 6f) * 0.2f + 0.8f)
            drawCircle(color = primaryColor.copy(alpha = eyeGlow), radius = 3.5.dp.toPx(), center = Offset(bossX - 6.dp.toPx(), bossY - 32.dp.toPx()))
            drawCircle(color = primaryColor.copy(alpha = eyeGlow), radius = 3.5.dp.toPx(), center = Offset(bossX + 6.dp.toPx(), bossY - 32.dp.toPx()))
        }

        BossType.INFERNO_DRAGON -> {
            // Majestic Fire Wyrm with Flapping Blazing Wings
            val wingFlap = sin(time * 8f) * 12.dp.toPx()
            // Fire Wings
            val leftWing = Path().apply {
                moveTo(bossX - 10.dp.toPx(), bossY - 15.dp.toPx())
                lineTo(bossX - 42.dp.toPx(), bossY - 40.dp.toPx() + wingFlap)
                lineTo(bossX - 32.dp.toPx(), bossY + 10.dp.toPx())
                close()
            }
            val rightWing = Path().apply {
                moveTo(bossX + 10.dp.toPx(), bossY - 15.dp.toPx())
                lineTo(bossX + 42.dp.toPx(), bossY - 40.dp.toPx() + wingFlap)
                lineTo(bossX + 32.dp.toPx(), bossY + 10.dp.toPx())
                close()
            }
            drawPath(path = leftWing, color = Color(0xCCEF4444))
            drawPath(path = rightWing, color = Color(0xCCEF4444))

            // Dragon Torso & Neck
            drawOval(
                color = Color(0xFF7F1D1D),
                topLeft = Offset(bossX - 16.dp.toPx(), bossY - 25.dp.toPx()),
                size = Size(32.dp.toPx(), 46.dp.toPx())
            )
            // Dragon Head & Horns
            drawCircle(color = Color(0xFF991B1B), radius = 14.dp.toPx(), center = Offset(bossX, bossY - 36.dp.toPx()))
            // Fiery Eyes
            drawCircle(color = Color(0xFFFEF08A), radius = 3.5.dp.toPx(), center = Offset(bossX - 5.dp.toPx(), bossY - 36.dp.toPx()))
            drawCircle(color = Color(0xFFFEF08A), radius = 3.5.dp.toPx(), center = Offset(bossX + 5.dp.toPx(), bossY - 36.dp.toPx()))
        }

        BossType.CYBER_GOLEM -> {
            // Sci-Fi Mecha Boss with Neon Pulse Visor
            // Mecha Chassis
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(bossX - 18.dp.toPx(), bossY - 20.dp.toPx()),
                size = Size(36.dp.toPx(), 42.dp.toPx()),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            // Cyan Reactor Core
            val corePulse = (sin(time * 10f) * 0.25f + 0.75f)
            drawCircle(color = primaryColor.copy(alpha = corePulse), radius = 7.dp.toPx(), center = Offset(bossX, bossY))
            // Mecha Visor Head
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(bossX - 14.dp.toPx(), bossY - 42.dp.toPx()),
                size = Size(28.dp.toPx(), 20.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            // Laser Visor Line
            drawLine(
                color = primaryColor,
                start = Offset(bossX - 10.dp.toPx(), bossY - 32.dp.toPx()),
                end = Offset(bossX + 10.dp.toPx(), bossY - 32.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        BossType.VOID_REAPER -> {
            // Ethereal Shadow Phantom Warlock
            // Dark Shadow Cloak
            val cloakPath = Path().apply {
                moveTo(bossX, bossY - 45.dp.toPx())
                cubicTo(bossX - 25.dp.toPx(), bossY - 20.dp.toPx(), bossX - 22.dp.toPx(), bossY + 25.dp.toPx(), bossX - 15.dp.toPx(), bossY + 30.dp.toPx())
                cubicTo(bossX, bossY + 18.dp.toPx(), bossX + 15.dp.toPx(), bossY + 30.dp.toPx(), bossX + 22.dp.toPx(), bossY + 25.dp.toPx())
                cubicTo(bossX + 25.dp.toPx(), bossY - 20.dp.toPx(), bossX + 15.dp.toPx(), bossY - 40.dp.toPx(), bossX, bossY - 45.dp.toPx())
                close()
            }
            drawPath(path = cloakPath, color = Color(0xDD09090B))
            // Purple Void Aura
            drawCircle(color = primaryColor.copy(alpha = 0.35f), radius = 28.dp.toPx(), center = Offset(bossX, bossY - 10.dp.toPx()))
            // Phantom Skull Eyes
            drawCircle(color = Color(0xFFC084FC), radius = 3.5.dp.toPx(), center = Offset(bossX - 6.dp.toPx(), bossY - 30.dp.toPx()))
            drawCircle(color = Color(0xFFC084FC), radius = 3.5.dp.toPx(), center = Offset(bossX + 6.dp.toPx(), bossY - 30.dp.toPx()))
        }
    }
}

private fun DrawScope.drawBossProjectiles(engine: StickmanGameEngine, time: Float) {
    val boss = engine.activeBossState.value ?: return
    if (boss.isDefeated) return

    for (proj in boss.projectiles) {
        val projColor = Color(proj.colorHex)
        val pulse = (sin(time * 16f) * 0.2f + 0.8f)

        // Outer energy aura
        drawCircle(
            color = projColor.copy(alpha = 0.45f * pulse),
            radius = proj.radius * 2.2f,
            center = Offset(proj.x, proj.y)
        )
        // Core orb
        drawCircle(
            color = projColor,
            radius = proj.radius * 1.2f,
            center = Offset(proj.x, proj.y)
        )
        // White hot center
        drawCircle(
            color = Color.White,
            radius = proj.radius * 0.55f,
            center = Offset(proj.x, proj.y)
        )
    }
}

private fun DrawScope.drawBossHud(
    engine: StickmanGameEngine,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    time: Float
) {
    val boss = engine.activeBossState.value ?: return
    if (boss.isDefeated) return

    val barWidth = 220.dp.toPx()
    val barHeight = 12.dp.toPx()
    val barX = (size.width - barWidth) / 2f
    val barY = 90.dp.toPx()

    // Boss Name Label
    val nameText = "⚔️ ${boss.type.bossName} - ${boss.type.title}"
    val nameStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        color = Color(boss.type.primaryColorHex),
        shadow = Shadow(color = Color.Black, offset = Offset(1.5f, 1.5f), blurRadius = 3f)
    )
    val nameLayout = textMeasurer.measure(nameText, nameStyle)
    drawText(
        textLayoutResult = nameLayout,
        topLeft = Offset((size.width - nameLayout.size.width) / 2f, barY - 18.dp.toPx())
    )

    // Boss Health Bar Background
    drawRoundRect(
        color = Color(0xCC0F172A),
        topLeft = Offset(barX, barY),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFF475569),
        topLeft = Offset(barX, barY),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Boss Current HP fill
    val hpFraction = (boss.currentHp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
    if (hpFraction > 0f) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFFEF4444), Color(boss.type.primaryColorHex))
            ),
            topLeft = Offset(barX + 2.dp.toPx(), barY + 2.dp.toPx()),
            size = Size((barWidth - 4.dp.toPx()) * hpFraction, barHeight - 4.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }

    // HP Text (e.g. 3 / 3 HP)
    val hpText = "HP: ${boss.currentHp} / ${boss.maxHp}"
    val hpStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 2f)
    )
    val hpLayout = textMeasurer.measure(hpText, hpStyle)
    drawText(
        textLayoutResult = hpLayout,
        topLeft = Offset((size.width - hpLayout.size.width) / 2f, barY + (barHeight - hpLayout.size.height) / 2f)
    )
}

private fun DrawScope.drawBridge(engine: StickmanGameEngine, stickSkin: AccessoryItem) {
    if (engine.stickLength <= 0f) return

    val startX = engine.bridgeStartX
    val startY = engine.floorY + engine.currentPlatform.heightOffset
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
    val isJumping = engine.isJumping
    val jumpOffsetY = if (isJumping) engine.jumpOffsetY else 0f
    val y = engine.stickmanY - jumpOffsetY + (if (engine.gameState.value == GameState.WALKING) engine.bridgeSagOffset else 0f)
    val isUpsideDown = engine.isUpsideDown
    val walkPhase = engine.walkPhase
    val rotation = engine.stickmanRotation + (if (isJumping) engine.jumpRotation else 0f)
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
        val bob = if (isJumping) -3.dp.toPx() else if (isWalking) -abs(sin(walkPhase)) * 2.dp.toPx() else 0f
        val currentHeadY = headCenterY + bob
        val currentNeckY = neckY + bob
        val currentHipY = hipY + bob * 0.7f

        // 1. Scarf / Cape (Drawn behind body)
        drawHeroCape(scarf, scarfColor, x, currentNeckY, isWalking || isJumping, if (isJumping) gameTime * 14f else walkPhase, gameTime)

        // 2. Spine / Torso (with slight forward lean during walking or leap lean during jump)
        val torsoLean = if (engine.isSlipping) -3.5.dp.toPx() else if (isJumping) 2.5.dp.toPx() else if (isWalking) 1.5.dp.toPx() else 0f
        drawLine(
            color = bodyColor,
            start = Offset(x + torsoLean, currentNeckY),
            end = Offset(x, currentHipY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 3. Legs: 2-Segment Articulated Kinematics (Hip -> Knee -> Foot)
        if (isJumping) {
            // High athletic airborne tuck pose
            val leftKneeX = x + 4.5.dp.toPx()
            val leftKneeY = currentHipY + 5.dp.toPx()
            val leftFootX = x - 2.dp.toPx()
            val leftFootY = currentHipY + 8.5.dp.toPx()

            val rightKneeX = x + 7.5.dp.toPx()
            val rightKneeY = currentHipY + 4.dp.toPx()
            val rightFootX = x + 1.5.dp.toPx()
            val rightFootY = currentHipY + 9.dp.toPx()

            // Left leg
            drawLine(color = bodyColor, start = Offset(x, currentHipY), end = Offset(leftKneeX, leftKneeY), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color = bodyColor, start = Offset(leftKneeX, leftKneeY), end = Offset(leftFootX, leftFootY), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)

            // Right leg
            drawLine(color = bodyColor, start = Offset(x, currentHipY), end = Offset(rightKneeX, rightKneeY), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color = bodyColor, start = Offset(rightKneeX, rightKneeY), end = Offset(rightFootX, rightFootY), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
        } else if (engine.isSlipping) {
            // High-speed ice slide stance (feet kicked forward, arms flailing)
            val slideWobble = sin(gameTime * 22f) * 2.dp.toPx()
            val foot1X = x + 12.dp.toPx() + slideWobble
            val foot2X = x + 6.dp.toPx() - slideWobble
            drawLine(
                color = bodyColor,
                start = Offset(x, currentHipY),
                end = Offset(foot1X, y),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = bodyColor,
                start = Offset(x, currentHipY),
                end = Offset(foot2X, y),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Speed slide lines under feet
            drawLine(
                color = Color(0xCC38BDF8),
                start = Offset(x - 14.dp.toPx(), y + 1.dp.toPx()),
                end = Offset(foot1X, y + 1.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else if (isWalking) {
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
        if (isJumping) {
            val shoulderY = currentNeckY + 2.dp.toPx()
            val shoulderX = x + torsoLean

            // Arms raised dynamically in the air for balance and joy
            val leftHandX = shoulderX - 6.dp.toPx()
            val leftHandY = shoulderY - 6.dp.toPx()
            val leftElbowX = (shoulderX + leftHandX) / 2f - 2.dp.toPx()
            val leftElbowY = (shoulderY + leftHandY) / 2f + 1.dp.toPx()

            drawLine(color = bodyColor, start = Offset(shoulderX, shoulderY), end = Offset(leftElbowX, leftElbowY), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color = bodyColor, start = Offset(leftElbowX, leftElbowY), end = Offset(leftHandX, leftHandY), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)

            val rightHandX = shoulderX + 8.dp.toPx()
            val rightHandY = shoulderY - 4.dp.toPx()
            val rightElbowX = (shoulderX + rightHandX) / 2f + 2.dp.toPx()
            val rightElbowY = (shoulderY + rightHandY) / 2f + 1.dp.toPx()

            drawLine(color = bodyColor, start = Offset(shoulderX, shoulderY), end = Offset(rightElbowX, rightElbowY), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(color = bodyColor, start = Offset(rightElbowX, rightElbowY), end = Offset(rightHandX, rightHandY), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        } else if (isWalking) {
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

    // Dynamic Hero Power-Up Auras & Forcefields
    val heroCenterY = y - 16.dp.toPx() - jumpOffsetY

    // 1. 🛡️ Aegis Invincibility Shield Forcefield Bubble
    if (engine.hasInvincibilityShield.value) {
        val shieldRadius = 26.dp.toPx()
        val pulse = (sin(gameTime * 6f) * 0.15f + 0.85f)

        // Forcefield dome glow
        drawCircle(
            color = Color(0x3338BDF8),
            radius = shieldRadius,
            center = Offset(x, heroCenterY)
        )
        // Shimmering outer energy barrier
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.85f * pulse),
            radius = shieldRadius,
            center = Offset(x, heroCenterY),
            style = Stroke(width = 2.2.dp.toPx())
        )
        // Orbiting defensive Aegis runes
        for (i in 0 until 3) {
            val rAng = (i * 120.0 + gameTime * 160.0) * PI / 180.0
            val rx = x + cos(rAng).toFloat() * shieldRadius
            val ry = heroCenterY + sin(rAng).toFloat() * shieldRadius
            drawCircle(
                color = Color(0xFFE0F2FE),
                radius = 2.5.dp.toPx(),
                center = Offset(rx, ry)
            )
        }
    }

    // 2. 🧲 Magnetic Attraction Field Lines
    if (engine.activeMagnetTime.value > 0f) {
        val magRadius = 28.dp.toPx()
        val magPulse = (sin(gameTime * 8f) * 0.2f + 0.8f)

        drawCircle(
            color = Color(0x22EF4444),
            radius = magRadius * magPulse,
            center = Offset(x, heroCenterY)
        )
        // Rotating magnetic flux arcs
        for (i in 0 until 2) {
            val startAng = (i * 180f + gameTime * 220f) % 360f
            val arcPath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = x - magRadius,
                        top = heroCenterY - magRadius,
                        right = x + magRadius,
                        bottom = heroCenterY + magRadius
                    ),
                    startAngleDegrees = startAng,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = true
                )
            }
            drawPath(
                path = arcPath,
                color = if (i == 0) Color(0xFFEF4444).copy(alpha = 0.8f) else Color(0xFF38BDF8).copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    // 3. ✨ 2X Gem Multiplier Golden Sparkles
    if (engine.activeGemDoublerTime.value > 0f) {
        for (i in 0 until 4) {
            val sparkY = heroCenterY + ((i * 14f - gameTime * 40f).mod(36.dp.toPx())) - 18.dp.toPx()
            val sparkX = x + sin(gameTime * 6f + i * 2f) * 14.dp.toPx()
            drawCircle(
                color = if (i % 2 == 0) Color(0xFFFFD700) else Color(0xFFFDE047),
                radius = 2.dp.toPx(),
                center = Offset(sparkX, sparkY)
            )
        }
    }

    // 4. ⚡ Shield Shatter Electrical Shockwave Flash
    if (engine.shieldShatterFx.value > 0f) {
        val shatterRadius = (1f - engine.shieldShatterFx.value) * 45.dp.toPx() + 20.dp.toPx()
        val shatterAlpha = engine.shieldShatterFx.value.coerceIn(0f, 1f)
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = shatterAlpha * 0.7f),
            radius = shatterRadius,
            center = Offset(x, heroCenterY),
            style = Stroke(width = 3.dp.toPx())
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
            ParticleShape.GLOW_TRAIL -> {
                // Dual-layer glowing speed trail particle: soft outer diffuse halo + bright energetic core
                val currentRadius = p.radius * (0.4f + 0.6f * p.alpha)
                drawCircle(
                    color = pColor.copy(alpha = (p.alpha * 0.35f).coerceIn(0f, 1f)),
                    radius = currentRadius * 2.4f,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = pColor.copy(alpha = (p.alpha * 0.75f).coerceIn(0f, 1f)),
                    radius = currentRadius * 1.3f,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = (p.alpha * 0.95f).coerceIn(0f, 1f)),
                    radius = currentRadius * 0.5f,
                    center = Offset(p.x, p.y)
                )
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

private fun DrawScope.drawActivePowerUpHud(
    engine: StickmanGameEngine,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    time: Float
) {
    val activeBadges = mutableListOf<Triple<String, Float, Color>>()

    if (engine.hasInvincibilityShield.value) {
        activeBadges.add(Triple("🛡️ AEGIS SHIELD", -1f, Color(0xFF38BDF8)))
    }
    if (engine.activeMagnetTime.value > 0f) {
        val rem = engine.activeMagnetTime.value
        activeBadges.add(Triple("🧲 MAGNET ${"%.1f".format(rem)}s", rem / 14f, Color(0xFFEF4444)))
    }
    if (engine.activeGemDoublerTime.value > 0f) {
        val rem = engine.activeGemDoublerTime.value
        activeBadges.add(Triple("✨ 2X GEMS ${"%.1f".format(rem)}s", rem / 15f, Color(0xFFFFD700)))
    }
    if (engine.activeSlowMoTime.value > 0f) {
        val rem = engine.activeSlowMoTime.value
        activeBadges.add(Triple("⏱️ SLOW-MO ${"%.1f".format(rem)}s", rem / 12f, Color(0xFF818CF8)))
    }

    if (activeBadges.isEmpty()) return

    val startX = 16.dp.toPx()
    var currentY = 110.dp.toPx()

    activeBadges.forEach { (label, progress, color) ->
        val textStyle = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.8f),
                offset = Offset(1f, 1f),
                blurRadius = 2f
            )
        )
        val textLayout = textMeasurer.measure(label, textStyle)
        val badgeW = (textLayout.size.width + 24.dp.toPx()).coerceAtLeast(110.dp.toPx())
        val badgeH = 24.dp.toPx()

        // Background chip
        drawRoundRect(
            color = Color(0xCC0F172A),
            topLeft = Offset(startX, currentY),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        // Neon border
        drawRoundRect(
            color = color.copy(alpha = 0.85f),
            topLeft = Offset(startX, currentY),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Progress bar if timed
        if (progress in 0f..1f) {
            val barW = (badgeW - 4.dp.toPx()) * progress
            drawRoundRect(
                color = color.copy(alpha = 0.35f),
                topLeft = Offset(startX + 2.dp.toPx(), currentY + 2.dp.toPx()),
                size = Size(barW, badgeH - 4.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
        }

        // Label text
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(startX + 12.dp.toPx(), currentY + (badgeH - textLayout.size.height) / 2f)
        )

        currentY += badgeH + 6.dp.toPx()
    }
}

