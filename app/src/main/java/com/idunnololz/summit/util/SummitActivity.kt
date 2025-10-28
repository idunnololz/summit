package com.idunnololz.summit.util

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.google.android.material.navigation.NavigationBarView
import com.idunnololz.summit.MainDirections
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
    consumeInsets = false,
  ) {

  val keyPressRegistrationManager = KeyPressRegistrationManager()
  private var windowInsetsController: WindowInsetsControllerCompat? = null

  fun getBottomNavHeight() = navBarController.bottomNavHeight
  fun showNavBar() {
    navBarController.showBottomNav()
  }
  fun hideNavBar() {
    navBarController.hideNavBar()
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

  fun setNavUiOpenPercent(showPercent: Float, force: Boolean = false) {
    if (navBarController.useNavigationRail && !force) return

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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
  }

  fun hideSystemUI() {
    windowInsetsController?.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
  }

  fun showSystemUI() {
    windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
  }

  abstract var navBarController: NavBarController
  abstract var moreActionsHelper: MoreActionsHelper
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

  abstract fun insetViewAutomaticallyByMarginsAndNavUi(
    lifecycleOwner: LifecycleOwner,
    view: View,
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
