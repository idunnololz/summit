package com.idunnololz.summit.settings.cache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
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
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.view.StorageUsageItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsCacheFragment : BaseFragment<FragmentCacheBinding>(),
    SettingValueUpdateCallback {

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

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = cacheSettings.getPageName(context)
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
    }

    override fun onDestroyView() {
        offlineManager.removeOfflineDownloadProgressListener(progressListener)
        super.onDestroyView()
    }

    private fun updateRendering() {
        if (!isBindingAvailable()) return

        val context = requireContext()

        val colors = listOf(
            ContextCompat.getColor(context, R.color.style_pink),
            ContextCompat.getColor(context, R.color.style_amber),
            ContextCompat.getColor(context, R.color.style_blue),
            ContextCompat.getColor(context, R.color.style_green),
            ContextCompat.getColor(context, R.color.style_orange),
        )

        val totalSize = Utils.getSizeOfFile(context.cacheDir)
        val imageDirSize = Utils.getSizeOfFile(directoryHelper.imagesDir)
        val videoDirSize = Utils.getSizeOfFile(directoryHelper.videosDir) +
            Utils.getSizeOfFile(directoryHelper.videoCacheDir)

        binding.storageUsageView.setStorageUsage(
            listOf(
                StorageUsageItem(
                    "Images",
                    imageDirSize,
                    colors[0],
                ),
                StorageUsageItem(
                    "Videos",
                    videoDirSize,
                    colors[1],
                ),
                StorageUsageItem(
                    "Other",
                    totalSize - (imageDirSize + videoDirSize),
                    colors[2],
                ),
            ),
        )
        cacheSettings.clearCache.bindTo(binding.clearMediaCache) {
            offlineManager.clearOfflineData()

            updateRendering()
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
