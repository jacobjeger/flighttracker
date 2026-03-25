package com.megalife.flighttracker.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.recyclerview.widget.RecyclerView

class DpadRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    var onBackPressed: (() -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed?.invoke()
            return true
        }

        return super.dispatchKeyEvent(event)
    }
}
