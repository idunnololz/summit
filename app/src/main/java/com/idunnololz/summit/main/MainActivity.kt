package com.idunnololz.summit.main

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.*
import androidx.navigation.NavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.idunnololz.summit.R
import com.idunnololz.summit.auth.RedditAuthManager
import com.idunnololz.summit.databinding.ActivityMainBinding
import com.idunnololz.summit.history.HistoryFragment
import com.idunnololz.summit.offline.OfflineFragment
import com.idunnololz.summit.post.PostFragment
import com.idunnololz.summit.preview.ImageViewerFragment
import com.idunnololz.summit.preview.VideoViewerFragment
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.redirect.RedirectHandlerDialogFragment
import com.idunnololz.summit.settings.AccountSettingsFragment
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.subreddit.SubredditFragment
import com.idunnololz.summit.tabs.SubredditTabsFragment
import com.idunnololz.summit.tabs.TabSubredditState
import com.idunnololz.summit.util.BaseActivity
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getCurrentNavigationFragment
import com.idunnololz.summit.util.ext.setupWithNavController
import com.idunnololz.summit.video.ExoPlayerManager
import kotlin.math.abs

class MainActivity : BaseActivity() {

    companion object {
        private val TAG = MainActivity::class.java.canonicalName
    }

    private lateinit var sharedViewModel: SharedViewModel

    var toolbarTextView: TextView? = null
        private set

    // holds the original Toolbar height.
    // this can also be obtained via (an)other method(s)
    private var toolbarHeight: Int = 0

    private var enableCustomAppBarVerticalOffsetListener = false
    private var lastCustomAppBarOffset = -1f

    lateinit var binding: ActivityMainBinding

    val headerOffset = MutableLiveData<Int>()
    val bottomNavViewOffset = MutableLiveData<Int>()
    val windowInsets = MutableLiveData<Rect>(Rect())

    private var animatingBottomNavView = false
    private var enableBottomNavViewScrolling = false

    private lateinit var viewModel: MainActivityViewModel

    private var subredditSelectorController: SubredditSelectorController? = null

    private var currentNavController: LiveData<NavController>? = null

    private var showNotificationBarBg: Boolean = true

    private lateinit var redditAppBarController: RedditAppBarController

    private val insetsChangedLiveData = MutableLiveData<Int>()

    private val onNavigationItemReselectedListeners =
        mutableListOf<BottomNavigationView.OnNavigationItemReselectedListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        viewModel.loadSubreddits()
        viewModel.subredditsLiveData.observe(this, Observer {
            subredditSelectorController?.setSubredditsState(it)
        })

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setupNotificationBarBg()
        setupActionBar()
        setupCustomActionBar()
        setupForFullScreen()

        redditAppBarController = RedditAppBarController(this, binding.customAppBar)

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState

        bottomNavViewOffset.observe(this, Observer {
            binding.bottomNavigationView.translationY = it.toFloat()
        })

        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        ExoPlayerManager.destroyAll()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    override fun onSupportNavigateUp(): Boolean =
        currentNavController?.value?.navigateUp() ?: false

    fun expandCustomActionBar() {
        val anim =
            ValueAnimator.ofInt(binding.customAppBar.height, Utils.convertDpToPixel(86f).toInt())
        anim.addUpdateListener { valueAnimator ->
            val newHeight = valueAnimator.animatedValue as Int
            binding.customActionBar.layoutParams = binding.customActionBar.layoutParams.apply {
                height = newHeight
            }

            if (valueAnimator.animatedFraction == 1f) {
                binding.sortGroup.visibility = View.VISIBLE
            }
        }
        anim.duration = 300
        anim.start()
    }

    fun collapseCustomActionBar() {
        val anim =
            ValueAnimator.ofInt(
                binding.customAppBar.height,
                resources.getDimensionPixelSize(R.dimen.custom_ab_height)
            )
        anim.addUpdateListener { valueAnimator ->
            val newHeight = valueAnimator.animatedValue as Int
            binding.customActionBar.layoutParams = binding.customActionBar.layoutParams.apply {
                height = newHeight
            }
        }
        anim.duration = 300
        anim.start()

        binding.sortGroup.visibility = View.GONE
    }

    private fun setupBottomNavigationBar() {
        val navGraphIds = listOf(
            R.navigation.main,
            R.navigation.offline,
            R.navigation.history,
            R.navigation.settings
        )

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = binding.bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.navHostContainer,
            intent = intent
        )

        binding.bottomNavigationView.setOnItemReselectedListener { menuItem ->
            Log.d(TAG, "Reselected nav item: ${menuItem.itemId}")

            onNavigationItemReselectedListeners.forEach {
                it.onNavigationItemReselected(menuItem)
            }
        }

        currentNavController = controller
    }

    fun registerOnNavigationItemReselectedListener(
        onNavigationItemReselectedListener: BottomNavigationView.OnNavigationItemReselectedListener
    ) {
        onNavigationItemReselectedListeners.add(onNavigationItemReselectedListener)
    }

    fun unregisterOnNavigationItemReselectedListener(
        onNavigationItemReselectedListener: BottomNavigationView.OnNavigationItemReselectedListener
    ) {
        onNavigationItemReselectedListeners.remove(onNavigationItemReselectedListener)
    }

    private fun setupForFullScreen() {
        showSystemUI(false)

        binding.rootView.systemUiVisibility =
            SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            handleSystemUiVisibility(visibility)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.contentView) { _, insets ->
            // Move toolbar below status bar
            binding.appBar.layoutParams = (binding.appBar.layoutParams as ViewGroup.MarginLayoutParams).apply {
                //topMargin = insets.systemWindowInsetTop
                leftMargin = insets.systemWindowInsetLeft
                rightMargin = insets.systemWindowInsetRight
            }
            binding.appBar.updatePadding(top = insets.systemWindowInsetTop)
            binding.customAppBar.layoutParams =
                (binding.customAppBar.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    topMargin = insets.systemWindowInsetTop
                    leftMargin = insets.systemWindowInsetLeft
                    rightMargin = insets.systemWindowInsetRight
                }
            binding.bottomNavigationView.layoutParams =
                (binding.bottomNavigationView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    leftMargin = insets.systemWindowInsetLeft
                    rightMargin = insets.systemWindowInsetRight
                }

            // Move our RecyclerView below toolbar + 10dp
            windowInsets.value = checkNotNull(windowInsets.value).apply {
                left = insets.systemWindowInsetLeft
                top = insets.systemWindowInsetTop
                right = insets.systemWindowInsetRight
                bottom = insets.systemWindowInsetBottom
            }

            val statusBarHeight = insets.systemWindowInsetTop
            onStatusBarHeightChanged(statusBarHeight)

            insetsChangedLiveData.postValue(0)

            insets
        }
    }

    private fun handleSystemUiVisibility(visibility: Int) {
        if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
            // The system bars are visible. Make any desired
            // adjustments to your UI, such as showing the action bar or
            // other navigational controls.
            showNotificationBarBgIfNeeded()
            Log.d(TAG, "System UI visible!")
        } else {
            // The system bars are NOT visible. Make any desired
            // adjustments to your UI, such as hiding the action bar or
            // other navigational controls.
            hideNotificationBarBgIfNeeded()
            Log.d(TAG, "System UI not visible!")
        }
    }

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
    }

    private fun showNotificationBarBgIfNeeded() {
        if (!showNotificationBarBg) return
        if (binding.notificationBarBg.translationY == 0f && binding.notificationBarBg.visibility == View.VISIBLE) return
        binding.notificationBarBg.visibility = View.VISIBLE
        binding.notificationBarBg.animate()
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

    fun disableCustomAppBar() {
        fixCustomAppBar()
        enableCustomAppBarVerticalOffsetListener = false
        binding.customAppBar.animate()
            .translationY(-binding.customAppBar.height.toFloat())
            .withEndAction {
                binding.customAppBar.visibility = View.INVISIBLE
            }

        subredditSelectorController?.hide()

        ValueAnimator.ofInt(headerOffset.value ?: 0, 0).apply {
            duration = 300
            addUpdateListener {
                headerOffset.value = it.animatedValue as Int
            }
        }.start()
    }

    fun enableCustomAppBar() {
        val customAppBar = binding.customAppBar
        binding.customActionBar.layoutParams =
            (binding.customActionBar.layoutParams as AppBarLayout.LayoutParams).apply {
                scrollFlags =
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            }
        headerOffset.value = (customAppBar.height + lastCustomAppBarOffset).toInt()
        enableCustomAppBarVerticalOffsetListener = true
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
    }

    var lastScrollFlags: Int = 0
    fun fixCustomAppBar() {
        binding.customActionBar.layoutParams =
            (binding.customActionBar.layoutParams as AppBarLayout.LayoutParams).apply {
                lastScrollFlags = scrollFlags
                scrollFlags = 0
            }
    }

    fun unfixCustomAppBar() {
        binding.customActionBar.layoutParams =
            (binding.customActionBar.layoutParams as AppBarLayout.LayoutParams).apply {
                scrollFlags = lastScrollFlags
            }
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

    fun hideBottomNav() {
        Log.d(TAG, "bottomNavigationView.height: ${binding.bottomNavigationView.height}")
        binding.bottomNavigationView.animate()
            .translationY(binding.bottomNavigationView.height.toFloat())
            .setDuration(250)
            .withEndAction {
                binding.bottomNavigationView.visibility = View.INVISIBLE
            }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return

        val data = intent.data ?: return
        if (data.authority == "auth") {
            RedditAuthManager.instance.handleAuthAttempt(data, supportFragmentManager)
        } else if (RedditUtils.isUriReddit(data)) {
            if (RedditUtils.isUriGallery(data) && data.pathSegments.size >= 2) {
                RedirectHandlerDialogFragment.newInstance(LinkUtils.getRedirectLink(data.pathSegments[1]))
                    .show(supportFragmentManager, "asd")
            } else if (RedditUtils.isUriRedirect(data)) {
                RedirectHandlerDialogFragment.newInstance(data.toString())
                    .show(supportFragmentManager, "asd")
            } else {
                if (binding.bottomNavigationView.selectedItemId != R.id.main) {
                    binding.bottomNavigationView.selectedItemId = R.id.main
                }
                executeWhenMainFragmentAvailable { mainFragment ->
                    mainFragment.navigateToUri(data)
                }
            }
        }
    }

    private fun executeWhenMainFragmentAvailable(fn: (MainFragment) -> Unit) {
        val navigateToPageRunnable = object : Runnable {
            override fun run() {
                val currentFragment = supportFragmentManager.getCurrentNavigationFragment()
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

    private fun setupNotificationBarBg() {
        // get the status bar height
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // handled by inset listener
        } else {
            binding.contentView.post {
                val rectangle = Rect()
                val window = window
                window.decorView.getWindowVisibleDisplayFrame(rectangle)
                val statusBarHeight = rectangle.top
                onStatusBarHeightChanged(statusBarHeight)
            }
        }
    }

    private fun onStatusBarHeightChanged(statusBarHeight: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // before kitkat, apps cannot draw under the status bar
            return
        }

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
        binding.customAppBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (enableCustomAppBarVerticalOffsetListener) {
                lastCustomAppBarOffset = verticalOffset.toFloat()
                headerOffset.value = verticalOffset + appBarLayout.height

                val percentShown = abs(lastCustomAppBarOffset) / binding.customAppBar.height
                if (!animatingBottomNavView)
                    bottomNavViewOffset.value =
                        (percentShown * binding.bottomNavigationView.height).toInt()
            }
        }

        binding.closeButton.setOnClickListener {
            collapseCustomActionBar()
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

    fun showActionBar() {
        hideActionBar(false)

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

    fun hideActionBar(hideToolbar: Boolean = true) {
        if (hideToolbar) {
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

    private fun createOrGetSubredditSelectorController(): SubredditSelectorController =
        subredditSelectorController ?: SubredditSelectorController(this).also {
            it.inflate(binding.overlayContainer)
//            it.setTopMargin(customActionBar.height)
            it.onVisibilityChangedCallback = { isVisible ->
                if (isVisible) {
                    fixCustomAppBar()
                } else {
                    unfixCustomAppBar()
                }
            }
            subredditSelectorController = it
        }

    fun showSubredditSelector(): SubredditSelectorController {
        val subredditSelectorController = createOrGetSubredditSelectorController()

        viewModel.subredditsLiveData.value?.let {
            subredditSelectorController.setSubredditsState(it)
        }

        subredditSelectorController.show()
        return subredditSelectorController
    }

    fun getCustomAppBarController(): RedditAppBarController = redditAppBarController

    private fun updateToolbarHeight() {
        if (toolbarHeight == 0) {
            toolbarHeight = binding.toolbar.height
            if (toolbarHeight == 0) {
                binding.toolbar.measure(0, 0)
                toolbarHeight = binding.toolbar.measuredHeight
            }
        }
    }

    override fun onBackPressed() {
        onBackPressed(force = false)
    }

    fun onBackPressed(force: Boolean) {
        if (!force && subredditSelectorController?.isVisible == true) {
            subredditSelectorController?.hide()
        } else {
            super.onBackPressed()
        }
    }

    fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    fun showSystemUI(animate: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or if (resources.getBoolean(R.bool.isLightTheme)) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else 0)
        }
    }

    fun insetRootViewAutomatically(lifecycleOwner: LifecycleOwner, rootView: View) {
        insetsChangedLiveData.observe(lifecycleOwner, Observer {
            val lp = rootView.layoutParams as ViewGroup.MarginLayoutParams
            val insets = checkNotNull(windowInsets.value)

            lp.topMargin = insets.top
            lp.bottomMargin = insets.bottom
            lp.leftMargin = insets.left
            lp.rightMargin = insets.right
            rootView.requestLayout()
        })
    }

    fun restoreTabState(state: TabSubredditState) {
        if (binding.bottomNavigationView.selectedItemId != R.id.main) {
            binding.bottomNavigationView.selectedItemId = R.id.main
        }
        executeWhenMainFragmentAvailable { mainFragment ->
            mainFragment.restoreTabState(state)
        }
    }

    inline fun <reified T> setupForFragment() {
        when (T::class) {
            SubredditTabsFragment::class -> {
                hideActionBar()
                disableBottomNavViewScrolling()
                showBottomNav()
                disableCustomAppBar()
                showNotificationBarBg()
            }
            SubredditFragment::class -> {
                hideActionBar()
                enableBottomNavViewScrolling()
                showNotificationBarBg()
                enableCustomAppBar()
            }
            PostFragment::class -> {
                hideActionBar()
                enableBottomNavViewScrolling()
                enableCustomAppBar()
                showNotificationBarBg()

                collapseCustomActionBar()
            }
            VideoViewerFragment::class -> {
                hideActionBar()
                disableBottomNavViewScrolling()
                hideBottomNav()
                disableCustomAppBar()
                hideNotificationBarBg()
            }
            ImageViewerFragment::class -> {
                disableBottomNavViewScrolling()
                hideBottomNav()
                disableCustomAppBar()
                hideNotificationBarBg()
            }
            OfflineFragment::class -> {
                hideActionBar()
                disableBottomNavViewScrolling()
                showBottomNav()
                disableCustomAppBar()
                showNotificationBarBg()
            }
            HistoryFragment::class -> {
                showActionBar()
                disableBottomNavViewScrolling()
                showBottomNav()
                disableCustomAppBar()
                showNotificationBarBg()
            }
            SettingsFragment::class -> {
                hideActionBar()
                disableBottomNavViewScrolling()
                showBottomNav()
                disableCustomAppBar()
                showNotificationBarBg()
            }
            AccountSettingsFragment::class -> {
                hideActionBar()
                disableBottomNavViewScrolling()
                showBottomNav()
                disableCustomAppBar()
                showNotificationBarBg()
            }
            else -> throw RuntimeException("No setup instructions for type: ${T::class.java.canonicalName}")
        }
    }

    fun getSnackbarContainer(): View = binding.snackbarContainer
}