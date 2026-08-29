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
import com.mygames.stickmanrush.ui.StickmanGameScreen

@Composable
fun StickmanApp(viewModel: GameViewModel) {
    val navController = rememberNavController()

    // 1. POPUP STATE: Check karega ki user logged in hai ya nahi
    val currentUser = FirebaseAuth.getInstance().currentUser
    var showAuthDialog by remember { 
        mutableStateOf(currentUser == null || currentUser.isAnonymous) 
    }

    // 2. MAIN LAYOUT (Bottom Bar ke sath)
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
            startDestination = "home", // App 'home' se start hoga
            modifier = Modifier.padding(innerPadding)
        ) {
            // YAHAN FIX KIYA HAI: "home" par ab woh menu nahi, direct Game open hoga!
            composable("home") {
                StickmanGameScreen(viewModel = viewModel)
            }
            
            // Agar tere paas baaki tabs (events, rank, team, shop) ki screens hain, 
            // toh unko yahan add kar sakta hai.
        }
    }

    // 3. OVERLAY: Pop-up jo game ke upar aayega
    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { /* Do nothing to force login/guest */ },
            onLoginSuccess = { showAuthDialog = false },
            onGuestPlay = {
                FirebaseAuth.getInstance().signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showAuthDialog = false 
                        }
                    }
            }
        )
    }
}
