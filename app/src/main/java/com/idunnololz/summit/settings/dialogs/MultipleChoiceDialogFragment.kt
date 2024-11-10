package com.idunnololz.summit.settings.dialogs

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentBottomMenuBinding
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.newPredictiveBackBackPressHandler
import kotlin.math.max

class MultipleChoiceDialogFragment :
    BaseDialogFragment<DialogFragmentBottomMenuBinding>(),
    FullscreenDialogFragment {

    companion object {
        private const val ARG_SETTING_ITEM = "ARG_SETTING_ITEM"
        private const val ARG_CURRENT_VALUE = "ARG_CURRENT_VALUE"

        fun newInstance(settingItem: RadioGroupSettingItem, currentValue: Int?) =
            MultipleChoiceDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SETTING_ITEM, settingItem)
                    if (currentValue != null) {
                        putInt(ARG_CURRENT_VALUE, currentValue)
                    }
                }
            }
    }

    private var bottomMenu: BottomMenu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                window.setBackgroundDrawable(null)
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setWindowAnimations(R.style.BottomSheetAnimations)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentBottomMenuBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val args = requireArguments()
        val settingItem = args.getParcelableCompat<RadioGroupSettingItem>(
            ARG_SETTING_ITEM,
        )

        if (settingItem == null) {
            dismiss()
            return
        }

        val currentValue = if (args.containsKey(ARG_CURRENT_VALUE)) {
            args.getInt(ARG_CURRENT_VALUE)
        } else {
            null
        }

        val faintColor = ContextCompat.getColor(context, R.color.colorTextFaint)

        BottomMenu(requireContext())
            .apply {
                setTitle(settingItem.title)
                settingItem.options.forEach {
                    val title = if (it.isDefault) {
                        SpannableStringBuilder().apply {
                            append(it.title)

                            val start = length
                            append(" (")
                            append(getString(R.string._default))
                            append(")")
                            val end = length

                            setSpan(
                                ForegroundColorSpan(faintColor),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                            )
                        }
                    } else {
                        it.title
                    }

                    if (it.icon != null) {
                        addItemWithIcon(it.id, title, it.icon)
                    } else {
                        addItem(it.id, title)
                    }
                }
                if (currentValue != null) {
                    setChecked(currentValue)
                }

                setOnMenuItemClickListener {
                    (parentFragment as SettingValueUpdateCallback).updateValue(
                        settingItem.id,
                        it.id,
                    )
                }

                onClose = {
                    dismiss()
                }
            }
            .also {
                bottomMenu = it
            }
            .show(
                bottomMenuContainer = requireMainActivity(),
                bottomSheetContainer = binding.root,
                handleBackPress = false,
                expandFully = true,
            )
    }

    override fun dismiss() {
        super.dismiss()
        bottomMenu?.close()
    }

    override fun dismissAllowingStateLoss() {
        super.dismissAllowingStateLoss()
        bottomMenu?.close()
    }

    override fun getPredictiveBackBackPressCallback(): OnBackPressedCallback =
        newPredictiveBackBackPressHandler { bottomMenu }

}
