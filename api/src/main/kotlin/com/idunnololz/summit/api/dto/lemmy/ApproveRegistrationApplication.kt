package com.idunnololz.summit.api.dto.lemmy

data class ApproveRegistrationApplication(
  val id: Int,
  val approve: Boolean,
  val deny_reason: String? = null,
)
