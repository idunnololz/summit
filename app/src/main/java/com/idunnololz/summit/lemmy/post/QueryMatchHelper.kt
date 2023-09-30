package com.idunnololz.summit.lemmy.post

import androidx.lifecycle.MutableLiveData
import java.lang.Math.floorMod
import javax.inject.Inject

class QueryMatchHelper @Inject constructor() {
    private var queryMatches: List<QueryResult> = listOf()
    private var currentMatchIndex: Int = 0
    val currentQueryMatch = MutableLiveData<QueryResult?>()

    val matchCount: Int
        get() = queryMatches.size

    fun setMatches(matches: List<QueryResult>) {
        queryMatches = matches
        currentMatchIndex = 0
        currentQueryMatch.value = queryMatches.firstOrNull()
    }

    fun nextMatch() {
        setCurrentMatchIndex(currentMatchIndex + 1)
    }

    fun prevMatch() {
        setCurrentMatchIndex(currentMatchIndex - 1)
    }

    private fun setCurrentMatchIndex(index: Int) {
        if (matchCount == 0) {
            return
        }

        currentMatchIndex = floorMod(index, matchCount)
        currentQueryMatch.value = queryMatches.getOrNull(currentMatchIndex)
    }

    data class QueryResult(
        val targetId: Int,
        val targetSubtype: Int,
        val relativeMatchIndex: Int,
        val itemIndex: Int,
        val matchIndex: Int,
    )

    data class HighlightTextData(
        val query: String,
        val matchIndex: Int?,
        val targetSubtype: Int?,
    )
}
