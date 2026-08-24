package com.idunnololz.summit.drafts.drafts

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DraftsViewModel @Inject constructor(
  private val lemmyClient: AccountAwareLemmyClient,
  private val draftsManager: DraftsManager,
  private val accountManager: AccountManager,
) : ViewModel() {

  companion object {
    private const val TAG = "DraftsViewModel"

    private const val LIMIT = 500
  }

  var draftType: Int? = DraftTypes.Post

  val apiInstance: String
    get() = lemmyClient.instance

  val currentAccount: Account?
    get() = accountManager.currentAccount.asAccount

  private val draftEntriesContext = Dispatchers.Default.limitedParallelism(1)

  private var selectedItems = setOf<Long>()
  private val draftEntries = mutableListOf<DraftEntry>()
  private val seenDrafts = mutableSetOf<Long>()
  private var isLoading = false
  private var hasMore = true
  private var loadingJob: Job? = null

  val model = MutableLiveData<DraftsModel>(DraftsModel())

  val selectedItemsCount
    get() = selectedItems.size

  var isInSelectMode: Boolean = false
    set(value) {
      field = value

      if (!value) {
        selectedItems = setOf()
      }

      viewModelScope.launch {
        generateItems()
      }
    }

  init {
    viewModelScope.launch {
      draftsManager.onDraftChanged.collect {
        loadMoreDrafts(force = true)
      }
    }
  }

  fun loadMoreDrafts(force: Boolean = false) {
    if (isLoading && !force) {
      return
    }

    isLoading = true

    loadingJob?.cancel()
    loadingJob = viewModelScope.launch(draftEntriesContext) {
      if (force) {
        reset()
      }

      val lastDraftEntry = draftEntries.lastOrNull()
      val ts = lastDraftEntry?.updatedTs ?: Long.MAX_VALUE

      Log.d(TAG, "Loading drafts type = $draftType from $ts")

      val draftType = draftType
      val drafts = if (draftType == null) {
        draftsManager.getAllDrafts(
          limit = LIMIT,
          updateTs = ts,
        )
      } else {
        draftsManager.getDraftsByType(
          draftType = draftType,
          limit = LIMIT,
          updateTs = ts,
        )
      }
      for (draft in drafts) {
        if (seenDrafts.add(draft.id)) {
          draftEntries.add(draft)
        }
      }
      hasMore = drafts.size == LIMIT

      Log.d(TAG, "Loaded ${drafts.size} drafts")

      generateItems()

      withContext(Dispatchers.Main) {
        isLoading = false
      }
    }
  }

  private suspend fun generateItems() {
    val items = mutableListOf<ViewModelItem>()
    var isEmpty = false

    items += ViewModelItem.HeaderItem

    withContext(draftEntriesContext) {
      isEmpty = draftEntries.isEmpty()

      for (draft in draftEntries) {
        when (draft.data) {
          is DraftData.CommentDraftData ->
            items.add(
              ViewModelItem.CommentDraftItem(
                draftEntry = draft,
                commentData = draft.data,
                isSelectable = isInSelectMode,
                isSelected = selectedItems.contains(draft.id)
              )
            )

          is DraftData.PostDraftData ->
            items.add(
              ViewModelItem.PostDraftItem(
                draftEntry = draft,
                postData = draft.data,
                isSelectable = isInSelectMode,
                isSelected = selectedItems.contains(draft.id),
              )
            )

          is DraftData.MessageDraftData -> {
            /* do nothing */
          }

          null -> {
            /* do nothing */
          }
        }
      }
    }
    if (hasMore) {
      items.add(ViewModelItem.LoadingItem)
    } else if (isEmpty) {
      items.add(ViewModelItem.EmptyItem)
    }

    model.postValue(
      model.value?.copy(
        items = items,
        isInSelectMode = isInSelectMode,
      )
    )
  }

  fun deleteDraft(draftId: Long) {
    viewModelScope.launch {
      draftsManager.deleteDraftWithId(draftId)
    }
  }

  fun deleteAllSelectedDrafts() {
    viewModelScope.launch {
      val selectedItems = selectedItems
      draftsManager.deleteDraftsWithIds(selectedItems.toList())

      isInSelectMode = false
    }
  }

  fun deleteAll(draftType: Int?) {
    viewModelScope.launch {
      reset()

      if (draftType != null) {
        draftsManager.deleteAll(draftType)
      } else {
        draftsManager.deleteAll()
      }

      isInSelectMode = false
    }
  }

  fun markItemAsSelected(itemId: Long, selected: Boolean) {
    viewModelScope.launch {
      if (selected) {
        selectedItems += itemId
      } else {
        selectedItems -= itemId
      }

      isInSelectMode = !selectedItems.isEmpty()

      generateItems()
    }
  }

  private suspend fun reset() {
    withContext(draftEntriesContext) {
      draftEntries.clear()
      seenDrafts.clear()
      isLoading = false
      hasMore = true
    }
  }
}
