package com.idunnololz.summit.settings.importAndExport

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.databinding.BackupItemBinding
import com.idunnololz.summit.util.PrettyPrintStyles
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.tsToConcise

class BackupsAdapter(
  private val context: Context,
  private val onBackupClick: (SettingsBackupManager.BackupInfo) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private sealed interface Item {
    data class BackupItem(
      val backupInfo: SettingsBackupManager.BackupInfo,
    ) : Item
  }

  private val adapterHelper = AdapterHelper<Item>(
    areItemsTheSame = { oldItem, newItem ->
      oldItem::class == newItem::class &&
        when (oldItem) {
          is Item.BackupItem ->
            oldItem.backupInfo.file.path ==
              (newItem as Item.BackupItem).backupInfo.file.path
        }
    },
  ).apply {
    addItemType(Item.BackupItem::class, BackupItemBinding::inflate) { item, b, h ->
      b.title.text = item.backupInfo.file.name
      b.subtitle.text =
        tsToConcise(context, item.backupInfo.file.lastModified(), PrettyPrintStyles.SHORT_DYNAMIC)
      b.root.setOnClickListener {
        onBackupClick(item.backupInfo)
      }
    }
  }

  var backupData: List<SettingsBackupManager.BackupInfo> = listOf()
    set(value) {
      field = value

      updateItems()
    }

  init {
    updateItems()
  }

  private fun updateItems() {
    val newItems = backupData.map { Item.BackupItem(it) }

    adapterHelper.setItems(newItems, this)
  }

  override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
    adapterHelper.onCreateViewHolder(parent, viewType)

  override fun getItemCount(): Int = adapterHelper.itemCount

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
    adapterHelper.onBindViewHolder(holder, position)
}
