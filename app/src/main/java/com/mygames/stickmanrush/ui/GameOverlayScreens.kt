package com.mygames.stickmanrush.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.mygames.stickmanrush.model.AccessoryItem
import com.mygames.stickmanrush.model.AccessoryType
import com.mygames.stickmanrush.model.CurrencyType
import com.mygames.stickmanrush.model.GemPack
import com.mygames.stickmanrush.model.GameState
import com.mygames.stickmanrush.model.ItemRarity
import com.mygames.stickmanrush.model.LeaderboardEntry
import com.mygames.stickmanrush.model.TournamentLeague
import kotlin.math.cos
import kotlin.math.sin

internal fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun GameHud(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val score by viewModel.engine.score.collectAsState()
    val currentLevel by viewModel.engine.currentLevel.collectAsState()
    val gems by viewModel.gems.collectAsState()
    val lives by viewModel.lives.collectAsState()
    val secondsUntilNextLife by viewModel.secondsUntilNextLife.collectAsState()
    val maxLives = viewModel.maxLives
    val currentStage by viewModel.engine.currentStage.collectAsState()
    val tier by viewModel.engine.difficultyTier.collectAsState()
    val gemCombo by viewModel.engine.gemCombo.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val gameState by viewModel.engine.gameState.collectAsState()
    val dailyMissions by viewModel.dailyMissions.collectAsState()
    val uncompletedClaimableCount = dailyMissions.count { it.currentProgress >= it.targetCount && !it.isClaimed }
    val firestoreWoodPlanks by viewModel.firestoreWoodPlanks.collectAsState()
    val firestoreCoins by viewModel.firestoreCoins.collectAsState()

    if (gameState == GameState.START) return

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            // TOP BAR: Left (Gems + Planks + Lives) | Center (Level & Stage) | Right (Daily/Weekly + Action Menu & Pause)
            // TOP BAR: Scrollable Row of Resource Counters & Action Buttons to prevent any overflow or clipping
            val hudScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hudScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gem Counter & Shop Button
                Surface(
                    onClick = { viewModel.openShop(true) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xCC0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6638BDF8)),
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(18.dp))
                        .testTag("hud_gem_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "💎", fontSize = 14.sp)
                        Text(
                            text = "$gems",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                // 🪵 WOOD PLANKS (Firebase Inventory slot_1)
                Surface(
                    onClick = { viewModel.addWoodPlanks(3) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xDD2D1810),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFB45309)),
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFFB45309))
                        .testTag("hud_wood_planks_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🪵", fontSize = 13.sp)
                        Text(
                            text = "$firestoreWoodPlanks",
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                // ❤️ LIVES COUNTER PILL (Clickable to replenish / buy lives)
                val regenMins = secondsUntilNextLife / 60
                val regenSecs = secondsUntilNextLife % 60
                val formattedHudTimer = String.format(java.util.Locale.US, "%02d:%02d", regenMins, regenSecs)

                Surface(
                    onClick = { viewModel.openLifeShop(true) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (lives <= 1) Color(0xFF7F1D1D) else Color(0xDD1E1B4B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (lives <= 1) Color(0xFFEF4444) else Color(0xFFF43F5E)
                    ),
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFFF43F5E))
                        .testTag("hud_lives_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (lives > 0) "❤️" else "💔",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$lives/$maxLives",
                            color = if (lives <= 1) Color(0xFFFDA4AF) else Color(0xFFFECDD3),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        if (lives < maxLives) {
                            Text(
                                text = "⏱️ $formattedHudTimer",
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        } else {
                            Text(
                                text = "+",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // 🎯 COLORFUL DAILY MISSIONS ICON with Claim Indicator Badge
                Surface(
                    onClick = { viewModel.openDailyMissions(true) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (uncompletedClaimableCount > 0) Color(0xFF047857) else Color(0xDD064E3B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (uncompletedClaimableCount > 0) Color(0xFF34D399) else Color(0xFF10B981)
                    ),
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF10B981))
                        .testTag("hud_daily_missions_colorful_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🎯", fontSize = 16.sp)
                        Text(
                            text = if (uncompletedClaimableCount > 0) "CLAIM" else "DAILY",
                            color = if (uncompletedClaimableCount > 0) Color(0xFFFDE047) else Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                        if (uncompletedClaimableCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                            )
                        }
                    }
                }

                // Level & Stage Indicator Pill
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xEE0B1329),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF38BDF8))
                        .testTag("stage_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🚩", fontSize = 13.sp)
                        Text(
                            text = "LV $currentLevel",
                            color = Color(0xFFF1F5F9),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 🏆 GLOBAL LEADERBOARD BUTTON
                Surface(
                    onClick = { viewModel.openLeaderboard(true) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xDD78350F),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFFFBBF24))
                        .testTag("hud_global_leaderboard_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🏆", fontSize = 14.sp)
                        Text(
                            text = "RANKS",
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // ⚡ COLORFUL WEEKLY MISSION BUTTON
                Surface(
                    onClick = { viewModel.openWeeklyMissions(true) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xDD312E81),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF818CF8)),
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF818CF8))
                        .testTag("hud_weekly_missions_colorful_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⚡", fontSize = 14.sp)
                        Text(
                            text = "WEEKLY",
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // 🎨 Theme Quick Toggle Button
                val currentHudTheme by viewModel.engine.currentStage.collectAsState()
                val currentHudThemeIcon = remember(currentHudTheme) { com.mygames.stickmanrush.game.StageThemes.getThemeIcon(currentHudTheme) }
                Surface(
                    onClick = { viewModel.openThemeSelector(true) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xCC1E1B4B),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFA855F7)),
                    modifier = Modifier.testTag("hud_theme_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(text = currentHudThemeIcon, fontSize = 13.sp)
                        Text(
                            text = "THEME",
                            color = Color(0xFFE9D5FF),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }

                // Master Game Menu Button
                IconButton(
                    onClick = { viewModel.openMainMenu(true) },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xCC1E293B), CircleShape)
                        .testTag("hud_main_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Game Menu",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Pause Button
                IconButton(
                    onClick = { viewModel.openPauseMenu(true) },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xCC1E293B), CircleShape)
                        .testTag("hud_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Score Display + Active Gem Combo Pill
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x33000000),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "$score",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .testTag("current_score_display")
                )
            }

            if (gemCombo > 1) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xDD065F46),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)),
                    modifier = Modifier.testTag("hud_gem_combo_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⚡", fontSize = 12.sp)
                        Text(
                            text = "${gemCombo}x GEM MULTIPLIER",
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 🎁 Dynamic 5-Level Milestone & Challenge Reward Tracker (Enlarged & High Visibility)
            val currentLevelNum = currentLevel.coerceAtLeast(1)
            val nextMilestone = if (currentLevelNum % 5 == 0) currentLevelNum else (((currentLevelNum - 1) / 5) + 1) * 5
            val currentBase = nextMilestone - 5
            val levelsInStage = currentLevelNum - currentBase
            val levelsRemaining = (nextMilestone - currentLevelNum).coerceAtLeast(0)
            val progress = (levelsInStage.toFloat() / 5f).coerceIn(0.1f, 1f)

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xEE0B1329),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B)),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFFF59E0B), spotColor = Color(0xFFF59E0B))
                    .testTag("hud_level_reward_tracker")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (levelsRemaining <= 1) "🔥" else "🏆",
                            fontSize = 18.sp
                        )
                        Text(
                            text = when {
                                levelsRemaining == 0 -> "👑 LEVEL $nextMilestone CHALLENGE REACHED! CONQUER IT!"
                                levelsRemaining == 1 -> "⚡ YOU ARE JUST 1 LEVEL AWAY FROM LEVEL $nextMilestone!"
                                else -> "🎯 LEVEL $nextMilestone CHALLENGE: $levelsRemaining BRIDGES TO GO!"
                            },
                            color = if (levelsRemaining <= 1) Color(0xFFFDE047) else Color(0xFFFBBF24),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Large High-Visibility Milestone Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Lv $currentLevelNum",
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .background(Color(0x55334155), CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF38BDF8))
                                        ),
                                        CircleShape
                                    )
                            )
                        }
                        Text(
                            text = "Lv $nextMilestone 🎁",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // BOTTOM ACTION CONTROLS: Jump & Flip during WALKING phase
    if (gameState == GameState.WALKING) {
        val isUpsideDown = viewModel.engine.isUpsideDown
        val isJumping = viewModel.engine.isJumping

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FLIP BUTTON (Left)
            Surface(
                onClick = { viewModel.engine.triggerFlip() },
                shape = RoundedCornerShape(22.dp),
                color = if (isUpsideDown) Color(0xDD0284C7) else Color(0xDD0F172A),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (isUpsideDown) Color(0xFF38BDF8) else Color(0xFF0284C7)
                ),
                modifier = Modifier
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color(0xFF38BDF8))
                    .testTag("hud_flip_action_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = if (isUpsideDown) "⬆️" else "🔄", fontSize = 18.sp)
                    Text(
                        text = if (isUpsideDown) "UPRIGHT" else "FLIP",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // JUMP BUTTON (Right)
            Surface(
                onClick = { viewModel.engine.triggerJump() },
                shape = RoundedCornerShape(22.dp),
                color = if (isJumping) Color(0xDD0284C7) else Color(0xDD0F172A),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (isJumping) Color(0xFFFDE047) else Color(0xFF38BDF8)
                ),
                modifier = Modifier
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color(0xFF38BDF8))
                    .testTag("hud_jump_action_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🦘", fontSize = 20.sp)
                    Text(
                        text = "JUMP",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun StartScreenOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val highScore by viewModel.highScore.collectAsState()
    val gems by viewModel.gems.collectAsState()
    val lives by viewModel.lives.collectAsState()
    val maxLives = viewModel.maxLives
    val secondsUntilNextLife by viewModel.secondsUntilNextLife.collectAsState()
    val savedLevel by viewModel.savedLevel.collectAsState()
    val highestUnlockedLevel by viewModel.highestUnlockedLevel.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val isDailyRewardAvailable by viewModel.isDailyRewardAvailable.collectAsState()
    val firestoreWoodPlanks by viewModel.firestoreWoodPlanks.collectAsState()
    val firestoreCoins by viewModel.firestoreCoins.collectAsState()
    val firestoreIsSynced by viewModel.firestoreIsSynced.collectAsState()

    // Play pleasant startup welcome melody when start screen first mounts
    LaunchedEffect(Unit) {
        viewModel.soundManager.playStartupMelody()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val streakGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streakGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x9906140E))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar: Royal Resource Counters (Scrollable horizontally to prevent any right-edge overflow or clipping)
            val topBarScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(topBarScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val nickname by viewModel.nickname.collectAsState()
                val playerLevel = 1 + highScore / 50

                Surface(
                    onClick = { viewModel.openPlayerStats(true) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("start_profile_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🥷", fontSize = 14.sp)
                        Column {
                            Text(
                                text = if (nickname.isNotBlank()) nickname else "Set Nickname",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Lvl $playerLevel",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Gold Coins / Gems Counter
                Surface(
                    onClick = { viewModel.openRealMoneyShop(true) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xEE064E3B),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF34D399)),
                    modifier = Modifier.testTag("start_gems_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "💰", fontSize = 13.sp)
                        Text(
                            text = "$gems",
                            color = Color(0xFFFEF08A),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "+",
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(Color(0xFF047857), CircleShape)
                                .padding(horizontal = 3.dp)
                        )
                    }
                }

                // 🪵 Wood Planks
                Surface(
                    onClick = { viewModel.addWoodPlanks(3) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xDD2D1810),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFB45309)),
                    modifier = Modifier.testTag("start_wood_planks_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🪵", fontSize = 12.sp)
                        Text(
                            text = "$firestoreWoodPlanks",
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                // Lives Regen Pill
                val regenMinutes = secondsUntilNextLife / 60
                val regenSeconds = secondsUntilNextLife % 60
                val formattedTimer = String.format(java.util.Locale.US, "%02d:%02d", regenMinutes, regenSeconds)

                Surface(
                    onClick = { viewModel.openLifeShop(true) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (lives == 0) Color(0xFF881337) else Color(0xFF831843),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, if (lives == 0) Color(0xFFF43F5E) else Color(0xFFFB7185)),
                    modifier = Modifier.testTag("start_lives_regen_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = if (lives > 0) "❤️" else "💔", fontSize = 12.sp)
                        Text(
                            text = "$lives/$maxLives",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        if (lives < maxLives) {
                            Text(
                                text = formattedTimer,
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        } else {
                            Text(
                                text = "FULL",
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Stars Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1B4B),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFBBF24)),
                    modifier = Modifier.testTag("start_stars_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⭐", fontSize = 12.sp)
                        Text(
                            text = "${highScore.coerceAtLeast(364)}",
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                // Settings Gear
                IconButton(
                    onClick = { viewModel.openSettings(true) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .border(1.dp, Color(0xFF64748B), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            // Middle Area with Dynamic Side Contest & Mission Stickers (Matching Video Left/Right Layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE CONTEST & SPIN STICKERS
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 0. Lucky Spin Badge (1 Free Spin per Day)
                    val isDailySpinReady = remember { viewModel.isDailyFreeSpinAvailable() }
                    val adSpinsCount by viewModel.adEarnedSpins.collectAsState()
                    Surface(
                        onClick = { viewModel.openSpinWheel(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF4C1D95),
                        border = androidx.compose.foundation.BorderStroke(2.dp, if (isDailySpinReady) Color(0xFFFFD700) else Color(0xFFA78BFA)),
                        modifier = Modifier
                            .size(width = 62.dp, height = 68.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = Color(0xFFA855F7))
                            .testTag("start_lucky_spin_button")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF7C3AED), Color(0xFF4C1D95))
                                    )
                                )
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "🎰", fontSize = 22.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isDailySpinReady) Color(0xFF047857) else Color(0xFF312E81)
                            ) {
                                Text(
                                    text = if (isDailySpinReady) "FREE 1x" else if (adSpinsCount > 0) "+$adSpinsCount Spin" else "SPIN",
                                    color = if (isDailySpinReady) Color(0xFFFEF08A) else Color.White,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    // 1. King's Cup Badge
                    Surface(
                        onClick = { viewModel.openContests(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF854D0E),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFEF08A)),
                        modifier = Modifier
                            .size(width = 62.dp, height = 68.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFEAB308), Color(0xFF854D0E))
                                    )
                                )
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "🏆", fontSize = 22.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF451A03)
                            ) {
                                Text(
                                    text = "15:31:24",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    // 2. Lightning Rush Badge
                    Surface(
                        onClick = { viewModel.openContests(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF4338CA),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFA5B4FC)),
                        modifier = Modifier
                            .size(width = 62.dp, height = 68.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF312E81))
                                    )
                                )
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "⚡", fontSize = 22.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E1B4B)
                            ) {
                                Text(
                                    text = "2d 15h",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    // 3. Champions Clash Badge
                    Surface(
                        onClick = { viewModel.openContests(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFBE185D),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFBCFE8)),
                        modifier = Modifier
                            .size(width = 62.dp, height = 70.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFEC4899), Color(0xFF831843))
                                    )
                                )
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "👑", fontSize = 24.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF047857)
                            ) {
                                Text(
                                    text = "Finished",
                                    color = Color(0xFFFEF08A),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }

                // CENTER: Title & Play Button Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    // Logo Icon with Hero Stickman
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF092B20),
                        border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFF34D399)),
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Color(0xFF10B981))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.mygames.stickmanrush.R.drawable.ic_launcher_foreground),
                                contentDescription = "Stickman Rush Hero Icon",
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }

                    // Title: STICKMAN RUSH
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("app_title_text")
                    ) {
                        Text(
                            text = "STICKMAN",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = 3.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "RUSH",
                                color = Color(0xFFF43F5E),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                letterSpacing = 4.sp
                            )
                            Text(
                                text = "⚡",
                                fontSize = 20.sp
                            )
                        }
                    }

                    // Real-Money Gem Shop Feature Card
                    Surface(
                        onClick = { viewModel.openRealMoneyShop(true) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F2942),
                        border = androidx.compose.foundation.BorderStroke(1.8.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(16.dp))
                            .testTag("start_buy_gems_shop_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "💎", fontSize = 24.sp)
                                Column {
                                    Text(
                                        text = "BUY GEMS SHOP",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Instant +3000 Gems",
                                        color = Color(0xFF7DD3FC),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = "BUY",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Play & Checkpoint Continue Card
                    val resumeLevel = savedLevel.coerceAtLeast(1)
                    val checkpoints = remember(highestUnlockedLevel) { viewModel.getUnlockedCheckpoints() }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (lives > 0) {
                                    viewModel.startGameFromCheckpoint(resumeLevel)
                                } else {
                                    viewModel.openLifeShop(true)
                                }
                            },
                            shape = RoundedCornerShape(22.dp),
                            color = if (lives > 0) Color(0xFF16A34A) else Color(0xFF7F1D1D),
                            border = androidx.compose.foundation.BorderStroke(3.dp, if (lives > 0) Color(0xFFFEF08A) else Color(0xFFEF4444)),
                            modifier = Modifier
                                .scale(pulseScale)
                                .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Color(0xFF22C55E))
                                .testTag("start_play_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (lives > 0) Icons.Default.PlayArrow else Icons.Default.FavoriteBorder,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = if (lives > 0) {
                                        if (resumeLevel > 1) "Resume Lv $resumeLevel ▶" else "Level 1 Play ▶"
                                    } else {
                                        "Out of Lives! (Refill ❤️)"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // 🚩 Checkpoint Quick Selector (If player unlocked higher checkpoints)
                        if (checkpoints.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚩 Checkpoints:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                checkpoints.takeLast(4).forEach { cp ->
                                    Surface(
                                        onClick = {
                                            if (lives > 0) {
                                                viewModel.startGameFromCheckpoint(cp)
                                            } else {
                                                viewModel.openLifeShop(true)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (resumeLevel == cp) Color(0xFF2563EB) else Color(0xFF1E293B),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (resumeLevel == cp) Color(0xFF93C5FD) else Color(0xFF475569)
                                        )
                                    ) {
                                        Text(
                                            text = "Lv $cp",
                                            color = if (resumeLevel == cp) Color.White else Color(0xFFCBD5E1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Wardrobe Shop & Realm Themes Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Wardrobe Shop Card
                        Surface(
                            onClick = { viewModel.openShop(true) },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E1B4B),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7)),
                            modifier = Modifier
                                .weight(1f)
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .testTag("start_wardrobe_shop_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "🥋", fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = "WARDROBE",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Skins & Bridges",
                                        color = Color(0xFFD8B4FE),
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        }

                        // 2. Realm Themes Card
                        val currentStartStage by viewModel.engine.currentStage.collectAsState()
                        val currentStartIcon = remember(currentStartStage) { com.mygames.stickmanrush.game.StageThemes.getThemeIcon(currentStartStage) }
                        Surface(
                            onClick = { viewModel.openThemeSelector(true) },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                            modifier = Modifier
                                .weight(1f)
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .testTag("start_environment_themes_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = currentStartIcon, fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = "REALM THEMES",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "13 Environments",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT SIDE CONTEST STICKERS (Mission Pursuit, Anvil Pass, Sky Race, Endless Treasure)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Mission Pursuit Clue Badge
                    Surface(
                        onClick = { viewModel.openDailyMissions(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0369A1),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF7DD3FC)),
                        modifier = Modifier
                            .size(width = 62.dp, height = 70.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF075985))
                                    )
                                )
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "🔍", fontSize = 24.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0C4A6E)
                            ) {
                                Text(
                                    text = "Missions",
                                    color = Color(0xFFFEF08A),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    // 2. Anvil / Holiday Pass Badge
                    Surface(
                        onClick = { viewModel.openWeeklyMissions(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFD97706),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFEF08A)),
                        modifier = Modifier
                            .size(width = 62.dp, height = 70.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                                    )
                                )
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "🪙", fontSize = 24.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF451A03)
                            ) {
                                Text(
                                    text = "Pass 5d",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    // 3. Endless Treasure Chest Badge
                    Surface(
                        onClick = { viewModel.openDailyReward(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF047857),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF6EE7B7)),
                        modifier = Modifier
                            .size(width = 62.dp, height = 70.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF10B981), Color(0xFF064E3B))
                                    )
                                )
                                .padding(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "🎁", fontSize = 24.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF064E3B)
                            ) {
                                Text(
                                    text = "Free Gift",
                                    color = Color(0xFFFEF08A),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Bar (Matching Video 5-Tab Bar: Events, Leaderboard, Home Castle, Team, Collection)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Events / Contests
                    BottomRoyalNavTab(
                        icon = "⭐",
                        label = "Events",
                        badge = "!",
                        isSelected = false,
                        onClick = { viewModel.openContests(true) }
                    )

                    // 2. Leaderboard / Trophy
                    BottomRoyalNavTab(
                        icon = "🏆",
                        label = "Ranks",
                        badge = null,
                        isSelected = false,
                        onClick = { viewModel.openLeaderboard(true) }
                    )

                    // 3. Home Castle (Center active tab)
                    BottomRoyalNavTab(
                        icon = "🏰",
                        label = "Home",
                        badge = null,
                        isSelected = true,
                        onClick = { }
                    )

                    // 4. Team / Friends
                    BottomRoyalNavTab(
                        icon = "👥",
                        label = "Team",
                        badge = "1",
                        isSelected = false,
                        onClick = { viewModel.openContests(true) }
                    )

                    // 5. Card / Sticker Collection
                    BottomRoyalNavTab(
                        icon = "🃏",
                        label = "Cards",
                        badge = null,
                        isSelected = false,
                        onClick = { viewModel.openDailyMissions(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomRoyalNavTab(
    icon: String,
    label: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF0284C7) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFBAE6FD)) else null
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(text = icon, fontSize = if (isSelected) 22.sp else 18.sp)
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                )
            }

            if (badge != null) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = badge, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SecondChanceReviveDialog(
    viewModel: GameViewModel,
    progressPercent: Int,
    onRevive: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    var countdownSeconds by remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            countdownSeconds--
        }
        if (countdownSeconds <= 0) {
            onDecline()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "second_chance_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xDD000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(3.dp, Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFF59E0B)))),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .scale(pulseScale)
                .shadow(32.dp, RoundedCornerShape(28.dp), ambientColor = Color(0xFFF59E0B), spotColor = Color(0xFFEF4444))
                .testTag("second_chance_revive_dialog")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF31101E),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Urgent Warning Icon with Countdown Ring
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF7F1D1D),
                        border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFF87171)),
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(16.dp, CircleShape, ambientColor = Color(0xFFEF4444))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "$countdownSeconds", color = Color(0xFFFDE047), fontSize = 36.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "SO CLOSE! (${progressPercent}% TRACK REACHED) 🏃‍♂️💨",
                        color = Color(0xFFFDE047),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SECOND-CHANCE REVIVE",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Resume immediately at the edge of your last platform with your full combo intact!",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // 1-Tap Rewarded Ad Revive Button
                Button(
                    onClick = onRevive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF10B981))
                        .testTag("second_chance_watch_ad_revive_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🎬", fontSize = 20.sp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "WATCH AD TO REVIVE (FREE)",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "1 Per Run • Preserves All Progress",
                                color = Color(0xFFD1FAE5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // No Thanks Button
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("second_chance_decline_button")
                ) {
                    Text(
                        text = "NO THANKS / GIVE UP",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val score by viewModel.engine.score.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val gemsRun by viewModel.engine.gemsCollectedRun.collectAsState()
    val totalGems by viewModel.gems.collectAsState()
    val lives by viewModel.lives.collectAsState()
    val savedLevel by viewModel.savedLevel.collectAsState()
    val isNewHigh by viewModel.engine.isNewHighScore.collectAsState()
    val lastNearMiss by viewModel.engine.lastNearMiss.collectAsState()
    val revivalsUsed by viewModel.engine.revivalsUsed.collectAsState()
    val reviveCost = viewModel.engine.getReviveCost()
    val canAffordRevive = totalGems >= reviveCost
    val firestoreWoodPlanks by viewModel.firestoreWoodPlanks.collectAsState()
    val firestoreCoins by viewModel.firestoreCoins.collectAsState()

    // Urgency countdown timer for second chance loss aversion
    var countdownSeconds by remember { mutableIntStateOf(7) }
    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            countdownSeconds--
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xBB000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF334155)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(24.dp, RoundedCornerShape(28.dp))
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Text(
                    text = "GAME OVER",
                    color = Color(0xFFEF4444),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                if (isNewHigh) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF59E0B)
                    ) {
                        Text(
                            text = "🎉 NEW HIGH SCORE! 🎉",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                }

                // Psychological Near-Miss Dopamine Trigger
                lastNearMiss?.let { nm ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF881337).copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFB7185)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("game_over_near_miss_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "🔥", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = "SO CLOSE! ALMOST MADE IT!",
                                    color = Color(0xFFFDA4AF),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = nm.message,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Stats Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Score", color = Color(0xFF94A3B8), fontSize = 16.sp)
                            Text(
                                "$score",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Divider(color = Color(0xFF334155))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Best High Score", color = Color(0xFF94A3B8), fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "$highScore",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("☁️", fontSize = 12.sp)
                            }
                        }

                        Divider(color = Color(0xFF334155))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wood Planks Left", color = Color(0xFF94A3B8), fontSize = 15.sp)
                            Text(
                                "$firestoreWoodPlanks 🪵",
                                color = Color(0xFFFDE68A),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(color = Color(0xFF334155))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Firestore Coins", color = Color(0xFF94A3B8), fontSize = 15.sp)
                            Text(
                                "$firestoreCoins 🪙",
                                color = Color(0xFFFEF08A),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(color = Color(0xFF334155))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gems Collected", color = Color(0xFF94A3B8), fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "+$gemsRun 💎",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Second Chance Revive Button (Psychological Loss Aversion & Urgent Escalation)
                if (score >= 1 && countdownSeconds > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (canAffordRevive) {
                            Button(
                                onClick = { viewModel.revivePlayer() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("game_over_revive_button")
                            ) {
                                Text(text = "✨", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "REVIVE & SAVE SCORE ($reviveCost 💎) • ${countdownSeconds}s",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.openOutOfGemsOffer(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF831843)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF43F5E)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("game_over_revive_buy_gems_button")
                            ) {
                                Text(text = "💎", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NEED ${reviveCost - totalGems} GEMS TO REVIVE • GET GEMS ⚡",
                                    color = Color(0xFFFDE047),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Text(
                            text = if (revivalsUsed == 0) "First Revival in this run" else "Revivals used: $revivalsUsed (Cost scales: 5 → 15 → 35 → 75 💎)",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Play Again / Checkpoint Retry / Life Restock Button
                val checkpointLevel = savedLevel.coerceAtLeast(1)
                if (lives > 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startGameFromCheckpoint(checkpointLevel) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("game_over_retry_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (checkpointLevel > 1) "CONTINUE LV $checkpointLevel ($lives ❤️ LEFT)" else "PLAY AGAIN ($lives ❤️ LEFT)",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        if (checkpointLevel > 1) {
                            OutlinedButton(
                                onClick = { viewModel.startGameFromCheckpoint(1) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("game_over_restart_level_1_button")
                            ) {
                                Text("Restart from Level 1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.openLifeShop(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDA4AF)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("game_over_out_of_lives_button")
                    ) {
                        Text(text = "💔", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "OUT OF LIVES • GET LIVES TO RETRY ⚡",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "5 Lives limit reached. Buy lives or restart from Level 1!",
                        color = Color(0xFFFDA4AF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                // Secondary Action Row (Themes, Ranks, Shop, Menu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.openThemeSelector(true) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA855F7)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("game_over_theme_button")
                    ) {
                        Text(text = "🎨 Theme", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { viewModel.openLeaderboard(true) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("game_over_leaderboard_button")
                    ) {
                        Text(text = "🏆 Rank", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { viewModel.openShop(true) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("game_over_shop_button")
                    ) {
                        Text(text = "🥋 Shop", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { viewModel.engine.resetGame(initial = true) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("game_over_home_button")
                    ) {
                        Text(text = "🏠 Menu", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyOverlayShopDialog(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedShopTab.collectAsState()
    val gems by viewModel.gems.collectAsState()
    val blueGems by viewModel.blueGems.collectAsState()
    val redGems by viewModel.redGems.collectAsState()
    val shopCurrencyFilter by viewModel.shopCurrencyFilter.collectAsState()
    val selectedHatId by viewModel.selectedHatId.collectAsState()
    val selectedScarfId by viewModel.selectedScarfId.collectAsState()
    val selectedStickId by viewModel.selectedStickId.collectAsState()
    val selectedSkinId by viewModel.selectedSkinId.collectAsState()
    val selectedThemeId by viewModel.selectedThemeId.collectAsState()
    val isDailyAvailable by viewModel.isDailyRewardAvailable.collectAsState()
    val rentedSkinId by viewModel.rentedSkinId.collectAsState()
    val rentedSkinRuns by viewModel.rentedSkinRunsRemaining.collectAsState()
    val rentedStickId by viewModel.rentedStickId.collectAsState()
    val rentedStickRuns by viewModel.rentedStickRunsRemaining.collectAsState()

    // Temporary previewed item for interactive top showcase
    var previewedItem by remember(selectedTab) {
        val initialPreview = when (selectedTab) {
            AccessoryType.HAT -> viewModel.getEquippedHat()
            AccessoryType.SCARF -> viewModel.getEquippedScarf()
            AccessoryType.STICK -> viewModel.getEquippedStick()
            AccessoryType.BODY_SKIN -> viewModel.getEquippedSkin()
            AccessoryType.THEME -> viewModel.getEquippedTheme()
            AccessoryType.GEM_VAULT -> null
        }
        mutableStateOf<AccessoryItem?>(initialPreview)
    }

    val filteredItems = remember(selectedTab, shopCurrencyFilter, viewModel.availableAccessories) {
        viewModel.availableAccessories.filter { item ->
            val matchesTab = item.type == selectedTab
            val matchesCurrency = when (shopCurrencyFilter) {
                "ALL" -> true
                "STANDARD" -> item.currencyType == CurrencyType.GEM
                "CONTEST_BLUE" -> item.currencyType == CurrencyType.BLUE_GEM
                "TOURNAMENT_RED" -> item.currencyType == CurrencyType.RED_GEM
                else -> true
            }
            matchesTab && matchesCurrency
        }
    }

    // Dynamic animation time for preview showcase
    val infiniteTransition = rememberInfiniteTransition(label = "shop_preview_trans")
    val previewTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shop_preview_time"
    )

    Dialog(
        onDismissRequest = { viewModel.openShop(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF070D1E),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1E293B)),
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("shop_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. Top Header Bar: Title, Gem Wallets, Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "👑", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "HERO WARDROBE",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Customize Outfits & Bridges",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Standard Gem Balance Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("shop_gem_balance")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(text = "💎", fontSize = 12.sp)
                                Text(
                                    text = "$gems",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Blue Gems (Contest)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0C2A4D),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.testTag("shop_blue_gems_balance")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(text = "🔷", fontSize = 12.sp)
                                Text(
                                    text = "$blueGems",
                                    color = Color(0xFF7DD3FC),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Red Gems (Tournament)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4C0519),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFB7185)),
                            modifier = Modifier.testTag("shop_red_gems_balance")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(text = "🔴", fontSize = 12.sp)
                                Text(
                                    text = "$redGems",
                                    color = Color(0xFFFDA4AF),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Close Dialog Button
                        IconButton(
                            onClick = { viewModel.openShop(false) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .testTag("shop_close_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Interactive Live Hero Showcase Stage
                val currentPreview = previewedItem ?: filteredItems.firstOrNull()
                if (currentPreview != null) {
                    val activeHat = if (selectedTab == AccessoryType.HAT) currentPreview else viewModel.getEquippedHat()
                    val activeScarf = if (selectedTab == AccessoryType.SCARF) currentPreview else viewModel.getEquippedScarf()
                    val activeStick = if (selectedTab == AccessoryType.STICK) currentPreview else viewModel.getEquippedStick()
                    val activeSkin = if (selectedTab == AccessoryType.BODY_SKIN) currentPreview else viewModel.getEquippedSkin()

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0B132B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(currentPreview.rarity.badgeBgHex)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .testTag("shop_hero_showcase")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live Preview Canvas with Stickman & Bridge
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(currentPreview.primaryColor).copy(alpha = 0.35f),
                                                Color(0xFF0F172A)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color(currentPreview.primaryColor).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val centerX = size.width * 0.45f
                                    val groundY = size.height * 0.78f
                                    val headRadius = 6.dp.toPx()
                                    val headY = groundY - 24.dp.toPx()
                                    val neckY = groundY - 18.dp.toPx()
                                    val hipY = groundY - 9.dp.toPx()
                                    val bodyCol = Color(activeSkin.primaryColor)

                                    // Bridge visual preview next to stickman
                                    val bridgeCol = Color(activeStick.primaryColor)
                                    drawLine(
                                        color = bridgeCol,
                                        start = Offset(centerX + 16.dp.toPx(), groundY),
                                        end = Offset(centerX + 16.dp.toPx(), groundY - 30.dp.toPx()),
                                        strokeWidth = 3.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.dp.toPx(),
                                        center = Offset(centerX + 16.dp.toPx(), groundY - 30.dp.toPx())
                                    )

                                    // Cape
                                    val flutter = (sin((previewTime * 4f).toDouble()).toFloat()) * 2.5.dp.toPx()
                                    drawLine(
                                        color = Color(activeScarf.primaryColor),
                                        start = Offset(centerX - 1.dp.toPx(), neckY),
                                        end = Offset(centerX - 9.dp.toPx(), neckY + 12.dp.toPx() + flutter),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Torso
                                    drawLine(
                                        color = bodyCol,
                                        start = Offset(centerX, neckY),
                                        end = Offset(centerX, hipY),
                                        strokeWidth = 2.8.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Legs
                                    drawLine(
                                        color = bodyCol,
                                        start = Offset(centerX, hipY),
                                        end = Offset(centerX - 3.5.dp.toPx(), groundY),
                                        strokeWidth = 2.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = bodyCol,
                                        start = Offset(centerX, hipY),
                                        end = Offset(centerX + 3.5.dp.toPx(), groundY),
                                        strokeWidth = 2.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Arms
                                    drawLine(
                                        color = bodyCol,
                                        start = Offset(centerX, neckY + 2.dp.toPx()),
                                        end = Offset(centerX + 7.dp.toPx(), neckY + 8.dp.toPx()),
                                        strokeWidth = 2.2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = bodyCol,
                                        start = Offset(centerX, neckY + 2.dp.toPx()),
                                        end = Offset(centerX - 5.dp.toPx(), neckY + 7.dp.toPx()),
                                        strokeWidth = 2.2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Head
                                    drawCircle(
                                        color = bodyCol,
                                        radius = headRadius,
                                        center = Offset(centerX, headY)
                                    )
                                    // Eye
                                    drawCircle(
                                        color = Color.Black,
                                        radius = 1.dp.toPx(),
                                        center = Offset(centerX + 2.5.dp.toPx(), headY - 1.dp.toPx())
                                    )

                                    // Hat
                                    val hatCol = Color(activeHat.primaryColor)
                                    drawRoundRect(
                                        color = hatCol,
                                        topLeft = Offset(centerX - headRadius - 1.dp.toPx(), headY - headRadius * 0.5f),
                                        size = Size((headRadius * 2f) + 2.dp.toPx(), 3.dp.toPx()),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Item Lore & Rarity Specs
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Rarity Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(currentPreview.rarity.badgeBgHex)
                                    ) {
                                        Text(
                                            text = currentPreview.rarity.label,
                                            color = Color(currentPreview.rarity.colorHex),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (currentPreview.isContestExclusive) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (currentPreview.currencyType == CurrencyType.BLUE_GEM) Color(0xFF0284C7) else Color(0xFFE11D48)
                                        ) {
                                            Text(
                                                text = if (currentPreview.currencyType == CurrencyType.BLUE_GEM) "🔷 CONTEST" else "🔴 TOURNEY",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${currentPreview.iconSymbol} PREVIEWING",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = currentPreview.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )

                                Text(
                                    text = currentPreview.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Category Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        AccessoryType.BODY_SKIN to ("🦸 Skins" to viewModel.availableAccessories.count { it.type == AccessoryType.BODY_SKIN }),
                        AccessoryType.STICK to ("🥢 Bridges" to viewModel.availableAccessories.count { it.type == AccessoryType.STICK }),
                        AccessoryType.HAT to ("👑 Hats" to viewModel.availableAccessories.count { it.type == AccessoryType.HAT }),
                        AccessoryType.SCARF to ("🧣 Capes" to viewModel.availableAccessories.count { it.type == AccessoryType.SCARF }),
                        AccessoryType.THEME to ("🌌 Backgrounds" to viewModel.availableAccessories.count { it.type == AccessoryType.THEME }),
                        AccessoryType.GEM_VAULT to ("💎 Vault" to viewModel.availableGemPacks.size)
                    )

                    tabs.forEach { (type, info) ->
                        val (label, count) = info
                        val isSelected = selectedTab == type
                        Surface(
                            onClick = {
                                viewModel.setShopTab(type)
                                if (type != AccessoryType.GEM_VAULT) {
                                    previewedItem = when (type) {
                                        AccessoryType.HAT -> viewModel.getEquippedHat()
                                        AccessoryType.SCARF -> viewModel.getEquippedScarf()
                                        AccessoryType.STICK -> viewModel.getEquippedStick()
                                        AccessoryType.BODY_SKIN -> viewModel.getEquippedSkin()
                                        AccessoryType.THEME -> viewModel.getEquippedTheme()
                                        else -> null
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("shop_tab_${type.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (type == AccessoryType.GEM_VAULT) "💎 Vault" else if (type == AccessoryType.THEME) "🌌 Bg" else label.split(" ")[0],
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Currency Filter Sub-bar (for filtering accessories by Gems vs Contest Blue Gems vs Tournament Red Gems)
                if (selectedTab != AccessoryType.GEM_VAULT) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filterOptions = listOf(
                            "ALL" to "All",
                            "STANDARD" to "💎 Standard",
                            "CONTEST_BLUE" to "🔷 Blue Gems",
                            "TOURNAMENT_RED" to "🔴 Red Gems"
                        )
                        filterOptions.forEach { (filterKey, filterLabel) ->
                            val isFilterSelected = shopCurrencyFilter == filterKey
                            Surface(
                                onClick = { viewModel.setShopCurrencyFilter(filterKey) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isFilterSelected) Color(0xFF3B82F6) else Color(0xFF1E293B),
                                border = if (isFilterSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .testTag("shop_currency_filter_${filterKey.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = filterLabel,
                                        color = if (isFilterSelected) Color.White else Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        fontWeight = if (isFilterSelected) FontWeight.Black else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Content Area: Gem Vault OR Accessories Grid
                if (selectedTab == AccessoryType.GEM_VAULT) {
                    GemVaultContent(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Items Grid using LazyVerticalGrid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 142.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shop_items_grid")
                    ) {
                    items(filteredItems) { item ->
                        val isUnlocked = viewModel.isItemUnlocked(item.id)
                        val isEquipped = when (item.type) {
                            AccessoryType.HAT -> selectedHatId == item.id
                            AccessoryType.SCARF -> selectedScarfId == item.id
                            AccessoryType.STICK -> selectedStickId == item.id
                            AccessoryType.BODY_SKIN -> selectedSkinId == item.id
                            AccessoryType.THEME -> selectedThemeId == item.id
                            AccessoryType.GEM_VAULT -> false
                        }
                        val isPreviewSelected = previewedItem?.id == item.id
                        val canAfford = when (item.currencyType) {
                            CurrencyType.GEM -> gems >= item.cost
                            CurrencyType.BLUE_GEM -> blueGems >= item.cost
                            CurrencyType.RED_GEM -> redGems >= item.cost
                        }
                        val curSymbol = when (item.currencyType) {
                            CurrencyType.GEM -> "💎"
                            CurrencyType.BLUE_GEM -> "🔷"
                            CurrencyType.RED_GEM -> "🔴"
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                isEquipped -> Color(0xFF1E3A8A).copy(alpha = 0.85f)
                                isPreviewSelected -> Color(0xFF1E293B)
                                else -> Color(0xFF0F172A)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isEquipped || isPreviewSelected) 1.5.dp else 1.dp,
                                color = when {
                                    isEquipped -> Color(0xFF38BDF8)
                                    isPreviewSelected -> Color(item.rarity.colorHex)
                                    else -> Color(0xFF1E293B)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    previewedItem = item
                                    viewModel.soundManager.playButton()
                                }
                                .testTag("shop_item_card_${item.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // Top Row: Rarity Tag & Status Dot
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(item.rarity.badgeBgHex)
                                    ) {
                                        Text(
                                            text = item.rarity.label,
                                            color = Color(item.rarity.colorHex),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    if (item.isContestExclusive) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (item.currencyType == CurrencyType.BLUE_GEM) Color(0xFF0369A1) else Color(0xFF9F1239)
                                        ) {
                                            Text(
                                                text = if (item.currencyType == CurrencyType.BLUE_GEM) "🔷 CONTEST" else "🔴 TOURNEY",
                                                color = Color.White,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    if (isEquipped) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF38BDF8)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = Color.Black,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                // Glowing Accessory Icon Orb
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(item.primaryColor).copy(alpha = 0.5f),
                                                    Color(0xFF0F172A)
                                                )
                                            )
                                        )
                                        .border(1.5.dp, Color(item.primaryColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = item.iconSymbol, fontSize = 22.sp)
                                }

                                // Item Name
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )

                                // Lore Description
                                Text(
                                    text = item.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 13.sp,
                                    maxLines = 2,
                                    modifier = Modifier.height(26.dp)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Buy / Equip Action Button
                                if (!isUnlocked && item.realMoneyPriceUsd.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Gem Buy Button
                                        Button(
                                            onClick = {
                                                previewedItem = item
                                                viewModel.buyOrEquip(item)
                                            },
                                            enabled = canAfford,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = when (item.currencyType) {
                                                    CurrencyType.GEM -> Color(0xFFF59E0B)
                                                    CurrencyType.BLUE_GEM -> Color(0xFF0284C7)
                                                    CurrencyType.RED_GEM -> Color(0xFFE11D48)
                                                },
                                                disabledContainerColor = Color(0xFF1E293B)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("shop_item_btn_${item.id}")
                                        ) {
                                            Text(
                                                text = if (canAfford) "$curSymbol ${item.cost}" else "NEED $curSymbol",
                                                color = if (canAfford) Color.White else Color(0xFF64748B),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.5.sp
                                            )
                                        }

                                        // Real Money Instant Buy Button
                                        Button(
                                            onClick = {
                                                previewedItem = item
                                                viewModel.buyItemRealMoney(item)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF16A34A)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("shop_item_real_money_btn_${item.id}")
                                        ) {
                                            Text(
                                                text = item.realMoneyPriceUsd,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            previewedItem = item
                                            viewModel.buyOrEquip(item)
                                        },
                                        enabled = isUnlocked || canAfford,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                isEquipped -> Color(0xFF38BDF8)
                                                isUnlocked -> Color(0xFF10B981)
                                                canAfford -> when (item.currencyType) {
                                                    CurrencyType.GEM -> Color(0xFFF59E0B)
                                                    CurrencyType.BLUE_GEM -> Color(0xFF0284C7)
                                                    CurrencyType.RED_GEM -> Color(0xFFE11D48)
                                                }
                                                else -> Color(0xFF334155)
                                            },
                                            disabledContainerColor = Color(0xFF1E293B)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .testTag("shop_item_btn_${item.id}")
                                    ) {
                                        Text(
                                            text = when {
                                                isEquipped -> "✓ EQUIPPED"
                                                isUnlocked -> "EQUIP"
                                                canAfford -> "$curSymbol ${item.cost}"
                                                else -> "NEED ${item.cost} $curSymbol"
                                            },
                                            color = when {
                                                isEquipped -> Color.Black
                                                isUnlocked -> Color.White
                                                canAfford -> Color.White
                                                else -> Color(0xFF64748B)
                                            },
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Skin "Test Drive" / Trial Option for unowned Skins & Bridges
                                val isRentedActive = when (item.type) {
                                    AccessoryType.BODY_SKIN -> rentedSkinId == item.id && rentedSkinRuns > 0
                                    AccessoryType.STICK -> rentedStickId == item.id && rentedStickRuns > 0
                                    else -> false
                                }
                                val rentedRunsRemaining = when (item.type) {
                                    AccessoryType.BODY_SKIN -> rentedSkinRuns
                                    AccessoryType.STICK -> rentedStickRuns
                                    else -> 0
                                }

                                if (!isUnlocked && (item.type == AccessoryType.BODY_SKIN || item.type == AccessoryType.STICK)) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (isRentedActive) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF6B21A8),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "⚡ TRIAL: $rentedRunsRemaining RUNS",
                                                    color = Color(0xFFFDE047),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                previewedItem = item
                                                viewModel.rentItemForTestDrive(item)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(26.dp)
                                                .testTag("shop_item_test_drive_${item.id}")
                                        ) {
                                            Text(
                                                text = "🎬 TEST DRIVE (3 RUNS)",
                                                color = Color(0xFFF3E8FF),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Quick Gem Rewards & Free Starter Boosters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Daily Reward shortcut
                    if (isDailyAvailable) {
                        Surface(
                            onClick = {
                                viewModel.openShop(false)
                                viewModel.openDailyReward(true)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF065F46),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("shop_claim_daily_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🎁", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Claim Daily Gems",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Free starter gem booster if player is running low
                    if (gems < 25) {
                        Surface(
                            onClick = {
                                viewModel.repository.addGems(25, com.mygames.stickmanrush.security.CurrencySource.DAILY_FREE_GEMS)
                                viewModel.soundManager.playGemCollect()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33FBBF24),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("claim_free_gems_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "✨", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+25 Free Gems Bonus",
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PauseMenuDialog(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()

    Dialog(onDismissRequest = { viewModel.openPauseMenu(false) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pause_menu_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "GAME PAUSED",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                // Sound toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { viewModel.toggleSound() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, contentDescription = null, tint = Color.White)
                        Text("Sound Effects", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Switch(checked = soundEnabled, onCheckedChange = { viewModel.toggleSound() })
                }

                // Haptics toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { viewModel.toggleHaptics() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = Color.White)
                        Text("Haptic Feedback", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Switch(checked = hapticsEnabled, onCheckedChange = { viewModel.toggleHaptics() })
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Resume button
                Button(
                    onClick = { viewModel.openPauseMenu(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }

                // Environment Themes & Backgrounds Button
                val currentPauseStage by viewModel.engine.currentStage.collectAsState()
                val currentPauseIcon = remember(currentPauseStage) { com.mygames.stickmanrush.game.StageThemes.getThemeIcon(currentPauseStage) }
                Surface(
                    onClick = {
                        viewModel.openPauseMenu(false)
                        viewModel.openThemeSelector(true)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1B4B),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_theme_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "🎨", fontSize = 16.sp)
                            Text(
                                text = "REALM THEMES ($currentPauseIcon ${currentPauseStage.name})",
                                color = Color(0xFFE9D5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        Text(text = "CHANGE ▶", color = Color(0xFFC084FC), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                // Global Leaderboard & World Ranks Button
                Surface(
                    onClick = {
                        viewModel.openPauseMenu(false)
                        viewModel.openLeaderboard(true)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF064E3B),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF34D399)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_leaderboard_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "🏆", fontSize = 16.sp)
                            Text(
                                text = "LEADERSHIP BOARD & RANKS",
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(text = "VIEW ▶", color = Color(0xFF6EE7B7), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                // Restart button
                OutlinedButton(
                    onClick = {
                        viewModel.openPauseMenu(false)
                        viewModel.engine.resetGame()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_restart_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART RUN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Main Menu button
                OutlinedButton(
                    onClick = { viewModel.returnToMainMenu() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("pause_main_menu_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Main Menu", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MAIN MENU", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun HowToPlayDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("how_to_play_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How to Play",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TutorialStep(
                        number = "1",
                        title = "Grow the Bridge",
                        desc = "Touch & hold anywhere on the screen to stretch your bridge stick vertically."
                    )
                    TutorialStep(
                        number = "2",
                        title = "Release to Drop",
                        desc = "Release your finger. The bridge falls forward 90 degrees to span the gap."
                    )
                    TutorialStep(
                        number = "3",
                        title = "Red Dot Bullseye (Bonus)",
                        desc = "Hit the center red dot on the next platform for PERFECT! (+2 score + bonus gems)."
                    )
                    TutorialStep(
                        number = "4",
                        title = "Flip for Gems",
                        desc = "Tap lower screen or FLIP button while walking to flip upside-down and grab gems hanging under the bridge! Tap again before hitting the wall."
                    )
                    TutorialStep(
                        number = "5",
                        title = "Jump over Obstacles & Fireballs",
                        desc = "Tap screen or JUMP button to leap into the air! Leap over buzzsaws, spike mines, and fiery boss blasts for bonus score & gems."
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("how_to_play_close_button")
                ) {
                    Text("GOT IT!", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun TutorialStep(number: String, title: String, desc: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF10B981),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = desc, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun DailyRewardDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStreak by viewModel.currentStreak.collectAsState()
    val isDailyRewardAvailable by viewModel.isDailyRewardAvailable.collectAsState()
    var claimedNotice by remember { mutableStateOf<String?>(null) }
    var isWatchingAdMultiplier by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_reward")
    val rewardGlow by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF334155)),
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .testTag("daily_reward_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎁 DAILY REWARDS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF831843).copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E))
                    ) {
                        Text(
                            text = "🔥 Login Streak: $currentStreak Day${if (currentStreak > 1) "s" else ""}",
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // 7-Day Rewards Grid (4 on top row, 3 on bottom row)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1: Days 1 to 4
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (day in 1..4) {
                            val rewardGems = viewModel.getStreakDayReward(day)
                            val isClaimed = if (isDailyRewardAvailable) day < currentStreak else day <= currentStreak
                            val isCurrentActive = day == currentStreak && isDailyRewardAvailable

                            DailyDayCard(
                                day = day,
                                gems = rewardGems,
                                isClaimed = isClaimed,
                                isCurrentActive = isCurrentActive,
                                glowScale = if (isCurrentActive) rewardGlow else 1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Row 2: Days 5 to 7
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (day in 5..7) {
                            val rewardGems = viewModel.getStreakDayReward(day)
                            val isClaimed = if (isDailyRewardAvailable) day < currentStreak else day <= currentStreak
                            val isCurrentActive = day == currentStreak && isDailyRewardAvailable
                            val isMegaPrize = day == 7

                            DailyDayCard(
                                day = day,
                                gems = rewardGems,
                                isClaimed = isClaimed,
                                isCurrentActive = isCurrentActive,
                                isMegaPrize = isMegaPrize,
                                glowScale = if (isCurrentActive) rewardGlow else 1f,
                                modifier = Modifier.weight(if (isMegaPrize) 1.2f else 1f)
                            )
                        }
                    }
                }

                // Claim Notice / Streak Description
                if (claimedNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF064E3B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = claimedNotice!!,
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (isDailyRewardAvailable) {
                            "Claim your Day $currentStreak gift now! Streak resets if you miss a day."
                        } else {
                            "Great job! Come back tomorrow to continue your streak and claim Day ${if (currentStreak >= 7) 1 else currentStreak + 1}!"
                        },
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDailyRewardAvailable) {
                        val rewardGems = viewModel.getStreakDayReward(currentStreak)

                        // 2X AD CLAIM BUTTON
                        Button(
                            onClick = {
                                if (!isWatchingAdMultiplier) {
                                    isWatchingAdMultiplier = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(1800)
                                        val awarded = viewModel.claimDailyReward(multiplier = 2)
                                        claimedNotice = "🎉 2X REWARD! Claimed +$awarded Gems! Streak is now $currentStreak days!"
                                        isWatchingAdMultiplier = false
                                    }
                                }
                            },
                            enabled = !isWatchingAdMultiplier,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .scale(rewardGlow)
                                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFF59E0B))
                                .testTag("claim_daily_reward_2x_ad_button")
                        ) {
                            Text(text = "📺", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWatchingAdMultiplier) "WATCHING AD..." else "DOUBLE REWARD (+$(${rewardGems * 2}) 💎) 🎬",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Regular Claim Button
                        OutlinedButton(
                            onClick = {
                                val awarded = viewModel.claimDailyReward(multiplier = 1)
                                claimedNotice = "🎉 Claimed +$awarded Gems! Streak is now $currentStreak days!"
                            },
                            enabled = !isWatchingAdMultiplier,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("claim_daily_reward_button")
                        ) {
                            Text(text = "🎁", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CLAIM REGULAR (+$rewardGems 💎)",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("daily_reward_collected_dismiss_button")
                        ) {
                            Text(
                                text = "CLAIMED FOR TODAY ✓",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "CLOSE",
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyDayCard(
    day: Int,
    gems: Int,
    isClaimed: Boolean,
    isCurrentActive: Boolean,
    isMegaPrize: Boolean = false,
    glowScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isCurrentActive -> Color(0xFFFBBF24)
        isClaimed -> Color(0xFF10B981)
        isMegaPrize -> Color(0xFFEC4899)
        else -> Color(0xFF334155)
    }

    val bgColor = when {
        isCurrentActive -> Color(0xFF1E1B4B)
        isClaimed -> Color(0xFF064E3B).copy(alpha = 0.4f)
        isMegaPrize -> Color(0xFF3B0764)
        else -> Color(0xFF1E293B)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(if (isCurrentActive) 1.8.dp else 1.dp, borderColor),
        modifier = modifier
            .scale(glowScale)
            .shadow(if (isCurrentActive) 6.dp else 0.dp, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isMegaPrize) "👑 DAY 7" else "Day $day",
                color = if (isCurrentActive) Color(0xFFFDE047) else Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isMegaPrize) "💎+$gems" else "+$gems",
                color = if (isClaimed) Color(0xFF34D399) else if (isCurrentActive) Color(0xFF38BDF8) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = when {
                    isClaimed -> "✓ Claimed"
                    isCurrentActive -> "READY!"
                    else -> "🔒"
                },
                color = when {
                    isClaimed -> Color(0xFF34D399)
                    isCurrentActive -> Color(0xFFFBBF24)
                    else -> Color(0xFF64748B)
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DailyMissionsDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyMissions by viewModel.dailyMissions.collectAsState()
    val totalMissions = dailyMissions.size
    val completedCount = dailyMissions.count { it.isClaimed }
    var isWatchingAdMissionId by remember { mutableStateOf<String?>(null) }
    var isWatchingAdClaimAll by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .testTag("daily_missions_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🎯", fontSize = 22.sp)
                            Column {
                                Text(
                                    text = "DAILY QUESTS",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Complete challenges for bonus gems",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .testTag("daily_missions_close_button")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Progress Overview
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Progress",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$completedCount / $totalMissions Claimed",
                                color = if (completedCount == totalMissions && totalMissions > 0) Color(0xFF34D399) else Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mission Items List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dailyMissions.forEach { mission ->
                            val isGoalMet = mission.currentProgress >= mission.targetCount
                            val progressFrac = (mission.currentProgress.toFloat() / mission.targetCount.toFloat()).coerceIn(0f, 1f)
                            val iconSymbol = when (mission.missionType) {
                                "BUILD_BRIDGES" -> "🥢"
                                "PERFECT_HITS" -> "🎯"
                                "COLLECT_GEMS" -> "💎"
                                "FLIP_WALK" -> "🤸"
                                "REACH_SCORE" -> "🏆"
                                else -> "⭐"
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (mission.isClaimed) Color(0xFF0F231B).copy(alpha = 0.5f) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = when {
                                        mission.isClaimed -> Color(0xFF059669)
                                        isGoalMet -> Color(0xFFFBBF24)
                                        else -> Color(0xFF334155)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mission_item_${mission.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = iconSymbol, fontSize = 24.sp)

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = mission.title,
                                            color = if (mission.isClaimed) Color(0xFF64748B) else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = mission.description,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )

                                        // Progress Bar
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { progressFrac },
                                                color = if (isGoalMet) Color(0xFF34D399) else Color(0xFF38BDF8),
                                                trackColor = Color(0xFF334155),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                            Text(
                                                text = "${mission.currentProgress}/${mission.targetCount}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Action / Claim Button
                                    when {
                                        mission.isClaimed -> {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF064E3B)
                                            ) {
                                                Text(
                                                    text = "✓ DONE",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                        isGoalMet -> {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // 2X Ad Claim
                                                Button(
                                                    onClick = {
                                                        if (isWatchingAdMissionId == null && !isWatchingAdClaimAll) {
                                                            isWatchingAdMissionId = mission.id
                                                            coroutineScope.launch {
                                                                kotlinx.coroutines.delay(1800)
                                                                viewModel.claimDailyMission(mission.id, mission.rewardGems, multiplier = 2)
                                                                isWatchingAdMissionId = null
                                                            }
                                                        }
                                                    },
                                                    enabled = isWatchingAdMissionId == null && !isWatchingAdClaimAll,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier.testTag("claim_mission_2x_${mission.id}")
                                                ) {
                                                    Text(
                                                        text = if (isWatchingAdMissionId == mission.id) "..." else "2X 🎬",
                                                        color = Color.Black,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }

                                                // 1X Claim
                                                Button(
                                                    onClick = { viewModel.claimDailyMission(mission.id, mission.rewardGems, multiplier = 1) },
                                                    enabled = isWatchingAdMissionId == null && !isWatchingAdClaimAll,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier.testTag("claim_mission_${mission.id}")
                                                ) {
                                                    Text(
                                                        text = "💎+${mission.rewardGems}",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF334155).copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = "💎+${mission.rewardGems}",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val readyToClaimCount = dailyMissions.count { (it.currentProgress >= it.targetCount || it.isCompleted) && !it.isClaimed }
                    val readyToClaimTotalGems = dailyMissions.filter { (it.currentProgress >= it.targetCount || it.isCompleted) && !it.isClaimed }.sumOf { it.rewardGems }

                    if (readyToClaimCount > 0) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 2X Ad Double All
                            Button(
                                onClick = {
                                    if (isWatchingAdMissionId == null && !isWatchingAdClaimAll) {
                                        isWatchingAdClaimAll = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1800)
                                            viewModel.claimAllDailyMissions(multiplier = 2)
                                            isWatchingAdClaimAll = false
                                        }
                                    }
                                },
                                enabled = isWatchingAdMissionId == null && !isWatchingAdClaimAll,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("claim_all_daily_missions_2x_ad_button")
                            ) {
                                Text(
                                    text = if (isWatchingAdClaimAll) "📺 WATCHING SPONSORED AD..." else "🎬 DOUBLE ALL CLAIM (+$(${readyToClaimTotalGems * 2}) 💎)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }

                            // Regular Claim All
                            OutlinedButton(
                                onClick = { viewModel.claimAllDailyMissions(multiplier = 1) },
                                enabled = isWatchingAdMissionId == null && !isWatchingAdClaimAll,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("claim_all_daily_missions_button")
                            ) {
                                Text(
                                    text = "✨ CLAIM ALL ($readyToClaimCount QUESTS • +$readyToClaimTotalGems 💎)",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("daily_missions_play_button")
                    ) {
                        Text(
                            text = "LET'S PLAY! 🥷",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GemVaultContent(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val gemPacks = viewModel.availableGemPacks
    val isDailyFreeAvailable = viewModel.isDailyFreeGemsAvailable()
    var selectedPackForCheckout by remember { mutableStateOf<GemPack?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Lucky Gem Spin Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1B4B),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF818CF8)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_spin_wheel_banner")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🎰", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "LUCKY GEM WHEEL",
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Spin & Win up to 120 Gems!",
                            color = Color(0xFFE0E7FF),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.openShop(false)
                        viewModel.openSpinWheel(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("vault_open_spin_btn")
                ) {
                    Text(
                        text = "SPIN NOW",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Real Money & Gem Packs Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 145.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("vault_gem_packs_grid")
        ) {
            items(gemPacks) { pack ->
                val isFreeCrate = pack.isDailyFree
                val isClaimable = !isFreeCrate || isDailyFreeAvailable
                val isPopular = pack.tag.isNotEmpty()

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isPopular) Color(0xFF1E293B) else Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isPopular) 1.5.dp else 1.dp,
                        color = if (isPopular) Color(0xFFFBBF24) else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isClaimable) {
                                if (isFreeCrate) {
                                    viewModel.buyGemPack(pack)
                                } else {
                                    selectedPackForCheckout = pack
                                }
                            }
                        }
                        .testTag("gem_pack_${pack.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (pack.tag.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isFreeCrate) Color(0xFF059669) else Color(0xFFD97706)
                            ) {
                                Text(
                                    text = pack.tag,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(text = pack.iconEmoji, fontSize = 28.sp)

                        Text(
                            text = pack.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "+${pack.gemAmount + pack.bonusGems} Gems",
                            color = Color(0xFF38BDF8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = if (pack.bonusGems > 0) "+${pack.bonusGems} Bonus Included!" else pack.perks,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Button(
                            onClick = {
                                if (isFreeCrate) {
                                    viewModel.buyGemPack(pack)
                                } else {
                                    selectedPackForCheckout = pack
                                }
                            },
                            enabled = isClaimable,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFreeCrate) Color(0xFF10B981) else Color(0xFFF59E0B),
                                disabledContainerColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .testTag("buy_pack_${pack.id}")
                        ) {
                            Text(
                                text = when {
                                    isFreeCrate && !isDailyFreeAvailable -> "CLAIMED"
                                    isFreeCrate -> "FREE DAILY"
                                    pack.priceUsd.isNotEmpty() -> pack.priceUsd
                                    else -> "${pack.scoreCost} PTS"
                                },
                                color = if (isClaimable) Color.Black else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Real-Money IAP Dialog
    selectedPackForCheckout?.let { pack ->
        RealMoneyIapCheckoutDialog(
            pack = pack,
            onConfirmPurchase = {
                viewModel.buyGemPackRealMoney(pack, activity)
                selectedPackForCheckout = null
            },
            onDismiss = { selectedPackForCheckout = null }
        )
    }
}

@Composable
fun RealMoneyIapCheckoutDialog(
    pack: GemPack,
    onConfirmPurchase: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .shadow(20.dp, RoundedCornerShape(24.dp))
                .testTag("iap_checkout_dialog")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Store Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🛡️", fontSize = 16.sp)
                        Text(
                            text = "SECURE IN-APP PURCHASE",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                // Pack Icon & Title
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E3A8A),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = pack.iconEmoji, fontSize = 36.sp)
                    }
                }

                Text(
                    text = pack.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                // Gems Breakdown Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Base Gem Amount", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("${pack.gemAmount} 💎", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (pack.bonusGems > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bonus Extra Gems", color = Color(0xFF34D399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("+${pack.bonusGems} 💎", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Delivered", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(
                                "${pack.gemAmount + pack.bonusGems} GEMS",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Perks
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "✨", fontSize = 14.sp)
                        Text(
                            text = pack.perks,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Confirm Buy Button
                Button(
                    onClick = onConfirmPurchase,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_real_money_purchase_btn")
                ) {
                    Text(
                        text = "1-TAP BUY FOR ${if (pack.priceUsd.isNotEmpty()) pack.priceUsd else "$0.99"} 💎",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Instant delivery • Secure simulated checkout",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LeaderboardAndContestDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val leaderboard = remember { viewModel.getLeaderboard() }
    val userLeague = remember { viewModel.getUserLeague() }
    val userHighScore by viewModel.highScore.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF334155)),
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .testTag("leaderboard_contest_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🏆", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "GLOBAL ARENA",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "World Ladder & Weekly Tournaments",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("leaderboard_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == 0) Color(0xFF10B981) else Color(0xFF1E293B),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("tab_world_ranks")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🌍 World Ranks",
                                color = if (selectedTab == 0) Color.White else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == 1) Color(0xFF10B981) else Color(0xFF1E293B),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("tab_weekly_contest")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "⚔️ Weekly Contest",
                                color = if (selectedTab == 1) Color.White else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // World Leaderboard Tab
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // User summary highlight banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "👑", fontSize = 18.sp)
                                    Column {
                                        Text(
                                            text = "YOU (StickHero)",
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "League: ${userLeague.title}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$userHighScore PTS",
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = userLeague.badgeEmoji,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Global Ranking Table
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("world_leaderboard_list")
                        ) {
                            items(leaderboard) { entry ->
                                val isTop3 = entry.rank <= 3
                                val rankColor = when (entry.rank) {
                                    1 -> Color(0xFFFFD700)
                                    2 -> Color(0xFFC0C0C0)
                                    3 -> Color(0xFFCD7F32)
                                    else -> Color(0xFF94A3B8)
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (entry.isCurrentUser) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF1E293B),
                                    border = if (entry.isCurrentUser) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = if (entry.rank == 1) "🥇" else if (entry.rank == 2) "🥈" else if (entry.rank == 3) "🥉" else "#${entry.rank}",
                                                color = rankColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                modifier = Modifier.width(28.dp)
                                            )
                                            Text(text = entry.countryFlag, fontSize = 16.sp)
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = entry.playerName,
                                                        color = if (entry.isCurrentUser) Color(0xFF38BDF8) else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF334155)
                                                    ) {
                                                        Text(
                                                            text = entry.league.title,
                                                            color = Color(0xFFCBD5E1),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "🎯 ${entry.perfectHits} Perfects",
                                                    color = Color(0xFF64748B),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${entry.score}",
                                            color = if (isTop3) Color(0xFFFBBF24) else Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Weekly Contest & Tournament Tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Season Timer Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF312E81),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "SEASON 14 TOURNAMENT",
                                        color = Color(0xFFA5B4FC),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "⏳ Ends in 2d 18h 44m",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF59E0B)
                                ) {
                                    Text(
                                        text = "200 💎 POOL",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Current League Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = "🛡️", fontSize = 24.sp)
                                        Column {
                                            Text(
                                                text = "Your Division",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = userLeague.title,
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF065F46)
                                    ) {
                                        Text(
                                            text = "PROMOTION ZONE",
                                            color = Color(0xFF34D399),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Divider(color = Color(0xFF334155))

                                Text(
                                    text = "Rewards for Top Finishers:",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🥇 1st Place: 200 💎 + Golden Scepter", color = Color(0xFFFBBF24), fontSize = 11.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🥈 Top 10%: 100 💎 + Emerald Bridge", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🥉 Top 25%: 50 💎 + Promotion", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text(
                        text = "CLOSE",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LuckySpinWheelDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSpinning by remember { mutableStateOf(false) }
    var isWatchingAd by remember { mutableStateOf(false) }
    var wonAmount by remember { mutableStateOf<Int?>(null) }
    var adNotice by remember { mutableStateOf<String?>(null) }
    var showCheckoutPack by remember { mutableStateOf<GemPack?>(null) }
    val rotationAngle = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val gems by viewModel.gems.collectAsState()
    val adEarnedSpins by viewModel.adEarnedSpins.collectAsState()
    val isFreeDailyAvailable by viewModel.isDailyFreeSpinAvailableFlow.collectAsState()
    val hasFreeOrBonusSpin = isFreeDailyAvailable || adEarnedSpins > 0

    Dialog(
        onDismissRequest = { if (!isSpinning && !isWatchingAd) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6366F1)),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .shadow(28.dp, RoundedCornerShape(28.dp))
                .testTag("lucky_spin_dialog")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E1B4B),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with current Gems indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🎰", fontSize = 22.sp)
                        Text(
                            text = "LUCKY GEM WHEEL",
                            color = Color(0xFFFBBF24),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "💎", fontSize = 12.sp)
                            Text(
                                text = "$gems",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Status Banner
                if (isFreeDailyAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF065F46),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399))
                    ) {
                        Text(
                            text = "🎁 1 FREE DAILY CLAIM AVAILABLE!",
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else if (adEarnedSpins > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E3A8A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Text(
                            text = "🎟️ SPINS AVAILABLE: $adEarnedSpins",
                            color = Color(0xFF93C5FD),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF334155),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
                    ) {
                        Text(
                            text = "🔒 Free claim used! Buy spins with Gems, Money or Watch Ad",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Spinning Wheel Canvas
                val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f
                        val sliceCount = 8
                        val sweepAngle = 360f / sliceCount
                        val colors = listOf(
                            Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899),
                            Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0xFF8B5CF6), Color(0xFFE11D48)
                        )

                        // Outer gold bezel
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            radius = radius,
                            center = center
                        )

                        // Wheel Slices rotated by rotationAngle.value
                        val currentRotation = rotationAngle.value
                        val gemValues = listOf(5, 10, 3, 20, 5, 15, 8, 25)
                        for (i in 0 until sliceCount) {
                            val startAngle = currentRotation + (i * sweepAngle)
                            drawArc(
                                color = colors[i % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
                                size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx())
                            )

                            val midAngleDeg = startAngle + sweepAngle / 2f
                            val midAngleRad = Math.toRadians(midAngleDeg.toDouble())
                            val textDistance = radius * 0.62f
                            val textX = center.x + (textDistance * cos(midAngleRad)).toFloat()
                            val textY = center.y + (textDistance * sin(midAngleRad)).toFloat()

                            val gemAmount = gemValues[i % gemValues.size]
                            val labelText = "$gemAmount💎"

                            val textLayoutResult = textMeasurer.measure(
                                text = labelText,
                                style = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                            val textTopLeft = Offset(
                                textX - textLayoutResult.size.width / 2f,
                                textY - textLayoutResult.size.height / 2f
                            )

                            rotate(
                                degrees = midAngleDeg + 90f,
                                pivot = Offset(textX, textY)
                            ) {
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = textTopLeft
                                )
                            }
                        }

                        // Center Hub
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 22.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            radius = 15.dp.toPx(),
                            center = center
                        )
                    }

                    // Indicator Pin
                    Text(
                        text = "🔻",
                        fontSize = 24.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-12).dp)
                    )
                }

                // Win Reveal Banner
                wonAmount?.let { amount ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF065F46),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF34D399)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎉 WON +$amount GEMS! 🎉",
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Ad/Purchase Notice
                adNotice?.let { notice ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E3A8A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notice,
                            color = Color(0xFFBFDBFE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                // 1. FREE SPIN (1x claim) OR AVAILABLE BONUS SPIN
                if (hasFreeOrBonusSpin) {
                    Button(
                        onClick = {
                            if (!isSpinning) {
                                isSpinning = true
                                wonAmount = null
                                adNotice = null
                                coroutineScope.launch {
                                    val targetAngle = rotationAngle.value + 1440f + (0..360).random()
                                    rotationAngle.animateTo(
                                        targetValue = targetAngle,
                                        animationSpec = tween(durationMillis = 2600, easing = FastOutSlowInEasing)
                                    )
                                    val prize = viewModel.spinLuckyWheel()
                                    wonAmount = prize
                                    isSpinning = false
                                }
                            }
                        },
                        enabled = !isSpinning && !isWatchingAd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFreeDailyAvailable) Color(0xFF10B981) else Color(0xFF3B82F6),
                            disabledContainerColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("spin_wheel_action_btn")
                    ) {
                        Text(
                            text = when {
                                isSpinning -> "SPINNING WHEEL..."
                                isFreeDailyAvailable -> "🎁 CLAIM 1 FREE SPIN!"
                                else -> "🎲 SPIN ($adEarnedSpins AVAILABLE)"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }

                // 2. BUY SPINS SECTION (Gems & Real Money)
                if (!hasFreeOrBonusSpin) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚡ GET MORE SPINS:",
                            color = Color(0xFFFBBF24),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )

                        // Buy with Gems Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1 Spin for 15 Gems
                            Button(
                                onClick = {
                                    if (viewModel.buySpinsWithGems(15, 1)) {
                                        adNotice = "✨ Purchased 1 Spin for 15 Gems!"
                                    } else {
                                        adNotice = "❌ Not enough gems (Need 15 💎)!"
                                    }
                                },
                                enabled = !isSpinning && !isWatchingAd,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("buy_1_spin_gems_btn")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("1 Spin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("15 💎", color = Color(0xFFFDE047), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }

                            // 3 Spins for 35 Gems (Discounted)
                            Button(
                                onClick = {
                                    if (viewModel.buySpinsWithGems(35, 3)) {
                                        adNotice = "🔥 Purchased 3 Spins for 35 Gems!"
                                    } else {
                                        adNotice = "❌ Not enough gems (Need 35 💎)!"
                                    }
                                },
                                enabled = !isSpinning && !isWatchingAd,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("buy_3_spins_gems_btn")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("3 Spins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("35 💎", color = Color(0xFFFDE047), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }

                        // Buy Real Money Spin Packs Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // $0.99 for 5 Spins + 50 Gems
                            Button(
                                onClick = {
                                    showCheckoutPack = GemPack(
                                        id = "spin_pack_5",
                                        name = "5 Lucky Spins Pack",
                                        gemAmount = 50,
                                        bonusGems = 0,
                                        iconEmoji = "🎰",
                                        tag = "BEST VALUE",
                                        priceUsd = "$0.99",
                                        perks = "5 Lucky Wheel Spins + 50 Bonus Gems"
                                    )
                                },
                                enabled = !isSpinning && !isWatchingAd,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("buy_spin_pack_real_money_btn")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("5 Spins + 50💎", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("$0.99 ⚡", color = Color(0xFF6EE7B7), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }

                            // $1.99 for 15 Spins + 150 Gems
                            Button(
                                onClick = {
                                    showCheckoutPack = GemPack(
                                        id = "spin_pack_15",
                                        name = "15 Lucky Spins Mega Pack",
                                        gemAmount = 150,
                                        bonusGems = 0,
                                        iconEmoji = "👑",
                                        tag = "MEGA DEAL",
                                        priceUsd = "$1.99",
                                        perks = "15 Lucky Wheel Spins + 150 Bonus Gems"
                                    )
                                },
                                enabled = !isSpinning && !isWatchingAd,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("buy_spin_mega_pack_real_money_btn")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("15 Spins + 150💎", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("$1.99 👑", color = Color(0xFFFDE047), fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Watch Video Ad for +1 Free Spin
                Button(
                    onClick = {
                        if (!isSpinning && !isWatchingAd) {
                            isWatchingAd = true
                            wonAmount = null
                            adNotice = "📺 Watching sponsored video ad..."
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(1800)
                                viewModel.watchAdForSpin { isVerified ->
                                    adNotice = if (isVerified) {
                                        "🎟️ Ad SSV verified! +1 Bonus Spin added!"
                                    } else {
                                        "⚠️ Ad verification failed or spoofed callback."
                                    }
                                }
                                isWatchingAd = false
                            }
                        }
                    },
                    enabled = !isSpinning && !isWatchingAd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!hasFreeOrBonusSpin) Color(0xFFF59E0B) else Color(0xFF1E293B),
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    border = if (!hasFreeOrBonusSpin) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("spin_wheel_watch_ad_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "📺", fontSize = 14.sp)
                        Text(
                            text = if (isWatchingAd) "WATCHING AD..." else "OR WATCH AD FOR +1 SPIN 🎬",
                            color = if (!hasFreeOrBonusSpin) Color.Black else Color(0xFFFDE047),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !isSpinning && !isWatchingAd,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(
                        text = "CLOSE",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Real Money checkout confirmation modal if buying spin pack
    showCheckoutPack?.let { pack ->
        RealMoneyIapCheckoutDialog(
            pack = pack,
            onConfirmPurchase = {
                val spinsToAdd = if (pack.id == "spin_pack_5") 5 else 15
                viewModel.buySpinsRealMoney(spinsToAdd)
                viewModel.repository.addGems(pack.gemAmount, com.mygames.stickmanrush.security.CurrencySource.IN_APP_PURCHASE)
                adNotice = "✅ Purchase successful! Added $spinsToAdd Spins and ${pack.gemAmount} 💎!"
                showCheckoutPack = null
            },
            onDismiss = { showCheckoutPack = null }
        )
    }
}

@Composable
fun LevelVictoryCelebrationDialog(
    celebrationText: String,
    levelNumber: Int? = null,
    viewModel: GameViewModel? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "victory_anim")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "victory_scale"
    )

    val coroutineScope = rememberCoroutineScope()
    var hasClaimed3x by remember { mutableStateOf(false) }
    var isWatching3xAd by remember { mutableStateOf(false) }
    val baseGems = 10 + ((levelNumber ?: 1) * 2)

    val headingText = if (levelNumber != null && levelNumber > 0) {
        "LEVEL $levelNumber COMPLETED! 🎉"
    } else {
        "LEVEL COMPLETED! 🎉"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(3.dp, Brush.sweepGradient(listOf(Color(0xFFFFD700), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFFFFD700)))),
            modifier = modifier
                .fillMaxWidth(0.94f)
                .scale(scaleAnim)
                .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = Color(0xFFFFD700), spotColor = Color(0xFFFFD700))
                .testTag("level_victory_dialog")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E1B4B),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Popping Crown & Sparkles Icon
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF312E81),
                    border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFFFD700)),
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(16.dp, CircleShape, ambientColor = Color(0xFFFFD700))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "👑", fontSize = 40.sp)
                    }
                }

                Text(
                    text = headingText,
                    color = Color(0xFFFFD700),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("level_completed_heading")
                )

                // High-Dopamine Motivation Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⚡ YOU ARE JUST A FEW BRIDGES AWAY FROM BIG REWARDS!",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = celebrationText,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF065F46),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF34D399)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎁 Base Level Reward: +$baseGems 💎",
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp
                        )
                    }
                }

                // 3x Win Multiplier Rewarded Placement
                if (!hasClaimed3x) {
                    Button(
                        onClick = {
                            if (!isWatching3xAd) {
                                isWatching3xAd = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1600)
                                    viewModel?.claim3xLevelBonus(baseGems)
                                    hasClaimed3x = true
                                    isWatching3xAd = false
                                }
                            }
                        },
                        enabled = !isWatching3xAd,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFF59E0B))
                            .testTag("claim_3x_win_multiplier_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🎬", fontSize = 18.sp)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isWatching3xAd) "CLAIMING 3X REWARD..." else "WATCH AD FOR 3X GEMS (+${baseGems * 3} 💎)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Triple Your Level Victory Gems",
                                    color = Color(0xFF78350F),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF78350F),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✨ 3X REWARD CLAIMED! (+${baseGems * 3} 💎) ✨",
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Immediate Continue Button (NO ARTIFICIAL DELAY - BUILDS TRUST)
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF10B981))
                        .testTag("continue_next_level_button")
                ) {
                    Text(
                        text = if (!hasClaimed3x) "CONTINUE TO NEXT LEVEL 🚀" else "NEXT LEVEL 🚀",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OutOfGemsSpecialOfferDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val gems by viewModel.gems.collectAsState()
    val reviveCost = viewModel.engine.getReviveCost()
    val neededGems = (reviveCost - gems).coerceAtLeast(1)
    val availablePacks = viewModel.availableGemPacks.filter { !it.isDailyFree }
    var selectedPackForCheckout by remember { mutableStateOf<GemPack?>(null) }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_offer")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val badgeGlow by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xDD020617))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF43F5E)),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .shadow(32.dp, RoundedCornerShape(26.dp), ambientColor = Color(0xFFF43F5E))
                    .testTag("out_of_gems_special_offer_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF4C0519),
                                    Color(0xFF1E1B4B),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Bar with Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE11D48).copy(alpha = badgeGlow),
                            modifier = Modifier.scale(pulseScale)
                        ) {
                            Text(
                                text = "🔥 LIMITED TIME 80% OFF DEAL",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("out_of_gems_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    // Urgency Header with Gems Icon
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFF43F5E),
                                        Color(0xFF881337),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .border(2.dp, Color(0xFFFDA4AF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💎", fontSize = 34.sp)
                    }

                    Text(
                        text = "OUT OF GEMS!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "You need $neededGems more Gems to revive & continue your high-score run! Grab an instant discounted booster bundle:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    // Flash Sale Mega Value Pack (Hero Offer)
                    val starterPack = availablePacks.firstOrNull() ?: GemPack(
                        id = "starter_hero",
                        name = "Instant Revival Chest",
                        gemAmount = 50,
                        bonusGems = 30,
                        iconEmoji = "💎",
                        tag = "80% OFF SPECIAL",
                        priceUsd = "$0.99",
                        perks = "Instantly revives stickman + 80 Total Gems!"
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF78350F).copy(alpha = 0.5f),
                                            Color(0xFF1E293B)
                                        )
                                    )
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = starterPack.iconEmoji, fontSize = 28.sp)
                                    Column {
                                        Text(
                                            text = starterPack.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "+${starterPack.gemAmount + starterPack.bonusGems} Gems Total (+${starterPack.bonusGems} FREE Bonus)",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFD97706)
                                ) {
                                    Text(
                                        text = "MOST POPULAR",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { selectedPackForCheckout = starterPack },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("out_of_gems_buy_hero_pack_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "⚡ GET ${starterPack.gemAmount + starterPack.bonusGems} GEMS FOR ${starterPack.priceUsd} ⚡",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // Secondary Discount Packs Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availablePacks.drop(1).take(2).forEach { pack ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPackForCheckout = pack }
                                    .testTag("out_of_gems_secondary_pack_${pack.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = pack.iconEmoji, fontSize = 20.sp)
                                    Text(
                                        text = "+${pack.gemAmount + pack.bonusGems} 💎",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                    Button(
                                        onClick = { selectedPackForCheckout = pack },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                    ) {
                                        Text(
                                            text = pack.priceUsd,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Free Booster / Spin fallback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                viewModel.openSpinWheel(true)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("out_of_gems_spin_fallback_btn")
                        ) {
                            Text("🎰 Spin Wheel", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                viewModel.openShop(true)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("out_of_gems_view_shop_btn")
                        ) {
                            Text("🏬 Gem Vault", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Secondary Real-Money Checkout Modal Confirmation
    selectedPackForCheckout?.let { pack ->
        RealMoneyIapCheckoutDialog(
            pack = pack,
            onConfirmPurchase = {
                viewModel.buyGemPackRealMoney(pack, activity)
                selectedPackForCheckout = null
                onDismiss()
                // Auto revive player if they now have enough gems
                if (viewModel.canAffordRevive()) {
                    viewModel.revivePlayer()
                }
            },
            onDismiss = { selectedPackForCheckout = null }
        )
    }
}

@Composable
fun RealMoneyGemShopDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val gemPacks = viewModel.availableGemPacks
    val currentGems by viewModel.gems.collectAsState()
    val isDailyFreeAvailable = viewModel.isDailyFreeGemsAvailable()
    var selectedPackForCheckout by remember { mutableStateOf<GemPack?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "gemGlow")
    val gemShimmer by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xEE000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0B132B),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .shadow(32.dp, RoundedCornerShape(26.dp), ambientColor = Color(0xFF38BDF8), spotColor = Color(0xFF0284C7))
                    .testTag("real_money_gem_shop_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "💎", fontSize = 24.sp, modifier = Modifier.scale(gemShimmer))
                                }
                            }

                            Column {
                                Text(
                                    text = "GEM BANK & STORE",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Official In-App Gem Packages",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                                .testTag("real_money_shop_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Current Balance Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "👛", fontSize = 16.sp)
                                Text(
                                    text = "YOUR CURRENT BALANCE",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "💎", fontSize = 16.sp)
                                Text(
                                    text = "$currentGems GEMS",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Packs Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("real_money_gem_packs_grid")
                    ) {
                        items(gemPacks) { pack ->
                            val isFreeCrate = pack.isDailyFree
                            val isClaimable = !isFreeCrate || isDailyFreeAvailable
                            val isPopular = pack.tag.isNotEmpty()

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isPopular) Color(0xFF1E293B) else Color(0xFF0F172A),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isPopular) 1.8.dp else 1.dp,
                                    color = if (isPopular) Color(0xFFFBBF24) else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isClaimable) {
                                            if (isFreeCrate) {
                                                viewModel.buyGemPack(pack)
                                            } else {
                                                selectedPackForCheckout = pack
                                            }
                                        }
                                    }
                                    .shadow(if (isPopular) 8.dp else 0.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFFFBBF24))
                                    .testTag("real_money_pack_${pack.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (pack.tag.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isFreeCrate) Color(0xFF059669) else Color(0xFFD97706)
                                        ) {
                                            Text(
                                                text = pack.tag,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(text = pack.iconEmoji, fontSize = 34.sp)

                                    Text(
                                        text = pack.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "+${pack.gemAmount + pack.bonusGems} Gems",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )

                                    Text(
                                        text = if (pack.bonusGems > 0) "+${pack.bonusGems} BONUS EXTRA!" else pack.perks,
                                        color = if (pack.bonusGems > 0) Color(0xFF34D399) else Color(0xFF94A3B8),
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (pack.bonusGems > 0) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Button(
                                        onClick = {
                                            if (isFreeCrate) {
                                                viewModel.buyGemPack(pack)
                                            } else {
                                                selectedPackForCheckout = pack
                                            }
                                        },
                                        enabled = isClaimable,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFreeCrate) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            disabledContainerColor = Color(0xFF334155)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .testTag("buy_real_money_pack_${pack.id}")
                                    ) {
                                        Text(
                                            text = when {
                                                isFreeCrate && !isDailyFreeAvailable -> "CLAIMED"
                                                isFreeCrate -> "FREE DAILY"
                                                pack.priceUsd.isNotEmpty() -> pack.priceUsd
                                                else -> "${pack.scoreCost} PTS"
                                            },
                                            color = if (isClaimable) Color.Black else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Footer security info & Spin shortcut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🔒", fontSize = 12.sp)
                            Text(
                                text = "100% Guaranteed Delivery",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                viewModel.openSpinWheel(true)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("🎰 Spin Wheel Free", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Checkout Confirmation Dialog
    selectedPackForCheckout?.let { pack ->
        RealMoneyIapCheckoutDialog(
            pack = pack,
            onConfirmPurchase = {
                viewModel.buyGemPackRealMoney(pack, activity)
                selectedPackForCheckout = null
            },
            onDismiss = { selectedPackForCheckout = null }
        )
    }
}

/**
 * 👑 Big Challenging & Motivational Human Psychology Dialog
 * Displays professional psychological provocation every 5 levels ("I challenge you can't clear level 5"),
 * and congratulatory motivational titles & rank celebrations on clear or failure.
 */
@Composable
fun ChallengePsychologyDialog(
    challenge: com.mygames.stickmanrush.model.ChallengeDialogData,
    onAccept: () -> Unit
) {
    val isTaunt = challenge.type == com.mygames.stickmanrush.model.ChallengeDialogType.PRE_LEVEL_TAUNT
    val isSuccess = challenge.type == com.mygames.stickmanrush.model.ChallengeDialogType.POST_LEVEL_VICTORY
    val isFail = challenge.type == com.mygames.stickmanrush.model.ChallengeDialogType.POST_LEVEL_FAIL

    val bgGradient = when {
        isSuccess -> listOf(Color(0xFF065F46), Color(0xFF022C22), Color(0xFF0F172A))
        isFail -> listOf(Color(0xFF881337), Color(0xFF4C0519), Color(0xFF0F172A))
        else -> listOf(Color(0xFF312E81), Color(0xFF1E1B4B), Color(0xFF0F172A))
    }

    val primaryAccent = when {
        isSuccess -> Color(0xFF10B981)
        isFail -> Color(0xFFF43F5E)
        else -> Color(0xFFF59E0B)
    }

    val borderStrokeColor = when {
        isSuccess -> Color(0xFF34D399)
        isFail -> Color(0xFFFB7185)
        else -> Color(0xFFFBBF24)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_challenge")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Dialog(
        onDismissRequest = onAccept,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF0B1329),
                border = androidx.compose.foundation.BorderStroke(2.5.dp, borderStrokeColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .shadow(30.dp, RoundedCornerShape(28.dp), ambientColor = primaryAccent, spotColor = primaryAccent)
                    .testTag("challenge_psychology_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.verticalGradient(bgGradient))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Badge Header
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = primaryAccent.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
                    ) {
                        Text(
                            text = when {
                                isSuccess -> "🏆 MILESTONE CLEARED"
                                isFail -> "💀 CHALLENGE FAILED"
                                else -> "🔥 STICKMAN BOSS CHALLENGE"
                            },
                            color = if (isFail) Color(0xFFFDA4AF) else Color(0xFFFDE047),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    // Large Animated Emoji / Icon
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(76.dp)
                            .background(primaryAccent.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, borderStrokeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isSuccess -> "👑"
                                isFail -> "💥"
                                else -> "⚡"
                            },
                            fontSize = 40.sp
                        )
                    }

                    // Main Title
                    Text(
                        text = challenge.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    // Message Body
                    Text(
                        text = challenge.message,
                        color = Color(0xFFE2E8F0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    // Awarded Stickman Title Badge if won
                    challenge.awardedTitle?.let { title ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF047857),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6EE7B7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🎖️ NEW HONORARY TITLE UNLOCKED",
                                    color = Color(0xFFBBF7D0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = title,
                                    color = Color(0xFFFEF08A),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Reward Gems pill if available
                    if (challenge.rewardGems > 0) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xCC0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "💎", fontSize = 18.sp)
                                Text(
                                    text = "+${challenge.rewardGems} GEMS REWARD EARNED!",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Action Button
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFail) Color(0xFFE11D48) else if (isSuccess) Color(0xFF10B981) else Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("challenge_accept_button")
                    ) {
                        Text(
                            text = challenge.buttonText,
                            color = if (isSuccess || isFail) Color.White else Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * ❤️ Life Shop Dialog
 * Gives options to replenish lives using Gems (30 gems for 3 lives, 50 for 7 lives, etc.),
 * or if out of gems, allows buying lives with real money or watching free advertisements!
 */
@Composable
fun LifeShopDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lives by viewModel.lives.collectAsState()
    val gems by viewModel.gems.collectAsState()
    val secondsUntilNextLife by viewModel.secondsUntilNextLife.collectAsState()
    val maxLives = viewModel.maxLives
    val packs = viewModel.lifeShopPacks

    var selectedPackForCheckout by remember { mutableStateOf<com.mygames.stickmanrush.model.LifeShopPack?>(null) }
    var showAdWatchingSimulator by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF43F5E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .shadow(24.dp, RoundedCornerShape(26.dp))
                    .testTag("life_shop_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF020617))
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "❤️", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "LIFE RECOVERY SHOP",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Current: $lives/$maxLives Lives Available",
                                    color = Color(0xFFFDA4AF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0x33FFFFFF), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Gem Balance Pill & 20-Min Regen Timer Banner
                    val regenMinutes = secondsUntilNextLife / 60
                    val regenSeconds = secondsUntilNextLife % 60
                    val formattedTimer = String.format(java.util.Locale.US, "%02d:%02d", regenMinutes, regenSeconds)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x661E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "💎 Gems:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    text = "$gems",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x66831843),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFB7185)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "⏱️ Regen:", color = Color(0xFFFDA4AF), fontSize = 11.sp)
                                Text(
                                    text = if (lives < maxLives) "+1 in $formattedTimer" else "Max Hearts Full",
                                    color = if (lives < maxLives) Color(0xFFFDE047) else Color(0xFF6EE7B7),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Life Packs List
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(packs) { pack ->
                            val isGemPack = pack.gemCost > 0
                            val isAffordable = if (isGemPack) gems >= pack.gemCost else true
                            val cardAlpha = if (isGemPack && !isAffordable) 0.4f else 1.0f

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isGemPack && !isAffordable) Color(0xFF0F172A) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    when {
                                        pack.isAd -> Color(0xFF10B981)
                                        pack.realMoneyPrice.isNotEmpty() -> Color(0xFFF59E0B)
                                        isAffordable -> Color(0xFFF43F5E)
                                        else -> Color(0xFF334155)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(cardAlpha)
                                    .testTag("life_pack_${pack.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pack Info
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(if (isGemPack && !isAffordable) Color(0xFF1E293B) else Color(0xFF334155), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = pack.iconEmoji, fontSize = 22.sp)
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "+${pack.livesCount} Lives",
                                                    color = if (isGemPack && !isAffordable) Color(0xFF94A3B8) else Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when {
                                                        pack.isAd -> Color(0xFF047857)
                                                        pack.realMoneyPrice.isNotEmpty() -> Color(0xFFB45309)
                                                        isAffordable -> Color(0xFF9F1239)
                                                        else -> Color(0xFF334155)
                                                    }
                                                ) {
                                                    Text(
                                                        text = pack.tag,
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = when {
                                                    pack.isAd -> "Watch 1 quick ad"
                                                    pack.realMoneyPrice.isNotEmpty() -> "Direct IAP Store"
                                                    else -> "${pack.gemCost} Gems cost"
                                                },
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Action Button
                                    Button(
                                        onClick = {
                                            if (pack.isAd) {
                                                showAdWatchingSimulator = true
                                            } else if (pack.realMoneyPrice.isNotEmpty()) {
                                                selectedPackForCheckout = pack
                                            } else if (isAffordable) {
                                                val bought = viewModel.buyLifePack(pack)
                                                if (!bought) {
                                                    viewModel.openOutOfGemsOffer(true)
                                                }
                                            }
                                        },
                                        enabled = if (isGemPack) isAffordable else true,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                pack.isAd -> Color(0xFF10B981)
                                                pack.realMoneyPrice.isNotEmpty() -> Color(0xFFF59E0B)
                                                isAffordable -> Color(0xFFE11D48)
                                                else -> Color(0xFF1E293B)
                                            },
                                            disabledContainerColor = Color(0xFF1E293B),
                                            disabledContentColor = Color(0xFF64748B)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                pack.isAd -> "WATCH AD"
                                                pack.realMoneyPrice.isNotEmpty() -> pack.realMoneyPrice
                                                else -> "${pack.gemCost} 💎"
                                            },
                                            color = when {
                                                pack.realMoneyPrice.isNotEmpty() -> Color.Black
                                                isAffordable -> Color.White
                                                else -> Color(0xFF64748B)
                                            },
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Informative Footer
                    Text(
                        text = "💡 Lives allow you to retry without losing your stage milestone progress.",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Free Ad Watching Simulator Dialog
    if (showAdWatchingSimulator) {
        var adTimer by remember { mutableIntStateOf(3) }
        LaunchedEffect(Unit) {
            while (adTimer > 0) {
                kotlinx.coroutines.delay(1000L)
                adTimer--
            }
            val adPack = packs.firstOrNull { it.isAd } ?: com.mygames.stickmanrush.model.LifeShopPack("ad", 2, isAd = true)
            viewModel.buyLifePack(adPack)
            showAdWatchingSimulator = false
        }

        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "📺", fontSize = 36.sp)
                    Text(
                        text = "SPONSORED ADVERTISEMENT",
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Rewarding you with +2 Free Lives in ${adTimer}s...",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    LinearProgressIndicator(
                        progress = { (3 - adTimer) / 3f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF334155)
                    )
                }
            }
        }
    }

    // Checkout Confirmation Dialog for Real Money Life Pack
    selectedPackForCheckout?.let { pack ->
        Dialog(
            onDismissRequest = { selectedPackForCheckout = null },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "🛒 CONFIRM STORE PURCHASE", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(
                        text = "Unlock +${pack.livesCount} Lives immediately for ${pack.realMoneyPrice}?",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedPackForCheckout = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = {
                                viewModel.buyLifePackRealMoney(pack, activity)
                                selectedPackForCheckout = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("BUY NOW", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 🌌 EnvironmentThemeDialog: Production-grade modal system to browse, preview,
 * and toggle background assets, celestial atmospheres, and platform styles.
 */
@Composable
fun EnvironmentThemeDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme by viewModel.engine.currentStage.collectAsState()
    val allThemes = remember { com.mygames.stickmanrush.game.StageThemes.stages }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF070D1E),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF334155)),
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("theme_selection_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Top Header Bar: Title, Badge, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E1B4B),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA855F7)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🌌", fontSize = 22.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "REALM & ENVIRONMENT THEMES",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Toggle dynamic backgrounds, skies & bridge platforms",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("theme_dialog_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // 2. Active Theme Hero Showcase Banner
                val currentIcon = remember(currentTheme) { com.mygames.stickmanrush.game.StageThemes.getThemeIcon(currentTheme) }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(18.dp))
                        .testTag("active_theme_hero_card")
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        currentTheme.bgTopColor,
                                        currentTheme.bgBottomColor.copy(alpha = 0.85f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x66000000),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = currentIcon, fontSize = 26.sp)
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981)
                                        ) {
                                            Text(
                                                text = "ACTIVE REALM",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "Stage ${currentTheme.stageNumber}",
                                            color = Color(0xFF7DD3FC),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = currentTheme.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = currentTheme.ambientDescription,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 10.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Quick Cycle / Random buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.cycleNextTheme() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("theme_quick_cycle_button")
                                ) {
                                    Text(text = "NEXT ➡️", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Grid of all 13 Themes
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("themes_grid"),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allThemes, key = { it.stageNumber }) { stage ->
                        val isEquipped = currentTheme.stageNumber == stage.stageNumber
                        val stageIcon = remember(stage) { com.mygames.stickmanrush.game.StageThemes.getThemeIcon(stage) }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isEquipped) 2.dp else 1.dp,
                                if (isEquipped) Color(0xFF38BDF8) else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(if (isEquipped) 8.dp else 2.dp, RoundedCornerShape(16.dp))
                                .testTag("theme_card_stage_${stage.stageNumber}")
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Sky Gradient & Celestial Preview Container
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    stage.bgTopColor,
                                                    stage.bgBottomColor
                                                )
                                            )
                                        )
                                        .padding(8.dp)
                                ) {
                                    // Celestial Icon & Stage Number Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0x99000000)
                                        ) {
                                            Text(
                                                text = "STAGE ${stage.stageNumber}",
                                                color = Color(0xFFFDE047),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(text = stageIcon, fontSize = 22.sp)
                                    }

                                    // Platform preview bar at bottom of sky
                                    Surface(
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                        color = stage.platformColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, stage.platformHighlightColor),
                                        modifier = Modifier
                                            .fillMaxWidth(0.55f)
                                            .height(10.dp)
                                            .align(Alignment.BottomCenter)
                                    ) {}
                                }

                                // Theme Info and Action Button
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${stage.stageNumber}. ${stage.name}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )

                                    Text(
                                        text = stage.ambientDescription,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp,
                                        maxLines = 2,
                                        lineHeight = 12.sp,
                                        modifier = Modifier.height(24.dp)
                                    )

                                    Button(
                                        onClick = { viewModel.selectTheme(stage) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEquipped) Color(0xFF0284C7) else Color(0xFF1E293B)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isEquipped) Color(0xFF38BDF8) else Color(0xFF475569)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .testTag("theme_select_button_${stage.stageNumber}")
                                    ) {
                                        Text(
                                            text = if (isEquipped) "✓ EQUIPPED" else "APPLY REALM",
                                            color = if (isEquipped) Color.White else Color(0xFFE2E8F0),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Bottom Action Row: Random Theme & Back
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val randomStage = allThemes.random()
                            viewModel.selectTheme(randomStage)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("theme_random_button")
                    ) {
                        Text(text = "🎲 RANDOM REALM", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("theme_done_button")
                    ) {
                        Text(text = "DONE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NicknameSetupDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    isInitialSetup: Boolean = false
) {
    val currentNickname by viewModel.nickname.collectAsState()
    var textInput by remember { mutableStateOf(currentNickname) }
    val isBlank = textInput.isBlank()

    Dialog(
        onDismissRequest = { if (!isInitialSetup) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !isInitialSetup, dismissOnClickOutside = !isInitialSetup)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .shadow(28.dp, RoundedCornerShape(24.dp))
                .testTag("nickname_setup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🥷", fontSize = 28.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CHOOSE YOUR STICKMAN NICKNAME",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isInitialSetup) "Welcome to Stickman Rush! Set your legendary player handle to begin your journey." else "Update your warrior handle across local storage and cloud leaderboard.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Input + Random Dice Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { if (it.length <= 16) textInput = it },
                        placeholder = { Text("e.g. ShadowBlade", color = Color(0xFF64748B)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nickname_input")
                    )

                    // Dice Randomizer Button
                    IconButton(
                        onClick = {
                            textInput = viewModel.randomStickmanNicknames.random()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                            .testTag("nickname_dice_button")
                    ) {
                        Text(text = "🎲", fontSize = 22.sp)
                    }
                }

                // Confirm Button
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.updateNickname(textInput.trim())
                            onDismiss()
                        }
                    },
                    enabled = !isBlank,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("nickname_confirm_button")
                ) {
                    Text(
                        text = "CONFIRM NICKNAME",
                        color = if (!isBlank) Color(0xFF0F172A) else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OutOfWoodPlanksDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firestorePlanks by viewModel.repository.firestoreWoodPlanks.collectAsState()

    LaunchedEffect(firestorePlanks) {
        if (firestorePlanks > 0) {
            viewModel.dismissOutOfPlanksDialog()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xDD020617))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .shadow(32.dp, RoundedCornerShape(26.dp), ambientColor = Color(0xFF10B981))
                    .testTag("out_of_wood_planks_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF064E3B),
                                    Color(0xFF0F172A),
                                    Color(0xFF020617)
                                )
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF065F46), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🪵", fontSize = 36.sp)
                    }

                    Text(
                        text = "OUT OF WOOD PLANKS!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "You need Wood Planks to construct bridges across canyons. Refill your planks now to keep playing!",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    // Option 1: Watch Short Ad -> Get +10 Free Planks
                    Button(
                        onClick = {
                            viewModel.watchAdForPlanks()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("watch_ad_planks_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📺", fontSize = 18.sp)
                            Text(
                                text = "WATCH AD -> GET +10 FREE PLANKS",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Option 2: Use 20 Gems -> Get +20 Planks
                    Button(
                        onClick = {
                            viewModel.buyPlanksWithGems()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("buy_planks_gems_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "💎", fontSize = 18.sp)
                            Text(
                                text = "USE 20 GEMS -> GET +20 PLANKS",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Option 3: Main Menu / Claim Daily
                    TextButton(
                        onClick = {
                            viewModel.dismissOutOfPlanksDialog()
                            viewModel.openMainMenu(true)
                        },
                        modifier = Modifier.testTag("main_menu_planks_button")
                    ) {
                        Text(
                            text = "🏠 Return to Main Menu / Claim Daily",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
