package com.idunnololz.summit.lemmy.utils

import com.idunnololz.summit.api.dto.lemmy.CommentView

val CommentView.upvotePercentage: Float
  get() {
    val totalCounts = counts.upvotes + counts.downvotes
    return if (totalCounts == 0) {
      0f
    } else {
      counts.upvotes / totalCounts.toFloat()
    }
  }
