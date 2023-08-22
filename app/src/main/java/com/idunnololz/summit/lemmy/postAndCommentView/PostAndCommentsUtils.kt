package com.idunnololz.summit.lemmy.postAndCommentView

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.fastAccountSwitcher.FastAccountSwitcherDialogFragment
import com.idunnololz.summit.lemmy.mod.BanUserDialogFragment
import com.idunnololz.summit.lemmy.mod.BanUserDialogFragmentArgs
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        },
    )
}

fun BaseFragment<*>.showMoreCommentOptions(
    instance: String,
    commentView: CommentView,
    actionsViewModel: MoreActionsViewModel,
    fragmentManager: FragmentManager,
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

    val bottomMenu = BottomMenu(requireContext()).apply {
        setTitle(R.string.more_actions)

        addItemWithIcon(R.id.reply, R.string.reply, R.drawable.baseline_reply_24)

        if (commentView.creator.id == currentAccount?.id) {
            addItemWithIcon(R.id.edit_comment, R.string.edit_comment, R.drawable.baseline_edit_24)
            addItemWithIcon(R.id.delete_comment, R.string.delete_comment, R.drawable.baseline_delete_24)
        }
        if (commentView.saved) {
            addItemWithIcon(R.id.remove_from_saved, R.string.remove_from_saved, R.drawable.baseline_bookmark_remove_24)
        } else {
            addItemWithIcon(R.id.save, R.string.save, R.drawable.baseline_bookmark_add_24)
        }

        val fullAccount = actionsViewModel.accountInfoManager.currentFullAccount.value
        if (fullAccount
                ?.accountInfo
                ?.miscAccountInfo
                ?.modCommunityIds
                ?.contains(commentView.community.id) == true
        ) {
            addDivider()

            addItemWithIcon(
                id = R.id.mod_tools,
                title = R.string.mod_tools,
                icon = R.drawable.outline_shield_24
            )

            addDivider()
        }

        addItemWithIcon(R.id.share, R.string.share, R.drawable.baseline_share_24)
        addItemWithIcon(
            R.id.share_fediverse_link,
            getString(R.string.share_source_link),
            R.drawable.ic_fediverse_24,
        )

        addDivider()
        addItemWithIcon(
            R.id.block_user,
            getString(R.string.block_this_user_format, commentView.creator.name),
            R.drawable.baseline_person_off_24,
        )
        addItemWithIcon(
            R.id.report_comment,
            getString(R.string.report_comment),
            R.drawable.baseline_outlined_flag_24,
        )
        addDivider()

        addItemWithIcon(R.id.view_source, R.string.view_source, R.drawable.baseline_code_24)

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
                        LinkUtils.getLinkForComment(instance, commentView.comment.id),
                    )
                }
                R.id.share_fediverse_link -> {
                    Utils.shareLink(
                        context,
                        commentView.comment.ap_id,
                    )
                }
                R.id.view_source -> {
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
                R.id.mod_tools -> {
                    ModActionsDialogFragment.show(commentView, childFragmentManager)
                }
                R.id.reply -> {
                    AddOrEditCommentFragment().apply {
                        arguments = AddOrEditCommentFragmentArgs(
                            instance = instance,
                            commentView = commentView,
                            postView = null,
                            editCommentView = null,
                        ).toBundle()
                    }.show(childFragmentManager, "asdf")
                }
                R.id.block_user -> {
                    actionsViewModel.blockPerson(commentView.creator.id)
                }
                R.id.report_comment -> {
                    ReportContentDialogFragment.show(
                        childFragmentManager,
                        null,
                        CommentRef(
                            instance,
                            commentView.comment.id
                        ),
                    )
                }
            }
        }
    }
    getMainActivity()?.showBottomMenu(bottomMenu, expandFully = false)

    return bottomMenu
}