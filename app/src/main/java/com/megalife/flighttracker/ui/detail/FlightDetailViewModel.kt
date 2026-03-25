package com.megalife.flighttracker.ui.detail

import androidx.lifecycle.*
import com.megalife.flighttracker.data.db.entity.RecentFlight
import com.megalife.flighttracker.data.db.entity.TrackedFlight
import com.megalife.flighttracker.data.model.FlightData
import com.megalife.flighttracker.data.model.WeatherResponse
import com.megalife.flighttracker.data.repository.AirportRepository
import com.megalife.flighttracker.data.repository.FlightRepository
import com.megalife.flighttracker.util.FlightUtils
import kotlinx.coroutines.launch

class FlightDetailViewModel(
    private val flightRepository: FlightRepository,
    private val airportRepository: AirportRepository
) : ViewModel() {

    private val _flight = MutableLiveData<FlightData>()
    val flight: LiveData<FlightData> = _flight

    private val _weather = MutableLiveData<WeatherResponse?>()
    val weather: LiveData<WeatherResponse?> = _weather

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isTracked = MutableLiveData<Boolean>()
    val isTracked: LiveData<Boolean> = _isTracked

    private var currentFlightId: String? = null

    fun loadFlight(flightId: String) {
        currentFlightId = flightId
        _isLoading.value = true

        viewModelScope.launch {
            _isTracked.value = flightRepository.isFlightTracked(flightId)

            val result = flightRepository.getFlightDetail(flightId)
            result.fold(
                onSuccess = { flight ->
                    _flight.value = flight
                    _isLoading.value = false
                    _error.value = null

                    // Save to recent
                    saveToRecent(flight)

                    // Load weather for destination
                    flight.destination?.city?.let { city ->
                        loadWeather(city)
                    }
                },
                onFailure = { e ->
                    _isLoading.value = false
                    _error.value = e.message
                }
            )
        }
    }

    fun loadFlightByIdent(ident: String) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = flightRepository.getFlightByIdent(ident)
            result.fold(
                onSuccess = { flights ->
                    val flight = flights.firstOrNull()
                    if (flight != null) {
                        currentFlightId = flight.faFlightId
                        _flight.value = flight
                        _isLoading.value = false
                        _isTracked.value = flight.faFlightId?.let {
                            flightRepository.isFlightTracked(it)
                        } ?: false

                        saveToRecent(flight)
                        flight.destination?.city?.let { loadWeather(it) }
                    } else {
                        _isLoading.value = false
                        _error.value = "Flight not found"
                    }
                },
                onFailure = { e ->
                    _isLoading.value = false
                    _error.value = e.message
                }
            )
        }
    }

    fun refreshFlight() {
        currentFlightId?.let { loadFlight(it) }
    }

    fun trackFlight(fcmToken: String?) {
        val flight = _flight.value ?: return
        val flightId = flight.faFlightId ?: return

        viewModelScope.launch {
            val tracked = TrackedFlight(
                flightId = flightId,
                flightNumber = FlightUtils.getFlightDisplayNumber(flight),
                airline = FlightUtils.getAirlineName(flight),
                origin = flight.origin?.codeIata ?: flight.origin?.code ?: "",
                destination = flight.destination?.codeIata ?: flight.destination?.code ?: "",
                originCity = flight.origin?.city ?: "",
                destinationCity = flight.destination?.city ?: "",
                originTimezone = flight.origin?.timezone ?: "",
                destinationTimezone = flight.destination?.timezone ?: "",
                scheduledDeparture = FlightUtils.parseIsoToMillis(flight.scheduledOut ?: flight.scheduledOff),
                scheduledArrival = FlightUtils.parseIsoToMillis(flight.scheduledIn ?: flight.scheduledOn),
                lastStatus = FlightUtils.getFlightStatus(flight),
                departureTerminal = flight.terminalOrigin,
                departureGate = flight.gateOrigin,
                arrivalTerminal = flight.terminalDestination,
                arrivalGate = flight.gateDestination,
                baggageCarousel = flight.baggageClaim,
                aircraftType = flight.aircraftType,
                progressPercent = flight.progressPercent ?: 0
            )
            flightRepository.trackFlight(tracked, fcmToken)
            _isTracked.value = true
        }
    }

    fun untrackFlight(fcmToken: String?) {
        val flightId = currentFlightId ?: return
        viewModelScope.launch {
            flightRepository.untrackFlight(flightId, fcmToken)
            _isTracked.value = false
        }
    }

    private suspend fun saveToRecent(flight: FlightData) {
        val flightId = flight.faFlightId ?: return
        flightRepository.addRecentFlight(
            RecentFlight(
                flightId = flightId,
                flightNumber = FlightUtils.getFlightDisplayNumber(flight),
                origin = flight.origin?.codeIata ?: flight.origin?.code ?: "",
                destination = flight.destination?.codeIata ?: flight.destination?.code ?: "",
                viewedAt = System.currentTimeMillis(),
                lastStatus = FlightUtils.getFlightStatus(flight)
            )
        )
    }

    private fun loadWeather(city: String) {
        viewModelScope.launch {
            val result = airportRepository.getWeather(city)
            result.fold(
                onSuccess = { _weather.value = it },
                onFailure = { _weather.value = null }
            )
        }
    }

    class Factory(
        private val flightRepository: FlightRepository,
        private val airportRepository: AirportRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FlightDetailViewModel(flightRepository, airportRepository) as T
        }
    }
}
