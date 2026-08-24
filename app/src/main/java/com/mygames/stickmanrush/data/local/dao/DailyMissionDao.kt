package com.mygames.stickmanrush.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mygames.stickmanrush.data.local.entity.DailyMissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyMissionDao {

    @Query("SELECT * FROM daily_missions WHERE assignedEpochDay = :epochDay ORDER BY isClaimed ASC, isCompleted DESC")
    fun getMissionsForDayFlow(epochDay: Long): Flow<List<DailyMissionEntity>>

    @Query("SELECT * FROM daily_missions WHERE assignedEpochDay = :epochDay")
    suspend fun getMissionsForDay(epochDay: Long): List<DailyMissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<DailyMissionEntity>)

    @Update
    suspend fun updateMission(mission: DailyMissionEntity)

    @Query("UPDATE daily_missions SET currentProgress = :progress, isCompleted = :completed WHERE id = :missionId")
    suspend fun updateProgress(missionId: String, progress: Int, completed: Boolean)

    @Query("UPDATE daily_missions SET isClaimed = 1 WHERE id = :missionId")
    suspend fun markClaimed(missionId: String)

    @Query("DELETE FROM daily_missions WHERE assignedEpochDay < :olderThanEpochDay")
    suspend fun cleanOldMissions(olderThanEpochDay: Long)
}
