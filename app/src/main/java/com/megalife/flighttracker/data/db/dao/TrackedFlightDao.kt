package com.megalife.flighttracker.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.megalife.flighttracker.data.db.entity.TrackedFlight

@Dao
interface TrackedFlightDao {
    @Query("SELECT * FROM tracked_flights ORDER BY CASE WHEN lastStatus IN ('En Route', 'Scheduled', 'On Time', 'Delayed') THEN 0 WHEN lastStatus = 'Landed' THEN 1 ELSE 2 END, scheduledDeparture ASC")
    fun getAllTrackedFlights(): LiveData<List<TrackedFlight>>

    @Query("SELECT * FROM tracked_flights")
    suspend fun getAllTrackedFlightsList(): List<TrackedFlight>

    @Query("SELECT * FROM tracked_flights WHERE flightId = :flightId")
    suspend fun getTrackedFlight(flightId: String): TrackedFlight?

    @Query("SELECT * FROM tracked_flights WHERE flightId = :flightId")
    fun getTrackedFlightLive(flightId: String): LiveData<TrackedFlight?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flight: TrackedFlight)

    @Update
    suspend fun update(flight: TrackedFlight)

    @Delete
    suspend fun delete(flight: TrackedFlight)

    @Query("DELETE FROM tracked_flights WHERE flightId = :flightId")
    suspend fun deleteById(flightId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM tracked_flights WHERE flightId = :flightId)")
    suspend fun isTracked(flightId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM tracked_flights WHERE flightId = :flightId)")
    fun isTrackedLive(flightId: String): LiveData<Boolean>

    @Query("DELETE FROM tracked_flights WHERE lastStatus = 'Landed' AND lastUpdated < :cutoff")
    suspend fun removeLandedBefore(cutoff: Long)

    @Query("SELECT COUNT(*) FROM tracked_flights")
    fun getCount(): LiveData<Int>
}
