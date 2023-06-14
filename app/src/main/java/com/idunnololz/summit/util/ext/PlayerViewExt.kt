package com.idunnololz.summit.util.ext

import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@UnstableApi
fun PlayerView.setup() {
    controllerShowTimeoutMs = 2500
}