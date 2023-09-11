package com.idunnololz.summit.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
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
import android.widget.TextView
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.MainDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.ActivityMainBinding
import com.idunnololz.summit.history.HistoryFragment
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.community.CommunityFragmentArgs
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoFragment
import com.idunnololz.summit.lemmy.inbox.InboxTabbedFragment
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityEditorDialogFragment
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.lemmy.person.PersonTabbedFragmentArgs
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.post.PostFragmentArgs
import com.idunnololz.summit.login.LoginFragment
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.preview.ImageViewerActivity
import com.idunnololz.summit.preview.ImageViewerActivityArgs
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.preview.VideoViewerFragment
import com.idunnololz.summit.saved.SavedTabbedFragment
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.cache.SettingCacheFragment
import com.idunnololz.summit.user.TabCommunityState
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.KeyPressRegistrationManager
import com.idunnololz.summit.util.SharedElementNames
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.reflect.KClass

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var binding: ActivityMainBinding

    val bottomNavViewOffset = MutableLiveData<Int>(0)
    val bottomNavViewAnimationOffset = MutableLiveData<Float>(0f)
    val windowInsets = MutableLiveData<Rect>(Rect())

    private var enableBottomNavViewScrolling = false

    private val viewModel: MainActivityViewModel by viewModels()

    private var communitySelectorController: CommunitySelectorController? = null

    private var currentNavController: NavController? = null

    private var showNotificationBarBg: Boolean = true

    val keyPressRegistrationManager = KeyPressRegistrationManager()

    val insetsChangedLiveData = MutableLiveData<Int>()

    private val onNavigationItemReselectedListeners =
        mutableListOf<NavigationBarView.OnItemReselectedListener>()

    private var currentBottomMenu: BottomMenu? = null
    var lastInsets: MainActivityInsets = MainActivityInsets()

    var lockUiOpenness = false

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    @Inject
    lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        // Markwon's Coil's imageloader breaks if the activity is destroyed... always recreate oncreate
        LemmyTextHelper.resetMarkwon(this)

        viewModel.communities.observe(this) {
            communitySelectorController?.setCommunities(it)
        }

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        registerInsetsHandler()

        fun updateBottomTranslationY() {
            binding.bottomNavigationView.translationY = bottomNavY
        }

        bottomNavViewOffset.observe(this) {
            updateBottomTranslationY()
        }
        bottomNavViewAnimationOffset.observe(this) {
            updateBottomTranslationY()
        }

        viewModel.unreadCount.observe(this) { unreadCount ->
            binding.bottomNavigationView.getOrCreateBadge(R.id.inboxTabbedFragment).apply {
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

        handleIntent(intent)

        runOnReady(this) {
            viewModel.loadCommunities()

            if (savedInstanceState == null) {
                setupBottomNavigationBar()
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

    fun onUpdateComplete() {
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

    fun launchChangelog() {
        launchPage(PostRef("lemmy.world", 4851929), switchToNativeInstance = true)
    }

    private val bottomNavY
        get() = bottomNavViewOffset.value!!.toFloat() + bottomNavViewAnimationOffset.value!!.toFloat()

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
            setupBottomNavigationBar()
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        currentNavController?.navigateUp() ?: false

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val handled = keyPressRegistrationManager.onKeyDown(keyCode, event)
        if (handled) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupBottomNavigationBar() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        binding.bottomNavigationView.setOnItemReselectedListener { menuItem ->
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarsInsets = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars(),
            )
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()) // keyboard
            val imeHeight = imeInsets.bottom
            val topInset = systemBarsInsets.top
            val bottomInset = max(systemBarsInsets.bottom, imeHeight)
            val leftInset = systemBarsInsets.left
            val rightInset = systemBarsInsets.right

            lastInsets = MainActivityInsets(
                imeHeight = imeHeight,
                topInset = topInset,
                bottomInset = bottomInset,
                leftInset = leftInset,
                rightInset = rightInset,
            )

            Log.d(TAG, "Updated insets: top: $topInset bottom: $bottomInset")

            binding.bottomNavigationView.layoutParams =
                (binding.bottomNavigationView.layoutParams as MarginLayoutParams).apply {
                    leftMargin = leftInset
                    rightMargin = rightInset
                }

            // Move our RecyclerView below toolbar + 10dp
            windowInsets.value = checkNotNull(windowInsets.value).apply {
                left = leftInset
                top = topInset
                right = rightInset
                bottom = bottomInset
            }
            binding.bottomNavigationView.layoutParams =
                (binding.bottomNavigationView.layoutParams as MarginLayoutParams).apply {
                    leftMargin = leftInset
                    rightMargin = rightInset
                }
            binding.bottomNavigationView.updatePadding(bottom = bottomInset)
            binding.snackbarContainer.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = binding.bottomNavigationView.height
            }

            onStatusBarHeightChanged(topInset)

            binding.navBarBg.updateLayoutParams<LayoutParams> {
                height = bottomInset
            }

            insetsChangedLiveData.postValue(0)

            currentBottomMenu?.setInsets(lastInsets.topInset, lastInsets.bottomInset)

            WindowInsetsCompat.CONSUMED
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun showNotificationBarBg() {
        showNotificationBarBg = true
        showNotificationBarBgIfNeeded()
    }

    private fun hideNotificationBarBg() {
        showNotificationBarBg = false
        hideNotificationBarBgIfNeeded()
    }

    private fun hideNotificationBarBgIfNeeded() {
        if (showNotificationBarBg) return
        binding.notificationBarBg.animate()
            .setDuration(250)
            .translationY((-binding.notificationBarBg.height).toFloat())
        binding.navBarBg.animate()
            .setDuration(250)
            .translationY((binding.navBarBg.height).toFloat())
    }

    private fun showNotificationBarBgIfNeeded() {
        if (!showNotificationBarBg) return
        if (binding.notificationBarBg.translationY == 0f && binding.notificationBarBg.visibility == View.VISIBLE) return
        binding.notificationBarBg.visibility = View.VISIBLE
        binding.notificationBarBg.animate()
            .setDuration(250)
            .translationY(0f)
        binding.navBarBg.animate()
            .setDuration(250)
            .translationY(0f)
    }

    private fun showBottomNavWithScrollBehavior() {
        showBottomNav()
        binding.bottomNavigationView.layoutParams =
            (binding.bottomNavigationView.layoutParams as CoordinatorLayout.LayoutParams).apply {
                behavior = HideBottomViewOnScrollBehavior<BottomNavigationView>()
            }
    }

    fun enableBottomNavViewScrolling() {
        enableBottomNavViewScrolling = true
        binding.bottomNavigationView.visibility = View.VISIBLE
    }

    fun disableBottomNavViewScrolling() {
        enableBottomNavViewScrolling = false
    }

    fun setNavUiOpenness(progress: Float) {
        if (lockUiOpenness) return
        bottomNavViewAnimationOffset.value = binding.bottomNavigationView.height * progress
    }

    fun showBottomNav(supportOpenness: Boolean = false) {
        if (enableBottomNavViewScrolling && binding.bottomNavigationView.visibility == View.VISIBLE) {
            return
        }

        val finalY =
            if (supportOpenness) {
                bottomNavY
            } else {
                0f
            }

        bottomNavViewOffset.value = 0
        if (binding.bottomNavigationView.visibility != View.VISIBLE) {
            binding.bottomNavigationView.visibility = View.VISIBLE
            binding.bottomNavigationView.translationY =
                binding.bottomNavigationView.height.toFloat()
            Log.d(TAG, "bottomNavigationView.height: ${binding.bottomNavigationView.height}")

            binding.bottomNavigationView.post {
                binding.bottomNavigationView.animate()
                    .translationY(finalY).duration = 250
            }
        } else {
            if (binding.bottomNavigationView.translationY == 0f) return

            binding.bottomNavigationView.animate()
                .translationY(finalY).duration = 250
        }
    }

    fun hideBottomNav(animate: Boolean) {
        Log.d(TAG, "bottomNavigationView.height: ${binding.bottomNavigationView.height}")

        if (animate) {
            binding.bottomNavigationView.animate()
                .translationY(binding.bottomNavigationView.height.toFloat())
                .setDuration(250)
        } else {
            binding.bottomNavigationView.translationY =
                binding.bottomNavigationView.height.toFloat()
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return

        val data = intent.data ?: return
        val page = LinkResolver.parseUrl(data.toString(), viewModel.currentInstance, mustHandle = true)

        if (page == null) {
            Log.d(TAG, "Unable to handle uri $data")

            AlertDialogFragment.Builder()
                .setMessage(getString(R.string.error_unable_to_handle_link, data.toString()))
                .createAndShow(supportFragmentManager, "error")
        } else {
            launchPage(page)
        }

//        if (LinkResolver.isLemmyUrl(data)) {
//            if (RedditUtils.isUriGallery(data) && data.pathSegments.size >= 2) {
//                RedirectHandlerDialogFragment.newInstance(LinkUtils.getRedirectLink(data.pathSegments[1]))
//                    .show(supportFragmentManager, "asd")
//            } else if (RedditUtils.isUriRedirect(data)) {
//                RedirectHandlerDialogFragment.newInstance(data.toString())
//                    .show(supportFragmentManager, "asd")
//            } else {
//                if (binding.bottomNavigationView.selectedItemId != R.id.main) {
//                    binding.bottomNavigationView.selectedItemId = R.id.main
//                }
//                executeWhenMainFragmentAvailable { mainFragment ->
//                    mainFragment.navigateToUri(data)
//                }
//            }
//        }
    }

    fun launchPage(page: PageRef, switchToNativeInstance: Boolean = false) {
        val isMainFragment = binding.bottomNavigationView.selectedItemId == R.id.mainFragment &&
            currentNavController?.currentDestination?.id == R.id.mainFragment

        fun handleWithMainFragment() {
            if (binding.bottomNavigationView.selectedItemId != R.id.mainFragment) {
                binding.bottomNavigationView.selectedItemId = R.id.mainFragment
            }
            executeWhenMainFragmentAvailable { mainFragment ->
                mainFragment.navigateToPage(page, switchToNativeInstance)
            }
        }

        if (isMainFragment) {
            handleWithMainFragment()
            return
        }

        when (page) {
            is CommunityRef -> {
//                handleWithMainFragment()

                currentNavController?.navigate(
                    R.id.action_global_community,
                    CommunityFragmentArgs(
                        null,
                        page
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
                currentNavController?.navigate(
                    R.id.personTabbedFragment2,
                    PersonTabbedFragmentArgs(
                        page,
                    ).toBundle(),
                )
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
                    binding.bottomNavigationView.post(this)
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
                it.inflate(binding.bottomSheetContainer)
                it.setCommunities(viewModel.communities.value)

                communitySelectorController = it
            }

    fun showCommunitySelector(currentCommunityRef: CommunityRef? = null): CommunitySelectorController {
        val communitySelectorController = createOrGetSubredditSelectorController()

        viewModel.communities.value.let {
            communitySelectorController.setCommunities(it)
            communitySelectorController.setCurrentCommunity(currentCommunityRef)
            communitySelectorController.onCommunityInfoClick = { ref ->
                showCommunityInfo(ref)
            }
        }

        communitySelectorController.show(binding.bottomSheetContainer, this, this)

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
    fun showSystemUI(animate: Boolean) {
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

    fun insetViewAutomaticallyByMargins(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val lp = rootView.layoutParams as MarginLayoutParams
            val insets = checkNotNull(windowInsets.value)

            lp.topMargin = insets.top
            lp.bottomMargin = insets.bottom
            lp.leftMargin = insets.left
            lp.rightMargin = insets.right
            rootView.requestLayout()
        }
    }

    fun insetViewExceptBottomAutomaticallyByMargins(lifecycleOwner: LifecycleOwner, view: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = checkNotNull(windowInsets.value)

            view.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                rightMargin = insets.right
            }
        }
    }

    fun insetViewExceptTopAutomaticallyByPadding(
        lifecycleOwner: LifecycleOwner,
        rootView: View,
        additionalPaddingBottom: Int = 0,
    ) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.setPadding(
                insets.leftInset,
                0,
                insets.rightInset,
                insets.bottomInset + additionalPaddingBottom,
            )
        }
    }

    fun insetViewExceptBottomAutomaticallyByPadding(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.setPadding(
                insets.leftInset,
                insets.topInset,
                insets.rightInset,
                0,
            )
        }
    }

    fun insetViewExceptTopAutomaticallyByMargins(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottomInset
                leftMargin = insets.leftInset
                rightMargin = insets.rightInset
            }
        }
    }

    fun insetViewAutomaticallyByPaddingAndNavUi(
        lifecycleOwner: LifecycleOwner,
        rootView: View,
        additionalPaddingBottom: Int = 0,
    ) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.setPadding(
                insets.leftInset,
                insets.topInset,
                insets.rightInset,
                getBottomNavHeight() + additionalPaddingBottom,
            )
        }
    }

    fun insetViewExceptTopAutomaticallyByPaddingAndNavUi(
        lifecycleOwner: LifecycleOwner,
        rootView: View,
        additionalPaddingBottom: Int = 0,
    ) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.setPadding(
                insets.leftInset,
                0,
                insets.rightInset,
                getBottomNavHeight() + additionalPaddingBottom,
            )
        }
    }

    fun insetViewExceptTopAutomaticallyByMarginAndNavUi(
        lifecycleOwner: LifecycleOwner,
        rootView: View,
        additionalPaddingBottom: Int = 0,
    ) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = getBottomNavHeight() + additionalPaddingBottom
                leftMargin = insets.leftInset
                rightMargin = insets.rightInset
            }
        }
    }

    fun insetViewAutomaticallyByPadding(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.setPadding(
                insets.leftInset,
                insets.topInset,
                insets.rightInset,
                insets.bottomInset,
            )
        }
    }

    fun restoreTabState(state: TabCommunityState?) {
        state ?: return
        if (binding.bottomNavigationView.selectedItemId != R.id.main) {
            binding.bottomNavigationView.selectedItemId = R.id.main
        }
        executeWhenMainFragmentAvailable { mainFragment ->
            mainFragment.restoreTabState(state)
        }
    }

    inline fun <reified T> setupForFragment(animate: Boolean = true) {
        setupForFragment(T::class, animate)
    }

    fun setupForFragment(t: KClass<*>, animate: Boolean) {
        Log.d("MainActivity", "setupForFragment(): $t")

        if (binding.root.height == 0) {
            binding.root.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        binding.root.viewTreeObserver.removeOnPreDrawListener(this)

                        setupForFragment(t, animate)

                        return false
                    }
                },
            )

            return
        }

        when (t) {
            CommunityFragment::class -> {
                enableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
            }
            PostFragment::class -> {
                disableBottomNavViewScrolling()
                hideBottomNav(animate)
                showNotificationBarBg()
            }
            VideoViewerFragment::class -> {
                disableBottomNavViewScrolling()
                hideBottomNav(animate)
                hideNotificationBarBg()
            }
            ImageViewerActivity::class -> {
                disableBottomNavViewScrolling()
                hideBottomNav(animate)
                hideNotificationBarBg()
            }
            SettingCacheFragment::class -> {
                disableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
            }
            HistoryFragment::class -> {
                disableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
            }
            LoginFragment::class -> {
                disableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
            }
            SettingsFragment::class -> {
                disableBottomNavViewScrolling()
                hideBottomNav(animate)
                hideNotificationBarBg()
            }
            PersonTabbedFragment::class -> {
                disableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
            }
            CommunityInfoFragment::class -> {
                disableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
            }
            SavedTabbedFragment::class -> {
                disableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
            }
            InboxTabbedFragment::class -> {
                disableBottomNavViewScrolling()
                showBottomNav(supportOpenness = true)
                showNotificationBarBg()
            }
            else ->
                throw RuntimeException("No setup instructions for type: ${t.java.canonicalName}")
        }
    }

    fun getSnackbarContainer(): View = binding.snackbarContainer
    fun getBottomNavHeight() = binding.bottomNavigationView.height

    fun runOnReady(lifecycleOwner: LifecycleOwner, cb: () -> Unit) {
        viewModel.isReady.observe(lifecycleOwner) {
            if (it == true) {
                cb()
            }
        }
    }

    fun showBottomMenu(bottomMenu: BottomMenu, expandFully: Boolean = true) {
        currentBottomMenu?.close()
        bottomMenu.setInsets(lastInsets.topInset, lastInsets.bottomInset)
        bottomMenu.show(this, binding.bottomSheetContainer, expandFully)
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

        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putExtras(ImageViewerActivityArgs(title, url, mimeType, transitionName).toBundle())
        }

        if (transitionName != null) {
            val sharedElements = mutableListOf<Pair<View, String>>()
            sharedElements += Pair.create(sharedElement, transitionName)
            if (appBar != null) {
                sharedElements += Pair.create(appBar, SharedElementNames.AppBar)
            }
            sharedElements += Pair.create(
                binding.bottomNavigationView,
                SharedElementNames.NavBar,
            )

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                *sharedElements.toTypedArray(),
            )
            startActivity(intent, options.toBundle())
        } else {
            startActivity(intent)
        }
    }

    fun openVideo(
        url: String,
        videoType: VideoType,
        videoState: VideoState?,
    ) {
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
}
