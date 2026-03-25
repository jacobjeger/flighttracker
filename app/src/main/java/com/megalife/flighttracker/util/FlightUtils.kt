package com.megalife.flighttracker.util

import com.megalife.flighttracker.data.model.FlightData
import java.text.SimpleDateFormat
import java.util.*

object FlightUtils {

    fun getFlightStatus(flight: FlightData): String {
        if (flight.cancelled == true) return "Cancelled"
        val progress = flight.progressPercent ?: 0
        val status = flight.status ?: ""

        return when {
            status.contains("cancelled", ignoreCase = true) -> "Cancelled"
            status.contains("landed", ignoreCase = true) || status.contains("arrived", ignoreCase = true) -> "Landed"
            progress in 1..99 || status.contains("en route", ignoreCase = true) || status.contains("airborne", ignoreCase = true) -> "En Route"
            flight.departureDelay != null && flight.departureDelay > 0 -> "Delayed"
            status.contains("delayed", ignoreCase = true) -> "Delayed"
            status.contains("scheduled", ignoreCase = true) -> "Scheduled"
            else -> "On Time"
        }
    }

    fun getStatusColor(status: String): Int {
        return when (status) {
            "On Time", "Scheduled" -> 0xFF4CAF50.toInt() // Green
            "Delayed" -> 0xFFFF9800.toInt() // Yellow/Orange
            "Cancelled" -> 0xFFF44336.toInt() // Red
            "Landed" -> 0xFF2196F3.toInt() // Blue
            "En Route" -> 0xFF2196F3.toInt() // Blue
            else -> 0xFF9E9E9E.toInt() // Grey
        }
    }

    fun getDelayMinutes(flight: FlightData): Int {
        val delay = flight.departureDelay ?: flight.arrivalDelay ?: 0
        return delay / 60 // API returns seconds
    }

    fun getStatusText(flight: FlightData): String {
        val status = getFlightStatus(flight)
        val delay = getDelayMinutes(flight)
        return when {
            status == "Delayed" && delay > 0 -> "Delayed ${delay}min"
            else -> status
        }
    }

    fun formatTime(isoTime: String?, timezone: String?): String {
        if (isoTime == null) return "--:--"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoTime) ?: return "--:--"

            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            outputFormat.timeZone = if (timezone != null) TimeZone.getTimeZone(timezone) else TimeZone.getDefault()
            outputFormat.format(date)
        } catch (e: Exception) {
            "--:--"
        }
    }

    fun formatTimeWithDate(isoTime: String?, timezone: String?): String {
        if (isoTime == null) return "--:--"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoTime) ?: return "--:--"

            val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            outputFormat.timeZone = if (timezone != null) TimeZone.getTimeZone(timezone) else TimeZone.getDefault()
            outputFormat.format(date)
        } catch (e: Exception) {
            "--:--"
        }
    }

    fun parseIsoToMillis(isoTime: String?): Long {
        if (isoTime == null) return 0
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(isoTime)?.time ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun formatDuration(seconds: Int?): String {
        if (seconds == null || seconds <= 0) return "--"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }

    fun getTimeUntil(isoTime: String?): String {
        if (isoTime == null) return ""
        val millis = parseIsoToMillis(isoTime)
        val diff = millis - System.currentTimeMillis()
        if (diff <= 0) return ""
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> "in ${hours}h ${minutes}m"
            else -> "in ${minutes}m"
        }
    }

    fun getTimeSince(isoTime: String?): String {
        if (isoTime == null) return ""
        val millis = parseIsoToMillis(isoTime)
        val diff = System.currentTimeMillis() - millis
        if (diff <= 0) return ""
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> "${hours}h ${minutes}m ago"
            else -> "${minutes}m ago"
        }
    }

    fun getFlightDisplayNumber(flight: FlightData): String {
        return flight.identIata ?: flight.ident ?: flight.flightNumber ?: "Unknown"
    }

    fun getAirlineName(flight: FlightData): String {
        return flight.operatorIata ?: flight.operator ?: flight.operatorIcao ?: ""
    }

    fun getAirlineCode(flight: FlightData): String {
        return flight.operatorIata ?: flight.operatorIcao ?: flight.operator?.take(2) ?: "??"
    }

    fun buildShareText(flight: FlightData): String {
        val status = getStatusText(flight)
        val originCode = flight.origin?.codeIata ?: flight.origin?.code ?: "?"
        val destCode = flight.destination?.codeIata ?: flight.destination?.code ?: "?"
        val depTime = formatTime(flight.scheduledOut ?: flight.scheduledOff, flight.origin?.timezone)
        val arrTime = formatTime(flight.scheduledIn ?: flight.scheduledOn, flight.destination?.timezone)
        val flightNum = getFlightDisplayNumber(flight)

        val sb = StringBuilder()
        sb.appendLine("✈️ $flightNum Update")
        sb.appendLine("$originCode → $destCode")
        sb.appendLine("Status: $status")
        sb.appendLine("Departure: $depTime from Terminal ${flight.terminalOrigin ?: "TBA"} Gate ${flight.gateOrigin ?: "TBA"}")
        sb.appendLine("Arrival: $arrTime at Terminal ${flight.terminalDestination ?: "TBA"} Gate ${flight.gateDestination ?: "TBA"}")
        if (flight.baggageClaim != null) {
            sb.appendLine("Baggage: Carousel ${flight.baggageClaim}")
        }
        return sb.toString()
    }

    fun getCurrentTimeAtTimezone(timezone: String?): String {
        if (timezone == null) return "--:--"
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone(timezone)
        return format.format(Date())
    }

    fun getAirlineColor(code: String): Int {
        // Generate a consistent color from airline code
        val hash = code.hashCode()
        val hue = (hash and 0xFF) * 360f / 256f
        return android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.8f))
    }
}
