package com.megalife.flighttracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.megalife.flighttracker.data.db.FlightDatabase
import com.megalife.flighttracker.data.repository.AirportRepository
import com.megalife.flighttracker.data.repository.FlightRepository

class FlightTrackerApp : Application() {

    val database by lazy { FlightDatabase.getDatabase(this) }
    val flightRepository by lazy {
        FlightRepository(database.trackedFlightDao(), database.recentFlightDao())
    }
    val airportRepository by lazy {
        AirportRepository(database.recentAirportDao())
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flight Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for flight status changes, gate changes, and delays"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "flight_updates"
    }
}
