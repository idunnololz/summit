package com.idunnololz.summit.util.ext

import android.app.Activity
import android.os.Build
import android.view.WindowManager

fun Activity.showAboveCutout() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}
