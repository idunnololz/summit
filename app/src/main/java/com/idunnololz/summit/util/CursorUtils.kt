package com.idunnololz.summit.util

import android.database.Cursor
import android.util.Log

const val INVALID_INDEX = -1

fun Cursor.getStringOrNull(col: Int): String? {
    if (col == INVALID_INDEX) {
        return null
    }

    return try {
        getString(col)
    } catch (e: Exception) {
        Log.e(
            "CursorUtils",
            "unexpected error retrieving valid column from cursor, " +
                "did the remote process die?",
            e,
        )
        null
    }
}
