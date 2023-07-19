package com.idunnololz.summit.lemmy.utils

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData

fun BaseFragment<*>.installOnActionResultHandler(
    actionsViewModel: MoreActionsViewModel,
    snackbarContainer: View,
    onSavePostChanged: ((PostView) -> Unit)? = null,
    onSaveCommentChanged: ((CommentView) -> Unit)? = null,
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

    actionsViewModel.savePostResult.observe(viewLifecycleOwner) {
        when (it) {
            is StatefulData.Error -> {
                actionsViewModel.savePostResult.setIdle()
                ErrorDialogFragment.show(
                    getString(R.string.unable_to_save_post),
                    it.error,
                    childFragmentManager
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                actionsViewModel.savePostResult.setIdle()
                Snackbar.make(snackbarContainer,
                    if (it.data.saved) {
                        R.string.post_saved
                    } else {
                        R.string.post_removed_from_saved
                    }
                    , Snackbar.LENGTH_SHORT)
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
                    childFragmentManager
                )
            }
            is StatefulData.Loading -> {}
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                actionsViewModel.saveCommentResult.setIdle()
                Snackbar.make(snackbarContainer,
                    if (it.data.saved) {
                        R.string.comment_saved
                    } else {
                        R.string.comment_removed_from_saved
                    }, Snackbar.LENGTH_SHORT)
                    .setup()
                    .show()

                onSaveCommentChanged?.invoke(it.data)
            }
        }
    }
}
