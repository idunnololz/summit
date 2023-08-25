package com.idunnololz.summit.actions

import android.content.Context
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.preferences.Preferences

class VotesManager(
    private val context: Context,
    private val preferences: Preferences,
) {

    companion object {
        const val NO_INFO = 0x100000
    }

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

    fun getVote(key: VotableRef): Int? =
        pendingVotes[key] ?: votes[key]

    fun setScore(key: VotableRef, count: Int) {
        scores[key] = count
    }

    fun getScore(key: VotableRef, force: Boolean = false) =
        when (key) {
            is VotableRef.CommentRef -> {
                if (!force && preferences.hideCommentScores) {
                    null
                } else {
                    scores[key]
                }
            }
            is VotableRef.PostRef -> {
                if (!force && preferences.hidePostScores) {
                    null
                } else {
                    scores[key]
                }
            }
        }

    fun setUpvotes(key: VotableRef, count: Int) {
        upvotes[key] = count
    }

    fun getUpvotes(key: VotableRef, force: Boolean = false) =
        when (key) {
            is VotableRef.CommentRef -> {
                if (!force && preferences.hideCommentScores) {
                    null
                } else {
                    upvotes[key]
                }
            }
            is VotableRef.PostRef -> {
                if (!force && preferences.hidePostScores) {
                    null
                } else {
                    upvotes[key]
                }
            }
        }

    fun setDownvotes(key: VotableRef, count: Int) {
        downvotes[key] = count
    }

    fun getDownvotes(key: VotableRef, force: Boolean = false) =
        when (key) {
            is VotableRef.CommentRef -> {
                if (!force && preferences.hideCommentScores) {
                    null
                } else {
                    downvotes[key]
                }
            }
            is VotableRef.PostRef -> {
                if (!force && preferences.hidePostScores) {
                    null
                } else {
                    downvotes[key]
                }
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
}
