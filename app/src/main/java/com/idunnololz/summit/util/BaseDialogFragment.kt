package com.idunnololz.summit.util

import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding

open class BaseDialogFragment<T : ViewBinding> : DialogFragment() {

    private val logTag: String = javaClass.canonicalName ?: "UNKNOWN_CLASS"

    private var _binding: T? = null
    val binding get() = _binding!!

    fun isBindingAvailable(): Boolean = _binding != null

    fun setBinding(binding: T) {
        _binding = binding
    }

    fun runOnUiThread(r: () -> Unit) {
        runOnUiThread(Runnable {
            r()
        })
    }

    fun runOnUiThread(r: Runnable) {
        if (isAdded) {
            activity?.runOnUiThread(fun() {
                val act = activity
                if (act == null || act.isFinishing) return

                try {
                    r.run()
                } catch (e: IllegalStateException) {/* do nothing */
                }
            })
        }
    }

    override fun onStart() {
        MyLog.d(logTag, "Lifecycle: onStart()")
        super.onStart()
    }

    override fun onResume() {
        MyLog.d(logTag, "Lifecycle: onResume()")
        super.onResume()
    }

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        MyLog.d(logTag, "Lifecycle: onCreateView()")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MyLog.d(logTag, "Lifecycle: onViewCreated()")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        MyLog.d(logTag, "Lifecycle: onActivityCreated()")
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroyView() {
        MyLog.d(logTag, "Lifecycle: onDestroyView()")
        super.onDestroyView()
    }

    override fun onPause() {
        MyLog.d(logTag, "Lifecycle: onPause()")
        super.onPause()
    }

    override fun onStop() {
        MyLog.d(logTag, "Lifecycle: onStop()")
        super.onStop()
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle) {
        MyLog.d(logTag, "Lifecycle: onSaveInstanceState()")
        super.onSaveInstanceState(outState)
    }
}