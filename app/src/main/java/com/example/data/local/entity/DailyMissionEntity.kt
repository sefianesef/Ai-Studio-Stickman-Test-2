package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_missions")
data class DailyMissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val missionType: String, // BUILD_BRIDGES, PERFECT_HITS, COLLECT_GEMS, REACH_SCORE, FLIP_WALK
    val targetCount: Int,
    val currentProgress: Int = 0,
    val rewardGems: Int = 5,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val assignedEpochDay: Long = 0L
)
