package com.idunnololz.summit.lemmy.search

import android.app.SearchManager
import android.app.SearchableInfo
import android.content.ContentResolver
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.window.layout.WindowMetricsCalculator
import coil.decode.SvgDecoder
import coil.load
import com.github.rubensousa.gravitySnapHelper.GravitySnapHelper
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.account.info.instance
import com.idunnololz.summit.account.loadProfileImageOrDefault
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.summit.TrendingCommunityData
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentSearchHomeBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.ItemSearchHomeCommunityBinding
import com.idunnololz.summit.databinding.ItemSearchHomeEmptyNoSectionsSelectedItemBinding
import com.idunnololz.summit.databinding.ItemSearchHomeMyCommunityBinding
import com.idunnololz.summit.databinding.ItemSearchHomeSearchSuggestionBinding
import com.idunnololz.summit.databinding.ItemSearchHomeTitleBinding
import com.idunnololz.summit.databinding.ItemSearchHomeTrendingCommunitiesBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyHeaderHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.communityPicker.CommunityPickerDialogFragment
import com.idunnololz.summit.lemmy.personPicker.PersonPickerDialogFragment
import com.idunnololz.summit.lemmy.search.SearchHomeFragment.SearchHomeAdapter.Item
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preferences.SearchHomeConfig
import com.idunnololz.summit.preferences.anySectionsEnabled
import com.idunnololz.summit.saved.FilteredPostsAndCommentsTabbedFragment
import com.idunnololz.summit.search.CustomSearchSuggestionsAdapter
import com.idunnololz.summit.settings.util.HorizontalSpaceItemDecoration
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.GridAutofitLayoutManager
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.clearExcludeRegionFromSystemGestures
import com.idunnololz.summit.util.excludeRegionFromSystemGestures
import com.idunnololz.summit.util.ext.focusAndShowKeyboard
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewStartAndEndByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.shimmer.newShimmerDrawable16to9
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SearchHomeFragment :
    BaseFragment<FragmentSearchHomeBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private const val TAG = "SearchHomeFragment"

        private const val ARG_SUGGESTION_TO_DELETE = "ARG_SUGGESTION_TO_DELETE"
    }

    private val viewModel: SearchHomeViewModel by viewModels()

    private var searchSuggestionsAdapter: CustomSearchSuggestionsAdapter? = null

    private val searchViewBackPressedHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.showSearch.value = false
            resetQuery()
        }
    }

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    @Inject
    lateinit var avatarHelper: AvatarHelper

    @Inject
    lateinit var preferences: Preferences

    private var adapter: SearchHomeAdapter? = null

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
                    result.personId,
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

        setBinding(FragmentSearchHomeBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireActivity().onBackPressedDispatcher.apply {
            addCallback(
                viewLifecycleOwner,
                searchViewBackPressedHandler,
            )
        }

        requireMainActivity().apply {
            insetViewStartAndEndByPadding(viewLifecycleOwner, binding.contentContainer)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            insetViewAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.searchContainer)
        }

        with(binding) {
            childFragmentManager.setFragmentResultListener(
                SearchHomeConfigDialogFragment.REQUEST_KEY,
                viewLifecycleOwner,
            ) { _, _ ->
                (recyclerView.adapter as? SearchHomeAdapter)?.searchHomeConfig =
                    preferences.searchHomeConfig
            }

            viewModel.currentAccountView.observe(viewLifecycleOwner) {
                it.loadProfileImageOrDefault(binding.accountImageView)
            }
            accountImageView.setOnClickListener {
                AccountsAndSettingsDialogFragment.newInstance()
                    .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
            }

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

            this@SearchHomeFragment.searchSuggestionsAdapter = searchSuggestionsAdapter

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
                        OldAlertDialogFragment.Builder()
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
                        if (event.action == KeyEvent.ACTION_DOWN &&
                            keyCode == KeyEvent.KEYCODE_ENTER
                        ) {
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
            filterByCommunity.let {
                it.setOnClickListener {
                    CommunityPickerDialogFragment.show(childFragmentManager)
                }
                it.setOnCloseIconClickListener {
                    viewModel.nextCommunityFilter.value = null
                }
            }
            filterByCreator.let {
                it.setOnClickListener {
                    PersonPickerDialogFragment.show(childFragmentManager)
                }
                it.setOnCloseIconClickListener {
                    viewModel.nextPersonFilter.value = null
                }
            }

            val adapter = adapter ?: SearchHomeAdapter(
                context = context,
                avatarHelper = avatarHelper,
                searchHomeConfig = preferences.searchHomeConfig,
                onSuggestionClick = { view, query ->
                    launchSearch(query)
                },
                onRemoveSuggestionClick = { view, query ->
                    viewModel.deleteSuggestion(
                        componentName = requireActivity().componentName,
                        suggestion = query,
                    )

                    if (preferences.hapticsOnActions) {
                        view.performHapticFeedbackCompat(
                            HapticFeedbackConstantsCompat.CONFIRM,
                        )
                    }
                },
                onCommunityClick = { view, community ->
                    requireMainActivity().launchPage(
                        page = community,
                        preferMainFragment = true,
                    )
                },
                onCommunityLongClick = { view, community ->
                    val url = community.toUrl(viewModel.apiInstance)
                    getMainActivity()?.showMoreLinkOptions(url, null)
                },
                onSettingsClick = { view ->
                    SearchHomeConfigDialogFragment.show(
                        childFragmentManager,
                    )
                },
            ).apply {
                stateRestorationPolicy = Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                restoreState(savedInstanceState)
            }.also {
                adapter = it
            }

            val layoutManager = GridAutofitLayoutManager(
                context,
                context.resources.getDimensionPixelSize(R.dimen.search_home_items_width),
            ).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter.getItem(position)) {
                            is Item.HeaderItem -> spanCount
                            is Item.TitleItem -> spanCount
                            is Item.SearchSuggestionItem -> 1
                            is Item.MyCommunityItem -> 1
                            is Item.TopCommunitiesItem -> spanCount
                            is Item.HotCommunitiesItem -> spanCount
                            is Item.TrendingCommunitiesItem -> spanCount
                            Item.EmptyNoSectionsEnabledItem -> spanCount
                        }
                    }
                }
            }
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(
                GridStartEndDecoration(
                    adapter,
                    Utils.convertDpToPixel(12f).toInt(),
                    layoutManager,
                ),
            )

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.generateModel(requireActivity().componentName, force = true)
            }

            viewModel.generateModel(requireActivity().componentName)

            viewModel.model.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.hideAll()
                    }
                    is StatefulData.Success -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.hideAll()

                        adapter.setData(it.data)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setupForFragment<FilteredPostsAndCommentsTabbedFragment>()
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

    private fun resetQuery() {
        viewModel.updateCurrentQuery("")
        binding.searchEditText.setText("")
        binding.searchEditTextDummy.setText("")
    }

    private fun launchSearch() {
        if (!isBindingAvailable()) return

        val query = binding.searchEditText.text
        val queryString = query?.toString() ?: ""

        launchSearch(queryString)
    }

    private fun launchSearch(query: String) {
        val directions = SearchHomeFragmentDirections.actionSearchHomeFragmentToSearchFragment(
            query,
            viewModel.currentSortTypeFlow.value,
            viewModel.nextPersonFilter.value,
            viewModel.nextCommunityFilter.value,
        )
        findNavController().navigateSafe(directions)
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        val context = context ?: return

        val suggestionToDelete = dialog.getExtra(ARG_SUGGESTION_TO_DELETE)
        if (suggestionToDelete != null) {
            val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as? SearchManager
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
                "query = ?",
                arrayOf(suggestionToDelete),
            )

            searchSuggestionsAdapter?.refreshSuggestions()
        }
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter?.saveState(outState)
    }

    private class SearchHomeAdapter(
        private val context: Context,
        private val avatarHelper: AvatarHelper,
        searchHomeConfig: SearchHomeConfig,
        private val onSuggestionClick: (View, String) -> Unit,
        private val onRemoveSuggestionClick: (View, String) -> Unit,
        private val onCommunityClick: (View, CommunityRef) -> Unit,
        private val onCommunityLongClick: (View, CommunityRef) -> Unit,
        private val onSettingsClick: (View) -> Unit,
    ) : Adapter<RecyclerView.ViewHolder>() {

        sealed interface Item {
            data object HeaderItem : Item

            data object EmptyNoSectionsEnabledItem : Item

            data class TitleItem(
                val title: String,
                val showSettingsButton: Boolean,
            ) : Item

            data class SearchSuggestionItem(
                val suggestion: String,
            ) : Item

            data class MyCommunityItem(
                val communityRef: CommunityRef,
                val community: AccountSubscription,
            ) : Item

            data class TopCommunitiesItem(
                val topCommunities: List<TrendingCommunityData>,
                val isLoading: Boolean,
            ) : Item

            data class TrendingCommunitiesItem(
                val trendingCommunities: List<TrendingCommunityData>,
                val isLoading: Boolean,
            ) : Item

            data class HotCommunitiesItem(
                val hotCommunities: List<TrendingCommunityData>,
                val isLoading: Boolean,
            ) : Item
        }

        class RecyclerViewSectionData(
            val name: String,
            val adapter: SuggestedCommunitiesAdapter,
            var layoutManager: LayoutManager,
        )

        var searchHomeConfig: SearchHomeConfig = searchHomeConfig
            set(value) {
                field = value
                refreshItems()
            }

        private var model: SearchHomeModel? = null

        private val topCommunitiesSectionData = newRecyclerViewSection("top")
        private val trendingCommunitiesSectionData = newRecyclerViewSection("trending")
        private val hotCommunitiesSectionData = newRecyclerViewSection("hot")

        private val sections = listOf(
            topCommunitiesSectionData,
            trendingCommunitiesSectionData,
            hotCommunitiesSectionData,
        )

        private val adapterHelper = AdapterHelper<Item>(
            { oldItem, newItem ->
                oldItem::class == newItem::class && when (oldItem) {
                    is Item.HeaderItem -> true
                    is Item.TitleItem ->
                        oldItem.title == (newItem as Item.TitleItem).title
                    is Item.SearchSuggestionItem ->
                        oldItem.suggestion == (newItem as Item.SearchSuggestionItem).suggestion
                    is Item.MyCommunityItem ->
                        oldItem.community.id == (newItem as Item.MyCommunityItem).community.id
                    is Item.HotCommunitiesItem -> true
                    is Item.TrendingCommunitiesItem -> true
                    is Item.TopCommunitiesItem -> true
                    Item.EmptyNoSectionsEnabledItem -> true
                }
            },
        ).apply {
            addItemType(Item.HeaderItem::class, ItemGenericHeaderBinding::inflate) { _, _, _ -> }
            addItemType(
                clazz = Item.EmptyNoSectionsEnabledItem::class,
                inflateFn = ItemSearchHomeEmptyNoSectionsSelectedItemBinding::inflate,
            ) { item, b, _ ->
                b.loadingView.showErrorWithButton(
                    context.getString(R.string.error_search_home_no_sections_selected),
                    context.getString(R.string.search_screen_settings),
                ) {
                    onSettingsClick(it)
                }
            }
            addItemType(
                clazz = Item.TitleItem::class,
                inflateFn = ItemSearchHomeTitleBinding::inflate,
            ) { item, b, _ ->
                b.title.text = item.title
                if (item.showSettingsButton) {
                    b.settings.excludeRegionFromSystemGestures()
                    b.settings.visibility = View.VISIBLE
                    b.settings.setOnClickListener {
                        onSettingsClick(it)
                    }
                } else {
                    b.settings.clearExcludeRegionFromSystemGestures()
                    b.settings.visibility = View.INVISIBLE
                }
            }
            addItemType(
                clazz = Item.SearchSuggestionItem::class,
                inflateFn = ItemSearchHomeSearchSuggestionBinding::inflate,
            ) { item, b, _ ->
                b.text.text = item.suggestion
                b.cardView.setOnClickListener {
                    onSuggestionClick(it, item.suggestion)
                }
                b.remove.setOnClickListener {
                    onRemoveSuggestionClick(it, item.suggestion)
                }
            }
            addItemType(
                clazz = Item.MyCommunityItem::class,
                inflateFn = ItemSearchHomeMyCommunityBinding::inflate,
            ) { item, b, _ ->
                avatarHelper.loadCommunityIcon(
                    b.icon,
                    item.community.toCommunityRef(),
                    item.community.icon,
                )

                b.title.text = item.community.name
                b.subtitle.text = item.community.instance

                b.cardView.setOnClickListener {
                    onCommunityClick(it, item.communityRef)
                }
                b.cardView.setOnLongClickListener {
                    onCommunityLongClick(it, item.communityRef)
                    true
                }
            }
            addItemType(
                clazz = Item.TopCommunitiesItem::class,
                inflateFn = ItemSearchHomeTrendingCommunitiesBinding::inflate,
                onViewCreated = { b ->
                    val sectionData = topCommunitiesSectionData
                    b.recyclerView.layoutManager = LinearLayoutManager(
                        context, LinearLayoutManager.HORIZONTAL, false,
                    ).apply {
                        onRestoreInstanceState(sectionData.layoutManager.onSaveInstanceState())
                    }.also {
                        sectionData.layoutManager = it
                    }
                    b.recyclerView.adapter = sectionData.adapter
                    b.recyclerView.addItemDecoration(
                        HorizontalSpaceItemDecoration(
                            context.resources.getDimensionPixelSize(R.dimen.padding_half),
                            context.resources.getDimensionPixelSize(R.dimen.padding),
                        ),
                    )

                    GravitySnapHelper(Gravity.START)
                        .apply {
                            this.snapPadding = -Utils.convertDpToPixel(16f).toInt()
                        }
                        .attachToRecyclerView(b.recyclerView)
                },
            ) { item, b, _ ->
                if (item.isLoading) {
                    b.progressBar.visibility = View.VISIBLE
                    b.recyclerView.visibility = View.GONE
                } else {
                    b.progressBar.visibility = View.GONE
                    b.recyclerView.visibility = View.VISIBLE
                }
            }
            addItemType(
                clazz = Item.TrendingCommunitiesItem::class,
                inflateFn = ItemSearchHomeTrendingCommunitiesBinding::inflate,
                onViewCreated = { b ->
                    val sectionData = trendingCommunitiesSectionData
                    b.recyclerView.layoutManager = LinearLayoutManager(
                        context, LinearLayoutManager.HORIZONTAL, false,
                    ).apply {
                        onRestoreInstanceState(sectionData.layoutManager.onSaveInstanceState())
                    }.also {
                        sectionData.layoutManager = it
                    }
                    b.recyclerView.adapter = sectionData.adapter
                    b.recyclerView.addItemDecoration(
                        HorizontalSpaceItemDecoration(
                            context.resources.getDimensionPixelSize(R.dimen.padding_half),
                            context.resources.getDimensionPixelSize(R.dimen.padding),
                        ),
                    )

                    GravitySnapHelper(Gravity.START)
                        .apply {
                            this.snapPadding = -Utils.convertDpToPixel(16f).toInt()
                        }
                        .attachToRecyclerView(b.recyclerView)
                },
            ) { item, b, _ ->
                if (item.isLoading) {
                    b.progressBar.visibility = View.VISIBLE
                    b.recyclerView.visibility = View.GONE
                } else {
                    b.progressBar.visibility = View.GONE
                    b.recyclerView.visibility = View.VISIBLE
                }
            }
            addItemType(
                clazz = Item.HotCommunitiesItem::class,
                inflateFn = ItemSearchHomeTrendingCommunitiesBinding::inflate,
                onViewCreated = { b ->
                    val sectionData = hotCommunitiesSectionData
                    b.recyclerView.layoutManager = LinearLayoutManager(
                        context, LinearLayoutManager.HORIZONTAL, false,
                    ).apply {
                        onRestoreInstanceState(sectionData.layoutManager.onSaveInstanceState())
                    }.also {
                        sectionData.layoutManager = it
                    }
                    b.recyclerView.adapter = sectionData.adapter
                    b.recyclerView.addItemDecoration(
                        HorizontalSpaceItemDecoration(
                            context.resources.getDimensionPixelSize(R.dimen.padding_half),
                            context.resources.getDimensionPixelSize(R.dimen.padding),
                        ),
                    )

                    GravitySnapHelper(Gravity.START)
                        .apply {
                            this.snapPadding = -Utils.convertDpToPixel(16f).toInt()
                        }
                        .attachToRecyclerView(b.recyclerView)
                },
            ) { item, b, _ ->
                if (item.isLoading) {
                    b.progressBar.visibility = View.VISIBLE
                    b.recyclerView.visibility = View.GONE
                } else {
                    b.progressBar.visibility = View.GONE
                    b.recyclerView.visibility = View.VISIBLE
                }
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder = adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount

        fun getItem(i: Int) = adapterHelper.items[i]

        fun setData(model: SearchHomeModel) {
            this.model = model
            refreshItems()
        }

        private fun refreshItems() {
            val model = model ?: return
            val newItems = mutableListOf<Item>()
            val communitySuggestions = model.communitySuggestionsDto
            var sectionIndex = 0

            newItems.add(Item.HeaderItem)

            if (model.suggestions.isNotEmpty() && searchHomeConfig.showSearchSuggestions) {
                newItems.add(
                    Item.TitleItem(
                        title = context.getString(R.string.recent_searches),
                        showSettingsButton = sectionIndex == 0,
                    ),
                )
                model.suggestions.mapTo(newItems) {
                    Item.SearchSuggestionItem(it)
                }
                sectionIndex++
            }

            if (model.myCommunities.isNotEmpty() && searchHomeConfig.showSubscribedCommunities) {
                newItems.add(
                    Item.TitleItem(
                        title = context.getString(R.string.your_communities),
                        showSettingsButton = sectionIndex == 0,
                    ),
                )
                model.myCommunities.mapTo(newItems) {
                    Item.MyCommunityItem(it.communityRef, it.sub)
                }
                sectionIndex++
            }

            if (communitySuggestions != null) {
                if (communitySuggestions.popularLast7Days.isNotEmpty() &&
                    searchHomeConfig.showTopCommunity7DaysSuggestions
                ) {
                    newItems.add(
                        Item.TitleItem(
                            title = context.getString(R.string.top_communities_this_week),
                            showSettingsButton = sectionIndex == 0,
                        ),
                    )
                    newItems.add(
                        Item.TopCommunitiesItem(
                            communitySuggestions.popularLast7Days,
                            isLoading = false,
                        ),
                    )
                    topCommunitiesSectionData.adapter
                        .setData(communitySuggestions.popularLast7Days)
                    sectionIndex++
                }

                if (communitySuggestions.trendingLast7Days.isNotEmpty() &&
                    searchHomeConfig.showTrendingCommunitySuggestions
                ) {
                    newItems.add(
                        Item.TitleItem(
                            title = context.getString(R.string.trending_communities),
                            showSettingsButton = sectionIndex == 0,
                        ),
                    )
                    newItems.add(
                        Item.TrendingCommunitiesItem(
                            communitySuggestions.trendingLast7Days,
                            isLoading = false,
                        ),
                    )
                    trendingCommunitiesSectionData.adapter
                        .setData(communitySuggestions.trendingLast7Days)
                    sectionIndex++
                }

                if (communitySuggestions.hot.isNotEmpty() &&
                    searchHomeConfig.showRisingCommunitySuggestions
                ) {
                    newItems.add(
                        Item.TitleItem(
                            title = context.getString(R.string.rising_communities),
                            showSettingsButton = sectionIndex == 0,
                        ),
                    )
                    newItems.add(
                        Item.HotCommunitiesItem(
                            communitySuggestions.hot,
                            isLoading = false,
                        ),
                    )
                    hotCommunitiesSectionData.adapter
                        .setData(communitySuggestions.hot)
                    sectionIndex++
                }
            } else if (model.isCommunitySuggestionsLoading) {
                if (searchHomeConfig.showTopCommunity7DaysSuggestions) {
                    newItems.add(
                        Item.TitleItem(
                            title = context.getString(R.string.top_communities_this_week),
                            showSettingsButton = sectionIndex == 0,
                        ),
                    )
                    newItems.add(
                        Item.TopCommunitiesItem(
                            listOf(),
                            isLoading = true,
                        ),
                    )
                    sectionIndex++
                }
                if (searchHomeConfig.showTrendingCommunitySuggestions) {
                    newItems.add(
                        Item.TitleItem(
                            title = context.getString(R.string.trending_communities),
                            showSettingsButton = sectionIndex == 0,
                        ),
                    )
                    newItems.add(
                        Item.TrendingCommunitiesItem(
                            listOf(),
                            isLoading = true,
                        ),
                    )
                    sectionIndex++
                }
                if (searchHomeConfig.showRisingCommunitySuggestions) {
                    newItems.add(
                        Item.TitleItem(
                            title = context.getString(R.string.rising_communities),
                            showSettingsButton = sectionIndex == 0,
                        ),
                    )
                    newItems.add(
                        Item.HotCommunitiesItem(
                            listOf(),
                            isLoading = true,
                        ),
                    )
                    sectionIndex++
                }
            }

            if (!searchHomeConfig.anySectionsEnabled) {
                newItems.add(Item.EmptyNoSectionsEnabledItem)
            }

            adapterHelper.setItems(newItems, this) {}
        }

        fun saveState(bundle: Bundle) {
            sections.forEach {
                bundle.putParcelable("summit.${it.name}", it.layoutManager.onSaveInstanceState())
            }
        }

        fun restoreState(bundle: Bundle?) {
            bundle ?: return

            sections.forEach { section ->
                bundle.getParcelableCompat<Parcelable>("summit.${section.name}")?.let {
                    section.layoutManager.onRestoreInstanceState(it)
                }
            }
        }

        private fun newRecyclerViewSection(name: String): RecyclerViewSectionData {
            return RecyclerViewSectionData(
                name = name,
                SuggestedCommunitiesAdapter(
                    context = context,
                    avatarHelper = avatarHelper,
                    onCommunityClick = onCommunityClick,
                    onCommunityLongClick = onCommunityLongClick,
                ).apply {
                    stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
                },
                LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false,
                ),
            )
        }
    }

    private class SuggestedCommunitiesAdapter(
        private val context: Context,
        private val avatarHelper: AvatarHelper,
        private val onCommunityClick: (View, CommunityRef) -> Unit,
        private val onCommunityLongClick: (View, CommunityRef) -> Unit,
    ) : Adapter<RecyclerView.ViewHolder>() {

        private sealed interface Item {
            data class SuggestedCommunityItem(
                val communityRef: CommunityRef,
                val community: TrendingCommunityData,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    is Item.SuggestedCommunityItem ->
                        old.community.url == (new as Item.SuggestedCommunityItem).community.url
                }
            },
        ).apply {
            addItemType(
                clazz = Item.SuggestedCommunityItem::class,
                inflateFn = ItemSearchHomeCommunityBinding::inflate,
            ) { item, b, _ ->
                if (item.community.banner != null) {
                    b.image.load(item.community.banner) {
                        placeholder(newShimmerDrawable16to9(context))
                    }
                } else {
                    b.image.load("file:///android_asset/banner_placeholder.svg") {
                        decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }

                        val windowMetrics = WindowMetricsCalculator.getOrCreate()
                            .computeCurrentWindowMetrics(context)
                        size(
                            windowMetrics.bounds.width(),
                            (windowMetrics.bounds.width() * (9 / 16.0)).toInt(),
                        )
                    }
                }

                avatarHelper.loadCommunityIcon(
                    b.icon,
                    item.communityRef,
                    item.community.icon,
                )

                b.title.text = item.community.name
                b.subtitle.text = item.community.baseurl

                val mauString =
                    LemmyUtils.abbrevNumber(item.community.counts.usersActiveMonth.toLong())
                @Suppress("SetTextI18n")
                b.mau.text = context.getString(R.string.mau_format2, mauString)
                b.mau.typeface = LemmyHeaderHelper.condensedTypeface

                val subsString = LemmyUtils.abbrevNumber(item.community.counts.subscribers.toLong())
                @Suppress("SetTextI18n")
                b.subs.text = context.getString(R.string.subs_format, subsString)
                b.subs.typeface = LemmyHeaderHelper.condensedTypeface

                val postsString = LemmyUtils.abbrevNumber(item.community.counts.posts.toLong())
                @Suppress("SetTextI18n")
                b.posts.text = context.getString(R.string.posts_format, postsString)
                b.posts.typeface = LemmyHeaderHelper.condensedTypeface

                b.cardView.setOnClickListener {
                    onCommunityClick(it, item.communityRef)
                }
                b.cardView.setOnLongClickListener {
                    onCommunityLongClick(it, item.communityRef)
                    true
                }
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder = adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount

        fun setData(communities: List<TrendingCommunityData>) {
            val newItems = mutableListOf<Item>()

            communities.mapTo(newItems) {
                Item.SuggestedCommunityItem(
                    CommunityRef.CommunityRefByName(
                        it.name,
                        it.baseurl,
                    ),
                    it,
                )
            }

            adapterHelper.setItems(newItems, this) {}
        }
    }

    class GridStartEndDecoration(
        private val adapter: Adapter<*>,
        private val spacing: Int,
        private val layoutManager: GridLayoutManager,
    ) : ItemDecoration() {

        private val itemFlags = mutableListOf<Int>()

        init {
            adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
                override fun onChanged() {
                    itemFlags.clear()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                    itemFlags.clear()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                    itemFlags.clear()
                }

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    itemFlags.clear()
                }

                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    itemFlags.clear()
                }

                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    itemFlags.clear()
                }
            })
        }

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            generateItemFlags()

            val position = parent.getChildAdapterPosition(view) // item position

            if (position == -1) {
                return
            }

            val spanData = itemFlags[position]
            val isStart = spanData and 0x10 != 0
            val isEnd = spanData and 0x1 != 0

            if (isStart && isEnd) {
                outRect.left = 0
                outRect.right = 0
                return
            }

            if (isStart) {
                outRect.left = spacing
            }
            if (isEnd) {
                outRect.right = spacing
            }
        }

        private fun generateItemFlags() {
            if (itemFlags.isNotEmpty()) return

            val spanCount = layoutManager.spanCount
            val spanSizeLookup = layoutManager.spanSizeLookup
            var rowSpanCount = 0

            for (i in 0 until adapter.itemCount) {
                var isStart = rowSpanCount == 0
                val isEnd: Boolean

                rowSpanCount += spanSizeLookup.getSpanSize(i)

                if (rowSpanCount >= spanCount) {
                    if (rowSpanCount > spanCount) {
                        isStart = true
                    }

                    rowSpanCount = 0
                    isEnd = true
                } else {
                    isEnd = false
                }

                itemFlags.add(
                    if (isStart) {
                        0x10
                    } else {
                        0x0
                    } or if (isEnd) {
                        0x1
                    } else {
                        0x0
                    },
                )
            }
        }
    }
}
