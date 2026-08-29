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
import com.mygames.stickmanrush.ui.MainMenuDialog
import com.mygames.stickmanrush.ui.StickmanGameScreen

@Composable
fun StickmanApp(viewModel: GameViewModel) {
    val navController = rememberNavController()

    // 1. POPUP STATE: Check karo ki user logged in hai ya nahi. Guest ko bhi har baar poochega.
    val currentUser = FirebaseAuth.getInstance().currentUser
    var showAuthDialog by remember { 
        mutableStateOf(currentUser == null || currentUser.isAnonymous) 
    }

    // 2. ORIGINAL NAVIGATION & SCAFFOLD
    Scaffold(
        bottomBar = {
            val navBackStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = navBackStackEntry?.destination?.route

            if (currentRoute != "game") {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                MainMenuDialog(
                    viewModel = viewModel,
                    onPlayClick = { navController.navigate("game") }
                )
            }
            composable("game") {
                StickmanGameScreen(viewModel = viewModel)
            }
            // Add other destinations (events, rank, team, shop) here
        }
    }

    // 3. OVERLAY: Pop-up hamesha screen ke upar aayega
    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { /* Optionally handle back press */ },
            onLoginSuccess = { showAuthDialog = false },
            onGuestPlay = {
                FirebaseAuth.getInstance().signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showAuthDialog = false 
                        } else {
                            android.util.Log.e("Auth", "Guest login failed")
                        }
                    }
            }
        )
    }
}
