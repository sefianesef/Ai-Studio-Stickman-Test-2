package com.mygames.stickmanrush

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mygames.stickmanrush.ui.BottomNavigationBar
import com.mygames.stickmanrush.ui.GameViewModel
import com.mygames.stickmanrush.ui.StickmanGameScreen

@Composable
fun StickmanApp(viewModel: GameViewModel) {
    val navController = rememberNavController()

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
}
