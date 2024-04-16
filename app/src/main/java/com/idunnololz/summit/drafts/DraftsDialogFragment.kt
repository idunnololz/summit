package com.idunnololz.summit.drafts

import android.graphics.Typeface
import android.os.Bundle
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.buildSpannedString
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.CommentDraftItemBinding
import com.idunnololz.summit.databinding.DialogFragmentDraftsBinding
import com.idunnololz.summit.databinding.DraftLoadingItemBinding
import com.idunnololz.summit.databinding.EmptyDraftItemBinding
import com.idunnololz.summit.databinding.PostDraftItemBinding
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DraftsDialogFragment :
    BaseDialogFragment<DialogFragmentDraftsBinding>(),
    FullscreenDialogFragment,
    BackPressHandler,
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        const val REQUEST_KEY = "DraftsDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        fun show(fragmentManager: FragmentManager, draftType: Int) {
            DraftsDialogFragment().apply {
                arguments = DraftsDialogFragmentArgs(draftType).toBundle()
            }.showAllowingStateLoss(fragmentManager, "DraftsDialogFragment")
        }
    }

    private val args by navArgs<DraftsDialogFragmentArgs>()

    private val viewModel: DraftsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentDraftsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        viewModel.draftType = args.draftType

        binding.toolbar.title = getString(R.string.drafts)
        binding.toolbar.setNavigationIcon(R.drawable.baseline_close_24)
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        binding.toolbar.setNavigationIconTint(
            context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
        )
        binding.toolbar.inflateMenu(R.menu.menu_drafts)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.delete_all -> {
                    AlertDialogFragment.Builder()
                        .setMessage(R.string.warn_delete_all_drafts)
                        .setPositiveButton(R.string.delete_all)
                        .setNegativeButton(R.string.cancel)
                        .createAndShow(childFragmentManager, "delete_all")
                    true
                }
                R.id.show_all_drafts -> {
                    if (viewModel.draftType == null) {
                        viewModel.draftType = args.draftType
                    } else {
                        viewModel.draftType = null
                    }
                    viewModel.loadMoreDrafts(force = true)
                    true
                }
                else -> false
            }
        }

        with(binding) {
            val adapter = DraftsAdapter(
                onDraftClick = {
                    // Convert the draft data to the correct type (eg. if comment draft was requested
                    // but the draft selected was a post then convert the post to a comment)
                    val draft = when (args.draftType) {
                        DraftTypes.Post -> {
                            when (it.data) {
                                is DraftData.CommentDraftData ->
                                    it.copy(
                                        id = 0L, // prevent overwriting the original
                                        data = DraftData.PostDraftData(
                                            originalPost = null,
                                            name = null,
                                            body = it.data.content,
                                            url = null,
                                            isNsfw = false,
                                            accountId = it.data.accountId,
                                            accountInstance = it.data.accountInstance,
                                            targetCommunityFullName = "",
                                        ),
                                    )
                                is DraftData.PostDraftData -> it
                                null -> null
                            }
                        }
                        DraftTypes.Comment -> {
                            when (it.data) {
                                is DraftData.CommentDraftData -> it
                                is DraftData.PostDraftData ->
                                    it.copy(
                                        id = 0L, // prevent overwriting the original
                                        data = DraftData.CommentDraftData(
                                            originalComment = null,
                                            postRef = null,
                                            parentCommentId = null,
                                            content = it.data.body ?: "",
                                            accountId = it.data.accountId,
                                            accountInstance = it.data.accountInstance,
                                        ),
                                    )
                                null -> null
                            }
                        }
                        else -> null
                    }

                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(REQUEST_KEY_RESULT to draft),
                    )
                    dismiss()
                },
                onDeleteClick = {
                    viewModel.deleteDraft(it)
                },
            )
            val layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = layoutManager
            recyclerView.setHasFixedSize(true)

            fun fetchPageIfLoadItem(position: Int) {
                (adapter.items.getOrNull(position) as? DraftsViewModel.ViewModelItem.LoadingItem)
                    ?.let {
                        viewModel.loadMoreDrafts()
                    }
            }

            fun checkIfFetchNeeded() {
                val firstPos = layoutManager.findFirstVisibleItemPosition()
                val lastPos = layoutManager.findLastVisibleItemPosition()

                fetchPageIfLoadItem(firstPos)
                fetchPageIfLoadItem(firstPos - 1)
                fetchPageIfLoadItem(lastPos)
                fetchPageIfLoadItem(lastPos + 1)
            }

            viewModel.viewModelItems.observe(viewLifecycleOwner) {
                val wasAtTop = layoutManager.findFirstVisibleItemPosition() == 0

                adapter.setItems(it) {
                    checkIfFetchNeeded()

                    if (wasAtTop) {
                        layoutManager.scrollToPosition(0)
                    }
                }
            }

            recyclerView.addItemDecoration(
                CustomDividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL,
                    dividerAfterLastItem = false,
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

            recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        checkIfFetchNeeded()
                    }
                },
            )
        }
    }

    override fun onBackPressed(): Boolean {
        dismiss()
        return true
    }

    private class DraftsAdapter(
        private val onDraftClick: (DraftEntry) -> Unit,
        private val onDeleteClick: (DraftEntry) -> Unit,
    ) : Adapter<ViewHolder>() {

        var items: List<DraftsViewModel.ViewModelItem> = listOf()
            private set

        private val adapterHelper = AdapterHelper<DraftsViewModel.ViewModelItem>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is DraftsViewModel.ViewModelItem.PostDraftItem -> {
                        old.draftEntry.id ==
                            (new as DraftsViewModel.ViewModelItem.PostDraftItem).draftEntry.id
                    }
                    is DraftsViewModel.ViewModelItem.CommentDraftItem -> {
                        old.draftEntry.id ==
                            (new as DraftsViewModel.ViewModelItem.CommentDraftItem).draftEntry.id
                    }
                    DraftsViewModel.ViewModelItem.LoadingItem -> true
                    DraftsViewModel.ViewModelItem.EmptyItem -> true
                }
            },
        ).apply {
            addItemType(
                clazz = DraftsViewModel.ViewModelItem.PostDraftItem::class,
                inflateFn = PostDraftItemBinding::inflate,
            ) { item, b, h ->
                b.title.text = if (item.postData.name.isNullOrBlank()) {
                    buildSpannedString {
                        append(b.title.context.getString(R.string.empty))
                        setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
                    }
                } else {
                    item.postData.name
                }
                b.text.text = if (item.postData.body.isNullOrBlank()) {
                    buildSpannedString {
                        append(b.title.context.getString(R.string.empty))
                        setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
                    }
                } else {
                    item.postData.body
                }

                b.delete.setOnClickListener {
                    onDeleteClick(item.draftEntry)
                }
                b.root.setOnClickListener {
                    onDraftClick(item.draftEntry)
                }
            }

            addItemType(
                clazz = DraftsViewModel.ViewModelItem.CommentDraftItem::class,
                inflateFn = CommentDraftItemBinding::inflate,
            ) { item, b, h ->
                b.text.text = item.commentData.content

                b.delete.setOnClickListener {
                    onDeleteClick(item.draftEntry)
                }
                b.root.setOnClickListener {
                    onDraftClick(item.draftEntry)
                }
            }
            addItemType(
                clazz = DraftsViewModel.ViewModelItem.LoadingItem::class,
                inflateFn = DraftLoadingItemBinding::inflate,
            ) { item, b, h ->
                b.loadingView.showProgressBar()
            }
            addItemType(
                clazz = DraftsViewModel.ViewModelItem.EmptyItem::class,
                inflateFn = EmptyDraftItemBinding::inflate,
            ) { item, b, h -> }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setItems(items: List<DraftsViewModel.ViewModelItem>, cb: () -> Unit) {
            this.items = items

            refreshItems(cb)
        }

        private fun refreshItems(cb: () -> Unit) {
            adapterHelper.setItems(items, this, cb)
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "delete_all") {
            viewModel.deleteAll(args.draftType)
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}
