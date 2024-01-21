package com.idunnololz.summit.saveForLater

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import coil.dispose
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentSaveForLaterBinding
import com.idunnololz.summit.databinding.SaveSlotBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.humanReadableByteCountSi
import com.idunnololz.summit.util.setupBottomSheetAndShow
import com.idunnololz.summit.util.tsToShortDate
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SaveForLaterDialogFragment : BaseDialogFragment<DialogFragmentSaveForLaterBinding>() {

    private val args by navArgs<SaveForLaterDialogFragmentArgs>()

    @Inject
    lateinit var saveForLaterManager: SaveForLaterManager

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

        setBinding(DialogFragmentSaveForLaterBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheetAndShow(
            bottomSheet = binding.bottomSheet,
            bottomSheetContainerInner = binding.bottomSheetContainerInner,
            overlay = binding.overlay,
            onClose = {
                dismiss()
            },
        )

        requireMainActivity().apply {
            doOnInsetChanged(viewLifecycleOwner) { insets ->
                binding.bottomSheet.updatePadding(bottom = insets.bottom)
                binding.bottomSheetContainerInner.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = insets.top
                }
            }
        }

        loadSaveSlots()
    }

    private fun loadSaveSlots() {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)

        with(binding) {
            slotsContainer.removeAllViews()

            saveForLaterManager.getSlotFiles().withIndex().forEach { (index, file) ->
                val b = SaveSlotBinding.inflate(inflater, slotsContainer, false)

                if (file.exists()) {
                    b.preview.load(file)
                } else {
                    b.preview.dispose()
                    b.preview.setImageDrawable(null)
                }

                b.text.text = getString(R.string.slot_format, (index + 1).toString())

                if (file.exists()) {
                    b.subtitle.text = "${humanReadableByteCountSi(file.length())} - ${tsToShortDate(file.lastModified())}"
                } else {
                    b.subtitle.text = getString(R.string.empty)
                }

                slotsContainer.addView(b.root)

                b.root.setOnClickListener {
                    file.parentFile?.mkdirs()
                    context.contentResolver.openInputStream(args.fileUri).use { input ->
                        file.outputStream().use { output ->
                            input?.copyTo(output)
                        }
                    }
                    dismiss()
                }
            }
        }
    }
}
