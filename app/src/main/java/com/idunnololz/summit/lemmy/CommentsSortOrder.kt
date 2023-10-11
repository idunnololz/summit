package com.idunnololz.summit.lemmy

import android.content.Context
import android.os.Parcelable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentSortType
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface CommentsSortOrder : Parcelable {

    @Parcelize
    @TypeLabel("1")
    data object Hot : CommentsSortOrder

    @Parcelize
    @TypeLabel("2")
    data object Top : CommentsSortOrder

    @Parcelize
    @TypeLabel("3")
    data object New : CommentsSortOrder

    @Parcelize
    @TypeLabel("4")
    data object Old : CommentsSortOrder
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
fun CommentSortType.toId(): Int =
    when (this) {
        CommentSortType.Hot -> R.id.sort_order_hot
        CommentSortType.Top -> R.id.sort_order_top
        CommentSortType.New -> R.id.sort_order_new
        CommentSortType.Old -> R.id.sort_order_old
    }

fun idToCommentsSortOrder(id: Int) =
    when (id) {
        R.id.sort_order_hot ->
            CommentsSortOrder.Hot
        R.id.sort_order_top ->
            CommentsSortOrder.Top
        R.id.sort_order_new ->
            CommentsSortOrder.New
        R.id.sort_order_old ->
            CommentsSortOrder.Old
        else -> null
    }
