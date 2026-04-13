package com.megalife.flighttracker.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.megalife.flighttracker.R
import com.megalife.flighttracker.data.model.FlightData
import com.megalife.flighttracker.util.FlightUtils

class FlightAdapter(
    private val onFlightClicked: (FlightData) -> Unit
) : ListAdapter<FlightData, FlightAdapter.ViewHolder>(FlightDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val airlineCircle: TextView = view.findViewById(R.id.airline_circle)
        val flightNumber: TextView = view.findViewById(R.id.flight_number)
        val airlineName: TextView = view.findViewById(R.id.airline_name)
        val statusBadge: TextView = view.findViewById(R.id.status_badge)
        val originCode: TextView = view.findViewById(R.id.origin_code)
        val destinationCode: TextView = view.findViewById(R.id.destination_code)
        val routeCities: TextView = view.findViewById(R.id.route_cities)
        val departureTime: TextView = view.findViewById(R.id.departure_time)
        val arrivalTime: TextView = view.findViewById(R.id.arrival_time)
        val departureTerminalGate: TextView = view.findViewById(R.id.departure_terminal_gate)
        val arrivalTerminalGate: TextView = view.findViewById(R.id.arrival_terminal_gate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flight, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val flight = getItem(position)
        bindFlight(holder, flight)

        holder.itemView.setOnClickListener {
            onFlightClicked(flight)
        }
    }

    private fun bindFlight(holder: ViewHolder, flight: FlightData) {
        val airlineCode = FlightUtils.getAirlineCode(flight)
        holder.airlineCircle.text = airlineCode
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(FlightUtils.getAirlineColor(airlineCode))
        holder.airlineCircle.background = bg

        holder.flightNumber.text = FlightUtils.getFlightDisplayNumber(flight)
        holder.airlineName.text = FlightUtils.getAirlineName(flight)

        val status = FlightUtils.getStatusText(flight)
        holder.statusBadge.text = status
        holder.statusBadge.setTextColor(FlightUtils.getStatusColor(FlightUtils.getFlightStatus(flight)))

        holder.originCode.text = flight.origin?.codeIata ?: flight.origin?.code ?: "?"
        holder.destinationCode.text = flight.destination?.codeIata ?: flight.destination?.code ?: "?"

        val originCity = flight.origin?.city ?: ""
        val destCity = flight.destination?.city ?: ""
        holder.routeCities.text = if (originCity.isNotEmpty() && destCity.isNotEmpty()) {
            "$originCity - $destCity"
        } else ""

        val depTime = FlightUtils.formatTime(
            flight.scheduledOut ?: flight.scheduledOff,
            flight.origin?.timezone
        )
        val arrTime = FlightUtils.formatTime(
            flight.scheduledIn ?: flight.scheduledOn,
            flight.destination?.timezone
        )
        holder.departureTime.text = "Dep: $depTime"
        holder.arrivalTime.text = "Arr: $arrTime"

        val depTerminal = flight.terminalOrigin?.let { "T$it" } ?: "TBA"
        val depGate = flight.gateOrigin?.let { "G$it" } ?: ""
        holder.departureTerminalGate.text = "$depTerminal $depGate".trim()

        val arrTerminal = flight.terminalDestination?.let { "T$it" } ?: "TBA"
        val arrGate = flight.gateDestination?.let { "G$it" } ?: ""
        holder.arrivalTerminalGate.text = "$arrTerminal $arrGate".trim()
    }

    class FlightDiffCallback : DiffUtil.ItemCallback<FlightData>() {
        override fun areItemsTheSame(oldItem: FlightData, newItem: FlightData): Boolean {
            return oldItem.faFlightId == newItem.faFlightId
        }

        override fun areContentsTheSame(oldItem: FlightData, newItem: FlightData): Boolean {
            return oldItem == newItem
        }
    }
}
