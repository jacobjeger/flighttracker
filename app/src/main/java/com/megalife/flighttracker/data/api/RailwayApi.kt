package com.megalife.flighttracker.data.api

import com.megalife.flighttracker.data.model.*
import retrofit2.http.Body
import retrofit2.http.POST

interface RailwayApi {

    @POST("api/register")
    suspend fun registerDevice(@Body registration: DeviceRegistration): BackendResponse

    @POST("api/track")
    suspend fun addTrackedFlight(@Body request: AddFlightRequest): BackendResponse

    @POST("api/untrack")
    suspend fun removeTrackedFlight(@Body request: RemoveFlightRequest): BackendResponse
}
