package com.idunnololz.summit.reddit

import android.content.Context
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentSortType

enum class CommentsSortOrder(
    val key: String
) {
    Hot("hot"),
    Top("top"),
    New("new"),
    Old("old"),
}

fun CommentsSortOrder.getLocalizedName(context: Context) =
    when (this) {
        CommentsSortOrder.Hot -> context.getString(R.string.sort_order_hot)
        CommentsSortOrder.Top -> context.getString(R.string.sort_order_top)
        CommentsSortOrder.New -> context.getString(R.string.sort_order_new)
        CommentsSortOrder.Old -> context.getString(R.string.sort_order_old)
    }

fun CommentsSortOrder.toApiSortOrder(): CommentSortType =
    when (this) {
        CommentsSortOrder.Hot -> CommentSortType.Hot
        CommentsSortOrder.Top -> CommentSortType.Top
        CommentsSortOrder.New -> CommentSortType.New
        CommentsSortOrder.Old -> CommentSortType.Old
    }