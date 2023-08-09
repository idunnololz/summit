package com.idunnololz.summit.settings.hiddenPosts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingHiddenPostsBinding
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.HiddenPostsSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingHiddenPostsFragment : BaseFragment<FragmentSettingHiddenPostsBinding>() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var hiddenPostsManager: HiddenPostsManager

    @Inject
    lateinit var hiddenPostsSettings: HiddenPostsSettings

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
            supportActionBar?.title = hiddenPostsSettings.getPageName(context)
        }

        updateRendering()
    }

    private fun updateRendering() {
        hiddenPostsSettings.resetHiddenPosts.bindTo(
            b = binding.resetHiddenPosts,
            onValueChanged = {
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
            },
        )
    }
}
