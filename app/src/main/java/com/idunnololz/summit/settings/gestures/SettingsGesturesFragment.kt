package com.idunnololz.summit.settings.gestures

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsGesturesBinding
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.GestureSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsGesturesFragment :
    BaseFragment<FragmentSettingsGesturesBinding>(),
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
            setupForFragment<SettingsFragment>()
        }

        setBinding(FragmentSettingsGesturesBinding.inflate(inflater, container, false))

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
        val context = requireContext()

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
        settings.postGestureActionColor1.bindTo(
            binding.postGestureAction1Color,
            { preferences.postGestureActionColor1 },
            {
                preferences.postGestureActionColor1 = it
            },
            { context.getColorCompat(R.color.style_red) },
        )

        settings.postGestureAction2.bindTo(
            binding.postGestureAction2,
            { preferences.postGestureAction2 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.postGestureActionColor2.bindTo(
            binding.postGestureAction2Color,
            { preferences.postGestureActionColor2 },
            {
                preferences.postGestureActionColor2 = it
            },
            { context.getColorCompat(R.color.style_blue) },
        )

        settings.postGestureAction3.bindTo(
            binding.postGestureAction3,
            { preferences.postGestureAction3 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.postGestureActionColor3.bindTo(
            binding.postGestureAction3Color,
            { preferences.postGestureActionColor3 },
            {
                preferences.postGestureActionColor3 = it
            },
            { context.getColorCompat(R.color.style_amber) },
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
        settings.commentGestureActionColor1.bindTo(
            binding.commentGestureAction1Color,
            { preferences.commentGestureActionColor1 },
            {
                preferences.commentGestureActionColor1 = it
            },
            { context.getColorCompat(R.color.style_red) },
        )

        settings.commentGestureAction2.bindTo(
            binding.commentGestureAction2,
            { preferences.commentGestureAction2 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.commentGestureActionColor2.bindTo(
            binding.commentGestureAction2Color,
            { preferences.commentGestureActionColor2 },
            {
                preferences.commentGestureActionColor2 = it
            },
            { context.getColorCompat(R.color.style_blue) },
        )

        settings.commentGestureAction3.bindTo(
            binding.commentGestureAction3,
            { preferences.commentGestureAction3 },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "aaaaaaa")
            },
        )
        settings.commentGestureActionColor3.bindTo(
            binding.commentGestureAction3Color,
            { preferences.commentGestureActionColor3 },
            {
                preferences.commentGestureActionColor3 = it
            },
            { context.getColorCompat(R.color.style_amber) },
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
