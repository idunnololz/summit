package com.idunnololz.summit.saveForLater

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import coil3.asImage
import coil3.dispose
import coil3.load
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentChooseSavedImageBinding
import com.idunnololz.summit.databinding.SaveSlotBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.PrettyPrintUtils.humanReadableByteCountSi
import com.idunnololz.summit.util.setupBottomSheetAndShow
import com.idunnololz.summit.util.shimmer.newShimmerDrawable16to9
import com.idunnololz.summit.util.tsToShortDate
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ChooseSavedImageDialogFragment : BaseDialogFragment<DialogFragmentChooseSavedImageBinding>() {

    companion object {

        private const val TAG = "ChooseSavedImageDialogFragment"

        const val REQUEST_KEY = "ChooseSavedImageDialogFragment_req"
        const val REQUEST_RESULT = "REQUEST_RESULT"
    }

    @Parcelize
    data class Result(
        val fileUri: Uri,
    ) : Parcelable

    private val args by navArgs<ChooseSavedImageDialogFragmentArgs>()

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

        setBinding(DialogFragmentChooseSavedImageBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)

            setupBottomSheetAndShow(
                bottomSheet = binding.bottomSheet,
                bottomSheetContainerInner = binding.bottomSheetContainerInner,
                overlay = binding.overlay,
                onClose = {
                    dismiss()
                },
                expandFully = true,
            )
        }

        requireMainActivity().apply {
            insets.observe(viewLifecycleOwner) { insets ->
                binding.bottomSheet.updatePadding(bottom = insets.bottomInset)
                binding.bottomSheetContainerInner.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = insets.topInset
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
                    b.preview.load(file) {
                        placeholder(newShimmerDrawable16to9(context).asImage())
                    }
                } else {
                    b.preview.dispose()
                    b.preview.setImageDrawable(null)
                }

                b.text.text = getString(R.string.slot_format, (index + 1).toString())

                if (file.exists()) {
                    lifecycle.coroutineScope.launch {
                        val hex = file.readBytes().take(
                            10,
                        ).joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }
                        Log.d(TAG, "File ${index + 1}. File name: ${file.absolutePath}. Hex: $hex")
                    }
                    b.subtitle.text = "${humanReadableByteCountSi(file.length())} - " +
                        "${tsToShortDate(file.lastModified())}"
                } else {
                    b.subtitle.text = getString(R.string.empty)
                }

                slotsContainer.addView(b.root)

                b.root.setOnClickListener {
                    if (file.exists()) {
                        setFragmentResult(
                            args.requestKey ?: REQUEST_KEY,
                            Bundle().apply {
                                putParcelable(REQUEST_RESULT, Result(file.toUri()))
                            },
                        )
                        dismiss()
                    }
                }
            }
        }
    }
}
