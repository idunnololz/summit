package com.idunnololz.summit.api.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoginResponse(
    val jwt: String? = null,
    val registration_created: Boolean,
    val verify_email_sent: Boolean,
): Parcelable
