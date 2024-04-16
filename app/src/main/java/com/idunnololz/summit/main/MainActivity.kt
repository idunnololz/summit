package com.idunnololz.summit.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.webkit.MimeTypeMap
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Pair
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.MainApplication
import com.idunnololz.summit.MainDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.fullName
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.ActivityMainBinding
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.CommunityFragmentArgs
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityEditorDialogFragment
import com.idunnololz.summit.lemmy.post.PostFragmentArgs
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.preview.ImageViewerActivity.Companion.ErrorCustomDownloadLocation
import com.idunnololz.summit.preview.ImageViewerActivityArgs
import com.idunnololz.summit.preview.ImageViewerContract
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.receiveFIle.ReceiveFileDialogFragment
import com.idunnololz.summit.receiveFIle.ReceiveFileDialogFragmentArgs
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.BottomMenuContainer
import com.idunnololz.summit.util.InsetsHelper
import com.idunnololz.summit.util.InsetsProvider
import com.idunnololz.summit.util.KeyPressRegistrationManager
import com.idunnololz.summit.util.SharedElementNames
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.launchChangelog
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity :
    BaseActivity(),
    BottomMenuContainer,
    InsetsProvider by InsetsHelper(
        consumeInsets = true,
    ) {

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

    private var currentNavController: NavController? = null

    private var showNotificationBarBg: Boolean = true

    internal lateinit var navBarController: NavBarController

    val keyPressRegistrationManager = KeyPressRegistrationManager()

    override val context: Context
        get() = this
    override val mainApplication: MainApplication
        get() = application as MainApplication

    private val onNavigationItemReselectedListeners =
        mutableListOf<NavigationBarView.OnItemReselectedListener>()

    private var currentBottomMenu: BottomMenu? = null

    var lockUiOpenness = false

    val useBottomNavBar: Boolean
        get() = navBarController.useBottomNavBar

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var accountManager: AccountManager

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
        LemmyTextHelper.resetMarkwon(this)

        viewModel.communities.observe(this) {
            communitySelectorController?.setCommunities(it)
        }
        moreActionsHelper.downloadAndShareFile.observe(this) {
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                return@observe
            }
            when (it) {
                is StatefulData.Error -> {}
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(it.data.toString())
                        ?: "image/jpeg"

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, it.data)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }
            }
        }

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))

        navBarController = NavBarController(
            activity = this,
            contentView = binding.contentView,
            lifecycleOwner = this,
            onNavBarChanged = {
                setupNavigationBar()
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
                        unreadCount.totalUnresolvedReportsCount

                if (allUnreads > 0) {
                    isVisible = true
                    number = allUnreads
                } else {
                    isVisible = false
                }
            }
        }

        runOnReady(this) {
            viewModel.loadCommunities()

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

        onPreferencesChanged()

        navBarController.setup()

        if (savedInstanceState == null) {
            handleIntent(intent) // must be called after navBarController.setup()
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
    }

    fun onPreferencesChanged() {
        if (preferences.transparentNotificationBar) {
            binding.notificationBarBgContainer.visibility = View.GONE
        } else {
            binding.notificationBarBgContainer.visibility = View.VISIBLE
        }

        navBarController.onPreferencesChanged(preferences)
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

    override fun onResume() {
        super.onResume()

        viewModel.updateUnreadCount()
    }

    override fun onDestroy() {
        super.onDestroy()

        ExoPlayerManager.destroyAll()
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

        navBarController.navBar.setupWithNavController(navController)

        navBarController.navBar.setOnItemReselectedListener { menuItem ->
            Log.d(TAG, "Reselected nav item: ${menuItem.itemId}")

            onNavigationItemReselectedListeners.forEach {
                it.onNavigationItemReselected(menuItem)
            }
        }

        currentNavController = navHostFragment.navController
    }

    fun registerOnNavigationItemReselectedListener(
        onNavigationItemReselectedListener: NavigationBarView.OnItemReselectedListener,
    ) {
        onNavigationItemReselectedListeners.add(onNavigationItemReselectedListener)
    }

    fun unregisterOnNavigationItemReselectedListener(
        onNavigationItemReselectedListener: NavigationBarView.OnItemReselectedListener,
    ) {
        onNavigationItemReselectedListeners.remove(onNavigationItemReselectedListener)
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
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleIntent(intent)
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

    fun setNavUiOpenPercent(progress: Float) {
        if (lockUiOpenness) return
        navBarController.bottomNavViewAnimationOffsetPercent.value = progress
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
            AlertDialogFragment.Builder()
                .setMessage(R.string.error_unable_to_read_file)
                .createAndShow(supportFragmentManager, "asdf")
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
            AlertDialogFragment.Builder()
                .setMessage(R.string.error_you_must_sign_in_to_create_a_post)
                .createAndShow(supportFragmentManager, "asdf")
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
        Log.d("notification", "Intent extras: ${intent.extras?.keySet()?.joinToString()}")
        if (intent.hasExtra(ARG_ACCOUNT_FULL_NAME)) {
            val direction = MainDirections.actionGlobalInboxTabbedFragment(
                intent.getIntExtra(ARG_NOTIFICATION_ID, 0),
            )

            runOnReady(this) {
                currentNavController?.navigate(direction)
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

            AlertDialogFragment.Builder()
                .setMessage(getString(R.string.error_unable_to_handle_link, data.toString()))
                .createAndShow(supportFragmentManager, "error")
        } else {
            launchPage(page)
        }
    }

    fun launchPage(
        page: PageRef,
        switchToNativeInstance: Boolean = false,
        preferMainFragment: Boolean = false,
    ) {
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
                        null,
                        page,
                    ).toBundle(),
                )
            }
            is PostRef -> {
                currentNavController?.navigate(
                    R.id.postFragment2,
                    PostFragmentArgs(
                        page.instance,
                        page.id,
                        null,
                        isSinglePage = true,
                        switchToNativeInstance = switchToNativeInstance,
                    ).toBundle(),
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
                )
            }
            is PersonRef -> {
                val direction = MainDirections.actionGlobalPersonTabbedFragment2(page)
                currentNavController?.navigateSafe(direction)
            }
        }
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

    private fun createOrGetSubredditSelectorController(): CommunitySelectorController =
        communitySelectorController
            ?: viewModel.communitySelectorControllerFactory.create(
                context = this,
                viewModel = viewModel,
                viewLifecycleOwner = this,
            ).also {
                it.setup(this, binding.root)

                communitySelectorController = it
            }

    fun showCommunitySelector(
        currentCommunityRef: CommunityRef? = null,
    ): CommunitySelectorController {
        val communitySelectorController = createOrGetSubredditSelectorController()

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

    fun insetViewAutomaticallyByPaddingAndNavUi(
        lifecycleOwner: LifecycleOwner,
        rootView: View,
        applyLeftInset: Boolean = true,
        applyTopInset: Boolean = true,
        applyRightInset: Boolean = true,
        applyBottomInset: Boolean = true,
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

    fun insetViewAutomaticallyByMarginAndNavUi(
        lifecycleOwner: LifecycleOwner,
        rootView: View,
        applyTopInset: Boolean = true,
        additionalPaddingBottom: Int = 0,
    ) {
        insets.observe(lifecycleOwner) {
            rootView.post {
                var bottomPadding = getBottomNavHeight()
                if (bottomPadding == 0) {
                    bottomPadding = it.bottomInset
                }

                rootView.updateLayoutParams<MarginLayoutParams> {
                    leftMargin = navBarController.newLeftInset
                    topMargin = if (applyTopInset) {
                        it.topInset
                    } else {
                        this.topMargin
                    }
                    rightMargin = navBarController.newRightInset
                    bottomMargin = bottomPadding + additionalPaddingBottom
                }
            }
        }
    }

    fun insetViewExceptTopAutomaticallyByPaddingAndNavUi(
        lifecycleOwner: LifecycleOwner,
        rootView: View,
        additionalPaddingBottom: Int = 0,
    ) {
        insets.observe(lifecycleOwner) { insets ->
            rootView.post {
                var bottomPadding = getBottomNavHeight()
                if (bottomPadding == 0) {
                    bottomPadding = insets.bottomInset
                }

                rootView.updatePadding(
                    left = insets.leftInset,
                    right = insets.rightInset,
                    bottom = bottomPadding + additionalPaddingBottom,
                )
            }
        }
    }

    fun getSnackbarContainer(): View = binding.snackbarContainer
    fun getBottomNavHeight() = navBarController.bottomNavHeight

    fun runOnReady(lifecycleOwner: LifecycleOwner, cb: () -> Unit) {
        viewModel.isReady.observe(lifecycleOwner) {
            if (it == true) {
                cb()
            }
        }
    }

    fun showBottomNav() {
        navBarController.showBottomNav()
    }

    fun hideBottomNav() {
        navBarController.hideNavBar(animate = true)
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
        )
        currentBottomMenu = bottomMenu
    }

    fun openImage(
        sharedElement: View?,
        appBar: View?,
        title: String?,
        url: String,
        mimeType: String?,
    ) {
        val transitionName = sharedElement?.transitionName

        val args = ImageViewerActivityArgs(title, url, mimeType, transitionName)

        if (transitionName != null) {
            val sharedElements = mutableListOf<Pair<View, String>>()
            sharedElements += Pair.create(sharedElement, transitionName)
            if (appBar != null) {
                sharedElements += Pair.create(appBar, SharedElementNames.AppBar)
            }

            if (navBarController.useBottomNavBar && !navBarController.useNavigationRail) {
                sharedElements += Pair.create(
                    navBarController.navBar,
                    SharedElementNames.NavBar,
                )
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

    fun showCommunityInfo(communityRef: CommunityRef) {
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

    fun navigateTopLevel(menuId: Int) {
        val currentNavController = currentNavController ?: return
        val menuItem = navBarController.navBar.menu.findItem(menuId) ?: return
        NavigationUI.onNavDestinationSelected(menuItem, currentNavController)
    }

    fun showDownloadsSettings() {
        val direction = MainDirections.actionGlobalSettingsFragment("downloads")
        currentNavController?.navigateSafe(direction)
    }
}
