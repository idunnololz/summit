package com.idunnololz.summit.reddit

import android.content.Context

/**
 * Only handles post likes and not comments.
 */
class LikesManager(
    private val context: Context
) {
    companion object {

        const val NO_INFO = 0x100000

        lateinit var instance: LikesManager

        fun initialize(context: Context) {
            instance = LikesManager(context)
        }
    }

    private val likes = hashMapOf<String, Int>()
    private val pendingLikes = hashMapOf<String, Int>()

    fun setPendingLike(key: String, like: Int) {
        pendingLikes[key] = like
    }

    fun clearPendingLike(key: String) {
        pendingLikes.remove(key)
    }

    fun setLike(key: String, like: Int) {
        likes[key] = like
    }

    fun getLike(key: String): Int? =
        pendingLikes[key] ?: likes[key]
}