package com.idunnololz.summit.api.local

import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.api.dto.lemmy.PersonId

class UserRegistrationApplication(
  val id: Int,
  val answer: String?,
  val email: String?,
  val ipAddress: String?,
  val userId: PersonId,
  val userName: String,
  val instance: String,
  val isRead: Boolean,
  val status: Status,
  val appliedAt: String? = null,
  val countryCode: String? = null,
  val throwawayEmail: Boolean? = null,
  val approvedBy: Person? = null,
  val approvedAt: String? = null,
  val referrer: String? = null,
  val denyReason: String? = null,
) {

  enum class Status {
    Approved,
    Declined,
    NoDecision,
  }
}
