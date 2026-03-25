package com.megalife.flighttracker.ui.recent

import androidx.lifecycle.*
import com.megalife.flighttracker.data.repository.FlightRepository
import kotlinx.coroutines.launch

class RecentViewModel(private val repository: FlightRepository) : ViewModel() {

    val recentFlights = repository.getRecentFlights()

    fun removeRecent(flightId: String) {
        viewModelScope.launch {
            repository.removeRecentFlight(flightId)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearRecentFlights()
        }
    }

    class Factory(private val repository: FlightRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecentViewModel(repository) as T
        }
    }
}
