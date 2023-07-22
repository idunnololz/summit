package com.idunnololz.summit.lemmy.postAndCommentView

import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.post.ModernThreadLinesDecoration
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.preferences.CommentsThreadStyle
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.clearItemDecorations

const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"

fun RecyclerView.setupForPostAndComments(preferences: Preferences) {
    clearItemDecorations()
    addItemDecoration(
        when (preferences.commentThreadStyle) {
            CommentsThreadStyle.Legacy ->
                OldThreadLinesDecoration(context, preferences.hideCommentActions)
            else -> {
                ModernThreadLinesDecoration(context, preferences.hideCommentActions)
            }
        }
    )
}

fun BaseFragment<*>.showMoreCommentOptions(
    instance: String,
    commentView: CommentView,
    actionsViewModel: MoreActionsViewModel,
): BottomMenu? {
    if (!isBindingAvailable()) return null

    val context = requireContext()
    val currentAccount = actionsViewModel.accountManager.currentAccount.value

    val onEditCommentClick: (CommentView) -> Unit = a@{
        if (currentAccount == null) {
            PreAuthDialogFragment.newInstance(R.id.action_edit_comment)
                .show(childFragmentManager, "asdf")
            return@a
        }

        AddOrEditCommentFragment().apply {
            arguments =
                AddOrEditCommentFragmentArgs(
                    currentAccount.instance, null, null, it
                ).toBundle()
        }.show(childFragmentManager, "asdf")
    }
    val onDeleteCommentClick: (CommentView) -> Unit = a@{
        if (currentAccount == null) {
            PreAuthDialogFragment.newInstance()
                .show(childFragmentManager, "asdf")
            return@a
        }

        AlertDialogFragment.Builder()
            .setMessage(R.string.delete_comment_confirm)
            .setPositiveButton(android.R.string.ok)
            .setNegativeButton(android.R.string.cancel)
            .setExtra(EXTRA_COMMENT_ID, it.comment.id.toString())
            .createAndShow(
                childFragmentManager,
                CONFIRM_DELETE_COMMENT_TAG
            )
    }


    val bottomMenu = BottomMenu(requireContext()).apply {
        setTitle(R.string.more_actions)

        if (commentView.creator.id == currentAccount?.id) {
            addItemWithIcon(R.id.edit_comment, R.string.edit_comment, R.drawable.baseline_edit_24)
            addItemWithIcon(R.id.delete_comment, R.string.delete_comment, R.drawable.baseline_delete_24)
        }
        if (commentView.saved) {
            addItemWithIcon(R.id.remove_from_saved, R.string.remove_from_saved, R.drawable.baseline_bookmark_remove_24)
        } else {
            addItemWithIcon(R.id.save, R.string.save, R.drawable.baseline_bookmark_add_24)
        }
        addItemWithIcon(R.id.share, R.string.share, R.drawable.baseline_share_24)

        setOnMenuItemClickListener {
            when (it.id) {
                R.id.edit_comment -> {
                    onEditCommentClick(commentView)
                }
                R.id.delete_comment -> {
                    onDeleteCommentClick(commentView)
                }
                R.id.save -> {
                    actionsViewModel.saveComment(commentView.comment.id, true)
                }
                R.id.remove_from_saved -> {
                    actionsViewModel.saveComment(commentView.comment.id, false)
                }
                R.id.share -> {
                    Utils.shareLink(
                        context,
                        LinkUtils.getLinkForComment(instance, commentView.comment.id)
                    )
                }
            }
        }
    }
    getMainActivity()?.showBottomMenu(bottomMenu)

    return bottomMenu
}