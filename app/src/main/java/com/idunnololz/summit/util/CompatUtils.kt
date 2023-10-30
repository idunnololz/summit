package com.idunnololz.summit.util

import android.os.Build

fun isPredictiveBackSupported() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
