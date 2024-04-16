package com.idunnololz.summit.lemmy.mod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.idunnololz.summit.R
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.DialogFragmentModActionsBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.AdminOrModActionsViewModel
import com.idunnololz.summit.lemmy.mod.ModActionResult.UpdatedObject
import com.idunnololz.summit.lemmy.mod.ModActionResult.setModActionResult
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.PurgeComment
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.PurgeCommunity
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.PurgePerson
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.PurgePost
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.RemoveComment
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.RemoveCommunity
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.RemovePost
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.UndoBanUserFromCommunity
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.UndoBanUserFromSite
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.UndoRemoveComment
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.UndoRemoveCommunity
import com.idunnololz.summit.lemmy.mod.ModActionWithReasonDialogFragment.ModActionWithReason.UndoRemovePost
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

@AndroidEntryPoint
class ModActionsDialogFragment :
    BaseBottomSheetDialogFragment<DialogFragmentModActionsBinding>(),
    FullscreenDialogFragment {

    companion object {

        fun show(postView: PostView, fragmentManager: FragmentManager) {
            show(
                communityId = postView.community.id,
                commentId = -1,
                postId = postView.post.id,
                personId = postView.creator.id,
                communityInstance = postView.community.instance,
                fragmentManager = fragmentManager,
            )
        }

        fun show(commentView: CommentView, fragmentManager: FragmentManager) {
            show(
                communityId = commentView.community.id,
                commentId = commentView.comment.id,
                postId = -1,
                personId = commentView.creator.id,
                communityInstance = commentView.community.instance,
                fragmentManager = fragmentManager,
            )
        }

        fun show(
            communityId: Int,
            commentId: Int,
            postId: Int,
            personId: PersonId,
            communityInstance: String,
            fragmentManager: FragmentManager,
        ) {
            ModActionsDialogFragment()
                .apply {
                    arguments = ModActionsDialogFragmentArgs(
                        communityId = communityId,
                        commentId = commentId,
                        postId = postId,
                        personId = personId,
                        communityInstance = communityInstance,
                    ).toBundle()
                }
                .show(fragmentManager, "ModActionsDialogFragment")
        }
    }

    private val args by navArgs<ModActionsDialogFragmentArgs>()

    private val viewModel: ModActionsViewModel by viewModels()
    private val actionsViewModel: AdminOrModActionsViewModel by viewModels()

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
            fun launchModActionWithReason(modAction: ModActionWithReason) {
                ModActionWithReasonDialogFragment()
                    .apply {
                        arguments = ModActionWithReasonDialogFragmentArgs(
                            modAction = modAction,
                        ).toBundle()
                    }
                    .showAllowingStateLoss(
                        parentFragmentManager,
                        "ModActionWithReasonDialogFragment",
                    )
                dismiss()
            }

            adapter = BottomMenu.BottomMenuAdapter(context).apply {
                title =
                    if (viewModel.isAdmin(args.communityInstance)) {
                        getString(R.string.admin_actions)
                    } else {
                        getString(R.string.mod_actions)
                    }
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
                            launchModActionWithReason(
                                RemovePost(
                                    args.postId,
                                ),
                            )
                        }
                        R.id.undo_remove_post -> {
                            launchModActionWithReason(
                                UndoRemovePost(
                                    args.postId,
                                ),
                            )
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
                            launchModActionWithReason(
                                UndoBanUserFromCommunity(
                                    args.communityId,
                                    args.personId,
                                ),
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
                            launchModActionWithReason(
                                RemoveComment(
                                    args.commentId,
                                ),
                            )
                        }
                        R.id.undo_remove_comment -> {
                            launchModActionWithReason(
                                UndoRemoveComment(
                                    args.commentId,
                                ),
                            )
                        }
                        R.id.ban_from_site -> {
                            val accountId = viewModel.currentAccount?.id

                            if (accountId != null) {
                                BanUserDialogFragment()
                                    .apply {
                                        arguments = BanUserDialogFragmentArgs(
                                            communityId = 0,
                                            personId = args.personId,
                                            accountId = accountId,
                                        ).toBundle()
                                    }
                                    .showAllowingStateLoss(
                                        parentFragmentManager,
                                        "BanUserDialogFragment",
                                    )
                            }
                            dismiss()
                        }
                        R.id.undo_ban_from_site -> {
                            launchModActionWithReason(
                                UndoBanUserFromSite(
                                    args.personId,
                                ),
                            )
                        }
                        R.id.purge_community -> {
                            launchModActionWithReason(
                                PurgeCommunity(
                                    args.communityId,
                                ),
                            )
                        }
                        R.id.purge_post -> {
                            launchModActionWithReason(
                                PurgePost(
                                    args.postId,
                                ),
                            )
                        }
                        R.id.purge_user -> {
                            launchModActionWithReason(
                                PurgePerson(
                                    args.personId,
                                ),
                            )
                        }
                        R.id.purge_comment -> {
                            launchModActionWithReason(
                                PurgeComment(
                                    args.commentId,
                                ),
                            )
                        }
                        R.id.remove_community -> {
                            launchModActionWithReason(
                                RemoveCommunity(
                                    args.communityId,
                                ),
                            )
                        }
                        R.id.undo_remove_community -> {
                            launchModActionWithReason(
                                UndoRemoveCommunity(
                                    args.communityId,
                                ),
                            )
                        }
                        R.id.hide_community -> {
                            launchModActionWithReason(
                                ModActionWithReason.HideCommunity(
                                    args.communityId,
                                ),
                            )
                        }
                        R.id.undo_hide_community -> {
                            launchModActionWithReason(
                                ModActionWithReason.UndoHideCommunity(
                                    args.communityId,
                                ),
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

            actionsViewModel.featurePostResult.handleStateChange(
                errorMessage = {
                    getString(R.string.error_unable_to_feature_post)
                },
                updatedObject = {
                    UpdatedObject.PostObject(
                        args.postId,
                    )
                },
            )
            actionsViewModel.lockPostResult.handleStateChange(
                errorMessage = {
                    getString(R.string.error_unable_to_lock_post)
                },
                updatedObject = {
                    UpdatedObject.PostObject(
                        args.postId,
                    )
                },
            )
            actionsViewModel.removePostResult.handleStateChange(
                errorMessage = {
                    getString(R.string.error_unable_to_remove_post)
                },
                updatedObject = {
                    UpdatedObject.PostObject(
                        args.postId,
                    )
                },
            )

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
            actionsViewModel.banUserFromSiteResult.handleStateChange(
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

            actionsViewModel.purgeCommunityResult.handleStateChange(
                { getString(R.string.error_purge_community) },
                { null },
            )
            actionsViewModel.purgeUserResult.handleStateChange(
                { getString(R.string.error_purge_user) },
                { null },
            )
            actionsViewModel.purgePostResult.handleStateChange(
                { getString(R.string.error_purge_post) },
                { UpdatedObject.PostObject(args.postId) },
            )
            actionsViewModel.purgeCommentResult.handleStateChange(
                { getString(R.string.error_purge_comment) },
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
                        setModActionResult(updatedObject())
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
                    setModActionResult(updatedObject())
                    dismiss()
                }
            }
        }
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
                            title = R.string.ban_user_from_community,
                            icon = R.drawable.outline_person_remove_24,
                        )
                    }
                    adapter?.addDividerIfNeeded()
                }
            }
        }

        if (viewModel.isAdmin(args.communityInstance)) {
            adapter?.addDividerIfNeeded()

            val userModState = data.modStates.filterIsInstance<UserModState>().firstOrNull()
            if (userModState != null) {
                if (userModState.isBannedFromSite) {
                    adapter?.addItemWithIcon(
                        id = R.id.undo_ban_from_site,
                        title = R.string.unban_user_from_site,
                        icon = R.drawable.baseline_person_add_alt_24,
                    )
                } else {
                    adapter?.addItemWithIcon(
                        id = R.id.ban_from_site,
                        title = R.string.ban_user_from_site,
                        icon = R.drawable.outline_person_remove_24,
                    )
                }
            }

            val communityModState = data.modStates.filterIsInstance<CommunityModState>()
                .firstOrNull()
            if (communityModState != null) {
                if (communityModState.isRemoved) {
                    adapter?.addItemWithIcon(
                        id = R.id.undo_remove_community,
                        title = R.string.undo_remove_community,
                        icon = R.drawable.baseline_add_24,
                    )
                } else {
                    adapter?.addItemWithIcon(
                        id = R.id.remove_community,
                        title = R.string.remove_community,
                        icon = R.drawable.baseline_remove_24,
                    )
                }
                if (communityModState.isHidden) {
                    adapter?.addItemWithIcon(
                        id = R.id.undo_hide_community,
                        title = R.string.undo_hide_community,
                        icon = R.drawable.baseline_visibility_24,
                    )
                } else {
                    adapter?.addItemWithIcon(
                        id = R.id.hide_community,
                        title = R.string.hide_community,
                        icon = R.drawable.baseline_visibility_off_24,
                    )
                }
            }

            adapter?.addDividerIfNeeded()

            adapter?.addItemWithIcon(
                id = R.id.purge_user,
                title = R.string.purge_user,
                icon = R.drawable.outline_person_remove_24,
                modifier = BottomMenu.ModifierIds.Danger,
            )

            if (data.modStates.any { it is PostModState }) {
                adapter?.addItemWithIcon(
                    id = R.id.purge_post,
                    title = R.string.purge_post,
                    icon = R.drawable.baseline_delete_24,
                    modifier = BottomMenu.ModifierIds.Danger,
                )
            }

            if (data.modStates.any { it is CommentModState }) {
                adapter?.addItemWithIcon(
                    id = R.id.purge_comment,
                    title = R.string.purge_comment,
                    icon = R.drawable.baseline_delete_24,
                    modifier = BottomMenu.ModifierIds.Danger,
                )
            }

            if (communityModState != null) {
                adapter?.addItemWithIcon(
                    id = R.id.purge_community,
                    title = R.string.purge_community,
                    icon = R.drawable.baseline_delete_24,
                    modifier = BottomMenu.ModifierIds.Danger,
                )
            }

            adapter?.addDividerIfNeeded()
        }

        adapter?.refreshItems {
            binding.recyclerView.requestLayout()
        }
    }
}
