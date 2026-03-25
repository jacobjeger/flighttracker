package com.megalife.flighttracker.ui.tracked

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.data.db.entity.TrackedFlight
import com.megalife.flighttracker.ui.MainActivity
import com.megalife.flighttracker.ui.adapter.TrackedFlightAdapter
import com.megalife.flighttracker.ui.detail.FlightDetailActivity
import com.megalife.flighttracker.util.FlightUtils

class TrackedFragment : Fragment() {

    private lateinit var viewModel: TrackedViewModel
    private lateinit var trackedList: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var adapter: TrackedFlightAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tracked, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as FlightTrackerApp
        viewModel = ViewModelProvider(this, TrackedViewModel.Factory(app.flightRepository))[TrackedViewModel::class.java]

        trackedList = view.findViewById(R.id.tracked_list)
        emptyState = view.findViewById(R.id.empty_state)

        setupRecyclerView()
        observeViewModel()

        // Handle * key for force refresh
        view.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (keyCode == KeyEvent.KEYCODE_STAR) {
                    forceRefresh()
                    return@setOnKeyListener true
                }
            }
            false
        }

        // Focus first item
        Handler(Looper.getMainLooper()).postDelayed({
            if (adapter.itemCount > 0) {
                trackedList.getChildAt(0)?.requestFocus()
            }
        }, 250)
    }

    private fun setupRecyclerView() {
        adapter = TrackedFlightAdapter(
            onFlightClicked = { tracked ->
                val intent = Intent(requireContext(), FlightDetailActivity::class.java)
                intent.putExtra(FlightDetailActivity.EXTRA_FLIGHT_ID, tracked.flightId)
                startActivity(intent)
            },
            onLongPress = { tracked ->
                showQuickActions(tracked)
            }
        )
        trackedList.layoutManager = LinearLayoutManager(requireContext())
        trackedList.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.trackedFlights.observe(viewLifecycleOwner) { flights ->
            adapter.submitList(flights)
            emptyState.visibility = if (flights.isEmpty()) View.VISIBLE else View.GONE
            trackedList.visibility = if (flights.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            if (refreshing) {
                view?.let { Snackbar.make(it, R.string.force_refresh, Snackbar.LENGTH_SHORT).show() }
            }
        }
    }

    private fun forceRefresh() {
        viewModel.forceRefresh()
    }

    private fun showQuickActions(tracked: TrackedFlight) {
        val items = arrayOf("Untrack", "Share Status")
        AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setTitle(tracked.flightNumber)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        val fcmToken = (requireActivity() as? MainActivity)?.fcmToken
                        viewModel.untrackFlight(tracked.flightId, fcmToken)
                    }
                    1 -> shareFlightStatus(tracked)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareFlightStatus(tracked: TrackedFlight) {
        val text = buildString {
            appendLine("✈️ ${tracked.flightNumber} Update")
            appendLine("${tracked.origin} → ${tracked.destination}")
            appendLine("Status: ${tracked.lastStatus}")
            tracked.departureTerminal?.let { appendLine("Departure: Terminal $it Gate ${tracked.departureGate ?: "TBA"}") }
            tracked.arrivalTerminal?.let { appendLine("Arrival: Terminal $it Gate ${tracked.arrivalGate ?: "TBA"}") }
            tracked.baggageCarousel?.let { appendLine("Baggage: Carousel $it") }
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Share flight status"))
    }
}
