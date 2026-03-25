package com.megalife.flighttracker.ui.search

import androidx.lifecycle.*
import com.megalife.flighttracker.data.model.FlightData
import com.megalife.flighttracker.data.repository.AirportRepository
import com.megalife.flighttracker.data.repository.FlightRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SearchMode {
    ALL,
    FLIGHTS,
    AIRPORTS
}

class SearchViewModel(
    private val flightRepository: FlightRepository,
    private val airportRepository: AirportRepository
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<FlightData>>()
    val searchResults: LiveData<List<FlightData>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _query = MutableLiveData<String>()
    val query: LiveData<String> = _query

    private val _searchMode = MutableLiveData(SearchMode.ALL)
    val searchMode: LiveData<SearchMode> = _searchMode

    private var searchJob: Job? = null

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
        // Re-search with current query if exists
        val currentQuery = _query.value
        if (!currentQuery.isNullOrBlank()) {
            search(currentQuery)
        }
    }

    fun search(queryText: String) {
        _query.value = queryText
        searchJob?.cancel()

        if (queryText.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            _error.value = null
            return
        }

        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _isLoading.value = true
            _error.value = null

            val mode = _searchMode.value ?: SearchMode.ALL

            val result = when (mode) {
                SearchMode.FLIGHTS -> flightRepository.searchFlightsByIdent(queryText)
                SearchMode.AIRPORTS -> flightRepository.searchByAirport(queryText)
                SearchMode.ALL -> flightRepository.searchFlights(queryText)
            }

            result.fold(
                onSuccess = { flights ->
                    _searchResults.value = flights
                    _isLoading.value = false
                    if (flights.isEmpty()) {
                        _error.value = "No flights found for \"$queryText\""
                    }
                },
                onFailure = { e ->
                    _searchResults.value = emptyList()
                    _isLoading.value = false
                    _error.value = e.message ?: "Search failed"
                }
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _query.value = ""
        _searchResults.value = emptyList()
        _isLoading.value = false
        _error.value = null
    }

    class Factory(
        private val flightRepository: FlightRepository,
        private val airportRepository: AirportRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(flightRepository, airportRepository) as T
        }
    }
}
