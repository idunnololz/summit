package com.idunnololz.summit.util

import android.util.Log
import com.idunnololz.summit.actions.ui.ActionDetailsFragment
import com.idunnololz.summit.actions.ui.ActionsTabbedFragment
import com.idunnololz.summit.history.HistoryFragment
import com.idunnololz.summit.inbox.InboxTabbedFragment
import com.idunnololz.summit.inbox.inbox.InboxFragment
import com.idunnololz.summit.lemmy.communities.CommunitiesFragment
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoFragment
import com.idunnololz.summit.lemmy.createOrEditCommunity.CreateOrEditCommunityFragment
import com.idunnololz.summit.lemmy.mod.viewVotes.ViewVotesFragment
import com.idunnololz.summit.lemmy.modlogs.ModLogsFragment
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.search.SearchHomeFragment
import com.idunnololz.summit.lemmy.search.SearchTabbedFragment
import com.idunnololz.summit.login.LoginFragment
import com.idunnololz.summit.preview.ImageViewerActivity
import com.idunnololz.summit.preview.VideoViewerFragment
import com.idunnololz.summit.saved.FilteredPostsAndCommentsTabbedFragment
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.accounts.SettingsAccountsFragment
import com.idunnololz.summit.settings.cache.SettingsCacheFragment
import com.idunnololz.summit.settings.commentQuickActions.CustomQuickActionsFragment
import com.idunnololz.summit.you.YouFragment
import kotlin.reflect.KClass

inline fun <reified T> BaseFragment<*>.setupForFragment(animate: Boolean = true) {
  setupForFragment(T::class, animate)
}

inline fun <reified T: BaseFragment<*>> T.setupForThisFragment(animate: Boolean = true) {
  setupForFragment(T::class, animate)
}

fun BaseFragment<*>.setupForFragment(t: KClass<*>, animate: Boolean) {
  getMainActivity()?.apply {
    Log.d("MainActivity", "setupForFragment(): $t")

    runWhenLaidOut {
      when (t) {
        CommunityFragment::class -> {
          showNotificationBarBg()
        }
        PostFragment::class -> {
          showNotificationBarBg()
        }
        VideoViewerFragment::class -> {
          navBarController.hideNavBar()
          hideNotificationBarBg()
        }
        ImageViewerActivity::class -> {
          navBarController.hideNavBar()
          hideNotificationBarBg()
        }
        SettingsCacheFragment::class -> {
          navBarController.showBottomNav()
          showNotificationBarBg()
        }
        HistoryFragment::class -> {
          navBarController.showBottomNav()
          showNotificationBarBg()
        }
        LoginFragment::class -> {
          navBarController.hideNavBar()
          hideNotificationBarBg()
        }
        BaseSettingsFragment::class,
        SettingsFragment::class -> {
          navBarController.hideNavBar()
          hideNotificationBarBg()
        }
        PersonTabbedFragment::class -> {
          hideNotificationBarBg()
        }
        CommunityInfoFragment::class -> {
          navBarController.showBottomNav()
          hideNotificationBarBg()
        }
        SearchHomeFragment::class,
        SearchTabbedFragment::class,
        FilteredPostsAndCommentsTabbedFragment::class -> {
          navBarController.showBottomNav()
          showNotificationBarBg()
        }
        InboxFragment::class,
        InboxTabbedFragment::class -> {
          showNotificationBarBg()
        }
        ActionDetailsFragment::class -> {
          navBarController.hideNavBar()
          hideNotificationBarBg()
        }
        ActionsTabbedFragment::class -> {
          navBarController.hideNavBar()
          hideNotificationBarBg()
        }
        CommunitiesFragment::class -> {
          navBarController.showBottomNav()
          showNotificationBarBg()
        }
        ModLogsFragment::class -> {
          navBarController.showBottomNav()
          showNotificationBarBg()
        }
        CreateOrEditCommunityFragment::class -> {
          navBarController.hideNavBar()
          hideNotificationBarBg()
        }
        YouFragment::class -> {
          navBarController.showBottomNav()
          showNotificationBarBg()
        }
        ViewVotesFragment::class -> {
          navBarController.showBottomNav()
          hideNotificationBarBg()
        }
        else -> {
          navBarController.showBottomNav()
          showNotificationBarBg()
        }
      }
    }
  }
}
