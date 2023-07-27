package com.idunnololz.summit.util

import com.squareup.moshi.Moshi

val moshi: Moshi by lazy {
    Moshi.Builder()
        .build()
}
