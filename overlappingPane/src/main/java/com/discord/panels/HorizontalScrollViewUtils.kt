package com.discord.panels

import android.widget.HorizontalScrollView

val HorizontalScrollView.isScrollable: Boolean
    get() {
        if (childCount == 0) {
            return false
        }
        val contentWidth = getChildAt(0).width + paddingTop + paddingBottom

        return width < contentWidth
    }