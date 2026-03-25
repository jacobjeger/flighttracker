package com.megalife.flighttracker.util

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

object DpadUtils {

    const val LONG_PRESS_THRESHOLD = 500L

    fun performHaptic(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun postFocus(view: View) {
        Handler(Looper.getMainLooper()).post {
            view.requestFocus()
        }
    }

    fun focusFirstChild(parent: ViewGroup) {
        Handler(Looper.getMainLooper()).post {
            if (parent is RecyclerView) {
                val firstChild = parent.getChildAt(0)
                firstChild?.requestFocus()
            } else {
                val firstFocusable = parent.focusSearch(View.FOCUS_DOWN)
                firstFocusable?.requestFocus()
            }
        }
    }

    fun isNumberKey(keyCode: Int): Boolean {
        return keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9
    }

    fun keyToChar(keyCode: Int): Char? {
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
            KeyEvent.KEYCODE_STAR -> '*'
            KeyEvent.KEYCODE_POUND -> '#'
            else -> null
        }
    }

    // T9-style mapping for text input
    fun getT9Chars(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_2 -> "ABC2"
            KeyEvent.KEYCODE_3 -> "DEF3"
            KeyEvent.KEYCODE_4 -> "GHI4"
            KeyEvent.KEYCODE_5 -> "JKL5"
            KeyEvent.KEYCODE_6 -> "MNO6"
            KeyEvent.KEYCODE_7 -> "PQRS7"
            KeyEvent.KEYCODE_8 -> "TUV8"
            KeyEvent.KEYCODE_9 -> "WXYZ9"
            KeyEvent.KEYCODE_0 -> " 0"
            KeyEvent.KEYCODE_1 -> "1"
            else -> ""
        }
    }
}
