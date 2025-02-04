package com.idunnololz.summit.account

sealed interface GuestOrUserAccount {
    val instance: String
}

data class GuestAccount(
    override val instance: String,
) : GuestOrUserAccount
