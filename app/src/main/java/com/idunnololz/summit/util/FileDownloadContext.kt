package com.idunnololz.summit.util

import android.os.Parcelable
import arrow.core.Either
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toCommunityRef
import kotlinx.parcelize.Parcelize

/**
 * Provides context about a file that is relevant for download.
 */
@Parcelize
data class FileDownloadContext(
  val communityRef: CommunityRef?,
): Parcelable

fun PostView.toFileDownloadContext() =
  FileDownloadContext(
    community.toCommunityRef(),
  )

fun CommentView.toFileDownloadContext() =
  FileDownloadContext(
    community.toCommunityRef(),
  )

fun Either<PostView, CommentView>.toFileDownloadContext() =
  FileDownloadContext(
    fold(
      {
        it.community.toCommunityRef()
      },
      {
        it.community.toCommunityRef()
      }
    ),
  )