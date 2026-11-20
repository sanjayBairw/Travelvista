package com.snj.travelvista.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itinerary_items",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class ItineraryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tripId: Long,
    val dayNumber: Int = 1,
    val timeSlot: String = "",
    val title: String,
    val itemType: String = "SIGHTSEEING", // SIGHTSEEING, HOTEL, FLIGHT, DINING, ACTIVITY
    val notes: String = "",
    val providerUrl: String = ""
)
