package com.idunnololz.summit.util

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.slidingpanelayout.widget.SlidingPaneLayout

class TwoPaneOnBackPressedCallback(
    private val slidingPaneLayout: SlidingPaneLayout,
) : OnBackPressedCallback(
    // Set the default 'enabled' state to true only if it is slidable, such as
    // when the panes overlap, and open, such as when the detail pane is
    // visible.
    slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen,
),
    SlidingPaneLayout.PanelSlideListener {

    init {
        slidingPaneLayout.addPanelSlideListener(this)

        slidingPaneLayout.doOnLayout {
            if (slidingPaneLayout.isOpen) {
                onPanelOpened(slidingPaneLayout)
            } else {
                onPanelClosed(slidingPaneLayout)
            }
        }
    }

    override fun handleOnBackPressed() {
        // Return to the list pane when the system back button is tapped.
        slidingPaneLayout.closePane()
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) { }

    override fun onPanelOpened(panel: View) {
        // Intercept the system back button when the detail pane becomes
        // visible.
        if (slidingPaneLayout.isSlideable) {
            isEnabled = true
        }
    }

    override fun onPanelClosed(panel: View) {
        // Disable intercepting the system back button when the user returns to
        // the list pane.
        isEnabled = false
    }
}
