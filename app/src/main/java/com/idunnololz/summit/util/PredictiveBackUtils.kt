package com.idunnololz.summit.util

import android.content.Context
import android.util.Log
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import com.idunnololz.summit.R
import com.idunnololz.summit.util.BaseDialogFragment.Companion.gestureInterpolator
import kotlin.math.max

fun newBottomSheetPredictiveBackBackPressHandler(
    context: Context,
    getBottomSheetView: () -> View?,
    onBackPress: () -> Unit,
): OnBackPressedCallback {
    val predictiveBackMargin = context.resources.getDimensionPixelSize(R.dimen.predictive_back_margin)
    var initialTouchY = -1f

    return object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // This invokes the sharedElementReturnTransition, which is
            // MaterialContainerTransform.
            onBackPress()
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            val background = getBottomSheetView() ?: return
            val progress = gestureInterpolator.getInterpolation(backEvent.progress)
            if (initialTouchY < 0f) {
                initialTouchY = backEvent.touchY
            }
            val progressY = gestureInterpolator.getInterpolation(
                (backEvent.touchY - initialTouchY) / background.height
            )

            // See the motion spec about the calculations below.
            // https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs

            // Shift horizontally.
            val maxTranslationX = (background.width / 20) - predictiveBackMargin
            background.translationX = progress * maxTranslationX *
                (if (backEvent.swipeEdge == androidx.activity.BackEventCompat.EDGE_LEFT) 1 else -1)

            // Shift vertically.
            val maxTranslationY = (background.height / 20) - predictiveBackMargin
            background.translationY = max(
                progressY * maxTranslationY,
                background.height * (0.1f * progress)
            )

            // Scale down from 100% to 90%.
            val scale = 1f - (0.1f * progress)
            background.scaleX = scale
            background.scaleY = scale
        }

        override fun handleOnBackCancelled() {
            val background = getBottomSheetView() ?: return
            initialTouchY = -1f
            background.run {
                translationX = 0f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
            }
        }
    }
}