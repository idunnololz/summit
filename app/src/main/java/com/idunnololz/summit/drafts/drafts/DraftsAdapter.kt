package com.idunnololz.summit.drafts.drafts

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.CommentDraftItemBinding
import com.idunnololz.summit.databinding.DraftLoadingItemBinding
import com.idunnololz.summit.databinding.EmptyDraftItemBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.PostDraftItemBinding
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.tsToShortDate

class DraftsAdapter(
  private val onDraftClick: (DraftEntry) -> Unit,
  private val onDeleteClick: (DraftEntry) -> Unit,
  private val onStartSelectionMode: (() -> Unit)? = null,
  private val onItemSelected: ((DraftEntry, Boolean) -> Unit)? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  var model: DraftsModel = DraftsModel()
    private set

  private val adapterHelper = AdapterHelper<ViewModelItem>(
      areItemsTheSame = { old, new ->
          old::class == new::class &&
                  when (old) {
                      is ViewModelItem.PostDraftItem -> {
                          old.draftEntry.id ==
                                  (new as ViewModelItem.PostDraftItem).draftEntry.id
                      }

                      is ViewModelItem.CommentDraftItem -> {
                          old.draftEntry.id ==
                                  (new as ViewModelItem.CommentDraftItem).draftEntry.id
                      }

                      ViewModelItem.LoadingItem -> true
                      ViewModelItem.EmptyItem -> true
                      ViewModelItem.HeaderItem -> true
                  }
      },
  ).apply {
    addItemType(
      clazz = ViewModelItem.HeaderItem::class,
      inflateFn = ItemGenericHeaderBinding::inflate,
    ) { _, _, _ -> }
    addItemType(
      clazz = ViewModelItem.PostDraftItem::class,
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

      bind(
        draftEntry = item.draftEntry,
        isSelectable = item.isSelectable,
        isSelected = item.isSelected,
        delete = b.delete,
        select = b.select,
        root = b.root,
      )
    }

    addItemType(
      clazz = ViewModelItem.CommentDraftItem::class,
      inflateFn = CommentDraftItemBinding::inflate,
    ) { item, b, h ->
      b.text.text = item.commentData.content

      b.date.text = tsToShortDate(item.draftEntry.updatedTs)

      bind(
        draftEntry = item.draftEntry,
        isSelectable = item.isSelectable,
        isSelected = item.isSelected,
        delete = b.delete,
        select = b.select,
        root = b.root,
      )
    }
    addItemType(
      clazz = ViewModelItem.LoadingItem::class,
      inflateFn = DraftLoadingItemBinding::inflate,
    ) { item, b, h ->
      b.loadingView.showProgressBar()
    }
    addItemType(
      clazz = ViewModelItem.EmptyItem::class,
      inflateFn = EmptyDraftItemBinding::inflate,
    ) { item, b, h -> }
  }

  private fun bind(
    draftEntry: DraftEntry,
    isSelectable: Boolean,
    isSelected: Boolean,
    delete: ImageView,
    select: CheckBox,
    root: View,
  ) {

    if (isSelectable) {
      select.visibility = View.VISIBLE

      select.isChecked = isSelected
      select.setOnClickListener {
        onItemSelected?.invoke(draftEntry, !isSelected)
      }
    } else {
      select.visibility = View.GONE
    }

    delete.setOnClickListener {
      onDeleteClick(draftEntry)
    }

    root.setOnClickListener {
      if (isSelectable) {
        onItemSelected?.invoke(draftEntry, !isSelected)
      } else {
        onDraftClick(draftEntry)
      }
    }

    if (onStartSelectionMode != null && onItemSelected != null) {
      root.setOnLongClickListener {
        onStartSelectionMode()
        onItemSelected(draftEntry, !isSelected)
        true
      }
    }
  }

  override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
    adapterHelper.onCreateViewHolder(parent, viewType)

  override fun getItemCount(): Int = adapterHelper.itemCount

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
    adapterHelper.onBindViewHolder(holder, position)

  fun setModel(model: DraftsModel, cb: () -> Unit) {
    this.model = model

    refreshItems(cb)
  }

  private fun refreshItems(cb: () -> Unit) {
    adapterHelper.setItems(model.items, this, cb)
  }
}