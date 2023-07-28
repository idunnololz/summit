package com.idunnololz.summit.util.ext

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat

fun Context.getColorFromAttribute(attribute: Int): Int {
    val attributes = obtainStyledAttributes(intArrayOf(attribute))
    val color = attributes.getColor(0, 0)
    attributes.recycle()
    return color
}

fun Context.getDimenFromAttribute(attribute: Int): Float {
    val attributes = obtainStyledAttributes(intArrayOf(attribute))
    val dimen = attributes.getDimension(0, 0f)
    attributes.recycle()
    return dimen
}

fun Context.getResIdFromAttribute(attribute: Int): Int {
    val attributes = obtainStyledAttributes(intArrayOf(attribute))
    val resourceId = attributes.getResourceId(0, 0)
    attributes.recycle()
    return resourceId
}

fun Context.getDimen(@DimenRes dimen: Int): Int {
    return resources.getDimension(dimen).toInt()
}

fun Context.getColorCompat(@ColorRes color: Int): Int =
    ContextCompat.getColor(this, color)

fun Context.getDrawableCompat(@DrawableRes drawableRes: Int): Drawable? =
    AppCompatResources.getDrawable(this, drawableRes)



fun Context.getPlainTextFromClipboard(): String? {
    val context = this
    val clipboard: ClipboardManager? =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    // If it does contain data, decide if you can handle the data.
    if (clipboard != null) {
        if (!clipboard.hasPrimaryClip()) {
        } else if (
            clipboard.primaryClipDescription
                ?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == false
        ) {
            // since the clipboard has data but it is not plain text
        } else {
            return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        }
    }

    return null
}