package com.idunnololz.summit.drafts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentDraftsBinding
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DraftsDialogFragment :
    BaseDialogFragment<DialogFragmentDraftsBinding>(),
    FullscreenDialogFragment,
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

    @Inject
    lateinit var animationsHelper: AnimationsHelper

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
                        it.setIcon(R.drawable.baseline_filter_list_off_24)
                    } else {
                        viewModel.draftType = null
                        it.setIcon(R.drawable.baseline_filter_list_24)
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
                                is DraftData.MessageDraftData -> null
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
                                is DraftData.MessageDraftData -> null
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
                    AlertDialogFragment.Builder()
                        .setMessage(R.string.warn_delete_draft)
                        .setPositiveButton(R.string.delete)
                        .setNegativeButton(R.string.cancel)
                        .setExtra("draft_id", it.id.toString())
                        .createAndShow(childFragmentManager, "delete")
                },
            )
            val layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = layoutManager
            recyclerView.setup(animationsHelper)
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

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "delete_all") {
            viewModel.deleteAll(args.draftType)
        } else if (tag == "delete") {
            dialog.getExtra("draft_id")?.toLong()?.let { draftId ->
                viewModel.deleteDraft(draftId)
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}
