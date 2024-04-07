package com.idunnololz.summit.util.ext

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import androidx.core.util.forEach
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

fun detachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment,
) {
    fragmentManager.beginTransaction()
        .detach(navHostFragment)
        .commitNow()
}

fun attachNavHostFragment(
    fragmentManager: FragmentManager,
    navHostFragment: NavHostFragment,
    isPrimaryNavFragment: Boolean,
) {
    fragmentManager.beginTransaction()
        .attach(navHostFragment)
        .apply {
            if (isPrimaryNavFragment) {
                setPrimaryNavigationFragment(navHostFragment)
            }
        }
        .commitNow()
}

fun removeNavHostFragment(
    fragmentManager: FragmentManager,
    fragmentTag: String,
) {
    val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?

    existingFragment ?: return

    fragmentManager.commitNow {
        detach(existingFragment)
        remove(existingFragment)
    }
}

fun obtainNavHostFragment(
    fragmentManager: FragmentManager,
    fragmentTag: String,
    navGraphId: Int,
    containerId: Int,
    startDestinationArgs: Bundle? = null,
): NavHostFragment {
    // If the Nav Host fragment exists, return it
    val existingFragment = fragmentManager.findFragmentByTag(fragmentTag) as NavHostFragment?
    existingFragment?.let { return it }

    // Otherwise, create it and return it.
    val navHostFragment = NavHostFragment.create(navGraphId, startDestinationArgs)
    fragmentManager.beginTransaction()
        .add(containerId, navHostFragment, fragmentTag)
        .commitNow()
    return navHostFragment
}

private fun FragmentManager.isOnBackStack(backStackName: String): Boolean {
    val backStackCount = backStackEntryCount
    for (index in 0 until backStackCount) {
        if (getBackStackEntryAt(index).name == backStackName) {
            return true
        }
    }
    return false
}

private fun getFragmentTag(index: Int) = "bottomNavigation#$index"
