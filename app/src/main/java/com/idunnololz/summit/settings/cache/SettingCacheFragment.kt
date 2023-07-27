package com.idunnololz.summit.settings.cache

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentCacheBinding
import com.idunnololz.summit.offline.OfflineDownloadProgressListener
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.view.StorageUsageItem
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingCacheFragment : BaseFragment<FragmentCacheBinding>() {

    private var progressListener: OfflineDownloadProgressListener? = null

    @Inject
    lateinit var offlineManager: OfflineManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        requireMainActivity().apply {
            setupForFragment<SettingCacheFragment>()
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
            supportActionBar?.title = context.getString(R.string.cache)
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

        binding.clearCacheMedia.setOnClickListener {
            offlineManager.clearOfflineData()

            refreshUi()
        }

        refreshUi()
    }

    override fun onDestroyView() {
        offlineManager.removeOfflineDownloadProgressListener(progressListener)
        super.onDestroyView()
    }

    private fun refreshUi() {
        if (!isBindingAvailable()) return

        val context = requireContext()

        val colors = listOf(
            ContextCompat.getColor(context, R.color.style_pink),
            ContextCompat.getColor(context, R.color.style_amber),
            ContextCompat.getColor(context, R.color.style_blue),
            ContextCompat.getColor(context, R.color.style_green),
            ContextCompat.getColor(context, R.color.style_orange),
        )

        binding.storageUsageView.setStorageUsage(
            listOf(
                StorageUsageItem(
                    "Images",
                    Utils.getSizeOfFile(offlineManager.imagesDir),
                    colors[0],
                ),
                StorageUsageItem(
                    "Videos",
                    Utils.getSizeOfFile(offlineManager.videosDir) +
                        Utils.getSizeOfFile(offlineManager.videoCacheDir),
                    colors[1],
                ),
                StorageUsageItem(
                    "Other",
                    Utils.getSizeOfFile(offlineManager.tabsDir),
                    colors[2],
                ),
            ),
        )
    }
}
