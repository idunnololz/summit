package com.idunnololz.summit.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentHistoryBinding
import com.idunnololz.summit.databinding.HistoryEntryItemBinding
import com.idunnololz.summit.databinding.HistoryHeaderItemBinding
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.LinkResolver
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment :
    BaseFragment<FragmentHistoryBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private const val TAG = "HistoryFragment"
    }

    private val viewModel: HistoryViewModel by viewModels()

    private lateinit var adapter: HistoryEntryAdapter

    @Inject
    lateinit var historyManager: HistoryManager

    private val onHistoryChangedListener = object : HistoryManager.OnHistoryChangedListener {
        override fun onHistoryChanged() {
            viewModel.loadHistory(force = true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

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

        adapter = HistoryEntryAdapter(
            removeEntry = {
                viewModel.removeEntry(it)
            },
            onEntryClick = {
                val pageRef = LinkResolver.parseUrl(it.url, viewModel.instance, mustHandle = true)
                if (pageRef == null) {
                    AlertDialogFragment.Builder()
                        .setMessage(R.string.error_history_entry_corrupt)
                        .createAndShow(this@HistoryFragment, "asdf")
                } else {
                    requireMainActivity().launchPage(pageRef)
                }
            },
        )

        requireMainActivity().apply {
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.history)

            binding.contentContainer.updatePadding(bottom = getBottomNavHeight())
        }

        historyManager.registerOnHistoryChangedListener(onHistoryChangedListener)

        viewModel.loadHistory()
        viewModel.historyData.observe(viewLifecycleOwner) {
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
        }
        viewModel.historyQueryData.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {}
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    adapter.queryResults = it.data
                }
            }
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.fastScroller.setRecyclerView(binding.recyclerView)

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadHistory(force = true)
        }

        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_fragment_history, menu)

                    val searchView: SearchView = menu.findItem(R.id.search).actionView as SearchView
                    searchView.setOnQueryTextListener(
                        object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String?): Boolean {
                                return true
                            }

                            override fun onQueryTextChange(newText: String?): Boolean {
                                if (newText != null) {
                                    adapter.setQuery(newText)
                                    viewModel.query(newText)
                                }

                                return true
                            }
                        },
                    )
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.search -> {
                            true
                        }
                        R.id.clear_history -> {
                            viewModel.clearHistory()
                            true
                        }
                        else ->
                            false
                    }
            },
        )
    }

    override fun onDestroyView() {
        historyManager.unregisterOnHistoryChangedListener(onHistoryChangedListener)
        super.onDestroyView()
    }

    private class HistoryEntryAdapter(
        private val removeEntry: (Long) -> Unit,
        private val onEntryClick: (LiteHistoryEntry) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private sealed interface Item {
            val headerId: Int

            data class HistoryItem(
                val sharableUrl: String,
                val data: LiteHistoryEntry,
                override val headerId: Int,
            ) : Item

            data class HeaderItem(
                override val headerId: Int,
                val date: Date,
            ) : Item
        }

        private var data: HistoryViewModel.HistoryEntryData? = null

        private var query: String = ""

        private val dateFormatter = SimpleDateFormat.getDateInstance()

        var queryResults: HistoryViewModel.HistoryQueryResult? = null
            set(value) {
                field = value

                refreshItems()
            }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                if (old::class != new::class) {
                    return@AdapterHelper false
                }

                when (old) {
                    is Item.HistoryItem -> {
                        old.data.id == (new as Item.HistoryItem).data.id
                    }
                    is Item.HeaderItem -> {
                        old.date == (new as Item.HeaderItem).date
                    }
                }
            },
        ).apply {
            addItemType(Item.HeaderItem::class, HistoryHeaderItemBinding::inflate) { item, b, _ ->
                b.title.text = dateFormatter.format(item.date)
            }
            addItemType(Item.HistoryItem::class, HistoryEntryItemBinding::inflate) { item, b, h ->
                b.title.text = item.data.shortDesc
                b.body.text = item.sharableUrl

                b.removeButton.setOnClickListener {
                    removeEntry(item.data.id)
                }

                h.itemView.setOnClickListener {
                    onEntryClick(item.data)
                }
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount

        fun setItems(newData: HistoryViewModel.HistoryEntryData) {
            data = newData

            refreshItems()
        }

        fun setQuery(newText: String) {
            query = newText

            refreshItems()
        }

        private fun refreshItems() {
            val data = data ?: return
            val newItems = mutableListOf<Item>()
            val lastDate: Calendar = Calendar.getInstance()
            val query = query
            val queryResults = queryResults

            lastDate.timeInMillis = 0
            var headerId = 0

            val calendar = Calendar.getInstance()

            fun add(it: LiteHistoryEntry) {
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
                        sharableUrl = LemmyUtils.convertRedditUrl(
                            it.url,
                            desiredFormat = "",
                            sharable = true,
                        ),
                        data = it,
                        headerId = headerId,
                    ),
                )
            }

            if (query.isNotBlank()) {
                if (query == queryResults?.query) {
                    queryResults.sortedEntries.forEach {
                        add(it)
                    }
                }
            } else {
                data.sortedEntries.forEach {
                    add(it)
                }
            }

            adapterHelper.setItems(newItems, this)
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}
