package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ProfessionalMainMenuDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalGems by viewModel.gems.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val dailyMissions by viewModel.dailyMissions.collectAsState()
    val uncompletedDailyCount = dailyMissions.count { it.currentProgress >= it.targetCount && !it.isClaimed }
    val weeklyMissions = remember { viewModel.getWeeklyMissions() }
    val uncompletedWeeklyCount = weeklyMissions.count { it.isCompleted && !it.isClaimed }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0A0F1D),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6)),
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(32.dp, RoundedCornerShape(24.dp))
                .testTag("professional_main_menu_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Player Profile Card
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
                            color = Color(0xFF0F2942),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                                    contentDescription = "Stickman Rush Logo",
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Stickman Rush",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE11D48)
                                ) {
                                    Text(
                                        text = "RUSH",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "🏆 Best: $highScore • Diamond League",
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
                            .testTag("menu_close_button")
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

                // Gem Vault Quick Indicator Banner
                Surface(
                    onClick = {
                        onDismiss()
                        viewModel.openShop(true)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E1B4B),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF818CF8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "💎", fontSize = 18.sp)
                            Text(
                                text = "Gem Vault Balance: $totalGems",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "+ STORE",
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Features Navigation Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_grid_options")
                ) {
                    // 1. Daily Missions
                    item {
                        MenuFeatureCard(
                            title = "Daily Missions",
                            subtitle = "Daily bounties & gems",
                            icon = "🎯",
                            badgeText = if (uncompletedDailyCount > 0) "$uncompletedDailyCount READY" else null,
                            badgeColor = Color(0xFF10B981),
                            borderColor = Color(0xFF10B981),
                            onClick = {
                                onDismiss()
                                viewModel.openDailyMissions(true)
                            }
                        )
                    }

                    // 2. Weekly Trials
                    item {
                        MenuFeatureCard(
                            title = "Weekly Trials",
                            subtitle = "Epic 345+ 💎 rewards",
                            icon = "⚡",
                            badgeText = if (uncompletedWeeklyCount > 0) "$uncompletedWeeklyCount READY" else "EPIC",
                            badgeColor = Color(0xFF6366F1),
                            borderColor = Color(0xFF6366F1),
                            onClick = {
                                onDismiss()
                                viewModel.openWeeklyMissions(true)
                            }
                        )
                    }

                    // 3. Global Leaderboard
                    item {
                        MenuFeatureCard(
                            title = "Global Arena",
                            subtitle = "World ranks & ladder",
                            icon = "🏆",
                            borderColor = Color(0xFFF59E0B),
                            onClick = {
                                onDismiss()
                                viewModel.openLeaderboard(true)
                            }
                        )
                    }

                    // 4. Tournaments & Contests
                    item {
                        MenuFeatureCard(
                            title = "Contests & Cups",
                            subtitle = "Live PvP grand prix",
                            icon = "⚔️",
                            badgeText = "LIVE",
                            badgeColor = Color(0xFFE11D48),
                            borderColor = Color(0xFFE11D48),
                            onClick = {
                                onDismiss()
                                viewModel.openContests(true)
                            }
                        )
                    }

                    // 5. Costume & Laser Shop
                    item {
                        MenuFeatureCard(
                            title = "Hero Shop",
                            subtitle = "Skins, laser bridges & trails",
                            icon = "👘",
                            borderColor = Color(0xFFEC4899),
                            onClick = {
                                onDismiss()
                                viewModel.openShop(true)
                            }
                        )
                    }

                    // 6. Lucky Spin Wheel
                    item {
                        MenuFeatureCard(
                            title = "Lucky Wheel",
                            subtitle = "Spin for 100 💎 jackpot",
                            icon = "🎰",
                            borderColor = Color(0xFF8B5CF6),
                            onClick = {
                                onDismiss()
                                viewModel.openSpinWheel(true)
                            }
                        )
                    }

                    // 7. Daily Login Gifts
                    item {
                        MenuFeatureCard(
                            title = "Login Rewards",
                            subtitle = "7-Day streak gifts",
                            icon = "🎁",
                            borderColor = Color(0xFF06B6D4),
                            onClick = {
                                onDismiss()
                                viewModel.openDailyReward(true)
                            }
                        )
                    }

                    // 8. Career Stats
                    item {
                        MenuFeatureCard(
                            title = "Career Stats",
                            subtitle = "Battle records & accuracy",
                            icon = "📊",
                            borderColor = Color(0xFF38BDF8),
                            onClick = {
                                onDismiss()
                                viewModel.openPlayerStats(true)
                            }
                        )
                    }

                    // 9. Game Settings
                    item {
                        MenuFeatureCard(
                            title = "Game Settings",
                            subtitle = "Audio, haptics & FPS",
                            icon = "⚙️",
                            borderColor = Color(0xFF64748B),
                            onClick = {
                                onDismiss()
                                viewModel.openSettings(true)
                            }
                        )
                    }

                    // 10. Tutorial & Guide
                    item {
                        MenuFeatureCard(
                            title = "How to Play",
                            subtitle = "Master the stick mechanics",
                            icon = "📖",
                            borderColor = Color(0xFF10B981),
                            onClick = {
                                onDismiss()
                                viewModel.openHowToPlay(true)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Button: Resume / Play
                Button(
                    onClick = {
                        onDismiss()
                        if (viewModel.engine.gameState.value == com.example.model.GameState.START) {
                            viewModel.engine.startGame()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF10B981))
                        .testTag("menu_play_continue_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAY NOW",
                        color = Color.White,
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
private fun MenuFeatureCard(
    title: String,
    subtitle: String,
    icon: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF10B981),
    borderColor: Color = Color(0xFF334155),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .testTag("menu_tile_${title.lowercase().replace(" ", "_")}")
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = icon, fontSize = 22.sp)
                    badgeText?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor
                        ) {
                            Text(
                                text = it,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
