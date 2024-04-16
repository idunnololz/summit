package com.idunnololz.summit.lemmy.utils.actions

import android.content.Intent
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.mod.ModActionResult
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.getParcelableCompat
import java.io.IOException

fun BaseFragment<*>.installOnActionResultHandler(
    moreActionsHelper: MoreActionsHelper,
    snackbarContainer: View,
    onSavePostChanged: ((SavePostResult) -> Unit)? = null,
    onSaveCommentChanged: ((SaveCommentResult) -> Unit)? = null,
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
                ErrorDialogFragment.show(
                    getString(R.string.error_unable_to_save_post),
                    it.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                Snackbar
                    .make(
                        snackbarContainer,
                        if (it.data.save) {
                            R.string.post_saved
                        } else {
                            R.string.post_removed_from_saved
                        },
                        Snackbar.LENGTH_SHORT,
                    )
                    .setAction(R.string.undo) { _ ->
                        moreActionsHelper.savePost(it.data.postId, !it.data.save)
                    }
                    .show()

                onSavePostChanged?.invoke(it.data)
                onPostUpdated?.invoke(it.data.postId)
            }
        }
    }
    moreActionsHelper.blockInstanceResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                Snackbar.make(
                    snackbarContainer,
                    R.string.error_unable_to_block_instance,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                onBlockInstanceChanged?.invoke()

                Snackbar
                    .make(
                        snackbarContainer,
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
                    snackbarContainer,
                    R.string.error_unable_to_block_community,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                onBlockCommunityChanged?.invoke()

                Snackbar
                    .make(
                        snackbarContainer,
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
                    snackbarContainer,
                    R.string.error_unable_to_block_person,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                onBlockPersonChanged?.invoke()

                Snackbar
                    .make(
                        snackbarContainer,
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
                ErrorDialogFragment.show(
                    getString(R.string.error_unable_to_save_comment),
                    it.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                Snackbar
                    .make(
                        snackbarContainer,
                        if (it.data.save) {
                            R.string.comment_saved
                        } else {
                            R.string.comment_removed_from_saved
                        },
                        Snackbar.LENGTH_SHORT,
                    )
                    .setAction(R.string.undo) { _ ->
                        moreActionsHelper.saveComment(it.data.commentId, !it.data.save)
                    }
                    .show()

                onSaveCommentChanged?.invoke(it.data)
                onCommentUpdated?.invoke(it.data.commentId)
            }
        }
    }
    moreActionsHelper.subscribeResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                ErrorDialogFragment.show(
                    getString(R.string.error_unable_to_update_subscription),
                    it.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                Snackbar
                    .make(
                        snackbarContainer,
                        if (it.data.subscribe) {
                            R.string.subscribed
                        } else {
                            R.string.unsubscribed
                        },
                        Snackbar.LENGTH_SHORT,
                    )
                    .setAction(R.string.undo) { _ ->
                        moreActionsHelper.updateSubscription(
                            it.data.communityId,
                            !it.data.subscribe,
                        )
                    }
                    .show()
            }
        }
    }

    moreActionsHelper.downloadResult.observe(this) {
        when (it) {
            is StatefulData.NotStarted -> {}
            is StatefulData.Error -> {
                Snackbar.make(
                    snackbarContainer,
                    R.string.error_downloading_image,
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is StatefulData.Loading -> {}
            is StatefulData.Success -> {
                it.data
                    .onSuccess { downloadResult ->
                        try {
                            val uri = downloadResult.uri
                            val mimeType = downloadResult.mimeType

                            val snackbarMsg =
                                getString(R.string.image_saved_format, downloadResult.uri)
                            Snackbar.make(
                                snackbarContainer,
                                snackbarMsg,
                                Snackbar.LENGTH_LONG,
                            ).setAction(R.string.view) {
                                Utils.safeLaunchExternalIntentWithErrorDialog(
                                    context,
                                    childFragmentManager,
                                    Intent(Intent.ACTION_VIEW).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        setDataAndType(uri, mimeType)
                                    },
                                )
                            }.show()

                            moreActionsHelper.downloadResult.postIdle()
                        } catch (e: IOException) {
                            /* do nothing */
                        }
                    }
                    .onFailure {
                        if (it is FileDownloadHelper.CustomDownloadLocationException) {
                            Snackbar
                                .make(
                                    snackbarContainer,
                                    R.string.error_downloading_image,
                                    Snackbar.LENGTH_LONG,
                                )
                                .setAction(R.string.downloads_settings) {
                                    getMainActivity()?.showDownloadsSettings()
                                }
                                .show()
                        } else {
                            FirebaseCrashlytics.getInstance().recordException(it)
                            Snackbar
                                .make(
                                    snackbarContainer,
                                    R.string.error_downloading_image,
                                    Snackbar.LENGTH_LONG,
                                )
                                .show()
                        }
                    }
            }
        }
    }
}
