package com.megalife.flighttracker.worker

import android.content.Context
import androidx.work.*
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.data.db.entity.TrackedFlight
import com.megalife.flighttracker.util.FlightUtils
import java.util.concurrent.TimeUnit

class FlightRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as FlightTrackerApp
        val repository = app.flightRepository

        try {
            // Remove stale landed flights (24h+)
            repository.removeStaleLandedFlights()

            // Refresh all tracked flights
            val trackedFlights = repository.getAllTrackedFlightsList()
            for (tracked in trackedFlights) {
                try {
                    val result = repository.getFlightDetail(tracked.flightId)
                    result.getOrNull()?.let { flight ->
                        val updated = TrackedFlight(
                            flightId = tracked.flightId,
                            flightNumber = FlightUtils.getFlightDisplayNumber(flight),
                            airline = FlightUtils.getAirlineName(flight),
                            origin = flight.origin?.codeIata ?: flight.origin?.code ?: tracked.origin,
                            destination = flight.destination?.codeIata ?: flight.destination?.code ?: tracked.destination,
                            originCity = flight.origin?.city ?: tracked.originCity,
                            destinationCity = flight.destination?.city ?: tracked.destinationCity,
                            originTimezone = flight.origin?.timezone ?: tracked.originTimezone,
                            destinationTimezone = flight.destination?.timezone ?: tracked.destinationTimezone,
                            scheduledDeparture = FlightUtils.parseIsoToMillis(flight.scheduledOut ?: flight.scheduledOff),
                            scheduledArrival = FlightUtils.parseIsoToMillis(flight.scheduledIn ?: flight.scheduledOn),
                            lastStatus = FlightUtils.getFlightStatus(flight),
                            departureTerminal = flight.terminalOrigin ?: tracked.departureTerminal,
                            departureGate = flight.gateOrigin ?: tracked.departureGate,
                            arrivalTerminal = flight.terminalDestination ?: tracked.arrivalTerminal,
                            arrivalGate = flight.gateDestination ?: tracked.arrivalGate,
                            baggageCarousel = flight.baggageClaim ?: tracked.baggageCarousel,
                            aircraftType = flight.aircraftType ?: tracked.aircraftType,
                            progressPercent = flight.progressPercent ?: tracked.progressPercent,
                            lastUpdated = System.currentTimeMillis()
                        )
                        repository.updateTrackedFlight(updated)
                    }
                } catch (_: Exception) {
                    // Skip individual flight errors
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "flight_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FlightRefreshWorker>(
                2, TimeUnit.MINUTES,
                1, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
