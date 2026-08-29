package com.mygames.stickmanrush.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicknameDialog(
    currentName: String,
    onConfirm: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = { /* Forced */ },
        title = { Text("Choose Your Nickname") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter a unique player name for leaderboard and profile:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    label = { Text("Nickname") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onConfirm(nameInput)
                    }
                },
                enabled = nameInput.isNotBlank()
            ) {
                Text("Confirm")
            }
        }
    )
}
