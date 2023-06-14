package com.idunnololz.summit.util

import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.main.MainActivity


abstract class BaseFragment<T : ViewBinding> : Fragment() {
    fun requireMainActivity(): MainActivity = requireActivity() as MainActivity

    private var _binding: T? = null
    val binding get() = _binding!!

    fun isBindingAvailable(): Boolean = _binding != null

    fun setBinding(binding: T) {
        _binding = binding
    }

    protected fun scheduleStartPostponedTransition(
        view: View,
        onStartPostponedEnterTransition: (() -> Unit)? = null
    ) {
        view.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                    startPostponedEnterTransition()
                    onStartPostponedEnterTransition?.invoke()
                    return true
                }
            })
    }

    override fun onResume() {
        super.onResume()

        Log.d(this::class.simpleName, "onResume()")
    }
}