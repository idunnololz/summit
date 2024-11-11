package com.idunnololz.summit.search

import android.content.SearchRecentSuggestionsProvider
import com.idunnololz.summit.BuildConfig

class SuggestionProvider : SearchRecentSuggestionsProvider() {

    companion object {
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.search.SuggestionProvider"
        const val MODE = DATABASE_MODE_QUERIES
    }

    init {
        setupSuggestions(AUTHORITY, MODE)
    }
}
