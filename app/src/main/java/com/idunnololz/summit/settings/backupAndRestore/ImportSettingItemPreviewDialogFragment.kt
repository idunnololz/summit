package com.idunnololz.summit.settings.backupAndRestore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentImportSettingItemPreviewBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.AllSettings
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ImportSettingItemPreviewDialogFragment : BaseDialogFragment<DialogFragmentImportSettingItemPreviewBinding>() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            settingKey: String,
            stringValue: String,
            type: String,
        ) {
            ImportSettingItemPreviewDialogFragment().apply {
                arguments = ImportSettingItemPreviewDialogFragmentArgs(
                    settingKey = settingKey,
                    stringValue = stringValue,
                    type = type
                ).toBundle()
            }.show(fragmentManager, "ImportSettingItemPreviewDialogFragment")
        }
    }

    private val args by navArgs<ImportSettingItemPreviewDialogFragmentArgs>()

    @Inject
    lateinit var allSettings: AllSettings

    @Inject
    lateinit var preferences: Preferences

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            val window = checkNotNull(dialog.window)
            window.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentImportSettingItemPreviewBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val keyToSettingItems = allSettings.generateMapFromKeysToRelatedSettingItems()
        val relatedSettingItems = keyToSettingItems[args.settingKey]

        with(binding) {

            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            key.text = args.settingKey.lowercase()
            importValue.text = args.stringValue
            type.text = args.type

            currentValue.text = preferences.all[args.settingKey].toString()

            relatedSettings.text =
                if (relatedSettingItems.isNullOrEmpty()) {
                    getString(R.string.unknown)
                } else {
                    relatedSettingItems.joinToString {
                        it.title
                    }
                }
        }
    }
}