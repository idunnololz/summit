package com.idunnololz.summit.lemmy.utils.actions

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.PostId
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.mod.ModActionResult
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.crashLogger.crashLogger
import com.idunnololz.summit.util.getParcelableCompat
import java.io.IOException
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

fun BaseFragment<*>.installOnActionResultHandler(
  context: Context,
  moreActionsHelper: MoreActionsHelper,
  snackbarContainer: View,
  onSavePostChanged: ((SavePostResult) -> Unit)? = null,
  onSaveCommentChanged: ((SaveCommentResult) -> Unit)? = null,
  onPostUpdated: ((PostId, accountId: Long?) -> Unit)? = null,
  onCommentUpdated: ((CommentId) -> Unit)? = null,
  onBlockInstanceChanged: (() -> Unit)? = null,
  onBlockCommunityChanged: (() -> Unit)? = null,
  onBlockPersonChanged: (() -> Unit)? = null,
) {
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
        onPostUpdated?.invoke(updatedObject.postId, updatedObject.accountId)
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
        onPostUpdated?.invoke(it.data.postId, it.data.accountId)
      }
    }
  }
  moreActionsHelper.deletePostResult.observe(viewLifecycleOwner) {
    when (it) {
      is StatefulData.Error -> {
        Snackbar.make(
          snackbarContainer,
          R.string.error_unable_to_delete_post,
          Snackbar.LENGTH_LONG,
        ).show()
      }
      is StatefulData.Loading -> {}
      is StatefulData.NotStarted -> {}
      is StatefulData.Success -> {
        onPostUpdated?.invoke(it.data.postId, it.data.accountId)

        Snackbar
          .make(
            snackbarContainer,
            if (it.data.delete) {
              R.string.post_deleted
            } else {
              R.string.post_restored
            },
            Snackbar.LENGTH_LONG,
          )
          .setAction(R.string.undo) { _ ->
            moreActionsHelper.deletePost(it.data.postId, !it.data.delete)
          }
          .show()
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

  moreActionsHelper.downloadResult.observe(viewLifecycleOwner) {
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
            } catch (_: IOException) {
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
              crashLogger?.recordException(it)
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

  viewLifecycleOwner.lifecycleScope.launch {
    moreActionsHelper.nsfwModeEnabledFlow.drop(1).collect {
      Snackbar
        .make(
          snackbarContainer,
          if (it) {
            R.string.nsfw_mode_enabled
          } else {
            R.string.nsfw_mode_disabled
          },
          Snackbar.LENGTH_LONG,
        )
        .show()
    }
  }
}
