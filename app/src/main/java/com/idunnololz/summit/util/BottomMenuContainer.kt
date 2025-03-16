package com.idunnololz.summit.util

import android.content.Context
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.MainApplication

interface BottomMenuContainer : LifecycleOwner, OnBackPressedDispatcherOwner, InsetsProvider {
    val context: Context
    val mainApplication: MainApplication
    val activity: BaseActivity?

    fun showBottomMenu(bottomMenu: BottomMenu, expandFully: Boolean = true)
}
