package com.idunnololz.summit.preferences

import kotlinx.serialization.SerialName

enum class BaseTheme {
    @SerialName("use_system")
    UseSystem,

    @SerialName("light")
    Light,

    @SerialName("dark")
    Dark,
}
