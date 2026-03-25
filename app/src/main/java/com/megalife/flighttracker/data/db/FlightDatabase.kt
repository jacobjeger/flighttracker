package com.megalife.flighttracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.megalife.flighttracker.data.db.dao.RecentAirportDao
import com.megalife.flighttracker.data.db.dao.RecentFlightDao
import com.megalife.flighttracker.data.db.dao.TrackedFlightDao
import com.megalife.flighttracker.data.db.entity.RecentAirport
import com.megalife.flighttracker.data.db.entity.RecentFlight
import com.megalife.flighttracker.data.db.entity.TrackedFlight

@Database(
    entities = [TrackedFlight::class, RecentFlight::class, RecentAirport::class],
    version = 1,
    exportSchema = false
)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun trackedFlightDao(): TrackedFlightDao
    abstract fun recentFlightDao(): RecentFlightDao
    abstract fun recentAirportDao(): RecentAirportDao

    companion object {
        @Volatile
        private var INSTANCE: FlightDatabase? = null

        fun getDatabase(context: Context): FlightDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                    "flight_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
