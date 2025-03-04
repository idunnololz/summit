package com.idunnololz.summit.actions

import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.preferences.Preferences

class VotesManager(
    private var preferences: Preferences,
) {

    private val votes = hashMapOf<VotableRef, Int>()
    private val pendingVotes = hashMapOf<VotableRef, Int>()

    private val scores = mutableMapOf<VotableRef, Int>()
    private val upvotes = mutableMapOf<VotableRef, Int>()
    private val downvotes = mutableMapOf<VotableRef, Int>()

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

    fun getVote(key: VotableRef): Int? = pendingVotes[key] ?: votes[key]

    fun setScore(key: VotableRef, count: Int) {
        scores[key] = count
    }

    fun getScore(key: VotableRef, force: Boolean = false) = if (shouldShowScore(key, force)) {
        scores[key]
    } else {
        null
    }

    fun setUpvotes(key: VotableRef, count: Int) {
        upvotes[key] = count
    }

    fun getUpvotes(key: VotableRef, force: Boolean = false) = if (shouldShowScore(key, force)) {
        upvotes[key]
    } else {
        null
    }

    fun setDownvotes(key: VotableRef, count: Int) {
        downvotes[key] = count
    }

    fun getDownvotes(key: VotableRef, force: Boolean = false) = if (shouldShowScore(key, force)) {
        downvotes[key]
    } else {
        null
    }

    fun shouldShowScore(key: VotableRef, force: Boolean = false) = when (key) {
        is VotableRef.CommentRef -> {
            !(!force && preferences.hideCommentScores)
        }
        is VotableRef.PostRef -> {
            !(!force && preferences.hidePostScores)
        }
    }

    fun setScoreIfNoneSet(key: VotableRef, score: Int) {
        scores.getOrPut(key) { score }
    }

    fun setUpvotesIfNoneSet(key: VotableRef, upvotes: Int) {
        this.upvotes.getOrPut(key) { upvotes }
    }

    fun setDownvotesIfNoneSet(key: VotableRef, downvotes: Int) {
        this.downvotes.getOrPut(key) { downvotes }
    }

    fun deleteScore(ref: VotableRef) {
        scores.remove(ref)
    }

    fun reset() {
        votes.clear()
        pendingVotes.clear()
        scores.clear()
        upvotes.clear()
        downvotes.clear()
    }

    fun onAccountChanged(accountPreferences: Preferences) {
        preferences = accountPreferences
        reset()
    }
}
