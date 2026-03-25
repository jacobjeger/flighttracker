package com.megalife.flighttracker.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.HapticFeedbackConstants
import com.google.android.material.bottomnavigation.BottomNavigationView

class DpadBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    var onUpFromNav: (() -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)

        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                selectPreviousTab()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                selectNextTab()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                onUpFromNav?.invoke()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // Already selected, enter content
                onUpFromNav?.invoke()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun selectPreviousTab() {
        val menu = menu
        val currentIndex = getSelectedIndex()
        val newIndex = if (currentIndex > 0) currentIndex - 1 else menu.size() - 1
        selectedItemId = menu.getItem(newIndex).itemId
    }

    private fun selectNextTab() {
        val menu = menu
        val currentIndex = getSelectedIndex()
        val newIndex = if (currentIndex < menu.size() - 1) currentIndex + 1 else 0
        selectedItemId = menu.getItem(newIndex).itemId
    }

    private fun getSelectedIndex(): Int {
        val menu = menu
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == selectedItemId) return i
        }
        return 0
    }
}
