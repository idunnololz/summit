package com.idunnololz.summit.preferences

object GlobalFontSizeId {
    const val Normal = 0

    const val Small = 1

    const val Large = 2

    const val ExtraLarge = 3

    fun getFontSizeMultiplier(fontSize: Int) =
        when (fontSize) {
            GlobalFontSizeId.Small -> 0.9f
            GlobalFontSizeId.Normal -> 1f
            GlobalFontSizeId.Large -> 1.143f
            GlobalFontSizeId.ExtraLarge -> 1.22f
            else -> 1f
        }
}
