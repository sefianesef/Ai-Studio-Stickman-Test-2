package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DailyMissionDao
import com.example.data.local.dao.InventoryDao
import com.example.data.local.dao.PlayerProfileDao
import com.example.data.local.entity.DailyMissionEntity
import com.example.data.local.entity.PlayerProfileEntity
import com.example.data.local.entity.PurchasedItemEntity

@Database(
    entities = [
        PlayerProfileEntity::class,
        PurchasedItemEntity::class,
        DailyMissionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun dailyMissionDao(): DailyMissionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stickman_hero.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
