package com.megalife.flighttracker.ui.airports

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    private var currentListIndex = 0
    private var centerDownTime = 0L

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            mgr.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun doHaptic(ms: Long = 20L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

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
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            return
        }

        viewModel.loadAirport(code)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        // For ACTION_UP on keys we handle, consume silently
        if (event.action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_STAR,
                KeyEvent.KEYCODE_POUND,
                KeyEvent.KEYCODE_BACK -> return true
            }
            return super.dispatchKeyEvent(event)
        }

        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                doHaptic()
                val adapter = currentAdapter()
                if (adapter.itemCount == 0) return true
                if (currentListIndex > 0) {
                    currentListIndex--
                }
                flightsList.scrollToPosition(currentListIndex)
                flightsList.post {
                    flightsList.findViewHolderForAdapterPosition(currentListIndex)?.itemView?.requestFocus()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                doHaptic()
                val adapter = currentAdapter()
                if (adapter.itemCount == 0) return true
                if (currentListIndex < adapter.itemCount - 1) {
                    currentListIndex++
                }
                flightsList.scrollToPosition(currentListIndex)
                flightsList.post {
                    flightsList.findViewHolderForAdapterPosition(currentListIndex)?.itemView?.requestFocus()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                doHaptic()
                if (showingDepartures) {
                    switchToArrivals()
                } else {
                    switchToDepartures()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                doHaptic()
                if (showingDepartures) {
                    switchToArrivals()
                } else {
                    switchToDepartures()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                doHaptic()
                // Track long-press timing
                if (event.repeatCount == 0) {
                    centerDownTime = event.eventTime
                }
                val held = event.eventTime - centerDownTime
                if (held >= 500) {
                    doHaptic(40)
                    showFilterMenu()
                    centerDownTime = Long.MAX_VALUE // prevent re-trigger
                    return true
                }
                // Short press on first tap only
                if (event.repeatCount == 0) {
                    val adapter = currentAdapter()
                    if (adapter.itemCount > 0 && currentListIndex in 0 until adapter.itemCount) {
                        val flights = if (showingDepartures) {
                            viewModel.departures.value?.let { applyFilterAndSort(it) }
                        } else {
                            viewModel.arrivals.value?.let { applyFilterAndSort(it) }
                        }
                        flights?.getOrNull(currentListIndex)?.let { openFlightDetail(it) }
                    }
                }
                return true
            }

            KeyEvent.KEYCODE_STAR -> {
                doHaptic()
                viewModel.forceRefresh()
                Snackbar.make(findViewById(android.R.id.content), R.string.force_refresh, Snackbar.LENGTH_SHORT).show()
                return true
            }

            KeyEvent.KEYCODE_POUND -> {
                doHaptic()
                showSortMenu()
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                doHaptic()
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun currentAdapter(): AirportFlightAdapter {
        return if (showingDepartures) departuresAdapter else arrivalsAdapter
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
            doHaptic()
            switchToDepartures()
        }
        tabArrivals.setOnClickListener {
            doHaptic()
            switchToArrivals()
        }
    }

    private fun switchToDepartures() {
        showingDepartures = true
        currentListIndex = 0
        tabDepartures.setTextColor(ContextCompat.getColor(this, R.color.accent_light))
        tabDepartures.setTypeface(null, android.graphics.Typeface.BOLD)
        tabArrivals.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        tabArrivals.setTypeface(null, android.graphics.Typeface.NORMAL)
        flightsList.adapter = departuresAdapter
        flightsList.scrollToPosition(0)
        flightsList.post {
            flightsList.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
        }
    }

    private fun switchToArrivals() {
        showingDepartures = false
        currentListIndex = 0
        tabArrivals.setTextColor(ContextCompat.getColor(this, R.color.accent_light))
        tabArrivals.setTypeface(null, android.graphics.Typeface.BOLD)
        tabDepartures.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        tabDepartures.setTypeface(null, android.graphics.Typeface.NORMAL)
        flightsList.adapter = arrivalsAdapter
        flightsList.scrollToPosition(0)
        flightsList.post {
            flightsList.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
        }
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
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    companion object {
        const val EXTRA_AIRPORT_CODE = "airport_code"
    }
}
