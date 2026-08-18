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

data class ContestTabItem(
    val title: String,
    val rankOrTier: String,
    val stickerEmoji: String,
    val bgColors: List<Color>,
    val borderColor: Color,
    val timeRemaining: String,
    val progressCurrent: Int,
    val progressMax: Int,
    val prizeDesc: String,
    val isClaimable: Boolean = false,
    val isFinished: Boolean = false
)

@Composable
fun RoyalContestCenterDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("CONTESTS") } // CONTESTS, TOURNAMENTS, LEAGUES
    val totalGems by viewModel.gems.collectAsState()
    val highScore by viewModel.highScore.collectAsState()

    var activeContests by remember {
        mutableStateOf(
            listOf(
                ContestTabItem(
                    title = "King's Cup",
                    rankOrTier = "Rank #1",
                    stickerEmoji = "👑",
                    bgColors = listOf(Color(0xFFEAB308), Color(0xFFCA8A04), Color(0xFF854D0E)),
                    borderColor = Color(0xFFFEF08A),
                    timeRemaining = "15:31:20",
                    progressCurrent = 48,
                    progressMax = 50,
                    prizeDesc = "+500 💎 + Unlimited Lives",
                    isClaimable = true
                ),
                ContestTabItem(
                    title = "Sky Race",
                    rankOrTier = "Rank #2",
                    stickerEmoji = "✈️",
                    bgColors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF075985)),
                    borderColor = Color(0xFFBAE6FD),
                    timeRemaining = "2d 15h",
                    progressCurrent = 14,
                    progressMax = 15,
                    prizeDesc = "+250 💎 + Magic Boosters",
                    isClaimable = false
                ),
                ContestTabItem(
                    title = "Champions Clash",
                    rankOrTier = "Quarter Final",
                    stickerEmoji = "🏆",
                    bgColors = listOf(Color(0xFFEC4899), Color(0xFFBE185D), Color(0xFF831843)),
                    borderColor = Color(0xFFFBCFE8),
                    timeRemaining = "Finished",
                    progressCurrent = 10,
                    progressMax = 10,
                    prizeDesc = "+1,000 💎 Grand Trophy",
                    isClaimable = true,
                    isFinished = true
                ),
                ContestTabItem(
                    title = "Team Tournament",
                    rankOrTier = "Star Trek #15",
                    stickerEmoji = "🛡️",
                    bgColors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFF4C1D95)),
                    borderColor = Color(0xFFDDD6FE),
                    timeRemaining = "00:29:15",
                    progressCurrent = 260,
                    progressMax = 270,
                    prizeDesc = "Chest Tier 4 Unlock",
                    isClaimable = false
                ),
                ContestTabItem(
                    title = "Lightning Rush",
                    rankOrTier = "30 Min Race",
                    stickerEmoji = "⚡",
                    bgColors = listOf(Color(0xFFF97316), Color(0xFFC2410C), Color(0xFF7C2D12)),
                    borderColor = Color(0xFFFED7AA),
                    timeRemaining = "27:56",
                    progressCurrent = 5,
                    progressMax = 5,
                    prizeDesc = "+150 💎 Instant Spark",
                    isClaimable = true
                ),
                ContestTabItem(
                    title = "Ancient Adventure",
                    rankOrTier = "Stage 1 (0/5)",
                    stickerEmoji = "🗿",
                    bgColors = listOf(Color(0xFF10B981), Color(0xFF047857), Color(0xFF064E3B)),
                    borderColor = Color(0xFFA7F3D0),
                    timeRemaining = "15:29:10",
                    progressCurrent = 2,
                    progressMax = 5,
                    prizeDesc = "Golden Relic Artifact",
                    isClaimable = false
                )
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

                    // Top Navigation Tabs (Weekly, Friends, Players, Teams)
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

                    // Top 3 Podium Highlights Banner (Inspired by video Contest screen)
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

                            // Podium 3-player layout (Rank 2, Rank 1, Rank 3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // #2 Player
                                PodiumPlayerColumn(rank = 2, name = "dhong", score = 111, avatar = "🥷", color = Color(0xFF38BDF8))
                                // #1 Player (Center, elevated)
                                PodiumPlayerColumn(rank = 1, name = "dane PRO", score = 166, avatar = "👑", color = Color(0xFFFBBF24), isLeader = true)
                                // #3 Player
                                PodiumPlayerColumn(rank = 3, name = "Sven", score = 69, avatar = "🦊", color = Color(0xFFF97316))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Event Cards List with Vibrant Badges
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeContests, key = { it.title }) { contest ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(2.dp, contest.borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(18.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.horizontalGradient(contest.bgColors))
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
                                                    Text(text = contest.stickerEmoji, fontSize = 26.sp)
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = contest.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = contest.rankOrTier,
                                                    color = Color(0xFFFEF08A),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
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
                                                    text = contest.timeRemaining,
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
                                            val frac = (contest.progressCurrent.toFloat() / contest.progressMax.toFloat()).coerceIn(0f, 1f)
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
                                            text = "${contest.progressCurrent}/${contest.progressMax}",
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
                                        Text(
                                            text = "Reward: ${contest.prizeDesc}",
                                            color = Color(0xFFFEF08A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )

                                        if (contest.isClaimable) {
                                            Button(
                                                onClick = {
                                                    activeContests = activeContests.map {
                                                        if (it.title == contest.title) it.copy(isClaimable = false) else it
                                                    }
                                                    viewModel.claimContest(contest.title)
                                                    viewModel.soundManager.playVictoryMusic()
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
                                                    text = if (contest.isFinished) "FINISHED" else "PLAYING",
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
