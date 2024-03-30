package com.idunnololz.summit.settings.hiddenPosts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentSettingHiddenPostsBinding
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.HiddenPostsSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingHiddenPostsFragment :
    BaseFragment<FragmentSettingHiddenPostsBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var hiddenPostsManager: HiddenPostsManager

    @Inject
    lateinit var settings: HiddenPostsSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingHiddenPostsBinding.inflate(inflater, container, false))

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
        val defaultDecimalFormat = PrettyPrintUtils.defaultDecimalFormat

        lifecycleScope.launch {
            val count = hiddenPostsManager.getHiddenPostsCount()

            withContext(Dispatchers.Main) {
                settings.hiddenPostsCount.copy(
                    description = getString(
                        R.string.hidden_posts_format,
                        defaultDecimalFormat.format(count),
                        defaultDecimalFormat.format(hiddenPostsManager.hiddenPostsLimit),
                    ),
                ).bindTo(
                    b = binding.hiddenPostsStats,
                    {},
                )
            }
        }

        settings.enableHiddenPosts.bindTo(
            binding.hiddenPosts,
            { preferences.isHiddenPostsEnabled },
            {
                preferences.isHiddenPostsEnabled = it
                updateRendering()
            },
        )

        settings.resetHiddenPosts.bindTo(
            b = binding.resetHiddenPosts,
            onValueChanged = {
                lifecycleScope.launch {
                    val count = withContext(Dispatchers.Default) {
                        hiddenPostsManager.getHiddenPostsCount()
                    }

                    withContext(Dispatchers.Main) {
                        AlertDialogFragment.Builder()
                            .setTitle(R.string.reset_hidden_posts_confirm_title)
                            .setMessage(
                                getString(
                                    R.string.reset_hidden_posts_confirm_desc_format,
                                    count.toString(),
                                ),
                            )
                            .setPositiveButton(R.string.yes)
                            .setNegativeButton(R.string.no)
                            .createAndShow(childFragmentManager, "reset_hidden_posts")
                    }
                }
            },
        )

        settings.viewHiddenPosts.bindTo(
            b = binding.viewHiddenPosts,
            onValueChanged = {
                val directions = SettingHiddenPostsFragmentDirections
                    .actionSettingHiddenPostsFragmentToHiddenPostsFragment()
                findNavController().navigateSafe(directions)
            },
        )
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            "reset_hidden_posts" -> {
                lifecycleScope.launch {
                    val count = withContext(Dispatchers.Default) {
                        hiddenPostsManager.getHiddenPostsCount()
                    }
                    hiddenPostsManager.clearHiddenPosts()

                    Snackbar
                        .make(
                            binding.coordinatorLayout,
                            getString(R.string.removed_hidden_posts_format, count.toString()),
                            Snackbar.LENGTH_LONG,
                        )
                        .show()
                }
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}
