package com.idunnololz.summit.alert

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.idunnololz.summit.R

class AlertDialogFragment : DialogFragment() {

    companion object {

        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ICON = "imgIcon"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_CANCELABLE = "cancelable"

        private const val EXTRA_POSITIVE_TEXT = "positive_text"
        private const val EXTRA_NEGATIVE_TEXT = "negative_text"
    }

    private lateinit var parent: AlertDialogFragmentListener

    class Builder {
        private val args = Bundle()

        fun setTitle(titleId: Int): Builder {
            args.putInt(EXTRA_TITLE, titleId)
            return this
        }

        fun setMessage(messageId: Int): Builder {
            args.putInt(EXTRA_MESSAGE, messageId)
            return this
        }

        fun setMessage(message: CharSequence?): Builder {
            args.putCharSequence(EXTRA_MESSAGE, message)
            return this
        }

        fun setPositiveButton(textId: Int): Builder {
            args.putInt(EXTRA_POSITIVE_TEXT, textId)
            return this
        }

        fun setNegativeButton(textId: Int): Builder {
            args.putInt(EXTRA_NEGATIVE_TEXT, textId)
            return this
        }

        fun setCancelable(b: Boolean): Builder {
            args.putBoolean(EXTRA_CANCELABLE, b)
            return this
        }

        fun setExtra(key: String, extra: String): Builder {
            args.putString(key, extra)
            return this
        }

        fun extras(fn: Bundle.() -> Unit): Builder {
            args.fn()
            return this
        }

        fun create(): AlertDialogFragment {
            val frag = AlertDialogFragment()
            frag.arguments = args
            return frag
        }

        fun create(parent: androidx.fragment.app.Fragment): AlertDialogFragment {
            val frag = create()
            frag.setTargetFragment(parent, 0)
            return frag
        }

        fun createAndShow(fm: androidx.fragment.app.FragmentManager?, tag: String) {
            fm ?: return

            val frag = create()
            fm.beginTransaction()
                .add(frag, tag)
                .commitAllowingStateLoss()
        }

        fun createAndShow(parent: androidx.fragment.app.Fragment, tag: String) {
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

        val builder = AlertDialog.Builder(activity, R.style.Theme_App_Dialog).setIcon(icon)

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
            parent.onPositiveClick(dialog, tag)
        }

        if (negativeTextId != 0) {
            builder.setNegativeButton(negativeTextId) { _, _ ->
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
