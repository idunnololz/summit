package com.idunnololz.summit.lemmy.utils

import com.idunnololz.summit.feedback.HelpAndFeedbackDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.DirectoryHelper
import java.io.File

fun BaseFragment<*>.showHelpAndFeedbackOptions() {
    if (!isBindingAvailable()) return
    HelpAndFeedbackDialogFragment.show(childFragmentManager)
}

fun DirectoryHelper.getFeedbackScreenshotFile(): File {
    return File(saveForLaterDir, "slot_feedback_screenshot.jpg")
}
