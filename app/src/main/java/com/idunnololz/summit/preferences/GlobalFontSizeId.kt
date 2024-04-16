package com.idunnololz.summit.preferences

object GlobalFontSizeId {
    const val Normal = 0

    const val Small = 1

    const val Large = 2

    const val ExtraLarge = 3

    const val Xxl = 5

    const val Xxxl = 6

    fun getFontSizeMultiplier(fontSize: Int) = when (fontSize) {
        Small -> 0.9f
        Normal -> 1f
        Large -> 1.143f
        ExtraLarge -> 1.22f
        Xxl -> 1.5f
        Xxxl -> 1.8f
        else -> 1f
    }
}
