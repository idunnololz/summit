package com.idunnololz.summit.lemmy.multicommunity

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.CommunitySelectorGroupItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorSearchResultCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorSelectedCommunityItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityDataSource.Companion.MULTI_COMMUNITY_DATA_SOURCE_LIMIT
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class CommunityAdapter(
    private val context: Context,
    private val offlineManager: OfflineManager,
    private val canSelectMultipleCommunities: Boolean,
    private val onTooManyCommunities: (Int) -> Unit = {},
    private val onSingleCommunitySelected: (
        CommunityRef.CommunityRefByName, icon: String?) -> Unit = { _, _ -> },
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Item {

        data class GroupHeaderItem(
            val text: String,
            val stillLoading: Boolean = false,
        ) : Item

        data class NoResultsItem(
            val text: String,
        ) : Item

        data class SelectedCommunityItem(
            val communityRef: CommunityRef.CommunityRefByName,
        ) : Item

        data class SearchResultCommunityItem(
            val text: String,
            val communityView: CommunityView,
            val monthlyActiveUsers: Int,
            val isChecked: Boolean,
        ) : Item
    }

    var selectedCommunities = LinkedHashSet<CommunityRef.CommunityRefByName>()
    private var serverResultsInProgress = false
    private var serverQueryResults: List<CommunityView> = listOf()

    private var query: String? = null

    private val adapterHelper = AdapterHelper<Item> (
        areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                is Item.SelectedCommunityItem -> {
                    old == new
                }
                is Item.GroupHeaderItem -> {
                    old.text == (new as Item.GroupHeaderItem).text
                }
                is Item.NoResultsItem -> true
                is Item.SearchResultCommunityItem -> {
                    old.communityView.community.id ==
                        (new as Item.SearchResultCommunityItem).communityView.community.id
                }
            }
        },
    ).apply {
        addItemType(
            clazz = Item.GroupHeaderItem::class,
            inflateFn = CommunitySelectorGroupItemBinding::inflate,
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
            b.icon.load(R.drawable.ic_subreddit_default)

            b.title.text = item.communityRef.name

            @Suppress("SetTextI18n")
            b.monthlyActives.text = "(${item.communityRef.instance})"

            b.checkbox.isChecked = true
            b.checkbox.setOnClickListener {
                toggleCommunity(item.communityRef)
            }
            h.itemView.setOnClickListener {
                toggleCommunity(item.communityRef)
            }
        }
        addItemType(
            clazz = Item.SearchResultCommunityItem::class,
            inflateFn = CommunitySelectorSearchResultCommunityItemBinding::inflate,
        ) { item, b, h ->
            b.icon.load(R.drawable.ic_subreddit_default)
            offlineManager.fetchImage(h.itemView, item.communityView.community.icon) {
                b.icon.load(it)
            }

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
                toggleCommunity(item.communityView.community.toCommunityRef())
            }
            h.itemView.setOnClickListener {
                if (canSelectMultipleCommunities) {
                    toggleCommunity(item.communityView.community.toCommunityRef())
                } else {
                    onSingleCommunitySelected(
                        item.communityView.community.toCommunityRef(),
                        item.communityView.community.icon
                    )
                }
            }
        }
        addItemType(
            clazz = Item.NoResultsItem::class,
            inflateFn = CommunitySelectorNoResultsItemBinding::inflate,
        ) { item, b, _ ->
            b.text.text = item.text
        }
    }

    private fun toggleCommunity(ref: CommunityRef.CommunityRefByName) {
        if (selectedCommunities.contains(ref)) {
            selectedCommunities.remove(ref)
        } else {
            if (selectedCommunities.size == MULTI_COMMUNITY_DATA_SOURCE_LIMIT) {
                onTooManyCommunities(MULTI_COMMUNITY_DATA_SOURCE_LIMIT)
            } else {
                selectedCommunities.add(ref)
            }
        }

        refreshItems {  }
    }

    override fun getItemViewType(position: Int): Int =
        adapterHelper.getItemViewType(position)

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
        this.selectedCommunities.addAll(selectedCommunities)

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
                selectedCommunities.forEach {
                    newItems += Item.SelectedCommunityItem(
                        it,
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
                newItems.add(Item.NoResultsItem(context.getString(R.string.no_results_found)))
            } else {
                serverQueryResults.forEach {
                    newItems += Item.SearchResultCommunityItem(
                        it.community.name,
                        it,
                        it.counts.users_active_month,
                        selectedCommunities.contains(it.community.toCommunityRef())
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

        refreshItems({})
    }

    fun setQueryServerResultsInProgress() {
        serverQueryResults = listOf()
        serverResultsInProgress = true

        refreshItems({})
    }
}