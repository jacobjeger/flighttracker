package com.megalife.flighttracker.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.megalife.flighttracker.data.db.entity.RecentFlight

@Dao
interface RecentFlightDao {
    @Query("SELECT * FROM recent_flights ORDER BY viewedAt DESC LIMIT 20")
    fun getRecentFlights(): LiveData<List<RecentFlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flight: RecentFlight)

    @Query("DELETE FROM recent_flights WHERE flightId = :flightId")
    suspend fun deleteById(flightId: String)

    @Query("DELETE FROM recent_flights")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recent_flights")
    suspend fun getCount(): Int

    @Query("DELETE FROM recent_flights WHERE viewedAt = (SELECT MIN(viewedAt) FROM recent_flights)")
    suspend fun deleteOldest()
}
