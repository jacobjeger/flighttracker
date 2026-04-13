package com.megalife.flighttracker.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.megalife.flighttracker.R
import com.megalife.flighttracker.data.db.entity.RecentFlight
import com.megalife.flighttracker.util.FlightUtils
import java.text.SimpleDateFormat
import java.util.*

class RecentFlightAdapter(
    private val onFlightClicked: (RecentFlight) -> Unit,
    private val onLongPress: (RecentFlight) -> Unit
) : ListAdapter<RecentFlight, RecentFlightAdapter.ViewHolder>(RecentDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val flightNumber: TextView = view.findViewById(R.id.flight_number)
        val route: TextView = view.findViewById(R.id.route)
        val statusBadge: TextView = view.findViewById(R.id.status_badge)
        val viewedAt: TextView = view.findViewById(R.id.viewed_at)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_flight, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flight = getItem(position)

        holder.flightNumber.text = flight.flightNumber
        holder.route.text = "${flight.origin} → ${flight.destination}"

        holder.statusBadge.text = flight.lastStatus
        holder.statusBadge.setTextColor(FlightUtils.getStatusColor(flight.lastStatus))

        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.viewedAt.text = sdf.format(Date(flight.viewedAt))

        holder.itemView.setOnClickListener {
            onFlightClicked(flight)
        }

        holder.itemView.setOnLongClickListener {
            onLongPress(flight)
            true
        }
    }

    class RecentDiffCallback : DiffUtil.ItemCallback<RecentFlight>() {
        override fun areItemsTheSame(oldItem: RecentFlight, newItem: RecentFlight) = oldItem.flightId == newItem.flightId
        override fun areContentsTheSame(oldItem: RecentFlight, newItem: RecentFlight) = oldItem == newItem
    }
}
