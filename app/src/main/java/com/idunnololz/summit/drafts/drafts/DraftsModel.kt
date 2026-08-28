package com.idunnololz.summit.drafts.drafts

import android.os.Parcelable
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import kotlinx.parcelize.Parcelize

@Parcelize
data class DraftsModel(
  val items: List<ViewModelItem> = listOf(ViewModelItem.LoadingItem),
  val isInSelectMode: Boolean = false,
) : Parcelable

sealed interface ViewModelItem : Parcelable {

  @Parcelize
  data object HeaderItem : ViewModelItem

  @Parcelize
  data class PostDraftItem(
    val draftEntry: DraftEntry,
    val postData: DraftData.PostDraftData,
    val isSelectable: Boolean,
    val isSelected: Boolean,
  ) : ViewModelItem

  @Parcelize
  data class CommentDraftItem(
    val draftEntry: DraftEntry,
    val commentData: DraftData.CommentDraftData,
    val isSelectable: Boolean,
    val isSelected: Boolean,
  ) : ViewModelItem

  @Parcelize
  data object LoadingItem : ViewModelItem

  @Parcelize
  data object EmptyItem : ViewModelItem
}
