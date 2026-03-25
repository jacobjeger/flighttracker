package com.megalife.flighttracker.ui.adapter

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.megalife.flighttracker.R
import com.megalife.flighttracker.data.db.entity.RecentFlight
import com.megalife.flighttracker.util.DpadUtils
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
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onFlightClicked(flight)
        }

        // D-pad long press
        var longPressHandler: Handler? = null
        var longPressRunnable: Runnable? = null

        holder.itemView.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            longPressHandler = Handler(Looper.getMainLooper())
                            longPressRunnable = Runnable { onLongPress(flight) }
                            longPressHandler?.postDelayed(longPressRunnable!!, DpadUtils.LONG_PRESS_THRESHOLD)
                        }
                        true
                    }
                    KeyEvent.ACTION_UP -> {
                        longPressRunnable?.let { longPressHandler?.removeCallbacks(it) }
                        if (event.eventTime - event.downTime < DpadUtils.LONG_PRESS_THRESHOLD) {
                            onFlightClicked(flight)
                        }
                        true
                    }
                    else -> false
                }
            } else false
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
