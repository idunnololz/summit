package com.idunnololz.summit.api.dto.lemmy

data class RegistrationApplicationView(
  val registration_application: RegistrationApplication,
  val creator_local_user: LocalUser,
  val creator: Person,
  val admin: Person? = null,
)
