package com.megalife.flighttracker.notification

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.ui.detail.FlightDetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlightMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val flightId = data["flight_id"] ?: return
        val title = data["title"] ?: "Flight Update"
        val body = data["body"] ?: ""
        val type = data["type"] ?: "status_change"

        showNotification(flightId, title, body, type)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Re-register with Railway backend
        val app = application as FlightTrackerApp
        CoroutineScope(Dispatchers.IO).launch {
            val trackedFlights = app.flightRepository.getAllTrackedFlightsList()
            app.flightRepository.registerDevice(token, trackedFlights.map { it.flightId })
        }
    }

    private fun showNotification(flightId: String, title: String, body: String, type: String) {
        val intent = Intent(this, FlightDetailActivity::class.java).apply {
            putExtra(FlightDetailActivity.EXTRA_FLIGHT_ID, flightId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(this, flightId.hashCode(), intent, flags)

        val notification = NotificationCompat.Builder(this, FlightTrackerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flight)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .build()

        try {
            NotificationManagerCompat.from(this).notify(flightId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
