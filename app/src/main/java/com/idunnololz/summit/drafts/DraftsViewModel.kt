package com.idunnololz.summit.drafts

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val draftEntries = mutableListOf<DraftEntry>()
    private val seenDrafts = mutableSetOf<Long>()
    private var isLoading = false
    private var hasMore = true
    private var loadingJob: Job? = null

    val viewModelItems = MutableLiveData<List<ViewModelItem>>(listOf(ViewModelItem.LoadingItem))

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

        items += ViewModelItem.HeaderItem

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

    fun deleteDraft(draftId: Long) {
        viewModelScope.launch {
            draftsManager.deleteDraftWithId(draftId)
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

    sealed interface ViewModelItem {

        data object HeaderItem : ViewModelItem

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
