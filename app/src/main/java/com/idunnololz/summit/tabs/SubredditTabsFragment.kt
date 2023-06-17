package com.idunnololz.summit.tabs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.card.MaterialCardView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FragmentSubredditTabsBinding
import com.idunnololz.summit.main.MainActivityViewModel
import com.idunnololz.summit.main.SubredditSelectorController
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.GridAutofitLayoutManager
import com.idunnololz.summit.util.GridSpaceItemDecoration
import com.idunnololz.summit.util.Utils
import java.io.File

class SubredditTabsFragment : BaseFragment<FragmentSubredditTabsBinding>() {

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    private val tabsManager = TabsManager.instance

    private var subredditSelectorController: SubredditSelectorController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        requireMainActivity().apply {
            setupForFragment<SubredditTabsFragment>()
        }

        setBinding(FragmentSubredditTabsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireMainActivity().apply {
            insetRootViewAutomatically(viewLifecycleOwner, view)
        }

        val context = requireContext()

        val adapter = SubredditTabsAdapter(context)

        binding.recyclerView.layoutManager =
            GridAutofitLayoutManager(context, Utils.convertDpToPixel(180f).toInt())
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            GridSpaceItemDecoration(
                Utils.convertDpToPixel(16f).toInt(), true, true
            )
        )

        binding.newTabButton.setOnClickListener {
            requireMainActivity().showSubredditSelector().apply {
                show()

                mainActivityViewModel.subredditsLiveData.value?.let {
                    setSubredditsState(it)
                }

                onSubredditSelectedListener = { _, url ->
                    TODO()
//                    val tabId = tabsManager.addNewTab(
//                        TabItem.PageTabItem.newTabItem(tabId = "", url = url)
//                    )
//
//                    Utils.hideKeyboard(activity)
//                    switchTabs(tabId)
//                    hide()
                }
            }
        }

        tabsManager.currentTabId.observe(viewLifecycleOwner, Observer {
            adapter.setCurrentTabId(it)
        })
        tabsManager.tabsChangedLiveData.observe(viewLifecycleOwner, Observer {
            adapter.setTabs(tabsManager.getAllTabs())
        })
    }

    private fun switchTabs(tabId: String) {
        if (tabsManager.currentTabId.value != tabId) {
            tabsManager.currentTabId.value = tabId
        }

        requireMainActivity().onBackPressed(force = true)
    }

    private data class TabViewItem(
        val title: String,
        val tabId: String,
        val thumbnailPath: String?,
        val thumbnailSignature: String?
    )

    private class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val cardView: MaterialCardView = view.findViewById(R.id.cardView)
        val closeButton: ImageButton = view.findViewById(R.id.closeButton)
    }

    private inner class SubredditTabsAdapter(
        private val context: Context
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val inflater = LayoutInflater.from(context)

        private var items: List<TabViewItem> = listOf()
        private var currentTabId: String? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = inflater.inflate(R.layout.subreddit_tab_item, parent, false)
            return TabViewHolder(v)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val h = holder as TabViewHolder
            val item = items[position]

            h.title.text = item.title
            h.cardView.setOnClickListener {
                switchTabs(item.tabId)
            }
            if (item.tabId == TabsManager.FIRST_FRAGMENT_TAB_ID) {
                h.closeButton.isClickable = false
                h.closeButton.setImageResource(R.drawable.baseline_home_black_24)
            } else {
                h.closeButton.setOnClickListener {
                    tabsManager.closeTab(item.tabId)
                    h.closeButton.setImageResource(R.drawable.baseline_close_black_18)
                }
            }

            item.thumbnailPath?.let {
                Glide.with(this@SubredditTabsFragment)
                    .load(File(it))
                    .centerCrop()
                    .signature(ObjectKey(item.thumbnailSignature ?: ""))
                    .into(h.thumbnail)
            }

            if (currentTabId == item.tabId) {
                h.cardView.setStrokeColor(
                    ContextCompat.getColorStateList(
                        context,
                        R.color.colorAccent
                    )
                )
                h.cardView.setContentPadding(0, 0, 0, 0)
            } else {
                h.cardView.setStrokeColor(
                    ContextCompat.getColorStateList(
                        context,
                        R.color.colorSurface
                    )
                )
                h.cardView.setContentPadding(0, 0, 0, 0)
            }
        }

        override fun getItemCount(): Int = items.size

        fun setCurrentTabId(tabId: String?) {
            currentTabId = tabId
            notifyItemChanged(items.indexOfFirst { it.tabId == currentTabId })
        }

        fun setTabs(allTabs: List<TabItem>) {
            val newItems = TODO()
//                allTabs.map {
//                when (it) {
//                    is TabItem.PageTabItem -> {
//                        TabViewItem(
//                            it.pageDetails.url,
//                            it.tabId,
//                            it.previewPath,
//                            it.previewSignature
//                        )
//                    }
//                }
//            }
//            val oldItems = items
//
//            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
//                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//                    val oldItem = oldItems[oldItemPosition]
//                    val newItem = newItems[newItemPosition]
//
//                    return oldItem.tabId == newItem.tabId
//                }
//
//                override fun getOldListSize(): Int = oldItems.size
//
//                override fun getNewListSize(): Int = newItems.size
//
//                override fun areContentsTheSame(
//                    oldItemPosition: Int,
//                    newItemPosition: Int
//                ): Boolean {
//                    val oldItem = oldItems[oldItemPosition]
//                    val newItem = newItems[newItemPosition]
//                    return areItemsTheSame(oldItemPosition, newItemPosition) && oldItem == newItem
//                }
//
//            })
//            this.items = newItems
//            diff.dispatchUpdatesTo(this)
        }
    }
}