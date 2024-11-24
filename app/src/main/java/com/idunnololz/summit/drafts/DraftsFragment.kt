package com.idunnololz.summit.drafts

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.DialogFragmentDraftsBinding
import com.idunnololz.summit.databinding.FragmentDraftsBinding
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class DraftsFragment :
    BaseFragment<FragmentDraftsBinding>(),
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

    private val args by navArgs<DraftsFragmentArgs>()

    private val viewModel: DraftsViewModel by viewModels()

    @Inject
    lateinit var draftsManager: DraftsManager

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentDraftsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        viewModel.draftType = args.draftType

        val parentFragment = parentFragment as DraftsTabbedFragment

        with(binding) {
            val adapter = DraftsAdapter(
                onDraftClick = {
                    openDraft(it)
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
                swipeRefreshLayout.isRefreshing = false

                adapter.setItems(it) {
                    checkIfFetchNeeded()
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

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadMoreDrafts(force = true)
            }
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

    fun onSelected() {
        (parentFragment as? DraftsTabbedFragment)?.binding?.fab?.apply {
            setOnClickListener {
                val currentAccount = viewModel.currentAccount ?: return@setOnClickListener

                viewLifecycleOwner.lifecycleScope.launch {
                    val draftData = when (viewModel.draftType) {
                        DraftTypes.Post ->
                            DraftData.PostDraftData(
                                    null,
                                    null,
                                    null,
                                    null,
                                    false,
                                    currentAccount.id,
                                    currentAccount.instance,
                                    "",
                                )
                        DraftTypes.Comment ->
                            DraftData.CommentDraftData(
                                originalComment = null,
                                postRef = null,
                                parentCommentId = null,
                                content = "",
                                accountId = currentAccount.id,
                                accountInstance = currentAccount.instance,
                            )
                        else -> return@launch
                    }

                    val id = draftsManager.saveDraft(
                        draftData = draftData,
                        showToast = false,
                    )
                    val draftEntry = draftsManager.getDraft(id)

                    draftEntry.firstOrNull()?.let {
                        openDraft(it)
                    }
                }
            }
        }
    }

    private fun openDraft(
        draftEntry: DraftEntry,
    ) {
        when (draftEntry.data) {
            is DraftData.CommentDraftData -> {
                AddOrEditCommentFragment()
                    .apply {
                        arguments =
                            AddOrEditCommentFragmentArgs(
                                instance = viewModel.apiInstance,
                                commentView = null,
                                postView = null,
                                editCommentView = null,
                                draft = draftEntry,
                            ).toBundle()
                    }
                    .showAllowingStateLoss(
                        childFragmentManager,
                        "AddOrEditCommentFragment"
                    )
            }
            is DraftData.PostDraftData -> {
                CreateOrEditPostFragment()
                    .apply {
                        arguments =
                            CreateOrEditPostFragmentArgs(
                                instance = viewModel.apiInstance,
                                communityName = draftEntry.data.targetCommunityFullName,
                                draft = draftEntry,
                            ).toBundle()
                    }
                    .showAllowingStateLoss(
                        childFragmentManager,
                        "CreateOrEditPostFragment"
                    )
            }
            is DraftData.MessageDraftData -> {}
            null -> {}
        }
    }
}
