package com.idunnololz.summit.settings.cache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.idunnololz.summit.R
import com.idunnololz.summit.cache.CachePolicy
import com.idunnololz.summit.cache.CachePolicyManager
import com.idunnololz.summit.databinding.FragmentCacheBinding
import com.idunnololz.summit.offline.OfflineDownloadProgressListener
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.CacheSettings
import com.idunnololz.summit.settings.SettingPath.getPageName
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.dialogs.MultipleChoiceDialogFragment
import com.idunnololz.summit.settings.dialogs.SettingValueUpdateCallback
import com.idunnololz.summit.settings.util.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import com.idunnololz.summit.view.StorageUsageItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsCacheFragment :
    BaseFragment<FragmentCacheBinding>(),
    SettingValueUpdateCallback {

    private val viewModel: SettingsCacheViewModel by viewModels()

    private var progressListener: OfflineDownloadProgressListener? = null

    @Inject
    lateinit var directoryHelper: DirectoryHelper

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var cacheSettings: CacheSettings

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var cachePolicyManager: CachePolicyManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
        }

        setBinding(FragmentCacheBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setupToolbar(binding.toolbar, cacheSettings.getPageName(context))
        }

//        doStandardOfflineButton.setOnClickListener {
//            OfflineService.startWithConfig(context,
//                OfflineTaskConfig(
//                    minPosts = 20,
//                    roundPostsToNearestPage = true
//                )
//            )
//        }
//
//        deleteImages.setOnClickListener {
//            OfflineManager.instance.deleteOfflineImages()
//        }

        updateRendering()

        with(binding) {
            val colors = listOf(
                ContextCompat.getColor(context, R.color.style_pink),
                ContextCompat.getColor(context, R.color.style_amber),
                ContextCompat.getColor(context, R.color.style_blue),
                ContextCompat.getColor(context, R.color.style_green),
                ContextCompat.getColor(context, R.color.style_orange),
            )

            viewModel.dataModel.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        storageUsageView.setErrorText(
                            context.getString(R.string.error_cache_size_calculation),
                        )
                    }
                    is StatefulData.Loading -> {
                        storageUsageView.setLoadingText(
                            context.getString(R.string.loading),
                        )
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        storageUsageView.setStorageUsage(
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
            }
        }

        viewModel.generateDataModel()
    }

    override fun onDestroyView() {
        offlineManager.removeOfflineDownloadProgressListener(progressListener)
        super.onDestroyView()
    }

    private fun updateRendering() {
        if (!isBindingAvailable()) return

        val context = requireContext()

        cacheSettings.clearCache.bindTo(binding.clearMediaCache) {
            offlineManager.clearOfflineData()

            updateRendering()
            viewModel.generateDataModel()
        }
        cacheSettings.cachePolicy.bindTo(
            binding.cachePolicy,
            { preferences.cachePolicy.value },
            { setting, currentValue ->
                MultipleChoiceDialogFragment.newInstance(setting, currentValue)
                    .showAllowingStateLoss(childFragmentManager, "cachePolicy")
            },
        )
    }

    override fun updateValue(key: Int, value: Any?) {
        when (key) {
            cacheSettings.cachePolicy.id -> {
                preferences.cachePolicy = CachePolicy.parse(value as Int)
                cachePolicyManager.refreshCachePolicy()
            }
        }

        updateRendering()
    }
}
