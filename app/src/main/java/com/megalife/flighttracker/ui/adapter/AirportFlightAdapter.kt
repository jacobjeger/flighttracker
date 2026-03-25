package com.megalife.flighttracker.ui.adapter

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
import com.megalife.flighttracker.data.model.FlightData
import com.megalife.flighttracker.util.FlightUtils

class AirportFlightAdapter(
    private val isDepartures: Boolean,
    private val onFlightClicked: (FlightData) -> Unit
) : ListAdapter<FlightData, AirportFlightAdapter.ViewHolder>(FlightDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val flightNumber: TextView = view.findViewById(R.id.flight_number)
        val airlineName: TextView = view.findViewById(R.id.airline_name)
        val airportName: TextView = view.findViewById(R.id.airport_name)
        val terminalGate: TextView = view.findViewById(R.id.terminal_gate)
        val scheduledTime: TextView = view.findViewById(R.id.scheduled_time)
        val statusBadge: TextView = view.findViewById(R.id.status_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_airport_flight, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flight = getItem(position)

        holder.flightNumber.text = FlightUtils.getFlightDisplayNumber(flight)
        holder.airlineName.text = FlightUtils.getAirlineName(flight)

        if (isDepartures) {
            holder.airportName.text = flight.destination?.let {
                "${it.codeIata ?: it.code} - ${it.city ?: ""}"
            } ?: ""
            val terminal = flight.terminalOrigin?.let { "T$it" } ?: ""
            val gate = flight.gateOrigin?.let { "G$it" } ?: ""
            holder.terminalGate.text = "$terminal $gate".trim()
            holder.scheduledTime.text = FlightUtils.formatTime(
                flight.scheduledOut ?: flight.scheduledOff,
                flight.origin?.timezone
            )
        } else {
            holder.airportName.text = flight.origin?.let {
                "${it.codeIata ?: it.code} - ${it.city ?: ""}"
            } ?: ""
            val terminal = flight.terminalDestination?.let { "T$it" } ?: ""
            val gate = flight.gateDestination?.let { "G$it" } ?: ""
            holder.terminalGate.text = "$terminal $gate".trim()
            holder.scheduledTime.text = FlightUtils.formatTime(
                flight.scheduledIn ?: flight.scheduledOn,
                flight.destination?.timezone
            )
        }

        val status = FlightUtils.getStatusText(flight)
        holder.statusBadge.text = status
        holder.statusBadge.setTextColor(FlightUtils.getStatusColor(FlightUtils.getFlightStatus(flight)))

        holder.itemView.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onFlightClicked(flight)
        }

        holder.itemView.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    onFlightClicked(flight)
                    return@setOnKeyListener true
                }
            }
            false
        }
    }

    class FlightDiffCallback : DiffUtil.ItemCallback<FlightData>() {
        override fun areItemsTheSame(oldItem: FlightData, newItem: FlightData) = oldItem.faFlightId == newItem.faFlightId
        override fun areContentsTheSame(oldItem: FlightData, newItem: FlightData) = oldItem == newItem
    }
}
