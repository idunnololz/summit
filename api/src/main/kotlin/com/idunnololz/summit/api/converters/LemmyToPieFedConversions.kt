package com.idunnololz.summit.api.converters

import com.idunnololz.summit.api.dto.piefed.CreateComment
import com.idunnololz.summit.api.dto.piefed.CreatePost

fun com.idunnololz.summit.api.dto.lemmy.CreateComment.toCreateComment() =
  CreateComment(
    this.content,
    this.post_id,
    this.parent_id,
    this.language_id,
  )

fun com.idunnololz.summit.api.dto.lemmy.CreatePost.toCreatePost() =
  CreatePost(
    this.name,
    this.community_id,
    this.url,
    this.body,
    this.nsfw,
    this.language_id,
  )