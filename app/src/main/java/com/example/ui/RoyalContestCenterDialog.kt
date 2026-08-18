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
import com.example.model.ContestTournament

@Composable
fun RoyalContestCenterDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("CONTESTS") }
    val totalGems by viewModel.gems.collectAsState()
    val highScore by viewModel.highScore.collectAsState()

    // Real game contests from GameViewModel
    val contestsState = remember { mutableStateOf(viewModel.getContests()) }

    fun refreshContests() {
        contestsState.value = viewModel.getContests()
    }

    // Dynamic gradient color maps for the game's actual contests
    val bgGradients = mapOf(
        "speed_builder" to listOf(Color(0xFFEAB308), Color(0xFFCA8A04), Color(0xFF854D0E)),
        "gem_frenzy" to listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF075985)),
        "perfect_aim" to listOf(Color(0xFFEC4899), Color(0xFFBE185D), Color(0xFF831843)),
        "endless_master" to listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFF4C1D95))
    )

    val borderColors = mapOf(
        "speed_builder" to Color(0xFFFEF08A),
        "gem_frenzy" to Color(0xFFBAE6FD),
        "perfect_aim" to Color(0xFFFBCFE8),
        "endless_master" to Color(0xFFDDD6FE)
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
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFFF59E0B)),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .shadow(32.dp, RoundedCornerShape(26.dp))
                    .testTag("royal_contest_center_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF020617))
                            )
                        )
                ) {
                    // Header Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF065F46),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)),
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
                            text = "Leaderboard & Events",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.CenterEnd)
                                .background(Color(0xFFDC2626), CircleShape)
                                .border(2.dp, Color(0xFFFEF08A), CircleShape)
                                .shadow(6.dp, CircleShape)
                                .testTag("contest_center_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Top Navigation Tabs
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Weekly", "Friends", "Players", "Teams").forEach { tab ->
                                val isTabSelected = selectedCategory == tab.uppercase() || (selectedCategory == "CONTESTS" && tab == "Weekly")
                                Surface(
                                    onClick = { selectedCategory = tab.uppercase() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isTabSelected) Color(0xFF3B82F6) else Color.Transparent,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = tab,
                                        color = if (isTabSelected) Color.White else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Top 3 Podium Highlights Banner
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF334155), Color(0xFF1E293B))
                                    )
                                )
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "👑 WEEKLY CONTEST PODIUM",
                                color = Color(0xFFFEF08A),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )

                            // Podium 3-player layout
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                PodiumPlayerColumn(rank = 2, name = "dhong", score = (highScore * 0.85).toInt().coerceAtLeast(111), avatar = "🥷", color = Color(0xFF38BDF8))
                                PodiumPlayerColumn(rank = 1, name = "dane PRO", score = (highScore * 1.25).toInt().coerceAtLeast(166), avatar = "👑", color = Color(0xFFFBBF24), isLeader = true)
                                PodiumPlayerColumn(rank = 3, name = "Sven", score = (highScore * 0.65).toInt().coerceAtLeast(69), avatar = "🦊", color = Color(0xFFF97316))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Actual Contests List with video layout & animations
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(contestsState.value, key = { it.id }) { contest ->
                            val gradientColors = bgGradients[contest.id] ?: listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8), Color(0xFF1E3A8A))
                            val borderColor = borderColors[contest.id] ?: Color(0xFF93C5FD)
                            val canClaim = contest.isCompleted && !contest.isClaimed

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(18.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.horizontalGradient(gradientColors))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left Sticker & Title
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White.copy(alpha = 0.2f),
                                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(text = contest.iconEmoji, fontSize = 26.sp)
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = contest.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = contest.subtitle,
                                                    color = Color(0xFFFEF08A),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        // Right Timer & Status Badge
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.Black.copy(alpha = 0.4f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(text = "⏱️", fontSize = 11.sp)
                                                Text(
                                                    text = contest.timeRemainingStr,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    // Progress Bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(10.dp)
                                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                        ) {
                                            val frac = (contest.currentProgress.toFloat() / contest.targetGoal.toFloat()).coerceIn(0f, 1f)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction = frac.coerceAtLeast(0.08f))
                                                    .fillMaxHeight()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(Color(0xFFFEF08A), Color(0xFF4ADE80))
                                                        ),
                                                        CircleShape
                                                    )
                                            )
                                        }

                                        Text(
                                            text = "${contest.currentProgress}/${contest.targetGoal} ${contest.goalUnit}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Prize & Action
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val prizeText = buildString {
                                            append("+${contest.prizePoolGems} 💎")
                                            if (contest.prizePoolBlueGems > 0) append(" +${contest.prizePoolBlueGems} 🔷")
                                            if (contest.prizePoolRedGems > 0) append(" +${contest.prizePoolRedGems} 🔴")
                                        }

                                        Text(
                                            text = "Reward: $prizeText",
                                            color = Color(0xFFFEF08A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )

                                        if (canClaim) {
                                            Button(
                                                onClick = {
                                                    viewModel.claimContest(contest.id)
                                                    refreshContests()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(text = "CLAIM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Black.copy(alpha = 0.3f)
                                            ) {
                                                Text(
                                                    text = if (contest.isClaimed) "CLAIMED ✓" else "PLAYING",
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PodiumPlayerColumn(
    rank: Int,
    name: String,
    score: Int,
    avatar: String,
    color: Color,
    isLeader: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(if (isLeader) 2.5.dp else 1.5.dp, color),
            modifier = Modifier.size(if (isLeader) 52.dp else 42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = avatar, fontSize = if (isLeader) 28.sp else 22.sp)
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = color
        ) {
            Text(
                text = "$rank",
                color = if (color == Color(0xFFFBBF24)) Color.Black else Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }

        Text(
            text = name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )

        Text(
            text = "Score: $score",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
        )
    }
}
