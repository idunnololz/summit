package com.idunnololz.summit.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.databinding.DialogFragmentHelpAndFeedbackBinding
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.openAppOnPlayStore
import com.idunnololz.summit.util.startFeedbackIntent
import com.idunnololz.summit.util.summitCommunityPage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HelpAndFeedbackDialogFragment :
    BaseBottomSheetDialogFragment<DialogFragmentHelpAndFeedbackBinding>(),
    FullscreenDialogFragment {

    companion object {
        fun show(fragmentManager: FragmentManager) = HelpAndFeedbackDialogFragment()
            .show(fragmentManager, "HelpAndFeedbackDialogFragment")
    }

    @Inject
    lateinit var preferences: Preferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(
            DialogFragmentHelpAndFeedbackBinding.inflate(
                inflater,
                container,
                false,
            ),
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
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

            if (preferences.shakeToSendFeedback) {
                disableGestureButton.visibility = View.VISIBLE
                disableGestureButton.setOnClickListener {
                    preferences.shakeToSendFeedback = false
                    disableGestureButton.visibility = View.GONE
                }
            } else {
                disableGestureButton.visibility = View.GONE
            }
        }
    }
}
