package com.idunnololz.summit.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.discord.panels.OverlappingPanelsLayout
import com.discord.panels.PanelState
import com.google.android.material.navigation.NavigationBarView
import com.idunnololz.summit.CommunityDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentMainBinding
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.community.CommunityFragmentArgs
import com.idunnololz.summit.lemmy.communityPicker.CommunityPickerDialogFragment
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityEditorDialogFragment
import com.idunnololz.summit.lemmy.post.PostFragmentArgs
import com.idunnololz.summit.main.communitiesPane.CommunitiesPaneController
import com.idunnololz.summit.main.communitiesPane.CommunitiesPaneViewModel
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.tabs.communityRef
import com.idunnololz.summit.tabs.isHomeTab
import com.idunnololz.summit.tabs.toTab
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.attachNavHostFragment
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.obtainNavHostFragment
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.isPredictiveBackSupported
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : BaseFragment<FragmentMainBinding>() {

    companion object {
        private const val TAG = "MainFragment"

        private const val SIS_CURRENT_TAB = "SIS_CURRENT_TAB"

        private fun getTagForTab(tabId: Long): String = "innerFragment:$tabId"
        private fun getTagForTab(communityRef: CommunityRef): String = "innerFragment:$communityRef"

        private fun getTagForTab(tab: TabsManager.Tab): String = when (tab) {
            is TabsManager.Tab.SubscribedCommunityTab ->
                getTagForTab(tab.communityRef)
            is TabsManager.Tab.UserCommunityTab ->
                getTagForTab(tab.userCommunityItem.id)
        }

        private fun getIdFromTag(tag: String): Long? = try {
            tag.split(":")[1].toLong()
        } catch (e: Exception) {
            null
        }
    }

    private val args: MainFragmentArgs by navArgs()

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val viewModel: MainFragmentViewModel by viewModels()
    private val communitiesPaneViewModel: CommunitiesPaneViewModel by viewModels()

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    @Inject
    lateinit var tabsManager: TabsManager

    @Inject
    lateinit var preferences: Preferences

    lateinit var communitiesPaneController: CommunitiesPaneController

    private var currentNavController: NavController? = null

    private val deferredNavigationRequests = mutableListOf<Runnable>()

    private var doubleBackToExitPressedOnce = false

    private val onDestinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            Log.d(TAG, "onDestinationChangedListener(): ${destination.label}")

            if (destination.id == R.id.communityFragment) {
                // Let community fragment handle it...
            } else {
                setStartPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)
            }
        }

    private val paneOnBackPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!isBindingAvailable()) return

            binding.rootView.closePanels()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!isBindingAvailable()) return

            val context = requireContext()

            if (currentNavController?.navigateUp() == false) {
                if (!findNavController().navigateUp()) {
                    val tab = tabsManager.currentTab.value

                    if (tab?.isHomeTab == false) {
                        changeCommunity(tabsManager.getHomeTab())
                        return
                    }

                    if (doubleBackToExitPressedOnce) {
                        getMainActivity()?.finish()
                        return
                    }

                    doubleBackToExitPressedOnce = true
                    Toast.makeText(
                        context,
                        context.getString(R.string.press_back_again_to_exit),
                        Toast.LENGTH_SHORT,
                    ).show()

                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(2000)
                        doubleBackToExitPressedOnce = false
                    }
                }
            }
        }
    }

    private val onNavigationItemReselectedListener =
        NavigationBarView.OnItemReselectedListener a@{
            if (it.itemId == R.id.mainFragment) {
                val tab = tabsManager.currentTab.value ?: return@a

                val currentFragment = getCurrentFragment()

                Log.d(TAG, "currentFragment: $currentFragment")

                if (!tab.isHomeTab &&
                    currentFragment is CommunityFragment && currentFragment.isPristineFirstPage()
                ) {
                    changeCommunity(tabsManager.getHomeTab())
                } else {
                    resetCurrentTab(tab)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getParcelableCompat<TabsManager.Tab>(SIS_CURRENT_TAB)?.let {
            Log.d(TAG, "restoring tab to ${it.communityRef}")
            tabsManager.updateCurrentTabNow(it)
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            onBackPressedCallback,
        )
        childFragmentManager.setFragmentResultListener(
            CommunityPickerDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<CommunityPickerDialogFragment.Result>(
                CommunityPickerDialogFragment.REQUEST_KEY_RESULT,
            )
            if (result != null) {
                userCommunitiesManager.addUserCommunity(
                    communityRef = result.communityRef,
                    icon = result.icon,
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentMainBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        communitiesPaneController = communitiesPaneViewModel.createController(
            binding = binding.startPanel,
            viewLifecycleOwner = viewLifecycleOwner,
            onCommunitySelected = { tabRef, resetTab: Boolean ->
                binding.rootView.closePanels()

                binding.rootView.postDelayed(
                    a@{
                        if (!isBindingAvailable()) return@a

                        val tab = tabsManager.getTab(tabRef)

                        val selectedTabFragment = childFragmentManager.findFragmentByTag(getTagForTab(tab))
                        if (selectedTabFragment?.findNavController() == currentNavController) {
                            if (resetTab) {
                                resetCurrentTab(tab)
                            }

                            // otherwise do nothing because we are already on the right tab
                        } else {
                            changeCommunity(tab)

                            if (resetTab) {
                                resetCurrentTab(tab)
                            }
                        }
                    },
                    200,
                )
            },
            onEditMultiCommunity = {
                MultiCommunityEditorDialogFragment.show(
                    fragmentManager = childFragmentManager,
                    multiCommunity = it.communityRef as CommunityRef.MultiCommunity,
                    dbId = it.id,
                )
            },
            onAddBookmarkClick = {
                val bottomMenu = BottomMenu(context).apply {
                    setTitle(R.string.add_bookmark)
                    addItemWithIcon(
                        id = R.id.add_community,
                        title = R.string.add_community,
                        icon = R.drawable.ic_community_24,
                    )
                    addItemWithIcon(
                        id = R.id.create_multi_community,
                        title = R.string.create_multi_community,
                        icon = R.drawable.baseline_dynamic_feed_24,
                    )

                    setOnMenuItemClickListener {
                        when (it.id) {
                            R.id.add_community -> {
                                CommunityPickerDialogFragment.show(
                                    childFragmentManager,
                                )
                            }
                            R.id.create_multi_community -> {
                                MultiCommunityEditorDialogFragment.show(
                                    childFragmentManager,
                                    CommunityRef.MultiCommunity(
                                        getString(R.string.default_multi_community_name),
                                        null,
                                        listOf(),
                                    ),
                                )
                            }
                        }
                    }
                }
                getMainActivity()?.showBottomMenu(bottomMenu)
            },
        )

        changeCommunity(requireNotNull(tabsManager.currentTab.value))

        viewModel.userCommunitiesChangedEvents.observe(viewLifecycleOwner) {
            it.contentIfNotHandled ?: return@observe

            purgeUnusedFragments()
        }
        viewModel.userCommunitiesUpdated.observe(viewLifecycleOwner) {
            val userCommunityItem = it.contentIfNotHandled ?: return@observe
            val tab = userCommunityItem.toTab()
            val currentTab = tabsManager.currentTab.value
            val isCurrentTabUpdated =
                (currentTab as? TabsManager.Tab.UserCommunityTab)?.userCommunityItem?.id ==
                    userCommunityItem.id

            if (isCurrentTabUpdated) {
                resetCurrentTab(tab)
            } else {
                try {
                    purgeFragments(listOf(getTagForTab(tab)))
                } catch (e: Exception) {
                    // do nothing
                }
            }
        }

        ArrayList(deferredNavigationRequests).forEach {
            it.run()
        }
        deferredNavigationRequests.clear()

        fun updatePaneBackPressHandler() {
            if (!isBindingAvailable()) return

            paneOnBackPressHandler.remove()

            Log.d(
                TAG,
                "updatePaneBackPressHandler(): selected panel: ${binding.rootView.getSelectedPanel()}",
            )
            if (binding.rootView.getSelectedPanel() != OverlappingPanelsLayout.Panel.CENTER) {
                requireMainActivity().onBackPressedDispatcher
                    .addCallback(viewLifecycleOwner, paneOnBackPressHandler)
            }
        }

        binding.rootView
            .registerStartPanelStateListeners(
                object : OverlappingPanelsLayout.PanelStateListener {
                    override fun onPanelStateChange(panelState: PanelState) {
                        when (panelState) {
                            PanelState.Closed -> {
                                getMainActivity()?.let {
                                    it.setNavUiOpenPercent(0f)
                                    Utils.hideKeyboard(it)
                                }

                                updatePaneBackPressHandler()
                            }
                            is PanelState.Closing -> {
                                getMainActivity()?.setNavUiOpenPercent(panelState.progress)
                            }
                            PanelState.Opened -> {
                                getMainActivity()?.setNavUiOpenPercent(100f)
                                communitiesPaneController.onShown()

                                updatePaneBackPressHandler()
                            }
                            is PanelState.Opening -> {
                                getMainActivity()?.setNavUiOpenPercent(panelState.progress)
                            }
                        }
                    }
                },
            )
        binding.rootView.setEndPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)

        addMenuProvider2(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    val currentNavController = currentNavController
                    if (menuItem.itemId == android.R.id.home && currentNavController != null) {
                        return currentNavController.navigateUp()
                    }
                    return false
                }
            },
        )

        requireMainActivity().apply {
            this.insetViewAutomaticallyByPadding(this, binding.startPanel.root)
        }
    }

    fun navigateToPage(page: PageRef, switchToNativeInstance: Boolean) {
        if (!isBindingAvailable()) return

        when (page) {
            is CommunityRef -> {
                currentNavController?.navigate(
                    R.id.communityFragment,
                    CommunityFragmentArgs(
                        tab = tabsManager.currentTab.value,
                        page,
                    ).toBundle(),
                )
            }
            is PostRef -> {
                currentNavController?.navigate(
                    R.id.postFragment,
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
                    R.id.postFragment,
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
                val direction = CommunityDirections.actionGlobalPersonTabbedFragment(page)
                currentNavController?.navigateSafe(direction)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        tabsManager.currentTab.value?.let {
            outState.putParcelable(
                SIS_CURRENT_TAB,
                it,
            )
        }
    }

    override fun onResume() {
        super.onResume()

        requireMainActivity().registerOnNavigationItemReselectedListener(
            onNavigationItemReselectedListener,
        )

        onBackPressedCallback.isEnabled = !(
            isPredictiveBackSupported() &&
                preferences.usePredictiveBack
            )
    }

    override fun onPause() {
        requireMainActivity().unregisterOnNavigationItemReselectedListener(
            onNavigationItemReselectedListener,
        )

        super.onPause()
    }

    override fun onDestroyView() {
        // Fixes some weird crash?
        view?.let { view ->
            Navigation.setViewNavController(view, findNavController())
        }

        super.onDestroyView()
        Log.d(TAG, "onDestroyView()")
    }

    private fun getCurrentFragment(): Fragment? {
        val currentFragment = binding.innerNavHostContainer.getFragment<Fragment>()
        return if (currentFragment is NavHostFragment) {
            currentFragment.childFragmentManager.fragments.getOrNull(0)
        } else {
            null
        }
    }

    private var lastNavHostFragment: NavHostFragment? = null
    private fun changeCommunity(tab: TabsManager.Tab) {
        lastNavHostFragment?.navController?.removeOnDestinationChangedListener(
            onDestinationChangedListener,
        )

        val selectedNavHostFragment = updateAttachedFragment(tab)
        selectedNavHostFragment.navController.addOnDestinationChangedListener(
            onDestinationChangedListener,
        )
        attachNavHostFragment(childFragmentManager, selectedNavHostFragment, true)
        lastNavHostFragment = selectedNavHostFragment

        tabsManager.updateCurrentTab(tab)
    }

    private fun setCurrentNavController(navController: NavController) {
        Log.d(TAG, "currentNavController: ${navController.currentDestination?.label}")
        currentNavController = navController
        sharedViewModel.currentNavController.value = currentNavController
    }

    private fun updateAttachedFragment(currentTab: TabsManager.Tab): NavHostFragment {
        val communityRef = currentTab.communityRef
        val fragmentManager = childFragmentManager
//        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val newlySelectedItemTag = getTagForTab(currentTab)
        Log.d(TAG, "new fragment tag: $newlySelectedItemTag")
        val selectedFragment = obtainNavHostFragment(
            fragmentManager = childFragmentManager,
            fragmentTag = newlySelectedItemTag,
            navGraphId = R.navigation.community,
            containerId = R.id.inner_nav_host_container,
            startDestinationArgs = CommunityFragmentArgs(
                tab = currentTab,
                communityRef = communityRef,
            ).toBundle(),
        )

        setCurrentNavController(selectedFragment.navController)

        // Exclude the first fragment tag because it's always in the back stack.
        // Commit a transaction that cleans the back stack and adds the first fragment
        // to it, creating the fixed started destination.
        fragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                androidx.navigation.ui.R.anim.nav_default_pop_enter_anim,
                androidx.navigation.ui.R.anim.nav_default_pop_exit_anim,
            )
            .attach(selectedFragment)
            .setPrimaryNavigationFragment(selectedFragment)
            .apply {
                // Detach all other Fragments
                fragmentManager.fragments.forEach {
                    if (it.tag != newlySelectedItemTag) {
                        Log.d(TAG, "detaching fragment with tag ${it.tag}")
                        detach(it)
                    }
                }
            }
            .setReorderingAllowed(true)
            .commitNow()

        return selectedFragment
    }

    private fun purgeUnusedFragments() {
//        val unusedFragmentTags = fragmentTags
//            .mapNotNull { getIdFromTag(it) }
//            .filter { userCommunitiesManager.getTab(it) == null }
//            .map { getTagForTab(it) }
//
//        purgeFragments(unusedFragmentTags)
    }

    private fun purgeFragments(fragmentsToRemoveTags: List<String>) {
        val fragmentManager = childFragmentManager
        val fragmentsToRemove = arrayListOf<Fragment>()

        fragmentsToRemoveTags.forEach {
            val existingFragment = fragmentManager.findFragmentByTag(it)
            if (lastNavHostFragment == existingFragment) {
                return@forEach // this is the current fragment, we can't purge it!
            }

            if (existingFragment != null) {
                fragmentsToRemove.add(existingFragment)
                Log.d(TAG, "Removing unused fragment $it")
            } else {
                Log.d(TAG, "Removing unused fragment tag $it")
            }
        }

        if (fragmentsToRemove.isNotEmpty()) {
            val transaction = fragmentManager.beginTransaction()
            fragmentsToRemove.forEach {
                transaction.remove(it)
            }
            transaction.commitNow()
        }
    }

    fun setStartPanelLockState(lockState: OverlappingPanelsLayout.LockState) {
        if (!isBindingAvailable()) {
            return
        }

        Log.d(TAG, "setStartPanelLockState(): $lockState")
        binding.rootView.setStartPanelLockState(lockState)
    }

    fun expandStartPane() {
        binding.rootView.openStartPanel()
    }

    private fun resetCurrentTab(tab: TabsManager.Tab) {
        val currentNavController = currentNavController ?: return

        currentNavController.popBackStack(currentNavController.graph.id, true)
        currentNavController.navigate(
            R.id.communityFragment,
            CommunityFragmentArgs(
                communityRef = tab.communityRef,
                tab = tab,
            ).toBundle(),
        )
    }
}
