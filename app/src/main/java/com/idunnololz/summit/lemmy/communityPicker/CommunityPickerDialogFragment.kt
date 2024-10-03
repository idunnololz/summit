package com.idunnololz.summit.lemmy.communityPicker

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.DialogFragmentCommunityPickerBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.multicommunity.CommunityAdapter
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class CommunityPickerDialogFragment :
    BaseDialogFragment<DialogFragmentCommunityPickerBinding>(),
    FullscreenDialogFragment,
    BackPressHandler {

    companion object {
        const val REQUEST_KEY = "CommunityPickerDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        fun show(fragmentManager: FragmentManager) {
            CommunityPickerDialogFragment()
                .showAllowingStateLoss(fragmentManager, "CommunityPickerDialogFragment")
        }
    }

    private var adapter: CommunityAdapter? = null

    private val viewModel: CommunityPickerViewModel by viewModels()

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    @Inject
    lateinit var avatarHelper: AvatarHelper

    @Parcelize
    data class Result(
        val communityRef: CommunityRef.CommunityRefByName,
        val icon: String?,
        val communityId: Int,
    ) : Parcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentCommunityPickerBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().apply {
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        val context = requireContext()

        with(binding) {
            searchEditText.addTextChangedListener {
                val query = it?.toString() ?: ""
                adapter?.setQuery(query) {
                    resultsRecyclerView.scrollToPosition(0)
                }
                viewModel.doQuery(query)
            }

            adapter = CommunityAdapter(
                context = context,
                offlineManager = offlineManager,
                canSelectMultipleCommunities = false,
                avatarHelper = avatarHelper,
                onSingleCommunitySelected = { ref, icon, communityId ->
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(
                            REQUEST_KEY_RESULT to Result(ref, icon, communityId),
                        ),
                    )
                    dismiss()
                },
            )
            resultsRecyclerView.adapter = adapter
            resultsRecyclerView.setHasFixedSize(true)
            resultsRecyclerView.layoutManager = LinearLayoutManager(context)
            resultsRecyclerView.setup(animationsHelper)

            viewModel.searchResults.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        adapter?.setQueryServerResults(listOf())
                    }
                    is StatefulData.Loading -> {
                        adapter?.setQueryServerResultsInProgress()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        adapter?.setQueryServerResults(it.data)
                    }
                }
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (!binding.searchEditText.text.isNullOrBlank()) {
            binding.searchEditText.setText("")
            return true
        }

        try {
            dismiss()
        } catch (e: IllegalStateException) {
            // do nothing... very rare
        }
        return true
    }
}
