package com.megalife.flighttracker.data.api

import com.megalife.flighttracker.data.model.*
import retrofit2.http.*

interface FlightAwareApi {

    @GET("flights/{ident}")
    suspend fun getFlightByIdent(
        @Path("ident") ident: String,
        @Header("x-apikey") apiKey: String
    ): FlightIdentResponse

    @GET("flights/{flight_id}")
    suspend fun getFlight(
        @Path("flight_id") flightId: String,
        @Header("x-apikey") apiKey: String
    ): FlightResponse

    @GET("flights/search")
    suspend fun searchFlights(
        @Query("query") query: String,
        @Header("x-apikey") apiKey: String
    ): FlightSearchResponse

    @GET("airports/{id}/flights/departures")
    suspend fun getAirportDepartures(
        @Path("id") airportId: String,
        @Header("x-apikey") apiKey: String,
        @Query("type") type: String = "Airline",
        @Query("max_pages") maxPages: Int = 2
    ): AirportFlightsResponse

    @GET("airports/{id}/flights/arrivals")
    suspend fun getAirportArrivals(
        @Path("id") airportId: String,
        @Header("x-apikey") apiKey: String,
        @Query("type") type: String = "Airline",
        @Query("max_pages") maxPages: Int = 2
    ): AirportFlightsResponse

    @GET("airports/{id}")
    suspend fun getAirport(
        @Path("id") airportId: String,
        @Header("x-apikey") apiKey: String
    ): AirportResponse
}
