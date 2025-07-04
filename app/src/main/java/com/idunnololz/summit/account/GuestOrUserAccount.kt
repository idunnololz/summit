package com.idunnololz.summit.account

sealed interface GuestOrUserAccount {
  val instance: String
}

data class GuestAccount(
  override val instance: String,
) : GuestOrUserAccount

val GuestOrUserAccount.key
  get() = when (this) {
    is Account -> this.fullName
    is GuestAccount -> "guest@!$^@$instance"
  }

val GuestOrUserAccount.asAccount: Account?
  get() = this as? Account