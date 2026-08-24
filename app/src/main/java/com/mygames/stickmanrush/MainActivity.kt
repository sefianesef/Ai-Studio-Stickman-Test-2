package com.mygames.stickmanrush

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mygames.stickmanrush.ui.StickmanGameScreen
import com.mygames.stickmanrush.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      enableEdgeToEdge()
    } catch (t: Throwable) {
      Log.w("MainActivity", "Failed to enable edge-to-edge", t)
    }

    try {
      setContent {
        MyApplicationTheme {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF06140E)
          ) {
            StickmanGameScreen()
          }
        }
      }
    } catch (t: Throwable) {
      Log.e("MainActivity", "Error setting content", t)
    }
  }
}

