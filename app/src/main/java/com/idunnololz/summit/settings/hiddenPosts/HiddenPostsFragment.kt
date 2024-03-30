package com.idunnololz.summit.settings.hiddenPosts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.actions.ui.ActionsTabbedFragment
import com.idunnololz.summit.databinding.ActionsItemEmptyBinding
import com.idunnololz.summit.databinding.FragmentHiddenPostsBinding
import com.idunnololz.summit.databinding.HiddenPostsItemBinding
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.insetViewAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@AndroidEntryPoint
class HiddenPostsFragment : BaseFragment<FragmentHiddenPostsBinding>() {

    companion object {
        private const val ONE_DAY_MS = 1000 * 60 * 60 * 24
    }

    private val viewModel: HiddenPostsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentHiddenPostsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<ActionsTabbedFragment>()

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.hidden_posts)

            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.root)
        }

        with(binding) {
            val adapter = HiddenPostsAdapter(
                context,
                removeHiddenPost = {
                    viewModel.removeHiddenPost(it)
                },
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
            )

            viewModel.hiddenPosts.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.showDefaultErrorMessageFor(it.error)
                        swipeRefreshLayout.isRefreshing = false
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                        swipeRefreshLayout.isRefreshing = false

                        adapter.hiddenPosts = it.data
                    }
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadHiddenPosts()
            }

            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.addItemDecoration(
                CustomDividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL,
                ).apply {
                    setDrawable(
                        checkNotNull(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.vertical_divider,
                            ),
                        ),
                    )
                },
            )
            fastScroller.setRecyclerView(binding.recyclerView)
        }
    }

    private class HiddenPostsAdapter(
        private val context: Context,
        private val removeHiddenPost: (Long) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data class HiddenPostItem(
                val hiddenPost: HiddenPostsManager.HiddenPost,
            ) : Item

            data object EmptyItem : Item
        }

        var hiddenPosts: List<HiddenPostsManager.HiddenPost> = listOf()
            set(value) {
                field = value

                refreshItems()
            }

        private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.HiddenPostItem ->
                        old.hiddenPost.id == (new as Item.HiddenPostItem).hiddenPost.id

                    Item.EmptyItem -> true
                }
            },
        ).apply {
            addItemType(Item.HiddenPostItem::class, HiddenPostsItemBinding::inflate) { item, b, h ->
                val hiddenPost = item.hiddenPost

                b.timestamp.text = if (System.currentTimeMillis() - hiddenPost.ts < ONE_DAY_MS * 7) {
                    dateStringToPretty(context, hiddenPost.ts)
                } else {
                    dateFormat.format(Instant.ofEpochMilli(hiddenPost.ts).atZone(ZoneId.systemDefault()).toLocalDate())
                }
                b.text.text = Utils.fromHtml(
                    context.getString(
                        R.string.post_hidden_format,
                        hiddenPost.hiddenPostId.toString(),
                        hiddenPost.instance,
                    ),
                )

                b.root.setOnClickListener {
                    onPageClick(PostRef(hiddenPost.instance, hiddenPost.hiddenPostId))
                }
                b.root.setOnLongClickListener {
                    PopupMenu(context, it)
                        .apply {
                            inflate(R.menu.menu_hidden_posts)

                            setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.remove -> {
                                        removeHiddenPost(item.hiddenPost.id)
                                    }

                                    else -> {}
                                }

                                true
                            }
                        }
                        .show()

                    true
                }
            }
            addItemType(Item.EmptyItem::class, ActionsItemEmptyBinding::inflate) { item, b, h ->
                b.text.setText(R.string.there_doesnt_seem_to_be_anything_here)
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount

        private fun refreshItems(cb: (() -> Unit)? = null) {
            val newItems = mutableListOf<Item>()

            if (hiddenPosts.isNotEmpty()) {
                hiddenPosts.mapTo(newItems) { Item.HiddenPostItem(it) }
            } else {
                newItems += Item.EmptyItem
            }

            adapterHelper.setItems(newItems, this, cb)
        }
    }
}
