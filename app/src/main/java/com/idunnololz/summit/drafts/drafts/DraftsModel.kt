package com.idunnololz.summit.drafts.drafts

import android.os.Parcelable
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.templates.PostAndCommentTemplatesViewModel.Item
import com.idunnololz.summit.templates.db.TemplateData
import kotlinx.parcelize.Parcelize

@Parcelize
data class DraftsModel(
  val items: List<ViewModelItem> = listOf(ViewModelItem.LoadingItem),
  val isInSelectMode: Boolean = false,
  val filter: Filter = Filter.Drafts,
) : Parcelable

enum class Filter {
  Drafts,
  Templates
}

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
  data class PostTemplateItem(
    val entryId: Long,
    val postTemplateData: TemplateData.PostTemplateData,
    val description: String,
    val isSelectable: Boolean,
    val isSelected: Boolean,
  ): ViewModelItem

  @Parcelize
  data class CommentTemplateItem(
    val entryId: Long,
    val commentTemplateData: TemplateData.CommentTemplateData,
    val description: String,
    val isSelectable: Boolean,
    val isSelected: Boolean,
  ): ViewModelItem

  @Parcelize
  data object LoadingItem : ViewModelItem

  @Parcelize
  data object EmptyItem : ViewModelItem
}
