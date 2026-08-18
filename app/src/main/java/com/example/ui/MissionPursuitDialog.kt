package com.example.ui

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

data class MissionPursuitItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val current: Int,
    val target: Int,
    val magnifyingGlasses: Int,
    val isClaimed: Boolean = false
)

@Composable
fun RoyalMissionPursuitDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStage by remember { mutableIntStateOf(1) }
    val totalGems by viewModel.gems.collectAsState()

    var stageProgress by remember { mutableIntStateOf(320) }
    val maxStageGoal = 3000

    var missions by remember {
        mutableStateOf(
            listOf(
                MissionPursuitItem("1", "Claim", "Complete 6 Steps!", "🎀", 6, 6, 20, false),
                MissionPursuitItem("2", "Claim", "Earn 1000 Coins / Gems!", "💰", 1000, 1000, 30, false),
                MissionPursuitItem("3", "Claim", "Collect 5 Cards / Stickers!", "🃏", 5, 5, 40, false),
                MissionPursuitItem("4", "Find 8 Objects!", "Search throughout the area", "🔍", 6, 8, 50, false),
                MissionPursuitItem("5", "Win 1 Super Hard Level!", "Overcome narrow bridges", "🌈", 1, 1, 70, false)
            )
        )
    }

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
            // Royal blue framed dialog container
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
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.CenterStart)
                                .background(Color(0xFF0369A1), CircleShape)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Text(
                            text = "Mission Pursuit",
                            color = Color.White,
                            fontSize = 24.sp,
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

                    // 2. Detective King & Dog Investigation Banner
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E1B4B),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(115.dp)
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
                            // King & Dog Emblems
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFDE047),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "👑", fontSize = 30.sp)
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFBAE6FD),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🐶", fontSize = 24.sp)
                                    }
                                }
                            }

                            // Center Clue Board & Status
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "🗺️", fontSize = 16.sp)
                                        Text(
                                            text = "CLUE BOARD",
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
                                        Text(text = "⏱️", fontSize = 12.sp)
                                        Text(
                                            text = "Active Event: 2d 15h",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Big Chest Vault
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFD97706),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFEF08A)),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🎁", fontSize = 30.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Chest Tier Milestones (500, 1500, 3000) & Magnifying Glass Progress Bar
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
                            // Chest Milestones
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChestTierPill(points = 500, icon = "🪵", isReached = stageProgress >= 500)
                                ChestTierPill(points = 1500, icon = "🥈", isReached = stageProgress >= 1500)
                                ChestTierPill(points = 3000, icon = "👑", isReached = stageProgress >= 3000)
                            }

                            // Progress Bar with Magnifying Glass Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🔍", fontSize = 16.sp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .background(Color(0xFF082F49), CircleShape)
                                        .border(1.dp, Color(0xFF38BDF8), CircleShape)
                                ) {
                                    val progressFraction = (stageProgress.toFloat() / maxStageGoal.toFloat()).coerceIn(0f, 1f)
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
                                        text = "$stageProgress / $maxStageGoal",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Missions List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(missions, key = { it.id }) { mission ->
                            val canClaim = mission.current >= mission.target && !mission.isClaimed

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (mission.isClaimed) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (canClaim) 2.dp else 1.dp,
                                    color = if (canClaim) Color(0xFF22C55E) else Color(0xFF38BDF8).copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.fillMaxWidth()
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
                                            Text(text = mission.iconEmoji, fontSize = 22.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Center Info & Big Green Claim Button
                                    if (canClaim) {
                                        Button(
                                            onClick = {
                                                missions = missions.map {
                                                    if (it.id == mission.id) it.copy(isClaimed = true) else it
                                                }
                                                stageProgress += mission.magnifyingGlasses * 10
                                                viewModel.soundManager.playVictoryMusic()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                                .testTag("mission_pursuit_claim_btn_${mission.id}")
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "Claim",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = mission.subtitle,
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
                                                text = mission.subtitle,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
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
                                            Text(text = if (mission.isClaimed) "✓" else "🔍", fontSize = 13.sp)
                                            Text(
                                                text = if (mission.isClaimed) "DONE" else "x${mission.magnifyingGlasses}",
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

                    // 5. Bottom Stage Tabs (Stage 1 to Stage 5)
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                        color = Color(0xFF0B2545),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (stage in 1..5) {
                                val isSelected = selectedStage == stage
                                Surface(
                                    onClick = { selectedStage = stage },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFFBBF24) else Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFFEF08A) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "Stage $stage",
                                        color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChestTierPill(points: Int, icon: String, isReached: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isReached) Color(0xFF15803D) else Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, if (isReached) Color(0xFF4ADE80) else Color(0xFFFBBF24))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 13.sp)
            Text(
                text = "$points",
                color = if (isReached) Color(0xFFBBF7D0) else Color(0xFFFEF08A),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}
