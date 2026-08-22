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
    val isWeeklyMissionsOpen by viewModel.isWeeklyMissionsOpen.collectAsState()
    val isContestsOpen by viewModel.isContestsOpen.collectAsState()
    val isMainMenuOpen by viewModel.isMainMenuOpen.collectAsState()
    val isPlayerStatsOpen by viewModel.isPlayerStatsOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isLeaderboardOpen by viewModel.isLeaderboardOpen.collectAsState()
    val isSpinWheelOpen by viewModel.isSpinWheelOpen.collectAsState()
    val isRealMoneyShopOpen by viewModel.isRealMoneyShopOpen.collectAsState()
    val isOutOfGemsOfferOpen by viewModel.isOutOfGemsOfferOpen.collectAsState()
    val isLifeShopOpen by viewModel.isLifeShopOpen.collectAsState()
    val activeChallengeDialog by viewModel.activeChallengeDialog.collectAsState()
    val activeLevelVictory by viewModel.activeLevelVictory.collectAsState()
    val levelVictoryCelebration by viewModel.engine.levelVictoryCelebration.collectAsState()

    val isPlaying = gameState != GameState.START && gameState != GameState.GAMEOVER
    val isStart = gameState == GameState.START
    val isGameOver = gameState == GameState.GAMEOVER

    // Play startup welcome melody and delay missions dialog
    LaunchedEffect(Unit) {
        viewModel.soundManager.playStartupMelody()
        kotlinx.coroutines.delay(7500)
        // Only open if the user hasn't started playing immediately or opened another menu
        if (viewModel.engine.gameState.value == GameState.START && !viewModel.isShopOpen.value && !viewModel.isMainMenuOpen.value && !viewModel.isRealMoneyShopOpen.value) {
            viewModel.openDailyMissions(true)
        }
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

        // 9. Daily Missions & Mission Pursuit Dialog (Inspired by Video)
        if (isDailyMissionsOpen) {
            RoyalMissionPursuitDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openDailyMissions(false) }
            )
        }

        // 10. Weekly Missions & Trials Dialog
        if (isWeeklyMissionsOpen) {
            WeeklyMissionsDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openWeeklyMissions(false) }
            )
        }

        // 11. Contests & Tournaments Center Dialog (Inspired by Video)
        if (isContestsOpen) {
            RoyalContestCenterDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openContests(false) }
            )
        }

        // 12. Master Professional Game Menu Dialog
        if (isMainMenuOpen) {
            ProfessionalMainMenuDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openMainMenu(false) }
            )
        }

        // 13. Player Career Battle Records Dialog
        if (isPlayerStatsOpen) {
            PlayerCareerStatsDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openPlayerStats(false) }
            )
        }

        // 14. Game Preferences & Audio/Display Settings Dialog
        if (isSettingsOpen) {
            GameSettingsDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openSettings(false) }
            )
        }

        // 15. Global Arena & World Leaderboard Dialog
        if (isLeaderboardOpen) {
            LeaderboardAndContestDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openLeaderboard(false) }
            )
        }

        // 16. Lucky Spin Wheel Dialog
        if (isSpinWheelOpen) {
            LuckySpinWheelDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openSpinWheel(false) }
            )
        }

        // 17. Dedicated Real-Money Gem Shop Dialog
        if (isRealMoneyShopOpen) {
            RealMoneyGemShopDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openRealMoneyShop(false) }
            )
        }

        // 18. Out of Gems Real-Money Special Offer Pop-up Dialog
        if (isOutOfGemsOfferOpen) {
            OutOfGemsSpecialOfferDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openOutOfGemsOffer(false) }
            )
        }

        // 19. Level Victory Milestone Celebration Dialog
        levelVictoryCelebration?.let { celebrationText ->
            LevelVictoryCelebrationDialog(
                celebrationText = celebrationText,
                levelNumber = activeLevelVictory?.levelNumber,
                onDismiss = { viewModel.engine.dismissVictoryCelebration() }
            )
        }

        // 20. Life Recovery & Purchase Shop Dialog
        if (isLifeShopOpen) {
            LifeShopDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openLifeShop(false) }
            )
        }

        // 21. Big Challenging Psychology Motivational Dialog (Every 5 Levels)
        activeChallengeDialog?.let { challenge ->
            ChallengePsychologyDialog(
                challenge = challenge,
                onAccept = { viewModel.dismissChallengeDialog() }
            )
        }
    }
}
