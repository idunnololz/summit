package com.idunnololz.summit.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner

fun InsetsProvider.insetViewAutomaticallyByMargins(lifecycleOwner: LifecycleOwner, rootView: View) {
    insets.observe(lifecycleOwner) {
        val lp = rootView.layoutParams as ViewGroup.MarginLayoutParams

        lp.topMargin = it.topInset
        lp.bottomMargin = it.bottomInset
        lp.leftMargin = it.leftInset
        lp.rightMargin = it.rightInset
        rootView.requestLayout()
    }
}

fun InsetsProvider.insetViewExceptBottomAutomaticallyByMargins(
    lifecycleOwner: LifecycleOwner,
    view: View,
) {
    insets.observe(lifecycleOwner) {
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = it.topInset
            leftMargin = it.leftInset
            rightMargin = it.rightInset
        }
    }
}

fun InsetsProvider.insetViewExceptTopAutomaticallyByPadding(
    lifecycleOwner: LifecycleOwner,
    rootView: View,
    additionalPaddingBottom: Int = 0,
) {
    insets.observe(lifecycleOwner) {
        val insets = it

        rootView.setPadding(
            insets.leftInset,
            0,
            insets.rightInset,
            insets.bottomInset + additionalPaddingBottom,
        )
    }
}

fun InsetsProvider.insetViewExceptBottomAutomaticallyByPadding(
    lifecycleOwner: LifecycleOwner,
    rootView: View,
) {
    insets.observe(lifecycleOwner) {
        rootView.setPadding(
            it.leftInset,
            it.topInset,
            it.rightInset,
            0,
        )
    }
}

fun InsetsProvider.insetViewExceptTopAutomaticallyByMargins(
    lifecycleOwner: LifecycleOwner,
    rootView: View,
) {
    insets.observe(lifecycleOwner) {
        rootView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = it.bottomInset
            leftMargin = it.leftInset
            rightMargin = it.rightInset
        }
    }
}

fun InsetsProvider.insetViewStartAndEndByPadding(lifecycleOwner: LifecycleOwner, rootView: View) {
    insets.observe(lifecycleOwner) { insets ->
        rootView.setPadding(
            insets.leftInset,
            0,
            insets.rightInset,
            0,
        )
    }
}

fun InsetsProvider.insetViewAutomaticallyByPadding(
    lifecycleOwner: LifecycleOwner,
    rootView: View,
    additionalPaddingBottom: Int = 0,
) {
    insets.observe(lifecycleOwner) { insets ->
        rootView.setPadding(
            insets.leftInset,
            insets.topInset,
            insets.rightInset,
            insets.bottomInset + additionalPaddingBottom,
        )
    }
}
