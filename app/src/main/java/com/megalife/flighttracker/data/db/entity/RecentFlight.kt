package com.megalife.flighttracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_flights")
data class RecentFlight(
    @PrimaryKey val flightId: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val viewedAt: Long,
    val lastStatus: String
)
