package com.idunnololz.summit.offline

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentOfflineBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.setProgressCompat
import com.idunnololz.summit.view.StorageUsageItem
import com.idunnololz.summit.view.StorageUsageView
import java.lang.RuntimeException

class OfflineFragment : BaseFragment<FragmentOfflineBinding>(), OfflineSchedulerDialogFragment.OfflineSchedulerListener {

    private var progressListener: OfflineDownloadProgressListener? = null

    private var adapter: OfflineItemsAdapter? = null

    private val offlineManager = OfflineManager.instance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requireMainActivity().apply {
            setupForFragment<OfflineFragment>()
        }

        setBinding(FragmentOfflineBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().insetRootViewAutomatically(viewLifecycleOwner, view)

        val context = requireContext()

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

        progressListener =
            OfflineManager.instance.addOfflineDownloadProgressListener { _, progress ->
                adapter?.updateOfflineDownload(progress)
            }

        adapter = OfflineItemsAdapter(context)
        adapter?.refreshItems()

        binding.swipeRefreshLayout.setOnRefreshListener {
            adapter?.refreshItems()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        binding.recyclerView.setHasFixedSize(false)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        OfflineManager.instance.removeOfflineDownloadProgressListener(progressListener)
        super.onDestroyView()
    }

    override fun onOfflineScheduleChanged(enable: Boolean, recurringEvent: RecurringEvent) {
    }

    private sealed class Item(
        val type: Int,
        val id: Long
    ) {

        companion object {
            const val TYPE_HEADER = 0
            const val TYPE_STORAGE_USAGE = 1
            const val TYPE_OFFLINE_ACTION = 2
            const val TYPE_ACTIVE_OFFLINE_DOWNLOAD = 3
            const val TYPE_CLEAR_OFFLINE_DATA = 4
        }

        class HeaderItem() : Item(TYPE_HEADER, 0)
        class StorageUsageItem(
            val storageUsage: List<com.idunnololz.summit.view.StorageUsageItem>
        ) : Item(TYPE_STORAGE_USAGE, 0)

        class OfflineActionItem() : Item(TYPE_OFFLINE_ACTION, 0)
        class ScheduleOfflineActionItem() : Item(TYPE_ACTIVE_OFFLINE_DOWNLOAD, 0)
        class ClearOfflineActionItem() : Item(TYPE_CLEAR_OFFLINE_DATA, 0)
    }

    private class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v)

    private class StorageUsageViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val storageUsageView: StorageUsageView = v.findViewById(R.id.storageUsageView)
    }

    private class OfflineActionViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val button: View = v
        val title: TextView = v.findViewById(R.id.title)
        val subtitle: TextView? = v.findViewById(R.id.subtitle)
        val progressBar: ProgressBar? = v.findViewById(R.id.progressBar)
    }

    private inner class OfflineItemsAdapter(
        private val context: Context
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        private var items: List<Item> = listOf()

        private var offlineDownloadProgress = -1.0

        override fun getItemViewType(position: Int): Int =
            when (items[position]) {
                is Item.HeaderItem -> R.layout.offline_fragment_header_item
                is Item.StorageUsageItem -> R.layout.offline_fragment_storage_usage_item
                is Item.OfflineActionItem -> R.layout.offline_fragment_offline_action_item
                is Item.ScheduleOfflineActionItem -> R.layout.offline_fragment_schedule_offline_action_item
                is Item.ClearOfflineActionItem -> R.layout.offline_fragment_clear_offline_action_item
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = inflater.inflate(viewType, parent, false)

            when (viewType) {
                R.layout.offline_fragment_header_item -> return HeaderViewHolder(v)
                R.layout.offline_fragment_storage_usage_item -> return StorageUsageViewHolder(v)
                R.layout.offline_fragment_offline_action_item -> return OfflineActionViewHolder(v)
                R.layout.offline_fragment_schedule_offline_action_item -> return OfflineActionViewHolder(
                    v
                )
                R.layout.offline_fragment_clear_offline_action_item -> return OfflineActionViewHolder(
                    v
                )
                else -> throw RuntimeException()
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            when (item) {
                is Item.HeaderItem -> {
                }
                is Item.StorageUsageItem -> {
                    val h = holder as StorageUsageViewHolder
                    h.storageUsageView.setStorageUsage(item.storageUsage)
                }
                is Item.OfflineActionItem -> {
                    val h = holder as OfflineActionViewHolder
                    h.button.setOnClickListener {
                        OfflineService.startWithConfig(
                            context,
                            OfflineTaskConfig(
                                minPosts = 20,
                                roundPostsToNearestPage = true
                            )
                        )
                    }

                    val progressBar = requireNotNull(h.progressBar)
                    val subtitle = requireNotNull(h.subtitle)

                    val lastDownloadTimeMs = offlineManager.getLastSuccessfulOfflineDownloadTime()
                    if (lastDownloadTimeMs > 0) {
                        val lastDownloadRelativeTime = DateUtils.getRelativeTimeSpanString(
                            offlineManager.getLastSuccessfulOfflineDownloadTime(),
                            System.currentTimeMillis(),
                            0,
                            DateUtils.FORMAT_NUMERIC_DATE
                        )
                        subtitle.text = getString(
                            R.string.last_fetched_format,
                            lastDownloadRelativeTime
                        )
                    } else {
                        subtitle.setText(R.string.offline_data_not_downloaded_yet)
                    }

                    if (offlineDownloadProgress < 0) {
                        h.title.setText(R.string.download_offline_data)

                        progressBar.visibility = View.GONE
                        subtitle.visibility = View.VISIBLE
                    } else {
                        h.title.setText(R.string.downloading_offline_data)

                        var animate = true
                        if (progressBar.visibility != View.VISIBLE) {
                            progressBar.visibility = View.VISIBLE
                            animate = false
                        }
                        subtitle.visibility = View.INVISIBLE
                        val p = (offlineDownloadProgress * 10000).toInt()
                        progressBar.setProgressCompat(p, animate)
                    }
                }
                is Item.ScheduleOfflineActionItem -> {
                    val h = holder as OfflineActionViewHolder
                    h.button.setOnClickListener {
                        OfflineSchedulerDialogFragment.newInstance()
                            .apply {
                                setTargetFragment(this@OfflineFragment, 0)
                            }
                            .show(parentFragmentManager, "scheduler")
                    }
                }
                is Item.ClearOfflineActionItem -> {
                    val h = holder as OfflineActionViewHolder
                    h.button.setOnClickListener {
                        offlineManager.clearOfflineData()
                        refreshItems()
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun refreshItems() {
            val oldItems = items
            val colors = listOf(
                ContextCompat.getColor(context, R.color.style_pink),
                ContextCompat.getColor(context, R.color.style_amber),
                ContextCompat.getColor(context, R.color.style_blue),
                ContextCompat.getColor(context, R.color.style_green),
                ContextCompat.getColor(context, R.color.style_orange)
            )
            val newItems = listOf(
                Item.HeaderItem(),
                Item.StorageUsageItem(
                    listOf(
                        StorageUsageItem(
                            "Images",
                            Utils.getSizeOfFile(OfflineManager.instance.imagesDir),
                            colors[0]
                        ),
                        StorageUsageItem(
                            "Videos",
                            Utils.getSizeOfFile(OfflineManager.instance.videosDir),
                            colors[1]
                        ),
                        StorageUsageItem(
                            "VideosCache",
                            Utils.getSizeOfFile(OfflineManager.instance.videoCacheDir),
                            colors[2]
                        )
                    )
                ),
                Item.OfflineActionItem(),
                Item.ScheduleOfflineActionItem(),
                Item.ClearOfflineActionItem()
            )

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition].type == newItems[newItemPosition].type
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]
                    return oldItem.type == newItem.type && areItemsTheSame(
                        oldItemPosition,
                        newItemPosition
                    ) && when {
                        oldItem is Item.StorageUsageItem -> {
                            oldItem.storageUsage == (newItem as Item.StorageUsageItem).storageUsage
                        }
                        else -> true
                    }
                }

            })
            this.items = newItems
            diff.dispatchUpdatesTo(this)
        }

        fun updateOfflineDownload(newProgress: Double) {
            offlineDownloadProgress = newProgress

            if (offlineDownloadProgress == 1.0) {
                offlineDownloadProgress = -1.0
                refreshItems()
            }

            notifyItemChanged(items.indexOfFirst { it is Item.OfflineActionItem }, Unit)
        }

    }
}