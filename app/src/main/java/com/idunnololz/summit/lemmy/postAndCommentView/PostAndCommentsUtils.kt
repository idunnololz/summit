package com.idunnololz.summit.lemmy.postAndCommentView

import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.contentDetails.ContentDetailsDialogFragment
import com.idunnololz.summit.lemmy.mod.ModActionsDialogFragment
import com.idunnololz.summit.lemmy.post.ModernThreadLinesDecoration
import com.idunnololz.summit.lemmy.post.OldThreadLinesDecoration
import com.idunnololz.summit.lemmy.report.ReportContentDialogFragment
import com.idunnololz.summit.preferences.CommentsThreadStyle
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.clearItemDecorations
import com.idunnololz.summit.util.ext.showAllowingStateLoss

const val CONFIRM_DELETE_COMMENT_TAG = "CONFIRM_DELETE_COMMENT_TAG"
const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"

fun RecyclerView.setupForPostAndComments(preferences: Preferences) {
    clearItemDecorations()
    addItemDecoration(
        when (preferences.commentThreadStyle) {
            CommentsThreadStyle.Legacy ->
                OldThreadLinesDecoration(context, preferences.hideCommentActions)
            CommentsThreadStyle.LegacyWithColors ->
                OldThreadLinesDecoration(context, preferences.hideCommentActions, colorful = true)
            CommentsThreadStyle.LegacyWithColorsAndDividers ->
                OldThreadLinesDecoration(context, preferences.hideCommentActions, colorful = true, dividers = true)
            else -> {
                ModernThreadLinesDecoration(context, preferences.hideCommentActions)
            }
        },
    )
}

fun BaseFragment<*>.showMoreCommentOptions(
    instance: String,
    commentView: CommentView,
    actionsViewModel: MoreActionsViewModel,
    fragmentManager: FragmentManager,
    onLoadComment: ((CommentId) -> Unit)? = null,
    onScreenshotClick: (() -> Unit)? = null,
): BottomMenu? {
    if (!isBindingAvailable()) return null

    val currentAccount = actionsViewModel.accountManager.currentAccount.asAccount

    val bottomMenu = BottomMenu(requireContext()).apply {
        setTitle(R.string.more_actions)

        addItemWithIcon(R.id.ca_reply, R.string.reply, R.drawable.baseline_reply_24)

        if (commentView.creator.id == currentAccount?.id) {
            addItemWithIcon(R.id.ca_edit_comment, R.string.edit_comment, R.drawable.baseline_edit_24)
            addItemWithIcon(R.id.ca_delete_comment, R.string.delete_comment, R.drawable.baseline_delete_24)
        }
        if (commentView.saved) {
            addItemWithIcon(R.id.ca_remove_from_saved, R.string.remove_from_saved, R.drawable.baseline_bookmark_remove_24)
        } else {
            addItemWithIcon(R.id.ca_save, R.string.save, R.drawable.baseline_bookmark_add_24)
        }

        val fullAccount = actionsViewModel.accountInfoManager.currentFullAccount.value
        val miscAccountInfo = fullAccount
            ?.accountInfo
            ?.miscAccountInfo

        if (instance == fullAccount?.account?.instance &&
            miscAccountInfo?.isAdmin == true
        ) {
            // Apparently if the user is an admin, they can ignore mod rules and perform any actions
            // they wish on their instance.
            addDivider()

            addItemWithIcon(
                id = R.id.ca_admin_tools,
                title = R.string.admin_tools,
                icon = R.drawable.outline_shield_24,
            )

            addDivider()
        } else if (miscAccountInfo
            ?.modCommunityIds
            ?.contains(commentView.community.id) == true
        ) {
            addDivider()

            addItemWithIcon(
                id = R.id.ca_mod_tools,
                title = R.string.mod_tools,
                icon = R.drawable.outline_shield_24,
            )

            addDivider()
        }

        addItemWithIcon(R.id.ca_share, R.string.share, R.drawable.baseline_share_24)

        if (onScreenshotClick != null) {
            addItemWithIcon(
                R.id.ca_screenshot,
                getString(R.string.take_screenshot),
                R.drawable.baseline_screenshot_24,
            )
        }
        addItemWithIcon(
            R.id.ca_share_fediverse_link,
            getString(R.string.share_source_link),
            R.drawable.ic_fediverse_24,
        )
        if (onLoadComment != null) {
            addItemWithIcon(
                R.id.ca_open_comment_in_new_screen,
                R.string.open_comment,
                R.drawable.baseline_open_in_new_24,
            )
        }

        addDivider()
        addItemWithIcon(
            R.id.ca_block_user,
            getString(R.string.block_this_user_format, commentView.creator.name),
            R.drawable.baseline_person_off_24,
        )
        addItemWithIcon(
            R.id.ca_report_comment,
            getString(R.string.report_comment),
            R.drawable.baseline_outlined_flag_24,
        )
        addDivider()

        addItemWithIcon(R.id.ca_view_source, R.string.view_source, R.drawable.baseline_code_24)
        addItemWithIcon(R.id.ca_detailed_view, R.string.detailed_view, R.drawable.baseline_open_in_full_24)

        setOnMenuItemClickListener {
            createCommentActionHandler(
                instance = instance,
                commentView = commentView,
                actionsViewModel = actionsViewModel,
                fragmentManager = fragmentManager,
                onLoadComment = onLoadComment,
                onScreenshotClick = onScreenshotClick
            )(it.id)
        }
    }
    getMainActivity()?.showBottomMenu(bottomMenu, expandFully = false)

    return bottomMenu
}

fun BaseFragment<*>.createCommentActionHandler(
    instance: String,
    commentView: CommentView,
    actionsViewModel: MoreActionsViewModel,
    fragmentManager: FragmentManager,
    onLoadComment: ((CommentId) -> Unit)? = null,
    onScreenshotClick: (() -> Unit)? = null,
): (Int) -> Unit = test@{ id: Int ->

    val context = requireContext()
    val currentAccount = actionsViewModel.accountManager.currentAccount.asAccount

    val onEditCommentClick: (CommentView) -> Unit = a@{
        if (currentAccount == null) {
            PreAuthDialogFragment.newInstance(R.id.action_edit_comment)
                .show(childFragmentManager, "asdf")
            return@a
        }

        AddOrEditCommentFragment().apply {
            arguments =
                AddOrEditCommentFragmentArgs(
                    currentAccount.instance,
                    null,
                    null,
                    it,
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
                CONFIRM_DELETE_COMMENT_TAG,
            )
    }

    when (id) {
        R.id.ca_edit_comment -> {
            onEditCommentClick(commentView)
        }
        R.id.ca_delete_comment -> {
            onDeleteCommentClick(commentView)
        }
        R.id.ca_save_toggle -> {
            if (commentView.saved) {
                actionsViewModel.saveComment(commentView.comment.id, false)
            } else {
                actionsViewModel.saveComment(commentView.comment.id, true)
            }
        }
        R.id.ca_save -> {
            actionsViewModel.saveComment(commentView.comment.id, true)
        }
        R.id.ca_remove_from_saved -> {
            actionsViewModel.saveComment(commentView.comment.id, false)
        }
        R.id.ca_share -> {
            Utils.shareLink(
                context,
                LinkUtils.getLinkForComment(instance, commentView.comment.id),
            )
        }
        R.id.ca_share_fediverse_link -> {
            Utils.shareLink(
                context,
                commentView.comment.ap_id,
            )
        }
        R.id.ca_view_source -> {
            PreviewCommentDialogFragment()
                .apply {
                    arguments = PreviewCommentDialogFragmentArgs(
                        "",
                        commentView.comment.content,
                        true,
                    ).toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "PreviewCommentDialogFragment")
        }
        R.id.ca_detailed_view -> {
            ContentDetailsDialogFragment
                .show(childFragmentManager, instance, commentView)
        }
        R.id.ca_admin_tools,
        R.id.ca_mod_tools,
        -> {
            ModActionsDialogFragment.show(commentView, childFragmentManager)
        }
        R.id.ca_reply -> {
            AddOrEditCommentFragment.showReplyDialog(
                instance = instance,
                postOrCommentView = Either.Right(commentView),
                fragmentManager = childFragmentManager,
            )
        }
        R.id.ca_block_user -> {
            actionsViewModel.blockPerson(commentView.creator.id)
        }
        R.id.ca_report_comment -> {
            ReportContentDialogFragment.show(
                childFragmentManager,
                null,
                CommentRef(
                    instance,
                    commentView.comment.id,
                ),
            )
        }
        R.id.ca_open_comment_in_new_screen -> {
            onLoadComment?.invoke(commentView.comment.id)
        }
        R.id.ca_screenshot -> {
            onScreenshotClick?.invoke()
        }
        R.id.ca_more -> {
            showMoreCommentOptions(
                instance = instance,
                commentView = commentView,
                actionsViewModel = actionsViewModel,
                fragmentManager = fragmentManager,
                onLoadComment = onLoadComment,
                onScreenshotClick = onScreenshotClick
            )
        }
    }
}
