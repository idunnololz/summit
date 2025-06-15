package com.idunnololz.summit.main

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.ActivityNavigator
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED
import com.google.android.material.navigationrail.NavigationRailView
import com.idunnololz.summit.R
import com.idunnololz.summit.preferences.GlobalLayoutMode
import com.idunnololz.summit.preferences.GlobalLayoutModes
import com.idunnololz.summit.preferences.NavRailGravityIds
import com.idunnololz.summit.preferences.NavigationRailModeId
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.navigation.NavBarConfig
import com.idunnololz.summit.settings.navigation.NavBarDestinations
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimen
import java.lang.ref.WeakReference
import kotlin.math.abs

class NavBarController(
  val context: Context,
  val contentView: CoordinatorLayout,
  val lifecycleOwner: LifecycleOwner,
  val onNavBarChanged: (NavigationBarView) -> Unit,
  val getWindowBounds: () -> Rect,
) {

  companion object {
    private const val TAG = "NavBarController"
  }

  private var _useNavigationRail: Boolean? = null

  val useNavigationRail: Boolean
    get() = _useNavigationRail == true

  var useBottomNavBar = true
  private var navRailMode = NavigationRailModeId.Auto
  private var globalLayoutMode: GlobalLayoutMode = GlobalLayoutModes.Auto
  private var showLabels = true
  private var navRailGravity: Int = NavRailGravityIds.TOP

  lateinit var navBar: NavigationBarView
  private lateinit var navBarContainer: View

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

  private val navRailWidth: Int
    get() =
      if (useBottomNavBar) {
        if (useNavigationRail) {
          if (navBarContainer.width == 0) {
            context.getDimen(
              com.google.android.material.R.dimen.m3_navigation_rail_default_width,
            )
          } else {
            navBarContainer.width
          }
        } else {
          0
        }
      } else {
        0
      }
  var useCustomNavBar: Boolean = false

  private var navBarConfig: NavBarConfig = NavBarConfig()

  private val onNavigationItemReselectedListeners =
    mutableListOf<NavigationBarView.OnItemReselectedListener>()

  fun setup() {
    onWindowSizeChanged()
  }

  private fun onWindowSizeChanged() {
    val bounds = getWindowBounds()

    val widthDp = bounds.width() / context.resources.displayMetrics.density
    val widthWindowSizeClass = when {
      widthDp < 600f -> WindowSizeClass.COMPACT
      widthDp < 840f -> WindowSizeClass.MEDIUM
      else -> WindowSizeClass.EXPANDED
    }

    val heightDp = bounds.height() / context.resources.displayMetrics.density
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
      NavigationRailView(context).apply {
        setBackgroundColor(
          context.getColorFromAttribute(
            com.google.android.material.R.attr.backgroundColor,
          ),
        )
        inflateMenu(R.menu.bottom_navigation_menu)
        labelVisibilityMode = LABEL_VISIBILITY_LABELED
      }
    } else {
      BottomNavigationView(context).apply {
        setBackgroundColor(
          context.getColorFromAttribute(
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
      onNavBarChanged(navBar)
    }
  }

  fun onInsetsChanged(leftInset: Int, topInset: Int, rightInset: Int, bottomInset: Int) {
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

  fun showBottomNav() {
    animateNavBar(1f)
  }

  fun animateNavBar(percentShown: Float, animate: Boolean? = null) {
    if (!useBottomNavBar) return

    val percentShown = percentShown.coerceIn(0f, 1f)

    val currentPercentShown: Float = if (useNavigationRail) {
      if (navBarContainer.width == 0) {
        Log.d(TAG, "navbar percentShown set before laid out")
        return
      } else {
        (navBarContainer.width.toFloat() + navBarContainer.translationX) / navBarContainer.width
      }
    } else {
      if (navBarContainer.height == 0) {
        Log.d(TAG, "navbar percentShown set before laid out")
        return
      } else {
        (navBarContainer.height - navBarContainer.translationY) / navBarContainer.height
      }
    }

    if (currentPercentShown + 0.01f > percentShown && currentPercentShown - 0.01f < percentShown) {
      return
    }

    val animate = animate ?: (abs(currentPercentShown - percentShown) > 0.1f)

    // To easily debug what is messing with the nav bar, add an exception to this log statement.
    Log.d(
      TAG,
      "animateNavBar() p$percentShown diff${abs(currentPercentShown - percentShown)}",
      kotlin.RuntimeException(),
    )

    val navigationBarOffset =
      if (useNavigationRail) {
        -navBarContainer.width.toFloat()
      } else {
        navBarContainer.height.toFloat()
      } * (1f - percentShown)

    if (useNavigationRail) {
      if (navBarContainer.visibility != View.VISIBLE || navBarContainer.alpha == 0f) {
        navBarContainer.visibility = View.VISIBLE
        navBarContainer.translationX = -navBar.width.toFloat() * (1f - percentShown)

        navBarContainer.animate().cancel()
        navBarContainer.animate()
          .translationX(navigationBarOffset)
          .alpha(1f)
          .apply {
            duration = 250
          }
      } else if (animate) {
        navBarContainer.animate().cancel()
        navBarContainer.animate()
          .alpha(1f)
          .translationX(-navigationBarOffset).duration = 250
      } else {
        navBarContainer.animate().cancel()
        navBarContainer.translationX = navigationBarOffset
      }
    } else {
      if (navBarContainer.visibility != View.VISIBLE || navBarContainer.alpha == 0f) {
        navBarContainer.visibility = View.VISIBLE
        navBarContainer.translationY = navBarContainer.height.toFloat() * (1f - percentShown)

        navBarContainer.animate().cancel()
        navBarContainer.animate()
          .translationY(navigationBarOffset)
          .alpha(1f)
          .apply {
            duration = 250
          }
      } else if (animate) {
        navBarContainer.animate().cancel()
        navBarContainer.animate()
          .alpha(1f)
          .translationY(navigationBarOffset)
          .apply {
            duration = 250
          }
      } else {
        navBarContainer.animate().cancel()
        navBarContainer.translationY = navigationBarOffset
      }
    }
  }

  fun hideNavBar() {
    animateNavBar(0f)
  }

  fun updatePaddingForNavBar(contentContainer: View) {
    if (useNavigationRail) {
      val width = if (navBarContainer.width == 0) {
        context.getDimen(
          com.google.android.material.R.dimen.m3_navigation_rail_default_width,
        )
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
    showLabels = preferences.showLabelsInNavBar
    navRailGravity = preferences.navRailGravity

    navRailMode =
      if (globalLayoutMode == GlobalLayoutModes.SmallScreen) {
        navRailMode
      } else {
        NavigationRailModeId.Auto
      }

    onWindowSizeChanged()

    navBar.labelVisibilityMode = if (showLabels) {
      NavigationBarView.LABEL_VISIBILITY_LABELED
    } else {
      NavigationBarView.LABEL_VISIBILITY_UNLABELED
    }

    (navBar as? NavigationRailView)?.let { navBar ->
      navBar.menuGravity = when (navRailGravity) {
        NavRailGravityIds.TOP -> Gravity.TOP
        NavRailGravityIds.CENTER -> Gravity.CENTER
        NavRailGravityIds.BOTTOM -> Gravity.BOTTOM
        else -> Gravity.TOP
      }
    }

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
                setIcon(R.drawable.ic_home_selector)
              }
            }
            NavBarDestinations.Saved -> {
              add(
                Menu.NONE,
                R.id.filteredPostsAndCommentsTabbedFragment,
                Menu.NONE,
                context.getString(R.string.saved),
              ).apply {
                setIcon(R.drawable.ic_bookmarks_selector)
              }
            }
            NavBarDestinations.Search -> {
              add(
                Menu.NONE,
                R.id.searchHomeFragment,
                Menu.NONE,
                context.getString(R.string.search),
              ).apply {
                setIcon(R.drawable.ic_search_selector)
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
                setIcon(R.drawable.ic_inbox_selector)
              }
            }
            NavBarDestinations.Profile -> {
              add(
                Menu.NONE,
                R.id.personTabbedFragment2,
                Menu.NONE,
                context.getString(R.string.user_profile),
              ).apply {
                setIcon(R.drawable.ic_account_selector)
              }
            }
            NavBarDestinations.You -> {
              add(
                Menu.NONE,
                R.id.youFragment,
                Menu.NONE,
                context.getString(R.string.you),
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

  fun setupWithNavController(navController: NavController) {
    setupWithNavController(navBar, navController)

    navBar.setOnItemReselectedListener { menuItem ->
      Log.d(TAG, "Reselected nav item: ${menuItem.itemId}")

      if (onNavigationItemReselectedListeners.isEmpty()) {
        if (navController.currentDestination?.id != menuItem.itemId) {
          navController.popBackStack(menuItem.itemId, false)
//                    navController.navigate(menuItem.itemId)
        }
      } else {
        onNavigationItemReselectedListeners.forEach {
          it.onNavigationItemReselected(menuItem)
        }
      }
    }
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

  /**
   * From [NavigationUI.setupWithNavController]
   */
  private fun setupWithNavController(
    navigationBarView: NavigationBarView,
    navController: NavController,
  ) {
    navigationBarView.setOnItemSelectedListener { item ->
      NavigationUI.onNavDestinationSelected(
        item,
        navController,
      )
    }

    val weakReference = WeakReference(navigationBarView)
    navController.addOnDestinationChangedListener(
      object : NavController.OnDestinationChangedListener {
        override fun onDestinationChanged(
          controller: NavController,
          destination: NavDestination,
          arguments: Bundle?,
        ) {
          val view = weakReference.get()
          if (view == null) {
            navController.removeOnDestinationChangedListener(this)
            return
          }
          if (destination is FloatingWindow) {
            return
          }

          // controller.currentBackStack.value.map { it.destination }
          var found = false
          view.menu.forEach { item ->
            if (destination.matchDestination(item.itemId)) {
              item.isChecked = true
              found = true
            }
          }

          val idToItem = mutableMapOf<Int, MenuItem>()
          view.menu.forEach {
            idToItem[it.itemId] = it
          }

          if (!found) {
            for (entry in navController.backQueueInternal().asReversed()) {
              if (idToItem.contains(entry.destination.id)) {
                idToItem[entry.destination.id]?.isChecked = true
                break
              }
            }
          }
        }
      },
    )
  }

  fun navigateWithArguments(
    @IdRes menuItemId: Int,
    navController: NavController,
    args: Bundle? = null,
  ) {
    val menuItem = navBar.menu.findItem(menuItemId)

    onNavDestinationSelected(
      menuItem.itemId,
      menuItem.order and Menu.CATEGORY_SECONDARY == 0,
      navController,
      args,
    )
  }

  fun navigateWithArguments(
    destinationId: Int,
    isTopLevel: Boolean,
    navController: NavController,
    args: Bundle? = null,
  ) {
    onNavDestinationSelected(
      destinationId,
      isTopLevel,
      navController,
      args,
    )
  }

  /**
   * This is a copy of NavigationUI.onNavDestinationSelected()
   */
  private fun onNavDestinationSelected(
    destinationId: Int,
    isTopLevel: Boolean,
    navController: NavController,
    args: Bundle? = null,
  ): Boolean {
    val builder = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
    if (
      navController.currentDestination!!.parent!!.findNode(destinationId)
        is ActivityNavigator.Destination
    ) {
      builder.setEnterAnim(androidx.navigation.ui.R.anim.nav_default_enter_anim)
        .setExitAnim(androidx.navigation.ui.R.anim.nav_default_exit_anim)
        .setPopEnterAnim(androidx.navigation.ui.R.anim.nav_default_pop_enter_anim)
        .setPopExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_exit_anim)
    } else {
      builder.setEnterAnim(androidx.navigation.ui.R.animator.nav_default_enter_anim)
        .setExitAnim(androidx.navigation.ui.R.animator.nav_default_exit_anim)
        .setPopEnterAnim(androidx.navigation.ui.R.animator.nav_default_pop_enter_anim)
        .setPopExitAnim(androidx.navigation.ui.R.animator.nav_default_pop_exit_anim)
    }
    if (isTopLevel) {
      builder.setPopUpTo(
        navController.graph.findStartDestination().id,
        inclusive = false,
        saveState = true,
      )
    }
    val options = builder.build()
    return try {
      // TODO provide proper API instead of using Exceptions as Control-Flow.
      navController.navigate(destinationId, args, options)
      // Return true only if the destination we've navigated to matches the MenuItem
      navController.currentDestination?.matchDestination(destinationId) == true
    } catch (e: IllegalArgumentException) {
      false
    }
  }

  private fun NavController.backQueueInternal(): ArrayDeque<NavBackStackEntry> {
    val impl = NavController::class.java.getDeclaredField("impl").let { field ->
      field.isAccessible = true
      field.get(this@backQueueInternal)
    }

    return impl::class.java.getDeclaredField("backQueue").let { field ->
      field.isAccessible = true
      @Suppress("UNCHECKED_CAST")
      field.get(impl) as ArrayDeque<NavBackStackEntry>
    }
  }

  private fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
    hierarchy.any { it.id == destId }

  enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }
}
