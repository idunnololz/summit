package com.idunnololz.summit.util

import android.graphics.Bitmap

internal val Bitmap.safeConfig: Bitmap.Config
    get() = config ?: Bitmap.Config.ARGB_8888