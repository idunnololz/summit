package com.idunnololz.summit.drafts.drafts

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.CommentDraftItemBinding
import com.idunnololz.summit.databinding.DraftLoadingItemBinding
import com.idunnololz.summit.databinding.EmptyDraftItemBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.ItemPostAndCommentTemplatePostBinding
import com.idunnololz.summit.databinding.PostDraftItemBinding
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.drafts.ViewModelItem.CommentTemplateItem
import com.idunnololz.summit.drafts.drafts.ViewModelItem.PostTemplateItem
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.imageTintListCompat
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.tsToShortDate

class DraftsAdapter(
  private val context: Context,
  private val filter: Filter,
  private val onDraftClick: (DraftEntry) -> Unit,
  private val onDeleteClick: (DraftEntry) -> Unit,
  private val onPostTemplateClick: (PostTemplateItem) -> Unit,
  private val onCommentTemplateClick: (CommentTemplateItem) -> Unit,
  private val onEditPostTemplateClick: (PostTemplateItem) -> Unit,
  private val onEditCommentTemplateClick: (CommentTemplateItem) -> Unit,
  private val onStartSelectionMode: (() -> Unit)? = null,
  private val onItemSelected: ((Long, Boolean) -> Unit)? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  var model: DraftsModel = DraftsModel()
    private set

  private val adapterHelper = AdapterHelper<ViewModelItem>(
    areItemsTheSame = { old, new ->
      old::class == new::class &&
        when (old) {
          is ViewModelItem.CommentDraftItem -> {
            old.draftEntry.id ==
              (new as ViewModelItem.CommentDraftItem).draftEntry.id
          }
          is ViewModelItem.PostDraftItem -> {
            old.draftEntry.id ==
              (new as ViewModelItem.PostDraftItem).draftEntry.id
          }
          is CommentTemplateItem ->
            old.entryId ==
              (new as CommentTemplateItem).entryId
          is PostTemplateItem ->
            old.entryId ==
              (new as PostTemplateItem).entryId

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
      clazz = CommentTemplateItem::class,
      inflateFn = ItemPostAndCommentTemplatePostBinding::inflate,
    ) { item, b, h ->
      b.icon.apply {
        if (item.isSelected) {
          val iconColor = context.getColorCompat(R.color.style_green)
          setImageResource(R.drawable.checkable_check_24)
          this.imageTintListCompat = ColorStateList.valueOf(iconColor)
          setBackgroundColor(ColorUtils.setAlphaComponent(iconColor, 77))
        } else {
          val iconColor = context.getColorCompat(R.color.style_amber)
          setImageResource(R.drawable.outline_comment_24)
          this.imageTintListCompat = ColorStateList.valueOf(iconColor)
          setBackgroundColor(ColorUtils.setAlphaComponent(iconColor, 77))
        }
      }
      b.more.isVisible = !item.isSelectable
      b.more.setOnClickListener {
        PopupMenu(context, b.more)
          .apply {
            menu.add(0, R.id.edit, 0, R.string.edit_template)
              .apply {
                setIcon(R.drawable.baseline_edit_24)
              }

            setOnMenuItemClickListener {
              when (it.itemId) {
                R.id.edit -> {
                  onEditCommentTemplateClick(item)
                }
              }

              true
            }

            show()
          }
      }
      b.title.text = if (item.commentTemplateData.name.isNullOrBlank()) {
        context.getString(R.string.no_name)
      } else {
        item.commentTemplateData.name
      }
      b.desc.text = item.description.ifBlank {
        context.getString(R.string.no_content)
      }
      b.root.setOnClickListener {
        if (item.isSelectable) {
          onItemSelected?.invoke(item.entryId, !item.isSelected)
        } else {
          onCommentTemplateClick(item)
        }
      }

      if (onStartSelectionMode != null && onItemSelected != null) {
        b.root.setOnLongClickListener {
          onStartSelectionMode()
          onItemSelected(item.entryId, !item.isSelected)
          true
        }
      }
    }
    addItemType(
      clazz = PostTemplateItem::class,
      inflateFn = ItemPostAndCommentTemplatePostBinding::inflate,
    ) { item, b, h ->
      b.icon.apply {
        if (item.isSelected) {
          val iconColor = context.getColorCompat(R.color.style_green)
          setImageResource(R.drawable.checkable_check_24)
          this.imageTintListCompat = ColorStateList.valueOf(iconColor)
          setBackgroundColor(ColorUtils.setAlphaComponent(iconColor, 77))
        } else {
          val iconColor = context.getColorCompat(R.color.style_blue)
          setImageResource(R.drawable.ic_post_24)
          this.imageTintListCompat = ColorStateList.valueOf(iconColor)
          setBackgroundColor(ColorUtils.setAlphaComponent(iconColor, 77))
        }
      }
      b.more.isVisible = !item.isSelectable
      b.more.setOnClickListener {
        PopupMenu(context, b.more)
          .apply {
            menu.add(0, R.id.edit, 0, R.string.edit_template)
              .apply {
                setIcon(R.drawable.baseline_edit_24)
              }

            setOnMenuItemClickListener {
              when (it.itemId) {
                R.id.edit -> {
                  onEditPostTemplateClick(item)
                }
              }

              true
            }

            show()
          }
      }
      b.title.text = if (item.postTemplateData.name.isNullOrBlank()) {
        context.getString(R.string.no_name)
      } else {
        item.postTemplateData.name
      }
      b.desc.text = item.description.ifBlank {
        context.getString(R.string.no_content)
      }
      b.root.setOnClickListener {
        if (item.isSelectable) {
          onItemSelected?.invoke(item.entryId, !item.isSelected)
        } else {
          onPostTemplateClick(item)
        }
      }

      if (onStartSelectionMode != null && onItemSelected != null) {
        b.root.setOnLongClickListener {
          onStartSelectionMode()
          onItemSelected(item.entryId, !item.isSelected)
          true
        }
      }
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
        onItemSelected?.invoke(draftEntry.id, !isSelected)
      }
    } else {
      select.visibility = View.GONE
    }

    delete.setOnClickListener {
      onDeleteClick(draftEntry)
    }

    root.setOnClickListener {
      if (isSelectable) {
        onItemSelected?.invoke(draftEntry.id, !isSelected)
      } else {
        onDraftClick(draftEntry)
      }
    }

    if (onStartSelectionMode != null && onItemSelected != null) {
      root.setOnLongClickListener {
        onStartSelectionMode()
        onItemSelected(draftEntry.id, !isSelected)
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
    adapterHelper.setItems(
      newItems = when (filter) {
        Filter.Drafts -> model.draftItems
        Filter.Templates -> model.templateItems
      },
      adapter = this,
      cb = cb
    )
  }
}
