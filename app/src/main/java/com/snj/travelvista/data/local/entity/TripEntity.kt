package com.snj.travelvista.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val destinationTitle: String,
    val destinationId: Long = -1L,
    val startDate: String,
    val endDate: String,
    val travelersCount: Int = 2,
    val estimatedBudgetUsd: Int = 0,
    val notes: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
