package com.idunnololz.summit.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ViewRecycler

class ThreadLinesHelper(
    private val context: Context
) {
    private val inflater = LayoutInflater.from(context)

    private val threadLineRecycler = ViewRecycler<View>()

    fun populateThreadLines(
        headerContainer: ViewGroup,
        depth: Int,
        baseDepth: Int = 0
    ) {
        check(depth >= baseDepth) { "Got depth ${depth} even though base depth was ${baseDepth}" }
        threadLineRecycler.ensureViewGroupHasChildren(headerContainer, depth - baseDepth) {
            inflater.inflate(R.layout.thread_line, headerContainer, false)
        }
    }
}