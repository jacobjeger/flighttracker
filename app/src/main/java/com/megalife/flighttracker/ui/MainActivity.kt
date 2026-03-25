package com.megalife.flighttracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.messaging.FirebaseMessaging
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.ui.airports.AirportsFragment
import com.megalife.flighttracker.ui.common.DpadBottomNavigationView
import com.megalife.flighttracker.ui.recent.RecentFragment
import com.megalife.flighttracker.ui.search.SearchFragment
import com.megalife.flighttracker.ui.tracked.TrackedFragment
import com.megalife.flighttracker.worker.FlightRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: DpadBottomNavigationView
    private var currentFragment: Fragment? = null
    var fcmToken: String? = null
        private set

    private val searchFragment by lazy { SearchFragment() }
    private val trackedFragment by lazy { TrackedFragment() }
    private val airportsFragment by lazy { AirportsFragment() }
    private val recentFragment by lazy { RecentFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()
        requestNotificationPermission()
        initFcm()
        FlightRefreshWorker.schedule(this)

        if (savedInstanceState == null) {
            switchFragment(searchFragment)
        }

        // Handle notification deep link
        intent?.getStringExtra("flight_id")?.let { flightId ->
            openFlightDetail(flightId)
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.onUpFromNav = {
            // Focus the content area of current fragment
            currentFragment?.view?.let { view ->
                val focusable = view.focusSearch(android.view.View.FOCUS_DOWN)
                    ?: view.focusSearch(android.view.View.FOCUS_FORWARD)
                focusable?.requestFocus()
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> switchFragment(searchFragment)
                R.id.nav_tracked -> switchFragment(trackedFragment)
                R.id.nav_airports -> switchFragment(airportsFragment)
                R.id.nav_recent -> switchFragment(recentFragment)
            }
            true
        }
    }

    private fun switchFragment(fragment: Fragment) {
        if (currentFragment === fragment) return
        currentFragment = fragment

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Post focus to first item in new fragment
        android.os.Handler(mainLooper).postDelayed({
            fragment.view?.let { view ->
                val focusable = view.focusSearch(android.view.View.FOCUS_DOWN)
                    ?: view.focusSearch(android.view.View.FOCUS_FORWARD)
                focusable?.requestFocus()
            }
        }, 250)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        // If DOWN from content area and no more focusable items below, go to nav bar
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            val currentFocus = currentFocus
            if (currentFocus != null) {
                val nextFocus = currentFocus.focusSearch(android.view.View.FOCUS_DOWN)
                if (nextFocus == null || nextFocus === bottomNav) {
                    bottomNav.requestFocus()
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("flight_id")?.let { flightId ->
            openFlightDetail(flightId)
        }
    }

    private fun openFlightDetail(flightId: String) {
        val detailIntent = android.content.Intent(this, com.megalife.flighttracker.ui.detail.FlightDetailActivity::class.java)
        detailIntent.putExtra(com.megalife.flighttracker.ui.detail.FlightDetailActivity.EXTRA_FLIGHT_ID, flightId)
        startActivity(detailIntent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun initFcm() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            fcmToken = token
            // Register with Railway backend
            val app = application as FlightTrackerApp
            CoroutineScope(Dispatchers.IO).launch {
                val tracked = app.flightRepository.getAllTrackedFlightsList()
                app.flightRepository.registerDevice(token, tracked.map { it.flightId })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FlightRefreshWorker.cancel(this)
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }
}
