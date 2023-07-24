package com.idunnololz.summit.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentBottomMenuBinding
import com.idunnololz.summit.databinding.DialogFragmentTextValueBinding
import com.idunnololz.summit.settings.RadioGroupSettingItem
import com.idunnololz.summit.settings.SettingItem
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.getParcelableCompat

class MultipleChoiceDialogFragment : BaseDialogFragment<DialogFragmentBottomMenuBinding>(),
    FullscreenDialogFragment {

    companion object {
        private const val ARG_SETTING_ITEM = "ARG_SETTING_ITEM"

        fun newInstance(settingItem: RadioGroupSettingItem) =
            MultipleChoiceDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SETTING_ITEM, settingItem)
                }
            }
    }

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
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentBottomMenuBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settingItem = requireArguments().getParcelableCompat<RadioGroupSettingItem>(
            ARG_SETTING_ITEM)

        if (settingItem == null) {
            dismiss()
            return
        }

        BottomMenu(requireContext())
            .apply {
                settingItem.options.forEach {
                    if (it.icon != null) {
                        addItemWithIcon(it.id, it.title, it.icon)
                    } else {
                        addItem(it.id, it.title)
                    }
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
            .show(requireMainActivity(), binding.root, handleBackPress = false, expandFully = true)
    }
}