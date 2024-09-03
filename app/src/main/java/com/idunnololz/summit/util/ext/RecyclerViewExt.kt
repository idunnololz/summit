package com.idunnololz.summit.util.ext

import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.util.AnimationsHelper

fun RecyclerView.clearItemDecorations() {
    while (itemDecorationCount > 0) {
        removeItemDecorationAt(0)
    }
}

fun RecyclerView.setup(animationsHelper: AnimationsHelper) {
    if (!animationsHelper.shouldAnimate(AnimationsHelper.AnimationLevel.Polish)) {
        itemAnimator = null
    }
}