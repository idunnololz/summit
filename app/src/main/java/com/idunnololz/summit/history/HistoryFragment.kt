package com.idunnololz.summit.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentHistoryBinding
import com.idunnololz.summit.lemmy.CommunityViewState
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.reddit.RedditUtils
import com.idunnololz.summit.tabs.TabCommunityState
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.moshi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : BaseFragment<FragmentHistoryBinding>() {

    companion object {
        private const val TAG = "HistoryFragment"
    }

    private var historyViewModel: HistoryViewModel? = null

    private lateinit var adapter: HistoryEntryAdapter

    private val historyManager = HistoryManager.instance

    private val disposables = CompositeDisposable()

    private val onHistoryChangedListener = object : HistoryManager.OnHistoryChangedListener {
        override fun onHistoryChanged() {
            historyViewModel?.loadHistory()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        requireMainActivity().apply {
            setupForFragment<HistoryFragment>()
        }

        setBinding(FragmentHistoryBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated()")
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        adapter = HistoryEntryAdapter(context)

        requireMainActivity().insetRootViewAutomatically(viewLifecycleOwner, view)

        historyViewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)

        historyManager.registerOnHistoryChangedListener(onHistoryChangedListener)

        historyViewModel?.loadHistory()
        historyViewModel?.historyEntriesLiveData?.observe(viewLifecycleOwner, Observer {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingView.hideAll()
                    binding.swipeRefreshLayout.isRefreshing = false

                    adapter.setItems(it.data)
                }
            }
        })
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.swipeRefreshLayout.setOnRefreshListener {
            historyViewModel?.loadHistory()
        }

        requireMainActivity().let {
            val toolbar = it.binding.toolbar
            it.windowInsets.observe(viewLifecycleOwner) {
                binding.rootView.setPadding(
                    binding.rootView.paddingLeft,
                    toolbar.layoutParams.height,
                    binding.rootView.paddingRight,
                    binding.rootView.paddingBottom
                )
            }
            it.setupActionBar(getString(R.string.history), false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_fragment_history, menu)

        val searchView: SearchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    adapter.setQuery(newText)
                }

                return true
            }

        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                return true
            }
            R.id.clear_history -> {
                historyManager.clearHistory()
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        historyManager.unregisterOnHistoryChangedListener(onHistoryChangedListener)
        super.onDestroyView()
    }

    private fun handleEntryClicked(data: LiteHistoryEntry) {
        when (data.type) {
            HistoryEntry.TYPE_PAGE_VISIT -> {
                startActivity(
                    Intent(context, MainActivity::class.java)
                        .setData(Uri.parse(data.url))
                )
            }
            HistoryEntry.TYPE_SUBREDDIT_STATE -> {
                val d = historyManager.getHistoryEntry(data.id)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it == null) {
                            AlertDialogFragment.Builder()
                                .setMessage(R.string.error_history_entry_deleted)
                                .createAndShow(this@HistoryFragment, "asdf")
                        } else {
                            val state = moshi.adapter(TabCommunityState::class.java).fromJson(it.extras)
                            requireMainActivity().restoreTabState(state)
                        }
                    }, {
                        AlertDialogFragment.Builder()
                            .setMessage(R.string.error_history_entry_corrupt)
                            .createAndShow(this@HistoryFragment, "asdf")
                    })
                disposables.add(d)
            }
        }
    }

    private sealed class Item {
        abstract val headerId: Int

        data class HistoryItem(
            val sharableUrl: String,
            val data: LiteHistoryEntry,
            override val headerId: Int
        ) : Item()

        data class HeaderItem(override val headerId: Int, val date: Date) : Item()
    }

    private class HistoryItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val body: TextView = v.findViewById(R.id.body)
        val removeButton: ImageButton = v.findViewById(R.id.removeButton)
    }

    private class HeaderItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
    }

    private inner class HistoryEntryAdapter(
        context: Context
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var data: List<LiteHistoryEntry> = listOf()

        private var items: List<Item> = listOf()

        private val inflater = LayoutInflater.from(context)

        private val dateFormatter = SimpleDateFormat.getDateInstance()

        private var query: String = ""

        override fun getItemViewType(position: Int): Int = when (items[position]) {
            is Item.HistoryItem -> R.layout.history_entry_item
            is Item.HeaderItem -> R.layout.history_header_item
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = inflater.inflate(viewType, parent, false)
            return when (viewType) {
                R.layout.history_entry_item -> {
                    HistoryItemViewHolder(v)
                }
                R.layout.history_header_item -> {
                    HeaderItemViewHolder(v)
                }
                else -> throw RuntimeException("Unknown view type $viewType")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            when (item) {
                is Item.HistoryItem -> {
                    val h = holder as HistoryItemViewHolder
                    h.title.text = item.data.shortDesc
                    h.body.text = item.sharableUrl

                    h.removeButton.setOnClickListener {
                        historyManager.removeEntry(item.data.id)
                    }

                    h.itemView.setOnClickListener {
                        handleEntryClicked(item.data)
                    }
                }
                is Item.HeaderItem -> {
                    val h = holder as HeaderItemViewHolder
                    h.title.text = dateFormatter.format(item.date)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun setItems(newData: List<LiteHistoryEntry>) {
            data = newData

            refreshItems()
        }

        fun setQuery(newText: String) {
            query = newText

            refreshItems()
        }

        private fun refreshItems() {
            val oldItems = items
            val newItems = mutableListOf<Item>()
            val lastDate: Calendar = Calendar.getInstance()
            lastDate.timeInMillis = 0
            var headerId = 0

            val calendar = Calendar.getInstance()

            data.let {
                if (query.isBlank()) {
                    data
                } else {
                    data.filter { it.url.contains(query) || it.shortDesc.contains(query) }
                }
            }.forEach {
                calendar.timeInMillis = it.ts
                if (calendar.get(Calendar.YEAR) != lastDate.get(Calendar.YEAR) ||
                    calendar.get(Calendar.DAY_OF_YEAR) != lastDate.get(Calendar.DAY_OF_YEAR)
                ) {
                    lastDate.set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                    lastDate.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR))

                    val headerItem = Item.HeaderItem(++headerId, lastDate.time)

                    newItems.add(headerItem)
                }
                newItems.add(
                    Item.HistoryItem(
                        sharableUrl = RedditUtils.convertRedditUrl(
                            it.url,
                            desiredFormat = "",
                            sharable = true
                        ),
                        data = it,
                        headerId = headerId
                    )
                )
            }

            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]
                    if (oldItem::class != newItem::class) {
                        return false
                    }
                    return when (oldItem) {
                        is Item.HistoryItem -> {
                            oldItem.data.id == (newItem as Item.HistoryItem).data.id
                        }
                        is Item.HeaderItem -> {
                            oldItem.date == (newItem as Item.HeaderItem).date
                        }
                    }
                }

                override fun getOldListSize(): Int = oldItems.size

                override fun getNewListSize(): Int = newItems.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = oldItems[oldItemPosition]
                    val newItem = newItems[newItemPosition]
                    return true
                }

            })

            this.items = newItems
            diff.dispatchUpdatesTo(this)
            notifyItemChanged(0, Unit)
        }
    }
}