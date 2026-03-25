package com.megalife.flighttracker.data.repository

import com.megalife.flighttracker.BuildConfig
import com.megalife.flighttracker.data.api.RetrofitClient
import com.megalife.flighttracker.data.db.dao.RecentFlightDao
import com.megalife.flighttracker.data.db.dao.TrackedFlightDao
import com.megalife.flighttracker.data.db.entity.RecentFlight
import com.megalife.flighttracker.data.db.entity.TrackedFlight
import com.megalife.flighttracker.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlightRepository(
    private val trackedFlightDao: TrackedFlightDao,
    private val recentFlightDao: RecentFlightDao
) {
    private val api = RetrofitClient.flightAwareApi
    private val railwayApi = RetrofitClient.railwayApi
    private val apiKey = BuildConfig.FLIGHTAWARE_API_KEY

    suspend fun searchFlights(query: String): Result<List<FlightData>> = withContext(Dispatchers.IO) {
        try {
            // Try as flight ident first (e.g. LY008, AA100, EL AL 8 -> ELAL8 won't match but LY008 will)
            val cleanQuery = query.trim().uppercase().replace("\\s+".toRegex(), "")
            if (cleanQuery.matches(Regex("^[A-Z]{2,3}\\d{1,4}$")) || cleanQuery.matches(Regex("^[A-Z0-9]{2}\\d{1,4}$"))) {
                try {
                    val response = api.getFlightByIdent(cleanQuery, apiKey)
                    val flights = response.flights
                    if (!flights.isNullOrEmpty()) {
                        return@withContext Result.success(flights)
                    }
                } catch (_: Exception) {
                    // Fall through to advanced search
                }
            }

            // Also try with spaces removed and common airline name patterns
            val trimmed = query.trim()
            // Try "EL AL 8" -> "LY8", handle airline name + number
            val withoutSpaceIdent = trimmed.replace("\\s+".toRegex(), "").uppercase()
            if (withoutSpaceIdent != cleanQuery && withoutSpaceIdent.matches(Regex("^[A-Z]+\\d{1,4}$"))) {
                try {
                    val response = api.getFlightByIdent(withoutSpaceIdent, apiKey)
                    val flights = response.flights
                    if (!flights.isNullOrEmpty()) {
                        return@withContext Result.success(flights)
                    }
                } catch (_: Exception) { }
            }

            // Fall back to advanced search
            val searchQuery = buildSearchQuery(query)
            try {
                val response = api.searchFlights(searchQuery, apiKey)
                Result.success(response.flights ?: emptyList())
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 400) {
                    // Advanced search failed, try ident as last resort
                    try {
                        val response = api.getFlightByIdent(cleanQuery, apiKey)
                        Result.success(response.flights ?: emptyList())
                    } catch (_: Exception) {
                        Result.success(emptyList())
                    }
                } else {
                    throw e
                }
            }
        } catch (e: retrofit2.HttpException) {
            Result.failure(mapHttpError(e))
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFlightDetail(flightId: String): Result<FlightData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFlight(flightId, apiKey)
            val flight = response.flights?.firstOrNull()
            if (flight != null) {
                Result.success(flight)
            } else {
                Result.failure(Exception("Flight not found"))
            }
        } catch (e: retrofit2.HttpException) {
            Result.failure(mapHttpError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFlightByIdent(ident: String): Result<List<FlightData>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFlightByIdent(ident, apiKey)
            Result.success(response.flights ?: emptyList())
        } catch (e: retrofit2.HttpException) {
            Result.failure(mapHttpError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSearchQuery(query: String): String {
        val parts = query.trim().split("\\s+".toRegex())
        // If it looks like origin-dest pair (two 3-letter codes)
        if (parts.size == 2 && parts.all { it.length == 3 && it.all { c -> c.isLetter() } }) {
            return "{origin ${parts[0].uppercase()}} {dest ${parts[1].uppercase()}}"
        }
        // If single airport code
        if (parts.size == 1 && parts[0].length == 3 && parts[0].all { it.isLetter() }) {
            return "{origin ${parts[0].uppercase()}}"
        }
        return query
    }

    private fun mapHttpError(e: retrofit2.HttpException): Exception {
        return when (e.code()) {
            401 -> Exception("Invalid API key — check settings")
            404 -> Exception("Flight not found")
            429 -> Exception("Too many requests — please wait a moment")
            500 -> Exception("FlightAware service unavailable — try again")
            else -> Exception("Network error: ${e.code()}")
        }
    }

    // Tracked flights
    fun getTrackedFlights() = trackedFlightDao.getAllTrackedFlights()

    suspend fun isFlightTracked(flightId: String) = trackedFlightDao.isTracked(flightId)

    fun isFlightTrackedLive(flightId: String) = trackedFlightDao.isTrackedLive(flightId)

    suspend fun trackFlight(flight: TrackedFlight, fcmToken: String?) {
        trackedFlightDao.insert(flight)
        fcmToken?.let {
            try {
                railwayApi.addTrackedFlight(AddFlightRequest(it, flight.flightId))
            } catch (_: Exception) { }
        }
    }

    suspend fun untrackFlight(flightId: String, fcmToken: String?) {
        trackedFlightDao.deleteById(flightId)
        fcmToken?.let {
            try {
                railwayApi.removeTrackedFlight(RemoveFlightRequest(it, flightId))
            } catch (_: Exception) { }
        }
    }

    suspend fun updateTrackedFlight(flight: TrackedFlight) {
        trackedFlightDao.update(flight)
    }

    suspend fun getAllTrackedFlightsList() = trackedFlightDao.getAllTrackedFlightsList()

    // Recent flights
    fun getRecentFlights() = recentFlightDao.getRecentFlights()

    suspend fun addRecentFlight(flight: RecentFlight) {
        if (recentFlightDao.getCount() >= 20) {
            recentFlightDao.deleteOldest()
        }
        recentFlightDao.insert(flight)
    }

    suspend fun removeRecentFlight(flightId: String) {
        recentFlightDao.deleteById(flightId)
    }

    suspend fun clearRecentFlights() {
        recentFlightDao.clearAll()
    }

    // Cleanup
    suspend fun removeStaleLandedFlights() {
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        trackedFlightDao.removeLandedBefore(cutoff)
    }

    // Backend registration
    suspend fun registerDevice(fcmToken: String, trackedFlightIds: List<String>) {
        try {
            railwayApi.registerDevice(DeviceRegistration(fcmToken, trackedFlightIds))
        } catch (_: Exception) { }
    }
}
