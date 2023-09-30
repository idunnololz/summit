package com.idunnololz.summit.lemmy.mod

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.DialogFragmentModActionsBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.mod.ModActionsViewModel.ModState.CommentModState
import com.idunnololz.summit.lemmy.mod.ModActionsViewModel.ModState.CommunityModState
import com.idunnololz.summit.lemmy.mod.ModActionsViewModel.ModState.PostModState
import com.idunnololz.summit.lemmy.mod.ModActionsViewModel.ModState.UserModState
import com.idunnololz.summit.util.BaseBottomSheetDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ModActionsDialogFragment :
    BaseBottomSheetDialogFragment<DialogFragmentModActionsBinding>(),
    FullscreenDialogFragment {

    companion object {

        const val REQUEST_KEY = "ModActionsDialogFragment_req"
        const val RESULT_UPDATED_OBJ = "RESULT_UPDATED_OBJ"

        fun show(
            postView: PostView,
            fragmentManager: FragmentManager,
        ) {
            show(
                communityId = postView.community.id,
                commentId = -1,
                postId = postView.post.id,
                personId = postView.creator.id,
                fragmentManager = fragmentManager,
            )
        }

        fun show(
            commentView: CommentView,
            fragmentManager: FragmentManager,
        ) {
            show(
                communityId = commentView.community.id,
                commentId = commentView.comment.id,
                postId = -1,
                personId = commentView.creator.id,
                fragmentManager = fragmentManager,
            )
        }

        fun show(
            communityId: Int,
            commentId: Int,
            postId: Int,
            personId: Int,
            fragmentManager: FragmentManager,
        ) {
            ModActionsDialogFragment()
                .apply {
                    arguments = ModActionsDialogFragmentArgs(
                        communityId,
                        commentId,
                        postId,
                        personId,
                    ).toBundle()
                }
                .show(fragmentManager, "ModActionsDialogFragment")
        }
    }

    sealed interface UpdatedObject : Parcelable {
        @Parcelize
        data class PostObject(
            val postId: Int,
        ) : UpdatedObject

        @Parcelize
        data class CommentObject(
            val commentId: Int,
        ) : UpdatedObject
    }

    private val args by navArgs<ModActionsDialogFragmentArgs>()

    private val viewModel: ModActionsViewModel by viewModels()
    private val actionsViewModel: MoreActionsViewModel by viewModels()

    private var adapter: BottomMenu.BottomMenuAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentModActionsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        with(binding) {
            adapter = BottomMenu.BottomMenuAdapter(context).apply {
                title = getString(R.string.mod_actions)
                onMenuItemClickListener = {
                    when (it.id) {
                        R.id.feature_post -> {
                            actionsViewModel.featurePost(args.postId, true)
                        }
                        R.id.remove_feature_post -> {
                            actionsViewModel.featurePost(args.postId, false)
                        }
                        R.id.lock_post -> {
                            actionsViewModel.lockPost(args.postId, true)
                        }
                        R.id.unlock_post -> {
                            actionsViewModel.lockPost(args.postId, false)
                        }
                        R.id.remove_post -> {
                            actionsViewModel.removePost(args.postId, true)
                        }
                        R.id.undo_remove_post -> {
                            actionsViewModel.removePost(args.postId, false)
                        }
                        R.id.mod -> {
                            actionsViewModel.mod(args.communityId, args.personId, true)
                        }
                        R.id.unmod -> {
                            actionsViewModel.mod(args.communityId, args.personId, false)
                        }
                        R.id.ban -> {
                            val accountId = viewModel.currentAccount?.id

                            if (accountId != null) {
                                BanUserDialogFragment()
                                    .apply {
                                        arguments = BanUserDialogFragmentArgs(
                                            args.communityId,
                                            args.personId,
                                            accountId,
                                        ).toBundle()
                                    }
                                    .showAllowingStateLoss(
                                        parentFragmentManager,
                                        "BanUserDialogFragment",
                                    )
                            }
                            dismiss()
                        }
                        R.id.undo_ban -> {
                            actionsViewModel.banUser(
                                communityId = args.communityId,
                                personId = args.personId,
                                ban = false,
                                removeData = false,
                                reason = null,
                                expiresDays = null,
                            )
                        }
                        R.id.remove_feature_comment -> {
                            actionsViewModel.distinguishComment(
                                commentId = args.commentId,
                                distinguish = false,
                            )
                        }
                        R.id.feature_comment -> {
                            actionsViewModel.distinguishComment(
                                commentId = args.commentId,
                                distinguish = true,
                            )
                        }
                        R.id.remove_comment -> {
                            actionsViewModel.removeComment(
                                commentId = args.commentId,
                                remove = true,
                            )
                        }
                        R.id.undo_remove_comment -> {
                            actionsViewModel.removeComment(
                                commentId = args.commentId,
                                remove = false,
                            )
                        }
                    }
                }
            }

            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            viewModel.loadModActionsState(
                communityId = args.communityId,
                postId = args.postId,
                commentId = args.commentId,
                personId = args.personId,
                force = true,
            )

            viewModel.currentModState.observe(viewLifecycleOwner) {
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

                        setupUi(it.data)
                    }
                }
            }

            actionsViewModel.deletePostAction.handleStateChange()
            actionsViewModel.featurePostAction.handleStateChange()
            actionsViewModel.lockPostAction.handleStateChange()
            actionsViewModel.removePostAction.handleStateChange()
            actionsViewModel.banUserResult.handleStateChange(
                { getString(R.string.error_unable_to_ban_user) },
                {
                    if (args.postId != -1) {
                        UpdatedObject.PostObject(args.postId)
                    } else if (args.commentId != -1) {
                        UpdatedObject.CommentObject(args.commentId)
                    } else {
                        null
                    }
                },
            )
            actionsViewModel.modUserResult.handleStateChange(
                { getString(R.string.error_unable_to_add_user_as_moderator) },
                {
                    if (args.postId != -1) {
                        UpdatedObject.PostObject(args.postId)
                    } else if (args.commentId != -1) {
                        UpdatedObject.CommentObject(args.commentId)
                    } else {
                        null
                    }
                },
            )
            actionsViewModel.distinguishCommentResult.handleStateChange(
                { getString(R.string.error_unable_to_update_comment) },
                { UpdatedObject.CommentObject(args.commentId) },
            )
            actionsViewModel.removeCommentResult.handleStateChange(
                { getString(R.string.error_unable_to_remove_comment) },
                { UpdatedObject.CommentObject(args.commentId) },
            )
        }
    }

    private fun <T> StatefulLiveData<T>.handleStateChange(
        errorMessage: () -> String,
        updatedObject: () -> UpdatedObject?,
    ) {
        this.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    if (it.error is ClientApiException && it.error.errorCode == 404) {
                        setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(
                                RESULT_UPDATED_OBJ to updatedObject(),
                            ),
                        )
                        dismiss()
                    } else {
                        ErrorDialogFragment.show(
                            errorMessage(),
                            it.error,
                            childFragmentManager,
                        )
                    }
                }

                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(
                            RESULT_UPDATED_OBJ to updatedObject(),
                        ),
                    )
                    dismiss()
                }
            }
        }
    }

    private fun MoreActionsViewModel.PostAction.handleStateChange() {
        this.state.handleStateChange(
            errorMessage = {
                when (actionType) {
                    MoreActionsViewModel.PostActionType.DeletePost -> {
                        getString(R.string.error_unable_to_delete_post)
                    }
                    MoreActionsViewModel.PostActionType.SavePost -> {
                        getString(R.string.error_unable_to_save_post)
                    }
                    MoreActionsViewModel.PostActionType.FeaturePost -> {
                        getString(R.string.error_unable_to_feature_post)
                    }
                    MoreActionsViewModel.PostActionType.LockPost -> {
                        getString(R.string.error_unable_to_lock_post)
                    }
                    MoreActionsViewModel.PostActionType.RemovePost -> {
                        getString(R.string.error_unable_to_remove_post)
                    }
                }
            },
            updatedObject = {
                UpdatedObject.PostObject(
                    args.postId,
                )
            },
        )
    }

    private fun setupUi(data: ModActionsViewModel.FullModState) {
        for (modState in data.modStates) {
            when (modState) {
                is CommentModState -> {
                    if (modState.isRemoved) {
                        adapter?.addItemWithIcon(
                            id = R.id.undo_remove_comment,
                            title = R.string.undo_remove_comment,
                            icon = R.drawable.baseline_add_24,
                        )
                    } else {
                        adapter?.addItemWithIcon(
                            id = R.id.remove_comment,
                            title = R.string.remove_comment,
                            icon = R.drawable.baseline_remove_24,
                        )
                    }
                    if (modState.isDistinguished != null) {
                        if (modState.isDistinguished) {
                            adapter?.addItemWithIcon(
                                id = R.id.remove_feature_comment,
                                title = R.string.remove_feature_comment,
                                icon = R.drawable.baseline_star_border_24,
                            )
                        } else {
                            adapter?.addItemWithIcon(
                                id = R.id.feature_comment,
                                title = R.string.feature_comment,
                                icon = R.drawable.baseline_star_24,
                            )
                        }
                    }
                }
                is CommunityModState -> {
                    if (args.personId != viewModel.currentAccount?.id) {
                        adapter?.addDividerIfNeeded()
                        if (modState.isMod) {
                            adapter?.addItemWithIcon(
                                id = R.id.unmod,
                                title = R.string.unmod_user,
                                icon = R.drawable.outline_remove_moderator_24,
                            )
                        } else {
                            adapter?.addItemWithIcon(
                                id = R.id.mod,
                                title = R.string.mod_user,
                                icon = R.drawable.outline_add_moderator_24,
                            )
                        }
                        adapter?.addDividerIfNeeded()
                    }
                }
                is PostModState -> {
                    if (modState.isRemoved) {
                        adapter?.addItemWithIcon(
                            id = R.id.undo_remove_post,
                            title = R.string.undo_remove_post,
                            icon = R.drawable.baseline_add_24,
                        )
                    } else {
                        adapter?.addItemWithIcon(
                            id = R.id.remove_post,
                            title = R.string.remove_post,
                            icon = R.drawable.baseline_remove_24,
                        )
                    }
                    if (modState.isFeatured) {
                        adapter?.addItemWithIcon(
                            id = R.id.remove_feature_post,
                            title = R.string.remove_feature_post,
                            icon = R.drawable.baseline_push_pin_24,
                        )
                    } else {
                        adapter?.addItemWithIcon(
                            id = R.id.feature_post,
                            title = R.string.feature_post,
                            icon = R.drawable.baseline_push_pin_24,
                        )
                    }
                    if (modState.isLocked) {
                        adapter?.addItemWithIcon(
                            id = R.id.unlock_post,
                            title = R.string.unlock_post,
                            icon = R.drawable.baseline_lock_open_24,
                        )
                    } else {
                        adapter?.addItemWithIcon(
                            id = R.id.lock_post,
                            title = R.string.lock_post,
                            icon = R.drawable.outline_lock_24,
                        )
                    }
                }
                is UserModState -> {
                    adapter?.addDividerIfNeeded()
                    if (modState.isBannedFromCommunity) {
                        adapter?.addItemWithIcon(
                            id = R.id.undo_ban,
                            title = R.string.unban_user,
                            icon = R.drawable.baseline_person_add_alt_24,
                        )
                    } else {
                        adapter?.addItemWithIcon(
                            id = R.id.ban,
                            title = R.string.ban_user,
                            icon = R.drawable.outline_person_remove_24,
                        )
                    }
                    adapter?.addDividerIfNeeded()
                }
            }
        }

        adapter?.refreshItems {
            binding.recyclerView.requestLayout()
        }
    }
}
