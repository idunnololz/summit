package com.idunnololz.summit.util

import android.view.KeyEvent
import androidx.core.view.MenuHostHelper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.CopyOnWriteArrayList

class KeyPressRegistrationManager {

    interface OnKeyPressHandler {
        fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
    }

    private val registrations: CopyOnWriteArrayList<OnKeyPressHandler> = CopyOnWriteArrayList()

    fun register(owner: LifecycleOwner, onKeyPressHandler: OnKeyPressHandler) {
        val lifecycle: Lifecycle = owner.lifecycle
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return
        }

        owner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    unregister(onKeyPressHandler)
                }
            },
        )
        registrations.add(onKeyPressHandler)
    }

    private fun unregister(onKeyPressHandler: OnKeyPressHandler) {
        registrations.remove(onKeyPressHandler)
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val iterator = registrations.iterator()
        while (iterator.hasNext()) {
            val callback = iterator.next()
            if (callback.onKeyDown(keyCode, event)) {
                return true
            }
        }

        return false
    }
}