package com.mygames.stickmanrush.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mygames.stickmanrush.security.CloudSyncStatus

@Composable
fun GameSettingsDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val leftHanded by viewModel.leftHandedMode.collectAsState()
    val highFps by viewModel.highFrameRate.collectAsState()
    val ultraParticles by viewModel.particleQualityUltra.collectAsState()
    val screenShake by viewModel.screenShakeEnabled.collectAsState()

    val syncStatus by viewModel.cloudSyncStatus.collectAsState()
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val cloudGems by viewModel.cloudAuthoritativeGems.collectAsState()

    var showResetConfirmation by remember { mutableStateOf(false) }
    var cloudSignInStatusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF64748B)),
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .shadow(28.dp, RoundedCornerShape(24.dp))
                .testTag("game_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
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
                            color = Color(0xFF64748B).copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF94A3B8)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "⚙️", fontSize = 22.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "SETTINGS",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Preferences, Audio & Display Options",
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
                            .testTag("settings_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable settings list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile & Identity Section
                    SettingsSectionTitle(title = "PLAYER PROFILE & IDENTITY")
                    val nickname by viewModel.nickname.collectAsState()
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "🥷", fontSize = 28.sp)
                                Column {
                                    Text(
                                        text = if (nickname.isNotBlank()) nickname else "Choose Nickname",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Player ID Handle & Cloud Sync",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.openNicknameSetup(true) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.testTag("settings_edit_nickname_button")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Audio Section
                    SettingsSectionTitle(title = "AUDIO & HAPTICS")
                    SettingsToggleCard(
                        title = "Sound Effects & Fanfare",
                        subtitle = "Tactile audio feedback and celebration music",
                        icon = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        isChecked = soundEnabled,
                        onCheckedChange = { viewModel.toggleSound() }
                    )
                    SettingsToggleCard(
                        title = "Haptic Vibration",
                        subtitle = "Vibrations for bullseyes, landings, and flips",
                        icon = Icons.Default.Vibration,
                        isChecked = hapticsEnabled,
                        onCheckedChange = { viewModel.toggleHaptics() }
                    )

                    // Gameplay & Controls Section
                    SettingsSectionTitle(title = "GAMEPLAY & CONTROLS")
                    SettingsToggleCard(
                        title = "Left-Handed Mode",
                        subtitle = "Optimizes touch regions for left-handed play",
                        icon = Icons.Default.PanTool,
                        isChecked = leftHanded,
                        onCheckedChange = { viewModel.toggleLeftHanded() }
                    )
                    SettingsToggleCard(
                        title = "Screen Shake Impact FX",
                        subtitle = "Dynamic screen rumble on bridge drop & impacts",
                        icon = Icons.Default.ScreenRotation,
                        isChecked = screenShake,
                        onCheckedChange = { viewModel.toggleScreenShake() }
                    )

                    // Performance & Visuals Section
                    SettingsSectionTitle(title = "GRAPHICS & PERFORMANCE")
                    SettingsToggleCard(
                        title = "High Frame Rate (60 / 120 FPS)",
                        subtitle = "Ultra-smooth physics and particle animations",
                        icon = Icons.Default.Speed,
                        isChecked = highFps,
                        onCheckedChange = { viewModel.toggleHighFrameRate() }
                    )
                    SettingsToggleCard(
                        title = "Ultra Particle Density",
                        subtitle = "Rich confetti, cherry blossoms & dust bursts",
                        icon = Icons.Default.AutoAwesome,
                        isChecked = ultraParticles,
                        onCheckedChange = { viewModel.toggleParticleQuality() }
                    )

                    // Cloud Server & Firebase Wallet Section
                    SettingsSectionTitle(title = "FIREBASE CLOUD WALLET")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloud_wallet_settings_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Cloud Wallet",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Server Authoritative Wallet",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (firebaseUser != null) {
                                                "Logged in: ${firebaseUser?.email ?: firebaseUser?.displayName ?: "Verified Player"}"
                                            } else {
                                                "Local Vault Only (Sign in to sync online)"
                                            },
                                            color = if (firebaseUser != null) Color(0xFF34D399) else Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (syncStatus) {
                                        CloudSyncStatus.SYNCED -> Color(0xFF065F46)
                                        CloudSyncStatus.CONNECTING -> Color(0xFF1E3A8A)
                                        CloudSyncStatus.OFFLINE_SAVED -> Color(0xFF78350F)
                                        CloudSyncStatus.DISCONNECTED, CloudSyncStatus.ERROR -> Color(0xFF334155)
                                    }
                                ) {
                                    Text(
                                        text = when (syncStatus) {
                                            CloudSyncStatus.SYNCED -> "ONLINE"
                                            CloudSyncStatus.CONNECTING -> "SYNCING"
                                            CloudSyncStatus.OFFLINE_SAVED -> "OFFLINE"
                                            CloudSyncStatus.DISCONNECTED -> "LOCAL"
                                            CloudSyncStatus.ERROR -> "RETRY"
                                        },
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Connected Project: stickman-rush-88867 | Authoritative server balances prevent local phone hacks from altering your gems. Changes made directly to phone files do not affect your cloud account.",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )

                            if (cloudGems != null && firebaseUser != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "💎 Cloud Authoritative Gems:", color = Color(0xFF67E8F9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "$cloudGems", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            if (cloudSignInStatusMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = cloudSignInStatusMessage ?: "",
                                    color = Color(0xFFFDE047),
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (firebaseUser == null) {
                                    Button(
                                        onClick = {
                                            val activity = context as? Activity
                                            if (activity != null) {
                                                cloudSignInStatusMessage = "Connecting with Google Credential Manager..."
                                                viewModel.signInWithGoogle(activity) { success, msg ->
                                                    cloudSignInStatusMessage = if (success) "Connected as $msg" else "Sign-In Error: $msg"
                                                }
                                            } else {
                                                cloudSignInStatusMessage = "Activity context required for Google Sign-In"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("google_signin_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sign In with Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.syncLocalProgressToCloud()
                                            cloudSignInStatusMessage = "Cloud sync triggered at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("sync_cloud_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.signOutCloud()
                                            cloudSignInStatusMessage = "Signed out from Google Cloud"
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                                        modifier = Modifier.testTag("signout_cloud_button")
                                    ) {
                                        Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Data Management Section
                    SettingsSectionTitle(title = "DATA MANAGEMENT")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Career Progress",
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Resets stats and missions to fresh state",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = { showResetConfirmation = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = "RESET", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(text = "Reset Progress?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(text = "Are you sure you want to reset your local stats? Your unlocked cosmetics will remain.", color = Color(0xFFCBD5E1)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCareerProgress()
                        showResetConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Reset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF38BDF8),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF334155)
                )
            )
        }
    }
}
