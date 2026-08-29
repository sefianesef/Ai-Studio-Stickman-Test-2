package com.mygames.stickmanrush.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { onNavigate("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF38BDF8),
                selectedTextColor = Color(0xFF38BDF8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0xFF1E293B)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Events") },
            label = { Text("Events") },
            selected = currentRoute == "events",
            onClick = { onNavigate("events") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF38BDF8),
                selectedTextColor = Color(0xFF38BDF8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0xFF1E293B)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Star, contentDescription = "Rank") },
            label = { Text("Rank") },
            selected = currentRoute == "rank",
            onClick = { onNavigate("rank") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF38BDF8),
                selectedTextColor = Color(0xFF38BDF8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0xFF1E293B)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Group, contentDescription = "Team") },
            label = { Text("Team") },
            selected = currentRoute == "team",
            onClick = { onNavigate("team") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF38BDF8),
                selectedTextColor = Color(0xFF38BDF8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0xFF1E293B)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Shop") },
            label = { Text("Shop") },
            selected = currentRoute == "shop",
            onClick = { onNavigate("shop") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF38BDF8),
                selectedTextColor = Color(0xFF38BDF8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B),
                indicatorColor = Color(0xFF1E293B)
            )
        )
    }
}
