package com.idunnololz.summit.util

import android.content.Context
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.main.ActivityInsets

interface BottomMenuContainer : LifecycleOwner, OnBackPressedDispatcherOwner, InsetsProvider {
    val context: Context
    val mainApplication: MainApplication

    fun showBottomMenu(bottomMenu: BottomMenu, expandFully: Boolean = true)
}
