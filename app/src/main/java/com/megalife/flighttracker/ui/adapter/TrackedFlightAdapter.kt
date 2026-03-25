package com.megalife.flighttracker.ui.adapter

import android.graphics.drawable.GradientDrawable
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
import com.megalife.flighttracker.data.db.entity.TrackedFlight
import com.megalife.flighttracker.util.DpadUtils
import com.megalife.flighttracker.util.FlightUtils
import java.text.SimpleDateFormat
import java.util.*

class TrackedFlightAdapter(
    private val onFlightClicked: (TrackedFlight) -> Unit,
    private val onLongPress: (TrackedFlight) -> Unit
) : ListAdapter<TrackedFlight, TrackedFlightAdapter.ViewHolder>(TrackedFlightDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val airlineCircle: TextView = view.findViewById(R.id.airline_circle)
        val flightNumber: TextView = view.findViewById(R.id.flight_number)
        val airlineName: TextView = view.findViewById(R.id.airline_name)
        val statusBadge: TextView = view.findViewById(R.id.status_badge)
        val originCode: TextView = view.findViewById(R.id.origin_code)
        val destinationCode: TextView = view.findViewById(R.id.destination_code)
        val departureTime: TextView = view.findViewById(R.id.departure_time)
        val arrivalTime: TextView = view.findViewById(R.id.arrival_time)
        val departureTerminalGate: TextView = view.findViewById(R.id.departure_terminal_gate)
        val arrivalTerminalGate: TextView = view.findViewById(R.id.arrival_terminal_gate)
        val baggageInfo: TextView = view.findViewById(R.id.baggage_info)
        val lastUpdated: TextView = view.findViewById(R.id.last_updated)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tracked_flight, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flight = getItem(position)

        // Airline circle
        val code = if (flight.airline.length >= 2) flight.airline.take(2) else flight.flightNumber.take(2)
        holder.airlineCircle.text = code.uppercase()
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(FlightUtils.getAirlineColor(code))
        holder.airlineCircle.background = bg

        holder.flightNumber.text = flight.flightNumber
        holder.airlineName.text = flight.airline

        holder.statusBadge.text = flight.lastStatus
        holder.statusBadge.setTextColor(FlightUtils.getStatusColor(flight.lastStatus))

        holder.originCode.text = flight.origin
        holder.destinationCode.text = flight.destination

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        if (flight.originTimezone.isNotEmpty()) {
            sdf.timeZone = TimeZone.getTimeZone(flight.originTimezone)
        }
        holder.departureTime.text = "Dep: ${sdf.format(Date(flight.scheduledDeparture))}"

        if (flight.destinationTimezone.isNotEmpty()) {
            sdf.timeZone = TimeZone.getTimeZone(flight.destinationTimezone)
        }
        holder.arrivalTime.text = "Arr: ${sdf.format(Date(flight.scheduledArrival))}"

        val depTerminal = flight.departureTerminal?.let { "T$it" } ?: "TBA"
        val depGate = flight.departureGate?.let { "G$it" } ?: ""
        holder.departureTerminalGate.text = "$depTerminal $depGate".trim()

        val arrTerminal = flight.arrivalTerminal?.let { "T$it" } ?: "TBA"
        val arrGate = flight.arrivalGate?.let { "G$it" } ?: ""
        holder.arrivalTerminalGate.text = "$arrTerminal $arrGate".trim()

        if (flight.baggageCarousel != null) {
            holder.baggageInfo.text = "Baggage: Carousel ${flight.baggageCarousel}"
            holder.baggageInfo.visibility = View.VISIBLE
        } else {
            holder.baggageInfo.visibility = View.GONE
        }

        val updatedSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.lastUpdated.text = "Updated ${updatedSdf.format(Date(flight.lastUpdated))}"

        // Click handling
        holder.itemView.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onFlightClicked(flight)
        }

        // D-pad long press handling
        var longPressHandler: Handler? = null
        var longPressRunnable: Runnable? = null

        holder.itemView.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            longPressHandler = Handler(Looper.getMainLooper())
                            longPressRunnable = Runnable {
                                onLongPress(flight)
                            }
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
            } else {
                false
            }
        }

        holder.itemView.setOnLongClickListener {
            onLongPress(flight)
            true
        }
    }

    class TrackedFlightDiffCallback : DiffUtil.ItemCallback<TrackedFlight>() {
        override fun areItemsTheSame(oldItem: TrackedFlight, newItem: TrackedFlight): Boolean {
            return oldItem.flightId == newItem.flightId
        }

        override fun areContentsTheSame(oldItem: TrackedFlight, newItem: TrackedFlight): Boolean {
            return oldItem == newItem
        }
    }
}
