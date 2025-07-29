package com.idunnololz.summit.account

const val GUEST_ACCOUNT_ID = 0L

sealed interface GuestOrUserAccount {
  val instance: String
  val id: Long
}

data class GuestAccount(
  override val instance: String,
  override val id: Long = GUEST_ACCOUNT_ID,
) : GuestOrUserAccount

val GuestOrUserAccount.key
  get() = when (this) {
    is Account -> this.fullName
    is GuestAccount -> "guest@!$^@$instance"
  }

val GuestOrUserAccount.asAccount: Account?
  get() = this as? Account
