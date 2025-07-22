package com.idunnololz.summit.preferences

typealias FontId = Int

object FontIds {
  const val DEFAULT: FontId = 0
  const val ROBOTO: FontId = 1
  const val ROBOTO_SERIF: FontId = 2
  const val OPEN_SANS: FontId = 3
  const val ATKINSON_HYPERLEGIBLE_NEXT: FontId = 4
}

fun FontId.toFontAsset(): String? = when (this) {
  FontIds.ROBOTO -> "fonts/Roboto-Regular.ttf"
  FontIds.ROBOTO_SERIF -> "fonts/RobotoSerif-Regular.ttf"
  FontIds.OPEN_SANS -> "fonts/OpenSans-Regular.ttf"
  FontIds.ATKINSON_HYPERLEGIBLE_NEXT -> "fonts/AtkinsonHyperlegibleNext-Regular.otf"
  else -> null
}
