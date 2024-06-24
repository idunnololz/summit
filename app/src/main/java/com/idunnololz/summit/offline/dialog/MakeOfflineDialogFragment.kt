package com.idunnololz.summit.offline.dialog

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentErrorBinding
import com.idunnololz.summit.databinding.DialogFragmentMakeOfflineBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.offline.OfflinePostFeedWork
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.setSizeDynamically
import com.idunnololz.summit.util.getParcelableCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MakeOfflineDialogFragment : BaseDialogFragment<DialogFragmentMakeOfflineBinding>() {

    companion object {
        private const val ARG_COMMUNITY_REF = "ARG_COMMUNITY_REF"

        fun newInstance(communityRef: CommunityRef) =
            MakeOfflineDialogFragment()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_COMMUNITY_REF, communityRef)
                    }
                }
    }

    private val viewModel: MakeOfflineViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentMakeOfflineBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            val communityRef = arguments?.getParcelableCompat<CommunityRef>(ARG_COMMUNITY_REF)

            if (communityRef == null) {
                dismiss()
                return
            }

            viewModel.runWorkerIfNotRun(communityRef)

            progress.max = 100
            subProgress.max = 100

            viewModel.progress.observe(viewLifecycleOwner) {
                progressText.text = when (it.currentPhase) {
                    OfflinePostFeedWork.ProgressPhase.Start ->
                        getString(R.string.offline_starting)
                    OfflinePostFeedWork.ProgressPhase.FetchingPostFeed ->
                        getString(R.string.offline_fetching_post_feed)
                    OfflinePostFeedWork.ProgressPhase.FetchingPosts ->
                        getString(R.string.offline_fetching_posts)
                    OfflinePostFeedWork.ProgressPhase.FetchingExtras ->
                        getString(R.string.offline_fetching_extras)
                    OfflinePostFeedWork.ProgressPhase.Complete ->
                        getString(R.string.offline_done)
                }
                progress.progress = (it.progressPercent * 100).toInt()
                subProgress.progress = (it.subProgressPercent * 100).toInt()

                if (it.currentPhase == OfflinePostFeedWork.ProgressPhase.Complete) {
                    TransitionManager.beginDelayedTransition(root)

                    progress.visibility = View.GONE
                    subProgress.visibility = View.GONE
                    cancel.visibility = View.GONE
                    dismiss.visibility = View.VISIBLE
                }
            }
            cancel.setOnClickListener {
                viewModel.cancel()
                dismiss()
            }
            dismiss.setOnClickListener {
                dismiss()
            }
        }
    }
}