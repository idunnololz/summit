package com.idunnololz.summit.lemmy.utils.actions

import android.net.Uri

interface DownloadAndShareFileListener {
  fun onComplete(fileUri: Uri): Boolean
}
