package com.idunnololz.summit.preview

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat
import com.idunnololz.summit.lemmy.PageRef
import kotlinx.parcelize.Parcelize

sealed interface ImageViewerResult : Parcelable {
  @Parcelize
  data class ShareImage(
    val url: String,
  ) : ImageViewerResult

  @Parcelize
  data class CopyImage(
    val url: String,
  ) : ImageViewerResult

  @Parcelize
  data class OpenPage(
    val pageRef: PageRef,
  ) : ImageViewerResult

  @Parcelize
  data object ErrorCustomDownloadLocation : ImageViewerResult
}

fun ImageViewerResult.toIntent() = Intent().apply {
  putExtra("payload", this@toIntent)
}

fun Intent.toImageViewerResult() =
  IntentCompat.getParcelableExtra(this, "payload", ImageViewerResult::class.java)
