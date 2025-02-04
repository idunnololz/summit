package com.idunnololz.summit.alert

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.alert.AlertDialogFragment.Builder
import com.idunnololz.summit.alert.AlertDialogFragment.Launcher
import com.idunnololz.summit.alert.AlertDialogFragment.Result
import com.idunnololz.summit.util.getParcelableCompat
import kotlinx.parcelize.Parcelize

class AlertDialogFragment : DialogFragment() {

    companion object {

        private const val EXTRA_REQUEST_KEY = "EXTRA_REQUEST_KEY"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ICON = "imgIcon"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_CANCELABLE = "cancelable"

        private const val EXTRA_POSITIVE_TEXT = "positive_text"
        private const val EXTRA_NEGATIVE_TEXT = "negative_text"

        private const val EXTRA_EXTRAS = "EXTRA_EXTRAS"

        private const val KEY_RESULT = "result"
    }

    class Launcher(
        private val tag: String,
        private val fragmentManagerProvider: () -> FragmentManager,
        private val handleResult: (Result) -> Unit,
    ) {
        private val reqKey: String = tag + "_req"

        fun register(fragment: Fragment) {
            fragment.childFragmentManager
                .setFragmentResultListener(reqKey, fragment) { _, bundle: Bundle ->
                    val result = bundle.getParcelableCompat<Result>(KEY_RESULT)

                    if (result != null) {
                        handleResult(result)
                    }
                }
        }

        fun launchDialog(builder: Builder.() -> Unit) {
            Builder()
                .apply {
                    builder()
                    requestKey = reqKey
                }
                .createAndShow(fragmentManagerProvider(), tag)
        }
    }

    sealed interface Result : Parcelable {

        val extras: Bundle?

        @Parcelize
        data class Positive(
            override val extras: Bundle?
        ) : Result

        @Parcelize
        data class Negative(
            override val extras: Bundle?
        ) : Result

        @Parcelize
        data class Neutral(
            override val extras: Bundle?
        ) : Result

        val isOk: Boolean
            get() = this is Positive
    }

    private lateinit var parent: AlertDialogFragmentListener

    class Builder {
        private val args = Bundle()

        var requestKey: String = ""
        var title: String = ""
        @StringRes var titleResId: Int = 0
        var message: String = ""
        @StringRes var messageResId: Int = 0
        @StringRes var positionButtonResId: Int = 0
        @StringRes var negativeButtonResId: Int = 0
        var cancelable = true

        val extras = Bundle()

        fun create(): AlertDialogFragment {
            val frag = AlertDialogFragment()
            args.apply {
                putString(EXTRA_REQUEST_KEY, requestKey)
                if (title.isNotBlank()) {
                    putString(EXTRA_TITLE, title)
                } else if (titleResId != 0) {
                    putInt(EXTRA_TITLE, titleResId)
                }
                if (message.isNotBlank()) {
                    putString(EXTRA_MESSAGE, message)
                } else if (messageResId != 0) {
                    putInt(EXTRA_MESSAGE, messageResId)
                }

                if (positionButtonResId != 0) {
                    putInt(EXTRA_POSITIVE_TEXT, positionButtonResId)
                }
                if (negativeButtonResId != 0) {
                    putInt(EXTRA_NEGATIVE_TEXT, negativeButtonResId)
                }
                putBoolean(EXTRA_CANCELABLE, cancelable)
                putBundle(EXTRA_EXTRAS, extras)
            }
            frag.arguments = args
            return frag
        }

        fun create(parent: Fragment): AlertDialogFragment {
            val frag = create()
            frag.setTargetFragment(parent, 0)
            return frag
        }

        fun createAndShow(fm: FragmentManager?, tag: String) {
            fm ?: return

            val frag = create()
            fm.beginTransaction()
                .add(frag, tag)
                .commitAllowingStateLoss()
        }

        fun createAndShow(parent: Fragment, tag: String) {
            val frag = create(parent)
            parent.parentFragmentManager
                .beginTransaction()
                .add(frag, tag)
                .commitAllowingStateLoss()
        }
    }

    fun getExtra(key: String): String? {
        return requireArguments().getString(key)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity ?: throw RuntimeException("Activity is null")

        val args = arguments
        val title = args!!.getInt(EXTRA_TITLE)
        val icon = args.getInt(EXTRA_ICON)
        val message = args.get(EXTRA_MESSAGE)

        var positiveTextId = args.getInt(EXTRA_POSITIVE_TEXT, 0)
        val negativeTextId = args.getInt(EXTRA_NEGATIVE_TEXT, 0)
        val reqKey = args.getString(EXTRA_REQUEST_KEY, "")

        val builder = MaterialAlertDialogBuilder(activity).setIcon(icon)

        if (title != 0) {
            builder.setTitle(title)
        }

        if (!args.getBoolean(EXTRA_CANCELABLE, true)) {
            builder.setCancelable(false)
            isCancelable = false
        }

        if (message == null) {
        } else if (message is Int) {
            builder.setMessage(message)
        } else {
            builder.setMessage(message as CharSequence)
        }

        if (positiveTextId == 0) {
            positiveTextId = android.R.string.ok
        }

        val f = targetFragment
        val ff = parentFragment
        if (f != null) {
            parent = f as AlertDialogFragmentListener
        } else if (ff != null && ff is AlertDialogFragmentListener) {
            parent = ff
        } else if (activity is AlertDialogFragmentListener) {
            parent = activity
        } else {
            parent = object : AlertDialogFragmentListener {
                override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
                    dismiss()
                }

                override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
                    try {
                        dismiss()
                    } catch (e: IllegalStateException) {
                        // do nothing
                    }
                }
            }
        }

        val dialog = this
        builder.setPositiveButton(positiveTextId) { _, _ ->
            setFragmentResult(reqKey, Bundle().apply {
                putParcelable(KEY_RESULT, Result.Positive(
                    args.getBundle(EXTRA_EXTRAS)
                ))
            })
            parent.onPositiveClick(dialog, tag)
        }

        if (negativeTextId != 0) {
            builder.setNegativeButton(negativeTextId) { _, _ ->
                setFragmentResult(reqKey, Bundle().apply {
                    putParcelable(KEY_RESULT, Result.Negative(
                        args.getBundle(EXTRA_EXTRAS)
                    ))
                })
                parent.onNegativeClick(dialog, tag)
            }
        }

        return builder.create()
    }

    interface AlertDialogFragmentListener {
        fun onPositiveClick(dialog: AlertDialogFragment, tag: String?)
        fun onNegativeClick(dialog: AlertDialogFragment, tag: String?)
    }
}

fun Fragment.newAlertDialogLauncher(tag: String, handleResult: (Result) -> Unit): Launcher {
    val launcher = Launcher(
        tag = tag,
        fragmentManagerProvider = { childFragmentManager },
        handleResult = handleResult
    )

    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            launcher.register(this@newAlertDialogLauncher)
        }
    })

    return launcher
}

fun Fragment.launchAlertDialog(tag: String, builder: Builder.() -> Unit) {
    Builder()
        .apply {
            builder()
            requestKey = tag + "_req"
        }
        .createAndShow(childFragmentManager, tag)
}