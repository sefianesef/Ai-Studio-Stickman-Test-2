package com.mygames.stickmanrush

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mygames.stickmanrush.ui.AuthDialog
import com.mygames.stickmanrush.ui.BottomNavigationBar
import com.mygames.stickmanrush.ui.GameViewModel
import com.mygames.stickmanrush.ui.NicknameDialog
import com.mygames.stickmanrush.ui.StickmanGameScreen

@Composable
fun StickmanApp(viewModel: GameViewModel) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val currentFirebaseUser = auth.currentUser

    // 1. Nickname from repository/firestore
    val currentNickname by viewModel.firestoreNickname.collectAsState()

    // 2. Auth Dialog State
    var showAuthDialog by remember {
        mutableStateOf(currentFirebaseUser == null)
    }

    // 3. Nickname Dialog State (Triggered only after Auth is done and nickname is missing)
    var showNicknameDialog by remember {
        mutableStateOf(false)
    }

    // Auth aur Nickname state check
    LaunchedEffect(currentFirebaseUser, currentNickname, showAuthDialog) {
        if (currentFirebaseUser != null && !showAuthDialog) {
            if (currentNickname.isBlank()) {
                showNicknameDialog = true
            } else {
                showNicknameDialog = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = navBackStackEntry?.destination?.route
            BottomNavigationBar(
                currentRoute = currentRoute ?: "home",
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                StickmanGameScreen(viewModel = viewModel)
            }
        }
    }

    // --- POP-UP FLOW SEQUENCE ---

    // 1. PEHLE: Authentication Dialog (Google / Email / Guest)
    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { /* Forced sign-in */ },
            onLoginSuccess = {
                showAuthDialog = false
            },
            onGuestPlay = {
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showAuthDialog = false
                        }
                    }
            }
        )
    }

    // 2. USKE BAAD: Nickname Selection Dialog (agar player ka name empty hai)
    if (!showAuthDialog && showNicknameDialog) {
        NicknameDialog(
            currentName = currentNickname,
            onConfirm = { chosenName ->
                if (chosenName.isNotBlank()) {
                    viewModel.updateNickname(chosenName.trim())
                    showNicknameDialog = false
                }
            }
        )
    }
}
