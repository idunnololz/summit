package com.idunnololz.summit.lemmy.multicommunity

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.account.info.instance
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.CommunitySelectorSearchResultCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorSelectedCommunityItemBinding
import com.idunnololz.summit.databinding.ItemCommunityGroupHeaderBinding
import com.idunnololz.summit.databinding.ItemCommunityNoResultsBinding
import com.idunnololz.summit.databinding.ItemCommunityStaticCommunityBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityDataSource.Companion.MULTI_COMMUNITY_DATA_SOURCE_LIMIT
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CommunityAdapter @AssistedInject constructor(
  @Assisted private val context: Context,
  @Assisted("canSelectMultipleCommunities")
  private val canSelectMultipleCommunities: Boolean,
  @Assisted("showFeeds")
  private val showFeeds: Boolean,
  @Assisted private val onTooManyCommunities: (Int) -> Unit = {},
  @Assisted private val onSingleCommunitySelected: (
    CommunityRef,
    icon: String?,
    communityId: Int,
  ) -> Unit = { _, _, _ -> },
  private val avatarHelper: AvatarHelper,
  private val offlineManager: OfflineManager,
  private val accountManager: AccountManager,
  private val accountInfoManager: AccountInfoManager,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  @AssistedFactory
  interface Factory {
    fun create(
      context: Context,
      @Assisted("canSelectMultipleCommunities")
      canSelectMultipleCommunities: Boolean,
      @Assisted("showFeeds")
      showFeeds: Boolean,
      onTooManyCommunities: (Int) -> Unit = {},
      onSingleCommunitySelected: (
        CommunityRef,
        icon: String?,
        communityId: Int,
      ) -> Unit = { _, _, _ -> },
    ): CommunityAdapter
  }

  private sealed interface Item {

    data class GroupHeaderItem(
      val text: String,
      val stillLoading: Boolean = false,
    ) : Item

    data class NoResultsItem(
      val text: String,
    ) : Item

    data class SelectedCommunityItem(
      val icon: String?,
      val communityRef: CommunityRef.CommunityRefByName,
    ) : Item

    data class SearchResultCommunityItem(
      val text: String,
      val communityView: CommunityView,
      val monthlyActiveUsers: Int,
      val isChecked: Boolean,
    ) : Item

    data class SubscribedCommunityItem(
      val text: String,
      val subscribedCommunity: AccountSubscription,
      val isChecked: Boolean,
    ) : Item

    class StaticChildItem(
      val text: String,
      @DrawableRes val iconResId: Int,
      val communityRef: CommunityRef,
    ) : Item
  }

  var subscribedCommunities: List<AccountSubscription>? = null
    set(value) {
      field = value

      refreshItems { }
    }
  var selectedCommunities = LinkedHashMap<CommunityRef.CommunityRefByName, String?>()

  private var serverResultsInProgress = false
  private var serverQueryResults: List<CommunityView> = listOf()

  private var query: String? = null

  private val adapterHelper = AdapterHelper<Item>(
    areItemsTheSame = { old, new ->
      old::class == new::class && when (old) {
        is Item.SelectedCommunityItem -> {
          old == new
        }
        is Item.GroupHeaderItem -> {
          old.text == (new as Item.GroupHeaderItem).text
        }
        is Item.NoResultsItem ->
          old.text == (new as Item.NoResultsItem).text
        is Item.SearchResultCommunityItem -> {
          old.communityView.community.id ==
            (new as Item.SearchResultCommunityItem).communityView.community.id
        }
        is Item.SubscribedCommunityItem -> {
          old.subscribedCommunity.id ==
            (new as Item.SubscribedCommunityItem).subscribedCommunity.id
        }
        is Item.StaticChildItem ->
          old.communityRef == (new as Item.StaticChildItem).communityRef
      }
    },
  ).apply {
    addItemType(
      clazz = Item.GroupHeaderItem::class,
      inflateFn = ItemCommunityGroupHeaderBinding::inflate,
    ) { item, b, _ ->
      b.titleTextView.text = item.text

      if (item.stillLoading) {
        b.progressBar.visibility = View.VISIBLE
      } else {
        b.progressBar.visibility = View.GONE
      }
    }

    addItemType(
      clazz = Item.SelectedCommunityItem::class,
      inflateFn = CommunitySelectorSelectedCommunityItemBinding::inflate,
    ) { item, b, h ->
      avatarHelper.loadCommunityIcon(
        imageView = b.icon,
        communityRef = item.communityRef,
        iconUrl = item.icon,
      )

      b.title.text = item.communityRef.name

      @Suppress("SetTextI18n")
      b.monthlyActives.text = "(${item.communityRef.instance})"

      b.checkbox.isChecked = true
      b.checkbox.setOnClickListener {
        toggleCommunity(item.communityRef, item.icon)
      }
      h.itemView.setOnClickListener {
        toggleCommunity(item.communityRef, item.icon)
      }
    }
    addItemType(
      clazz = Item.SearchResultCommunityItem::class,
      inflateFn = CommunitySelectorSearchResultCommunityItemBinding::inflate,
    ) { item, b, h ->
      avatarHelper.loadCommunityIcon(
        imageView = b.icon,
        community = item.communityView.community,
      )

      b.title.text = item.text
      val mauString = LemmyUtils.abbrevNumber(item.monthlyActiveUsers.toLong())

      @Suppress("SetTextI18n")
      b.monthlyActives.text = "(${context.getString(R.string.mau_format, mauString)}) " +
        "(${item.communityView.community.instance})"

      if (canSelectMultipleCommunities) {
        b.checkbox.visibility = View.VISIBLE
      } else {
        b.checkbox.visibility = View.GONE
      }
      b.checkbox.isChecked = item.isChecked
      b.checkbox.setOnClickListener {
        toggleCommunity(
          item.communityView.community.toCommunityRef(),
          item.communityView.community.icon,
        )
      }
      h.itemView.setOnClickListener {
        if (canSelectMultipleCommunities) {
          toggleCommunity(
            item.communityView.community.toCommunityRef(),
            item.communityView.community.icon,
          )
        } else {
          onSingleCommunitySelected(
            item.communityView.community.toCommunityRef(),
            item.communityView.community.icon,
            item.communityView.community.id,
          )
        }
      }
    }
    addItemType(
      clazz = Item.SubscribedCommunityItem::class,
      inflateFn = CommunitySelectorSearchResultCommunityItemBinding::inflate,
    ) { item, b, h ->
      avatarHelper.loadCommunityIcon(
        imageView = b.icon,
        communityRef = item.subscribedCommunity.toCommunityRef(),
        iconUrl = item.subscribedCommunity.icon,
      )

      b.title.text = item.text

      b.monthlyActives.text = item.subscribedCommunity.instance

      if (canSelectMultipleCommunities) {
        b.checkbox.visibility = View.VISIBLE
      } else {
        b.checkbox.visibility = View.GONE
      }
      b.checkbox.isChecked = item.isChecked
      b.checkbox.setOnClickListener {
        toggleCommunity(
          item.subscribedCommunity.toCommunityRef(),
          item.subscribedCommunity.icon,
        )
      }
      h.itemView.setOnClickListener {
        if (canSelectMultipleCommunities) {
          toggleCommunity(
            item.subscribedCommunity.toCommunityRef(),
            item.subscribedCommunity.icon,
          )
        } else {
          onSingleCommunitySelected(
            item.subscribedCommunity.toCommunityRef(),
            item.subscribedCommunity.icon,
            item.subscribedCommunity.id,
          )
        }
      }
    }
    addItemType(
      clazz = Item.StaticChildItem::class,
      inflateFn = ItemCommunityStaticCommunityBinding::inflate,
    ) { item, b, h ->
      b.icon.setImageResource(item.iconResId)
      b.textView.text = item.text

      h.itemView.setOnClickListener {
        onSingleCommunitySelected(item.communityRef, null, 0)
      }
    }
    addItemType(
      clazz = Item.NoResultsItem::class,
      inflateFn = ItemCommunityNoResultsBinding::inflate,
    ) { item, b, _ ->
      b.text.text = item.text
    }
  }

  private fun toggleCommunity(ref: CommunityRef.CommunityRefByName, icon: String?) {
    if (selectedCommunities.contains(ref)) {
      selectedCommunities.remove(ref)
    } else {
      if (selectedCommunities.size == MULTI_COMMUNITY_DATA_SOURCE_LIMIT) {
        onTooManyCommunities(MULTI_COMMUNITY_DATA_SOURCE_LIMIT)
      } else {
        selectedCommunities[ref] = icon
      }
    }

    refreshItems { }
  }

  override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
    adapterHelper.onCreateViewHolder(parent, viewType)

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    adapterHelper.onBindViewHolder(holder, position)
  }

  override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
    super.onViewRecycled(holder)
    offlineManager.cancelFetch(holder.itemView)
  }

  override fun getItemCount(): Int = adapterHelper.itemCount

  fun setSelectedCommunities(selectedCommunities: List<CommunityRef.CommunityRefByName>) {
    this.selectedCommunities.clear()
    selectedCommunities.forEach {
      this.selectedCommunities[it] = null
    }

    refreshItems { }
  }

  private fun refreshItems(cb: () -> Unit) {
    val query = query
    val isQueryActive = !query.isNullOrBlank()

    val newItems = mutableListOf<Item>()

    if (canSelectMultipleCommunities) {
      newItems.add(Item.GroupHeaderItem(context.getString(R.string.selected_communities)))
      if (selectedCommunities.isEmpty()) {
        newItems += Item.NoResultsItem(context.getString(R.string.no_communities_selected))
      } else {
        selectedCommunities.forEach { (communityRef, icon) ->
          newItems += Item.SelectedCommunityItem(
            icon,
            communityRef,
          )
        }
      }
    }

    if (isQueryActive) {
      newItems.add(
        Item.GroupHeaderItem(
          context.getString(R.string.server_results),
          serverResultsInProgress,
        ),
      )
      if (serverQueryResults.isEmpty()) {
        if (serverResultsInProgress) {
          newItems.add(Item.NoResultsItem(context.getString(R.string.loading)))
        } else {
          newItems.add(Item.NoResultsItem(context.getString(R.string.no_results_found)))
        }
      } else {
        serverQueryResults.forEach {
          newItems += Item.SearchResultCommunityItem(
            it.community.name,
            it,
            it.counts.users_active_month,
            selectedCommunities.contains(it.community.toCommunityRef()),
          )
        }
      }
    } else {
      if (showFeeds) {
        newItems.add(Item.GroupHeaderItem(context.getString(R.string.feeds)))
        newItems.add(
          Item.StaticChildItem(
            context.getString(R.string.all),
            R.drawable.ic_feed_all,
            CommunityRef.All(),
          ),
        )

        val account = accountManager.currentAccount.asAccount
        if (account != null) {
          newItems.add(
            Item.StaticChildItem(
              context.getString(R.string.subscribed),
              R.drawable.baseline_subscriptions_24,
              CommunityRef.Subscribed(null),
            ),
          )
          newItems.add(
            Item.StaticChildItem(
              context.getString(R.string.local),
              R.drawable.ic_feed_home,
              CommunityRef.Local(null),
            ),
          )
          if (accountInfoManager.currentFullAccount.value
              ?.accountInfo
              ?.miscAccountInfo
              ?.modCommunityIds
              ?.isNotEmpty() == true
          ) {
            newItems.add(
              Item.StaticChildItem(
                context.getString(R.string.moderated_communities),
                R.drawable.outline_shield_24,
                CommunityRef.ModeratedCommunities(null),
              ),
            )
          }

          newItems.add(
            Item.StaticChildItem(
              context.getString(R.string.all_subscribed),
              R.drawable.baseline_groups_24,
              CommunityRef.AllSubscribed(),
            ),
          )
        } else {
          newItems.add(
            Item.StaticChildItem(
              context.getString(R.string.local),
              R.drawable.ic_feed_home,
              CommunityRef.Local(null),
            ),
          )
        }
      }

      val subscribedCommunities = subscribedCommunities
      if (!subscribedCommunities.isNullOrEmpty()) {
        newItems.add(
          Item.GroupHeaderItem(
            context.getString(R.string.subscribed_communities),
            false,
          ),
        )
        subscribedCommunities.forEach {
          newItems += Item.SubscribedCommunityItem(
            it.name,
            it,
            selectedCommunities.contains(it.toCommunityRef()),
          )
        }
      }
    }

    adapterHelper.setItems(newItems, this, cb)
  }

  fun setQuery(query: String?, cb: () -> Unit) {
    this.query = query

    refreshItems(cb)
  }

  fun setQueryServerResults(serverQueryResults: List<CommunityView>) {
    this.serverQueryResults = serverQueryResults
    serverResultsInProgress = false

    refreshItems {}
  }

  fun setQueryServerResultsInProgress() {
    serverQueryResults = listOf()
    serverResultsInProgress = true

    refreshItems {}
  }
}
