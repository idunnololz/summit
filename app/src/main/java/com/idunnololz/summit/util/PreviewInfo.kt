package com.idunnololz.summit.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class PreviewInfo(
    private val url: String,
    val width: Int,
    val height: Int,
) : Parcelable {

    fun getUrl(): String = Utils.fromHtml(url).toString()
}
