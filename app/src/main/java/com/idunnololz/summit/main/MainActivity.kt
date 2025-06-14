package com.idunnololz.summit.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Pair
import androidx.core.view.MenuItemCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.window.layout.WindowMetricsCalculator
import coil3.Image
import coil3.asDrawable
import coil3.target.Target
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.MainDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.alert.launchAlertDialog
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.ActivityMainBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.feedback.PostFeedbackDialogFragment
import com.idunnololz.summit.feedback.ShakeFeedbackHelper
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.CommunityFragmentArgs
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityEditorDialogFragment
import com.idunnololz.summit.lemmy.post.PostFragmentArgs
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.getFeedbackScreenshotFile
import com.idunnololz.summit.lemmy.utils.showShareSheetForImage
import com.idunnololz.summit.links.LinkFixer
import com.idunnololz.summit.links.LinkResolver
import com.idunnololz.summit.links.ResolvingLinkDialog
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.preview.ImageViewerActivity.Companion.ErrorCustomDownloadLocation
import com.idunnololz.summit.preview.ImageViewerActivityArgs
import com.idunnololz.summit.preview.ImageViewerContract
import com.idunnololz.summit.receiveFIle.ReceiveFileDialogFragment
import com.idunnololz.summit.receiveFIle.ReceiveFileDialogFragmentArgs
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.SharedElementNames
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.SummitActivity
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.launchChangelog
import com.idunnololz.summit.video.ExoPlayerManagerManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : SummitActivity() {

  companion object {
    private val TAG = MainActivity::class.java.simpleName

    private const val ARG_ACCOUNT_FULL_NAME = "ARG_ACCOUNT_FULL_NAME"
    private const val ARG_NOTIFICATION_ID = "ARG_NOTIFICATION_ID"

    fun createInboxItemIntent(context: Context, account: Account, notificationId: Int): Intent {
      return Intent(context, MainActivity::class.java).apply {
        putExtra(ARG_ACCOUNT_FULL_NAME, account.fullName)
        putExtra(ARG_NOTIFICATION_ID, notificationId)
        action = Intent.ACTION_VIEW
      }
    }

    fun createInboxPageIntent(context: Context, account: Account): Intent {
      return Intent(context, MainActivity::class.java).apply {
        putExtra(ARG_ACCOUNT_FULL_NAME, account.fullName)
        action = Intent.ACTION_VIEW
      }
    }
  }

  private lateinit var binding: ActivityMainBinding

  private val viewModel: MainActivityViewModel by viewModels()

  private var communitySelectorController: CommunitySelectorController? = null

  override var currentNavController: NavController? = null

  private var showNotificationBarBg: Boolean = true

  override lateinit var navBarController: NavBarController

  override val context: Context
    get() = this
  override val mainApplication: MainApplication
    get() = application as MainApplication
  override val activity: BaseActivity
    get() = this

  private var currentBottomMenu: BottomMenu? = null

  override var lockUiOpenness = false

  val useBottomNavBar: Boolean
    get() = navBarController.useBottomNavBar

  @Inject
  override lateinit var moreActionsHelper: MoreActionsHelper

  @Inject
  lateinit var themeManager: ThemeManager

  @Inject
  lateinit var userCommunitiesManager: UserCommunitiesManager

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var accountManager: AccountManager

  @Inject
  lateinit var linkFixer: LinkFixer

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var avatarHelper: AvatarHelper

  @Inject
  lateinit var directoryHelper: DirectoryHelper

  @Inject
  lateinit var shakeFeedbackHelper: ShakeFeedbackHelper

  @Inject
  lateinit var exoPlayerManagerManager: ExoPlayerManagerManager

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  private val imageViewerLauncher = registerForActivityResult(
    ImageViewerContract(),
  ) { resultCode ->
    // Handle the returned Uri

    if (resultCode == ErrorCustomDownloadLocation) {
      lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
          showDownloadsSettings()
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()

    WindowCompat.setDecorFitsSystemWindows(window, false)

    super.onCreate(savedInstanceState)

    // Markwon's Coil's imageloader breaks if the activity is destroyed... always recreate oncreate
    lemmyTextHelper.resetMarkwon(this)

    viewModel.communities.observe(this) {
      communitySelectorController?.setCommunities(it)
    }
    moreActionsHelper.downloadAndShareFile.observe(this) {
      if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        return@observe
      }
      when (it) {
        is StatefulData.Error -> {
          ErrorDialogFragment.show(
            getString(R.string.error_unable_to_share_file),
            it.error,
            supportFragmentManager,
          )
        }
        is StatefulData.Loading -> {}
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          showShareSheetForImage(this, it.data)
        }
      }
    }

    binding = ActivityMainBinding.inflate(LayoutInflater.from(this))

    navBarController = NavBarController(
      context = this,
      contentView = binding.contentView,
      lifecycleOwner = this,
      onNavBarChanged = {
        setupNavigationBar()
      },
      getWindowBounds = {
        WindowMetricsCalculator.getOrCreate()
          .computeCurrentWindowMetrics(activity)
          .bounds
      },
    )

    setContentView(binding.root)

    registerInsetsHandler(binding.root)
    registerInsetsHandler()

    viewModel.unreadCount.observe(this) { unreadCount ->
      if (!navBarController.useBottomNavBar) return@observe

      navBarController.navBar.getOrCreateBadge(R.id.inboxTabbedFragment).apply {
        val allUnreads =
          unreadCount.totalUnreadCount +
            unreadCount.totalUnresolvedReportsCount +
            unreadCount.totalUnreadApplicationsCount

        if (allUnreads > 0) {
          isVisible = true
          number = allUnreads
        } else {
          isVisible = false
        }
      }
    }
    viewModel.newActionErrorsCount.observe(this) { count ->
      if (!navBarController.useBottomNavBar) return@observe

      navBarController.navBar.getOrCreateBadge(R.id.youFragment).apply {
        if (count > 0) {
          isVisible = true
          number = count
        } else {
          isVisible = false
        }
      }
    }

    viewModel.currentAccount.observe(this) {
      if (!navBarController.useBottomNavBar) return@observe

      updateUserAvatar()
    }

    runOnReady(this) {
      if (savedInstanceState == null) {
        setupNavigationBar()
      } // Else, need to wait for onRestoreInstanceState

      lifecycleScope.launch(Dispatchers.Default) {
        themeManager.themeOverlayChanged.collect {
          withContext(Dispatchers.Main) {
            recreate()
          }
        }
      }
    }

    // Set up an OnPreDrawListener to the root view.
    val content: View = findViewById(android.R.id.content)
    content.viewTreeObserver.addOnPreDrawListener(
      object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
          // Check whether the initial data is ready.
          return if (viewModel.isReady.value == true) {
            // The content is ready. Start drawing.
            content.viewTreeObserver.removeOnPreDrawListener(this)
            true
          } else {
            // The content isn't ready. Suspend.
            false
          }
        }
      },
    )

    navBarController.setup()

    if (savedInstanceState == null) {
      runOnReady(this) {
        handleIntent(intent) // must be called after navBarController.setup()
      }
    }

    val newInstall = preferences.appVersionLastLaunch == 0
    val isVersionUpdate = isVersionUpdate()
    if (isVersionUpdate) {
      preferences.appVersionLastLaunch = BuildConfig.VERSION_CODE
    }
    if (isVersionUpdate || newInstall) {
      binding.rootView.post {
        onUpdateComplete()
      }
    }

    onPreferencesChanged()

    lifecycleScope.launch {
      preferences.onPreferenceChangeFlow.collect {
        onPreferencesChanged()
      }
    }
  }

  private fun updateUserAvatar() {
    val accountView = viewModel.currentAccount.value

    navBarController.navBar.menu.findItem(R.id.youFragment)?.apply {
      MenuItemCompat.setIconTintMode(this, PorterDuff.Mode.DST)

      if (accountView != null) {
        val account = accountView.account
        avatarHelper.loadAvatar(
          object : Target {
            override fun onSuccess(result: Image) {
              navBarController.navBar.post {
                icon = result.asDrawable(binding.contentView.context.resources)
              }
            }
          },
          imageUrl = accountView.profileImage.toString(),
          personName = account.name,
          personId = account.id,
          personInstance = account.instance,
        )
      } else {
        setIcon(R.drawable.outline_account_circle_24)
      }
    }
  }

  fun getRootView() = binding.rootView

  fun onPreferencesChanged() {
    if (preferences.transparentNotificationBar) {
      binding.notificationBarBgContainer.visibility = View.GONE
    } else {
      binding.notificationBarBgContainer.visibility = View.VISIBLE
    }

    if (preferences.shakeToSendFeedback) {
      shakeFeedbackHelper.start()
    } else {
      shakeFeedbackHelper.stop()
    }

    navBarController.onPreferencesChanged(preferences)

    updateUserAvatar()
  }

  private fun isVersionUpdate(): Boolean {
    val curVersion = BuildConfig.VERSION_CODE
    if (preferences.appVersionLastLaunch == 0) {
      Log.d(TAG, "New install")
      preferences.appVersionLastLaunch = curVersion
      return false
    }
    val prevVersion = preferences.appVersionLastLaunch
    Log.w(TAG, "Current version: $prevVersion New version: $curVersion")
    return prevVersion < curVersion
  }

  private fun onUpdateComplete() {
    Snackbar
      .make(
        binding.snackbarContainer,
        getString(R.string.update_complete_message, BuildConfig.VERSION_NAME),
        5000, // 5s
      )
      .setAction(R.string.changelog) {
        launchChangelog()
      }
      .show()
  }

  override fun onStart() {
    super.onStart()

    viewModel.updateUnreadCount()
  }

  override fun onResume() {
    super.onResume()

    if (preferences.shakeToSendFeedback) {
      shakeFeedbackHelper.start()
    }
  }

  override fun onPause() {
    shakeFeedbackHelper.stop()

    super.onPause()
  }

  override fun onDestroy() {
    super.onDestroy()

    exoPlayerManagerManager.destroyAll()
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)

    runOnReady(this) {
      // Now that BottomNavigationBar has restored its instance state
      // and its selectedItemId, we can proceed with setting up the
      // BottomNavigationBar with Navigation
      setupNavigationBar()
    }
  }

  override fun onSupportNavigateUp(): Boolean = currentNavController?.navigateUp() ?: false

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    val handled = keyPressRegistrationManager.onKeyDown(keyCode, event)
    if (handled) {
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  private fun setupNavigationBar() {
    val navHostFragment =
      supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

    val navController = navHostFragment.navController

    navBarController.setupWithNavController(navController)

    currentNavController = navHostFragment.navController
  }

  private fun registerInsetsHandler() {
    onInsetsChanged = {
      with(it) {
        Log.d(TAG, "Updated insets: top: $topInset bottom: $bottomInset")

        navBarController.onInsetsChanged(
          leftInset = leftInset,
          topInset = topInset,
          rightInset = rightInset,
          bottomInset = bottomInset,
        )

        binding.snackbarContainer.updateLayoutParams<MarginLayoutParams> {
          bottomMargin = navBarController.bottomNavHeight
        }

        onStatusBarHeightChanged(topInset)

        binding.navBarBg.updateLayoutParams<LayoutParams> {
          height = bottomInset
        }

        binding.root.updateLayoutParams<MarginLayoutParams> {
          leftMargin = mainLeftInset
          rightMargin = mainRightInset
        }
      }
    }
  }

  @SuppressLint("MissingSuperCall")
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    runOnReady(this) {
      handleIntent(intent)
    }
  }

  fun showNotificationBarBg() {
    showNotificationBarBg = true
    showNotificationBarBgIfNeeded()
  }

  fun hideNotificationBarBg() {
    showNotificationBarBg = false
    hideNotificationBarBgIfNeeded()
  }

  private fun hideNotificationBarBgIfNeeded() {
    if (showNotificationBarBg) return
    if (binding.notificationBarBg.translationY.toInt() != -binding.notificationBarBg.height) {
      binding.notificationBarBg.animate()
        .setDuration(250)
        .translationY((-binding.notificationBarBg.height).toFloat())
    }
    if (binding.navBarBg.translationY.toInt() != binding.navBarBg.height) {
      binding.navBarBg.animate()
        .setDuration(250)
        .translationY((binding.navBarBg.height).toFloat())
    }
  }

  private fun showNotificationBarBgIfNeeded() {
    if (!showNotificationBarBg) return
    if (binding.notificationBarBg.translationY == 0f &&
      binding.notificationBarBg.visibility == View.VISIBLE
    ) {
      return
    }

    binding.notificationBarBg.visibility = View.VISIBLE
    binding.notificationBarBg.animate()
      .setDuration(250)
      .translationY(0f)
    binding.navBarBg.animate()
      .setDuration(250)
      .translationY(0f)
  }

  private fun handleIntent(intent: Intent?) {
    intent ?: return

    when (intent.action) {
      Intent.ACTION_SEND -> {
        if ("text/plain" == intent.type) {
          handleSendText(intent) // Handle text being sent
        } else if (intent.type?.startsWith("image/") == true) {
          handleSendImage(intent) // Handle single image being sent
        }
      }
      Intent.ACTION_VIEW -> {
        handleViewIntent(intent)
      }
      else -> {
        // Handle other intents, such as being started from the home screen
      }
    }
  }

  private fun handleSendImage(intent: Intent) {
    val fileUri = IntentCompat.getParcelableExtra(
      intent,
      Intent.EXTRA_STREAM,
      Uri::class.java,
    )

    if (fileUri == null) {
      launchAlertDialog("error_send_image") {
        messageResId = R.string.error_unable_to_read_file
      }
      return
    }

    ReceiveFileDialogFragment()
      .apply {
        arguments = ReceiveFileDialogFragmentArgs(
          fileUri = fileUri,
        ).toBundle()
      }
      .show(supportFragmentManager, "CreateOrEditPostFragment")
  }

  private fun handleSendText(intent: Intent) {
    val account = accountManager.currentAccount.asAccount

    if (account == null) {
      launchAlertDialog("error_send_text") {
        messageResId = R.string.error_you_must_sign_in_to_create_a_post
      }
      return
    }

    CreateOrEditPostFragment()
      .apply {
        arguments = CreateOrEditPostFragmentArgs(
          account.instance,
          null,
          null,
          null,
          extraText = intent.getStringExtra(Intent.EXTRA_TEXT),
        ).toBundle()
      }
      .show(supportFragmentManager, "CreateOrEditPostFragment")
  }

  private fun handleViewIntent(intent: Intent) {
    Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString()}")
    if (intent.hasExtra(ARG_ACCOUNT_FULL_NAME)) {
      val direction = MainDirections.actionGlobalInboxTabbedFragment(
        notificationId = intent.getIntExtra(ARG_NOTIFICATION_ID, 0),
        refresh = true,
      )

      runOnReady(this) {
        currentNavController?.let { currentNavController ->
          navBarController.navigateWithArguments(
            R.id.inboxTabbedFragment,
            currentNavController,
            direction.arguments,
          )
        }
      }
      return
    }

    val data = intent.data ?: return
    val page = LinkResolver.parseUrl(
      url = data.toString(),
      currentInstance = viewModel.currentInstance,
      mustHandle = true,
    )

    if (page == null) {
      Log.d(TAG, "Unable to handle uri $data")

      launchAlertDialog("error") {
        message = getString(R.string.error_unable_to_handle_link, data.toString())
      }
    } else {
      runOnReady(this) {
        launchPage(page)
      }
    }
  }

  override fun launchPage(
    page: PageRef,
    switchToNativeInstance: Boolean,
    preferMainFragment: Boolean,
  ) {
    Log.d(TAG, "launchPage(): $page")

    val pageRefResult = linkFixer.fixPageRefSync(page)

    if (pageRefResult.isSuccess) {
      launchPageInternal(
        page = pageRefResult.getOrNull() ?: page,
        switchToNativeInstance = switchToNativeInstance,
        preferMainFragment = preferMainFragment,
      )
      return
    }

    var job: Job? = null

    val alertDialog = ResolvingLinkDialog.show(this@MainActivity)
      ?.apply {
        setOnCancelListener {
          job?.cancel()
        }
      }

    job = lifecycleScope.launch {
      val pageRef = linkFixer.fixPageRef(page) ?: page

      withContext(Dispatchers.Main) {
        alertDialog?.dismiss()
        launchPageInternal(pageRef, switchToNativeInstance, preferMainFragment)
      }
    }
  }

  private fun launchPageInternal(
    page: PageRef,
    switchToNativeInstance: Boolean = false,
    preferMainFragment: Boolean = false,
  ) {
    Log.d(TAG, "launchPageInternal(): $page")

    val isMainFragment = navBarController.navBar.selectedItemId == R.id.mainFragment &&
      currentNavController?.currentDestination?.id == R.id.mainFragment

    fun handleWithMainFragment() {
      if (navBarController.navBar.selectedItemId != R.id.mainFragment) {
        navBarController.navBar.selectedItemId = R.id.mainFragment
      }
      executeWhenMainFragmentAvailable { mainFragment ->
        mainFragment.navigateToPage(page, switchToNativeInstance)
      }
    }

    if (isMainFragment || preferMainFragment) {
      handleWithMainFragment()
      return
    }

    when (page) {
      is CommunityRef -> {
        currentNavController?.navigate(
          R.id.action_global_community,
          CommunityFragmentArgs(
            communityRef = page,
            tab = null,
          ).toBundle(),
        )
      }
      is PostRef -> {
        currentNavController?.navigate(
          R.id.postFragment2,
          PostFragmentArgs(
            instance = page.instance,
            id = page.id,
            currentCommunity = null,
            isSinglePage = true,
            switchToNativeInstance = switchToNativeInstance,
          ).toBundle(),
          NavOptions.Builder()
            .setEnterAnim(androidx.navigation.ui.R.animator.nav_default_enter_anim)
            .setExitAnim(androidx.navigation.ui.R.animator.nav_default_exit_anim)
            .setPopEnterAnim(
              androidx.navigation.ui.R.animator.nav_default_pop_enter_anim,
            )
            .setPopExitAnim(androidx.navigation.ui.R.animator.nav_default_pop_exit_anim)
            .build(),
        )
      }
      is CommentRef -> {
        currentNavController?.navigate(
          R.id.postFragment2,
          PostFragmentArgs(
            instance = page.instance,
            id = 0,
            commentId = page.id,
            currentCommunity = null,
            isSinglePage = true,
          ).toBundle(),
          NavOptions.Builder()
            .setEnterAnim(androidx.navigation.ui.R.animator.nav_default_enter_anim)
            .setExitAnim(androidx.navigation.ui.R.animator.nav_default_exit_anim)
            .setPopEnterAnim(
              androidx.navigation.ui.R.animator.nav_default_pop_enter_anim,
            )
            .setPopExitAnim(androidx.navigation.ui.R.animator.nav_default_pop_exit_anim)
            .build(),
        )
      }
      is PersonRef -> {
        val direction = MainDirections.actionGlobalPersonTabbedFragment2(page)
        currentNavController?.navigateSafe(direction)
      }
    }
  }

  fun launchModLogs(instance: String, filterByMod: PersonRef) {
    val direction = MainDirections.actionGlobalModLogsFragment(
      instance, null, filterByMod = filterByMod)
    currentNavController?.navigateSafe(direction)
  }

  fun launchViewVotes(postId: PostId, commentId: CommentId) {
    val direction = MainDirections.actionGlobalViewVotesFragment(
      postId = postId.toLong(),
      commentId = commentId.toLong(),
    )
    currentNavController?.navigateSafe(direction)
  }

  private fun executeWhenMainFragmentAvailable(fn: (MainFragment) -> Unit) {
    val navigateToPageRunnable = object : Runnable {
      override fun run() {
        val navHostFragment =
          supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            as? NavHostFragment
        val currentFragment = navHostFragment
          ?.childFragmentManager
          ?.fragments
          ?.getOrNull(0)
        if (currentFragment is MainFragment) {
          fn(currentFragment)
        } else {
          // super hacky :x
          navBarController.navBar.post(this)
        }
      }
    }

    navigateToPageRunnable.run()
  }

  private fun onStatusBarHeightChanged(statusBarHeight: Int) {
    val lp = binding.notificationBarBg.layoutParams
    if (lp.height != statusBarHeight) {
      Log.d(TAG, "New status bar height: $statusBarHeight")
      lp.height = statusBarHeight
      binding.notificationBarBg.layoutParams = lp

      if (!showNotificationBarBg) {
        binding.notificationBarBg.visibility = View.INVISIBLE
      }
    }
  }

  private fun createOrGetCommunitySelectorController(): CommunitySelectorController =
    communitySelectorController
      ?: viewModel.communitySelectorControllerFactory.create(
        context = this,
        viewModel = viewModel,
        viewLifecycleOwner = this,
      ).also {
        it.setup(this, binding.root)

        communitySelectorController = it
      }

  override fun showCommunitySelector(
    currentCommunityRef: CommunityRef?,
  ): CommunitySelectorController {
    val communitySelectorController = createOrGetCommunitySelectorController()

    viewModel.communities.value.let {
      communitySelectorController.setCommunities(it)
      communitySelectorController.setCurrentCommunity(currentCommunityRef)
      communitySelectorController.onCommunityInfoClick = { ref ->
        showCommunityInfo(ref)
      }
    }

    communitySelectorController.show(
      bottomSheetContainer = binding.root,
      this,
    )

    return communitySelectorController
  }

  override fun insetViewAutomaticallyByPaddingAndNavUi(
    lifecycleOwner: LifecycleOwner,
    rootView: View,
    applyLeftInset: Boolean,
    applyTopInset: Boolean,
    applyRightInset: Boolean,
    applyBottomInset: Boolean,
  ) {
    insets.observe(lifecycleOwner) {
      rootView.post {
        var bottomPadding = getBottomNavHeight()
        if (bottomPadding == 0) {
          bottomPadding = it.bottomInset
        }

        rootView.setPadding(
          if (applyLeftInset) {
            navBarController.newLeftInset
          } else {
            rootView.paddingLeft
          },
          if (applyTopInset) {
            it.topInset
          } else {
            rootView.paddingTop
          },
          if (applyRightInset) {
            navBarController.newRightInset
          } else {
            rootView.paddingRight
          },
          if (applyBottomInset) {
            bottomPadding
          } else {
            rootView.paddingBottom
          },
        )
      }
    }
  }

  override fun insetViewAutomaticallyByMarginsAndNavUi(
    lifecycleOwner: LifecycleOwner,
    view: View,
    applyLeftInset: Boolean,
    applyTopInset: Boolean,
    applyRightInset: Boolean,
    applyBottomInset: Boolean,
  ) {
    insets.observe(lifecycleOwner) {
      var bottomPadding = getBottomNavHeight()
      if (bottomPadding == 0) {
        bottomPadding = it.bottomInset
      }

      view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        if (applyLeftInset) {
          leftMargin = navBarController.newLeftInset
        }
        if (applyTopInset) {
          topMargin = it.topInset
        }
        if (applyRightInset) {
          rightMargin = navBarController.newRightInset
        }
        if (applyBottomInset) {
          bottomMargin = bottomPadding
        }
      }
    }
  }

  fun getSnackbarContainer(): View = binding.snackbarContainer

  override fun runOnReady(lifecycleOwner: LifecycleOwner, cb: () -> Unit) {
    viewModel.isReady.observe(lifecycleOwner) {
      if (it == true) {
        cb()
      }
    }
  }

  fun runWhenLaidOut(cb: () -> Unit) {
    if (binding.root.height == 0) {
      binding.root.viewTreeObserver.addOnPreDrawListener(
        object : ViewTreeObserver.OnPreDrawListener {
          override fun onPreDraw(): Boolean {
            binding.root.viewTreeObserver.removeOnPreDrawListener(this)

            cb()
            return false
          }
        },
      )
      return
    }
    cb()
  }

  override fun showBottomMenu(bottomMenu: BottomMenu, expandFully: Boolean) {
    currentBottomMenu?.close()
    bottomMenu.show(
      bottomMenuContainer = this,
      bottomSheetContainer = binding.root,
      expandFully = expandFully,
      avatarHelper = avatarHelper,
    )
    currentBottomMenu = bottomMenu
  }

  override fun openImage(
    sharedElement: View?,
    appBar: View?,
    title: String?,
    url: String,
    mimeType: String?,
    urlAlt: String?,
    mimeTypeAlt: String?,
  ) {
    val transitionName =
      if (animationsHelper.shouldAnimate(AnimationsHelper.AnimationLevel.Extras)) {
        sharedElement?.transitionName
      } else {
        null
      }

    val args = ImageViewerActivityArgs(
      title = title,
      url = url,
      mimeType = mimeType,
      urlAlt = urlAlt,
      mimeTypeAlt = mimeTypeAlt,
      transitionName = transitionName,
    )

    if (transitionName != null) {
      val sharedElements = mutableListOf<Pair<View, String>>()
      sharedElements += Pair.create(sharedElement, transitionName)
      if (appBar != null) {
        sharedElements += Pair.create(appBar, SharedElementNames.APP_BAR)
      }

      if (navBarController.useBottomNavBar && !navBarController.useNavigationRail) {
        sharedElements += Pair.create(navBarController.navBar, SharedElementNames.NAV_BAR)
      }

      val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
        this,
        *sharedElements.toTypedArray(),
      )
      imageViewerLauncher.launch(args, options)
    } else {
      imageViewerLauncher.launch(args)
    }
  }

  fun openAllUserTagsFragment() {
    val direction = MainDirections.actionGlobalUserTagsFragment()
    currentNavController?.navigateSafe(direction)
  }

  override fun showCommunityInfo(communityRef: CommunityRef) {
    if (communityRef is CommunityRef.MultiCommunity) {
      val userCommunityItem = userCommunitiesManager.getAllUserCommunities()
        .firstOrNull { it.communityRef == communityRef }
      if (userCommunityItem != null) {
        val navHostFragment =
          supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            as? NavHostFragment
        val currentFragment = navHostFragment
          ?.childFragmentManager
          ?.fragments
          ?.getOrNull(0)

        currentFragment?.childFragmentManager?.let { childFragmentManager ->
          MultiCommunityEditorDialogFragment.show(
            childFragmentManager,
            userCommunityItem.communityRef as CommunityRef.MultiCommunity,
            userCommunityItem.id,
          )
        }
      }
    } else {
      val direction = MainDirections.actionGlobalCommunityInfoFragment(communityRef)
      currentNavController?.navigateSafe(direction)
    }
  }

  fun showCreateCommunityScreen() {
    val direction = MainDirections.actionGlobalCreateOrEditCommunityFragment(null)
    currentNavController?.navigateSafe(direction)
  }

  val useNavigationRail
    get() = navBarController.useNavigationRail

  fun navigateTopLevel(menuId: Int) {
    val currentNavController = currentNavController ?: return
    val menuItem = navBarController.navBar.menu.findItem(menuId)

    if (menuItem == null) {
      navBarController.navigateWithArguments(
        destinationId = menuId,
        isTopLevel = true,
        navController = currentNavController,
      )
    } else {
      NavigationUI.onNavDestinationSelected(menuItem, currentNavController)
    }
  }

  suspend fun requestScreenshot(): File {
    val screenshot = directoryHelper.getFeedbackScreenshotFile()

    if (screenshot.exists()) {
      screenshot.delete()
    }

    screenshot.parentFile?.mkdirs()

    val bitmap: Bitmap
    withContext(Dispatchers.Main) {
      val rootView = binding.root
      bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      rootView.draw(canvas)
    }

    screenshot.outputStream().use {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
    }

    return screenshot
  }

  fun requestScreenshotAndShowFeedbackScreen() {
    lifecycleScope.launch {
      delay(500)
      requestScreenshot()

      PostFeedbackDialogFragment.show(supportFragmentManager)
    }
  }
}
