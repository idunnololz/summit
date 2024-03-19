package com.idunnololz.summit.lemmy.utils

import androidx.fragment.app.FragmentManager
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.links.LinkPreviewDialogFragment
import com.idunnololz.summit.links.LinkType
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.GlobalSettings
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.BottomMenuContainer
import com.idunnololz.summit.util.ContentUtils
import com.idunnololz.summit.util.Utils

fun BottomMenuContainer.showMoreImageOrLinkOptions(
    url: String,
    actionsViewModel: MoreActionsViewModel,
    fragmentManager: FragmentManager,
    textOrFileName: String? = null,
    mimeType: String? = null,
): BottomMenu {
    val context = this.context

    val isImage = ContentUtils.isUrlImage(url)

    val textOrFileName = if (textOrFileName.isNullOrBlank()) {
        null
    } else {
        textOrFileName
    }

    val bottomMenu = BottomMenu(context).apply {
        if (isImage) {
            setTitle(R.string.image_actions)
        } else {
            setTitle(R.string.link_actions)
        }

        if (isImage) {
            addItemWithIcon(R.id.download, R.string.download_image, R.drawable.baseline_download_24)

            addDivider()
        }

        addItemWithIcon(R.id.copy_link, R.string.copy_link_address, R.drawable.baseline_content_copy_24)
        if (textOrFileName != null) {
            addItemWithIcon(
                R.id.copy_link_text,
                R.string.copy_link_text,
                R.drawable.baseline_content_copy_24,
            )
        }
        if (GlobalSettings.shareImagesDirectly && isImage) {
            addItemWithIcon(R.id.share_image, R.string.share_image, R.drawable.baseline_share_24)
        } else {
            addItemWithIcon(R.id.share_link, R.string.share_link, R.drawable.baseline_share_24)
        }
        addItemWithIcon(R.id.open_in_browser, R.string.open_in_browser, R.drawable.baseline_public_24)
        addItemWithIcon(R.id.open_link_incognito, R.string.open_in_incognito, R.drawable.ic_incognito_24)
        addItemWithIcon(R.id.preview_link, R.string.preview_link, R.drawable.baseline_preview_24)

        setOnMenuItemClickListener {
            createImageOrLinkActionsHandler(
                url = url,
                actionsViewModel = actionsViewModel,
                fragmentManager = fragmentManager,
                textOrFileName = textOrFileName,
                mimeType = mimeType
            )(it.id)
        }
    }
    showBottomMenu(bottomMenu, expandFully = false)

    return bottomMenu
}

fun BottomMenuContainer.createImageOrLinkActionsHandler(
    url: String,
    actionsViewModel: MoreActionsViewModel,
    fragmentManager: FragmentManager,
    textOrFileName: String? = null,
    mimeType: String? = null,
): (Int) -> Unit = a@{ id: Int ->

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
        R.id.download -> {
            actionsViewModel.downloadFile(
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
            actionsViewModel.downloadAndShareImage(url)
        }
        R.id.open_in_browser -> {
            onLinkClick(
                context = context,
                application = mainApplication,
                fragmentManager = fragmentManager,
                url = url,
                text = null,
                linkType = LinkType.Action
            )
        }
        R.id.open_link_incognito -> {
            Utils.openExternalLink(context, url, openNewIncognitoTab = true)
        }
        R.id.preview_link -> {
            LinkPreviewDialogFragment.show(fragmentManager, url)
        }
    }
}