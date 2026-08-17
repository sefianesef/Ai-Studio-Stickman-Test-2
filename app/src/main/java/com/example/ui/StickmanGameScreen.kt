package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GameState

@Composable
fun StickmanGameScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.engine.gameState.collectAsState()
    val isShopOpen by viewModel.isShopOpen.collectAsState()
    val isPauseMenuOpen by viewModel.isPauseMenuOpen.collectAsState()
    val isHowToPlayOpen by viewModel.isHowToPlayOpen.collectAsState()
    val isDailyRewardOpen by viewModel.isDailyRewardOpen.collectAsState()
    val isDailyMissionsOpen by viewModel.isDailyMissionsOpen.collectAsState()
    val isLeaderboardOpen by viewModel.isLeaderboardOpen.collectAsState()
    val isSpinWheelOpen by viewModel.isSpinWheelOpen.collectAsState()
    val levelVictoryCelebration by viewModel.engine.levelVictoryCelebration.collectAsState()

    val isPlaying = gameState != GameState.START && gameState != GameState.GAMEOVER
    val isStart = gameState == GameState.START
    val isGameOver = gameState == GameState.GAMEOVER

    // Auto-pop up Daily Missions dialog on initial game launch to maximize engagement
    LaunchedEffect(Unit) {
        viewModel.openDailyMissions(true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06140E))
    ) {
        // 1. Core Physics & Canvas rendering (Continuous world backdrop)
        StickmanGameCanvas(viewModel = viewModel)

        // 2. HUD / PLAYING Overlay (Score, Gems, Stage, Controls) with Fade In / Out
        AnimatedVisibility(
            visible = isPlaying,
            enter = fadeIn(animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing))
        ) {
            GameHud(viewModel = viewModel)
        }

        // 3. Start Screen Overlay with Fade In / Out
        AnimatedVisibility(
            visible = isStart,
            enter = fadeIn(animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing))
        ) {
            StartScreenOverlay(viewModel = viewModel)
        }

        // 4. Game Over Overlay with Fade In + Scale / Out
        AnimatedVisibility(
            visible = isGameOver,
            enter = fadeIn(animationSpec = tween(durationMillis = 450, delayMillis = 100, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 450, delayMillis = 100, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)) +
                   scaleOut(targetScale = 0.95f, animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing))
        ) {
            GameOverDialog(viewModel = viewModel)
        }

        // 5. Shop Dialog
        if (isShopOpen) {
            ShopDialog(viewModel = viewModel)
        }

        // 6. Pause Dialog
        if (isPauseMenuOpen) {
            PauseMenuDialog(viewModel = viewModel)
        }

        // 7. How to Play Dialog
        if (isHowToPlayOpen) {
            HowToPlayDialog(onDismiss = { viewModel.openHowToPlay(false) })
        }

        // 8. Daily Reward Dialog
        if (isDailyRewardOpen) {
            DailyRewardDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openDailyReward(false) }
            )
        }

        // 9. Daily Missions Dialog
        if (isDailyMissionsOpen) {
            DailyMissionsDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openDailyMissions(false) }
            )
        }

        // 10. Global Arena & Weekly Contest Dialog
        if (isLeaderboardOpen) {
            LeaderboardAndContestDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openLeaderboard(false) }
            )
        }

        // 11. Lucky Spin Wheel Dialog
        if (isSpinWheelOpen) {
            LuckySpinWheelDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openSpinWheel(false) }
            )
        }

        // 12. Level Victory Milestone Celebration Dialog
        levelVictoryCelebration?.let { celebrationText ->
            LevelVictoryCelebrationDialog(
                celebrationText = celebrationText,
                onDismiss = { viewModel.engine.dismissVictoryCelebration() }
            )
        }
    }
}
