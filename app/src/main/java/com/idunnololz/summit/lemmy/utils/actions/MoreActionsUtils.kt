package com.idunnololz.summit.lemmy.utils.actions

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.mod.ModActionResult
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.getParcelableCompat

fun BaseFragment<*>.installOnActionResultHandler(
    moreActionsHelper: MoreActionsHelper,
    snackbarContainer: View,
    onSavePostChanged: ((PostView) -> Unit)? = null,
    onSaveCommentChanged: ((CommentView) -> Unit)? = null,
    onPostUpdated: ((PostId) -> Unit)? = null,
    onCommentUpdated: ((CommentId) -> Unit)? = null,
    onBlockInstanceChanged: (() -> Unit)? = null,
    onBlockCommunityChanged: (() -> Unit)? = null,
    onBlockPersonChanged: (() -> Unit)? = null,
) {
    val context = snackbarContainer.context

    childFragmentManager.setFragmentResultListener(
        ModActionResult.REQUEST_KEY,
        viewLifecycleOwner,
    ) { requestKey, result ->
        val updatedObject = result.getParcelableCompat<ModActionResult.UpdatedObject>(
            ModActionResult.RESULT_UPDATED_OBJ,
        )

        when (updatedObject) {
            is ModActionResult.UpdatedObject.CommentObject -> {
                onCommentUpdated?.invoke(updatedObject.commentId)
            }
            is ModActionResult.UpdatedObject.PostObject -> {
                onPostUpdated?.invoke(updatedObject.postId)
            }
            null -> { /* do nothing */ }
        }
    }

    moreActionsHelper.savePostResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                moreActionsHelper.savePostResult.setIdle()
                ErrorDialogFragment.show(
                    getString(R.string.error_unable_to_save_post),
                    it.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                moreActionsHelper.savePostResult.setIdle()
                Snackbar.make(
                    snackbarContainer,
                    if (it.data.saved) {
                        R.string.post_saved
                    } else {
                        R.string.post_removed_from_saved
                    },
                    Snackbar.LENGTH_SHORT,
                )
                    .show()

                onSavePostChanged?.invoke(it.data)
                onPostUpdated?.invoke(it.data.post.id)
            }
        }
    }
    moreActionsHelper.blockInstanceResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                Snackbar.make(
                    requireMainActivity().getSnackbarContainer(),
                    R.string.error_unable_to_block_instance,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                moreActionsHelper.blockInstanceResult.setIdle()
                onBlockInstanceChanged?.invoke()

                Snackbar
                    .make(
                        requireMainActivity().getSnackbarContainer(),
                        if (it.data.blocked) {
                            R.string.instance_blocked
                        } else {
                            R.string.instance_unblocked
                        },
                        Snackbar.LENGTH_LONG,
                    )
                    .setAction(R.string.undo) { _ ->
                        moreActionsHelper.blockInstance(it.data.instanceId, !it.data.blocked)
                    }
                    .show()
            }
        }
    }
    moreActionsHelper.blockCommunityResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                Snackbar.make(
                    requireMainActivity().getSnackbarContainer(),
                    R.string.error_unable_to_block_community,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                moreActionsHelper.blockCommunityResult.setIdle()
                onBlockCommunityChanged?.invoke()

                Snackbar
                    .make(
                        requireMainActivity().getSnackbarContainer(),
                        if (it.data.blocked) {
                            R.string.community_blocked
                        } else {
                            R.string.community_unblocked
                        },
                        Snackbar.LENGTH_LONG,
                    )
                    .setAction(R.string.undo) { _ ->
                        moreActionsHelper.blockCommunity(it.data.communityId, !it.data.blocked)
                    }
                    .show()
            }
        }
    }
    moreActionsHelper.blockPersonResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                Snackbar.make(
                    requireMainActivity().getSnackbarContainer(),
                    R.string.error_unable_to_block_person,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                moreActionsHelper.blockPersonResult.setIdle()
                onBlockPersonChanged?.invoke()

                Snackbar
                    .make(
                        requireMainActivity().getSnackbarContainer(),
                        if (it.data.blocked) {
                            R.string.user_blocked
                        } else {
                            R.string.user_unblocked
                        },
                        Snackbar.LENGTH_LONG,
                    )
                    .setAction(R.string.undo) { _ ->
                        moreActionsHelper.blockPerson(it.data.personId, !it.data.blocked)
                    }
                    .show()
            }
        }
    }
    moreActionsHelper.saveCommentResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                moreActionsHelper.saveCommentResult.setIdle()
                ErrorDialogFragment.show(
                    getString(R.string.error_unable_to_save_comment),
                    it.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                moreActionsHelper.saveCommentResult.setIdle()
                Snackbar.make(
                    snackbarContainer,
                    if (it.data.saved) {
                        R.string.comment_saved
                    } else {
                        R.string.comment_removed_from_saved
                    },
                    Snackbar.LENGTH_SHORT,
                )
                    .show()

                onSaveCommentChanged?.invoke(it.data)
                onCommentUpdated?.invoke(it.data.comment.id)
            }
        }
    }
}
