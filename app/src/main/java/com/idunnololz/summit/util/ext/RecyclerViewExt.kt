package com.idunnololz.summit.util.ext

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.clearItemDecorations() {
    while (itemDecorationCount > 0) {
        removeItemDecorationAt(0)
    }
}
