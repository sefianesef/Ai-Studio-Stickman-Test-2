package com.mygames.stickmanrush

import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.mygames.stickmanrush.ui.AuthDialog
import com.mygames.stickmanrush.ui.MainMenuDialog
import com.mygames.stickmanrush.ui.GameViewModel

@Composable
fun StickmanApp(viewModel: GameViewModel) {
    var showAuthDialog by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser == null)
    }

    // Original UI
    MainMenuDialog(viewModel = viewModel)

    // AUTH POPUP OVERLAY
    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { },
            onLoginSuccess = {
                showAuthDialog = false
            },
            onGuestPlay = {
                FirebaseAuth.getInstance()
                    .signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showAuthDialog = false
                        }
                    }
            }
        )
    }
}
