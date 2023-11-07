package com.idunnololz.summit.preferences

/**
 * Settings that are stored in global state for easy access.
 */
object GlobalSettings {

    var shareImagesDirectly: Boolean = false
        private set

    var warnReplyToOldContentThresholdMs: Long? = null
        private set

    fun refresh(preferences: Preferences) {
        shareImagesDirectly = preferences.shareImagesDirectly
        warnReplyToOldContentThresholdMs = if (preferences.warnReplyToOldContent) {
            preferences.warnReplyToOldContentThresholdMs
        } else {
            null
        }
    }
}