package com.idunnololz.summit.util

import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.google.android.material.navigation.NavigationBarView
import com.idunnololz.summit.MainDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.main.CommunitySelectorController
import com.idunnololz.summit.main.NavBarController
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.video.VideoState

abstract class SummitActivity :
  BaseActivity(),
  BottomMenuContainer,
  InsetsProvider by InsetsHelper(
    consumeInsets = true,
  ) {

  val keyPressRegistrationManager = KeyPressRegistrationManager()

  fun getBottomNavHeight() = navBarController.bottomNavHeight
  fun showBottomNav() {
    navBarController.showBottomNav()
  }
  fun hideBottomNav() {
    navBarController.hideNavBar(animate = true)
  }

  fun openVideo(url: String, videoType: VideoType, videoState: VideoState?) {
    val direction = MainDirections.actionGlobalVideoViewerFragment(url, videoType, videoState)
    currentNavController?.navigateSafe(direction)
  }

  fun openSettings() {
    val direction = MainDirections.actionGlobalSettingsFragment(null)
    currentNavController?.navigateSafe(direction)
  }

  fun openAccountSettings() {
    val direction = MainDirections.actionGlobalSettingsFragment("web")
    currentNavController?.navigateSafe(direction)
  }

  fun showDownloadsSettings() {
    val direction = MainDirections.actionGlobalSettingsFragment("downloads")
    currentNavController?.navigateSafe(direction)
  }

  fun showCommunities(instance: String) {
    val directions = MainDirections.actionGlobalCommunitiesFragment(instance)
    currentNavController?.navigateSafe(directions)
  }

  fun setNavUiOpenPercent(showPercent: Float) {
    if (lockUiOpenness) return
    navBarController.animateNavBar(showPercent, animate = false)
  }

  fun registerOnNavigationItemReselectedListener(
    onNavigationItemReselectedListener: NavigationBarView.OnItemReselectedListener,
  ) {
    navBarController.registerOnNavigationItemReselectedListener(
      onNavigationItemReselectedListener,
    )
  }

  fun unregisterOnNavigationItemReselectedListener(
    onNavigationItemReselectedListener: NavigationBarView.OnItemReselectedListener,
  ) {
    navBarController.unregisterOnNavigationItemReselectedListener(
      onNavigationItemReselectedListener,
    )
  }

  fun hideSystemUI() {
    // Enables regular immersive mode.
    // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
    // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    window.decorView.systemUiVisibility = (
      View.SYSTEM_UI_FLAG_IMMERSIVE
        // Set the content to appear under the system bars so that the
        // content doesn't resize when the system bars hide and show.
        or SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        // Hide the nav bar and status bar
        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_FULLSCREEN
      )
  }

  // Shows the system bars by removing all the flags
  // except for the ones that make the content appear under the system bars.
  fun showSystemUI() {
    window.decorView.systemUiVisibility = (
      SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        or if (resources.getBoolean(R.bool.isLightTheme)) {
          View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
          0
        }
      )
  }

  abstract var navBarController: NavBarController
  abstract var moreActionsHelper: MoreActionsHelper
  abstract var lockUiOpenness: Boolean
  protected abstract var currentNavController: NavController?

  abstract fun runOnReady(lifecycleOwner: LifecycleOwner, cb: () -> Unit)

  abstract fun insetViewAutomaticallyByPaddingAndNavUi(
    lifecycleOwner: LifecycleOwner,
    rootView: View,
    applyLeftInset: Boolean = true,
    applyTopInset: Boolean = true,
    applyRightInset: Boolean = true,
    applyBottomInset: Boolean = true,
  )

  abstract fun launchPage(
    page: PageRef,
    switchToNativeInstance: Boolean = false,
    preferMainFragment: Boolean = false,
  )

  abstract fun showCommunitySelector(
    currentCommunityRef: CommunityRef? = null,
  ): CommunitySelectorController

  abstract fun openImage(
    sharedElement: View?,
    appBar: View?,
    title: String?,
    url: String,
    mimeType: String?,
    urlAlt: String? = null,
    mimeTypeAlt: String? = null,
  )

  abstract fun showCommunityInfo(communityRef: CommunityRef)
}
