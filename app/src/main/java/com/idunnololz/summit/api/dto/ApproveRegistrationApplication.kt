package com.idunnololz.summit.api.dto

data class ApproveRegistrationApplication(
    val id: Int,
    val approve: Boolean,
    val deny_reason: String? = null,
    val auth: String,
)
