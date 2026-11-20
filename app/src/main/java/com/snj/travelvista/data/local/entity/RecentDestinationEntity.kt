package com.snj.travelvista.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_destinations")
data class RecentDestinationEntity(
    @PrimaryKey
    val destinationId: Long,
    val title: String,
    val location: String,
    val imageUrl: String,
    val category: String,
    val viewedAtTimestamp: Long = System.currentTimeMillis()
)
