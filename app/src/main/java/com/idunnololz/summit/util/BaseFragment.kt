package com.idunnololz.summit.util

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.CallSuper
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.ext.runAfterLayout

abstract class BaseFragment<T : ViewBinding> : Fragment() {

    private val logTag: String = if (BuildConfig.DEBUG) {
        javaClass.simpleName ?: "UNKNOWN_CLASS"
    } else {
        javaClass.canonicalName ?: "UNKNOWN_CLASS"
    }

    fun requireMainActivity(): MainActivity = requireActivity() as MainActivity
    fun getMainActivity(): MainActivity? = activity as? MainActivity

    private var _binding: T? = null
    val binding get() = _binding!!

    fun isBindingAvailable(): Boolean = _binding != null

    fun setBinding(binding: T) {
        _binding = binding
    }

    fun runOnReady(cb: () -> Unit) {
        requireMainActivity().runOnReady(viewLifecycleOwner) {
            if (isBindingAvailable()) {
                cb()
            }
        }
    }

    protected fun scheduleStartPostponedTransition(
        view: View,
        onStartPostponedEnterTransition: (() -> Unit)? = null,
    ) {
        view.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                    startPostponedEnterTransition()
                    onStartPostponedEnterTransition?.invoke()
                    return true
                }
            },
        )
    }

    fun runAfterLayout(callback: () -> Unit) {
        if (!isBindingAvailable()) return

        binding.root.runAfterLayout(callback)
    }

    fun addMenuProvider2(menuProvider: MenuProvider) {
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MyLog.d(logTag, "Lifecycle: onCreate()")
        super.onCreate(savedInstanceState)
    }

    @CallSuper
    override fun onStart() {
        MyLog.d(logTag, "Lifecycle: onStart()")
        super.onStart()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()

        Log.d(this::class.simpleName, "onResume()")
    }

    @CallSuper
    override fun onDestroyView() {
        MyLog.d(logTag, "Lifecycle: onDestroyView()")
        super.onDestroyView()

        _binding = null
    }

    @CallSuper
    override fun onDestroy() {
        MyLog.d(logTag, "Lifecycle: onDestroy()")
        super.onDestroy()
    }
}
