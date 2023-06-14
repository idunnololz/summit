package com.idunnololz.summit.main

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentMainBinding
import com.idunnololz.summit.post.PostFragmentArgs
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.subreddit.SubredditFragment
import com.idunnololz.summit.subreddit.SubredditFragmentArgs
import com.idunnololz.summit.tabs.*
import com.idunnololz.summit.util.*
import com.idunnololz.summit.util.ext.attachNavHostFragment
import com.idunnololz.summit.util.ext.detachNavHostFragment
import com.idunnololz.summit.util.ext.getCurrentNavigationFragment

class MainFragment : BaseFragment<FragmentMainBinding>() {

    companion object {
        private val TAG = MainFragment::class.java.canonicalName

        private val SIS_FRAGMENT_TAGS = "SIS_FRAGMENT_TAGS"


        fun getTagForTab(tabId: String): String = "innerFragment:$tabId"
        fun getIdFromTag(tag: String): String = tag.split(":")[1]
    }

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val args: MainFragmentArgs by navArgs()

    private lateinit var firstFragmentTag: String

    private val tabsManager = TabsManager.instance

    private val fragmentTags = hashSetOf<String>()

    private var currentNavController: NavController? = null

    private val deferredNavigationRequests = mutableListOf<Runnable>()

    private val onDestinationChangedListener =
        NavController.OnDestinationChangedListener { controller, destination, arguments ->
            Log.d(TAG, "onDestinationChangedListener(): ${destination.label}")

            //tabsManager.persistTabBackstack(controller.saveState())
        }

    private val onNavigationItemReselectedListener =
        BottomNavigationView.OnNavigationItemReselectedListener a@{
            if (it.itemId == R.id.main) {
                val tabId = tabsManager.currentTabId.value ?: return@a

                val tabItem = checkNotNull(tabsManager.getTab(tabId))

                currentNavController?.popBackStack(R.id.subreddit, false)

                currentNavController?.navigate(
                    R.id.subredditFragment,
                    tabItem.toSubredditFragmentArgs()
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.getStringArrayList(SIS_FRAGMENT_TAGS)?.forEach {
            fragmentTags.add(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        postponeEnterTransition()
        setHasOptionsMenu(true) // required so that we can handle up press

        setBinding(FragmentMainBinding.inflate(inflater, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firstTab = checkNotNull(tabsManager.getTab(TabsManager.FIRST_FRAGMENT_TAB_ID))
        firstFragmentTag = getTagForTab(firstTab.tabId)
        val navHostFragment = obtainNavHostFragment(
            childFragmentManager,
            firstFragmentTag,
            R.navigation.subreddit,
            R.id.innerNavHostContainer,
            firstTab.toSubredditFragmentArgs()
        )

        attachNavHostFragment(childFragmentManager, navHostFragment, true)
        if (tabsManager.currentTabId.value == TabsManager.FIRST_FRAGMENT_TAB_ID) {
            setCurrentNavController(navHostFragment.navController)
        }

        tabsManager.getAllTabs().forEach { tab ->
            if (tabsManager.currentTabId.value != TabsManager.FIRST_FRAGMENT_TAB_ID) {
                childFragmentManager.findFragmentByTag(getTagForTab(tab.tabId))?.let {
                    detachNavHostFragment(
                        childFragmentManager,
                        it as NavHostFragment
                    )
                }
            }
        }

        childFragmentManager.addOnBackStackChangedListener {
            if (childFragmentManager.backStackEntryCount == 0) {
                if (tabsManager.currentTabId.value != firstTab.tabId) {
                    tabsManager.currentTabId.value = firstTab.tabId
                    setCurrentNavController(navHostFragment.navController)
                }
            }
        }

        requireMainActivity().apply {
            binding.abTabsImageView.setOnClickListener {
                findNavController().navigate(R.id.subredditTabsFragment)
            }
        }

        tabsManager.tabsChangedLiveData.observe(viewLifecycleOwner) {
            if (!it.isLoading) {
                requireMainActivity().binding.abTabsImageView.setTabs(it.getTabsCount())
            }
            purgeUnusedFragments()
        }

        tabsManager.currentTabId.observe(viewLifecycleOwner) { tabIndex ->
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
                    currentNavController?.navigate(
                        R.id.subredditFragment,
                        SubredditFragmentArgs(
                            url = RedditUtils.extractPrefixedSubreddit(uri.toString())
                        ).toBundle()
                    )
                } else if (pathSegments.size > 2) {
                    currentNavController?.navigate(
                        R.id.postFragment,
                        PostFragmentArgs(url = uri.toString(), originalPost = null).toBundle()
                    )
                } else {
                    Log.d(TAG, "Unable to handle uri $uri")

                    AlertDialogFragment.Builder()
                        .setMessage(getString(R.string.error_unable_to_handle_link, uri.toString()))
                        .createAndShow(childFragmentManager, "error")
                }
            } else if (pathSegments[0].equals("comments", ignoreCase = true)) {
                currentNavController?.navigate(
                    R.id.postFragment,
                    PostFragmentArgs(url = uri.toString(), originalPost = null).toBundle()
                )
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
    private fun changeTab(tabIndex: String?) {
        tabIndex ?: return

        lastNavHostFragment?.navController?.removeOnDestinationChangedListener(
            onDestinationChangedListener
        )

        val selectedNavHostFragment =
            updateAttachedFragment(checkNotNull(tabsManager.getTab(tabIndex)))
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentNavController = currentNavController
        if (item.itemId == android.R.id.home && currentNavController != null) {
            return currentNavController.navigateUp()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateAttachedFragment(currentTab: TabItem): NavHostFragment {
        val fragmentManager = childFragmentManager
        fragmentManager.popBackStack(firstFragmentTag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val newlySelectedItemTag = getTagForTab(currentTab.tabId)
        val selectedFragment = obtainNavHostFragment(
            childFragmentManager,
            newlySelectedItemTag,
            R.navigation.subreddit,
            R.id.innerNavHostContainer,
            currentTab.toSubredditFragmentArgs()
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
                    tabsManager.getAllTabs().forEach { tab ->
                        if (getTagForTab(tab.tabId) != newlySelectedItemTag) {
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
            .filter { tabsManager.getTab(it) == null }
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

    fun TabItem.toSubredditFragmentArgs(): Bundle = when (this) {
        is TabItem.PageTabItem ->
            SubredditFragmentArgs(url = pageDetails.url).toBundle()
    }

    fun restoreTabState(state: TabSubredditState) {
        // we need to check if tab is still open since user could have closed it
        if (tabsManager.getTab(state.tabId) == null) {
            // tab was closed... restore it
            tabsManager.addNewTab(
                TabItem.PageTabItem.newTabItem(
                    tabId = state.tabId,
                    url = state.viewState.subredditState.subreddit
                )
            )
        }

        if (tabsManager.currentTabId.value != state.tabId) {
            tabsManager.currentTabId.value = state.tabId
        }

        val runnable = object : Runnable {
            override fun run() {
                val fragmentTag = getTagForTab(state.tabId)
                val fragment = childFragmentManager.findFragmentByTag(fragmentTag)

                if (fragment?.isAdded != true) {
                    binding.rootView.post(this)
                } else {
                    val curFrag = childFragmentManager.getCurrentNavigationFragment()
                    if (curFrag is SubredditFragment) {
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