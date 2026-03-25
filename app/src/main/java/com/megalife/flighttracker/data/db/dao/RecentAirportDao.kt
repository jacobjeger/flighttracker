package com.megalife.flighttracker.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.megalife.flighttracker.data.db.entity.RecentAirport

@Dao
interface RecentAirportDao {
    @Query("SELECT * FROM recent_airports ORDER BY viewedAt DESC LIMIT 10")
    fun getRecentAirports(): LiveData<List<RecentAirport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(airport: RecentAirport)

    @Query("DELETE FROM recent_airports WHERE airportCode = :code")
    suspend fun deleteByCode(code: String)

    @Query("DELETE FROM recent_airports")
    suspend fun clearAll()
}
