package com.idunnololz.summit.lemmy.utils

import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.isCommunityBlocked
import com.idunnololz.summit.account.info.isPersonBlocked
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkPreviewDialogFragment
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.AdvancedLink
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.BottomMenuContainer
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.showAllowingStateLoss

fun BottomMenuContainer.showAdvancedLinkOptions(
    url: String,
    moreActionsHelper: MoreActionsHelper,
    fragmentManager: FragmentManager,
    textOrFileName: String? = null,
    mimeType: String? = null,
): BottomMenu {
    val context = this.context
    val instance = moreActionsHelper.apiInstance
    val mainActivity = this as? MainActivity

    val advancedLink = LinkUtils.analyzeLink(url, instance)

    val textOrFileName = if (textOrFileName.isNullOrBlank()) {
        null
    } else {
        textOrFileName
    }

    val bottomMenu = BottomMenu(context).apply {
        when (advancedLink) {
            is AdvancedLink.ImageLink -> {
                setTitle(
                    context.getString(
                        R.string.link_actions_format,
                        context.getString(R.string.link_type_image_link),
                    ),
                )

                if (mainActivity != null) {
                    addItemWithIcon(
                        R.id.preview_image,
                        R.string.preview_image,
                        R.drawable.baseline_image_24,
                    )
                }

                addItemWithIcon(
                    R.id.download,
                    R.string.download_image,
                    R.drawable.baseline_download_24,
                )
                addItemWithIcon(
                    R.id.share_image,
                    R.string.share_image,
                    R.drawable.baseline_share_24,
                )

                addDivider()
            }
            is AdvancedLink.OtherLink ->
                setTitle(R.string.link_actions)
            is AdvancedLink.PageLink ->
                when (val pageRef = advancedLink.pageRef) {
                    is CommentRef ->
                        setTitle(
                            context.getString(
                                R.string.link_actions_format,
                                context.getString(R.string.link_type_comment_link),
                            ),
                        )
                    is CommunityRef.CommunityRefByName -> {
                        setTitle(
                            context.getString(
                                R.string.link_actions_format,
                                context.getString(R.string.link_type_community_link),
                            ),
                        )
                        if (mainActivity != null) {
                            addItemWithIcon(
                                id = R.id.community_info,
                                title = R.string.community_info,
                                icon = R.drawable.ic_community_24,
                            )

                            val isSubbed = moreActionsHelper
                                .accountInfoManager
                                .subscribedCommunities
                                .value
                                .any { it.toCommunityRef() == pageRef }

                            if (isSubbed) {
                                addItemWithIcon(
                                    id = R.id.unsubscribe,
                                    title = R.string.unsubscribe,
                                    icon = R.drawable.baseline_subscriptions_remove_24,
                                )
                            } else {
                                addItemWithIcon(
                                    id = R.id.subscribe,
                                    title = R.string.subscribe,
                                    icon = R.drawable.baseline_subscriptions_add_24,
                                )
                            }

                            if (moreActionsHelper.fullAccount?.isCommunityBlocked(pageRef) == true) {
                                addItemWithIcon(
                                    id = R.id.unblock_community,
                                    title = context.getString(
                                        R.string.unblock_this_community_format,
                                        pageRef.name,
                                    ),
                                    icon = R.drawable.ic_community_default,
                                )
                            } else {
                                addItemWithIcon(
                                    id = R.id.block_community,
                                    title = context.getString(
                                        R.string.block_this_community_format,
                                        pageRef.name,
                                    ),
                                    icon = R.drawable.baseline_block_24,
                                )
                            }

                            addDivider()
                        }
                    }
                    is CommunityRef.Local,
                    is CommunityRef.All,
                    is CommunityRef.ModeratedCommunities,
                    is CommunityRef.Subscribed,
                    -> {
                        setTitle(
                            context.getString(
                                R.string.link_actions_format,
                                context.getString(R.string.link_type_instance_link),
                            ),
                        )
                        if (mainActivity != null) {
                            addItemWithIcon(
                                id = R.id.community_info,
                                title = R.string.instance_info,
                                icon = R.drawable.ic_community_24,
                            )

                            addDivider()
                        }
                    }
                    is CommunityRef.MultiCommunity -> setTitle(R.string.link_actions)
                    is PersonRef.PersonRefByName -> {
                        setTitle(
                            context.getString(
                                R.string.link_actions_format,
                                context.getString(R.string.link_type_user_link),
                            ),
                        )
                        if (mainActivity != null) {
                            addItemWithIcon(
                                id = R.id.user_info,
                                title = R.string.user_profile,
                                icon = R.drawable.baseline_account_circle_24,
                            )
                            addItemWithIcon(
                                id = R.id.message,
                                title = context.getString(R.string.send_message),
                                icon = R.drawable.baseline_message_24,
                            )

                            if (moreActionsHelper.fullAccount?.isPersonBlocked(pageRef) == true) {
                                addItemWithIcon(
                                    id = R.id.unblock_user,
                                    title = context.getString(
                                        R.string.unblock_this_user_format,
                                        pageRef.name,
                                    ),
                                    icon = R.drawable.baseline_person_24,
                                )
                            } else {
                                addItemWithIcon(
                                    id = R.id.block_user,
                                    title = context.getString(
                                        R.string.block_this_user_format,
                                        pageRef.name,
                                    ),
                                    icon = R.drawable.baseline_person_off_24,
                                )
                            }

//                            addItemWithIcon(
//                                id = R.id.more_user_options,
//                                title = R.string.more_user_options,
//                                icon = R.drawable.baseline_more_horiz_24
//                            )

                            addDivider()
                        }
                    }
                    is PostRef ->
                        setTitle(
                            context.getString(
                                R.string.link_actions_format,
                                context.getString(R.string.link_type_post_link),
                            ),
                        )
                }
            is AdvancedLink.VideoLink ->
                setTitle(
                    context.getString(
                        R.string.link_actions_format,
                        context.getString(R.string.link_type_video_link),
                    ),
                )
        }

        addItemWithIcon(
            R.id.copy_link,
            R.string.copy_link_address,
            R.drawable.baseline_content_copy_24,
        )
        if (textOrFileName != null) {
            addItemWithIcon(
                R.id.copy_link_text,
                R.string.copy_link_text,
                R.drawable.baseline_content_copy_24,
            )
        }
        addItemWithIcon(R.id.share_link, R.string.share_link, R.drawable.baseline_share_24)
        addItemWithIcon(
            R.id.open_in_browser,
            R.string.open_in_browser,
            R.drawable.baseline_public_24,
        )
        addItemWithIcon(
            R.id.open_link_incognito,
            R.string.open_in_incognito,
            R.drawable.ic_incognito_24,
        )
        addItemWithIcon(R.id.preview_link, R.string.preview_link, R.drawable.baseline_preview_24)

        setOnMenuItemClickListener {
            createImageOrLinkActionsHandler(
                advancedLink = advancedLink,
                moreActionsHelper = moreActionsHelper,
                fragmentManager = fragmentManager,
                textOrFileName = textOrFileName,
                mimeType = mimeType,
            )(it.id)
        }
    }
    showBottomMenu(bottomMenu, expandFully = false)

    return bottomMenu
}

fun BottomMenuContainer.createImageOrLinkActionsHandler(
    url: String,
    moreActionsHelper: MoreActionsHelper,
    fragmentManager: FragmentManager,
    textOrFileName: String? = null,
    mimeType: String? = null,
): (Int) -> Unit = createImageOrLinkActionsHandler(
    advancedLink = LinkUtils.analyzeLink(url, moreActionsHelper.apiInstance),
    moreActionsHelper = moreActionsHelper,
    fragmentManager = fragmentManager,
    textOrFileName = textOrFileName,
    mimeType = mimeType,
)

fun BottomMenuContainer.createImageOrLinkActionsHandler(
    advancedLink: AdvancedLink,
    moreActionsHelper: MoreActionsHelper,
    fragmentManager: FragmentManager,
    textOrFileName: String? = null,
    mimeType: String? = null,
): (Int) -> Unit = a@{ id: Int ->

    val mainActivity = this as? MainActivity
    val url = advancedLink.url

    val fileName = with(textOrFileName ?: url) {
        val s = substring(lastIndexOf('/') + 1)
        val indexOfDot = s.lastIndexOf('.')
        if (indexOfDot != -1) {
            s
        } else {
            run {
                val urlHasExtension = url.lastIndexOf(".") != -1
                if (urlHasExtension) {
                    s + url.substring(url.lastIndexOf("."))
                } else {
                    s
                }
            }
        }
    }

    when (id) {
        R.id.preview_image -> {
            mainActivity?.openImage(
                sharedElement = null,
                appBar = null,
                title = null,
                url = url,
                mimeType = mimeType,
            )
        }
        R.id.download -> {
            moreActionsHelper.downloadFile(
                context = context,
                destFileName = fileName,
                url = url,
                mimeType = mimeType,
            )
        }
        R.id.copy_link -> {
            Utils.copyToClipboard(context, url)
        }
        R.id.copy_link_text -> {
            Utils.copyToClipboard(context, requireNotNull(textOrFileName))
        }
        R.id.share_link -> {
            Utils.shareLink(context, url)
        }
        R.id.share_image -> {
            moreActionsHelper.downloadAndShareImage(url)
        }
        R.id.open_in_browser -> {
            onLinkClick(
                context = context,
                application = mainApplication,
                fragmentManager = fragmentManager,
                url = url,
                text = null,
                linkContext = LinkContext.Action,
            )
        }
        R.id.open_link_incognito -> {
            Utils.openExternalLink(context, url, openNewIncognitoTab = true)
        }
        R.id.preview_link -> {
            LinkPreviewDialogFragment.show(fragmentManager, url)
        }
        R.id.community_info -> {
            (advancedLink as? AdvancedLink.PageLink)?.let {
                (it.pageRef as? CommunityRef)?.let {
                    mainActivity?.showCommunityInfo(it)
                }
            }
        }
        R.id.user_info -> {
            (advancedLink as? AdvancedLink.PageLink)?.let {
                mainActivity?.launchPage(it.pageRef)
            }
        }
        R.id.message -> {
            (advancedLink as? AdvancedLink.PageLink)?.let {
                (it.pageRef as? PersonRef)?.let {
                    AddOrEditCommentFragment()
                        .apply {
                            arguments = AddOrEditCommentFragmentArgs(
                                instance = moreActionsHelper.apiInstance,
                                commentView = null,
                                postView = null,
                                editCommentView = null,
                                inboxItem = null,
                                personRef = it,
                            ).toBundle()
                        }
                        .showAllowingStateLoss(fragmentManager, "CreateOrEditPostFragment")
                }
            }
        }
        R.id.block_user,
        R.id.unblock_user,
        -> {
            (advancedLink as? AdvancedLink.PageLink)?.let {
                (it.pageRef as? PersonRef)?.let {
                    moreActionsHelper.blockPerson(it, block = id == R.id.block_user)
                }
            }
        }
        R.id.block_community,
        R.id.unblock_community,
        -> {
            (advancedLink as? AdvancedLink.PageLink)?.let {
                (it.pageRef as? CommunityRef.CommunityRefByName)?.let {
                    moreActionsHelper.blockCommunity(it, block = id == R.id.block_community)
                }
            }
        }
        R.id.more_user_options -> {
            (advancedLink as? AdvancedLink.PageLink)?.let {
                (it.pageRef as? PersonRef)?.let {
                }
            }
        }
        R.id.subscribe,
        R.id.unsubscribe,
        -> {
            (advancedLink as? AdvancedLink.PageLink)?.let {
                (it.pageRef as? CommunityRef.CommunityRefByName)?.let {
                    moreActionsHelper.updateSubscription(
                        communityRef = it,
                        subscribe = id == R.id.subscribe,
                    )
                }
            }
        }
    }
}
