package com.idunnololz.summit.util

import android.util.Log
import com.idunnololz.summit.actions.ui.ActionsTabbedFragment
import com.idunnololz.summit.history.HistoryFragment
import com.idunnololz.summit.inbox.InboxTabbedFragment
import com.idunnololz.summit.lemmy.communities.CommunitiesFragment
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoFragment
import com.idunnololz.summit.lemmy.createOrEditCommunity.CreateOrEditCommunityFragment
import com.idunnololz.summit.lemmy.mod.viewVotes.ViewVotesFragment
import com.idunnololz.summit.lemmy.modlogs.ModLogsFragment
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.login.LoginFragment
import com.idunnololz.summit.preview.ImageViewerActivity
import com.idunnololz.summit.preview.VideoViewerFragment
import com.idunnololz.summit.saved.FilteredPostsAndCommentsTabbedFragment
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.cache.SettingsCacheFragment
import com.idunnololz.summit.you.YouFragment
import kotlin.reflect.KClass

inline fun <reified T> BaseFragment<*>.setupForFragment(animate: Boolean = true) {
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
        FilteredPostsAndCommentsTabbedFragment::class -> {
          navBarController.showBottomNav()
          showNotificationBarBg()
        }
        InboxTabbedFragment::class -> {
          showNavBar()
          showNotificationBarBg()
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
        else ->
          throw RuntimeException(
            "No setup instructions for type: ${t.java.canonicalName}",
          )
      }
    }
  }
}
