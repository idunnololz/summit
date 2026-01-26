package com.idunnololz.summit.localTracking.screen

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.CornerTreatment
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.DummyTopItemBinding
import com.idunnololz.summit.databinding.FragmentLocalStatsBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.LocalStatsCommunityStatItemBinding
import com.idunnololz.summit.databinding.LocalStatsNoDataItemBinding
import com.idunnololz.summit.databinding.LocalStatsPersonStatItemBinding
import com.idunnololz.summit.databinding.LocalStatsSectionTitleItemBinding
import com.idunnololz.summit.databinding.LocalStatsSpaceItemBinding
import com.idunnololz.summit.databinding.LocalStatsStatEndItemBinding
import com.idunnololz.summit.databinding.LocalStatsSummaryItemBinding
import com.idunnololz.summit.databinding.LocalStatsWarningItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.toUri
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocalStatsFragment : BaseFragment<FragmentLocalStatsBinding>() {

  private val viewModel: LocalStatsViewModel by viewModels()

  @Inject
  lateinit var avatarHelper: AvatarHelper

  @Inject
  lateinit var preferences: Preferences

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(FragmentLocalStatsBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val context = requireContext()

    with(binding) {
      requireSummitActivity().apply {
        setupForFragment<LocalStatsFragment>()
        insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
        insetViewAutomaticallyByPaddingAndNavUi(
          viewLifecycleOwner,
          root,
          applyTopInset = false,
        )

        setupToolbar(toolbar, getString(R.string.local_account_stats))
      }

      viewModel.loadData(force = false)

      recyclerView.setHasFixedSize(true)
      recyclerView.layoutManager = LinearLayoutManager(context)

      swipeRefreshLayout.setOnRefreshListener {
        viewModel.loadData(force = true)
      }

      val adapter = LocalStatsAdapter(
        context = context,
        avatarHelper = avatarHelper,
        onCommunityClick = {
          requireSummitActivity().launchPage(it)
        },
        onPersonClick = {
          requireSummitActivity().launchPage(it)
        },
        onLocalTrackingSettingsClick = {
          requireSummitActivity().showLocalTrackingEventsSettings()
        },
      )
      recyclerView.adapter = adapter

      viewModel.data.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Error -> {
            loadingView.showDefaultErrorMessageFor(it.error)
            swipeRefreshLayout.isRefreshing = false
          }
          is StatefulData.Loading -> {
            loadingView.showProgressBar()
          }
          is StatefulData.NotStarted -> {
            loadingView.hideAll()
            swipeRefreshLayout.isRefreshing = false
          }
          is StatefulData.Success -> {
            loadingView.hideAll()
            swipeRefreshLayout.isRefreshing = false

            adapter.model = it.data
          }
        }
      }

      toolbar.addMenuProvider(object : MenuProvider {
        override fun onCreateMenu(
          menu: Menu,
          menuInflater: MenuInflater,
        ) {
          menuInflater.inflate(R.menu.menu_local_stats, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
          when (menuItem.itemId) {
            R.id.settings -> {
              requireSummitActivity().showLocalTrackingEventsSettings()
            }
          }
          return true
        }

      })
    }
  }

  override fun onResume() {
    super.onResume()

    (binding.recyclerView.adapter as? LocalStatsAdapter)?.isLocalTrackingEnabled =
      preferences.localTrackingEnabled
  }

  class LocalStatsAdapter(
    private val context: Context,
    private val avatarHelper: AvatarHelper,
    private val onCommunityClick: (CommunityRef) -> Unit,
    private val onPersonClick: (PersonRef) -> Unit,
    private val onLocalTrackingSettingsClick: () -> Unit,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var model: LocalStatsViewModel.Model? = null
      set(value) {
        field = value

        refresh()
      }

    var isLocalTrackingEnabled: Boolean? = null
      set(value) {
        field = value

        refresh()
      }

    sealed interface Item {

      data object HeaderItem : Item

      data class StatSummaryItem(
        val events: Int
      ) : Item

      data class SectionTitleItem(
        val title: String
      ) : Item

      data class CommunityStat(
        val communityRef: CommunityRef?,
        val value: Int
      ) : Item

      data class PersonStat(
        val person: Person?,
        val value: Int
      ) : Item

      data object SpaceItem : Item

      data object StatEndItem : Item

      data object FooterItem : Item

      data object NoDataItem : Item

      data object LocalTrackingDisabledWarningItem : Item
    }

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class && when (old) {
          is Item.CommunityStat ->
            old.communityRef == (new as Item.CommunityStat).communityRef
          is Item.PersonStat ->
            old.person == (new as Item.PersonStat).person
          is Item.SectionTitleItem ->
            old.title == (new as Item.SectionTitleItem).title
          is Item.StatSummaryItem ->
            true
          Item.StatEndItem -> false
          Item.SpaceItem -> false
          Item.FooterItem -> true
          Item.LocalTrackingDisabledWarningItem -> true
          Item.HeaderItem -> true
          Item.NoDataItem -> false
        }
      }
    ).apply {
      addItemType(
        clazz = Item.HeaderItem::class,
        inflateFn = DummyTopItemBinding::inflate,
      ) { _, _, _ -> }
      addItemType(
        clazz = Item.StatSummaryItem::class,
        inflateFn = LocalStatsSummaryItemBinding::inflate
      ) { item, b, h ->
        b.stat.text = PrettyPrintUtils.defaultDecimalFormat.format(item.events)
      }
      addItemType(
        clazz = Item.SectionTitleItem::class,
        inflateFn = LocalStatsSectionTitleItemBinding::inflate,
        onViewCreated = { b ->
          b.card.shapeAppearanceModel = b.card.shapeAppearanceModel.toBuilder().apply {
            setBottomLeftCorner(CornerFamily.CUT, 0f)
            setBottomRightCorner(CornerFamily.CUT, 0f)
          }.build()
        }
      ) { item, b, h ->
        b.title.text = item.title
      }
      addItemType(
        clazz = Item.CommunityStat::class,
        inflateFn = LocalStatsCommunityStatItemBinding::inflate,
        onViewCreated = { b ->
          b.card.shapeAppearanceModel = b.card.shapeAppearanceModel.toBuilder().apply {
            setAllCorners(CornerFamily.CUT, 0f)
          }.build()
        }
      ) { item, b, h ->

        if (item.communityRef != null) {
          b.title.text = item.communityRef.getLocalizedFullName(context)
          b.card.setOnClickListener { onCommunityClick(item.communityRef) }
        } else {
          b.title.text = context.getString(R.string.unknown)
          b.card.setOnClickListener(null)
        }
        b.stat.text = PrettyPrintUtils.defaultDecimalFormat.format(item.value)
      }
      addItemType(
        clazz = Item.PersonStat::class,
        inflateFn = LocalStatsPersonStatItemBinding::inflate,
        onViewCreated = { b ->
          b.card.shapeAppearanceModel = b.card.shapeAppearanceModel.toBuilder().apply {
            setAllCorners(CornerFamily.CUT, 0f)
          }.build()
        }
      ) { item, b, h ->
        if (item.person != null) {
          b.title.text = "${item.person.display_name ?: item.person.name}@${item.person.instance}"
          b.card.setOnClickListener { onPersonClick(item.person.toPersonRef()) }
        } else {
          b.title.text = context.getString(R.string.unknown)
          b.card.setOnClickListener(null)
        }
        b.stat.text = PrettyPrintUtils.defaultDecimalFormat.format(item.value)
      }
      addItemType(
        clazz = Item.StatEndItem::class,
        inflateFn = LocalStatsStatEndItemBinding::inflate,
        onViewCreated = { b ->
          b.card.shapeAppearanceModel = b.card.shapeAppearanceModel.toBuilder().apply {
            setTopLeftCorner(CornerFamily.CUT, 0f)
            setTopRightCorner(CornerFamily.CUT, 0f)
          }.build()
        }
      ) { item, b, h -> }
      addItemType(
        clazz = Item.FooterItem::class,
        inflateFn = GenericSpaceFooterItemBinding::inflate,
      ) { item, b, h -> }
      addItemType(
        clazz = Item.SpaceItem::class,
        inflateFn = LocalStatsSpaceItemBinding::inflate,
      ) { item, b, h -> }
      addItemType(
        clazz = Item.LocalTrackingDisabledWarningItem::class,
        inflateFn = LocalStatsWarningItemBinding::inflate,
      ) { item, b, h ->
        b.body.text = context.getString(R.string.warn_local_tracking_disabled)
        b.card.setOnClickListener { onLocalTrackingSettingsClick() }
      }
      addItemType(
        clazz = Item.NoDataItem::class,
        inflateFn = LocalStatsNoDataItemBinding::inflate,
      ) { item, b, h -> }
    }

    override fun getItemViewType(position: Int): Int =
      adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int,
    ): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(
      holder: RecyclerView.ViewHolder,
      position: Int,
    ) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int =
      adapterHelper.itemCount

    private fun refresh() {
      val data = model ?: return
      val newItems = mutableListOf<Item>()

      newItems += Item.HeaderItem

      if (isLocalTrackingEnabled == false) {
        newItems += Item.LocalTrackingDisabledWarningItem
        newItems += Item.SpaceItem
      }

      newItems += Item.SectionTitleItem(context.getString(R.string.frequented_communities))
      if (data.mostVisitedCommunities.isEmpty()) {
        newItems += Item.NoDataItem
      } else {
        data.mostVisitedCommunities.mapTo(newItems) {
          Item.CommunityStat(it.key, it.value)
        }
      }
      newItems += Item.StatEndItem
      newItems += Item.SpaceItem

      newItems += Item.SectionTitleItem(context.getString(R.string.favorite_communities))
      if (data.favoriteCommunities.isEmpty()) {
        newItems += Item.NoDataItem
      } else {
        data.favoriteCommunities.mapTo(newItems) {
          Item.CommunityStat(it.key, it.value)
        }
      }
      newItems += Item.StatEndItem
      newItems += Item.SpaceItem

      newItems += Item.SectionTitleItem(context.getString(R.string.user_interactions))
      if (data.userInteractions.isEmpty()) {
        newItems += Item.NoDataItem
      } else {
        data.userInteractions.mapTo(newItems) {
          Item.PersonStat(it.first, it.second)
        }
      }
      newItems += Item.StatEndItem
      newItems += Item.SpaceItem

      newItems += Item.StatSummaryItem(data.events)

      newItems += Item.FooterItem

      adapterHelper.setItems(newItems, this)
    }

  }
}