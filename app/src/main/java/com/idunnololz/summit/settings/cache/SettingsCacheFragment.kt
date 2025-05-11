package com.idunnololz.summit.settings.cache

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.idunnololz.summit.R
import com.idunnololz.summit.cache.CachePolicy
import com.idunnololz.summit.cache.CachePolicyManager
import com.idunnololz.summit.databinding.SettingItemStorageViewBinding
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.settings.BaseSettingsFragment
import com.idunnololz.summit.settings.CacheSettings
import com.idunnololz.summit.settings.SettingModelItem
import com.idunnololz.summit.settings.asCustomItem
import com.idunnololz.summit.settings.asCustomViewSettingsItem
import com.idunnololz.summit.settings.asSingleChoiceSelectorItem
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.view.StorageUsageItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsCacheFragment :
  BaseSettingsFragment() {

  private val viewModel: SettingsCacheViewModel by viewModels()

  @Inject
  lateinit var directoryHelper: DirectoryHelper

  @Inject
  lateinit var offlineManager: OfflineManager

  @Inject
  override lateinit var settings: CacheSettings

  @Inject
  lateinit var cachePolicyManager: CachePolicyManager

  data class StorageViewModel(
    val error: String?,
    val loading: Boolean,
    val items: List<StorageUsageItem>,
  )

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel.dataModel.observe(viewLifecycleOwner) {
      refresh()
    }

    viewModel.generateDataModel()
  }

  override fun generateData(): List<SettingModelItem> {
    val context = requireContext()
    val colors = listOf(
      ContextCompat.getColor(context, R.color.style_pink),
      ContextCompat.getColor(context, R.color.style_amber),
      ContextCompat.getColor(context, R.color.style_blue),
      ContextCompat.getColor(context, R.color.style_green),
      ContextCompat.getColor(context, R.color.style_orange),
    )

    return listOf(
      settings.cacheInfo.asCustomViewSettingsItem(
        typeId = R.id.custom_setting_cache_info,
        inflateFn = SettingItemStorageViewBinding::inflate,
        bindViewHolder = { item, b, h ->
          if (item.payload.error != null) {
            b.storageUsageView.setErrorText(item.payload.error)
          } else if (item.payload.loading) {
            b.storageUsageView.setLoadingText(context.getString(R.string.loading))
          } else {
            b.storageUsageView.setStorageUsage(item.payload.items)
          }
        },
        payload = viewModel.dataModel.value.let {
          return@let when (it) {
            is StatefulData.Error -> {
              StorageViewModel(
                context.getString(R.string.error_cache_size_calculation),
                false,
                listOf()
              )
            }
            is StatefulData.Loading -> {
              StorageViewModel(
                null,
                true,
                listOf()
              )
            }
            is StatefulData.NotStarted -> {
              StorageViewModel(
                null,
                true,
                listOf()
              )
            }
            is StatefulData.Success -> {
              StorageViewModel(
                null,
                false,
                listOf(
                  StorageUsageItem(
                    context.getString(R.string.images),
                    it.data.imagesSizeBytes,
                    colors[0],
                  ),
                  StorageUsageItem(
                    context.getString(R.string.videos),
                    it.data.videosSizeBytes,
                    colors[1],
                  ),
                  StorageUsageItem(
                    context.getString(R.string.other_cached_media),
                    it.data.cacheMediaSizeBytes,
                    colors[3],
                  ),
                  StorageUsageItem(
                    context.getString(R.string.network),
                    it.data.cacheNetworkCacheSizeBytes,
                    colors[4],
                  ),
                  StorageUsageItem(
                    context.getString(R.string.other),
                    it.data.cacheOtherSizeBytes,
                    colors[2],
                  ),
                ),
              )
            }
          }
        },
      ),
      settings.clearCache.asCustomItem {
        offlineManager.clearOfflineData()

        refresh()
        viewModel.generateDataModel()
      },
      settings.cachePolicy.asSingleChoiceSelectorItem(
        { preferences.cachePolicy.value },
        {
          preferences.cachePolicy = CachePolicy.parse(it)
          cachePolicyManager.refreshCachePolicy()
          refresh()
        },
      )
    )
  }
}
