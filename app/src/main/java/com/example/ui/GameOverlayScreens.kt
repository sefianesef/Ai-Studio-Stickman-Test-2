package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AccessoryItem
import com.example.model.AccessoryType
import com.example.model.CurrencyType
import com.example.model.GemPack
import com.example.model.GameState
import com.example.model.ItemRarity
import com.example.model.LeaderboardEntry
import com.example.model.TournamentLeague
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameHud(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val score by viewModel.engine.score.collectAsState()
    val currentLevel by viewModel.engine.currentLevel.collectAsState()
    val gems by viewModel.gems.collectAsState()
    val currentStage by viewModel.engine.currentStage.collectAsState()
    val tier by viewModel.engine.difficultyTier.collectAsState()
    val gemCombo by viewModel.engine.gemCombo.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val gameState by viewModel.engine.gameState.collectAsState()
    val dailyMissions by viewModel.dailyMissions.collectAsState()
    val uncompletedClaimableCount = dailyMissions.count { it.currentProgress >= it.targetCount && !it.isClaimed }

    if (gameState == GameState.START) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gem Counter & Shop Button
            Surface(
                onClick = { viewModel.openShop(true) },
                shape = RoundedCornerShape(20.dp),
                color = Color(0x990F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4438BDF8)),
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .testTag("hud_gem_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "💎", fontSize = 16.sp)
                    Text(
                        text = "$gems",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Shop",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

                // Level, Stage & Difficulty Tier Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xCC0B1329),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF38BDF8))
                            .testTag("stage_indicator")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = "🚩", fontSize = 13.sp)
                            Text(
                                text = "LV $currentLevel • ${currentStage.name.uppercase()}",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Difficulty Rank Badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(tier.badgeColorHex).copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(tier.badgeColorHex)),
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color(tier.badgeColorHex))
                            .testTag("hud_difficulty_tier")
                    ) {
                        Text(
                            text = tier.title.uppercase(),
                            color = Color(tier.badgeColorHex),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

            // Action Buttons (Menu, Daily Missions, Weekly, Leaderboard, Spin, Sound & Pause)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Persistent Daily Missions Button (with claimable red badge)
                Box {
                    IconButton(
                        onClick = { viewModel.openDailyMissions(true) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (uncompletedClaimableCount > 0) Color(0xCC065F46) else Color(0x770F172A), CircleShape)
                            .testTag("hud_missions_button")
                    ) {
                        Text(text = "🎯", fontSize = 16.sp)
                    }
                    if (uncompletedClaimableCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .background(Color(0xFFEF4444), CircleShape)
                        )
                    }
                }

                // Weekly Trials Button
                IconButton(
                    onClick = { viewModel.openWeeklyMissions(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x770F172A), CircleShape)
                        .testTag("hud_weekly_missions_button")
                ) {
                    Text(text = "⚡", fontSize = 16.sp)
                }

                // Leaderboard Button
                IconButton(
                    onClick = { viewModel.openLeaderboard(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x770F172A), CircleShape)
                        .testTag("hud_leaderboard_button")
                ) {
                    Text(text = "🏆", fontSize = 16.sp)
                }

                // Master Game Menu Button
                IconButton(
                    onClick = { viewModel.openMainMenu(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x992563EB), CircleShape)
                        .testTag("hud_main_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Game Menu",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Pause Button
                IconButton(
                    onClick = { viewModel.openPauseMenu(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x770F172A), CircleShape)
                        .testTag("hud_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            val nextMilestone = ((currentLevel / 5) + 1) * 5
            val levelsRemaining = nextMilestone - currentLevel
            val progress = ((5 - levelsRemaining).toFloat() / 5f).coerceIn(0f, 1f)

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
                            text = if (levelsRemaining == 1) "🔥" else "🏆",
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (levelsRemaining == 1) {
                                "⚡ YOU ARE JUST 1 LEVEL AWAY FROM THE BIG REWARD!"
                            } else {
                                "🎯 LEVEL $nextMilestone MILESTONE: $levelsRemaining BRIDGES TO GO!"
                            },
                            color = if (levelsRemaining == 1) Color(0xFFFDE047) else Color(0xFFFBBF24),
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
                            text = "Lv $currentLevel",
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
                                    .fillMaxWidth(fraction = progress.coerceAtLeast(0.08f))
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
}

@Composable
fun StartScreenOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val highScore by viewModel.highScore.collectAsState()
    val gems by viewModel.gems.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val isDailyRewardAvailable by viewModel.isDailyRewardAvailable.collectAsState()

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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            // Top Bar: Gems, Master Menu, Daily Streak Tracker, & High Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gems
                Surface(
                    onClick = { viewModel.openShop(true) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6638BDF8)),
                    modifier = Modifier.testTag("start_gems_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = "💎", fontSize = 16.sp)
                        Text(
                            text = "$gems",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Master Game Menu Button
                Surface(
                    onClick = { viewModel.openMainMenu(true) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEE2563EB),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF60A5FA)),
                    modifier = Modifier.testTag("start_master_menu_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "MENU",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                // Daily Streak Tracker Pill
                Surface(
                    onClick = { viewModel.openDailyReward(true) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDailyRewardAvailable) Color(0xFF831843) else Color(0xDD0F172A),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (isDailyRewardAvailable) Color(0xFFF43F5E) else Color(0x66F97316)
                    ),
                    modifier = Modifier
                        .scale(if (isDailyRewardAvailable) streakGlowScale else 1f)
                        .testTag("start_daily_streak_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🔥", fontSize = 15.sp)
                        Text(
                            text = "Day $currentStreak",
                            color = if (isDailyRewardAvailable) Color(0xFFFDE047) else Color(0xFFFB923C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // High score badge
                Surface(
                    onClick = { viewModel.openLeaderboard(true) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xDD0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FBBF24)),
                    modifier = Modifier.testTag("start_high_score_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🏆", fontSize = 15.sp)
                        Text(
                            text = "$highScore",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Center: Title, Daily Gift Card & Tap Prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Game Logo Icon
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFF10B981)),
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color(0xFF10B981), spotColor = Color(0xFF10B981))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🥷", fontSize = 44.sp)
                    }
                }

                // Title
                Text(
                    text = "STICKMAN\nHERO",
                    color = Color.White,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(8.dp))
                        .testTag("app_title_text")
                )

                Text(
                    text = "Bridge Master & Gem Rush",
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )

                // Daily Streak Banner (Always informative & engaging)
                Surface(
                    onClick = { viewModel.openDailyReward(true) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDailyRewardAvailable) Color(0xEE1E1B4B) else Color(0xDD0F172A),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isDailyRewardAvailable) Color(0xFFEC4899) else Color(0xFFF97316)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .scale(if (isDailyRewardAvailable) streakGlowScale else 1f)
                        .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = if (isDailyRewardAvailable) Color(0xFFEC4899) else Color(0xFFF97316))
                        .testTag("start_daily_streak_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = if (isDailyRewardAvailable) "🎁" else "🔥", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = if (isDailyRewardAvailable) "Daily Login Reward Ready!" else "Daily Streak: Day $currentStreak Active 🔥",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isDailyRewardAvailable) "Day $currentStreak Reward • Tap to claim gems" else "Keep your login streak alive for Day 7 Mega Bonus!",
                                    color = if (isDailyRewardAvailable) Color(0xFFF472B6) else Color(0xFFFB923C),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDailyRewardAvailable) Color(0xFF065F46) else Color(0xFF334155)
                        ) {
                            Text(
                                text = if (isDailyRewardAvailable) "CLAIM" else "VIEW",
                                color = if (isDailyRewardAvailable) Color(0xFFFDE047) else Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Persistent Level Progress & Milestone Card (Always visible across Home / Start screen)
                val startLevel by viewModel.engine.currentLevel.collectAsState()
                val nextMilestone = ((startLevel / 5) + 1) * 5
                val levelsRemaining = nextMilestone - startLevel
                val milestoneProgress = ((5 - levelsRemaining).toFloat() / 5f).coerceIn(0f, 1f)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xDD0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFF59E0B))
                        .testTag("start_persistent_level_progress_card")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(text = "🏆", fontSize = 14.sp)
                                Text(
                                    text = "CURRENT STAGE: LEVEL $startLevel",
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "Milestone Lv $nextMilestone 🎁",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Persistent Linear Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color(0x44334155), CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = milestoneProgress.coerceAtLeast(0.06f))
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF38BDF8))
                                        ),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Play Button Card
                Surface(
                    onClick = { viewModel.engine.startGame() },
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .scale(pulseScale)
                        .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = Color(0xFF10B981))
                        .testTag("start_play_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 36.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "TAP TO PLAY",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Instruction Hint
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x66000000),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Hold screen to grow bridge • Release to drop\nTap while walking to flip for gems",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Bottom Bar: Shop, Daily Rewards, Quests & How to play
            val dailyMissions by viewModel.dailyMissions.collectAsState()
            val uncompletedQuestsCount = dailyMissions.count { it.currentProgress >= it.targetCount && !it.isClaimed }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.openLeaderboard(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("start_open_leaderboard_button")
                ) {
                    Text(text = "🏆", fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ranks",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                Button(
                    onClick = { viewModel.openSpinWheel(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("start_open_spin_button")
                ) {
                    Text(text = "🎰", fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Spin",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))

                Button(
                    onClick = { viewModel.openShop(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("start_open_shop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = "Costumes",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Shop",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = { viewModel.openDailyReward(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier.testTag("start_open_daily_button")
                ) {
                    Text(text = "🎁", fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Gifts",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box {
                    Button(
                        onClick = { viewModel.openDailyMissions(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("start_open_quests_button")
                    ) {
                        Text(text = "🎯", fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Quests",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    if (uncompletedQuestsCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .background(Color(0xFFEF4444), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { viewModel.openHowToPlay(true) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .testTag("start_how_to_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "How to play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
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
    val isNewHigh by viewModel.engine.isNewHighScore.collectAsState()
    val lastNearMiss by viewModel.engine.lastNearMiss.collectAsState()
    val revivalsUsed by viewModel.engine.revivalsUsed.collectAsState()
    val reviveCost = viewModel.engine.getReviveCost()
    val canAffordRevive = totalGems >= reviveCost

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
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            Text(
                                "$highScore",
                                color = Color(0xFFFBBF24),
                                fontSize = 20.sp,
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
                                onClick = { viewModel.openShop(true) },
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
                                    text = "NEED ${reviveCost - totalGems} GEMS TO REVIVE • TOP UP",
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

                // Play Again Button
                Button(
                    onClick = { viewModel.engine.resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("game_over_retry_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "TRY AGAIN",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                // Secondary Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.openLeaderboard(true) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("game_over_leaderboard_button")
                    ) {
                        Text(text = "🏆 Rank", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.openShop(true) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("game_over_shop_button")
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Shop", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shop", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.engine.resetGame(initial = true) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("game_over_home_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Menu", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ShopDialog(
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
                        AccessoryType.BODY_SKIN to ("🦸 Outfits" to viewModel.availableAccessories.count { it.type == AccessoryType.BODY_SKIN }),
                        AccessoryType.STICK to ("🥢 Bridges" to viewModel.availableAccessories.count { it.type == AccessoryType.STICK }),
                        AccessoryType.HAT to ("👑 Hats" to viewModel.availableAccessories.count { it.type == AccessoryType.HAT }),
                        AccessoryType.SCARF to ("🧣 Capes" to viewModel.availableAccessories.count { it.type == AccessoryType.SCARF }),
                        AccessoryType.THEME to ("🌌 Realms" to viewModel.availableAccessories.count { it.type == AccessoryType.THEME }),
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
                                    text = if (type == AccessoryType.GEM_VAULT) "💎 Vault" else label.split(" ")[0],
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
                                viewModel.repository.addGems(25)
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
                        desc = "Tap while stickman is walking to flip upside-down and grab gems hanging under the bridge! Tap again before hitting the wall."
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
                        Button(
                            onClick = {
                                val awarded = viewModel.claimDailyReward()
                                claimedNotice = "🎉 Claimed +$awarded Gems! Streak is now $currentStreak days!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .scale(rewardGlow)
                                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF10B981))
                                .testTag("claim_daily_reward_button")
                        ) {
                            Text(text = "🎁", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CLAIM +$rewardGems GEMS",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
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
                                            Button(
                                                onClick = { viewModel.claimDailyMission(mission.id, mission.rewardGems) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("claim_mission_${mission.id}")
                                            ) {
                                                Text(
                                                    text = "CLAIM 💎+${mission.rewardGems}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black
                                                )
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
                        Button(
                            onClick = { viewModel.claimAllDailyMissions() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("claim_all_daily_missions_button")
                        ) {
                            Text(
                                text = "✨ CLAIM ALL ($readyToClaimCount QUESTS • +$readyToClaimTotalGems 💎)",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
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
                viewModel.buyGemPackRealMoney(pack)
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
    var wonAmount by remember { mutableStateOf<Int?>(null) }
    val rotationAngle = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
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
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val isFreeSpinAvailable = remember { viewModel.isDailyFreeSpinAvailable() }
                val spinCost = viewModel.getSpinCost()
                val canAfford = viewModel.canAffordSpin()

                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🎰", fontSize = 22.sp)
                    Text(
                        text = "LUCKY GEM WHEEL",
                        color = Color(0xFFFBBF24),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                if (isFreeSpinAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF065F46),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399))
                    ) {
                        Text(
                            text = "🎁 FREE DAILY SPIN READY!",
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Next Free Spin tomorrow • Spin now for 💎 $spinCost",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Spinning Wheel Canvas
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp),
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
                        }

                        // Center Hub
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 24.dp.toPx(),
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            radius = 16.dp.toPx(),
                            center = center
                        )
                    }

                    // Indicator Pin
                    Text(
                        text = "🔻",
                        fontSize = 26.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-14).dp)
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
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Spin Action Button
                Button(
                    onClick = {
                        if (!isSpinning && canAfford) {
                            isSpinning = true
                            wonAmount = null
                            coroutineScope.launch {
                                val targetAngle = rotationAngle.value + 1440f + (0..360).random()
                                rotationAngle.animateTo(
                                    targetValue = targetAngle,
                                    animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
                                )
                                val prize = viewModel.spinLuckyWheel()
                                wonAmount = prize
                                isSpinning = false
                            }
                        }
                    },
                    enabled = !isSpinning && canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFreeSpinAvailable) Color(0xFF10B981) else Color(0xFFF59E0B),
                        disabledContainerColor = Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spin_wheel_action_btn")
                ) {
                    Text(
                        text = when {
                            isSpinning -> "SPINNING..."
                            isFreeSpinAvailable -> "CLAIM FREE SPIN! 🎁"
                            canAfford -> "SPIN FOR 💎 $spinCost"
                            else -> "NEED 💎 $spinCost TO SPIN"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !isSpinning,
                    modifier = Modifier.fillMaxWidth()
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
fun LevelVictoryCelebrationDialog(
    celebrationText: String,
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Popping Crown & Sparkles Icon
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF312E81),
                    border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFFFD700)),
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(16.dp, CircleShape, ambientColor = Color(0xFFFFD700))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "👑", fontSize = 42.sp)
                    }
                }

                Text(
                    text = "LEVEL COMPLETED! 🎉",
                    color = Color(0xFFFFD700),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                // High-Dopamine Motivation Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF065F46),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF34D399)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎁 +3 Bonus Gems Claimed! 💎",
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF10B981))
                        .testTag("continue_next_level_button")
                ) {
                    Text(
                        text = "CONTINUE TO NEXT LEVEL 🚀",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

