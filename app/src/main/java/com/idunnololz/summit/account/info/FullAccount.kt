package com.idunnololz.summit.account.info

import com.idunnololz.summit.account.Account

data class FullAccount(
    val account: Account,
    val accountInfo: AccountInfo,
) {
    val accountId =
        account.id
}
