package com.idunnololz.summit.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.main.ActivityInsets
import kotlin.math.max

interface InsetsProvider {
    val insets: LiveData<ActivityInsets>
    var onInsetsChanged: ((ActivityInsets) -> Unit)?
    fun registerInsetsHandler(rootView: View)
}

class InsetsHelper(
    private val consumeInsets: Boolean,
) : InsetsProvider {

    private val _insets = MutableLiveData<ActivityInsets>()

    override val insets: LiveData<ActivityInsets>
        get() = _insets
    override var onInsetsChanged: ((ActivityInsets) -> Unit)? = null

    override fun registerInsetsHandler(rootView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBarsInsets = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars(),
            )
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()) // keyboard
            val imeHeight = imeInsets.bottom
            val topInset = systemBarsInsets.top
            val bottomInset = max(systemBarsInsets.bottom, imeHeight)
            val leftInset = 0
            val rightInset = 0

            val mainLeftInset = systemBarsInsets.left
            val mainRightInset = systemBarsInsets.right

            val newInsets = ActivityInsets(
                imeHeight = imeHeight,
                topInset = topInset,
                bottomInset = bottomInset,
                leftInset = leftInset,
                rightInset = rightInset,
                mainLeftInset = mainLeftInset,
                mainRightInset = mainRightInset,
            )

            onInsetsChanged?.invoke(newInsets)

            _insets.value = newInsets

            if (consumeInsets) {
                WindowInsetsCompat.CONSUMED
            } else {
                insets
            }
        }
    }
}
