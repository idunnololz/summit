package com.idunnololz.summit.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.databinding.FragmentSettingsBinding
import com.idunnololz.summit.databinding.SettingSearchResultItemBinding
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.focusAndShowKeyboard
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByPadding
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.insetViewStartAndEndByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.summitCommunityPage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    private val args by navArgs<SettingsFragmentArgs>()

    @Inject
    lateinit var mainSettings: MainSettings

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    private val viewModel: SettingsViewModel by viewModels()

    private var adapter: SettingItemsAdapter? = null

    private var handledLink = false

    private val searchViewBackPressedHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.showSearch.value = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            searchViewBackPressedHandler,
        )

        val context = requireContext()

        requireMainActivity().apply {
            insetViewStartAndEndByPadding(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByPadding(viewLifecycleOwner, binding.contentView)
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.searchContainer)

            binding.searchBar.setOnClickListener {
                viewModel.showSearch.value = true
            }
        }

        with(binding) {
            recyclerView.setup(animationsHelper)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)

            if (adapter == null) {
                adapter = SettingItemsAdapter(
                    context = context,
                    onSettingClick = {
                        when (it.id) {
                            mainSettings.settingTheme.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingThemeFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingPostAndComments.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingPostAndCommentsFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingLemmyWeb.id -> {
                                launchWebSettings()
                                true
                            }
                            mainSettings.settingGestures.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingGesturesFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingCache.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingCacheFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingPostsFeed.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingsContentFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.commentListSettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingCommentListFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingHiddenPosts.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingHiddenPostsFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingAbout.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingAboutFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingSummitCommunity.id -> {
                                getMainActivity()?.launchPage(summitCommunityPage)
                                true
                            }
                            mainSettings.miscSettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingMiscFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.loggingSettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingLoggingFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.historySettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingHistoryFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.navigationSettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingNavigationFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.userActionsSettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToActionsTabbedFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.settingAccount.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingAccountsFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.backupAndRestoreSettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingBackupAndRestoreFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            mainSettings.downloadSettings.id -> {
                                launchDownloadsSettings()
                                true
                            }
                            mainSettings.notificationSettings.id -> {
                                val directions = SettingsFragmentDirections
                                    .actionSettingsFragmentToSettingNotificationsFragment()
                                findNavController().navigateSafe(directions)
                                true
                            }
                            else -> false
                        }
                    },
                    childFragmentManager,
                )
            }
            recyclerView.adapter = adapter?.apply {
                this.firstTitleHasTopMargin = false
                this.setData(mainSettings.allSettings)
            }

            viewModel.showSearch.observe(viewLifecycleOwner) {
                if (it) {
                    showSearch()
                } else {
                    hideSearch()
                }
            }
            viewModel.searchResults.observe(viewLifecycleOwner) {
                if (searchResultsRecyclerView.adapter == null) {
                    setupSearchRecyclerView()
                }

                (searchResultsRecyclerView.adapter as? SearchResultAdapter)?.setData(it) {
                    searchResultsRecyclerView.scrollToPosition(0)
                }
            }

            searchEditText.addTextChangedListener {
                viewModel.query(it?.toString())
            }
        }

        handleLinkIfNeeded()
        hideSearch(animate = false)
    }

    override fun onResume() {
        super.onResume()

        setupForFragment<SettingsFragment>()
    }

    private fun setupSearchRecyclerView() {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        binding.searchResultsRecyclerView.apply {
            adapter = SearchResultAdapter(
                context = context,
                onResultClick = {
                    when (viewModel.searchIdToPage[it.id]) {
                        is PostAndCommentsSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingCommentListFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is GestureSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingGesturesFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is LemmyWebSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingWebFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is MainSettings -> {
                            hideSearch()
                        }
                        is PostsFeedSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingsContentFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is AboutSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingAboutFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is CacheSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingCacheFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is HiddenPostsSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingHiddenPostsFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is PostAndCommentsAppearanceSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingPostAndCommentsFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is ThemeSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingThemeFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is PostsFeedAppearanceSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionGlobalSettingViewTypeFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is MiscSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingMiscFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is LoggingSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingLoggingFragment()
                            findNavController().navigateSafe(directions)
                        }
                        is HistorySettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingHistoryFragment()
                            findNavController().navigateSafe(directions)
                        }

                        is NavigationSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingNavigationFragment()
                            findNavController().navigateSafe(directions)
                        }

                        is ActionsSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToActionsTabbedFragment()
                            findNavController().navigateSafe(directions)
                        }

                        is ImportAndExportSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingBackupAndRestoreFragment()
                            findNavController().navigateSafe(directions)
                        }

                        is PerAccountSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingAccountsFragment()
                            findNavController().navigateSafe(directions)
                        }

                        is DownloadSettings -> {
                            launchDownloadsSettings()
                        }

                        is PerCommunitySettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingPerCommunityFragment()
                            findNavController().navigateSafe(directions)
                        }

                        is NotificationSettings -> {
                            val directions = SettingsFragmentDirections
                                .actionSettingsFragmentToSettingNotificationsFragment()
                            findNavController().navigateSafe(directions)
                        }

                        null -> {
                            // do nothing
                        }
                    }
                    true
                },
            )
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun handleLinkIfNeeded() {
        val link = args.link
        if (link != null && !handledLink) {
            handledLink = true

            arguments = args.copy(link = null).toBundle()

            when (link) {
                "web" -> {
                    launchWebSettings()
                }
                "downloads" -> {
                    launchDownloadsSettings()
                }
            }
        }
    }

    private fun launchWebSettings() {
        val directions = SettingsFragmentDirections
            .actionSettingsFragmentToSettingWebFragment()
        findNavController().navigateSafe(directions)
    }

    private fun launchDownloadsSettings() {
        val directions = SettingsFragmentDirections
            .actionSettingsFragmentToSettingDownloadsFragment()
        findNavController().navigateSafe(directions)
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
                    binding.searchContainer.visibility = View.GONE
                    binding.searchContainer.alpha = 1f
                }
        } else {
            binding.searchContainer.visibility = View.GONE
            binding.searchContainer.alpha = 1f
        }

        searchViewBackPressedHandler.isEnabled = false
    }

    private class SearchResultAdapter(
        private val context: Context,
        private val onResultClick: (SettingItem) -> Boolean,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {

            data class SearchResultItem(
                val settingItem: SettingItem,
                val page: SearchableSettings,
            ) : Item
        }

        var data: List<SettingsViewModel.SettingSearchResultItem> = listOf()
            private set

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.SearchResultItem ->
                        old.settingItem.id == (new as Item.SearchResultItem).settingItem.id
                }
            },
        ).apply {
            addItemType(
                clazz = Item.SearchResultItem::class,
                inflateFn = SettingSearchResultItemBinding::inflate,
            ) { item, b, h ->
                val settingItem = item.settingItem

                b.title.text = settingItem.title
                if (settingItem.description == null) {
                    b.desc.text =
                        (item.page.parents + listOf(item.page::class))
                            .joinToString(" > ") {
                                it.getPageName(context)
                            }
                } else {
                    b.desc.text = settingItem.description
                }

                b.root.setOnClickListener {
                    onResultClick(settingItem)
                }
            }
        }

        init {
            refreshItems()
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems(onItemsUpdated: () -> Unit = {}) {
            val newItems = mutableListOf<Item>()

            data.mapTo(newItems) {
                Item.SearchResultItem(
                    it.settingItem,
                    it.page,
                )
            }

            adapterHelper.setItems(newItems, this, onItemsUpdated)
        }

        fun setData(
            data: List<SettingsViewModel.SettingSearchResultItem>,
            onItemsUpdated: () -> Unit = {},
        ) {
            this.data = data

            refreshItems {
                onItemsUpdated()
            }
        }
    }
}
