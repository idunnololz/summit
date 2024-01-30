package com.idunnololz.summit.account

sealed interface GuestOrUserAccount

data class GuestAccount(
    val instance: String,
) : GuestOrUserAccount
