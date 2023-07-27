package com.idunnololz.summit.actions

import android.content.Context
import com.idunnololz.summit.lemmy.utils.VotableRef

class VotesManager(
    private val context: Context,
) {

    companion object {
        const val NO_INFO = 0x100000
    }

    private val votes = hashMapOf<VotableRef, Int>()
    private val pendingVotes = hashMapOf<VotableRef, Int>()

    private val scores = mutableMapOf<VotableRef, Int>()

    fun setPendingVote(key: VotableRef, like: Int) {
        pendingVotes[key] = like
    }

    fun clearPendingVotes(key: VotableRef) {
        pendingVotes.remove(key)
    }

    fun setVote(key: VotableRef, like: Int) {
        votes[key] = like
    }

    fun setVoteIfNoneSet(key: VotableRef, like: Int) {
        votes.getOrPut(key) { like }
    }

    fun getVote(key: VotableRef): Int? =
        pendingVotes[key] ?: votes[key]

    fun setScore(key: VotableRef, count: Int) {
        scores[key] = count
    }

    fun getScore(key: VotableRef) =
        scores[key]

    fun setScoreIfNoneSet(key: VotableRef, score: Int) {
        scores.getOrPut(key) { score }
    }

    fun deleteScore(ref: VotableRef) {
        scores.remove(ref)
    }

    fun reset() {
        votes.clear()
        pendingVotes.clear()
        scores.clear()
    }
}
