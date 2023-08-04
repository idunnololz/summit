package com.idunnololz.summit.lemmy.search

import android.app.SearchManager
import android.app.SearchableInfo
import android.content.Context
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
import com.idunnololz.summit.api.dto.SearchType
import com.idunnololz.summit.databinding.FragmentSearchBinding
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.community.ViewPagerController
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.utils.installOnActionResultHandler
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saved.SavedTabbedFragment
import com.idunnololz.summit.search.CustomSearchSuggestionsAdapter
import com.idunnololz.summit.search.SuggestionProvider
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.focusAndShowKeyboard
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class SearchTabbedFragment : BaseFragment<FragmentSearchBinding>() {

    companion object {
        private const val TAG = "SearchTabbedFragment"
    }

    private val args by navArgs<SearchTabbedFragmentArgs>()

    val viewModel: SearchViewModel by viewModels()
    val actionsViewModel: MoreActionsViewModel by viewModels()
    var viewPagerController: ViewPagerController? = null

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

//        requireActivity().onBackPressedDispatcher.addCallback(
//            viewLifecycleOwner, queryBackPressHandler)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, searchViewBackPressedHandler)

        val context = requireContext()

        if (args.query.isNotBlank()) {
            binding.searchEditText.setText(args.query)
            performSearch()
        }

        requireMainActivity().apply {
            setupForFragment<SavedTabbedFragment>()

            insetViewAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.coordinatorLayoutContainer)
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
                            "onPageSelected: $position (${tabName})",
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

            fab.hide()
        }
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

    private fun launchSearch() {
        if (!isBindingAvailable()) return

        val query = binding.searchEditText.text
        val queryString = query?.toString() ?: ""

        val directions = SearchTabbedFragmentDirections.actionSearchFragmentSelf(queryString)
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

    private fun hideSearch(animate: Boolean = true) {
        Utils.hideKeyboard(requireMainActivity())

        if (animate) {
            binding.searchContainer.animate()
                .alpha(0f)
                .withEndAction {
                    binding.searchContainer.visibility = View.GONE
                    binding.searchContainer.alpha = 1f
                }
        } else {
            binding.searchContainer.visibility = View.GONE
            binding.searchContainer.alpha = 1f
        }

        searchViewBackPressedHandler.isEnabled = false
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
        queryBackPressHandler.isEnabled = !(binding.searchEditText.text.isNullOrBlank() &&
                binding.searchEditTextDummy.text.isNullOrBlank())
    }

    fun closePost(postFragment: PostFragment) {
        viewPagerController?.closePost(postFragment)
    }
}