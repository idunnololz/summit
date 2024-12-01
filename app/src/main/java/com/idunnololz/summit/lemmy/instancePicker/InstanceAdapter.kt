package com.idunnololz.summit.lemmy.instancePicker

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.databinding.InstanceSelectorSearchResultInstanceItemBinding
import com.idunnololz.summit.databinding.ItemInstanceSelectorGroupTitleBinding
import com.idunnololz.summit.lemmy.multicommunity.MultiCommunityDataSource.Companion.MULTI_COMMUNITY_DATA_SOURCE_LIMIT
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class InstanceAdapter(
    private val context: Context,
    private val offlineManager: OfflineManager,
    private val canSelectMultipleCommunities: Boolean,
    private val onTooManyInstances: (Int) -> Unit = {},
    private val onSingleInstanceSelected: (
        instance: String,
    ) -> Unit = { },
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Item {

        data class GroupHeaderItem(
            val text: String,
            val stillLoading: Boolean = false,
        ) : Item

        data class NoResultsItem(
            val text: String,
        ) : Item

        data class SelectedInstanceItem(
            val text: String,
            val instance: String,
        ) : Item

        data class SearchResultInstanceItem(
            val text: String,
            val instance: String,
            val isChecked: Boolean,
        ) : Item
    }

    var selectedInstances = LinkedHashSet<String>()
    private var serverResultsInProgress = false
    private var serverQueryResults: InstancePickerViewModel.SearchResults? = null

    private var query: String? = null

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                is Item.SelectedInstanceItem -> {
                    old == new
                }
                is Item.GroupHeaderItem -> {
                    old.text == (new as Item.GroupHeaderItem).text
                }
                is Item.NoResultsItem -> true
                is Item.SearchResultInstanceItem -> {
                    old.instance ==
                        (new as Item.SearchResultInstanceItem).instance
                }
            }
        },
    ).apply {
        addItemType(
            clazz = Item.GroupHeaderItem::class,
            inflateFn = ItemInstanceSelectorGroupTitleBinding::inflate,
        ) { item, b, _ ->
            b.titleTextView.text = item.text

            if (item.stillLoading) {
                b.progressBar.visibility = View.VISIBLE
            } else {
                b.progressBar.visibility = View.GONE
            }
        }

        addItemType(
            clazz = Item.SelectedInstanceItem::class,
            inflateFn = InstanceSelectorSearchResultInstanceItemBinding::inflate,
        ) { item, b, h ->

            b.title.text = item.text

            b.checkbox.isChecked = true
            b.checkbox.setOnClickListener {
                toggleCommunity(item.instance)
            }
            h.itemView.setOnClickListener {
                toggleCommunity(item.instance)
            }
        }
        addItemType(
            clazz = Item.SearchResultInstanceItem::class,
            inflateFn = InstanceSelectorSearchResultInstanceItemBinding::inflate,
        ) { item, b, h ->

            b.title.text = item.text

            if (canSelectMultipleCommunities) {
                b.checkbox.visibility = View.VISIBLE
            } else {
                b.checkbox.visibility = View.GONE
            }
            b.checkbox.isChecked = item.isChecked
            b.checkbox.setOnClickListener {
                toggleCommunity(item.instance)
            }
            h.itemView.setOnClickListener {
                if (canSelectMultipleCommunities) {
                    toggleCommunity(item.instance)
                } else {
                    onSingleInstanceSelected(item.instance)
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

    private fun toggleCommunity(instance: String) {
        if (selectedInstances.contains(instance)) {
            selectedInstances.remove(instance)
        } else {
            if (selectedInstances.size == MULTI_COMMUNITY_DATA_SOURCE_LIMIT) {
                onTooManyInstances(MULTI_COMMUNITY_DATA_SOURCE_LIMIT)
            } else {
                selectedInstances.add(instance)
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

    private fun refreshItems(cb: () -> Unit) {
        val query = query
        val isQueryActive = true

        val newItems = mutableListOf<Item>()

        if (canSelectMultipleCommunities) {
            newItems.add(Item.GroupHeaderItem(context.getString(R.string.selected_communities)))
            if (selectedInstances.isEmpty()) {
                newItems += Item.NoResultsItem(context.getString(R.string.no_communities_selected))
            } else {
                selectedInstances.forEach {
                    newItems += Item.SelectedInstanceItem(
                        it,
                        it,
                    )
                }
            }
        }

        if (isQueryActive) {
            newItems.add(
                Item.GroupHeaderItem(
                    context.getString(R.string.suggestions),
                    serverResultsInProgress,
                ),
            )
            val serverQueryResults = serverQueryResults

            if (serverQueryResults != null) {
                if (serverQueryResults.results.isEmpty()) {
                    newItems.add(Item.NoResultsItem(context.getString(R.string.no_results_found)))
                } else {
                    serverQueryResults.results.forEach {
                        newItems += Item.SearchResultInstanceItem(
                            it,
                            it,
                            selectedInstances.contains(it),
                        )
                    }
                }
            }
        }

        adapterHelper.setItems(newItems, this, cb)
    }

    fun setQuery(query: String?, cb: () -> Unit) {
        this.query = query

        refreshItems(cb)
    }

    fun setQueryServerResults(serverQueryResults: InstancePickerViewModel.SearchResults) {
        this.serverQueryResults = serverQueryResults
        serverResultsInProgress = false

        refreshItems({})
    }

    fun setQueryServerResultsInProgress() {
        serverQueryResults = null
        serverResultsInProgress = true

        refreshItems({})
    }
}
