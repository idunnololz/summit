package com.idunnololz.summit.util.ext

import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.util.Utils
import java.lang.IllegalStateException

fun DialogFragment.showAllowingStateLoss(fm: FragmentManager, tag: String) {
    try {
        show(fm, tag)
    } catch (e: IllegalStateException) {
        // do nothing
    }
}

fun DialogFragment.setSizeDynamically(width: Int, height: Int) {
    val dialog = dialog
    val isFullScreen = width == ViewGroup.LayoutParams.MATCH_PARENT &&
        height == ViewGroup.LayoutParams.MATCH_PARENT

    if (dialog != null) {
        val window = checkNotNull(dialog.window)

        if (isFullScreen) {
            window.setLayout(width, height)
            return
        }

        val w = Utils.getScreenWidth(requireContext())
        val maxW = Utils.convertDpToPixel(600f)
        if (w > maxW) {
            window.setLayout(maxW.toInt(), height)
        } else {
            window.setLayout(width, height)
        }
    }
}