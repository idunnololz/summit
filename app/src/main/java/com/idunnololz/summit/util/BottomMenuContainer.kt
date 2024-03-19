package com.idunnololz.summit.util

import android.content.Context
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.main.MainActivityInsets

interface BottomMenuContainer : LifecycleOwner, OnBackPressedDispatcherOwner {
    val context: Context
    val mainApplication: MainApplication
    val lastInsetLiveData: MutableLiveData<MainActivityInsets>

    fun showBottomMenu(bottomMenu: BottomMenu, expandFully: Boolean = true)
}