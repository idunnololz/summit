package com.idunnololz.summit.presets

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil3.load
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.api.SummitServerClient
import com.idunnololz.summit.api.summit.PresetDto
import com.idunnololz.summit.databinding.ItemPresetsHeaderBinding
import com.idunnololz.summit.databinding.ItemPresetsPresetBinding
import com.idunnololz.summit.util.recyclerView.AdapterHelper

data class PresetData(
  val id: String,
  val presetName: String,
  val presetDescription: String,
  val presetData: String,
  val createTs: Long,
  val updateTs: Long,
  val isApproved: Boolean,
  val phonePreviewUrl: String,
  val tabletPreviewUrl: String,
  val hasPreview: Boolean,
)

fun List<PresetDto>.toPresetData(summitServerClient: SummitServerClient) = mapNotNull {
  val id = it.id ?: return@mapNotNull null
  PresetData(
    id = id,
    presetName = it.presetName,
    presetDescription = it.presetDescription,
    presetData = it.presetData,
    createTs = it.createTs,
    updateTs = it.updateTs,
    isApproved = it.isApproved ?: return@mapNotNull null,
    phonePreviewUrl =
    summitServerClient.getAuthPublicFileUrl("presets/$id.1.jpeg"),
    tabletPreviewUrl =
    summitServerClient.getAuthPublicFileUrl("presets/$id.2.jpeg"),
    hasPreview = it.hasPreview ?: false,
  )
}

class PresetsAdapter(
  private val includeHeader: Boolean,
  private val onShareAPresetClick: () -> Unit,
) : RecyclerView.Adapter<ViewHolder>() {

  private sealed interface Item {
    data object HeaderItem : Item

    data class PresetItem(
      val presetData: PresetData,
    ) : Item

    data object FooterItem : Item
  }

  private val adapterHelper = AdapterHelper<Item>({ old, new ->
    old::class == new::class && when (old) {
      Item.HeaderItem -> true
      Item.FooterItem -> true
      is Item.PresetItem ->
        old.presetData.id == (new as Item.PresetItem).presetData.id
    }
  }).apply {
    addItemType(Item.HeaderItem::class, ItemPresetsHeaderBinding::inflate) { item, b, h ->
      b.shareAPreset.setOnClickListener {
        onShareAPresetClick()
      }
    }
    addItemType(Item.PresetItem::class, ItemPresetsPresetBinding::inflate) { item, b, h ->
      b.title.text = item.presetData.presetName
      if (item.presetData.presetDescription.isNotBlank()) {
        b.body.visibility = View.VISIBLE
        b.body.text = item.presetData.presetDescription
      } else {
        b.body.visibility = View.GONE
      }

      if (!item.presetData.hasPreview) {
        b.phonePreviewContainer.visibility = View.GONE
        b.tabletPreviewContainer.visibility = View.GONE
      } else {
        b.phonePreviewContainer.visibility = View.VISIBLE
        b.tabletPreviewContainer.visibility = View.VISIBLE

        b.tabletPreview.load(item.presetData.tabletPreviewUrl) {
          this.httpHeaders(
            NetworkHeaders.Builder()
              .set("Authorization", "Bearer ${BuildConfig.SUMMIT_JWT}")
              .build(),
          )
        }
        b.phonePreview.load(item.presetData.phonePreviewUrl) {
          this.httpHeaders(
            NetworkHeaders.Builder()
              .set("Authorization", "Bearer ${BuildConfig.SUMMIT_JWT}")
              .build(),
          )
        }
      }
    }
  }

  private var data: List<PresetData>? = null

  init {
    refreshItems()
  }

  override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
    adapterHelper.onCreateViewHolder(parent, viewType)

  override fun getItemCount(): Int = adapterHelper.itemCount

  override fun onBindViewHolder(holder: ViewHolder, position: Int) =
    adapterHelper.onBindViewHolder(holder, position)

  fun setData(presets: List<PresetData>) {
    this.data = presets
    refreshItems()
  }

  private fun refreshItems() {
    val data = this.data
    val newItems = mutableListOf<Item>()

    if (includeHeader) {
      newItems += Item.HeaderItem
    }

    data?.mapTo(newItems) {
      Item.PresetItem(it)
    }

    adapterHelper.setItems(newItems, this)
  }
}
