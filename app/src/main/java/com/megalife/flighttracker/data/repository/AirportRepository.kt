package com.megalife.flighttracker.data.repository

import com.megalife.flighttracker.BuildConfig
import com.megalife.flighttracker.data.api.RetrofitClient
import com.megalife.flighttracker.data.db.dao.RecentAirportDao
import com.megalife.flighttracker.data.db.entity.RecentAirport
import com.megalife.flighttracker.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AirportRepository(
    private val recentAirportDao: RecentAirportDao
) {
    private val api = RetrofitClient.flightAwareApi
    private val weatherApi = RetrofitClient.weatherApi
    private val apiKey = BuildConfig.FLIGHTAWARE_API_KEY

    suspend fun getAirportInfo(airportCode: String): Result<AirportResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAirport(airportCode.uppercase(), apiKey)
            Result.success(response)
        } catch (e: retrofit2.HttpException) {
            Result.failure(mapHttpError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAirportDepartures(airportCode: String): Result<List<FlightData>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAirportDepartures(airportCode.uppercase(), apiKey)
            Result.success(response.departures ?: emptyList())
        } catch (e: retrofit2.HttpException) {
            Result.failure(mapHttpError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAirportArrivals(airportCode: String): Result<List<FlightData>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAirportArrivals(airportCode.uppercase(), apiKey)
            Result.success(response.arrivals ?: emptyList())
        } catch (e: retrofit2.HttpException) {
            Result.failure(mapHttpError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeather(city: String): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val response = weatherApi.getWeather(city, BuildConfig.WEATHER_API_KEY)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recent airports
    fun getRecentAirports() = recentAirportDao.getRecentAirports()

    suspend fun addRecentAirport(airport: RecentAirport) {
        recentAirportDao.insert(airport)
    }

    suspend fun clearRecentAirports() {
        recentAirportDao.clearAll()
    }

    private fun mapHttpError(e: retrofit2.HttpException): Exception {
        return when (e.code()) {
            401 -> Exception("Invalid API key — check settings")
            404 -> Exception("Airport not found")
            429 -> Exception("Too many requests — please wait a moment")
            500 -> Exception("FlightAware service unavailable — try again")
            else -> Exception("Network error: ${e.code()}")
        }
    }
}
