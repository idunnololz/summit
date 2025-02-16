package com.idunnololz.summit.lemmy.utils

import androidx.lifecycle.lifecycleScope
import com.idunnololz.summit.R
import com.idunnololz.summit.feedback.HelpAndFeedbackDialogFragment
import com.idunnololz.summit.feedback.PostFeedbackDialogFragment
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.openAppOnPlayStore
import com.idunnololz.summit.util.startFeedbackIntent
import com.idunnololz.summit.util.summitCommunityPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

fun BaseFragment<*>.showHelpAndFeedbackOptions(
) {
    if (!isBindingAvailable()) return
    HelpAndFeedbackDialogFragment.show(childFragmentManager)
}

fun DirectoryHelper.getFeedbackScreenshotFile(): File {
    return File(saveForLaterDir, "slot_feedback_screenshot.jpg")
}