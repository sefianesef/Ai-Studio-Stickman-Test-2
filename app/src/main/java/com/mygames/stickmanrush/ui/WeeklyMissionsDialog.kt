package com.mygames.stickmanrush.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mygames.stickmanrush.model.WeeklyMissionItem

@Composable
fun WeeklyMissionsDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weeklyMissions = remember { mutableStateOf(viewModel.getWeeklyMissions()) }
    val totalGems by viewModel.gems.collectAsState()

    fun refreshMissions() {
        weeklyMissions.value = viewModel.getWeeklyMissions()
    }

    val claimableCount = weeklyMissions.value.count { it.isCompleted && !it.isClaimed }
    val totalClaimableGems = weeklyMissions.value.filter { it.isCompleted && !it.isClaimed }.sumOf { it.rewardGems }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B132B),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6366F1)),
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .shadow(28.dp, RoundedCornerShape(24.dp))
                .testTag("weekly_missions_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                            shape = CircleShape,
                            color = Color(0xFF4F46E5).copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "⚡", fontSize = 22.sp)
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "WEEKLY TRIALS",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF4F46E5)
                                ) {
                                    Text(
                                        text = "EPIC",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Resets in: 4d 16h • Huge Gem Rewards",
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
                            .testTag("weekly_missions_close_button")
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

                // Gems and Progress Highlight Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x334F46E5),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0x66818CF8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Weekly Bounty Pool",
                                color = Color(0xFFC7D2FE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Up to 345 💎 Total Bounty",
                                color = Color(0xFFFBBF24),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E1B4B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$totalGems",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Weekly Missions
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weekly_missions_list")
                ) {
                    items(weeklyMissions.value, key = { it.id }) { mission ->
                        WeeklyMissionCard(
                            mission = mission,
                            onClaim = {
                                viewModel.claimWeeklyMission(mission.id, mission.rewardGems)
                                refreshMissions()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Claim All Button (if any available)
                if (claimableCount > 0) {
                    Button(
                        onClick = {
                            viewModel.claimAllWeeklyMissions()
                            refreshMissions()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFF10B981))
                            .testTag("weekly_claim_all_button")
                    ) {
                        Text(
                            text = "CLAIM ALL ($totalClaimableGems 💎)",
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
}

@Composable
private fun WeeklyMissionCard(
    mission: WeeklyMissionItem,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = (mission.currentProgress.toFloat() / mission.targetCount.toFloat()).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (mission.isClaimed) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(
            width = if (mission.isCompleted && !mission.isClaimed) 1.5.dp else 1.dp,
            color = when {
                mission.isClaimed -> Color(0xFF334155)
                mission.isCompleted -> Color(0xFF10B981)
                else -> Color(0xFF475569)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_mission_card_${mission.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = mission.iconEmoji, fontSize = 26.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = mission.title,
                                color = if (mission.isClaimed) Color(0xFF94A3B8) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = mission.description,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Reward & Action
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val rewardText = buildString {
                        append("+${mission.rewardGems} 💎")
                        if (mission.rewardBlueGems > 0) append(" +${mission.rewardBlueGems} 🔷")
                        if (mission.rewardRedGems > 0) append(" +${mission.rewardRedGems} 🔴")
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33FBBF24),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FBBF24))
                    ) {
                        Text(
                            text = rewardText,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    when {
                        mission.isClaimed -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF065F46)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text(text = "CLAIMED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        mission.isCompleted -> {
                            Button(
                                onClick = onClaim,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("weekly_claim_btn_${mission.id}")
                            ) {
                                Text(
                                    text = "CLAIM",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = "${mission.currentProgress}/${mission.targetCount}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (mission.isCompleted) Color(0xFF10B981) else Color(0xFF6366F1),
                trackColor = Color(0xFF0F172A),
                drawStopIndicator = {}
            )
        }
    }
}
