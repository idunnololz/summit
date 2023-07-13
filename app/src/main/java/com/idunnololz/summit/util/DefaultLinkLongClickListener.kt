package com.idunnololz.summit.util

import android.content.Context
import android.graphics.RectF
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.idunnololz.summit.R

class DefaultLinkLongClickListener(
    private val context: Context
) : CustomLinkMovementMethod.OnLinkLongClickListener {
    override fun onLongClick(textView: TextView, url: String, text: String, rect: RectF): Boolean {
        val popupMenu = PopupMenu(context, textView)
        popupMenu.inflate(R.menu.menu_url)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.copy_link -> {
                    Utils.copyToClipboard(context, url)
                    true
                }
                R.id.share_link -> {
                    Utils.shareText(context, url)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
        return true
    }
}