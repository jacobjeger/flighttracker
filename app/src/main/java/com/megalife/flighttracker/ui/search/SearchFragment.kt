package com.megalife.flighttracker.ui.search

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
import com.megalife.flighttracker.ui.adapter.FlightAdapter
import com.megalife.flighttracker.ui.detail.FlightDetailActivity
import com.megalife.flighttracker.util.DpadUtils

class SearchFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var searchEditText: EditText
    private lateinit var searchStatus: TextView
    private lateinit var resultsList: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var adapter: FlightAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as FlightTrackerApp
        viewModel = ViewModelProvider(this, SearchViewModel.Factory(app.flightRepository))[SearchViewModel::class.java]

        searchEditText = view.findViewById(R.id.search_edit_text)
        searchStatus = view.findViewById(R.id.search_status)
        resultsList = view.findViewById(R.id.search_results_list)
        emptyState = view.findViewById(R.id.empty_state)

        setupRecyclerView()
        setupSearch()
        observeViewModel()

        // Focus search field on open
        Handler(Looper.getMainLooper()).post {
            searchEditText.requestFocus()
        }
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

        // Handle D-pad: number keys type directly, DOWN moves to results
        searchEditText.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                when {
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // Move focus to first result
                        if (adapter.itemCount > 0) {
                            resultsList.getChildAt(0)?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    keyCode == KeyEvent.KEYCODE_BACK -> {
                        if (searchEditText.text.isNotEmpty()) {
                            searchEditText.text.clear()
                            viewModel.clearSearch()
                            return@setOnKeyListener true
                        }
                    }
                    DpadUtils.isNumberKey(keyCode) -> {
                        // Number keys type the digit directly
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
    }
}
