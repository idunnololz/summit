package com.idunnololz.summit.main

import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED
import com.google.android.material.navigationrail.NavigationRailView
import com.idunnololz.summit.R
import com.idunnololz.summit.preferences.GlobalLayoutMode
import com.idunnololz.summit.preferences.GlobalLayoutModes
import com.idunnololz.summit.preferences.NavigationRailModeId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.navigation.NavBarConfig
import com.idunnololz.summit.settings.navigation.NavBarDestinations
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimen

class NavBarController(
    val activity: Activity,
    val contentView: CoordinatorLayout,
    val lifecycleOwner: LifecycleOwner,
    val onNavBarChanged: () -> Unit,
) {

    companion object {
        private const val TAG = "NavBarController"
    }

    private val context = activity

    private var _useNavigationRail: Boolean? = null

    val useNavigationRail: Boolean
        get() = _useNavigationRail!!

    var useBottomNavBar = true
    var navRailMode = NavigationRailModeId.Auto
    var globalLayoutMode: GlobalLayoutMode = GlobalLayoutModes.Auto

    lateinit var navBar: NavigationBarView
    lateinit var navBarContainer: View

    var newLeftInset: Int = 0
    var newRightInset: Int = 0

    val bottomNavHeight: Int
        get() =
            if (useBottomNavBar) {
                if (useNavigationRail) {
                    0
                } else {
                    navBarContainer.height
                }
            } else {
                0
            }

    val navRailWidth: Int
        get() =
            if (useBottomNavBar) {
                if (useNavigationRail) {
                    if (navBarContainer.width == 0) {
                        activity.getDimen(com.google.android.material.R.dimen.m3_navigation_rail_default_width)
                    } else {
                        navBarContainer.width
                    }
                } else {
                    0
                }
            } else {
                0
            }

    val navBarOffsetPercent = MutableLiveData<Float>(0f)
    val bottomNavViewAnimationOffsetPercent = MutableLiveData<Float>(0f)
    val bottomNavOpenPercent: Float
        get() = navBarOffsetPercent.value!! +
            bottomNavViewAnimationOffsetPercent.value!!
    var useCustomNavBar: Boolean = false

    private var enableBottomNavViewScrolling = false
    private var navBarConfig: NavBarConfig = NavBarConfig()

    init {
        navBarOffsetPercent.observe(lifecycleOwner) {
            updateOpenness(bottomNavOpenPercent)
        }
        bottomNavViewAnimationOffsetPercent.observe(lifecycleOwner) {
            updateOpenness(bottomNavOpenPercent)
        }
    }

    fun setup() {
        onWindowSizeChanged()
    }

    private fun onWindowSizeChanged() {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(activity)

        val widthDp = metrics.bounds.width() /
            activity.resources.displayMetrics.density
        val widthWindowSizeClass = when {
            widthDp < 600f -> WindowSizeClass.COMPACT
            widthDp < 840f -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        val heightDp = metrics.bounds.height() /
            activity.resources.displayMetrics.density
        val heightWindowSizeClass = when {
            heightDp < 480f -> WindowSizeClass.COMPACT
            heightDp < 900f -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }

        val useNavigationRail: Boolean =
            when (navRailMode) {
                NavigationRailModeId.ManualOn -> true
                NavigationRailModeId.ManualOff -> false
                NavigationRailModeId.Auto -> {
                    widthWindowSizeClass != WindowSizeClass.COMPACT
                }
                else -> false
            }

        if (useNavigationRail == _useNavigationRail) {
            return
        }
        _useNavigationRail = useNavigationRail

        val navBarChanged = ::navBar.isInitialized
        if (navBarChanged) {
            contentView.removeView(navBarContainer)
        }

        navBar = if (useNavigationRail) {
            NavigationRailView(activity).apply {
                setBackgroundColor(
                    activity.getColorFromAttribute(
                        com.google.android.material.R.attr.backgroundColor,
                    ),
                )
                inflateMenu(R.menu.bottom_navigation_menu)
                labelVisibilityMode = LABEL_VISIBILITY_LABELED
            }
        } else {
            BottomNavigationView(activity).apply {
                setBackgroundColor(
                    activity.getColorFromAttribute(
                        com.google.android.material.R.attr.colorSurface,
                    ),
                )
                inflateMenu(R.menu.bottom_navigation_menu)
                labelVisibilityMode = LABEL_VISIBILITY_LABELED
            }
        }

        navBarContainer = if (useNavigationRail) {
            FrameLayout(context).apply {
                val materialDivider = MaterialDivider(context)

                addView(navBar)
                addView(materialDivider)

                navBar.updateLayoutParams<FrameLayout.LayoutParams> {
                    width = CoordinatorLayout.LayoutParams.WRAP_CONTENT
                    height = CoordinatorLayout.LayoutParams.MATCH_PARENT
                    gravity = Gravity.START
                }
                materialDivider.updateLayoutParams<FrameLayout.LayoutParams> {
                    width = context.resources.getDimensionPixelSize(R.dimen.divider_size)
                    height = FrameLayout.LayoutParams.MATCH_PARENT
                    gravity = Gravity.END
                }
            }
        } else {
            navBar
        }

        contentView.addView(navBarContainer)

        if (useNavigationRail) {
            navBarContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                width = CoordinatorLayout.LayoutParams.WRAP_CONTENT
                height = CoordinatorLayout.LayoutParams.MATCH_PARENT
                gravity = Gravity.START
            }
        } else {
            navBarContainer.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                width = CoordinatorLayout.LayoutParams.MATCH_PARENT
                height = CoordinatorLayout.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
            }
        }

        if (navBarChanged) {
            onNavBarChanged()
        }
    }

    fun onInsetsChanged(
        leftInset: Int,
        topInset: Int,
        rightInset: Int,
        bottomInset: Int,
    ) {
        val isRtl = navBar.layoutDirection == FrameLayout.LAYOUT_DIRECTION_RTL

        if (useNavigationRail) {
            navBar.updatePadding(top = topInset, bottom = bottomInset)

            val width = navRailWidth

            if (isRtl) {
                newLeftInset = leftInset
                newRightInset = rightInset + width
            } else {
                newLeftInset = leftInset + width
                newRightInset = rightInset
            }
        } else {
            navBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = leftInset
                rightMargin = rightInset
            }
            navBar.updatePadding(bottom = bottomInset)

            newLeftInset = leftInset
            newRightInset = rightInset
        }
    }

    fun updateOpenness(navOpenness: Float) {
        if (!useBottomNavBar) return

        Log.d(TAG, "updateOpenness(): $navOpenness")

        if (useNavigationRail) {
            navBarContainer.translationX = -navOpenness * navBarContainer.width
        } else {
            navBarContainer.translationY = navOpenness * navBarContainer.height
        }
    }

    fun enableBottomNavViewScrolling() {
        if (!useBottomNavBar) return
        if (useNavigationRail) return

        enableBottomNavViewScrolling = true
        navBar.visibility = View.VISIBLE
    }

    fun disableBottomNavViewScrolling() {
        enableBottomNavViewScrolling = false
    }

    fun showBottomNav(supportOpenness: Boolean = false) {
        if (!useBottomNavBar) return
        if (enableBottomNavViewScrolling && navBar.visibility == View.VISIBLE) {
            return
        }

        Log.d(TAG, "showBottomNav() bottomNavigationView.height: ${navBarContainer.height} bottomNavOpenness: $bottomNavOpenPercent")

        val navigationBarOffset =
            if (supportOpenness) {
                bottomNavOpenPercent
            } else {
                0f
            } *
                if (useNavigationRail) {
                    navBarContainer.width.toFloat()
                } else {
                    navBarContainer.height.toFloat()
                }

        navBarOffsetPercent.value = 0f

        if (useNavigationRail) {
            if (navBarContainer.visibility != View.VISIBLE) {
                navBarContainer.visibility = View.VISIBLE
                navBarContainer.translationX =
                    -navBar.width.toFloat()

                navBarContainer.post {
                    navBarContainer.animate()
                        .translationX(navigationBarOffset).duration = 250
                }
            } else {
                if (navBarContainer.translationX == 0f) return

                navBarContainer.animate()
                    .translationX(navigationBarOffset).duration = 250
            }
        } else {
            if (navBarContainer.visibility != View.VISIBLE) {
                navBarContainer.visibility = View.VISIBLE
                navBarContainer.translationY =
                    navBarContainer.height.toFloat()

                navBarContainer.post {
                    navBarContainer.animate()
                        .translationY(navigationBarOffset).duration = 250
                }
            } else {
                if (navBarContainer.translationY == 0f) return

                navBarContainer.animate()
                    .translationY(navigationBarOffset).duration = 250
            }
        }
    }

    fun hideNavBar(animate: Boolean) {
        if (!useBottomNavBar) return

        if (useNavigationRail) {
            if (animate) {
                if (navBarContainer.translationX > -navBarContainer.width.toFloat()) {
                    navBarContainer.animate()
                        .translationX(-navBarContainer.width.toFloat())
                        .apply {
                            duration = 250
                        }
                }
            } else {
                navBarContainer.translationX = -navBarContainer.width.toFloat()
            }
        } else {
            if (animate) {
                if (navBarContainer.translationY < navBarContainer.height.toFloat()) {
                    navBarContainer.animate()
                        .translationY(navBarContainer.height.toFloat())
                        .apply {
                            duration = 250
                        }
                }
            } else {
                navBarContainer.translationY = navBarContainer.height.toFloat()
            }
        }
    }

    fun updatePaddingForNavBar(contentContainer: View) {
        if (useNavigationRail) {
            val width = if (navBarContainer.width == 0) {
                activity.getDimen(com.google.android.material.R.dimen.m3_navigation_rail_default_width)
            } else {
                navBarContainer.width
            }

            contentContainer.updatePaddingRelative(start = width)
        } else {
            contentContainer.updatePadding(bottom = bottomNavHeight)
        }
    }

    fun onPreferencesChanged(preferences: Preferences) {
        useBottomNavBar = preferences.useBottomNavBar
        navRailMode = preferences.navigationRailMode
        useCustomNavBar = preferences.useCustomNavBar
        navBarConfig = preferences.navBarConfig
        globalLayoutMode = preferences.globalLayoutMode

        navRailMode =
            if (globalLayoutMode == GlobalLayoutModes.SmallScreen) {
                navRailMode
            } else {
                NavigationRailModeId.Auto
            }

        onWindowSizeChanged()

        if (!useBottomNavBar) {
            navBar.visibility = View.GONE
        } else if (useCustomNavBar) {
            navBar.setTag(R.id.custom_nav_bar, true)
            navBar.menu.apply {
                clear()
                val navBarDestinations = navBarConfig.navBarDestinations
                for (dest in navBarDestinations) {
                    when (dest) {
                        NavBarDestinations.Home -> {
                            add(
                                Menu.NONE,
                                R.id.mainFragment,
                                Menu.NONE,
                                context.getString(R.string.home),
                            ).apply {
                                setIcon(R.drawable.baseline_home_24)
                            }
                        }
                        NavBarDestinations.Saved -> {
                            add(
                                Menu.NONE,
                                R.id.savedFragment,
                                Menu.NONE,
                                context.getString(R.string.saved),
                            ).apply {
                                setIcon(R.drawable.baseline_bookmark_24)
                            }
                        }
                        NavBarDestinations.Search -> {
                            add(
                                Menu.NONE,
                                R.id.searchFragment,
                                Menu.NONE,
                                context.getString(R.string.search),
                            ).apply {
                                setIcon(R.drawable.baseline_search_24)
                            }
                        }
                        NavBarDestinations.History -> {
                            add(
                                Menu.NONE,
                                R.id.historyFragment,
                                Menu.NONE,
                                context.getString(R.string.history),
                            ).apply {
                                setIcon(R.drawable.baseline_history_24)
                            }
                        }
                        NavBarDestinations.Inbox -> {
                            add(
                                Menu.NONE,
                                R.id.inboxTabbedFragment,
                                Menu.NONE,
                                context.getString(R.string.inbox),
                            ).apply {
                                setIcon(R.drawable.baseline_inbox_24)
                            }
                        }
                        NavBarDestinations.Profile -> {
                            add(
                                Menu.NONE,
                                R.id.personTabbedFragment2,
                                Menu.NONE,
                                context.getString(R.string.user_profile),
                            ).apply {
                                setIcon(R.drawable.outline_account_circle_24)
                            }
                        }
                        NavBarDestinations.None -> {
                        }
                    }
                }
            }
        } else {
            if (navBar.getTag(R.id.custom_nav_bar) == true) {
                navBar.menu.clear()
                navBar.inflateMenu(R.menu.bottom_navigation_menu)
                navBar.setTag(R.id.custom_nav_bar, false)
            }
        }

        if (!useBottomNavBar) {
            navBarContainer.visibility = View.GONE
        }
    }

    enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }
}
