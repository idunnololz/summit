package com.idunnololz.summit.lemmy.utils

data class StableAccountId(
    val accountId: Long,
    val accountInstance: String,
) {
    override fun toString(): String = "id%$accountId@$accountInstance"
}
