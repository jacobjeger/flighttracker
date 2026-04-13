package com.megalife.flighttracker.ui.airports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.ui.adapter.RecentAirportAdapter

class AirportsFragment : Fragment() {

    private lateinit var viewModel: AirportsViewModel
    private lateinit var searchEditText: EditText
    private lateinit var recentLabel: TextView
    private lateinit var recentAirportsList: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var adapter: RecentAirportAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_airports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as FlightTrackerApp
        viewModel = ViewModelProvider(this, AirportsViewModel.Factory(app.airportRepository))[AirportsViewModel::class.java]

        searchEditText = view.findViewById(R.id.airport_search_edit_text)
        recentLabel = view.findViewById(R.id.recent_label)
        recentAirportsList = view.findViewById(R.id.recent_airports_list)
        emptyState = view.findViewById(R.id.empty_state)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = RecentAirportAdapter { airport ->
            openAirportDetail(airport.airportCode)
        }
        recentAirportsList.layoutManager = LinearLayoutManager(requireContext())
        recentAirportsList.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.recentAirports.observe(viewLifecycleOwner) { airports ->
            adapter.submitList(airports)
            recentLabel.visibility = if (airports.isNotEmpty()) View.VISIBLE else View.GONE
            recentAirportsList.visibility = if (airports.isNotEmpty()) View.VISIBLE else View.GONE
            emptyState.visibility = if (airports.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openAirportDetail(code: String) {
        val intent = Intent(requireContext(), AirportDetailActivity::class.java)
        intent.putExtra(AirportDetailActivity.EXTRA_AIRPORT_CODE, code)
        startActivity(intent)
    }

    // --- Public API for Activity's centralized D-pad handling ---

    fun getListView(): RecyclerView? = if (::recentAirportsList.isInitialized) recentAirportsList else null

    fun getItemCount(): Int = if (::adapter.isInitialized) adapter.itemCount else 0

    fun getSearchField(): EditText? = if (::searchEditText.isInitialized) searchEditText else null

    fun submitAirportSearch() {
        val query = searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            openAirportDetail(query.uppercase())
        }
    }

    fun onFilterToggle() {
        // * key: focus the search field for filtering
        if (::searchEditText.isInitialized) {
            searchEditText.text.clear()
            searchEditText.requestFocus()
        }
    }

    fun onSortToggle() {
        // # key: toggle sort order on recent airports list
        // TODO: implement sort toggling in viewmodel when needed
    }
}
