package com.idunnololz.summit.util.ext

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

fun FragmentManager.getCurrentNavigationFragment(): Fragment? =
    primaryNavigationFragment?.childFragmentManager?.fragments?.firstOrNull()