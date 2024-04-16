package com.idunnololz.summit.lemmy.personPicker

import android.content.Context
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.CommunitySelectorGroupItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.databinding.PersonSelectorSearchResultPersonItemBinding
import com.idunnololz.summit.databinding.PersonSelectorSelectedPersonItemBinding
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.tint
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class PersonAdapter(
    private val context: Context,
    private val offlineManager: OfflineManager,
    private val canSelectMultiplePersons: Boolean,
    private val instance: String,
    private val onTooManyPersons: (Int) -> Unit = {},
    private val onSinglePersonSelected: (
        PersonRef.PersonRefByName,
        icon: String?,
        personId: PersonId,
    ) -> Unit = { _, _, _ -> },
    private val maxSelectedPersons: Int = 100,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Item {

        data class GroupHeaderItem(
            val text: String,
            val stillLoading: Boolean = false,
        ) : Item

        data class NoResultsItem(
            val text: String,
        ) : Item

        data class SelectedPersonItem(
            val personRef: PersonRef.PersonRefByName,
        ) : Item

        data class SearchResultPersonItem(
            val text: String,
            val personView: PersonView,
            val isChecked: Boolean,
        ) : Item
    }

    private val unimportantColor: Int = ContextCompat.getColor(context, R.color.colorTextFaint)

    var selectedPersons = LinkedHashSet<PersonRef.PersonRefByName>()
    private var serverResultsInProgress = false
    private var serverQueryResults: List<PersonView> = listOf()

    private var query: String? = null

    private val adapterHelper = AdapterHelper<Item>(
        areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                is Item.SelectedPersonItem -> {
                    old == new
                }
                is Item.GroupHeaderItem -> {
                    old.text == (new as Item.GroupHeaderItem).text
                }
                is Item.NoResultsItem -> true
                is Item.SearchResultPersonItem -> {
                    old.personView.person.id ==
                        (new as Item.SearchResultPersonItem).personView.person.id
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
            clazz = Item.SelectedPersonItem::class,
            inflateFn = PersonSelectorSelectedPersonItemBinding::inflate,
        ) { item, b, h ->
            b.icon.setImageDrawable(
                context.getDrawableCompat(R.drawable.baseline_person_24)?.tint(
                    context.getColorFromAttribute(
                        androidx.appcompat.R.attr.colorControlNormal,
                    ),
                ),
            )

            b.title.text = buildSpannedString {
                append(item.personRef.name)

                if (item.personRef.instance != instance) {
                    val s = length
                    append("@")
                    append(item.personRef.instance)
                    val e = length
                    setSpan(
                        ForegroundColorSpan(unimportantColor),
                        s,
                        e,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }

            b.checkbox.isChecked = true
            b.checkbox.setOnClickListener {
                toggleCommunity(item.personRef)
            }
            h.itemView.setOnClickListener {
                toggleCommunity(item.personRef)
            }
        }
        addItemType(
            clazz = Item.SearchResultPersonItem::class,
            inflateFn = PersonSelectorSearchResultPersonItemBinding::inflate,
        ) { item, b, h ->
            b.icon.setImageDrawable(
                context.getDrawableCompat(R.drawable.baseline_person_24)?.tint(
                    context.getColorFromAttribute(
                        androidx.appcompat.R.attr.colorControlNormal,
                    ),
                ),
            )

            offlineManager.fetchImage(h.itemView, item.personView.person.avatar) {
                b.icon.load(it)
            }

            b.title.text = buildSpannedString {
                append(item.text)

                if (item.personView.person.instance != instance) {
                    val s = length
                    append("@")
                    append(item.personView.person.instance)
                    val e = length
                    setSpan(
                        ForegroundColorSpan(unimportantColor),
                        s,
                        e,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }

            if (canSelectMultiplePersons) {
                b.checkbox.visibility = View.VISIBLE
            } else {
                b.checkbox.visibility = View.GONE
            }
            b.checkbox.isChecked = item.isChecked
            b.checkbox.setOnClickListener {
                toggleCommunity(item.personView.person.toPersonRef())
            }
            h.itemView.setOnClickListener {
                if (canSelectMultiplePersons) {
                    toggleCommunity(item.personView.person.toPersonRef())
                } else {
                    onSinglePersonSelected(
                        item.personView.person.toPersonRef(),
                        item.personView.person.avatar,
                        item.personView.person.id,
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

    private fun toggleCommunity(ref: PersonRef.PersonRefByName) {
        if (selectedPersons.contains(ref)) {
            selectedPersons.remove(ref)
        } else {
            if (selectedPersons.size == maxSelectedPersons) {
                onTooManyPersons(maxSelectedPersons)
            } else {
                selectedPersons.add(ref)
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

    fun setSelectedPersons(selectedCommunities: List<PersonRef.PersonRefByName>) {
        this.selectedPersons.clear()
        this.selectedPersons.addAll(selectedCommunities)

        refreshItems { }
    }

    private fun refreshItems(cb: () -> Unit) {
        val query = query
        val isQueryActive = !query.isNullOrBlank()

        val newItems = mutableListOf<Item>()

        if (canSelectMultiplePersons) {
            newItems.add(Item.GroupHeaderItem(context.getString(R.string.selected_communities)))
            if (selectedPersons.isEmpty()) {
                newItems += Item.NoResultsItem(context.getString(R.string.no_communities_selected))
            } else {
                selectedPersons.forEach {
                    newItems += Item.SelectedPersonItem(
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
                    newItems += Item.SearchResultPersonItem(
                        it.person.name,
                        it,
                        selectedPersons.contains(it.person.toPersonRef()),
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

    fun setQueryServerResults(serverQueryResults: List<PersonView>) {
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
