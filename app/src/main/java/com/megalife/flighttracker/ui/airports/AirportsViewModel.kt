package com.megalife.flighttracker.ui.airports

import androidx.lifecycle.*
import com.megalife.flighttracker.data.db.entity.RecentAirport
import com.megalife.flighttracker.data.model.AirportResponse
import com.megalife.flighttracker.data.model.FlightData
import com.megalife.flighttracker.data.repository.AirportRepository
import kotlinx.coroutines.launch

class AirportsViewModel(private val repository: AirportRepository) : ViewModel() {

    val recentAirports = repository.getRecentAirports()

    private val _airportInfo = MutableLiveData<AirportResponse?>()
    val airportInfo: LiveData<AirportResponse?> = _airportInfo

    private val _departures = MutableLiveData<List<FlightData>>()
    val departures: LiveData<List<FlightData>> = _departures

    private val _arrivals = MutableLiveData<List<FlightData>>()
    val arrivals: LiveData<List<FlightData>> = _arrivals

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentAirportCode: String? = null

    fun loadAirport(code: String) {
        currentAirportCode = code.uppercase()
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = repository.getAirportInfo(code)
            result.fold(
                onSuccess = { airport ->
                    _airportInfo.value = airport
                    _isLoading.value = false

                    // Save to recent
                    repository.addRecentAirport(
                        RecentAirport(
                            airportCode = airport.airportCode ?: code.uppercase(),
                            airportName = airport.name ?: "",
                            city = airport.city ?: "",
                            viewedAt = System.currentTimeMillis()
                        )
                    )

                    // Load departures and arrivals
                    loadDepartures(code)
                    loadArrivals(code)
                },
                onFailure = { e ->
                    _isLoading.value = false
                    _error.value = e.message
                }
            )
        }
    }

    fun loadDepartures(code: String? = null) {
        val airportCode = code ?: currentAirportCode ?: return
        viewModelScope.launch {
            val result = repository.getAirportDepartures(airportCode)
            result.fold(
                onSuccess = { _departures.value = it },
                onFailure = { _departures.value = emptyList() }
            )
        }
    }

    fun loadArrivals(code: String? = null) {
        val airportCode = code ?: currentAirportCode ?: return
        viewModelScope.launch {
            val result = repository.getAirportArrivals(airportCode)
            result.fold(
                onSuccess = { _arrivals.value = it },
                onFailure = { _arrivals.value = emptyList() }
            )
        }
    }

    fun forceRefresh() {
        currentAirportCode?.let { loadAirport(it) }
    }

    class Factory(private val repository: AirportRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AirportsViewModel(repository) as T
        }
    }
}
