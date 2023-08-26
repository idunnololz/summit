package com.idunnololz.summit.settings.gestures

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.databinding.FragmentSettingGesturesBinding
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.GestureSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.cache.SettingCacheFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingGesturesFragment :
    BaseFragment<FragmentSettingGesturesBinding>(),
    SettingValueUpdateCallback {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var settings: GestureSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        requireMainActivity().apply {
            setupForFragment<SettingCacheFragment>()
        }

        setBinding(FragmentSettingGesturesBinding.inflate(inflater, container, false))

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
        settings.useGestureActions.bindTo(
            binding.gestureActions,
            { preferences.useGestureActions },
            {
                preferences.useGestureActions = it
                preferences.hideCommentActions = preferences.useGestureActions

                postAndCommentViewBuilder.onPreferencesChanged()

                updateRendering()
            },
        )

        settings.postGestureAction1.bindTo(
            binding.postGestureAction1,
            { preferences.postGestureAction1 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )

        settings.postGestureAction2.bindTo(
            binding.postGestureAction2,
            { preferences.postGestureAction2 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )

        settings.postGestureAction3.bindTo(
            binding.postGestureAction3,
            { preferences.postGestureAction3 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.postGestureSize.bindTo(
            binding.postGestureSize,
            { preferences.postGestureSize },
            {
                preferences.postGestureSize = it
            },
        )

        settings.commentGestureAction1.bindTo(
            binding.commentGestureAction1,
            { preferences.commentGestureAction1 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )

        settings.commentGestureAction2.bindTo(
            binding.commentGestureAction2,
            { preferences.commentGestureAction2 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )

        settings.commentGestureAction3.bindTo(
            binding.commentGestureAction3,
            { preferences.commentGestureAction3 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.commentGestureSize.bindTo(
            binding.commentGestureSize,
            { preferences.commentGestureSize },
            {
                preferences.commentGestureSize = it
            },
        )
    }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            settings.postGestureAction1.id -> {
                preferences.postGestureAction1 = value as Int
            }
            settings.postGestureAction2.id -> {
                preferences.postGestureAction2 = value as Int
            }
            settings.postGestureAction3.id -> {
                preferences.postGestureAction3 = value as Int
            }
            settings.commentGestureAction1.id -> {
                preferences.commentGestureAction1 = value as Int
            }
            settings.commentGestureAction2.id -> {
                preferences.commentGestureAction2 = value as Int
            }
            settings.commentGestureAction3.id -> {
                preferences.commentGestureAction3 = value as Int
            }
        }

        updateRendering()
    }
}
