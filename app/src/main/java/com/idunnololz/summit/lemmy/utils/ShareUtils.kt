package com.idunnololz.summit.lemmy.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import com.idunnololz.summit.R

fun showShareSheetForImage(
    context: Context,
    uri: Uri
) {
    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    val mimeType = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension)
        ?: "image/jpeg"
    val title = context.getString(R.string.share_image)

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        clipData = ClipData.newUri(context.contentResolver, null, uri)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(Intent.createChooser(shareIntent, title))
}