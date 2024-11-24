package com.idunnololz.summit.util

import android.util.Log
import com.idunnololz.summit.actions.ui.ActionsTabbedFragment
import com.idunnololz.summit.history.HistoryFragment
import com.idunnololz.summit.lemmy.communities.CommunitiesFragment
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoFragment
import com.idunnololz.summit.lemmy.createOrEditCommunity.CreateOrEditCommunityFragment
import com.idunnololz.summit.lemmy.inbox.InboxTabbedFragment
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
                    navBarController.enableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    showNotificationBarBg()
                }
                PostFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    showNotificationBarBg()
                }
                VideoViewerFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.hideNavBar(animate)
                    hideNotificationBarBg()
                }
                ImageViewerActivity::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.hideNavBar(animate)
                    hideNotificationBarBg()
                }
                SettingsCacheFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    showNotificationBarBg()
                }
                HistoryFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    showNotificationBarBg()
                }
                LoginFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.hideNavBar(animate)
                    hideNotificationBarBg()
                }
                SettingsFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.hideNavBar(animate)
                    hideNotificationBarBg()
                }
                PersonTabbedFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    hideNotificationBarBg()
                }
                CommunityInfoFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    hideNotificationBarBg()
                }
                FilteredPostsAndCommentsTabbedFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    showNotificationBarBg()
                }
                InboxTabbedFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav(supportOpenness = true)
                    showNotificationBarBg()
                }
                ActionsTabbedFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.hideNavBar(animate)
                    hideNotificationBarBg()
                }
                CommunitiesFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    showNotificationBarBg()
                }
                ModLogsFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    showNotificationBarBg()
                }
                CreateOrEditCommunityFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.hideNavBar(animate)
                    hideNotificationBarBg()
                }
                YouFragment::class -> {
                    navBarController.disableBottomNavViewScrolling()
                    navBarController.showBottomNav()
                    showNotificationBarBg()
                }
                else ->
                    throw RuntimeException(
                        "No setup instructions for type: ${t.java.canonicalName}",
                    )
            }
        }
    }
}
