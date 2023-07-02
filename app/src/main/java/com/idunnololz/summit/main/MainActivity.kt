package com.idunnololz.summit.main

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationBarView
import com.idunnololz.summit.MainDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.ActivityMainBinding
import com.idunnololz.summit.history.HistoryFragment
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.login.LoginFragment
import com.idunnololz.summit.offline.OfflineFragment
import com.idunnololz.summit.preferences.ThemeManager
import com.idunnololz.summit.preview.ImageViewerFragment
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.preview.VideoViewerFragment
import com.idunnololz.summit.saved.SavedFragment
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.user.TabCommunityState
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.video.ExoPlayerManager
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.reflect.KClass

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private val sharedViewModel: SharedViewModel by viewModels()

    var toolbarTextView: TextView? = null
        private set

    // holds the original Toolbar height.
    // this can also be obtained via (an)other method(s)
    private var toolbarHeight: Int = 0

    private var lastCustomAppBarOffset = -1f

    private var lastToolbarAppBarOffset = -1f

    private lateinit var binding: ActivityMainBinding

    val headerOffset = MutableLiveData<Int>()
    val bottomNavViewOffset = MutableLiveData<Int>(0)
    val bottomNavViewAnimationOffset = MutableLiveData<Float>(0f)
    val windowInsets = MutableLiveData<Rect>(Rect())

    private var animatingBottomNavView = false
    private var enableBottomNavViewScrolling = false

    private val viewModel: MainActivityViewModel by viewModels()

    private var communitySelectorController: CommunitySelectorController? = null

    private var currentNavController: NavController? = null

    private var showNotificationBarBg: Boolean = true

    private lateinit var lemmyAppBarController: LemmyAppBarController

    val insetsChangedLiveData = MutableLiveData<Int>()

    private val onNavigationItemReselectedListeners =
        mutableListOf<NavigationBarView.OnItemReselectedListener>()

    private var currentBottomMenu: BottomMenu? = null
    var lastInsets: MainActivityInsets = MainActivityInsets()

    var lockUiOpenness = false

    @Inject
    lateinit var themeManager: ThemeManager

    private var isMaterialYou = false

    private fun updateTheme() {
        isMaterialYou = themeManager.useMaterialYou.value
        if (isMaterialYou) {
            Log.d("HAHA", "2Dynamic color applied!")
            DynamicColors.applyToActivityIfAvailable(this@MainActivity)
        } else {
            // do nothing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        updateTheme()

        viewModel.communities.observe(this) {
            communitySelectorController?.setCommunities(it)
        }

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setupActionBar()
        setupCustomActionBar()
        registerInsetsHandler()
        registerCurrentAccountListener()
        registerDefaultCommunityListener()

        hideActionBar(animate = true)

        lemmyAppBarController = LemmyAppBarController(this, binding.customAppBar)

        fun updateBottomTranslationY() {
            binding.bottomNavigationView.translationY =
                bottomNavViewOffset.value!!.toFloat() + bottomNavViewAnimationOffset.value!!.toFloat()
        }

        bottomNavViewOffset.observe(this) {
            updateBottomTranslationY()
        }
        bottomNavViewAnimationOffset.observe(this) {
            updateBottomTranslationY()
        }

        handleIntent(intent)

        runOnReady(this) {
            viewModel.loadCommunities()

            if (savedInstanceState == null) {
                setupBottomNavigationBar()
            } // Else, need to wait for onRestoreInstanceState

            lifecycleScope.launch(Dispatchers.Default) {
                themeManager.useMaterialYou.collect {
                    withContext(Dispatchers.Main) {
                        if (it != isMaterialYou) {
                            updateTheme()

                            recreate()
                        }
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
            }
        )
    }

    private fun registerDefaultCommunityListener() {
        viewModel.defaultCommunity.observe(this) {
            lemmyAppBarController.setDefaultCommunity(it)
        }
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
        onNavigationItemReselectedListener: NavigationBarView.OnItemReselectedListener
    ) {
        onNavigationItemReselectedListeners.add(onNavigationItemReselectedListener)
    }

    fun unregisterOnNavigationItemReselectedListener(
        onNavigationItemReselectedListener: NavigationBarView.OnItemReselectedListener
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

            // Move toolbar below status bar
            binding.appBar.layoutParams = (binding.appBar.layoutParams as ViewGroup.MarginLayoutParams).apply {
                //topMargin = insets.systemWindowInsetTop
                leftMargin = leftInset
                rightMargin = rightInset
            }
            binding.appBar.updatePadding(top = topInset)

            binding.customAppBar.layoutParams =
                (binding.customAppBar.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    leftMargin = leftInset
                    rightMargin = rightInset
                }
            binding.bottomNavigationView.layoutParams =
                (binding.bottomNavigationView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    leftMargin = leftInset
                    rightMargin = rightInset
                }
            binding.customActionBar.updatePadding(top = topInset)

            // Move our RecyclerView below toolbar + 10dp
            windowInsets.value = checkNotNull(windowInsets.value).apply {
                left = leftInset
                top = topInset
                right = rightInset
                bottom = bottomInset
            }
            binding.bottomNavigationView.layoutParams =
                (binding.bottomNavigationView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    leftMargin = leftInset
                    rightMargin = rightInset
                }
            binding.snackbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = binding.bottomNavigationView.height
            }
            binding.bottomNavigationView.updatePadding(bottom = bottomInset)

            onStatusBarHeightChanged(topInset)

            binding.navBarBg.updateLayoutParams<LayoutParams> {
                height = bottomInset
            }

            insetsChangedLiveData.postValue(0)

            WindowInsetsCompat.CONSUMED
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

    fun disableCustomAppBar(animate: Boolean) {
        binding.customAppBar.removeOnOffsetChangedListener(customAppBarOnOffsetChangedListener)

        if (animate) {
            binding.customAppBar.animate()
                .translationY(-binding.customAppBar.height.toFloat())
        } else {
            binding.customAppBar.translationY = -binding.customAppBar.height.toFloat()
        }

        communitySelectorController?.hide()

        ValueAnimator.ofInt(headerOffset.value ?: 0, 0).apply {
            duration = 300
            addUpdateListener {
                headerOffset.value = it.animatedValue as Int
            }
        }.start()
    }

    private val customAppBarOnOffsetChangedListener =
        AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            lastCustomAppBarOffset = verticalOffset.toFloat()
            headerOffset.value = verticalOffset + appBarLayout.height

            val percentShown = abs(lastCustomAppBarOffset) / binding.customAppBar.height
            if (!animatingBottomNavView)
                bottomNavViewOffset.value =
                    (percentShown * binding.bottomNavigationView.height).toInt()
        }

    fun setNavUiOpenness(progress: Float) {
        if (lockUiOpenness) return
        binding.customAppBar.translationY = -binding.customAppBar.height * progress
        bottomNavViewAnimationOffset.value = binding.bottomNavigationView.height * progress
    }

    fun enableCustomAppBarListener() {
        binding.customAppBar.removeOnOffsetChangedListener(customAppBarOnOffsetChangedListener)
        binding.customAppBar.addOnOffsetChangedListener(customAppBarOnOffsetChangedListener)
    }

    fun enableCustomAppBar() {
        enableCustomAppBarListener()
        val customAppBar = binding.customAppBar
        headerOffset.value = (customAppBar.height + lastCustomAppBarOffset).toInt()
        customAppBar.visibility = View.VISIBLE
        customAppBar.animate()
            .translationY(0f)
        val percentShown = abs(lastCustomAppBarOffset) / customAppBar.height
        val endOff = percentShown * binding.bottomNavigationView.height

        animatingBottomNavView = true
        ValueAnimator.ofInt(binding.bottomNavigationView.translationY.toInt(), endOff.toInt()).apply {
            duration = 300
            addUpdateListener {
                bottomNavViewOffset.value = it.animatedValue as Int

                if (it.animatedFraction == 1.0f) {
                    animatingBottomNavView = false
                }
            }
        }.start()
        ValueAnimator.ofFloat(requireNotNull(bottomNavViewAnimationOffset.value), 0f).apply {
            duration = 300
            addUpdateListener {
                bottomNavViewAnimationOffset.value = it.animatedValue as Float
            }
        }.start()
    }

    var lastScrollFlagsToolbar: Int = 0
    fun fixToolbar() {
        binding.toolbar.layoutParams =
            (binding.toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
                lastScrollFlagsToolbar = scrollFlags
                scrollFlags = 0
            }
    }

    fun unfixToolbar() {
        binding.toolbar.layoutParams =
            (binding.toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
                scrollFlags = lastScrollFlagsToolbar
            }
    }

    fun showCustomAppBar() {
        binding.customAppBar.setExpanded(true)
    }

    fun showBottomNav() {
        if (enableBottomNavViewScrolling && binding.bottomNavigationView.visibility == View.VISIBLE) {
            return
        }
        if (binding.bottomNavigationView.visibility != View.VISIBLE) {
            binding.bottomNavigationView.visibility = View.VISIBLE
            binding.bottomNavigationView.translationY =
                binding.bottomNavigationView.height.toFloat()
            Log.d(TAG, "bottomNavigationView.height: ${binding.bottomNavigationView.height}")
            binding.bottomNavigationView.post {
                binding.bottomNavigationView.animate()
                    .translationY(0f)
                    .setDuration(250)
            }
        } else {
            if (binding.bottomNavigationView.translationY == 0f) return

            binding.bottomNavigationView.animate()
                .translationY(0f)
                .setDuration(250)
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

    fun launchPage(page: PageRef) {
        if (binding.bottomNavigationView.selectedItemId != R.id.mainFragment) {
            binding.bottomNavigationView.selectedItemId = R.id.mainFragment
        }

        executeWhenMainFragmentAvailable { mainFragment ->
            mainFragment.navigateToPage(page)
        }
    }

    private fun executeWhenMainFragmentAvailable(fn: (MainFragment) -> Unit) {
        val navigateToPageRunnable = object : Runnable {
            override fun run() {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                            as NavHostFragment
                val currentFragment = navHostFragment.childFragmentManager.fragments.getOrNull(0)
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

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayShowHomeEnabled(true)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        toolbarTextView = Utils.getToolbarTextView(binding.toolbar)
    }

    private fun setupCustomActionBar() {
        binding.customActionBar.layoutParams =
            (binding.customActionBar.layoutParams as AppBarLayout.LayoutParams).apply {
                scrollFlags =
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            }
    }

    fun setupActionBar(@StringRes title: Int, showUp: Boolean) {
        setupActionBar(getString(title), showUp, showSpinner = false, animateActionBarIn = true)
    }

    @JvmOverloads
    fun setupActionBar(
        title: CharSequence,
        showUp: Boolean,
        showSpinner: Boolean = false,
        animateActionBarIn: Boolean = true
    ) {
        val ab = supportActionBar
        if (ab != null) {
            if (showSpinner) {
                ab.setDisplayShowTitleEnabled(false)
            } else {
                ab.setDisplayShowTitleEnabled(true)
                ab.title = title
            }
        }
        // undo any alpha changes. See EsportsTeamFragment.
        toolbarTextView?.alpha = 1f
        if (animateActionBarIn) {
            showActionBar()
        }

        if (showUp) {
            ab?.setDisplayHomeAsUpEnabled(true)
            ab?.setDisplayShowHomeEnabled(true)
        } else {
            ab?.setDisplayHomeAsUpEnabled(false)
            ab?.setDisplayShowHomeEnabled(false)
        }
    }

    private val onOffsetChangedListener = AppBarLayout.OnOffsetChangedListener {
            appBarLayout, verticalOffset ->

        lastToolbarAppBarOffset = verticalOffset.toFloat()
        headerOffset.value = verticalOffset + appBarLayout.height

        val percentShown = abs(lastToolbarAppBarOffset) / binding.appBar.height
        if (!animatingBottomNavView)
            bottomNavViewOffset.value =
                (percentShown * binding.bottomNavigationView.height).toInt()
    }
    fun showActionBar() {
        setSupportActionBar(binding.toolbar)

        hideActionBar(animate = true, false)
        unfixToolbar()

        binding.toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags =
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
        }
//        val supportActionBar = supportActionBar ?: return
//        if (supportActionBar.isShowing) return
        binding.appBar.visibility = View.VISIBLE
        if (ViewCompat.isLaidOut(binding.appBar)) {
            binding.appBar.animate().translationY(0f)
        } else {
            binding.appBar.translationY = 0f
        }
//        supportActionBar.show()
    }

    fun enableScrollingBottomNavByActionBar() {
        binding.appBar.addOnOffsetChangedListener(onOffsetChangedListener)
    }

    fun hideActionBar(animate: Boolean, hideToolbar: Boolean = true) {
        binding.appBar.removeOnOffsetChangedListener(onOffsetChangedListener)
        if (hideToolbar) {
            fixToolbar()
//            val supportActionBar = supportActionBar ?: return
//            if (!supportActionBar.isShowing) {
//                return
//            }
            if (ViewCompat.isLaidOut(binding.appBar)) {
                binding.appBar.animate().translationY((-binding.appBar.height).toFloat())
            } else {
                binding.appBar.visibility = View.INVISIBLE
            }
//            supportActionBar.hide()
        }

        updateToolbarHeight()
    }

    private fun createOrGetSubredditSelectorController(): CommunitySelectorController =
        communitySelectorController
            ?: viewModel.communitySelectorControllerFactory.create(this).also {
                it.inflate(this, binding.overlayContainer)
                it.setCommunities(viewModel.communities.value)

                communitySelectorController = it
            }

    fun showCommunitySelector(): CommunitySelectorController {
        val communitySelectorController = createOrGetSubredditSelectorController()

        viewModel.communities.value.let {
            communitySelectorController.setCommunities(it)
        }

        communitySelectorController.show(this, lastInsets)

        return communitySelectorController
    }

    fun getCustomAppBarController() = lemmyAppBarController

    private fun updateToolbarHeight() {
        if (toolbarHeight == 0) {
            toolbarHeight = binding.toolbar.height
            if (toolbarHeight == 0) {
                binding.toolbar.measure(0, 0)
                toolbarHeight = binding.toolbar.measuredHeight
            }
        }
    }

    fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    fun showSystemUI(animate: Boolean) {
        window.decorView.systemUiVisibility = (SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or if (resources.getBoolean(R.bool.isLightTheme)) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else 0)
    }

    fun insetViewAutomaticallyByMargins(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val lp = rootView.layoutParams as ViewGroup.MarginLayoutParams
            val insets = checkNotNull(windowInsets.value)

            lp.topMargin = insets.top
            lp.bottomMargin = insets.bottom
            lp.leftMargin = insets.left
            lp.rightMargin = insets.right
            rootView.requestLayout()
        }
    }

    fun insetViewExceptBottomAutomaticallyByMargins(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val lp = rootView.layoutParams as ViewGroup.MarginLayoutParams
            val insets = checkNotNull(windowInsets.value)

            lp.topMargin = insets.top
            lp.leftMargin = insets.left
            lp.rightMargin = insets.right
            rootView.requestLayout()
        }
    }


    fun insetViewExceptTopAutomaticallyByPadding(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.setPadding(
                insets.leftInset,
                0,
                insets.rightInset,
                insets.bottomInset,
            )
        }
    }

    fun insetViewExceptTopAutomaticallyByPaddingAndNavUi(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner) {
            val insets = lastInsets

            rootView.setPadding(
                insets.leftInset,
                0,
                insets.rightInset,
                insets.bottomInset + getBottomNavHeight() + getCustomAppBarHeight(),
            )
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
        Log.d("MainActivity", "setupForFragment(): ${t}")

        if (binding.root.height == 0) {
            binding.root.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        binding.root.viewTreeObserver.removeOnPreDrawListener(this)

                        setupForFragment(t, animate)

                        return false
                    }
                }
            )

            return
        }

        when (t) {
            CommunityFragment::class -> {
                hideActionBar(animate)
                enableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
                enableCustomAppBar()
            }
            PostFragment::class -> {
                hideActionBar(animate)
                disableBottomNavViewScrolling()
//                hideBottomNav()
                showNotificationBarBg()
                enableCustomAppBarListener()
//                disableCustomAppBar()
            }
            VideoViewerFragment::class -> {
                hideActionBar(animate)
                disableBottomNavViewScrolling()
                hideBottomNav(animate)
                disableCustomAppBar(animate)
                hideNotificationBarBg()
            }
            ImageViewerFragment::class -> {
                showActionBar()
                disableBottomNavViewScrolling()
                hideBottomNav(animate)
                disableCustomAppBar(animate)
                hideNotificationBarBg()
            }
            OfflineFragment::class -> {
                hideActionBar(animate)
                disableBottomNavViewScrolling()
                showBottomNav()
                disableCustomAppBar(animate)
                showNotificationBarBg()
            }
            HistoryFragment::class -> {
                hideActionBar(animate)
                disableBottomNavViewScrolling()
                showBottomNav()
                showNotificationBarBg()
                disableCustomAppBar(animate)
            }
            LoginFragment::class -> {
                hideActionBar(animate)
                disableBottomNavViewScrolling()
                showBottomNav()
                disableCustomAppBar(animate)
                showNotificationBarBg()
            }
            SavedFragment::class -> {
                hideActionBar(animate)
                disableBottomNavViewScrolling()
                showBottomNav()
                disableCustomAppBar(animate)
                showNotificationBarBg()
            }
            SettingsFragment::class -> {
                hideActionBar(animate)
                disableBottomNavViewScrolling()
                hideBottomNav(animate)
                disableCustomAppBar(animate)
                hideNotificationBarBg()
            }
            else ->
                throw RuntimeException("No setup instructions for type: ${t.java.canonicalName}")
        }
    }

    fun getSnackbarContainer(): View = binding.snackbarContainer
    fun getToolbarHeight() = binding.toolbar.layoutParams.height
    fun getBottomNavHeight() = binding.bottomNavigationView.height
    fun getCustomAppBarHeight() = binding.customAppBar.height

    private fun registerCurrentAccountListener() {
        viewModel.currentAccount.observe(this) {
            lemmyAppBarController.onAccountChanged(it)
        }
    }

    fun runOnReady(lifecycleOwner: LifecycleOwner, cb: () -> Unit) {
        viewModel.isReady.observe(lifecycleOwner) {
            if (it == true) {
                cb()
            }
        }
    }

    fun showBottomMenu(bottomMenu: BottomMenu) {
        currentBottomMenu?.close()
        bottomMenu.setInsets(lastInsets.topInset, lastInsets.bottomInset)
        bottomMenu.show(this, binding.bottomSheetContainer)
        currentBottomMenu = bottomMenu
    }

    fun openImage(
        title: String?,
        url: String,
        mimeType: String?,
    ) {
        val direction = MainDirections.actionGlobalImageViewerFragment(title, url, mimeType)
        currentNavController?.navigateSafe(direction)
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
        val direction = MainDirections.actionGlobalSettingsFragment()
        currentNavController?.navigateSafe(direction)
    }
}