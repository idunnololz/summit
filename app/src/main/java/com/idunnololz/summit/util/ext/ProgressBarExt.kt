package com.idunnololz.summit.util.ext

import android.os.Build
import android.widget.ProgressBar

fun ProgressBar.setProgressCompat(progress: Int, animate: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        setProgress(progress, animate)
    } else {
        setProgress(progress)
    }
}