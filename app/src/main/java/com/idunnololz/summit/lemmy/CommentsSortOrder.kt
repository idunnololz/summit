package com.idunnololz.summit.lemmy

import android.content.Context
import android.os.Parcelable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentSortType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("t")
sealed interface CommentsSortOrder : Parcelable {

    @Serializable
    @SerialName("1")
    @Parcelize
    data object Hot : CommentsSortOrder

    @Serializable
    @SerialName("2")
    @Parcelize
    data object Top : CommentsSortOrder

    @Serializable
    @SerialName("3")
    @Parcelize
    data object New : CommentsSortOrder

    @Serializable
    @SerialName("4")
    @Parcelize
    data object Old : CommentsSortOrder
}

fun CommentsSortOrder.getLocalizedName(context: Context) = when (this) {
    CommentsSortOrder.Hot -> context.getString(R.string.sort_order_hot)
    CommentsSortOrder.Top -> context.getString(R.string.sort_order_top)
    CommentsSortOrder.New -> context.getString(R.string.sort_order_new)
    CommentsSortOrder.Old -> context.getString(R.string.sort_order_old)
}

fun CommentsSortOrder.toApiSortOrder(): CommentSortType = when (this) {
    CommentsSortOrder.Hot -> CommentSortType.Hot
    CommentsSortOrder.Top -> CommentSortType.Top
    CommentsSortOrder.New -> CommentSortType.New
    CommentsSortOrder.Old -> CommentSortType.Old
}
fun CommentSortType.toId(): Int = when (this) {
    CommentSortType.Hot -> R.id.sort_order_hot
    CommentSortType.Top -> R.id.sort_order_top
    CommentSortType.New -> R.id.sort_order_new
    CommentSortType.Old -> R.id.sort_order_old
}

fun idToCommentsSortOrder(id: Int) = when (id) {
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
