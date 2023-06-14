package com.idunnololz.summit.util.ext

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import java.lang.IllegalStateException

fun DialogFragment.showAllowingStateLoss(fm: FragmentManager, tag: String) {
    try {
        show(fm, tag)
    } catch (e: IllegalStateException) {
        // do nothing
    }
}