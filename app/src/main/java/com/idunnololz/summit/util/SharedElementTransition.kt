package com.idunnololz.summit.util

import androidx.transition.*

class SharedElementTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER
        addTransition(ChangeBounds())
            .addTransition(ChangeTransform())
            .addTransition(ChangeImageTransform())
            .addTransition(ChangeClipBounds())
            .addTransition(TextSizeTransition())
            .setDuration(300)
    }
}