package com.idunnololz.summit.lemmy.mod

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PurgeCommunity
import com.idunnololz.summit.databinding.DialogFragmentRemoveCommentBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.AdminOrModActionsViewModel
import com.idunnololz.summit.lemmy.mod.ModActionResult.setModActionResult
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.ext.setSizeDynamically
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class ModActionWithReasonDialogFragment :
    BaseDialogFragment<DialogFragmentRemoveCommentBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    private val args by navArgs<ModActionWithReasonDialogFragmentArgs>()

    private val actionsViewModel: AdminOrModActionsViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        setSizeDynamically(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentRemoveCommentBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modAction = args.modAction as ModActionWithReason

        with(binding) {
            fun getReasonText() = reasonEditText.text.toString().ifBlank {
                null
            }

            when (modAction) {
                is ModActionWithReason.RemoveComment -> {
                    binding.title.setText(R.string.remove_comment)
                    binding.positiveButton.setText(R.string.remove_comment)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.removeComment(
                            modAction.commentId,
                            remove = true,
                            reason = getReasonText(),
                        )
                    }
                    actionsViewModel.removeCommentResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_remove_comment)
                        },
                        getUpdatedObject = {
                            ModActionResult.UpdatedObject.CommentObject(modAction.commentId)
                        },
                    )
                }
                is ModActionWithReason.UndoRemoveComment -> {
                    binding.title.setText(R.string.undo_remove_comment)
                    binding.positiveButton.setText(R.string.undo_remove_comment)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.removeComment(
                            modAction.commentId,
                            remove = false,
                            reason = getReasonText(),
                        )
                    }
                    actionsViewModel.removeCommentResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_remove_comment)
                        },
                        getUpdatedObject = {
                            ModActionResult.UpdatedObject.CommentObject(modAction.commentId)
                        },
                    )
                }
                is ModActionWithReason.RemovePost -> {
                    binding.title.setText(R.string.remove_post)
                    binding.positiveButton.setText(R.string.remove_post)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.removePost(
                            modAction.postId,
                            remove = true,
                            reason = getReasonText(),
                        )
                    }
                    actionsViewModel.removePostResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_remove_post)
                        },
                        getUpdatedObject = {
                            ModActionResult.UpdatedObject.PostObject(modAction.postId, modAction.accountId)
                        },
                    )
                }
                is ModActionWithReason.UndoRemovePost -> {
                    binding.title.setText(R.string.undo_remove_post)
                    binding.positiveButton.setText(R.string.undo_remove_post)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.removePost(
                            modAction.postId,
                            remove = false,
                            reason = getReasonText(),
                        )
                    }
                    actionsViewModel.removePostResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_remove_post)
                        },
                        getUpdatedObject = {
                            ModActionResult.UpdatedObject.PostObject(modAction.postId, modAction.accountId)
                        },
                    )
                }

                is ModActionWithReason.PurgeComment -> {
                    binding.title.setText(R.string.purge_comment)
                    binding.positiveButton.setText(R.string.purge_comment)
                    binding.positiveButton.setOnClickListener {
                        AlertDialogFragment.Builder()
                            .setTitle(R.string.purge_comment_title)
                            .setMessage(R.string.purge_community_body)
                            .setPositiveButton(R.string.purge_comment)
                            .setNegativeButton(android.R.string.cancel)
                            .createAndShow(childFragmentManager, "purge_comment")
                    }
                    actionsViewModel.purgeCommentResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_purge_comment)
                        },
                        getUpdatedObject = {
                            ModActionResult.UpdatedObject.CommentObject(modAction.commentId)
                        },
                    )
                }
                is ModActionWithReason.PurgeCommunity -> {
                    binding.title.setText(R.string.purge_community)
                    binding.positiveButton.setText(R.string.purge_community)
                    binding.positiveButton.setOnClickListener {
                        AlertDialogFragment.Builder()
                            .setTitle(R.string.purge_community_title)
                            .setMessage(R.string.purge_community_body)
                            .setPositiveButton(R.string.purge_community)
                            .setNegativeButton(android.R.string.cancel)
                            .createAndShow(childFragmentManager, "purge_community")
                    }
                    actionsViewModel.purgeCommunityResult.handleResult(
                        { getString(R.string.error_purge_community) },
                        { null },
                    )
                }
                is ModActionWithReason.PurgePerson -> {
                    binding.title.setText(R.string.purge_person)
                    binding.positiveButton.setText(R.string.purge_person)
                    binding.positiveButton.setOnClickListener {
                        AlertDialogFragment.Builder()
                            .setTitle(R.string.purge_user_title)
                            .setMessage(R.string.purge_community_body)
                            .setPositiveButton(R.string.purge_user)
                            .setNegativeButton(android.R.string.cancel)
                            .createAndShow(childFragmentManager, "purge_user")
                    }
                    actionsViewModel.purgeUserResult.handleResult(
                        { getString(R.string.error_purge_user) },
                        { null },
                    )
                }
                is ModActionWithReason.PurgePost -> {
                    binding.title.setText(R.string.purge_post)
                    binding.positiveButton.setText(R.string.purge_post)
                    binding.positiveButton.setOnClickListener {
                        AlertDialogFragment.Builder()
                            .setTitle(R.string.purge_post_title)
                            .setMessage(R.string.purge_community_body)
                            .setPositiveButton(R.string.purge_post)
                            .setNegativeButton(android.R.string.cancel)
                            .createAndShow(childFragmentManager, "purge_post")
                    }
                    actionsViewModel.purgePostResult.handleResult(
                        { getString(R.string.error_purge_post) },
                        { ModActionResult.UpdatedObject.PostObject(modAction.postId, modAction.accountId) },
                    )
                }
                is ModActionWithReason.UndoBanUserFromCommunity -> {
                    binding.title.setText(R.string.undo_user_ban)
                    binding.positiveButton.setText(R.string.undo_user_ban)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.banUser(
                            communityId = modAction.communityId,
                            personId = modAction.personId,
                            ban = false,
                            removeData = false,
                            reason = getReasonText(),
                            expiresDays = null,
                        )
                    }
                    actionsViewModel.banUserResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_ban_user)
                        },
                        getUpdatedObject = {
                            null
                        },
                    )
                }
                is ModActionWithReason.UndoBanUserFromSite -> {
                    binding.title.setText(R.string.undo_user_ban_from_instance)
                    binding.positiveButton.setText(R.string.undo_user_ban)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.banUserFromSite(
                            personId = modAction.personId,
                            ban = false,
                            removeData = false,
                            reason = getReasonText(),
                            expiresDays = null,
                        )
                    }
                    actionsViewModel.banUserFromSiteResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_ban_user)
                        },
                        getUpdatedObject = {
                            null
                        },
                    )
                }

                is ModActionWithReason.RemoveCommunity -> {
                    binding.title.setText(R.string.remove_community)
                    binding.positiveButton.setText(R.string.remove_community)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.removeCommunity(
                            communityId = modAction.communityId,
                            remove = true,
                            reason = getReasonText(),
                        )
                    }
                    actionsViewModel.removeCommunityResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_remove_community)
                        },
                        getUpdatedObject = {
                            null
                        },
                    )
                }
                is ModActionWithReason.UndoRemoveCommunity -> {
                    binding.title.setText(R.string.undo_remove_community)
                    binding.positiveButton.setText(R.string.undo_remove_community)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.removeCommunity(
                            communityId = modAction.communityId,
                            remove = false,
                            reason = getReasonText(),
                        )
                    }
                    actionsViewModel.removeCommunityResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_remove_community)
                        },
                        getUpdatedObject = {
                            null
                        },
                    )
                }
                is ModActionWithReason.HideCommunity -> {
                    binding.title.setText(R.string.hide_community)
                    binding.positiveButton.setText(R.string.hide_community)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.hideCommunity(
                            communityId = modAction.communityId,
                            hide = true,
                            reason = getReasonText(),
                        )
                    }
                    actionsViewModel.hideCommunityResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_hide_community)
                        },
                        getUpdatedObject = {
                            null
                        },
                    )
                }
                is ModActionWithReason.UndoHideCommunity -> {
                    binding.title.setText(R.string.undo_hide_community)
                    binding.positiveButton.setText(R.string.undo_hide_community)
                    binding.positiveButton.setOnClickListener {
                        actionsViewModel.hideCommunity(
                            communityId = modAction.communityId,
                            hide = false,
                            reason = getReasonText(),
                        )
                    }
                    actionsViewModel.hideCommunityResult.handleResult(
                        onErrorMessage = {
                            getString(R.string.error_unable_to_hide_community)
                        },
                        getUpdatedObject = {
                            null
                        },
                    )
                }
            }

            binding.cancel.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun <T> StatefulLiveData<T>.handleResult(
        onErrorMessage: () -> String,
        getUpdatedObject: () -> ModActionResult.UpdatedObject?,
    ) {
        this.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.progressBar.visibility = View.GONE

                    ErrorDialogFragment.show(
                        onErrorMessage(),
                        it.error,
                        childFragmentManager,
                    )
                    disableViews(false)
                }
                is StatefulData.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    disableViews(true)
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.progressBar.visibility = View.GONE
                    disableViews(false)

                    setModActionResult(
                        getUpdatedObject(),
                    )

                    dismiss()
                }
            }
        }
    }

    fun disableViews(disable: Boolean) {
        if (!isBindingAvailable()) {
            return
        }

        with(binding) {
            reason.isEnabled = !disable
            positiveButton.isEnabled = !disable
            cancel.isEnabled = !disable
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        if (!isBindingAvailable()) {
            return
        }

        when (tag) {
            "purge_community" -> {
                actionsViewModel.purgeCommunity(
                    communityId = (args.modAction as ModActionWithReason.PurgeCommunity).communityId,
                    reason = binding.reasonEditText.text.toString().ifBlank {
                        null
                    },
                )
            }
            "purge_post" -> {
                actionsViewModel.purgePost(
                    postId = (args.modAction as ModActionWithReason.PurgePost).postId,
                    reason = binding.reasonEditText.text.toString().ifBlank {
                        null
                    },
                )
            }
            "purge_user" -> {
                actionsViewModel.purgePerson(
                    personId = (args.modAction as ModActionWithReason.PurgePerson).personId,
                    reason = binding.reasonEditText.text.toString().ifBlank {
                        null
                    },
                )
            }
            "purge_comment" -> {
                actionsViewModel.purgeComment(
                    commentId = (args.modAction as ModActionWithReason.PurgeComment).commentId,
                    reason = binding.reasonEditText.text.toString().ifBlank {
                        null
                    },
                )
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }

    sealed class ModActionWithReason : Parcelable {
        @Parcelize
        data class RemovePost(
            val postId: PostId,
            val accountId: Long?,
        ) : ModActionWithReason()

        @Parcelize
        data class UndoRemovePost(
            val postId: PostId,
            val accountId: Long?,
        ) : ModActionWithReason()

        @Parcelize
        data class RemoveComment(
            val commentId: CommentId,
        ) : ModActionWithReason()

        @Parcelize
        data class UndoRemoveComment(
            val commentId: CommentId,
        ) : ModActionWithReason()

        @Parcelize
        data class PurgeComment(
            val commentId: CommentId,
        ) : ModActionWithReason()

        @Parcelize
        data class PurgeCommunity(
            val communityId: CommunityId,
        ) : ModActionWithReason()

        @Parcelize
        data class PurgePerson(
            val personId: PersonId,
        ) : ModActionWithReason()

        @Parcelize
        data class PurgePost(
            val postId: PostId,
            val accountId: Long?,
        ) : ModActionWithReason()

        @Parcelize
        data class UndoBanUserFromCommunity(
            val communityId: CommunityId,
            val personId: PersonId,
        ) : ModActionWithReason()

        @Parcelize
        data class UndoBanUserFromSite(
            val personId: PersonId,
        ) : ModActionWithReason()

        @Parcelize
        data class RemoveCommunity(
            val communityId: CommunityId,
        ) : ModActionWithReason()

        @Parcelize
        data class UndoRemoveCommunity(
            val communityId: CommunityId,
        ) : ModActionWithReason()

        @Parcelize
        data class HideCommunity(
            val communityId: CommunityId,
        ) : ModActionWithReason()

        @Parcelize
        data class UndoHideCommunity(
            val communityId: CommunityId,
        ) : ModActionWithReason()
    }
}
