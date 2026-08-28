package com.idunnololz.summit.lemmy.search

import android.app.SearchManager
import android.app.SearchableInfo
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.SearchType
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.util.INVALID_INDEX
import com.idunnololz.summit.util.getStringOrNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Remember to set [componentName]!!!
 */
@OptIn(FlowPreview::class)
class SearchSuggestionsHelper @AssistedInject constructor(
  @Assisted private val coroutineScope: CoroutineScope,
  @Assisted private val context: Context,
  private val apiClient: AccountAwareLemmyClient,
) {

  companion object {
    private const val TAG = "SearchSuggestionsHelper"
    private const val QUERY_LIMIT = 40
  }

  @AssistedFactory
  interface Factory {
    fun create(coroutineScope: CoroutineScope, application: Context): SearchSuggestionsHelper
  }

  private class IntermediateData(
    val currentQuery: String,
    var suggestions: List<String>? = null,
    var communityResults: List<CommunityView>? = null,
  )

  class Result(
    val query: String,
    val inProgress: Boolean,
    val suggestions: List<String>?,
    val communityResults: List<CommunityView>?,
  )

  private var refreshSuggestionsJob: Job? = null
  private var communityQueryJob: Job? = null

  private var intermediateData = IntermediateData("")

  private val mutex = Mutex()

  private var searchableInfo: SearchableInfo? = null

  val query = MutableStateFlow<String>("")
  var componentName = MutableStateFlow<ComponentName?>(null)
  val refreshRequest = MutableSharedFlow<Unit>()

  val result = MutableStateFlow<Result?>(null)

  init {
    coroutineScope.launch {
      query.debounce(50.milliseconds).collect {
        if (intermediateData.currentQuery != it) {
          intermediateData = IntermediateData(it)

          tryDoWork()
        }
      }
    }
    coroutineScope.launch {
      componentName.collect {
        searchableInfo = runCatching {
          val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager
          searchManager.getSearchableInfo(it)
        }.getOrNull()

        tryDoWork()
      }
    }
    coroutineScope.launch {
      refreshRequest.collect {
        tryDoWork()
      }
    }
  }

  fun clearSuggestions() {
    val searchable = searchableInfo ?: return
    val authority = searchable.suggestAuthority ?: return

    val uriBuilder = Uri.Builder()
      .scheme(ContentResolver.SCHEME_CONTENT)
      .authority(authority)
      .query("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
      .fragment("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
      .appendEncodedPath("suggestions")

    val uri = uriBuilder.build()

    // finally, make the query
    context.contentResolver.delete(uri, null, null)

    refreshRequest.tryEmit(Unit)
  }

  fun deleteSuggestion(suggestionToDelete: String) {
    val searchable = searchableInfo ?: return
    val authority = searchable.suggestAuthority ?: return

    val uriBuilder = Uri.Builder()
      .scheme(ContentResolver.SCHEME_CONTENT)
      .authority(authority)
      .query("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
      .fragment("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
      .appendEncodedPath("suggestions")

    val uri = uriBuilder.build()

    // finally, make the query
    context.contentResolver.delete(
      uri,
      "query = ?",
      arrayOf(suggestionToDelete),
    )

    refreshRequest.tryEmit(Unit)
  }

  fun runRawSuggestionsQuery(searchable: SearchableInfo?, query: String?, limit: Int): Cursor? =
    getSearchManagerSuggestions(searchable, query, limit)

  private fun tryDoWork() {
    coroutineScope.launch {
      val searchableInfo = searchableInfo ?: return@launch
      val intermediateData = intermediateData
      val currentQuery = intermediateData.currentQuery

      refreshSuggestionsJob?.cancel()
      communityQueryJob?.cancel()

      fun publishResult(inProgress: Boolean) {
        if (query.value != intermediateData.currentQuery) {
          cancel()
          return
        }

        result.value = Result(
          query = currentQuery,
          inProgress = inProgress,
          suggestions = intermediateData.suggestions,
          communityResults = intermediateData.communityResults,
        )
      }

      refreshSuggestionsJob = coroutineScope.launch(Dispatchers.IO) {
        refreshSuggestions(intermediateData, searchableInfo)
        publishResult(inProgress = true)
      }
      communityQueryJob = coroutineScope.launch(Dispatchers.IO) {
        if (currentQuery.isEmpty()) {
          return@launch
        }

        runCommunityQuery(currentQuery)
        publishResult(inProgress = true)
      }

      refreshSuggestionsJob?.join()
      communityQueryJob?.join()

      publishResult(inProgress = false)
    }
  }

  private suspend fun refreshSuggestions(
    intermediateData: IntermediateData,
    searchableInfo: SearchableInfo?,
  ) {
    val query = intermediateData.currentQuery
    val seen = mutableSetOf<String>()
    val newSuggestions = ArrayList<String>()

    runInterruptible(Dispatchers.IO) {
      // Query 2x the limit because there might be case sensitive duplicates...
      getSearchManagerSuggestions(searchableInfo, query, QUERY_LIMIT).use { c ->
        if (c != null) {
          var text1Col = INVALID_INDEX
          try {
            text1Col = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
          } catch (e: Exception) {
            Log.e(TAG, "error changing cursor and caching columns", e)
          }

          while (c.moveToNext()) {
            c.getStringOrNull(text1Col)?.let {
              if (seen.add(it.lowercase(Locale.US))) {
                newSuggestions.add(it)
                Log.d(TAG, "Got suggestion $it")
              }
            }
          }
        }
      }
    }

    mutex.withLock {
      intermediateData.suggestions = newSuggestions
    }
  }

  private suspend fun runCommunityQuery(query: String) {
    val result = withContext(Dispatchers.IO) {
      apiClient.searchWithRetry(
        sortType = SortType.TopMonth,
        listingType = ListingType.All,
        searchType = SearchType.Communities,
        query = query,
        limit = 20,
      )
    }
    result.onSuccess {
      mutex.withLock {
        intermediateData.communityResults = it.communities
      }
    }
  }

  private fun getSearchManagerSuggestions(
    searchable: SearchableInfo?,
    query: String?,
    limit: Int,
  ): Cursor? {
    if (searchable == null) {
      return null
    }
    if (query == null) {
      return null
    }

    val authority = searchable.suggestAuthority ?: return null

    val uriBuilder = Uri.Builder()
      .scheme(ContentResolver.SCHEME_CONTENT)
      .authority(authority)
      .query("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
      .fragment("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()

    // if content path provided, insert it now
    val contentPath = searchable.suggestPath
    if (contentPath != null) {
      uriBuilder.appendEncodedPath(contentPath)
    }

    // append standard suggestion query path
    uriBuilder.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY)

    // get the query selection, may be null
    val selection = searchable.suggestSelection
    // inject query, either as selection args or inline
    var selArgs: Array<String>? = null
    if (selection != null) { // use selection if provided
      selArgs = arrayOf(query)
    } else { // no selection, use REST pattern
      uriBuilder.appendPath(query)
    }

    if (limit > 0) {
      uriBuilder.appendQueryParameter("limit", limit.toString())
    }

    val uri = uriBuilder.build()

    // finally, make the query
    return context.contentResolver.query(uri, null, selection, selArgs, null)
  }
}
