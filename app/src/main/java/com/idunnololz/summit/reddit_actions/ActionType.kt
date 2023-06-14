package com.idunnololz.summit.reddit_actions

/**
 * Used to deserialize action from db.
 */
enum class ActionType constructor(val code: Int) {
    UNKNOWN(-1),
    VOTE(1),
    COMMENT(2),
    DELETE_COMMENT(3);

    companion object {
        fun fromCode(code: Int): ActionType = values().first { it.ordinal == code }
    }
}