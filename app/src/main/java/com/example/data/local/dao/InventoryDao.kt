package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PurchasedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM purchased_items ORDER BY unlockedAt ASC")
    fun getAllPurchasedItemsFlow(): Flow<List<PurchasedItemEntity>>

    @Query("SELECT itemId FROM purchased_items")
    fun getPurchasedItemIdsFlow(): Flow<List<String>>

    @Query("SELECT itemId FROM purchased_items")
    suspend fun getPurchasedItemIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM purchased_items WHERE itemId = :itemId LIMIT 1)")
    suspend fun isItemUnlocked(itemId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PurchasedItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<PurchasedItemEntity>)

    @Query("DELETE FROM purchased_items WHERE itemId = :itemId")
    suspend fun deleteItem(itemId: String)
}
