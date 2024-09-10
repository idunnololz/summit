package com.idunnololz.summit.cache

enum class CachePolicy(val value: Int) {
    Aggressive(100),
    Moderate(50),
    Lite(10),
    Minimum(0);

    companion object {
        fun parse(value: Int) =
            when (value) {
                Aggressive.value -> Aggressive
                Moderate.value -> Moderate
                Lite.value -> Lite
                Minimum.value -> Minimum
                else -> Moderate
            }
    }
}