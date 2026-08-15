package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchased_items")
data class PurchasedItemEntity(
    @PrimaryKey val itemId: String,
    val itemType: String,
    val costPaid: Int = 0,
    val unlockedAt: Long = System.currentTimeMillis()
)
