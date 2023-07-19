package com.idunnololz.summit.util

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.CallSuper
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.R
import com.idunnololz.summit.main.MainActivity

open class BaseDialogFragment<T : ViewBinding>() : DialogFragment() {
    fun requireMainActivity(): MainActivity = requireActivity() as MainActivity
    fun getMainActivity(): MainActivity? = activity as? MainActivity

    private val logTag: String = javaClass.canonicalName ?: "UNKNOWN_CLASS"

    private var _binding: T? = null
    val binding get() = _binding!!

    private val isFullscreen: Boolean = this is FullscreenDialogFragment

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

        val dialog = dialog ?: return
        val window = checkNotNull(dialog.window)

        if (isFullscreen) {

        } else {
            window.setBackgroundDrawableResource(R.drawable.dialog_background)
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (this is BackPressHandler) {
            val dialog = object : Dialog(requireActivity(), theme) {
                override fun onBackPressed() {
                    (this@BaseDialogFragment as BackPressHandler).onBackPressed()
                }
            }

            return dialog
        }

        return super.onCreateDialog(savedInstanceState)
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val dialog = dialog
        if (dialog != null) {
            val window = checkNotNull(dialog.window)

            try {
                window.requestFeature(Window.FEATURE_NO_TITLE)
            } catch (e: Exception) {
                // do nothing
            }
        }

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

    fun addMenuProvider(menuProvider: MenuProvider) {
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}