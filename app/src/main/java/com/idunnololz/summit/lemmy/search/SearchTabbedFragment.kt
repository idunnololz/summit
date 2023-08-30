package com.idunnololz.summit.lemmy.search

import android.app.SearchManager
import android.app.SearchableInfo
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.databinding.FragmentSearchBinding
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.community.ViewPagerController
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toSortOrder
import com.idunnololz.summit.lemmy.utils.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.showSortTypeMenu
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saved.SavedTabbedFragment
import com.idunnololz.summit.search.CustomSearchSuggestionsAdapter
import com.idunnololz.summit.search.SuggestionProvider
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.focusAndShowKeyboard
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchTabbedFragment :
    BaseFragment<FragmentSearchBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private const val TAG = "SearchTabbedFragment"

        private const val ARG_SUGGESTION_TO_DELETE = "ARG_SUGGESTION_TO_DELETE"
    }

    private val args by navArgs<SearchTabbedFragmentArgs>()

    val viewModel: SearchViewModel by viewModels()
    val actionsViewModel: MoreActionsViewModel by viewModels()
    var viewPagerController: ViewPagerController? = null

    private var searchSuggestionsAdapter: CustomSearchSuggestionsAdapter? = null

    @Inject
    lateinit var preferences: Preferences

    private val searchViewBackPressedHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            hideSearch()
        }
    }
    private val queryBackPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            resetQuery()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSearchBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            searchViewBackPressedHandler,
        )

        val context = requireContext()

        viewModel.setSortType(args.sortType)
        if (args.query.isNotBlank()) {
            binding.searchEditText.setText(args.query)
            performSearch()
        }

        requireMainActivity().apply {
            setupForFragment<SavedTabbedFragment>()

            insetViewAutomaticallyByPaddingAndNavUi(
                viewLifecycleOwner, binding.coordinatorLayoutContainer)
        }

        with(binding) {
            val allTabs = listOf(
                SearchType.All,
                SearchType.Posts,
                SearchType.Comments,
                SearchType.Communities,
                SearchType.Users,
                SearchType.Url,
            )

            if (viewPager.adapter == null) {
                viewPager.offscreenPageLimit = 5
                val adapter =
                    ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)

                allTabs.forEach {
                    adapter.addFrag(
                        SearchResultsFragment::class.java,
                        it.toLocalizedString(),
                        SearchResultsFragmentArgs(
                            it,
                        ).toBundle(),
                    )
                }
                viewPager.adapter = adapter
            }

            val adapter: ViewPagerAdapter? = viewPager.adapter as? ViewPagerAdapter

            TabLayoutMediator(
                tabLayout,
                binding.viewPager,
                binding.viewPager.adapter as ViewPagerAdapter,
            ).attachWithAutoDetachUsingLifecycle(viewLifecycleOwner)

            binding.viewPager.registerOnPageChangeCallback(
                object : OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)

                        val tabName = adapter?.getTitleForPosition(position)
                        Log.d(
                            TAG,
                            "onPageSelected: $position ($tabName)",
                        )

                        val searchType = allTabs[position]

                        viewModel.setActiveType(searchType)
                    }
                },
            )

            viewPagerController = ViewPagerController(
                this@SearchTabbedFragment,
                topViewPager,
                childFragmentManager,
                viewModel,
                true,
                compatibilityMode = preferences.compatibilityMode,
                retainClosedPosts = preferences.retainLastPost,
            ) {
                if (it == 0) {
                    val lastSelectedPost = viewModel.lastSelectedPost
                    if (lastSelectedPost != null) {
                        // We came from a post...
//                        adapter?.highlightPost(lastSelectedPost)
                        viewModel.lastSelectedPost = null
                    }
                } else {
                    val lastSelectedPost = viewModel.lastSelectedPost
                    if (lastSelectedPost != null) {
//                        adapter?.highlightPostForever(lastSelectedPost)
                    }
                }
            }.apply {
                init()
            }
            topViewPager.disableLeftSwipe = true

            installOnActionResultHandler(
                actionsViewModel = actionsViewModel,
                snackbarContainer = binding.coordinatorLayout,
            )

            val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            val searchableInfo: SearchableInfo? = searchManager.getSearchableInfo(
                requireActivity().componentName,
            )

            if (searchableInfo == null) {
                Log.d(TAG, "searchableInfo is null!")
            }

            val searchSuggestionsAdapter = CustomSearchSuggestionsAdapter(
                context,
                searchableInfo,
                viewModel.viewModelScope,
            )

            this@SearchTabbedFragment.searchSuggestionsAdapter = searchSuggestionsAdapter

            searchSuggestionsRecyclerView.setHasFixedSize(false)
            searchSuggestionsRecyclerView.adapter = searchSuggestionsAdapter
            searchSuggestionsRecyclerView.layoutManager = LinearLayoutManager(context)

            searchSuggestionsAdapter.setListener(
                object : CustomSearchSuggestionsAdapter.OnSuggestionListener {
                    override fun onSuggestionsChanged(newSuggestions: List<String>) {}

                    override fun onSuggestionSelected(query: String) {
                        searchEditText.setText(query)
                        launchSearch()
                    }

                    override fun onSuggestionLongClicked(query: String) {
                        AlertDialogFragment.Builder()
                            .setTitle(R.string.delete_suggest)
                            .setPositiveButton(android.R.string.ok)
                            .setNegativeButton(android.R.string.cancel)
                            .setExtra(ARG_SUGGESTION_TO_DELETE, query)
                            .createAndShow(childFragmentManager, "asdf")
                    }
                },
            )
            searchSuggestionsAdapter.copyTextToSearchViewClickedListener = {
                searchEditText.setText(it)
            }

            searchSuggestionsAdapter.setQuery("")

            searchEditText.addTextChangedListener {
                searchSuggestionsAdapter.setQuery(it?.toString() ?: "")

                updateQueryBackHandler()
            }

            searchEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    launchSearch()
                }
                true
            }
            searchEditText.setOnKeyListener(
                object : View.OnKeyListener {
                    override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                        // If the event is a key-down event on the "enter" button
                        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                            launchSearch()
                            return true
                        }
                        return false
                    }
                },
            )

            searchEditTextDummy.setOnClickListener {
                showSearch()
            }

            viewModel.currentQueryLiveData.observe(viewLifecycleOwner) {
                searchEditTextDummy.setText(it)
                updateQueryBackHandler()
            }

            if (viewModel.currentQueryFlow.value.isNotBlank()) {
                hideSearch(animate = false)
            }

            fab.setOnClickListener {
                showMoreOptions()
            }
        }
    }

    private fun showMoreOptions() {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        val bottomMenu = BottomMenu(context).apply {
            setTitle(R.string.more_actions)

            addItemWithIcon(
                R.id.sort_order,
                getString(R.string.sort_order),
                R.drawable.baseline_sort_24,
            )

            addItemWithIcon(
                R.id.clear_search_history,
                getString(R.string.clear_search_history),
                R.drawable.baseline_delete_24,
            )

            setOnMenuItemClickListener {
                when (it.id) {
                    R.id.sort_order -> {
                        showSortTypeMenu(
                            getCurrentSortType = {
                                viewModel.currentSortTypeFlow.value.toSortOrder()
                            },
                            onSortOrderSelected = {
                                viewModel.setSortType(it.toApiSortOrder())
                            },
                        )
                    }
                    R.id.clear_search_history -> {
                        val adapter = binding.searchSuggestionsRecyclerView.adapter as?
                            CustomSearchSuggestionsAdapter

                        adapter?.clearSuggestions()
                    }
                }
            }
        }

        getMainActivity()?.showBottomMenu(bottomMenu)
    }

    private fun showSearch() {
        if (!isBindingAvailable()) return

        binding.searchContainer.visibility = View.VISIBLE
        binding.searchContainer.alpha = 0f
        binding.searchContainer.animate()
            .alpha(1f)
        binding.searchEditText.requestFocus()
        binding.root.findFocus()?.focusAndShowKeyboard()

        searchViewBackPressedHandler.isEnabled = true
    }

    private fun hideSearch(animate: Boolean = true) {
        Utils.hideKeyboard(requireMainActivity())

        if (animate) {
            binding.searchContainer.animate()
                .alpha(0f)
                .withEndAction {
                    if (isBindingAvailable()) {
                        binding.searchContainer.visibility = View.GONE
                        binding.searchContainer.alpha = 1f
                    }
                }
        } else {
            binding.searchContainer.visibility = View.GONE
            binding.searchContainer.alpha = 1f
        }

        searchViewBackPressedHandler.isEnabled = false
    }

    private fun launchSearch() {
        if (!isBindingAvailable()) return

        val query = binding.searchEditText.text
        val queryString = query?.toString() ?: ""

        val directions = SearchTabbedFragmentDirections.actionSearchFragmentSelf(
            queryString,
            viewModel.currentSortTypeFlow.value,
        )
        findNavController().navigateSafe(directions)
    }

    private fun performSearch() {
        if (!isBindingAvailable()) return

        val query = binding.searchEditText.text
        val queryString = query?.toString() ?: ""

        saveSuggestion(queryString)

        Log.d(TAG, "performing search with query '$query'")

        hideSearch()

        viewModel.updateCurrentQuery(queryString)
    }

    private fun saveSuggestion(query: String) {
        val context = context ?: return
        val suggestions = SearchRecentSuggestions(
            context,
            SuggestionProvider.AUTHORITY,
            SuggestionProvider.MODE,
        )
        suggestions.saveRecentQuery(query, null)
    }

    fun SearchType.toLocalizedString() =
        when (this) {
            SearchType.All -> getString(R.string.all)
            SearchType.Comments -> getString(R.string.comments)
            SearchType.Posts -> getString(R.string.posts)
            SearchType.Communities -> getString(R.string.communities)
            SearchType.Users -> getString(R.string.users)
            SearchType.Url -> getString(R.string.urls)
        }

    private fun resetQuery() {
        viewModel.updateCurrentQuery("")
        binding.searchEditText.setText("")
        binding.searchEditTextDummy.setText("")
    }

    private fun updateQueryBackHandler() {
        if (!isBindingAvailable()) return

        queryBackPressHandler.isEnabled = !(
            binding.searchEditText.text.isNullOrBlank() &&
                binding.searchEditTextDummy.text.isNullOrBlank()
            )
    }

    fun closePost(postFragment: PostFragment) {
        viewPagerController?.closePost(postFragment)
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        val context = context ?: return

        val suggestionToDelete = dialog.getExtra(ARG_SUGGESTION_TO_DELETE)
        if (suggestionToDelete != null) {
            val searchManager = context?.getSystemService(Context.SEARCH_SERVICE) as? SearchManager
            val searchableInfo: SearchableInfo? = searchManager?.getSearchableInfo(
                requireActivity().componentName,
            )
            val searchable = searchableInfo ?: return
            val authority = searchable.suggestAuthority ?: return

            val uriBuilder = Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .query("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
                .fragment("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
                .appendEncodedPath("suggestions")

            val uri = uriBuilder.build()

            // finally, make the query
            context.contentResolver.delete(
                uri,
                "query = '$suggestionToDelete'",
                null,
            )

            searchSuggestionsAdapter?.refreshSuggestions()
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}
