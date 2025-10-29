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
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.api.ApiFeature
import com.idunnololz.summit.api.dto.lemmy.SearchType
import com.idunnololz.summit.databinding.FragmentSearchBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.util.SlidingPaneController
import com.idunnololz.summit.lemmy.communityPicker.CommunityPickerDialogFragment
import com.idunnololz.summit.lemmy.personPicker.PersonPickerDialogFragment
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.toLocalizedString
import com.idunnololz.summit.lemmy.toSortOrder
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.actions.installOnActionResultHandler
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.lemmy.utils.showSortTypeMenu
import com.idunnololz.summit.links.SiteBackendHelper
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saved.FilteredPostsAndCommentsTabbedFragment
import com.idunnololz.summit.search.CustomSearchSuggestionsAdapter
import com.idunnololz.summit.search.SuggestionProvider
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ViewPagerAdapter
import com.idunnololz.summit.util.ext.attachWithAutoDetachUsingLifecycle
import com.idunnololz.summit.util.ext.focusAndShowKeyboard
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchTabbedFragment : BaseFragment<FragmentSearchBinding>() {

  companion object {
    private const val TAG = "SearchTabbedFragment"

    private const val ARG_SUGGESTION_TO_DELETE = "ARG_SUGGESTION_TO_DELETE"
  }

  private val args by navArgs<SearchTabbedFragmentArgs>()

  val viewModel: SearchTabbedViewModel by viewModels()
  var slidingPaneController: SlidingPaneController? = null

  private var searchSuggestionsAdapter: CustomSearchSuggestionsAdapter? = null

  @Inject
  lateinit var moreActionsHelper: MoreActionsHelper

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var siteBackendHelper: SiteBackendHelper

  private var isOnViewCreatedCalled = false

  private val searchViewBackPressedHandler = object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
      viewModel.showSearch.value = false
    }
  }

  private val deleteSuggestionDialogLauncher = newAlertDialogLauncher("delete_suggestion") a@{
    val context = context ?: return@a

    val suggestionToDelete = it.extras?.getString(ARG_SUGGESTION_TO_DELETE) ?: return@a
    val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as? SearchManager
    val searchableInfo: SearchableInfo? = searchManager?.getSearchableInfo(
      requireActivity().componentName,
    )
    val searchable = searchableInfo ?: return@a
    val authority = searchable.suggestAuthority ?: return@a

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
      "query = ?",
      arrayOf(suggestionToDelete),
    )

    searchSuggestionsAdapter?.refreshSuggestions()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    childFragmentManager.setFragmentResultListener(
      CommunityPickerDialogFragment.REQUEST_KEY,
      this,
    ) { key, bundle ->
      val result = bundle.getParcelableCompat<CommunityPickerDialogFragment.Result>(
        CommunityPickerDialogFragment.REQUEST_KEY_RESULT,
      )
      if (result != null) {
        viewModel.nextCommunityFilter.value = SearchTabbedViewModel.CommunityFilter(
          result.communityId,
          result.communityRef as CommunityRef.CommunityRefByName,
        )
      }
    }
    childFragmentManager.setFragmentResultListener(
      PersonPickerDialogFragment.REQUEST_KEY,
      this,
    ) { _, bundle ->
      val result = bundle.getParcelableCompat<PersonPickerDialogFragment.Result>(
        PersonPickerDialogFragment.REQUEST_KEY_RESULT,
      )
      if (result != null) {
        viewModel.nextPersonFilter.value = SearchTabbedViewModel.PersonFilter(
          result.personRef,
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

    setBinding(FragmentSearchBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    requireActivity().onBackPressedDispatcher.apply {
      addCallback(
        viewLifecycleOwner,
        searchViewBackPressedHandler,
      )
    }

    val context = requireContext()

    val personFilter = args.personFilter
    val communityFilter = args.communityFilter

    if (!isOnViewCreatedCalled && savedInstanceState == null) {
      viewModel.setSortType(args.sortType)
    }
    viewModel.setCurrentPersonFilter(personFilter)
    viewModel.setCurrentCommunityFilter(communityFilter)
    viewModel.nextCommunityFilter.value = communityFilter
    viewModel.nextPersonFilter.value = personFilter
    if (args.query.isNotBlank()) {
      binding.searchEditText.setText(args.query)
      performSearch()
    }

    requireSummitActivity().apply {
      setupForFragment<FilteredPostsAndCommentsTabbedFragment>()

      insetViewAutomaticallyByPaddingAndNavUi(
        viewLifecycleOwner,
        binding.coordinatorLayoutContainer,
      )
    }

    val apiInfo = siteBackendHelper.getApiInfoFromCache(viewModel.instance)

    with(binding) {
      val allTabs = listOfNotNull(
        if (apiInfo?.supportsFeature(ApiFeature.SearchAll) != false) {
          SearchType.All
        } else {
          null
        },
        SearchType.Posts,
        if (apiInfo?.supportsFeature(ApiFeature.SearchComments) != false) {
          SearchType.Comments
        } else {
          null
        },
        SearchType.Communities,
        SearchType.Users,
        SearchType.Url,
      )

      if (personFilter == null) {
        readonlyFilterByCreator.visibility = View.GONE
      } else {
        readonlyFilterByCreator.text = personFilter.personRef.fullName
      }
      if (communityFilter == null) {
        readonlyFilterByCommunity.visibility = View.GONE
      } else {
        readonlyFilterByCommunity.text = communityFilter.communityRef.fullName
      }
      if (!readonlyFilterByCommunity.isVisible && !readonlyFilterByCreator.isVisible) {
        currentFiltersContainer.visibility = View.GONE
      }

      readonlyFilterByCreator.setOnClickListener {
        viewModel.showSearch.value = true
        filterByCreator.performClick()
      }
      readonlyFilterByCommunity.setOnClickListener {
        viewModel.showSearch.value = true
        filterByCommunity.performClick()
      }

      if (viewPager.adapter == null) {
        viewPager.offscreenPageLimit = 5
        val adapter =
          ViewPagerAdapter(context, childFragmentManager, viewLifecycleOwner.lifecycle)

        allTabs.forEach {
          adapter.addFrag(
            SearchResultsFragment::class.java,
            it.toLocalizedString(context),
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

      slidingPaneController = SlidingPaneController(
        fragment = this@SearchTabbedFragment,
        slidingPaneLayout = binding.slidingPaneLayout,
        childFragmentManager = childFragmentManager,
        viewModel = viewModel,
        globalLayoutMode = preferences.globalLayoutMode,
        retainClosedPosts = preferences.retainLastPost,
        emptyScreenText = getString(R.string.select_a_post),
        fragmentContainerId = R.id.post_fragment_container,
      ).apply {
        onPageSelectedListener = { isOpen ->
          if (!isOpen) {
            val lastSelectedPost = viewModel.lastSelectedItem
            if (lastSelectedPost != null) {
              // We came from a post...
//                        adapter?.highlightPost(lastSelectedPost)
              viewModel.lastSelectedItem = null
            }
          } else {
            val lastSelectedPost = viewModel.lastSelectedItem
            if (lastSelectedPost != null) {
//                        adapter?.highlightPostForever(lastSelectedPost)
            }
          }
        }
        init()
      }

      installOnActionResultHandler(
        context = context,
        moreActionsHelper = moreActionsHelper,
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

      searchSuggestionsRecyclerView.setup(animationsHelper)
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
            deleteSuggestionDialogLauncher.launchDialog {
              titleResId = R.string.delete_suggest
              positionButtonResId = android.R.string.ok
              negativeButtonResId = android.R.string.cancel
              extras.putString(ARG_SUGGESTION_TO_DELETE, query)
            }
          }
        },
      )
      searchSuggestionsAdapter.copyTextToSearchViewClickedListener = {
        searchEditText.setText(it)
      }

      searchSuggestionsAdapter.setQuery("")

      searchEditText.addTextChangedListener {
        searchSuggestionsAdapter.setQuery(it?.toString() ?: "")
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
        viewModel.showSearch.value = true
      }

      viewModel.showSearch.observe(viewLifecycleOwner) {
        if (it) {
          showSearch()
        } else {
          hideSearch()
        }
      }
      viewModel.currentQueryLiveData.observe(viewLifecycleOwner) {
        searchEditTextDummy.setText(it)
      }
      viewModel.nextPersonFilter.observe(viewLifecycleOwner) {
        binding.filterByCreator.let { chip ->
          chip.isCloseIconVisible = it != null
          if (it == null) {
            chip.text = getString(R.string.filter_by_creator)
          } else {
            chip.text = it.personRef.fullName
          }
        }
      }
      viewModel.nextCommunityFilter.observe(viewLifecycleOwner) {
        binding.filterByCommunity.let { chip ->
          chip.isCloseIconVisible = it != null
          if (it == null) {
            chip.text = getString(R.string.filter_by_community)
          } else {
            chip.text = it.communityRef.fullName
          }
        }
      }

      if (viewModel.currentQueryFlow.value.isNotBlank()) {
        hideSearch(animate = false)
        viewModel.showSearch.value = false
      }

      fab.setOnClickListener {
        showMoreOptions()
      }
      binding.fab.setup(preferences)
      binding.filterByCommunity.let {
        it.setOnClickListener {
          CommunityPickerDialogFragment.show(childFragmentManager)
        }
        it.setOnCloseIconClickListener {
          viewModel.nextCommunityFilter.value = null
        }
      }
      binding.filterByCreator.let {
        it.setOnClickListener {
          PersonPickerDialogFragment.show(childFragmentManager)
        }
        it.setOnCloseIconClickListener {
          viewModel.nextPersonFilter.value = null
        }
      }
    }

    isOnViewCreatedCalled = true
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
        getString(R.string.sort_by),
        R.drawable.baseline_sort_24,
      )

      addDivider()

      addItemWithIcon(
        R.id.clear_search_history,
        getString(R.string.clear_search_history),
        R.drawable.baseline_delete_24,
      )

      setOnMenuItemClickListener { actionItem ->
        when (actionItem.id) {
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
    Utils.hideKeyboard(requireSummitActivity())

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
      viewModel.nextPersonFilter.value,
      viewModel.nextCommunityFilter.value,
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

  private fun resetQuery() {
    viewModel.updateCurrentQuery("")
    binding.searchEditText.setText("")
    binding.searchEditTextDummy.setText("")
  }

  fun closePost(postFragment: Fragment) {
    slidingPaneController?.closePost(postFragment)
  }
}
