package com.idunnololz.summit.spans

import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View


class SpoilerSpan(
    private val spoilerColor: Int,
    private val spoilerRevealedColor: Int,
    private val textColor: Int
) : ClickableSpan() {

    var showSpoiler = false

    override fun onClick(widget: View) {
        //Toggle the shown state
        showSpoiler = !showSpoiler

        widget.requestLayout()
        widget.invalidate()
    }

    override fun updateDrawState(ds: TextPaint) {
        //Don't call the super method otherwise this may override our settings!
        super.updateDrawState(ds);

        ds.isUnderlineText = false
        if (showSpoiler) {
            ds.color = textColor
            ds.bgColor = spoilerRevealedColor
        } else {
            //Spoiler is not shown, make the text color the same as the background color
            ds.color = Color.TRANSPARENT
            ds.bgColor = spoilerColor
        }
    }
}