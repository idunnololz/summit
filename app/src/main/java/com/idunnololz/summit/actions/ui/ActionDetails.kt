package com.idunnololz.summit.actions.ui

import android.os.Parcelable
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import kotlinx.parcelize.Parcelize

sealed interface ActionDetails : Parcelable {
    @Parcelize
    data object SuccessDetails : ActionDetails

    @Parcelize
    data object PendingDetails : ActionDetails

    @Parcelize
    data class FailureDetails(
        val reason: LemmyActionFailureReason,
    ) : ActionDetails
}
