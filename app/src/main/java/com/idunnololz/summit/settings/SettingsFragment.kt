package com.idunnololz.summit.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSettingsBinding
import com.idunnololz.summit.databinding.SettingSearchResultItemBinding
import com.idunnololz.summit.lemmy.search.SearchHomeConfigDialogFragment
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.util.asCustomItem
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.focusAndShowKeyboard
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.navigateSafe
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
  lateinit var settings: MainSettings

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var allSettings: dagger.Lazy<AllSettings>

  @Inject
  lateinit var globalStateStorage: GlobalStateStorage

  private val viewModel: SettingsViewModel by viewModels()

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

    requireSummitActivity().apply {
      insetViewStartAndEndByPadding(viewLifecycleOwner, binding.recyclerView)
      insetViewExceptBottomAutomaticallyByPadding(viewLifecycleOwner, binding.contentView)
      insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.searchContainer)
    }

    viewModel.searchableSettings = allSettings

    with(binding) {
      searchBar.setOnClickListener {
        viewModel.showSearch.value = true
      }
      toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
      toolbar.setNavigationIconTint(
        context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
      )
      toolbar.setNavigationOnClickListener {
        findNavController().navigateUp()
      }

      updateRendering()

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

  private fun Bundle.addSettingReference(setting: SettingItem): Bundle {
    this.putString(BaseSettingsFragment.ARG_SETTING_NAME, setting.title)
    return this
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
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is GestureSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingGesturesFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is LemmyWebSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingWebFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is MainSettings -> {
              hideSearch()

              binding.recyclerView.post {
                highlightSetting(it)
              }
            }
            is PostsFeedSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingsContentFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is AboutSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingAboutFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is CacheSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingCacheFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is HiddenPostsSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingHiddenPostsFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is PostAndCommentsAppearanceSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingPostAndCommentsFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is ThemeSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingThemeFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is PostsFeedAppearanceSettings -> {
              val directions = SettingsFragmentDirections
                .actionGlobalSettingViewTypeFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is MiscSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingMiscFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is LoggingSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingLoggingFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }
            is HistorySettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingHistoryFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is NavigationSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingNavigationFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is ActionsSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToActions()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is ImportAndExportSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingBackupAndRestoreFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is PerAccountSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingAccountsFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is DownloadSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingDownloadsFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is PerCommunitySettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingPerCommunityFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is NotificationSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingNotificationsFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is HapticSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingsHapticsFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is SearchHomeSettings -> {
              SearchHomeConfigDialogFragment.show(childFragmentManager)
            }

            is VideoPlayerSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingsVideoPlayerFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
            }

            is DefaultAppsSettings -> {
              val directions = SettingsFragmentDirections
                .actionSettingsFragmentToSettingsDefaultAppsFragment()
              findNavController().navigate(directions.actionId, Bundle().addSettingReference(it))
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

  private fun highlightSetting(it: SettingItem) {
    if (!isBindingAvailable()) {
      return
    }

    binding.appBar.setExpanded(false)
    val adapter = binding.recyclerView.adapter as? SettingsAdapter ?: return
    val index = adapter.findIndex(it.title)
    if (index >= 0) {
      (binding.recyclerView.layoutManager as LinearLayoutManager)
        .scrollToPositionWithOffset(index, Utils.convertDpToPixel(48f).toInt())
      adapter.highlight(it.title)
    }
  }

  private fun handleLinkIfNeeded() {
    val link = args.link
    if (link != null && !handledLink) {
      handledLink = true

      arguments = args.copy(link = null).toBundle()

      when (link) {
        "web" -> {
          launchWebSettings(popSettingsFragment = true)
        }
        "downloads" -> {
          launchDownloadsSettings()
        }
      }
    }
  }

  private fun launchWebSettings(popSettingsFragment: Boolean) {
    val directions = SettingsFragmentDirections
      .actionSettingsFragmentToSettingWebFragment()
    findNavController().navigateSafe(
      directions,
      NavOptions.Builder()
        .setEnterAnim(androidx.navigation.ui.R.animator.nav_default_enter_anim)
        .setExitAnim(androidx.navigation.ui.R.animator.nav_default_exit_anim)
        .setPopEnterAnim(
          androidx.navigation.ui.R.animator.nav_default_pop_enter_anim,
        )
        .setPopExitAnim(androidx.navigation.ui.R.animator.nav_default_pop_exit_anim)
        .apply {
          if (popSettingsFragment) {
            setPopUpTo(R.id.settingsFragment, true)
          }
        }
        .build(),
    )
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
    Utils.hideKeyboard(requireSummitActivity())

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

  private fun updateRendering() {
    val context = requireContext()

    with(binding) {
      val adapter = SettingsAdapter(
        context = context,
        globalStateStorage = globalStateStorage,
        useFooter = true,
        getSummitActivity = { requireSummitActivity() },
        onValueChanged = { refresh() },
        onLinkClick = { url, text, linkContext ->
          onLinkClick(url, text, linkContext)
        },
      )
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(true)
      recyclerView.adapter = adapter

      val data = generateData()

      adapter.setData(data)
    }
  }

  fun refresh() {
    if (!isBindingAvailable()) return

    (binding.recyclerView.adapter as? SettingsAdapter)?.setData(generateData())
  }

  private fun generateData(): List<SettingModelItem> {
    val context = requireContext()

    return listOf(
      SettingModelItem.SubgroupItem(
        context.getString(R.string.look_and_feel),
        listOf(
          settings.settingTheme.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingThemeFragment()
            findNavController().navigateSafe(directions)
          },
          settings.settingPostsFeed.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingsContentFragment()
            findNavController().navigateSafe(directions)
          },
          settings.settingPostAndComments.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingPostAndCommentsFragment()
            findNavController().navigateSafe(directions)
          },
          settings.settingGestures.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingGesturesFragment()
            findNavController().navigateSafe(directions)
          },
          settings.hapticSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingsHapticsFragment()
            findNavController().navigateSafe(directions)
          },
          settings.videoPlayerSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingsVideoPlayerFragment()
            findNavController().navigateSafe(directions)
          },
          settings.miscSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingMiscFragment()
            findNavController().navigateSafe(directions)
          },
        ),
      ),
      SettingModelItem.SubgroupItem(
        context.getString(R.string.account_settings),
        listOf(
          settings.settingLemmyWeb.asCustomItem {
            launchWebSettings(popSettingsFragment = false)
          },
          settings.settingAccount.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingAccountsFragment()
            findNavController().navigateSafe(directions)
          },
        ),
      ),
      SettingModelItem.SubgroupItem(
        context.getString(R.string.systems),
        listOf(
          settings.settingCache.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingCacheFragment()
            findNavController().navigateSafe(directions)
          },
          settings.settingHiddenPosts.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingHiddenPostsFragment()
            findNavController().navigateSafe(directions)
          },
          settings.loggingSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingLoggingFragment()
            findNavController().navigateSafe(directions)
          },
          settings.historySettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingHistoryFragment()
            findNavController().navigateSafe(directions)
          },
          settings.navigationSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingNavigationFragment()
            findNavController().navigateSafe(directions)
          },
          settings.userActionsSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToActions()
            findNavController().navigateSafe(directions)
          },
          settings.downloadSettings.asCustomItem {
            launchDownloadsSettings()
          },
          settings.notificationSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingNotificationsFragment()
            findNavController().navigateSafe(directions)
          },
          settings.defaultAppsSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingsDefaultAppsFragment()
            findNavController().navigateSafe(directions)
          },
          settings.backupAndRestoreSettings.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingBackupAndRestoreFragment()
            findNavController().navigateSafe(directions)
          },
        ),
      ),
//        SubgroupItem(
//            context.getString(R.string.experimental),
//            listOf(settingPresets),
//      val directions = SettingsFragmentDirections
//      .actionSettingsFragmentToPresetsFragment()
//    findNavController().navigateSafe(directions)
//        ),
      SettingModelItem.SubgroupItem(
        context.getString(R.string.about),
        listOf(
          settings.settingAbout.asCustomItem {
            val directions = SettingsFragmentDirections
              .actionSettingsFragmentToSettingAboutFragment()
            findNavController().navigateSafe(directions)
          },
          settings.settingSummitCommunity.asCustomItem {
            getMainActivity()?.launchPage(summitCommunityPage)
          },
        ),
      ),
    )
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
