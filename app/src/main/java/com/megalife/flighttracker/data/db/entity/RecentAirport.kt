package com.megalife.flighttracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_airports")
data class RecentAirport(
    @PrimaryKey val airportCode: String,
    val airportName: String,
    val city: String,
    val viewedAt: Long
)
