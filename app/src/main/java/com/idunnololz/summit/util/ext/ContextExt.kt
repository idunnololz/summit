package com.idunnololz.summit.util.ext

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.TypedValue
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.idunnololz.summit.R
import java.io.IOException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

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

fun Context.getTextSizeFromTextAppearance(attribute: Int): Float {
    val attributes = obtainStyledAttributes(
        getResIdFromAttribute(attribute),
        com.google.android.material.R.styleable.TextAppearance,
    )
    val fontSize = attributes.getDimension(
        com.google.android.material.R.styleable.TextAppearance_android_textSize,
        0f,
    )
    attributes.recycle()

    return fontSize
}

fun Context.getDimen(@DimenRes dimen: Int): Int {
    return resources.getDimension(dimen).toInt()
}

fun Context.getColorCompat(@ColorRes color: Int): Int = ContextCompat.getColor(this, color)

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

fun Context.hasInternet(): Boolean {
    var isConnected = false // Initial Value
    val connectivityManager = getSystemService(
        Context.CONNECTIVITY_SERVICE,
    ) as ConnectivityManager
    val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
    if (activeNetwork != null && activeNetwork.isConnected) {
        isConnected = true
    }
    return isConnected
}

fun Context.getLocaleListFromXml(): LocaleListCompat {
    val tagsList = mutableListOf<CharSequence>()
    try {
        val xpp: XmlPullParser = resources.getXml(R.xml.locales_config)
        while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
            if (xpp.eventType == XmlPullParser.START_TAG) {
                if (xpp.name == "locale") {
                    tagsList.add(xpp.getAttributeValue(0))
                }
            }
            xpp.next()
        }
    } catch (e: XmlPullParserException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return LocaleListCompat.forLanguageTags(tagsList.joinToString(","))
}

fun Context.getActivity(): AppCompatActivity? {
    if (this is AppCompatActivity) {
        return this
    }

    if (this is ContextWrapper) {
        val baseContext = this.baseContext
        if (baseContext is AppCompatActivity) {
            return baseContext
        }
    }
    return null
}

fun Context.spToPx(sp: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
}
