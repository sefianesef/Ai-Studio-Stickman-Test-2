package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val totalGems: Int = 10,
    val highScore: Int = 0,
    val totalBridgesBuilt: Int = 0,
    val totalPerfectHits: Int = 0,
    val currentStreak: Int = 1,
    val lastClaimEpochDay: Long = 0L,
    val selectedHatId: String = "hat_none",
    val selectedScarfId: String = "scarf_gold",
    val selectedStickId: String = "stick_wood",
    val selectedSkinId: String = "skin_white",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
