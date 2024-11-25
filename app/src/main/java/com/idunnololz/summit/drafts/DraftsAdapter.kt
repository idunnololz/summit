package com.idunnololz.summit.drafts

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.CommentDraftItemBinding
import com.idunnololz.summit.databinding.DraftLoadingItemBinding
import com.idunnololz.summit.databinding.EmptyDraftItemBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.PostDraftItemBinding
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.tsToShortDate

class DraftsAdapter(
    private val onDraftClick: (DraftEntry) -> Unit,
    private val onDeleteClick: (DraftEntry) -> Unit,
) : RecyclerView.Adapter<ViewHolder>() {

    var items: List<DraftsViewModel.ViewModelItem> = listOf()
        private set

    private val adapterHelper = AdapterHelper<DraftsViewModel.ViewModelItem>(
        areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                is DraftsViewModel.ViewModelItem.PostDraftItem -> {
                    old.draftEntry.id ==
                        (new as DraftsViewModel.ViewModelItem.PostDraftItem).draftEntry.id
                }
                is DraftsViewModel.ViewModelItem.CommentDraftItem -> {
                    old.draftEntry.id ==
                        (new as DraftsViewModel.ViewModelItem.CommentDraftItem).draftEntry.id
                }
                DraftsViewModel.ViewModelItem.LoadingItem -> true
                DraftsViewModel.ViewModelItem.EmptyItem -> true
                DraftsViewModel.ViewModelItem.HeaderItem -> true
            }
        },
    ).apply {
        addItemType(
            clazz = DraftsViewModel.ViewModelItem.HeaderItem::class,
            inflateFn = ItemGenericHeaderBinding::inflate
        ) { _, _, _ -> }
        addItemType(
            clazz = DraftsViewModel.ViewModelItem.PostDraftItem::class,
            inflateFn = PostDraftItemBinding::inflate,
        ) { item, b, h ->
            b.title.text = if (item.postData.name.isNullOrBlank()) {
                buildSpannedString {
                    append(b.title.context.getString(R.string.empty))
                    setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
                }
            } else {
                item.postData.name
            }
            b.text.text = if (item.postData.body.isNullOrBlank()) {
                buildSpannedString {
                    append(b.title.context.getString(R.string.empty))
                    setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
                }
            } else {
                item.postData.body
            }

            b.date.text = tsToShortDate(item.draftEntry.updatedTs)

            b.delete.setOnClickListener {
                onDeleteClick(item.draftEntry)
            }
            b.root.setOnClickListener {
                onDraftClick(item.draftEntry)
            }
        }

        addItemType(
            clazz = DraftsViewModel.ViewModelItem.CommentDraftItem::class,
            inflateFn = CommentDraftItemBinding::inflate,
        ) { item, b, h ->
            b.text.text = item.commentData.content

            b.date.text = tsToShortDate(item.draftEntry.updatedTs)

            b.delete.setOnClickListener {
                onDeleteClick(item.draftEntry)
            }
            b.root.setOnClickListener {
                onDraftClick(item.draftEntry)
            }
        }
        addItemType(
            clazz = DraftsViewModel.ViewModelItem.LoadingItem::class,
            inflateFn = DraftLoadingItemBinding::inflate,
        ) { item, b, h ->
            b.loadingView.showProgressBar()
        }
        addItemType(
            clazz = DraftsViewModel.ViewModelItem.EmptyItem::class,
            inflateFn = EmptyDraftItemBinding::inflate,
        ) { item, b, h -> }
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        adapterHelper.onBindViewHolder(holder, position)

    fun setItems(items: List<DraftsViewModel.ViewModelItem>, cb: () -> Unit) {
        this.items = items

        refreshItems(cb)
    }

    private fun refreshItems(cb: () -> Unit) {
        adapterHelper.setItems(items, this, cb)
    }
}