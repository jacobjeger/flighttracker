package com.megalife.flighttracker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import com.megalife.flighttracker.FlightTrackerApp
import com.megalife.flighttracker.R
import com.megalife.flighttracker.ui.airports.AirportsFragment
import com.megalife.flighttracker.ui.detail.FlightDetailActivity
import com.megalife.flighttracker.ui.recent.RecentFragment
import com.megalife.flighttracker.ui.search.SearchFragment
import com.megalife.flighttracker.ui.tracked.TrackedFragment
import com.megalife.flighttracker.worker.FlightRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var currentFragment: Fragment? = null
    var fcmToken: String? = null
        private set

    private val searchFragment by lazy { SearchFragment() }
    private val trackedFragment by lazy { TrackedFragment() }
    private val airportsFragment by lazy { AirportsFragment() }
    private val recentFragment by lazy { RecentFragment() }

    // D-pad state
    private var focusInNav = false
    private var navSelectedIndex = 0
    private var contentFocusedIndex = 0

    // Long-press CENTER handling
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressFired = false

    // Vibrator
    private lateinit var vibrator: Vibrator

    // Tab item IDs in order
    private val tabIds = intArrayOf(
        R.id.nav_search,
        R.id.nav_tracked,
        R.id.nav_airports,
        R.id.nav_recent
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        bottomNav = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()
        requestNotificationPermission()
        initFcm()
        FlightRefreshWorker.schedule(this)

        if (savedInstanceState == null) {
            navSelectedIndex = 0
            switchFragment(searchFragment)
        }

        // Handle notification deep link
        intent?.getStringExtra("flight_id")?.let { flightId ->
            openFlightDetail(flightId)
        }
    }

    private fun setupBottomNavigation() {
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
        contentFocusedIndex = 0

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Post focus to content area of new fragment
        bottomNav.post {
            focusContent()
        }
    }

    // --- Haptics via Vibrator service ---

    private fun doHaptic(ms: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

    // --- Fragment list accessors ---

    private fun getFragmentRecyclerView(): RecyclerView? {
        val frag = currentFragment ?: return null
        return when (frag) {
            is SearchFragment -> frag.getListView()
            is TrackedFragment -> frag.getListView()
            is AirportsFragment -> frag.getListView()
            is RecentFragment -> frag.getListView()
            else -> null
        }
    }

    private fun getFragmentListCount(): Int {
        val rv = getFragmentRecyclerView() ?: return 0
        return rv.adapter?.itemCount ?: 0
    }

    private fun getFragmentSearchField(): EditText? {
        val frag = currentFragment ?: return null
        return when (frag) {
            is SearchFragment -> frag.getSearchField()
            is AirportsFragment -> frag.getSearchField()
            else -> null
        }
    }

    // --- Focus management ---

    private fun focusContent() {
        focusInNav = false
        val rv = getFragmentRecyclerView()
        val count = getFragmentListCount()
        if (rv != null && count > 0) {
            val idx = contentFocusedIndex.coerceIn(0, count - 1)
            focusRecyclerItem(rv, idx)
        } else {
            // Try search field or first focusable
            val searchField = getFragmentSearchField()
            if (searchField != null) {
                searchField.requestFocus()
            } else {
                currentFragment?.view?.let { v ->
                    val focusable = v.focusSearch(android.view.View.FOCUS_DOWN)
                        ?: v.focusSearch(android.view.View.FOCUS_FORWARD)
                    focusable?.requestFocus()
                }
            }
        }
    }

    private fun focusRecyclerItem(rv: RecyclerView, index: Int) {
        contentFocusedIndex = index
        rv.scrollToPosition(index)
        rv.post {
            rv.findViewHolderForAdapterPosition(index)?.itemView?.requestFocus()
        }
    }

    private fun focusNavBar() {
        focusInNav = true
        bottomNav.requestFocus()
    }

    // --- Central D-pad dispatch ---

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        // If an EditText has focus, let all non-D-pad keys pass through
        // so the EditText/IME handles text input naturally
        val focused = currentFocus
        val editTextFocused = focused is EditText
        if (editTextFocused && !isDpadKey(keyCode) && keyCode != KeyEvent.KEYCODE_DPAD_CENTER
            && keyCode != KeyEvent.KEYCODE_ENTER && !isSpecialKey(keyCode)) {
            return super.dispatchKeyEvent(event)
        }

        // Long-press CENTER handling
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            // If EditText focused, CENTER/ENTER on Search tab should submit airport search
            if (editTextFocused && currentFragment is AirportsFragment && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                doHaptic(20)
                (currentFragment as AirportsFragment).submitAirportSearch()
                return true
            }
            if (editTextFocused && event.action == KeyEvent.ACTION_UP) return true
            return handleCenterKey(event)
        }

        // Consume both ACTION_DOWN and ACTION_UP for handled keys
        if (event.action == KeyEvent.ACTION_UP) {
            if (isDpadKey(keyCode) || isSpecialKey(keyCode)) {
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        // --- ACTION_DOWN handling ---

        if (focusInNav) {
            return handleNavKeyDown(keyCode)
        } else {
            return handleContentKeyDown(keyCode)
        }
    }

    private fun isDpadKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
    }

    private fun isSpecialKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_STAR || keyCode == KeyEvent.KEYCODE_POUND
    }

    private fun isNumberKey(keyCode: Int): Boolean {
        return keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9
    }

    // --- CENTER key with long-press ---

    private fun handleCenterKey(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    longPressFired = false
                    longPressRunnable = Runnable {
                        longPressFired = true
                        doHaptic(20)
                        onCenterLongPress()
                    }
                    longPressHandler.postDelayed(longPressRunnable!!, LONG_PRESS_THRESHOLD)
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                longPressRunnable = null
                if (!longPressFired) {
                    doHaptic(20)
                    onCenterShortPress()
                }
                return true
            }
        }
        return true
    }

    private fun onCenterShortPress() {
        if (focusInNav) {
            // Select the currently highlighted tab
            bottomNav.selectedItemId = tabIds[navSelectedIndex]
            return
        }
        // In content area: click focused item or perform default action
        val rv = getFragmentRecyclerView()
        if (rv != null && getFragmentListCount() > 0) {
            rv.findViewHolderForAdapterPosition(contentFocusedIndex)?.itemView?.performClick()
        } else {
            // Let the currently focused view handle it
            currentFocus?.performClick()
        }
    }

    private fun onCenterLongPress() {
        if (focusInNav) return
        // Long press on a list item - trigger long click
        val rv = getFragmentRecyclerView()
        if (rv != null && getFragmentListCount() > 0) {
            rv.findViewHolderForAdapterPosition(contentFocusedIndex)?.itemView?.performLongClick()
        }
    }

    // --- Nav bar key handling ---

    private fun handleNavKeyDown(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                doHaptic(10)
                focusContent()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                doHaptic(10)
                if (navSelectedIndex > 0) {
                    navSelectedIndex--
                    highlightNavItem(navSelectedIndex)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                doHaptic(10)
                if (navSelectedIndex < tabIds.size - 1) {
                    navSelectedIndex++
                    highlightNavItem(navSelectedIndex)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Already at bottom, do nothing
                return true
            }
            else -> return handleGlobalKeys(keyCode)
        }
    }

    private fun highlightNavItem(index: Int) {
        // Move focus to the menu item view at the given index
        val menuItemId = tabIds[index]
        val itemView = bottomNav.findViewById<android.view.View>(menuItemId)
        itemView?.requestFocus()
    }

    // --- Content area key handling ---

    private fun handleContentKeyDown(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                doHaptic(10)
                val count = getFragmentListCount()
                val rv = getFragmentRecyclerView()
                if (rv != null && count > 0) {
                    if (contentFocusedIndex < count - 1) {
                        contentFocusedIndex++
                        focusRecyclerItem(rv, contentFocusedIndex)
                    } else {
                        // At end of list, go to bottom nav
                        navSelectedIndex = tabIds.indexOf(bottomNav.selectedItemId).coerceAtLeast(0)
                        focusNavBar()
                        highlightNavItem(navSelectedIndex)
                    }
                } else {
                    // No list items, go to nav
                    navSelectedIndex = tabIds.indexOf(bottomNav.selectedItemId).coerceAtLeast(0)
                    focusNavBar()
                    highlightNavItem(navSelectedIndex)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                doHaptic(10)
                val rv = getFragmentRecyclerView()
                if (rv != null && contentFocusedIndex > 0) {
                    contentFocusedIndex--
                    focusRecyclerItem(rv, contentFocusedIndex)
                } else {
                    // At top of list - try search field or mode selector
                    val searchField = getFragmentSearchField()
                    if (searchField != null) {
                        searchField.requestFocus()
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // In content area, left/right do nothing by default
                return true
            }
            else -> return handleGlobalKeys(keyCode)
        }
    }

    // --- Global key handling (number keys, *, #) ---

    private fun handleGlobalKeys(keyCode: Int): Boolean {
        // Number keys 0-9: type into search field on Search/Airports tab
        if (isNumberKey(keyCode)) {
            val searchField = getFragmentSearchField()
            if (searchField != null) {
                doHaptic(10)
                val char = keyToChar(keyCode)
                if (char != null) {
                    searchField.append(char.toString())
                    searchField.requestFocus()
                }
                return true
            }
        }

        // * key: force refresh on Tracked tab, filter on Airports
        if (keyCode == KeyEvent.KEYCODE_STAR) {
            doHaptic(30)
            when (currentFragment) {
                is TrackedFragment -> (currentFragment as TrackedFragment).forceRefresh()
                is AirportsFragment -> (currentFragment as AirportsFragment).onFilterToggle()
            }
            return true
        }

        // # key: sort on Airports
        if (keyCode == KeyEvent.KEYCODE_POUND) {
            doHaptic(30)
            if (currentFragment is AirportsFragment) {
                (currentFragment as AirportsFragment).onSortToggle()
            }
            return true
        }

        return super.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }

    private fun keyToChar(keyCode: Int): Char? {
        return when (keyCode) {
            KeyEvent.KEYCODE_0 -> '0'
            KeyEvent.KEYCODE_1 -> '1'
            KeyEvent.KEYCODE_2 -> '2'
            KeyEvent.KEYCODE_3 -> '3'
            KeyEvent.KEYCODE_4 -> '4'
            KeyEvent.KEYCODE_5 -> '5'
            KeyEvent.KEYCODE_6 -> '6'
            KeyEvent.KEYCODE_7 -> '7'
            KeyEvent.KEYCODE_8 -> '8'
            KeyEvent.KEYCODE_9 -> '9'
            else -> null
        }
    }

    // --- Existing functionality ---

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("flight_id")?.let { flightId ->
            openFlightDetail(flightId)
        }
    }

    private fun openFlightDetail(flightId: String) {
        val detailIntent = Intent(this, FlightDetailActivity::class.java)
        detailIntent.putExtra(FlightDetailActivity.EXTRA_FLIGHT_ID, flightId)
        startActivity(detailIntent)
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        FlightRefreshWorker.cancel(this)
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
        private const val LONG_PRESS_THRESHOLD = 500L
    }
}
