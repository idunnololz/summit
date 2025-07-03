package com.idunnololz.summit.lemmy

import android.content.Context
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.LemmyHeaderHelper.Companion.NEW_PERSON_DURATION
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.tsToConcise

interface HeaderInfo {
  val publishedTsString: String
  val editedTsString: String?
  val isAuthorCakeDay: Boolean
  val newUserTsString: String?
}

class CommentHeaderInfo(
  override val publishedTsString: String,
  override val editedTsString: String?,
  override val isAuthorCakeDay: Boolean,
  override val newUserTsString: String?,
) : HeaderInfo

fun CommentView.toCommentHeaderInfo(context: Context) =
  CommentHeaderInfo(
    publishedTsString = tsToConcise(context, this.comment.published),
    editedTsString = this.comment.updated?.let { updated ->
      tsToConcise(context, updated)
    },
    isAuthorCakeDay = this.creator.isCakeDay(),
    newUserTsString = getNewUserTsString(context,this.creator),
  )

/**
 * Information used to bind to the header UI of a post.
 */
data class PostHeaderInfo(
  override val publishedTsString: String,
  override val editedTsString: String?,
  override val isAuthorCakeDay: Boolean,
  override val newUserTsString: String?,
) : HeaderInfo

fun PostView.toPostHeaderInfo(context: Context) =
  PostHeaderInfo(
    publishedTsString = tsToConcise(context, this.post.published),
    editedTsString = this.post.updated?.let { updated ->
      tsToConcise(context, updated)
    },
    isAuthorCakeDay = this.creator.isCakeDay(),
    newUserTsString = getNewUserTsString(context,this.creator),
  )

private fun getNewUserTsString(context: Context, person: Person): String? {
  val personCreationTs = dateStringToTs(person.published)
  val isPersonNew =
    System.currentTimeMillis() - personCreationTs < NEW_PERSON_DURATION

  if (!isPersonNew) {
    return null
  }

  return tsToConcise(context, person.published)
}
