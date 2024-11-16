package com.idunnololz.summit.lemmy.utils

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.LinkPreviewDialogFragment
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.crashlytics
import java.io.IOException

fun BaseFragment<*>.showMoreVideoOptions(
    url: String,
    moreActionsHelper: MoreActionsHelper,
    fragmentManager: FragmentManager,
): BottomMenu? {
    if (!isBindingAvailable()) return null

    val context = requireContext()

    val bottomMenu = BottomMenu(requireContext()).apply {
        setTitle(R.string.more_actions)

        addItemWithIcon(R.id.download, R.string.download_video, R.drawable.baseline_download_24)

        addDivider()

        addItemWithIcon(
            R.id.copy_link,
            R.string.copy_link_address,
            R.drawable.baseline_content_copy_24,
        )
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
            when (it.id) {
                R.id.download -> {
                    moreActionsHelper.downloadVideo(context, url)
                }
                R.id.copy_link -> {
                    Utils.copyToClipboard(context, url)
                }
                R.id.share_link -> {
                    Utils.shareLink(context, url)
                }
                R.id.open_in_browser -> {
                    onLinkClick(url, null, LinkContext.Action)
                }
                R.id.open_link_incognito -> {
                    Utils.openExternalLink(context, url, openNewIncognitoTab = true)
                }
                R.id.preview_link -> {
                    LinkPreviewDialogFragment.show(fragmentManager, url)
                }
            }
        }
    }
    getMainActivity()?.showBottomMenu(bottomMenu, expandFully = false)

    val observer = Observer<StatefulData<FileDownloadHelper.DownloadResult>> {
        val parent = getMainActivity() ?: return@Observer

        when (it) {
            is StatefulData.NotStarted -> {}
            is StatefulData.Error -> {
                if (it.error is FileDownloadHelper.CustomDownloadLocationException) {
                    Snackbar
                        .make(
                            parent.getSnackbarContainer(),
                            R.string.error_downloading_image,
                            Snackbar.LENGTH_LONG,
                        )
                        .setAction(R.string.downloads_settings) {
                            getMainActivity()?.showDownloadsSettings()
                        }
                        .show()
                } else {
                    crashlytics?.recordException(it.error)
                    Snackbar
                        .make(
                            parent.getSnackbarContainer(),
                            R.string.error_downloading_image,
                            Snackbar.LENGTH_LONG,
                        )
                        .show()
                }
            }
            is StatefulData.Loading -> {}
            is StatefulData.Success -> {
                try {
                    val downloadResult = it.data
                    val uri = downloadResult.uri
                    val mimeType = downloadResult.mimeType

                    val snackbarMsg = getString(R.string.video_saved_format, downloadResult.uri)
                    Snackbar
                        .make(
                            parent.getSnackbarContainer(),
                            snackbarMsg,
                            Snackbar.LENGTH_LONG,
                        )
                        .setAction(R.string.view) {
                            Utils.safeLaunchExternalIntentWithErrorDialog(
                                context,
                                childFragmentManager,
                                Intent(Intent.ACTION_VIEW).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    setDataAndType(uri, mimeType)
                                },
                            )
                        }
                        .show()
                } catch (e: IOException) {
                    /* do nothing */
                }
            }
        }
    }

    moreActionsHelper.downloadVideoResult.observe(viewLifecycleOwner, observer)

    return bottomMenu
}
