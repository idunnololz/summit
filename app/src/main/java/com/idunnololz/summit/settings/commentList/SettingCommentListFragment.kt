package com.idunnololz.summit.settings.commentList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsCommentListBinding
import com.idunnololz.summit.lemmy.idToCommentsSortOrder
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.CommentListSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingCommentListFragment :
    BaseFragment<FragmentSettingsCommentListBinding>(),
    SettingValueUpdateCallback {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var settings: CommentListSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsCommentListBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = settings.getPageName(context)
        }

        updateRendering()
    }

    private fun updateRendering() {
        settings.defaultCommentsSortOrder.bindTo(
            binding.defaultCommentsSortOrder,
            {
                preferences.defaultCommentsSortOrder?.toApiSortOrder()?.toId()
                    ?: R.id.comments_sort_order_default
            },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )

        settings.relayStyleNavigation.bindTo(
            binding.relayStyleNavigation,
            { preferences.commentsNavigationFab },
            { preferences.commentsNavigationFab = it },
        )
        settings.useVolumeButtonNavigation.bindTo(
            binding.useVolumeButtonNavigation,
            { preferences.useVolumeButtonNavigation },
            { preferences.useVolumeButtonNavigation = it },
        )
        settings.collapseChildCommentsByDefault.bindTo(
            binding.collapseChildCommentsByDefault,
            { preferences.collapseChildCommentsByDefault },
            { preferences.collapseChildCommentsByDefault = it },
        )
        settings.hideCommentScores.bindTo(
            binding.hideCommentScores,
            { preferences.hideCommentScores },
            {
                preferences.hideCommentScores = it
            },
        )
        settings.autoCollapseCommentThreshold.bindTo(
            binding.autoCollapseComments,
            { convertAutoCollapseCommentToOptionId(preferences.autoCollapseCommentThreshold) },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.showCommentUpvotePercentage.bindTo(
            binding.showCommentUpvotePercentage,
            { preferences.showCommentUpvotePercentage },
            {
                preferences.showCommentUpvotePercentage = it
            },
        )
    }

    private fun convertAutoCollapseCommentToOptionId(value: Float) =
        when {
            value >= 0.499f -> {
                R.id.auto_collapse_comment_threshold_50
            }
            value >= 0.399f -> {
                R.id.auto_collapse_comment_threshold_40
            }
            value >= 0.299f -> {
                R.id.auto_collapse_comment_threshold_30
            }
            value >= 0.199f -> {
                R.id.auto_collapse_comment_threshold_20
            }
            value >= 0f -> {
                R.id.auto_collapse_comment_threshold_10
            }
            else -> {
                R.id.auto_collapse_comment_threshold_never_collapse
            }
        }

    private fun convertOptionIdToAutoCollapseCommentThreshold(@IdRes id: Int) =
        when (id) {
            R.id.auto_collapse_comment_threshold_50 -> 0.5f
            R.id.auto_collapse_comment_threshold_40 -> 0.4f
            R.id.auto_collapse_comment_threshold_30 -> 0.3f
            R.id.auto_collapse_comment_threshold_20 -> 0.2f
            R.id.auto_collapse_comment_threshold_10 -> 0.1f
            R.id.auto_collapse_comment_threshold_never_collapse -> -1f
            else -> null
        }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            settings.defaultCommentsSortOrder.id -> {
                preferences.defaultCommentsSortOrder = idToCommentsSortOrder(value as Int)
            }
            settings.autoCollapseCommentThreshold.id -> {
                val threshold = convertOptionIdToAutoCollapseCommentThreshold(value as Int)

                if (threshold != null) {
                    preferences.autoCollapseCommentThreshold = threshold
                }
            }
        }

        updateRendering()
    }
}
