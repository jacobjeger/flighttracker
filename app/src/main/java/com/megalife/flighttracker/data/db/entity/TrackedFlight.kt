package com.megalife.flighttracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_flights")
data class TrackedFlight(
    @PrimaryKey val flightId: String,
    val flightNumber: String,
    val airline: String = "",
    val origin: String,
    val destination: String,
    val originCity: String = "",
    val destinationCity: String = "",
    val originTimezone: String = "",
    val destinationTimezone: String = "",
    val scheduledDeparture: Long,
    val scheduledArrival: Long,
    val lastStatus: String,
    val departureTerminal: String? = null,
    val departureGate: String? = null,
    val arrivalTerminal: String? = null,
    val arrivalGate: String? = null,
    val baggageCarousel: String? = null,
    val aircraftType: String? = null,
    val progressPercent: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
