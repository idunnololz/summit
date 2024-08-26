package com.idunnololz.summit.lemmy.postListView

import androidx.fragment.app.FragmentManager
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.contentDetails.ContentDetailsDialogFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragment
import com.idunnololz.summit.lemmy.createOrEditPost.CreateOrEditPostFragmentArgs
import com.idunnololz.summit.lemmy.fastAccountSwitcher.FastAccountSwitcherDialogFragment
import com.idunnololz.summit.lemmy.mod.ModActionsDialogFragment
import com.idunnololz.summit.lemmy.report.ReportContentDialogFragment
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.showAllowingStateLoss

fun BaseFragment<*>.showMorePostOptions(
    instance: String,
    accountId: Long?,
    postView: PostView,
    moreActionsHelper: MoreActionsHelper,
    fragmentManager: FragmentManager,
    isPostMenu: Boolean = false,
    onSortOrderClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onFindInPageClick: () -> Unit = {},
    onScreenshotClick: (() -> Unit)? = null,
) {
    if (!isBindingAvailable()) {
        return
    }

    val context = requireContext()

    val bottomMenu = BottomMenu(context).apply {
        setTitle(R.string.more_actions)

        addItemWithIcon(
            R.id.pa_reply,
            getString(R.string.add_comment),
            R.drawable.baseline_add_comment_24,
        )

        if (isPostMenu) {
            addDivider()
            addItemWithIcon(
                R.id.sort_comments_by,
                R.string.sort_comments_by,
                R.drawable.baseline_sort_24,
            )
            addItemWithIcon(
                R.id.refresh,
                R.string.refresh,
                R.drawable.baseline_refresh_24,
            )
            addItemWithIcon(
                R.id.find_in_page,
                R.string.find_in_page,
                R.drawable.outline_find_in_page_24,
            )
            addDivider()
        }

        if (postView.post.creator_id == moreActionsHelper.accountManager.currentAccount.asAccount?.id) {
            addItemWithIcon(R.id.edit_post, R.string.edit_post, R.drawable.baseline_edit_24)

            if (postView.post.deleted) {
                addItemWithIcon(
                    R.id.delete_undo,
                    R.string.restore_post,
                    R.drawable.baseline_delete_24,
                )
            } else {
                addItemWithIcon(R.id.delete, R.string.delete_post, R.drawable.baseline_delete_24)
            }
        }

        val fullAccount = moreActionsHelper.accountInfoManager.currentFullAccount.value
        val miscAccountInfo = fullAccount
            ?.accountInfo
            ?.miscAccountInfo

        if (instance == fullAccount?.account?.instance &&
            miscAccountInfo?.isAdmin == true
        ) {
            addDivider()

            addItemWithIcon(
                id = R.id.pa_admin_tools,
                title = R.string.admin_tools,
                icon = R.drawable.outline_shield_24,
            )

            addDivider()
        } else if (miscAccountInfo
                ?.modCommunityIds
                ?.contains(postView.community.id) == true
        ) {
            addDivider()

            addItemWithIcon(
                id = R.id.pa_mod_tools,
                title = R.string.mod_tools,
                icon = R.drawable.outline_shield_24,
            )

            addDivider()
        }

        addItemWithIcon(
            R.id.hide_post,
            getString(R.string.hide_post),
            R.drawable.baseline_hide_24,
        )

        if (postView.read) {
            addItemWithIcon(
                R.id.pa_mark_post_as_unread,
                getString(R.string.mark_as_unread),
                R.drawable.outline_thread_unread_24,
            )
        } else {
            addItemWithIcon(
                R.id.pa_mark_post_as_read,
                getString(R.string.mark_as_read),
                R.drawable.baseline_check_24,
            )
        }

        if (postView.saved) {
            addItemWithIcon(
                R.id.pa_remove_from_saved,
                getString(R.string.remove_from_saved),
                R.drawable.baseline_bookmark_remove_24,
            )
        } else {
            addItemWithIcon(
                R.id.pa_save,
                getString(R.string.save),
                R.drawable.baseline_bookmark_add_24,
            )
        }

        addItemWithIcon(
            R.id.pa_share,
            getString(R.string.share),
            R.drawable.baseline_share_24,
        )

        if (onScreenshotClick != null) {
            addItemWithIcon(
                R.id.pa_screenshot,
                getString(R.string.take_screenshot),
                R.drawable.baseline_screenshot_24,
            )
        }

        addItemWithIcon(
            R.id.pa_cross_post,
            getString(R.string.cross_post),
            R.drawable.baseline_content_copy_24,
        )

        addItemWithIcon(
            R.id.pa_share_fediverse_link,
            getString(R.string.share_source_link),
            R.drawable.ic_fediverse_24,
        )

        addItemWithIcon(
            R.id.pa_community_info,
            getString(R.string.community_info),
            R.drawable.ic_community_24,
        )

        addDivider()
        addItemWithIcon(
            R.id.report_post,
            getString(R.string.report_post),
            R.drawable.baseline_outlined_flag_24,
        )
        addItemWithIcon(
            R.id.pa_block_user,
            getString(R.string.block_this_user_format, postView.creator.name),
            R.drawable.baseline_person_off_24,
        )
        addItemWithIcon(
            R.id.block_community,
            getString(R.string.block_this_community_format, postView.community.name),
            R.drawable.baseline_block_24,
        )
        addItemWithIcon(
            R.id.block_instance,
            getString(R.string.block_this_instance_format, postView.instance),
            R.drawable.baseline_public_off_24,
        )
        addDivider()
//        addItemWithIcon(
//            R.id.switch_account_temp,
//            R.string.switch_account_for_post,
//            R.drawable.baseline_account_circle_24)
        addItemWithIcon(R.id.pa_view_source, R.string.view_raw, R.drawable.baseline_code_24)
        addItemWithIcon(
            R.id.pa_detailed_view,
            R.string.detailed_view,
            R.drawable.baseline_open_in_full_24,
        )

        setOnMenuItemClickListener {
            createPostActionHandler(
                instance = instance,
                accountId = accountId,
                postView = postView,
                moreActionsHelper = moreActionsHelper,
                fragmentManager = fragmentManager,
                onSortOrderClick = onSortOrderClick,
                onRefreshClick = onRefreshClick,
                onFindInPageClick = onFindInPageClick,
                onScreenshotClick = onScreenshotClick,
            )(it.id)
        }
    }

    getMainActivity()?.showBottomMenu(bottomMenu, expandFully = false)
}

fun BaseFragment<*>.createPostActionHandler(
    instance: String,
    accountId: Long?,
    postView: PostView,
    moreActionsHelper: MoreActionsHelper,
    fragmentManager: FragmentManager,
    onSortOrderClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onFindInPageClick: () -> Unit = {},
    onScreenshotClick: (() -> Unit)? = null,
): (Int) -> Unit = a@{ id: Int ->
    val context = requireContext()

    when (id) {
        R.id.pa_reply -> {
            if (moreActionsHelper.accountManager.currentAccount.value == null) {
                PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                    .show(childFragmentManager, "PreAuthDialogFragment")
                return@a
            }

            AddOrEditCommentFragment.showReplyDialog(
                instance = instance,
                postOrCommentView = Either.Left(postView),
                fragmentManager = childFragmentManager,
                accountId = accountId,
            )
        }
        R.id.edit_post -> {
            CreateOrEditPostFragment()
                .apply {
                    arguments = CreateOrEditPostFragmentArgs(
                        instance = moreActionsHelper.apiInstance,
                        post = postView.post,
                        communityName = null,
                    ).toBundle()
                }
                .showAllowingStateLoss(childFragmentManager, "CreateOrEditPostFragment")
        }
        R.id.delete -> {
            moreActionsHelper.deletePost(postView.post.id, delete = true)
        }
        R.id.delete_undo -> {
            moreActionsHelper.deletePost(postView.post.id, delete = false)
        }
        R.id.hide_post -> {
            moreActionsHelper.hidePost(postView.post.id)
        }
        R.id.pa_save_toggle -> {
            if (postView.saved) {
                moreActionsHelper.savePost(postView.post.id, save = false, accountId = accountId)
            } else {
                moreActionsHelper.savePost(postView.post.id, save = true, accountId = accountId)
            }
        }
        R.id.pa_save -> {
            moreActionsHelper.savePost(postView.post.id, save = true, accountId = accountId)
        }
        R.id.pa_remove_from_saved -> {
            moreActionsHelper.savePost(postView.post.id, save = false, accountId = accountId)
        }
        R.id.pa_community_info -> {
            getMainActivity()?.showCommunityInfo(postView.community.toCommunityRef())
        }
        R.id.block_community -> {
            moreActionsHelper.blockCommunity(postView.community.id)
        }
        R.id.pa_block_user -> {
            moreActionsHelper.blockPerson(postView.creator.id)
        }
        R.id.block_instance -> {
            moreActionsHelper.blockInstance(postView.community.instance_id)
        }
        R.id.pa_share -> {
            Utils.shareLink(
                context = context,
                link = LinkUtils.getLinkForPost(instance, postView.post.id),
            )
        }
        R.id.pa_cross_post -> {
            CreateOrEditPostFragment()
                .apply {
                    arguments = CreateOrEditPostFragmentArgs(
                        instance = moreActionsHelper.apiInstance,
                        post = null,
                        communityName = null,
                        crosspost = postView.post,
                    ).toBundle()
                }
                .showAllowingStateLoss(childFragmentManager, "CreateOrEditPostFragment")
        }
        R.id.pa_share_fediverse_link -> {
            Utils.shareLink(
                context = context,
                link = postView.post.ap_id,
            )
        }
        R.id.pa_view_source -> {
            PreviewCommentDialogFragment()
                .apply {
                    arguments = PreviewCommentDialogFragmentArgs(
                        "",
                        buildString {
                            appendLine("Title:")
                            appendLine(postView.post.name)
                            appendLine()
                            appendLine("Body:")
                            appendLine(postView.post.body ?: "")
                            appendLine()
                            appendLine("Url:")
                            appendLine(postView.post.url ?: "")
                        },
                        true,
                    ).toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "PreviewCommentDialogFragment")
        }
        R.id.pa_detailed_view -> {
            ContentDetailsDialogFragment
                .show(childFragmentManager, instance, postView)
        }
        R.id.pa_admin_tools -> {
            ModActionsDialogFragment.show(postView, childFragmentManager)
        }
        R.id.pa_mod_tools -> {
            ModActionsDialogFragment.show(postView, childFragmentManager)
        }
        R.id.report_post -> {
            ReportContentDialogFragment.show(
                childFragmentManager,
                PostRef(
                    instance,
                    postView.post.id,
                ),
                null,
            )
        }
        R.id.switch_account_temp -> {
            FastAccountSwitcherDialogFragment.show(childFragmentManager)
        }
        R.id.sort_comments_by -> {
            onSortOrderClick()
        }
        R.id.refresh -> {
            onRefreshClick()
        }
        R.id.find_in_page -> {
            onFindInPageClick()
        }
        R.id.pa_screenshot -> {
            onScreenshotClick?.invoke()
        }
        R.id.pa_more -> {
            showMorePostOptions(
                instance = instance,
                accountId = accountId,
                postView = postView,
                moreActionsHelper = moreActionsHelper,
                fragmentManager = fragmentManager,
                onSortOrderClick = onSortOrderClick,
                onRefreshClick = onRefreshClick,
                onFindInPageClick = onFindInPageClick,
                onScreenshotClick = onScreenshotClick,
            )
        }
        R.id.pa_mark_post_as_unread -> {
            moreActionsHelper.onPostRead(
                postView = postView,
                delayMs = 0,
                read = false,
                accountId = accountId,
            )
        }
        R.id.pa_mark_post_as_read -> {
            moreActionsHelper.onPostRead(
                postView = postView,
                delayMs = 0,
                read = true,
                accountId = accountId,
            )
        }
    }
}
