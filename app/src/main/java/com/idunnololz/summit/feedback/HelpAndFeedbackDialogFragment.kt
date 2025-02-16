package com.idunnololz.summit.feedback

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.FragmentManager
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.theme.overlay.MaterialThemeOverlay
import com.idunnololz.summit.databinding.DialogFragmentHelpAndFeedbackBinding
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.openAppOnPlayStore
import com.idunnololz.summit.util.startFeedbackIntent
import com.idunnololz.summit.util.summitCommunityPage
import javax.inject.Inject


class HelpAndFeedbackDialogFragment :
    BaseBottomSheetDialogFragment<DialogFragmentHelpAndFeedbackBinding>(),
    FullscreenDialogFragment {

    companion object {
        fun show(fragmentManager: FragmentManager) = HelpAndFeedbackDialogFragment()
            .show(fragmentManager, "HelpAndFeedbackDialogFragment")
    }

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(
            DialogFragmentHelpAndFeedbackBinding.inflate(
                inflater, container, false,
            ),
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (binding) {
            postToCommunityButton.setOnClickListener {
                getMainActivity()?.requestScreenshotAndShowFeedbackScreen()
                dismiss()
            }
            emailButton.setOnClickListener {
                startFeedbackIntent(requireContext())
                dismiss()
            }
            rateButton.setOnClickListener {
                openAppOnPlayStore()
                dismiss()
            }
            communityButton.setOnClickListener {
                getMainActivity()?.launchPage(summitCommunityPage)
                dismiss()
            }
        }
    }
}