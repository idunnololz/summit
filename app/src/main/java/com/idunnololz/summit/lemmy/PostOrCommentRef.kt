package com.idunnololz.summit.lemmy

import android.os.Parcelable
import arrow.core.Either
import kotlinx.parcelize.Parcelize

sealed interface PostOrCommentRef : Parcelable {
  @Parcelize
  data class PostRef(
    val ref: com.idunnololz.summit.lemmy.PostRef,
  ) : PostOrCommentRef

  @Parcelize
  data class CommentRef(
    val ref: com.idunnololz.summit.lemmy.CommentRef,
  ) : PostOrCommentRef
}

fun PostOrCommentRef.toEither(): Either<PostRef, CommentRef> = when (this) {
  is PostOrCommentRef.CommentRef ->
    Either.Right(ref)
  is PostOrCommentRef.PostRef ->
    Either.Left(ref)
}
