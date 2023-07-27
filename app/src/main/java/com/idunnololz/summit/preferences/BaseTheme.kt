package com.idunnololz.summit.preferences

import com.squareup.moshi.Json

enum class BaseTheme {
    @Json(name = "use_system")
    UseSystem,

    @Json(name = "light")
    Light,

    @Json(name = "dark")
    Dark,
}
