package com.idunnololz.summit.preferences

import com.idunnololz.summit.lemmy.utils.DEFAULT_UNKNOWN_SCORE_STRING

/**
 * Settings that are stored in global state for easy access.
 *
 * Yes this is a terrible practice, but it makes certain features possible.
 */
object GlobalSettings {

  var warnReplyToOldContentThresholdMs: Long? = null
    private set

  var stringForUnknownScore: String = DEFAULT_UNKNOWN_SCORE_STRING

  fun refresh(preferences: Preferences) {
    warnReplyToOldContentThresholdMs = if (preferences.warnReplyToOldContent) {
      preferences.warnReplyToOldContentThresholdMs
    } else {
      null
    }
    stringForUnknownScore = preferences.stringForNullScore ?: DEFAULT_UNKNOWN_SCORE_STRING
  }
}
