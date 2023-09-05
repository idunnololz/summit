package com.idunnololz.summit.settings.postList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.FilterItemBinding
import com.idunnololz.summit.databinding.FragmentSettingsFilterListBinding
import com.idunnololz.summit.filterLists.FilterEntry
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFilterListFragment : BaseFragment<FragmentSettingsFilterListBinding>() {

    private val args by navArgs<SettingsFilterListFragmentArgs>()

    private val viewModel: SettingsFilterListViewModel by viewModels()

    @Inject
    lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(
            AddOrEditFilterDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<FilterEntry>(
                AddOrEditFilterDialogFragment.REQUEST_KEY_RESULT,
            )

            if (result != null) {
                viewModel.addFilter(result)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsFilterListBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            requireMainActivity().apply {
                setupForFragment<SettingsFragment>(animate = false)

                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayShowHomeEnabled(true)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = args.title

                insetViewAutomaticallyByPadding(viewLifecycleOwner, coordinatorLayout)
            }

            val adapter = FiltersAdapter(
                onEditClick = {
                    AddOrEditFilterDialogFragment
                        .newInstance(
                            it,
                        )
                        .show(childFragmentManager, "dddssf")
                },
                onDeleteClick = {
                    viewModel.deleteFilter(it)
                },
            )

            viewModel.filters.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        if (it.data.isEmpty()) {
                            loadingView.showErrorText(R.string.there_doesnt_seem_to_be_anything_here)
                        }

                        adapter.data = it.data
                    }
                }
            }

            fab.setOnClickListener {
                AddOrEditFilterDialogFragment
                    .newInstance(
                        FilterEntry(0, args.contentType, args.filterType, "", false),
                    )
                    .show(childFragmentManager, "sadsaad")
            }
            binding.fab.setup(preferences)

            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)

            viewModel.getFilters(args.contentType, args.filterType)
        }
    }

    private class FiltersAdapter(
        private val onEditClick: (FilterEntry) -> Unit,
        private val onDeleteClick: (FilterEntry) -> Unit,
    ) : Adapter<RecyclerView.ViewHolder>() {

        sealed interface Item {
            data class FilterItem(
                val filter: FilterEntry,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.FilterItem ->
                        old.filter.id == (new as Item.FilterItem).filter.id
                }
            },
        ).apply {
            this.addItemType(Item.FilterItem::class, FilterItemBinding::inflate) { item, b, h ->
                b.icon.setImageResource(
                    if (item.filter.isRegex) {
                        R.drawable.ic_regex_24
                    } else {
                        R.drawable.baseline_text_fields_24
                    },
                )
                b.title.text = item.filter.filter
                b.edit.setOnClickListener {
                    onEditClick(item.filter)
                }
                b.delete.setOnClickListener {
                    onDeleteClick(item.filter)
                }
            }
        }

        var data: List<FilterEntry> = listOf()
            set(value) {
                field = value
                refreshItems()
            }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            val newItems = data.map { Item.FilterItem(it) }
            adapterHelper.setItems(newItems, this)
        }
    }
}
