package com.idunnololz.summit.lemmy.post

import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.lemmy.CommentNodeData
import com.idunnololz.summit.models.PostView

data class PostModel(
  val postListView: PostListItem.PostListView,
  val commentTree: List<CommentNodeData>,
  val crossPosts: List<PostView>,
  val newlyPostedCommentId: CommentId?,
  val selectedCommentId: CommentId?,
  val isSingleCommentChain: Boolean,
  val isNativePost: Boolean,
  val accountInstance: String?,
  val isCommentsLoaded: Boolean,
  val commentPath: String?,
  val wasUpdateForced: Boolean,
  val loadCommentError: Throwable?,
)