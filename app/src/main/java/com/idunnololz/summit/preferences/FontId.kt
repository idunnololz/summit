package com.idunnololz.summit.preferences

typealias FontId = Int

object FontIds {
    const val Default: FontId = 0
    const val Roboto: FontId = 1
    const val RobotoSerif: FontId = 2
    const val OpenSans: FontId = 3
}

fun FontId.toFontAsset(): String? =
    when (this) {
        FontIds.Roboto -> "fonts/Roboto-Regular.ttf"
        FontIds.RobotoSerif -> "fonts/RobotoSerif-Regular.ttf"
        FontIds.OpenSans -> "fonts/OpenSans-Regular.ttf"
        else -> null
    }
