package com.megalife.flighttracker.ui.airports

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
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
import com.megalife.flighttracker.util.DpadUtils

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
        setupSearch()
        observeViewModel()

        Handler(Looper.getMainLooper()).post {
            searchEditText.requestFocus()
        }
    }

    private fun setupRecyclerView() {
        adapter = RecentAirportAdapter { airport ->
            openAirportDetail(airport.airportCode)
        }
        recentAirportsList.layoutManager = LinearLayoutManager(requireContext())
        recentAirportsList.adapter = adapter
    }

    private fun setupSearch() {
        searchEditText.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                when {
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) -> {
                        val query = searchEditText.text.toString().trim()
                        if (query.isNotEmpty()) {
                            openAirportDetail(query.uppercase())
                            return@setOnKeyListener true
                        }
                    }
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (adapter.itemCount > 0) {
                            recentAirportsList.getChildAt(0)?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    keyCode == KeyEvent.KEYCODE_BACK -> {
                        if (searchEditText.text.isNotEmpty()) {
                            searchEditText.text.clear()
                            return@setOnKeyListener true
                        }
                    }
                    DpadUtils.isNumberKey(keyCode) -> {
                        val char = DpadUtils.keyToChar(keyCode)
                        if (char != null) {
                            searchEditText.append(char.toString())
                            return@setOnKeyListener true
                        }
                    }
                }
            }
            false
        }
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
}
