package com.idunnololz.summit.lemmy

import android.util.Log

object RateLimitManager {

    private const val TAG = "RateLimitManager"

    var isRatelimitUnknown: Boolean = true

    private var tokensRemaining: Int = -1
    private var tokensUsed: Int = -1
    private var nextResetTime: Long = -1

    fun isRateLimitHit(): Boolean {
        if (isRatelimitUnknown) {
            return false
        }

        return tokensRemaining == 0 && System.currentTimeMillis() < nextResetTime
    }

    fun onRateLimitInfoReceived(remaining: Int, used: Int, secondsRemaining: Int) {
        tokensRemaining = remaining
        tokensUsed = used
        nextResetTime = System.currentTimeMillis() + (secondsRemaining * 1000) + 1000

        Log.d(TAG, "Tokens: $tokensRemaining:$tokensUsed. Reset in ${secondsRemaining}s")
    }

    fun getTimeUntilNextRefreshMs(): Long =
        (nextResetTime - System.currentTimeMillis()).coerceAtLeast(0)
}
