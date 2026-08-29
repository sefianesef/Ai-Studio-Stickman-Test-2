package com.mygames.stickmanrush.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.mygames.stickmanrush.model.GameState

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
    val isThemeSelectorOpen by viewModel.isThemeSelectorOpen.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val isNicknameSetupOpen by viewModel.isNicknameSetupOpen.collectAsState()
    
    // Auth & Launch Sequence
    val auth = FirebaseAuth.getInstance()
    var isUserAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
    var hasHandledLuckyWheelOnLaunch by remember { mutableStateOf(false) }

    // --- STRICT SEQUENTIAL LAUNCH FLOW ---
    LaunchedEffect(isUserAuthenticated, nickname) {
        if (!isUserAuthenticated) {
            // STEP 1: Login
            viewModel.openNicknameSetup(false)
        } else if (nickname.isBlank()) {
            // STEP 2: Nickname
            viewModel.openNicknameSetup(true)
        } else if (!hasHandledLuckyWheelOnLaunch) {
            // STEP 3: Lucky Wheel (First launch / Daily free spin)
            hasHandledLuckyWheelOnLaunch = true
            viewModel.openNicknameSetup(false)
            if (viewModel.isDailyFreeSpinAvailable()) {
                viewModel.openSpinWheel(true)
            }
        }
    }

    val isSpinWheelOpen by viewModel.isSpinWheelOpen.collectAsState()
    val isRealMoneyShopOpen by viewModel.isRealMoneyShopOpen.collectAsState()
    val isOutOfGemsOfferOpen by viewModel.isOutOfGemsOfferOpen.collectAsState()
    val isLifeShopOpen by viewModel.isLifeShopOpen.collectAsState()
    val isOutOfPlanksDialog by viewModel.isOutOfPlanksDialog.collectAsState()
    val activeChallengeDialog by viewModel.activeChallengeDialog.collectAsState()
    val activeLevelVictory by viewModel.activeLevelVictory.collectAsState()
    val levelVictoryCelebration by viewModel.engine.levelVictoryCelebration.collectAsState()
    val activeSecondChancePrompt by viewModel.engine.activeSecondChancePrompt.collectAsState()
    val secondChanceProgressPercent by viewModel.engine.secondChanceProgressPercent.collectAsState()

    val gameEngine = viewModel.engine

    LaunchedEffect(gameEngine) {
        gameEngine.onGemCollectedListener = { amount ->
            viewModel.onGemCollected(amount) 
        }
    }

    val isPlaying = gameState != GameState.START && gameState != GameState.GAMEOVER
    val isStart = gameState == GameState.START
    val isGameOver = gameState == GameState.GAMEOVER

    LaunchedEffect(Unit) {
        viewModel.soundManager.playStartupMelody()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06140E))
    ) {
        // 1. Core Physics & Canvas rendering
        StickmanGameCanvas(viewModel = viewModel)

        // 2. HUD Overlay
        AnimatedVisibility(
            visible = isPlaying,
            enter = fadeIn(animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing))
        ) {
            GameHud(viewModel = viewModel)
        }

        // 3. Start Screen Overlay
        AnimatedVisibility(
            visible = isStart,
            enter = fadeIn(animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing))
        ) {
            StartScreenOverlay(viewModel = viewModel)
        }

        // 4. Game Over Overlay
        AnimatedVisibility(
            visible = isGameOver,
            enter = fadeIn(animationSpec = tween(durationMillis = 450, delayMillis = 100, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 450, delayMillis = 100, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)) +
                   scaleOut(targetScale = 0.95f, animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing))
        ) {
            GameOverDialog(viewModel = viewModel)
        }

        // ==========================================
        // DIALOGS & OVERLAYS IN STRICT SEQUENCE
        // ==========================================

        // 1. STEP 1: AUTH DIALOG (Google / Email / Guest)
        if (!isUserAuthenticated) {
            AuthDialog(
                viewModel = viewModel,
                onDismiss = {
                    isUserAuthenticated = true
                }
            )
        }

        // 2. STEP 2: NICKNAME SETUP DIALOG
        if (isUserAuthenticated && isNicknameSetupOpen) {
            NicknameSetupDialog(
                viewModel = viewModel,
                onDismiss = {
                    viewModel.openNicknameSetup(false)
                    if (viewModel.isDailyFreeSpinAvailable()) {
                        viewModel.openSpinWheel(true)
                    }
                },
                isInitialSetup = nickname.isBlank()
            )
        }

        // 3. STEP 3: LUCKY SPIN WHEEL DIALOG
        if (isUserAuthenticated && !isNicknameSetupOpen && isSpinWheelOpen) {
            LuckySpinWheelDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.openSpinWheel(false) }
            )
        }

        // Secondary Menus (Triggered only when requested by user)
        if (isShopOpen) ShopDialog(viewModel = viewModel)
        if (isPauseMenuOpen) PauseMenuDialog(viewModel = viewModel)
        if (isHowToPlayOpen) HowToPlayDialog(onDismiss = { viewModel.openHowToPlay(false) })
        if (isDailyRewardOpen) DailyRewardDialog(viewModel = viewModel, onDismiss = { viewModel.openDailyReward(false) })
        if (isDailyMissionsOpen) RoyalMissionPursuitDialog(viewModel = viewModel, onDismiss = { viewModel.openDailyMissions(false) })
        if (isWeeklyMissionsOpen) WeeklyMissionsDialog(viewModel = viewModel, onDismiss = { viewModel.openWeeklyMissions(false) })
        if (isContestsOpen) RoyalContestCenterDialog(viewModel = viewModel, onDismiss = { viewModel.openContests(false) })
        if (isMainMenuOpen) ProfessionalMainMenuDialog(viewModel = viewModel, onDismiss = { viewModel.openMainMenu(false) })
        if (isPlayerStatsOpen) PlayerCareerStatsDialog(viewModel = viewModel, onDismiss = { viewModel.openPlayerStats(false) })
        if (isSettingsOpen) GameSettingsDialog(viewModel = viewModel, onDismiss = { viewModel.openSettings(false) })
        if (isLeaderboardOpen) LeaderboardAndContestDialog(viewModel = viewModel, onDismiss = { viewModel.openLeaderboard(false) })
        if (isRealMoneyShopOpen) RealMoneyGemShopDialog(viewModel = viewModel, onDismiss = { viewModel.openRealMoneyShop(false) })
        if (isOutOfGemsOfferOpen) OutOfGemsSpecialOfferDialog(viewModel = viewModel, onDismiss = { viewModel.openOutOfGemsOffer(false) })
        if (isLifeShopOpen) LifeShopDialog(viewModel = viewModel, onDismiss = { viewModel.openLifeShop(false) })
        if (isThemeSelectorOpen) EnvironmentThemeDialog(viewModel = viewModel, onDismiss = { viewModel.openThemeSelector(false) })

        // Level Victory Celebration
        levelVictoryCelebration?.let { celebrationText ->
            LevelVictoryCelebrationDialog(
                celebrationText = celebrationText,
                levelNumber = activeLevelVictory?.levelNumber,
                viewModel = viewModel,
                onDismiss = { viewModel.dismissLevelVictory() }
            )
        }

        // Challenge Dialog (Every 5 Levels)
        if (!activeSecondChancePrompt) {
            activeChallengeDialog?.let { challenge ->
                ChallengePsychologyDialog(
                    challenge = challenge,
                    onAccept = { viewModel.dismissChallengeDialog() }
                )
            }
        }

        // Second Chance Revive
        if (activeSecondChancePrompt) {
            SecondChanceReviveDialog(
                viewModel = viewModel,
                progressPercent = secondChanceProgressPercent,
                onRevive = { viewModel.acceptSecondChanceRevive() },
                onDecline = { viewModel.declineSecondChanceRevive() }
            )
        }

        // Out of Wood Planks
        if (isOutOfPlanksDialog) {
            OutOfWoodPlanksDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissOutOfPlanksDialog() }
            )
        }
    }
}
