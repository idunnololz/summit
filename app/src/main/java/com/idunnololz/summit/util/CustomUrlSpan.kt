package com.idunnololz.summit.util

import android.os.Parcel
import android.text.TextPaint
import android.text.style.URLSpan

class CustomUrlSpan : URLSpan {
    constructor(url: String?) : super(url)
    constructor(src: Parcel) : super(src)

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)

        ds.isUnderlineText = false
    }
}