package com.megalife.flighttracker.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.megalife.flighttracker.R
import com.megalife.flighttracker.data.db.entity.RecentAirport

class RecentAirportAdapter(
    private val onAirportClicked: (RecentAirport) -> Unit
) : ListAdapter<RecentAirport, RecentAirportAdapter.ViewHolder>(AirportDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val airportCode: TextView = view.findViewById(R.id.airport_code)
        val airportName: TextView = view.findViewById(R.id.airport_name)
        val airportCity: TextView = view.findViewById(R.id.airport_city)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_airport, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val airport = getItem(position)

        holder.airportCode.text = airport.airportCode
        holder.airportName.text = airport.airportName
        holder.airportCity.text = airport.city

        holder.itemView.setOnClickListener {
            onAirportClicked(airport)
        }
    }

    class AirportDiffCallback : DiffUtil.ItemCallback<RecentAirport>() {
        override fun areItemsTheSame(oldItem: RecentAirport, newItem: RecentAirport) = oldItem.airportCode == newItem.airportCode
        override fun areContentsTheSame(oldItem: RecentAirport, newItem: RecentAirport) = oldItem == newItem
    }
}
