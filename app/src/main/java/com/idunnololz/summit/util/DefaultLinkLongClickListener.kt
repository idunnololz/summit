package com.idunnololz.summit.util

import android.content.Context
import android.graphics.RectF
import android.widget.TextView

class DefaultLinkLongClickListener(
    private val context: Context,
    private val onLinkLongClick: (url: String, text: String) -> Unit,
) : CustomLinkMovementMethod.OnLinkLongClickListener {
    override fun onLongClick(textView: TextView, url: String, text: String, rect: RectF): Boolean {
        onLinkLongClick(url, text)

        return true
    }
}