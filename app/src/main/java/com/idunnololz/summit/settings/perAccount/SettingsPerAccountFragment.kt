package com.idunnololz.summit.settings.perAccount

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.PerAccountSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsPerAccountFragment : BaseSettingsFragment() {

  private val args by navArgs<SettingsPerAccountFragmentArgs>()

  private val viewModel: SettingsPerAccountViewModel by viewModels()

  @Inject
  override lateinit var settings: PerAccountSettings

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

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

            getMainActivity()?.apply {
              supportActionBar?.title = it.data.account.name
              supportActionBar?.subtitle = it.data.account.instance
            }

            refresh()
          }
        }
      }

      viewModel.loadAccount(args.accountId)
    }
  }

  override fun generateData(): List<SettingModelItem> {
    val data = viewModel.preferenceData.valueOrNull ?: return listOf()

//    adapter = SettingItemsAdapter(
//      context = context,
//      onSettingClick = {
//        when (it.id) {
//          settings.settingViewType.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingsContentFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingTheme.id -> {
//            val directions = SettingsPerAccountFragmentDirections
//              .actionSettingPerAccountFragmentToSettingThemeFragment(
//                data.account,
//              )
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingPostAndComment.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingPostAndCommentsFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingGestures.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingGesturesFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingCache.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingCacheFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingPostList.id -> {
//            val directions = SettingsPerAccountFragmentDirections
//              .actionSettingPerAccountFragmentToSettingsContentFragment(
//                data.account,
//              )
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.commentListSettings.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingCommentListFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingHiddenPosts.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingHiddenPostsFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingAbout.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingAboutFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingSummitCommunity.id -> {
//            val fm = parentFragmentManager
//            for (i in 0 until fm.backStackEntryCount) {
//              fm.popBackStack()
//            }
//            getMainActivity()?.launchPage(summitCommunityPage)
//            true
//          }
//          settings.miscSettings.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingMiscFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.loggingSettings.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingLoggingFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.historySettings.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingHistoryFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.navigationSettings.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingNavigationFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.userActionsSettings.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToActions()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.settingAccount.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingAccountsFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.backupAndRestoreSettings.id -> {
//            val directions = SettingsFragmentDirections
//              .actionSettingsFragmentToSettingBackupAndRestoreFragment()
//            findNavController().navigateSafe(directions)
//            true
//          }
//          settings.manageSettings.id -> {
//            val directions = SettingsPerAccountFragmentDirections
//              .actionSettingPerAccountFragmentToManageSettingsFragment(
//                data.account,
//              )
//            findNavController().navigateSafe(directions)
//            true
//          }
//          else -> false
//        }
//      },
//      childFragmentManager,
//    )

    return listOf(
      settings.desc.asCustomItem(),
      settings.settingTheme.asCustomItem {
        val directions = SettingsPerAccountFragmentDirections
          .actionSettingPerAccountFragmentToSettingThemeFragment(
            data.account,
          )
        findNavController().navigateSafe(directions)
      },
      settings.settingPostList.asCustomItem {
        val directions = SettingsPerAccountFragmentDirections
          .actionSettingPerAccountFragmentToSettingsContentFragment(
            data.account,
          )
        findNavController().navigateSafe(directions)
      },
      settings.manageSettings.asCustomItem {
        val directions = SettingsPerAccountFragmentDirections
          .actionSettingPerAccountFragmentToManageSettingsFragment(
            data.account,
          )
        findNavController().navigateSafe(directions)
      },
    )
  }
}
