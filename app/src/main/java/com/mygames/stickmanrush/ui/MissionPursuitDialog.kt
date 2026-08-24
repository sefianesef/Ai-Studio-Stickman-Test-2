package com.mygames.stickmanrush.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygames.stickmanrush.data.local.entity.DailyMissionEntity

@Composable
fun RoyalMissionPursuitDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Exact game missions from Room Database Flow in GameViewModel
    val dailyMissions by viewModel.dailyMissions.collectAsState()
    val totalGems by viewModel.gems.collectAsState()

    val totalMissions = dailyMissions.size
    val completedCount = dailyMissions.count { it.isClaimed }
    val readyToClaimCount = dailyMissions.count { it.currentProgress >= it.targetCount && !it.isClaimed }

    // Pulsing reward animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

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
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Royal blue framed dialog container with curved edges & golden border
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0F4C81),
                border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFFEAB308)),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .shadow(32.dp, RoundedCornerShape(26.dp))
                    .testTag("royal_mission_pursuit_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF075985), Color(0xFF0C4A6E))
                            )
                        )
                ) {
                    // 1. Top Title Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0369A1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "💎", fontSize = 13.sp)
                                Text(text = "$totalGems", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Daily Quests",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .shadow(8.dp)
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.CenterEnd)
                                .background(Color(0xFFDC2626), CircleShape)
                                .border(2.dp, Color(0xFFFEF08A), CircleShape)
                                .shadow(6.dp, CircleShape)
                                .testTag("mission_pursuit_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // 2. Detective / Quests Banner Header
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E1B4B),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(105.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF4338CA), Color(0xFF312E81), Color(0xFF1E1B4B))
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ninja / Game Hero Emblem
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFDE047),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🥷", fontSize = 28.sp)
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFBAE6FD),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🎯", fontSize = 22.sp)
                                    }
                                }
                            }

                            // Center Clue Board Status
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF78350F),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "📜", fontSize = 14.sp)
                                        Text(
                                            text = "ACTIVE QUESTS",
                                            color = Color(0xFFFEF08A),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF7C2D12),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDBA74))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "⏱️", fontSize = 11.sp)
                                        Text(
                                            text = "Resets Daily at Midnight",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Prize Chest Vault
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFD97706),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFEF08A)),
                                modifier = Modifier
                                    .size(52.dp)
                                    .scale(if (readyToClaimCount > 0) pulseScale else 1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = if (completedCount == totalMissions && totalMissions > 0) "👑" else "🎁", fontSize = 28.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Progress Milestones Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0369A1),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Progression",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "$completedCount / $totalMissions Claimed",
                                    color = if (completedCount == totalMissions && totalMissions > 0) Color(0xFF4ADE80) else Color(0xFFFEF08A),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }

                            // Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .background(Color(0xFF082F49), CircleShape)
                                    .border(1.dp, Color(0xFF38BDF8), CircleShape)
                            ) {
                                val progressFraction = if (totalMissions > 0) (completedCount.toFloat() / totalMissions.toFloat()).coerceIn(0f, 1f) else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = progressFraction.coerceAtLeast(0.06f))
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFFBBF24), Color(0xFF34D399), Color(0xFF38BDF8))
                                            ),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = "${(progressFraction * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Exact Game Missions List with Video Layout & Animations
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dailyMissions, key = { it.id }) { mission ->
                            val isGoalMet = mission.currentProgress >= mission.targetCount
                            val canClaim = isGoalMet && !mission.isClaimed
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
                                color = if (mission.isClaimed) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (canClaim) 2.dp else 1.dp,
                                    color = when {
                                        mission.isClaimed -> Color(0xFF059669)
                                        canClaim -> Color(0xFF22C55E)
                                        else -> Color(0xFF38BDF8).copy(alpha = 0.6f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mission_item_${mission.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left Icon
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF334155),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = iconSymbol, fontSize = 22.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Center Info & Big Green Claim Button
                                    if (canClaim) {
                                        Button(
                                            onClick = {
                                                viewModel.claimDailyMission(mission.id, mission.rewardGems)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .scale(pulseScale)
                                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                                .testTag("mission_claim_btn_${mission.id}")
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "Claim",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = mission.title,
                                                    color = Color(0xFFDCFCE7),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = mission.title,
                                                color = if (mission.isClaimed) Color(0xFF94A3B8) else Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = mission.description,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )

                                            // Progress bar
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Right Reward Pill
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (mission.isClaimed) Color(0xFF15803D) else Color(0xFF047857),
                                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF4ADE80))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(text = if (mission.isClaimed) "✓" else "💎", fontSize = 13.sp)
                                            Text(
                                                text = if (mission.isClaimed) "DONE" else "+${mission.rewardGems}",
                                                color = Color(0xFFFEF08A),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Bottom Info Bar
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                        color = Color(0xFF0B2545),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Complete in-game bridges, flips, and high scores to earn daily gems!",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
