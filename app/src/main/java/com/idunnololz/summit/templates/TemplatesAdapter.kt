package com.idunnololz.summit.templates

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.TemplateItemAddTemplateItemBinding
import com.idunnololz.summit.databinding.TemplateItemTemplateItemBinding
import com.idunnololz.summit.templates.db.TemplateData
import com.idunnololz.summit.templates.db.TemplateEntry
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class TemplatesAdapter(
  private val onTemplateClick: (template: TemplateEntry) -> Unit,
  private val onAddTemplateClick: () -> Unit,
  private val onEditTemplateClick: (template: TemplateEntry) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  sealed interface Item {
    data class TemplateItem(
      val template: TemplateEntry,
      val data: TemplateData,
    ) : Item

    data object AddTemplateItem: Item
  }

  private val adapterHelper = AdapterHelper<Item>({ old, new ->
    when (old) {
      is Item.TemplateItem ->
        old.template.id == (new as Item.TemplateItem).template.id
      Item.AddTemplateItem -> true
    }
  }).apply {
    addItemType(
      clazz = Item.TemplateItem::class,
      inflateFn = TemplateItemTemplateItemBinding::inflate,
    ) { item, b, h ->
      b.title.text = item.data.title
      b.body.text = item.data.content

      b.card.setOnClickListener {
        onTemplateClick(item.template)
      }
      b.edit.setOnClickListener {
        onEditTemplateClick(item.template)
      }
    }
    addItemType(
      clazz = Item.AddTemplateItem::class,
      inflateFn = TemplateItemAddTemplateItemBinding::inflate,
    ) { item, b, h ->
      b.addTemplateButton.setOnClickListener {
        onAddTemplateClick()
      }
    }
  }

  private var data: List<TemplateEntry>? = null

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

  fun setData(data: List<TemplateEntry>) {
    this.data = data

    refreshItems()
  }

  private fun refreshItems() {
    val data = data ?: return
    val newItems = mutableListOf<Item>()

    data.forEach {
      if (it.data != null) {
        newItems += Item.TemplateItem(it, it.data)
      }
    }

    newItems += Item.AddTemplateItem

    adapterHelper.setItems(newItems, this)
  }
}