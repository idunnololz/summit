package com.idunnololz.summit.util.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.tabs.TabLayoutMediator

fun TabLayoutMediator.attachWithAutoDetachUsingLifecycle(
    lifecycleOwner: LifecycleOwner,
): TabLayoutMediator {
    lifecycleOwner.lifecycle.addObserver(
        object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    detach()
                    source.lifecycle.removeObserver(this)
                }
            }
        },
    )
    attach()
    return this
}
