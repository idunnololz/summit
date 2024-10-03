package com.idunnololz.summit.lemmy.createOrEditPost

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.CommunitySearchResultCommunityItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorGroupItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class CommunitySearchResultsAdapter(
    private val context: Context,
    private val offlineManager: OfflineManager,
    private val avatarHelper: AvatarHelper,
    private val onCommunitySelected: (CommunityView) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Item {

        data class GroupHeaderItem(
            val text: String,
            val stillLoading: Boolean = false,
        ) : Item

        data class NoResultsItem(
            val text: String,
        ) : Item

        data class SearchResultCommunityItem(
            val text: String,
            val communityView: CommunityView,
            val monthlyActiveUsers: Int,
        ) : Item
    }

    private var serverResultsInProgress = false
    private var serverQueryResults: List<CommunityView> = listOf()

    private var query: String? = null

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
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
            clazz = Item.SearchResultCommunityItem::class,
            inflateFn = CommunitySearchResultCommunityItemBinding::inflate,
        ) { item, b, h ->
            avatarHelper.loadCommunityIcon(b.icon, item.communityView.community)

            b.title.text = item.text
            val mauString = LemmyUtils.abbrevNumber(item.monthlyActiveUsers.toLong())

            @Suppress("SetTextI18n")
            b.monthlyActives.text = "(${context.getString(R.string.mau_format, mauString)}) " +
                "(${item.communityView.community.instance})"

            h.itemView.setOnClickListener {
                onCommunitySelected(item.communityView)
            }
        }
        addItemType(
            clazz = Item.NoResultsItem::class,
            inflateFn = CommunitySelectorNoResultsItemBinding::inflate,
        ) { item, b, _ ->
            b.text.text = item.text
        }
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

    private fun refreshItems(cb: () -> Unit) {
        val newItems = mutableListOf<Item>()

        newItems.add(
            Item.GroupHeaderItem(
                context.getString(R.string.server_results),
                serverResultsInProgress,
            ),
        )
        if (serverQueryResults.isEmpty() && !serverResultsInProgress) {
            newItems.add(Item.NoResultsItem(context.getString(R.string.no_results_found)))
        } else {
            serverQueryResults.forEach {
                newItems += Item.SearchResultCommunityItem(
                    it.community.name,
                    it,
                    it.counts.users_active_month,
                )
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
