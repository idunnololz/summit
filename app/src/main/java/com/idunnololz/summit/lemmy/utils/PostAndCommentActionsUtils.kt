package com.idunnololz.summit.lemmy.utils

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.mod.ModActionsDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.getParcelableCompat

fun BaseFragment<*>.installOnActionResultHandler(
    actionsViewModel: MoreActionsViewModel,
    snackbarContainer: View,
    onSavePostChanged: ((PostView) -> Unit)? = null,
    onSaveCommentChanged: ((CommentView) -> Unit)? = null,
    onPostUpdated: ((PostId) -> Unit)? = null,
    onCommentUpdated: ((CommentId) -> Unit)? = null,
    getSnackbarOffset: (() -> Int)? = null,
) {
    fun Snackbar.setup(): Snackbar =
        apply {
            if (getSnackbarOffset != null) {
                this.view.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = getSnackbarOffset()
                }
            }
        }

    childFragmentManager.setFragmentResultListener(
        ModActionsDialogFragment.REQUEST_KEY,
        viewLifecycleOwner,
    ) { requestKey, result ->
        val updatedObject = result.getParcelableCompat<ModActionsDialogFragment.UpdatedObject>(
            ModActionsDialogFragment.RESULT_UPDATED_OBJ,
        )

        when (updatedObject) {
            is ModActionsDialogFragment.UpdatedObject.CommentObject -> {
                onCommentUpdated?.invoke(updatedObject.commentId)
            }
            is ModActionsDialogFragment.UpdatedObject.PostObject -> {
                onPostUpdated?.invoke(updatedObject.postId)
            }
            null -> { /* do nothing */ }
        }
    }

    actionsViewModel.savePostAction.state.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                actionsViewModel.savePostAction.state.setIdle()
                ErrorDialogFragment.show(
                    getString(R.string.unable_to_save_post),
                    it.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                actionsViewModel.savePostAction.state.setIdle()
                Snackbar.make(
                    snackbarContainer,
                    if (it.data.saved) {
                        R.string.post_saved
                    } else {
                        R.string.post_removed_from_saved
                    },
                    Snackbar.LENGTH_SHORT,
                )
                    .setup()
                    .show()

                onSavePostChanged?.invoke(it.data)
            }
        }
    }
    actionsViewModel.saveCommentResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                actionsViewModel.saveCommentResult.setIdle()
                ErrorDialogFragment.show(
                    getString(R.string.unable_to_save_comment),
                    it.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                actionsViewModel.saveCommentResult.setIdle()
                Snackbar.make(
                    snackbarContainer,
                    if (it.data.saved) {
                        R.string.comment_saved
                    } else {
                        R.string.comment_removed_from_saved
                    },
                    Snackbar.LENGTH_SHORT,
                )
                    .setup()
                    .show()

                onSaveCommentChanged?.invoke(it.data)
            }
        }
    }

    fun MoreActionsViewModel.PostAction.handleStateChange() {
        this.state.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
//                    if (it.error is ClientApiException && it.error.errorCode == 404) {
//                        onPostUpdated(actionType)
//                        (parentFragment as? CommunityFragment)?.onPostUpdated()
//                    } else {
                    ErrorDialogFragment.show(
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
                        },
                        it.error,
                        childFragmentManager,
                    )
//                    }
                }

                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    onPostUpdated?.invoke(it.data.post.id)
                }
            }
        }
    }

    actionsViewModel.deletePostAction.handleStateChange()
    actionsViewModel.featurePostAction.handleStateChange()
    actionsViewModel.lockPostAction.handleStateChange()
    actionsViewModel.removePostAction.handleStateChange()

    actionsViewModel.banUserResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                actionsViewModel.banUserResult.setIdle()
                ErrorDialogFragment.show(
                    getString(R.string.unable_to_ban_user),
                    it.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                actionsViewModel.banUserResult.setIdle()
            }
        }
    }
}
