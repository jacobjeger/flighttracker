package com.megalife.flighttracker.ui.search

import androidx.lifecycle.*
import com.megalife.flighttracker.data.model.FlightData
import com.megalife.flighttracker.data.repository.FlightRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: FlightRepository) : ViewModel() {

    private val _searchResults = MutableLiveData<List<FlightData>>()
    val searchResults: LiveData<List<FlightData>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _query = MutableLiveData<String>()
    val query: LiveData<String> = _query

    private var searchJob: Job? = null

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

            val result = repository.searchFlights(queryText)
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

    class Factory(private val repository: FlightRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(repository) as T
        }
    }
}
