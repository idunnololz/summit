package com.idunnololz.summit.main

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.discord.panels.OverlappingPanelsLayout
import com.discord.panels.PanelState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentMainBinding
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.community.CommunityFragmentArgs
import com.idunnololz.summit.user.*
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.attachNavHostFragment
import com.idunnololz.summit.util.ext.detachNavHostFragment
import com.idunnololz.summit.util.ext.getCurrentNavigationFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : BaseFragment<FragmentMainBinding>() {

    companion object {
        private val TAG = MainFragment::class.java.canonicalName

        private val SIS_FRAGMENT_TAGS = "SIS_FRAGMENT_TAGS"


        fun getTagForTab(tabId: Long): String = "innerFragment:$tabId"
        fun getIdFromTag(tag: String): Long = tag.split(":")[1].toLong()
    }

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val args: MainFragmentArgs by navArgs()

    private lateinit var firstFragmentTag: String

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    private val fragmentTags = hashSetOf<String>()

    private var currentNavController: NavController? = null

    private val deferredNavigationRequests = mutableListOf<Runnable>()

    private val onDestinationChangedListener =
        NavController.OnDestinationChangedListener { controller, destination, arguments ->
            Log.d(TAG, "onDestinationChangedListener(): ${destination.label}")

            //tabsManager.persistTabBackstack(controller.saveState())
        }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (currentNavController?.navigateUp() == false) {
                if (!findNavController().navigateUp()) {
                    getMainActivity()?.finish()
                }
            }
        }
    }

    private val onNavigationItemReselectedListener =
        BottomNavigationView.OnNavigationItemReselectedListener a@{
            if (it.itemId == R.id.mainFragment) {
                val tabId = userCommunitiesManager.currentTabId.value ?: return@a

                val tabItem = checkNotNull(userCommunitiesManager.getTab(tabId))

                currentNavController?.popBackStack(R.id.community, false)

                currentNavController?.navigate(
                    R.id.communityFragment,
                    tabItem.toCommunityFragmentArgs()
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getStringArrayList(SIS_FRAGMENT_TAGS)?.forEach {
            fragmentTags.add(it)
        }
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this,
            onBackPressedCallback
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        postponeEnterTransition()

        setBinding(FragmentMainBinding.inflate(inflater, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firstTab = checkNotNull(
            userCommunitiesManager.getTab(UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID))
        firstFragmentTag = getTagForTab(firstTab.id)
        val navHostFragment = obtainNavHostFragment(
            childFragmentManager,
            firstFragmentTag,
            R.navigation.community,
            R.id.innerNavHostContainer,
            firstTab.toCommunityFragmentArgs()
        )

        attachNavHostFragment(childFragmentManager, navHostFragment, true)
        if (userCommunitiesManager.currentTabId.value == UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID) {
            setCurrentNavController(navHostFragment.navController)
        }

        userCommunitiesManager.getAllUserCommunities().forEach { tab ->
            if (userCommunitiesManager.currentTabId.value != UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID) {
                childFragmentManager.findFragmentByTag(getTagForTab(tab.id))?.let {
                    detachNavHostFragment(
                        childFragmentManager,
                        it as NavHostFragment
                    )
                }
            }
        }

        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                if (userCommunitiesManager.currentTabId.value != firstTab.id) {
                    userCommunitiesManager.currentTabId.value = firstTab.id
                    setCurrentNavController(navHostFragment.navController)
                }
            }
        }

        userCommunitiesManager.userCommunitiesChangedLiveData.observe(viewLifecycleOwner) {
            purgeUnusedFragments()
        }

        userCommunitiesManager.currentTabId.observe(viewLifecycleOwner) { tabIndex ->
            changeTab(tabIndex)
        }

        if (savedInstanceState == null) {
            if (args.url != null) {
                val uri = Uri.parse(args.url)
                navigateToUri(uri)
            }
        }

        ArrayList(deferredNavigationRequests).forEach {
            it.run()
        }
        deferredNavigationRequests.clear()

        binding.rootView.post {
            startPostponedEnterTransition()
        }

        binding.rootView.registerStartPanelStateListeners(object : OverlappingPanelsLayout.PanelStateListener {
            override fun onPanelStateChange(panelState: PanelState) {
                Log.d(TAG, "panelState: ${panelState}")
                when (panelState) {
                    PanelState.Closed -> {
                        getMainActivity()?.setNavUiOpenness(0f)
                    }
                    is PanelState.Closing -> {
                        getMainActivity()?.setNavUiOpenness(panelState.progress)
                    }
                    PanelState.Opened -> {
                        getMainActivity()?.setNavUiOpenness(100f)
                    }
                    is PanelState.Opening -> {
                        getMainActivity()?.setNavUiOpenness(panelState.progress)
                    }
                }
            }
        })
        binding.rootView.registerEndPanelStateListeners(object : OverlappingPanelsLayout.PanelStateListener {
            override fun onPanelStateChange(panelState: PanelState) {
                Log.d(TAG, "panelState: ${panelState}")
                when (panelState) {
                    PanelState.Closed -> {
                        getMainActivity()?.setNavUiOpenness(0f)
                    }
                    is PanelState.Closing -> {
                        getMainActivity()?.setNavUiOpenness(panelState.progress)
                    }
                    PanelState.Opened -> {
                        getMainActivity()?.setNavUiOpenness(100f)
                    }
                    is PanelState.Opening -> {
                        getMainActivity()?.setNavUiOpenness(panelState.progress)
                    }
                }
            }
        })

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                val currentNavController = currentNavController
                if (menuItem.itemId == android.R.id.home && currentNavController != null) {
                    return currentNavController.navigateUp()
                }
                return false
            }

        })

        navHostFragment.navController.addOnDestinationChangedListener { controller, destination, arguments ->
            if (destination.id == R.id.postFragment || destination.id == R.id.communityFragment) {
                binding.rootView.setStartPanelLockState(OverlappingPanelsLayout.LockState.UNLOCKED)
                binding.rootView.setEndPanelLockState(OverlappingPanelsLayout.LockState.UNLOCKED)
            } else {
                binding.rootView.setStartPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)
                binding.rootView.setEndPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)
            }
        }
    }

    fun navigateToUri(uri: Uri) {
        if (currentNavController == null) {
            deferredNavigationRequests.add {
                navigateToUri(uri)
            }
            return
        }

        val pathSegments = uri.pathSegments
        if (pathSegments.size >= 1) {
            if (pathSegments[0].equals("r", ignoreCase = true)) {
                if (pathSegments.size == 2) {
//                    currentNavController?.navigate(
//                        R.id.subredditFragment,
//                        CommunityFragmentArgs(
//                            url = RedditUtils.extractPrefixedSubreddit(uri.toString())
//                        ).toBundle()
//                    )
                } else if (pathSegments.size > 2) {
//                    currentNavController?.navigate(
//                        R.id.postFragment,
//                        PostFragmentArgs(url = uri.toString()).toBundle()
//                    )

                    TODO()
                } else {
                    Log.d(TAG, "Unable to handle uri $uri")

                    AlertDialogFragment.Builder()
                        .setMessage(getString(R.string.error_unable_to_handle_link, uri.toString()))
                        .createAndShow(childFragmentManager, "error")
                }
            } else if (pathSegments[0].equals("comments", ignoreCase = true)) {
//                currentNavController?.navigate(
//                    R.id.postFragment,
//                    PostFragmentArgs(url = uri.toString()).toBundle()
//                )
                TODO()
            } else {
                Log.d(TAG, "Unable to handle uri $uri")

                AlertDialogFragment.Builder()
                    .setMessage(getString(R.string.error_unable_to_handle_link, uri.toString()))
                    .createAndShow(childFragmentManager, "error")
            }
        } else {
            Log.d(TAG, "Unable to handle uri $uri")

            AlertDialogFragment.Builder()
                .setMessage(getString(R.string.error_unable_to_handle_link, uri.toString()))
                .createAndShow(childFragmentManager, "error")
        }
    }

    private var lastNavHostFragment: NavHostFragment? = null
    private fun changeTab(tabId: Long?) {
        tabId ?: return

        lastNavHostFragment?.navController?.removeOnDestinationChangedListener(
            onDestinationChangedListener
        )

        val selectedNavHostFragment =
            updateAttachedFragment(checkNotNull(userCommunitiesManager.getTab(tabId)))
        selectedNavHostFragment.navController.addOnDestinationChangedListener(
            onDestinationChangedListener
        )
        lastNavHostFragment = selectedNavHostFragment
    }

    private fun setCurrentNavController(navController: NavController) {
        Log.d(TAG, "currentNavController: ${navController.currentDestination?.label}")
        currentNavController = navController
        sharedViewModel.currentNavController.value = currentNavController
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putStringArrayList(
            SIS_FRAGMENT_TAGS,
            arrayListOf<String>().apply { addAll(fragmentTags) })
    }

    override fun onResume() {
        super.onResume()

        requireMainActivity().registerOnNavigationItemReselectedListener(
            onNavigationItemReselectedListener
        )
    }

    override fun onPause() {
        requireMainActivity().unregisterOnNavigationItemReselectedListener(
            onNavigationItemReselectedListener
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

    private fun updateAttachedFragment(currentTab: UserCommunityItem): NavHostFragment {
        val fragmentManager = childFragmentManager
        fragmentManager.popBackStack(firstFragmentTag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val newlySelectedItemTag = getTagForTab(currentTab.id)
        val selectedFragment = obtainNavHostFragment(
            childFragmentManager,
            newlySelectedItemTag,
            R.navigation.community,
            R.id.innerNavHostContainer,
            currentTab.toCommunityFragmentArgs()
        )

        setCurrentNavController(selectedFragment.navController)

        // Exclude the first fragment tag because it's always in the back stack.
        if (firstFragmentTag != newlySelectedItemTag) {
            // Commit a transaction that cleans the back stack and adds the first fragment
            // to it, creating the fixed started destination.
            fragmentManager.beginTransaction()
                .setCustomAnimations(
                    androidx.navigation.ui.R.anim.nav_default_enter_anim,
                    androidx.navigation.ui.R.anim.nav_default_exit_anim,
                    androidx.navigation.ui.R.anim.nav_default_pop_enter_anim,
                    androidx.navigation.ui.R.anim.nav_default_pop_exit_anim
                )
                .attach(selectedFragment)
                .setPrimaryNavigationFragment(selectedFragment)
                .apply {
                    // Detach all other Fragments
                    userCommunitiesManager.getAllUserCommunities().forEach { tab ->
                        if (getTagForTab(tab.id) != newlySelectedItemTag) {
                            detach(fragmentManager.findFragmentByTag(firstFragmentTag)!!)
                        }
                    }
                }
                .addToBackStack(firstFragmentTag)
                .setReorderingAllowed(true)
                .commit()
        } else {
            fragmentManager.beginTransaction()
                .setPrimaryNavigationFragment(selectedFragment)
                .commit()
        }

        return selectedFragment
    }

    private fun purgeUnusedFragments() {
        val unusedFragmentTags = fragmentTags
            .map { getIdFromTag(it) }
            .filter { userCommunitiesManager.getTab(it) == null }
            .map { getTagForTab(it) }

        purgeFragments(unusedFragmentTags)
    }

    private fun purgeFragments(fragmentsToRemoveTags: List<String>) {
        val fragmentManager = childFragmentManager
        val fragmentsToRemove = arrayListOf<Fragment>()

        fragmentsToRemoveTags.forEach {
            val existingFragment = fragmentManager.findFragmentByTag(it)
            check(fragmentTags.remove(it)) { "Expected tag to be in fragmentTags but was not: $it" }
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

    private fun obtainNavHostFragment(
        fragmentManager: FragmentManager,
        fragmentTag: String,
        navGraphId: Int,
        containerId: Int,
        startDestinationArgs: Bundle? = null
    ): NavHostFragment {
        fragmentTags.add(fragmentTag)

        return com.idunnololz.summit.util.ext.obtainNavHostFragment(
            fragmentManager,
            fragmentTag,
            navGraphId,
            containerId,
            startDestinationArgs
        )
    }

    fun UserCommunityItem.toCommunityFragmentArgs(): Bundle =
        CommunityFragmentArgs(communityRef = communityRef).toBundle()

    fun restoreTabState(state: TabCommunityState) {
        // we need to check if tab is still open since user could have closed it
        if (userCommunitiesManager.getTab(state.tabId) == null) {
            // tab was closed... restore it
            sharedViewModel.addTab(
                UserCommunityItem(
                    id = state.tabId,
                    communityRef = state.viewState.communityState.communityRef
                )
            )
        }

        if (userCommunitiesManager.currentTabId.value != state.tabId) {
            userCommunitiesManager.currentTabId.value = state.tabId
        }

        val runnable = object : Runnable {
            override fun run() {
                val fragmentTag = getTagForTab(state.tabId)
                val fragment = childFragmentManager.findFragmentByTag(fragmentTag)

                if (fragment?.isAdded != true) {
                    binding.rootView.post(this)
                } else {
                    val curFrag = childFragmentManager.getCurrentNavigationFragment()
                    if (curFrag is CommunityFragment) {
                        curFrag.restoreState(state.viewState, reload = true)
                    } else {
                        // Pop the back stack to the start destination of the current navController graph
                        fragment.findNavController().let {
                            it.popBackStack(
                                it.graph.startDestinationId, false
                            )
                        }
                        binding.rootView.post(this)
                    }
                }
            }
        }

        runnable.run()
    }
}