package com.github.drjacky.imagepicker.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap

/**
 * This file was taken from
 *     https://gist.github.com/HBiSoft/15899990b8cd0723c3a894c1636550a8
 *
 * Later on it was modified from the below resource:
 *     https://raw.githubusercontent.com/iPaulPro/aFileChooser/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
 *     https://raw.githubusercontent.com/iPaulPro/aFileChooser/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
 */

object FileUriUtils {

  /**
   * Get Image Extension i.e. .png, .jpg
   *
   * @return extension of image with dot, or default .jpg if it none.
   */
  fun getImageExtension(context: Context, uriImage: Uri): String {
    var extension: String?

    extension = try {
      val mimeTypeMap = MimeTypeMap.getSingleton()
      mimeTypeMap.getExtensionFromMimeType(context.contentResolver.getType(uriImage))
    } catch (e: Exception) {
      null
    }

    if (extension.isNullOrEmpty()) {
      // default extension for matches the previous behavior of the plugin
      extension = "jpg"
    }

    return ".$extension"
  }

  fun getImageExtensionFormat(context: Context, uri: Uri): Bitmap.CompressFormat {
    val extension = getImageExtension(context, uri)
    return if (extension == ".png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
  }
}
