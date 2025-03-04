package com.idunnololz.summit.util

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.BackEventCompat
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.CallSuper
import androidx.core.view.MenuProvider
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.R
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.BaseDialogFragment.Companion.gestureInterpolator

open class BaseDialogFragment<T : ViewBinding>() : DialogFragment() {

    companion object {
        val gestureInterpolator = PathInterpolatorCompat
            .create(0f, 0f, 0f, 1f)
    }

    fun requireMainActivity(): MainActivity = requireActivity() as MainActivity
    fun getMainActivity(): MainActivity? = activity as? MainActivity

    private val logTag: String = javaClass.canonicalName ?: "UNKNOWN_CLASS"

    private var _binding: T? = null
    val binding get() = _binding!!

    private val isFullscreen: Boolean = this is FullscreenDialogFragment

    private var _onBackPressedDispatcher: OnBackPressedDispatcher? = null
    val onBackPressedDispatcher get() = _onBackPressedDispatcher!!

    fun isBindingAvailable(): Boolean = _binding != null

    fun setBinding(binding: T) {
        _binding = binding
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
        return super.onCreateDialog(savedInstanceState).also {
            _onBackPressedDispatcher = (it as ComponentDialog).onBackPressedDispatcher
        }
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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

        onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            getPredictiveBackBackPressCallback(),
        )
    }

    open fun getPredictiveBackBackPressCallback(): OnBackPressedCallback {
        val predictiveBackMargin = resources.getDimensionPixelSize(R.dimen.predictive_back_margin)
        var initialTouchY = -1f

        return object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // This invokes the sharedElementReturnTransition, which is
                // MaterialContainerTransform.
                dismiss()
            }

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                super.handleOnBackStarted(backEvent)

                val background = dialog?.window?.decorView ?: return
                val focus = background.findFocus()

                focus?.clearFocus()
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                val background = dialog?.window?.decorView ?: return
                val progress = gestureInterpolator.getInterpolation(backEvent.progress)
                if (initialTouchY < 0f) {
                    initialTouchY = backEvent.touchY
                }
                val progressY = gestureInterpolator.getInterpolation(
                    (backEvent.touchY - initialTouchY) / background.height,
                )

                // See the motion spec about the calculations below.
                // https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs

                // Shift horizontally.
                val maxTranslationX = (background.width / 20) - predictiveBackMargin
                background.translationX = progress * maxTranslationX *
                    (if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1)

                // Shift vertically.
                val maxTranslationY = (background.height / 20) - predictiveBackMargin
                background.translationY = progressY * maxTranslationY

                // Scale down from 100% to 90%.
                val scale = 1f - (0.1f * progress)
                background.scaleX = scale
                background.scaleY = scale
            }

            override fun handleOnBackCancelled() {
                val background = dialog?.window?.decorView ?: return
                initialTouchY = -1f
                background.run {
                    translationX = 0f
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
            }
        }
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

fun BaseDialogFragment<*>.newPredictiveBackBackPressHandler(
    getBottomMenu: () -> BottomMenu?,
): OnBackPressedCallback = newBottomSheetPredictiveBackBackPressHandler(
    requireContext(),
    { getBottomMenu()?.bottomSheetView },
) {
    dismiss()
}
