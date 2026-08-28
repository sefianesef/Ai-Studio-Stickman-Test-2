package com.mygames.stickmanrush

import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.mygames.stickmanrush.ui.AuthDialog
import com.mygames.stickmanrush.ui.MainMenuDialog
import com.mygames.stickmanrush.ui.GameViewModel

@Composable
fun StickmanApp(viewModel: GameViewModel) {
    // Yeh check karega ki pehle se login hai ya nahi
    var showAuthDialog by remember { 
        mutableStateOf(FirebaseAuth.getInstance().currentUser == null) 
    }

    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false }
        )
    } else {
        // Jab popup band hoga, tab Main Menu khulega
        MainMenuDialog(viewModel = viewModel)
    }
}
