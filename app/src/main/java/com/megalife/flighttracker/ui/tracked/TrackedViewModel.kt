package com.megalife.flighttracker.ui.tracked

import androidx.lifecycle.*
import com.megalife.flighttracker.data.repository.FlightRepository
import kotlinx.coroutines.launch

class TrackedViewModel(private val repository: FlightRepository) : ViewModel() {

    val trackedFlights = repository.getTrackedFlights()

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    fun untrackFlight(flightId: String, fcmToken: String?) {
        viewModelScope.launch {
            repository.untrackFlight(flightId, fcmToken)
        }
    }

    fun forceRefresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val tracked = repository.getAllTrackedFlightsList()
                for (flight in tracked) {
                    try {
                        val result = repository.getFlightDetail(flight.flightId)
                        result.getOrNull()?.let { data ->
                            val updated = flight.copy(
                                lastStatus = com.megalife.flighttracker.util.FlightUtils.getFlightStatus(data),
                                departureTerminal = data.terminalOrigin ?: flight.departureTerminal,
                                departureGate = data.gateOrigin ?: flight.departureGate,
                                arrivalTerminal = data.terminalDestination ?: flight.arrivalTerminal,
                                arrivalGate = data.gateDestination ?: flight.arrivalGate,
                                baggageCarousel = data.baggageClaim ?: flight.baggageCarousel,
                                progressPercent = data.progressPercent ?: flight.progressPercent,
                                lastUpdated = System.currentTimeMillis()
                            )
                            repository.updateTrackedFlight(updated)
                        }
                    } catch (_: Exception) { }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    class Factory(private val repository: FlightRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrackedViewModel(repository) as T
        }
    }
}
