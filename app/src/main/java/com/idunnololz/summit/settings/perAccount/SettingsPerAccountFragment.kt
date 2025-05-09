package com.idunnololz.summit.settings.perAccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsPerAccountBinding
import com.idunnololz.summit.settings.PerAccountSettings
import com.idunnololz.summit.settings.SettingItemsAdapter
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.SettingsFragmentDirections
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.summitCommunityPage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPerAccountFragment : BaseFragment<FragmentSettingsPerAccountBinding>() {

  private val args by navArgs<SettingsPerAccountFragmentArgs>()

  private val viewModel: SettingsPerAccountViewModel by viewModels()

  private var adapter: SettingItemsAdapter? = null

  @Inject
  lateinit var settings: PerAccountSettings

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentSettingsPerAccountBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    requireSummitActivity().apply {
      setupForFragment<SettingsFragment>()
      insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

      setSupportActionBar(binding.toolbar)

      supportActionBar?.setDisplayShowHomeEnabled(true)
      supportActionBar?.setDisplayHomeAsUpEnabled(true)
      supportActionBar?.title = context.getString(R.string.account_settings)
      supportActionBar?.subtitle = null
    }

    with(binding) {
      viewModel.preferenceData.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            if (it.error is SettingsPerAccountViewModel.NoAccountError) {
              findNavController().navigateUp()
            }
          }
          is StatefulData.Loading -> {
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {}
          is StatefulData.Success -> {
            loadingView.hideAll()

            bindUi(it.data)
          }
        }
      }

      viewModel.loadAccount(args.accountId)
    }
  }

  private fun bindUi(data: SettingsPerAccountViewModel.PreferenceData) {
    if (!isBindingAvailable()) {
      return
    }

    getMainActivity()?.apply {
      supportActionBar?.title = data.account.name
      supportActionBar?.subtitle = data.account.instance
    }

    val context = requireContext()

    with(binding) {
      recyclerView.setup(animationsHelper)
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(true)

      if (adapter == null) {
        adapter = SettingItemsAdapter(
          context = context,
          onSettingClick = {
            when (it.id) {
              settings.settingViewType.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingsContentFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingTheme.id -> {
                val directions = SettingsPerAccountFragmentDirections
                  .actionSettingPerAccountFragmentToSettingThemeFragment(
                    data.account,
                  )
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingPostAndComment.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingPostAndCommentsFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingGestures.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingGesturesFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingCache.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingCacheFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingPostList.id -> {
                val directions = SettingsPerAccountFragmentDirections
                  .actionSettingPerAccountFragmentToSettingsContentFragment(
                    data.account,
                  )
                findNavController().navigateSafe(directions)
                true
              }
              settings.commentListSettings.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingCommentListFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingHiddenPosts.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingHiddenPostsFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingAbout.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingAboutFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingSummitCommunity.id -> {
                val fm = parentFragmentManager
                for (i in 0 until fm.backStackEntryCount) {
                  fm.popBackStack()
                }
                getMainActivity()?.launchPage(summitCommunityPage)
                true
              }
              settings.miscSettings.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingMiscFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.loggingSettings.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingLoggingFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.historySettings.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingHistoryFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.navigationSettings.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingNavigationFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.userActionsSettings.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToActions()
                findNavController().navigateSafe(directions)
                true
              }
              settings.settingAccount.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingAccountsFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.backupAndRestoreSettings.id -> {
                val directions = SettingsFragmentDirections
                  .actionSettingsFragmentToSettingBackupAndRestoreFragment()
                findNavController().navigateSafe(directions)
                true
              }
              settings.manageSettings.id -> {
                val directions = SettingsPerAccountFragmentDirections
                  .actionSettingPerAccountFragmentToManageSettingsFragment(
                    data.account,
                  )
                findNavController().navigateSafe(directions)
                true
              }
              else -> false
            }
          },
          childFragmentManager,
        )
      }
      recyclerView.adapter = adapter?.apply {
        this.firstTitleHasTopMargin = false
        this.setData(settings.allSettings)
      }
    }
  }
}
