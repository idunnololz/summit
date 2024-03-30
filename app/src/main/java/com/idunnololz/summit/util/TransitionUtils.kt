package com.idunnololz.summit.util

import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionSet

fun makeTransition() =
    TransitionSet()
        .addTransition(Fade(Fade.IN or Fade.OUT))
        .addTransition(ChangeBounds())
        .addTransition(ChangeClipBounds())
        .addTransition(ChangeImageTransform())
        .setOrdering(TransitionSet.ORDERING_TOGETHER)
        .setDuration(Utils.ANIMATION_DURATION_MS)