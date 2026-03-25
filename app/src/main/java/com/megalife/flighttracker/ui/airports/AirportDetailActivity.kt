package com.megalife.flighttracker.ui.airports

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.data.model.FlightData
import com.megalife.flighttracker.ui.adapter.AirportFlightAdapter
import com.megalife.flighttracker.ui.detail.FlightDetailActivity
import com.megalife.flighttracker.util.FlightUtils

class AirportDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: AirportsViewModel
    private lateinit var airportCodeText: TextView
    private lateinit var airportNameText: TextView
    private lateinit var airportLocation: TextView
    private lateinit var airportStatus: TextView
    private lateinit var tabDepartures: TextView
    private lateinit var tabArrivals: TextView
    private lateinit var flightsList: RecyclerView
    private lateinit var loadingIndicator: ProgressBar

    private lateinit var departuresAdapter: AirportFlightAdapter
    private lateinit var arrivalsAdapter: AirportFlightAdapter
    private var showingDepartures = true

    private var currentFilter = "All"
    private var currentSort = "time"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_airport_detail)

        val app = application as FlightTrackerApp
        viewModel = ViewModelProvider(this, AirportsViewModel.Factory(app.airportRepository))[AirportsViewModel::class.java]

        bindViews()
        setupAdapters()
        setupTabs()
        observeViewModel()

        val code = intent.getStringExtra(EXTRA_AIRPORT_CODE) ?: run {
            finish()
            return
        }

        viewModel.loadAirport(code)

        // Handle special keys
        flightsList.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                when (keyCode) {
                    KeyEvent.KEYCODE_STAR -> {
                        viewModel.forceRefresh()
                        Snackbar.make(v, R.string.force_refresh, Snackbar.LENGTH_SHORT).show()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_POUND -> {
                        showSortMenu()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (showingDepartures) {
                            switchToArrivals()
                        } else {
                            switchToDepartures()
                        }
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (showingDepartures) {
                            switchToArrivals()
                        } else {
                            switchToDepartures()
                        }
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun bindViews() {
        airportCodeText = findViewById(R.id.airport_code)
        airportNameText = findViewById(R.id.airport_name)
        airportLocation = findViewById(R.id.airport_location)
        airportStatus = findViewById(R.id.airport_status)
        tabDepartures = findViewById(R.id.tab_departures)
        tabArrivals = findViewById(R.id.tab_arrivals)
        flightsList = findViewById(R.id.flights_list)
        loadingIndicator = findViewById(R.id.loading_indicator)
    }

    private fun setupAdapters() {
        departuresAdapter = AirportFlightAdapter(isDepartures = true) { flight ->
            openFlightDetail(flight)
        }
        arrivalsAdapter = AirportFlightAdapter(isDepartures = false) { flight ->
            openFlightDetail(flight)
        }
        flightsList.layoutManager = LinearLayoutManager(this)
        flightsList.adapter = departuresAdapter
    }

    private fun setupTabs() {
        tabDepartures.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchToDepartures()
        }
        tabDepartures.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        switchToDepartures()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        switchToArrivals()
                        tabArrivals.requestFocus()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        flightsList.getChildAt(0)?.requestFocus()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }

        tabArrivals.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            switchToArrivals()
        }
        tabArrivals.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        switchToArrivals()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        switchToDepartures()
                        tabDepartures.requestFocus()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        flightsList.getChildAt(0)?.requestFocus()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun switchToDepartures() {
        showingDepartures = true
        tabDepartures.setTextColor(ContextCompat.getColor(this, R.color.accent_light))
        tabDepartures.setTypeface(null, android.graphics.Typeface.BOLD)
        tabArrivals.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        tabArrivals.setTypeface(null, android.graphics.Typeface.NORMAL)
        flightsList.adapter = departuresAdapter
        Handler(Looper.getMainLooper()).postDelayed({
            flightsList.getChildAt(0)?.requestFocus()
        }, 100)
    }

    private fun switchToArrivals() {
        showingDepartures = false
        tabArrivals.setTextColor(ContextCompat.getColor(this, R.color.accent_light))
        tabArrivals.setTypeface(null, android.graphics.Typeface.BOLD)
        tabDepartures.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        tabDepartures.setTypeface(null, android.graphics.Typeface.NORMAL)
        flightsList.adapter = arrivalsAdapter
        Handler(Looper.getMainLooper()).postDelayed({
            flightsList.getChildAt(0)?.requestFocus()
        }, 100)
    }

    private fun observeViewModel() {
        viewModel.airportInfo.observe(this) { airport ->
            if (airport != null) {
                airportCodeText.text = airport.airportCode ?: ""
                airportNameText.text = airport.name ?: ""
                airportLocation.text = listOfNotNull(airport.city, airport.state, airport.countryCode).joinToString(", ")
            }
        }

        viewModel.departures.observe(this) { flights ->
            departuresAdapter.submitList(applyFilterAndSort(flights))
        }

        viewModel.arrivals.observe(this) { flights ->
            arrivalsAdapter.submitList(applyFilterAndSort(flights))
        }

        viewModel.isLoading.observe(this) { loading ->
            loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                Snackbar.make(findViewById(android.R.id.content), error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun applyFilterAndSort(flights: List<FlightData>): List<FlightData> {
        var filtered = when (currentFilter) {
            "On Time" -> flights.filter { FlightUtils.getFlightStatus(it) == "On Time" }
            "Delayed" -> flights.filter { FlightUtils.getFlightStatus(it) == "Delayed" }
            "Cancelled" -> flights.filter { FlightUtils.getFlightStatus(it) == "Cancelled" }
            else -> flights
        }

        filtered = when (currentSort) {
            "status" -> filtered.sortedBy { FlightUtils.getFlightStatus(it) }
            "airline" -> filtered.sortedBy { FlightUtils.getAirlineName(it) }
            else -> filtered.sortedBy { it.scheduledOut ?: it.scheduledOff ?: it.scheduledIn ?: it.scheduledOn }
        }

        return filtered
    }

    private fun showFilterMenu() {
        val items = arrayOf("All", "On Time", "Delayed", "Cancelled")
        AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle("Filter")
            .setItems(items) { _, which ->
                currentFilter = items[which]
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSortMenu() {
        val items = arrayOf("Scheduled Time", "Status", "Airline")
        AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle("Sort By")
            .setItems(items) { _, which ->
                currentSort = when (which) {
                    0 -> "time"
                    1 -> "status"
                    2 -> "airline"
                    else -> "time"
                }
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshList() {
        viewModel.departures.value?.let { departuresAdapter.submitList(applyFilterAndSort(it)) }
        viewModel.arrivals.value?.let { arrivalsAdapter.submitList(applyFilterAndSort(it)) }
    }

    private fun openFlightDetail(flight: FlightData) {
        val intent = Intent(this, FlightDetailActivity::class.java)
        intent.putExtra(FlightDetailActivity.EXTRA_FLIGHT_ID, flight.faFlightId)
        intent.putExtra(FlightDetailActivity.EXTRA_FLIGHT_IDENT, flight.identIata ?: flight.ident)
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val EXTRA_AIRPORT_CODE = "airport_code"
    }
}
