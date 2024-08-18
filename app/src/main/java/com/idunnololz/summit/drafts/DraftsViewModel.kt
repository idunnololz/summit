package com.idunnololz.summit.drafts

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DraftsViewModel @Inject constructor(
    private val draftsManager: DraftsManager,
) : ViewModel() {

    companion object {
        private const val TAG = "DraftsViewModel"

        private const val LIMIT = 500
    }

    var draftType: Int? = DraftTypes.Post

    @OptIn(ExperimentalCoroutinesApi::class)
    private val draftEntriesContext = Dispatchers.IO.limitedParallelism(1)

    private val draftEntries = mutableListOf<DraftEntry>()
    private val seenDrafts = mutableSetOf<Long>()
    private var isLoading = false
    private var hasMore = true
    private var loadingJob: Job? = null

    val viewModelItems = MutableLiveData<List<ViewModelItem>>(listOf(ViewModelItem.LoadingItem))

    fun loadMoreDrafts(force: Boolean = false) {
        if (force) {
            reset()
        }

        if (isLoading) {
            return
        }

        isLoading = true

        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
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
            withContext(draftEntriesContext) {
                for (draft in drafts) {
                    if (seenDrafts.add(draft.id)) {
                        draftEntries.add(draft)
                    }
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

        withContext(draftEntriesContext) {
            for (draft in draftEntries) {
                when (draft.data) {
                    is DraftData.CommentDraftData ->
                        items.add(ViewModelItem.CommentDraftItem(draft, draft.data))

                    is DraftData.PostDraftData ->
                        items.add(ViewModelItem.PostDraftItem(draft, draft.data))

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
        } else if (items.isEmpty()) {
            items.add(ViewModelItem.EmptyItem)
        }

        viewModelItems.postValue(items)
    }

    fun deleteDraft(entry: DraftEntry) {
        viewModelScope.launch {
            draftsManager.deleteDraft(entry)

            withContext(draftEntriesContext) {
                draftEntries.remove(entry)
            }

            generateItems()
        }
    }

    fun deleteAll(draftType: Int) {
        viewModelScope.launch {
            draftsManager.deleteAll(draftType)

            reset()
        }
    }

    private fun reset() {
        draftEntries.clear()
        seenDrafts.clear()
        isLoading = false
        hasMore = true

        loadMoreDrafts()
    }

    sealed interface ViewModelItem {
        data class PostDraftItem(
            val draftEntry: DraftEntry,
            val postData: DraftData.PostDraftData,
        ) : ViewModelItem

        data class CommentDraftItem(
            val draftEntry: DraftEntry,
            val commentData: DraftData.CommentDraftData,
        ) : ViewModelItem

        data object LoadingItem : ViewModelItem

        data object EmptyItem : ViewModelItem
    }
}
