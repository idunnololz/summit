package com.idunnololz.summit.util.ext

import android.annotation.SuppressLint
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@SuppressLint("UnsafeOptInUsageError")
fun PlayerView.setup() {
    controllerShowTimeoutMs = 2500
}