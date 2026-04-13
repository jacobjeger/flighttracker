package com.megalife.flighttracker.ui.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.ui.adapter.FlightAdapter
import com.megalife.flighttracker.ui.detail.FlightDetailActivity

class SearchFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var searchEditText: EditText
    private lateinit var searchStatus: TextView
    private lateinit var resultsList: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var adapter: FlightAdapter
    private lateinit var modeAll: TextView
    private lateinit var modeFlights: TextView
    private lateinit var modeAirports: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as FlightTrackerApp
        viewModel = ViewModelProvider(
            this,
            SearchViewModel.Factory(app.flightRepository, app.airportRepository)
        )[SearchViewModel::class.java]

        searchEditText = view.findViewById(R.id.search_edit_text)
        searchStatus = view.findViewById(R.id.search_status)
        resultsList = view.findViewById(R.id.search_results_list)
        emptyState = view.findViewById(R.id.empty_state)
        modeAll = view.findViewById(R.id.mode_all)
        modeFlights = view.findViewById(R.id.mode_flights)
        modeAirports = view.findViewById(R.id.mode_airports)

        setupModeSelector()
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupModeSelector() {
        val modeClickListener = View.OnClickListener { v ->
            when (v.id) {
                R.id.mode_all -> selectMode(SearchMode.ALL)
                R.id.mode_flights -> selectMode(SearchMode.FLIGHTS)
                R.id.mode_airports -> selectMode(SearchMode.AIRPORTS)
            }
        }

        modeAll.setOnClickListener(modeClickListener)
        modeFlights.setOnClickListener(modeClickListener)
        modeAirports.setOnClickListener(modeClickListener)

        // Set initial state
        selectMode(SearchMode.ALL)
    }

    private fun selectMode(mode: SearchMode) {
        viewModel.setSearchMode(mode)
        updateModeUI(mode)

        // Update hint
        searchEditText.hint = when (mode) {
            SearchMode.ALL -> getString(R.string.search_hint_all)
            SearchMode.FLIGHTS -> getString(R.string.search_hint_flights)
            SearchMode.AIRPORTS -> getString(R.string.search_hint_airports)
        }
    }

    private fun updateModeUI(mode: SearchMode) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        modeAll.setTextColor(if (mode == SearchMode.ALL) activeColor else inactiveColor)
        modeAll.setTypeface(null, if (mode == SearchMode.ALL) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        modeAll.setBackgroundResource(if (mode == SearchMode.ALL) R.drawable.bg_button_focusable else R.drawable.bg_focusable)

        modeFlights.setTextColor(if (mode == SearchMode.FLIGHTS) activeColor else inactiveColor)
        modeFlights.setTypeface(null, if (mode == SearchMode.FLIGHTS) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        modeFlights.setBackgroundResource(if (mode == SearchMode.FLIGHTS) R.drawable.bg_button_focusable else R.drawable.bg_focusable)

        modeAirports.setTextColor(if (mode == SearchMode.AIRPORTS) activeColor else inactiveColor)
        modeAirports.setTypeface(null, if (mode == SearchMode.AIRPORTS) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        modeAirports.setBackgroundResource(if (mode == SearchMode.AIRPORTS) R.drawable.bg_button_focusable else R.drawable.bg_focusable)
    }

    private fun setupRecyclerView() {
        adapter = FlightAdapter { flight ->
            val intent = Intent(requireContext(), FlightDetailActivity::class.java)
            intent.putExtra(FlightDetailActivity.EXTRA_FLIGHT_ID, flight.faFlightId)
            intent.putExtra(FlightDetailActivity.EXTRA_FLIGHT_IDENT, flight.identIata ?: flight.ident)
            startActivity(intent)
        }
        resultsList.layoutManager = LinearLayoutManager(requireContext())
        resultsList.adapter = adapter
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.search(s?.toString() ?: "")
            }
        })
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { flights ->
            adapter.submitList(flights)
            resultsList.visibility = if (flights.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            searchStatus.visibility = if (loading) View.VISIBLE else View.GONE
            searchStatus.text = getString(R.string.searching)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                emptyState.text = error
                emptyState.visibility = View.VISIBLE
                resultsList.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
            }
        }

        viewModel.searchMode.observe(viewLifecycleOwner) { mode ->
            updateModeUI(mode)
        }
    }

    // --- Public API for Activity's centralized D-pad handling ---

    fun getListView(): RecyclerView? = if (::resultsList.isInitialized) resultsList else null

    fun getItemCount(): Int = if (::adapter.isInitialized) adapter.itemCount else 0

    fun getSearchField(): EditText? = if (::searchEditText.isInitialized) searchEditText else null

    fun getSearchMode(): SearchMode = viewModel.searchMode.value ?: SearchMode.ALL

    fun setSearchMode(mode: SearchMode) {
        selectMode(mode)
    }
}
