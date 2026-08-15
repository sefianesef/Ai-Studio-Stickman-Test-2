package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PlayerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getPlayerProfileFlow(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    suspend fun getPlayerProfile(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: PlayerProfileEntity)

    @Query("UPDATE player_profile SET totalGems = :gems, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateGems(gems: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE player_profile SET highScore = :highScore, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateHighScore(highScore: Int, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE player_profile 
        SET totalBridgesBuilt = totalBridgesBuilt + 1, updatedAt = :timestamp 
        WHERE id = 1
    """)
    suspend fun incrementBridgesBuilt(timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE player_profile 
        SET totalPerfectHits = totalPerfectHits + 1, updatedAt = :timestamp 
        WHERE id = 1
    """)
    suspend fun incrementPerfectHits(timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE player_profile 
        SET selectedHatId = :hatId, 
            selectedScarfId = :scarfId, 
            selectedStickId = :stickId, 
            selectedSkinId = :skinId, 
            updatedAt = :timestamp 
        WHERE id = 1
    """)
    suspend fun updateEquippedCustomizations(
        hatId: String,
        scarfId: String,
        stickId: String,
        skinId: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE player_profile 
        SET currentStreak = :streak, 
            lastClaimEpochDay = :claimDay, 
            updatedAt = :timestamp 
        WHERE id = 1
    """)
    suspend fun updateDailyStreak(
        streak: Int,
        claimDay: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE player_profile SET soundEnabled = :enabled WHERE id = 1")
    suspend fun updateSoundEnabled(enabled: Boolean)

    @Query("UPDATE player_profile SET hapticsEnabled = :enabled WHERE id = 1")
    suspend fun updateHapticsEnabled(enabled: Boolean)
}
