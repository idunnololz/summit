package com.idunnololz.summit.lemmy.utils

import android.util.LayoutDirection
import android.view.Gravity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.idunnololz.summit.preferences.Preferences

fun FloatingActionButton.setup(preferences: Preferences) {
    this.updateLayoutParams<CoordinatorLayout.LayoutParams> {
        if (preferences.leftHandMode && this.gravity and Gravity.END != 0) {
            this.gravity = this.gravity and Gravity.END.inv() or Gravity.START
        } else if (!preferences.leftHandMode && this.gravity and Gravity.START != 0) {
            this.gravity = this.gravity and Gravity.START.inv() or Gravity.END
        }
    }
}