package com.megalife.flighttracker.ui.detail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.data.model.FlightData
import com.megalife.flighttracker.ui.MainActivity
import com.megalife.flighttracker.util.FlightUtils
import java.util.Timer
import java.util.TimerTask

class FlightDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: FlightDetailViewModel
    private var refreshTimer: Timer? = null

    // Views
    private lateinit var scrollView: ScrollView
    private lateinit var airlineCircle: TextView
    private lateinit var flightNumber: TextView
    private lateinit var airlineName: TextView
    private lateinit var statusBadge: TextView
    private lateinit var originCode: TextView
    private lateinit var originCity: TextView
    private lateinit var originName: TextView
    private lateinit var destCode: TextView
    private lateinit var destCity: TextView
    private lateinit var destName: TextView
    private lateinit var flightDuration: TextView
    private lateinit var depTerminal: TextView
    private lateinit var depGate: TextView
    private lateinit var arrTerminal: TextView
    private lateinit var arrGate: TextView
    private lateinit var gateChangedLabel: TextView
    private lateinit var baggageInfo: TextView
    private lateinit var progressContainer: LinearLayout
    private lateinit var progressSectionHeader: TextView
    private lateinit var flightProgressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var progressDetail: TextView
    private lateinit var aircraftType: TextView
    private lateinit var aircraftRegistration: TextView
    private lateinit var weatherTemp: TextView
    private lateinit var weatherCondition: TextView
    private lateinit var trackButton: TextView
    private lateinit var shareButton: TextView
    private lateinit var airlineFullName: TextView
    private lateinit var airlinePhone: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var delayText: TextView
    private lateinit var timeUntil: TextView
    private lateinit var destLocalTime: TextView

    // Time rows
    private lateinit var scheduledDepLabel: TextView
    private lateinit var scheduledDepValue: TextView
    private lateinit var scheduledDepAlt: TextView
    private lateinit var actualDepLabel: TextView
    private lateinit var actualDepValue: TextView
    private lateinit var actualDepAlt: TextView
    private lateinit var scheduledArrLabel: TextView
    private lateinit var scheduledArrValue: TextView
    private lateinit var scheduledArrAlt: TextView
    private lateinit var actualArrLabel: TextView
    private lateinit var actualArrValue: TextView
    private lateinit var actualArrAlt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flight_detail)

        val app = application as FlightTrackerApp
        viewModel = ViewModelProvider(
            this,
            FlightDetailViewModel.Factory(app.flightRepository, app.airportRepository)
        )[FlightDetailViewModel::class.java]

        bindViews()
        observeViewModel()

        val flightId = intent.getStringExtra(EXTRA_FLIGHT_ID)
        val flightIdent = intent.getStringExtra(EXTRA_FLIGHT_IDENT)

        when {
            flightId != null -> viewModel.loadFlight(flightId)
            flightIdent != null -> viewModel.loadFlightByIdent(flightIdent)
            else -> {
                finish()
                return
            }
        }

        setupButtons()

        // Focus first button
        Handler(Looper.getMainLooper()).postDelayed({
            trackButton.requestFocus()
        }, 300)
    }

    private fun bindViews() {
        scrollView = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0) as ScrollView

        airlineCircle = findViewById(R.id.airline_circle)
        flightNumber = findViewById(R.id.flight_number)
        airlineName = findViewById(R.id.airline_name)
        statusBadge = findViewById(R.id.status_badge)
        originCode = findViewById(R.id.origin_code)
        originCity = findViewById(R.id.origin_city)
        originName = findViewById(R.id.origin_name)
        destCode = findViewById(R.id.dest_code)
        destCity = findViewById(R.id.dest_city)
        destName = findViewById(R.id.dest_name)
        flightDuration = findViewById(R.id.flight_duration)
        depTerminal = findViewById(R.id.dep_terminal)
        depGate = findViewById(R.id.dep_gate)
        arrTerminal = findViewById(R.id.arr_terminal)
        arrGate = findViewById(R.id.arr_gate)
        gateChangedLabel = findViewById(R.id.gate_changed_label)
        baggageInfo = findViewById(R.id.baggage_info)
        progressContainer = findViewById(R.id.progress_container)
        progressSectionHeader = findViewById(R.id.progress_section_header)
        flightProgressBar = findViewById(R.id.flight_progress_bar)
        progressText = findViewById(R.id.progress_text)
        progressDetail = findViewById(R.id.progress_detail)
        aircraftType = findViewById(R.id.aircraft_type)
        aircraftRegistration = findViewById(R.id.aircraft_registration)
        weatherTemp = findViewById(R.id.weather_temp)
        weatherCondition = findViewById(R.id.weather_condition)
        trackButton = findViewById(R.id.track_button)
        shareButton = findViewById(R.id.share_button)
        airlineFullName = findViewById(R.id.airline_full_name)
        airlinePhone = findViewById(R.id.airline_phone)
        loadingIndicator = findViewById(R.id.loading_indicator)
        delayText = findViewById(R.id.delay_text)
        timeUntil = findViewById(R.id.time_until)
        destLocalTime = findViewById(R.id.dest_local_time)

        // Time rows
        val scheduledDepRow = findViewById<View>(R.id.scheduled_departure_row)
        scheduledDepLabel = scheduledDepRow.findViewById(R.id.time_label)
        scheduledDepValue = scheduledDepRow.findViewById(R.id.time_value)
        scheduledDepAlt = scheduledDepRow.findViewById(R.id.time_alt)

        val actualDepRow = findViewById<View>(R.id.actual_departure_row)
        actualDepLabel = actualDepRow.findViewById(R.id.time_label)
        actualDepValue = actualDepRow.findViewById(R.id.time_value)
        actualDepAlt = actualDepRow.findViewById(R.id.time_alt)

        val scheduledArrRow = findViewById<View>(R.id.scheduled_arrival_row)
        scheduledArrLabel = scheduledArrRow.findViewById(R.id.time_label)
        scheduledArrValue = scheduledArrRow.findViewById(R.id.time_value)
        scheduledArrAlt = scheduledArrRow.findViewById(R.id.time_alt)

        val actualArrRow = findViewById<View>(R.id.actual_arrival_row)
        actualArrLabel = actualArrRow.findViewById(R.id.time_label)
        actualArrValue = actualArrRow.findViewById(R.id.time_value)
        actualArrAlt = actualArrRow.findViewById(R.id.time_alt)
    }

    private fun observeViewModel() {
        viewModel.flight.observe(this) { flight ->
            displayFlight(flight)
        }

        viewModel.weather.observe(this) { weather ->
            if (weather != null) {
                val temp = weather.main?.temp?.toInt() ?: "--"
                weatherTemp.text = "${temp}°C"
                weatherCondition.text = weather.weather?.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
            } else {
                weatherTemp.text = "--"
                weatherCondition.text = "Unavailable"
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                Snackbar.make(findViewById(android.R.id.content), error, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.isTracked.observe(this) { tracked ->
            trackButton.text = if (tracked) getString(R.string.untrack_flight) else getString(R.string.track_flight)
        }
    }

    private fun displayFlight(flight: FlightData) {
        val code = FlightUtils.getAirlineCode(flight)
        airlineCircle.text = code
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(FlightUtils.getAirlineColor(code))
        airlineCircle.background = bg

        flightNumber.text = FlightUtils.getFlightDisplayNumber(flight)
        airlineName.text = FlightUtils.getAirlineName(flight)

        val status = FlightUtils.getFlightStatus(flight)
        val statusText = FlightUtils.getStatusText(flight)
        statusBadge.text = statusText
        statusBadge.setTextColor(FlightUtils.getStatusColor(status))

        // Route
        originCode.text = flight.origin?.codeIata ?: flight.origin?.code ?: "?"
        originCity.text = flight.origin?.city ?: ""
        originName.text = flight.origin?.name ?: ""
        destCode.text = flight.destination?.codeIata ?: flight.destination?.code ?: "?"
        destCity.text = flight.destination?.city ?: ""
        destName.text = flight.destination?.name ?: ""
        flightDuration.text = "Duration: ${FlightUtils.formatDuration(flight.filedEte)}"

        // Times
        val originTz = flight.origin?.timezone
        val destTz = flight.destination?.timezone

        scheduledDepLabel.text = "Scheduled Departure"
        scheduledDepValue.text = FlightUtils.formatTime(flight.scheduledOut ?: flight.scheduledOff, originTz)
        scheduledDepAlt.text = destTz?.let { "(${FlightUtils.formatTime(flight.scheduledOut ?: flight.scheduledOff, it)} dest)" } ?: ""

        val actualDep = flight.actualOut ?: flight.actualOff ?: flight.estimatedOut ?: flight.estimatedOff
        actualDepLabel.text = if (flight.actualOut != null || flight.actualOff != null) "Actual Departure" else "Estimated Departure"
        actualDepValue.text = FlightUtils.formatTime(actualDep, originTz)
        actualDepAlt.text = ""

        scheduledArrLabel.text = "Scheduled Arrival"
        scheduledArrValue.text = FlightUtils.formatTime(flight.scheduledIn ?: flight.scheduledOn, destTz)
        scheduledArrAlt.text = originTz?.let { "(${FlightUtils.formatTime(flight.scheduledIn ?: flight.scheduledOn, it)} orig)" } ?: ""

        val actualArr = flight.actualIn ?: flight.actualOn ?: flight.estimatedIn ?: flight.estimatedOn
        actualArrLabel.text = if (flight.actualIn != null || flight.actualOn != null) "Actual Arrival" else "Estimated Arrival"
        actualArrValue.text = FlightUtils.formatTime(actualArr, destTz)
        actualArrAlt.text = ""

        // Delay
        val delay = FlightUtils.getDelayMinutes(flight)
        if (delay > 0) {
            delayText.text = "+${delay} minutes"
            delayText.visibility = View.VISIBLE
        } else {
            delayText.visibility = View.GONE
        }

        // Time until
        val depTime = flight.scheduledOut ?: flight.scheduledOff
        val arrTime = flight.scheduledIn ?: flight.scheduledOn
        when (status) {
            "Scheduled", "On Time", "Delayed" -> {
                val until = FlightUtils.getTimeUntil(depTime)
                if (until.isNotEmpty()) {
                    timeUntil.text = "Departs $until"
                    timeUntil.visibility = View.VISIBLE
                } else {
                    timeUntil.visibility = View.GONE
                }
            }
            "En Route" -> {
                val until = FlightUtils.getTimeUntil(arrTime)
                if (until.isNotEmpty()) {
                    timeUntil.text = "Arrives $until"
                    timeUntil.visibility = View.VISIBLE
                } else {
                    timeUntil.visibility = View.GONE
                }
            }
            else -> timeUntil.visibility = View.GONE
        }

        // Destination local time
        destTz?.let {
            destLocalTime.text = "Current time at destination: ${FlightUtils.getCurrentTimeAtTimezone(it)}"
        }

        // Terminal & Gate
        depTerminal.text = "Terminal ${flight.terminalOrigin ?: "TBA"}"
        depGate.text = "Gate ${flight.gateOrigin ?: "TBA"}"
        arrTerminal.text = "Terminal ${flight.terminalDestination ?: "TBA"}"
        arrGate.text = "Gate ${flight.gateDestination ?: "TBA"}"

        if (flight.terminalOrigin == null && flight.gateOrigin == null) {
            depTerminal.setTextColor(ContextCompat.getColor(this, R.color.text_hint))
            depGate.setTextColor(ContextCompat.getColor(this, R.color.text_hint))
        }

        // Baggage
        if (flight.baggageClaim != null) {
            baggageInfo.text = "Baggage: Carousel ${flight.baggageClaim}"
            baggageInfo.visibility = View.VISIBLE
        } else {
            baggageInfo.visibility = View.GONE
        }

        // Progress
        val progress = flight.progressPercent ?: 0
        if (progress > 0 && status == "En Route") {
            progressSectionHeader.visibility = View.VISIBLE
            progressContainer.visibility = View.VISIBLE
            flightProgressBar.progress = progress
            progressText.text = "$progress% complete"

            val depSince = FlightUtils.getTimeSince(flight.actualOff ?: flight.actualOut)
            val arrIn = FlightUtils.getTimeUntil(flight.estimatedOn ?: flight.estimatedIn ?: arrTime)
            if (depSince.isNotEmpty() && arrIn.isNotEmpty()) {
                progressDetail.text = "Departed $depSince — Arrives $arrIn"
            }
        } else {
            progressSectionHeader.visibility = View.GONE
            progressContainer.visibility = View.GONE
        }

        // Aircraft
        aircraftType.text = flight.aircraftType ?: "Unknown"
        aircraftRegistration.text = flight.registration?.let { "Registration: $it" } ?: ""

        // Airline info
        airlineFullName.text = FlightUtils.getAirlineName(flight)
    }

    private fun setupButtons() {
        trackButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            toggleTrack()
        }
        trackButton.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                toggleTrack()
                true
            } else false
        }

        shareButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            shareFlight()
        }
        shareButton.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                shareFlight()
                true
            } else false
        }

        airlinePhone.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            makeCall()
        }
        airlinePhone.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                makeCall()
                true
            } else false
        }
    }

    private fun toggleTrack() {
        val isTracked = viewModel.isTracked.value ?: false
        // Get FCM token - try from parent activity or fetch fresh
        val fcmToken: String? = null // Will be fetched from FirebaseMessaging
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (isTracked) {
                viewModel.untrackFlight(token)
            } else {
                viewModel.trackFlight(token)
            }
        }.addOnFailureListener {
            if (isTracked) {
                viewModel.untrackFlight(null)
            } else {
                viewModel.trackFlight(null)
            }
        }
    }

    private fun shareFlight() {
        val flight = viewModel.flight.value ?: return
        val text = FlightUtils.buildShareText(flight)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Share flight status"))
    }

    private fun makeCall() {
        val phone = airlinePhone.text.toString()
        if (phone.isBlank()) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
            startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PERMISSION)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        // Auto-refresh every 2 minutes
        refreshTimer = Timer()
        refreshTimer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread { viewModel.refreshFlight() }
            }
        }, 120_000L, 120_000L)
    }

    override fun onPause() {
        super.onPause()
        refreshTimer?.cancel()
        refreshTimer = null
    }

    companion object {
        const val EXTRA_FLIGHT_ID = "flight_id"
        const val EXTRA_FLIGHT_IDENT = "flight_ident"
        private const val REQUEST_CALL_PERMISSION = 200
    }
}
