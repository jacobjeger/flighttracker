package com.megalife.flighttracker.ui.recent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.data.db.entity.RecentFlight
import com.megalife.flighttracker.ui.adapter.RecentFlightAdapter
import com.megalife.flighttracker.ui.detail.FlightDetailActivity

class RecentFragment : Fragment() {

    private lateinit var viewModel: RecentViewModel
    private lateinit var recentList: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var clearAllButton: TextView
    private lateinit var adapter: RecentFlightAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as FlightTrackerApp
        viewModel = ViewModelProvider(this, RecentViewModel.Factory(app.flightRepository))[RecentViewModel::class.java]

        recentList = view.findViewById(R.id.recent_list)
        emptyState = view.findViewById(R.id.empty_state)
        clearAllButton = view.findViewById(R.id.clear_all_button)

        setupRecyclerView()
        observeViewModel()

        clearAllButton.setOnClickListener {
            viewModel.clearAll()
        }
    }

    private fun setupRecyclerView() {
        adapter = RecentFlightAdapter(
            onFlightClicked = { recent ->
                val intent = Intent(requireContext(), FlightDetailActivity::class.java)
                intent.putExtra(FlightDetailActivity.EXTRA_FLIGHT_ID, recent.flightId)
                startActivity(intent)
            },
            onLongPress = { recent ->
                showQuickActions(recent)
            }
        )
        recentList.layoutManager = LinearLayoutManager(requireContext())
        recentList.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.recentFlights.observe(viewLifecycleOwner) { flights ->
            adapter.submitList(flights)
            emptyState.visibility = if (flights.isEmpty()) View.VISIBLE else View.GONE
            recentList.visibility = if (flights.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showQuickActions(recent: RecentFlight) {
        val items = arrayOf("Remove from Recent", "Track Flight")
        AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setTitle(recent.flightNumber)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> viewModel.removeRecent(recent.flightId)
                    1 -> {
                        // Open detail to track
                        val intent = Intent(requireContext(), FlightDetailActivity::class.java)
                        intent.putExtra(FlightDetailActivity.EXTRA_FLIGHT_ID, recent.flightId)
                        startActivity(intent)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- Public API for Activity's centralized D-pad handling ---

    fun getListView(): RecyclerView? = if (::recentList.isInitialized) recentList else null

    fun getItemCount(): Int = if (::adapter.isInitialized) adapter.itemCount else 0

    fun showQuickActions(position: Int) {
        val recent = adapter.currentList.getOrNull(position) ?: return
        showQuickActions(recent)
    }

    fun clearAll() {
        viewModel.clearAll()
    }
}
