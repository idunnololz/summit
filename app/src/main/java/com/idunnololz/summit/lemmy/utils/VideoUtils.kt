package com.idunnololz.summit.lemmy.utils

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.R
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.mod.ModActionsDialogFragment
import com.idunnololz.summit.lemmy.postAndCommentView.CONFIRM_DELETE_COMMENT_TAG
import com.idunnololz.summit.lemmy.postAndCommentView.EXTRA_COMMENT_ID
import com.idunnololz.summit.lemmy.report.ReportContentDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import java.io.IOException

fun BaseFragment<*>.showMoreVideoOptions(
    url: String,
    actionsViewModel: MoreActionsViewModel,
): BottomMenu? {
    if (!isBindingAvailable()) return null

    val context = requireContext()

    val bottomMenu = BottomMenu(requireContext()).apply {
        setTitle(R.string.more_actions)

        addItemWithIcon(R.id.download, R.string.download_video, R.drawable.baseline_download_24)

        setOnMenuItemClickListener {
            when (it.id) {
                R.id.download -> {
                    actionsViewModel.downloadVideo(context, url)
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
                FirebaseCrashlytics.getInstance().recordException(it.error)
                Snackbar.make(parent.getSnackbarContainer(), R.string.error_downloading_image, Snackbar.LENGTH_LONG)
                    .show()
            }
            is StatefulData.Loading -> {}
            is StatefulData.Success -> {
                try {
                    val downloadResult = it.data
                    val uri = downloadResult.uri
                    val mimeType = downloadResult.mimeType

                    val snackbarMsg = getString(R.string.video_saved_format, downloadResult.uri)
                    Snackbar.make(
                        parent.getSnackbarContainer(),
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
                } catch (e: IOException) { /* do nothing */
                }
            }
        }
    }

    actionsViewModel.downloadVideoResult.observe(viewLifecycleOwner, observer)

    return bottomMenu
}
