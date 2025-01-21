package com.idunnololz.summit.lemmy.search

import android.app.Application
import android.app.SearchManager
import android.app.SearchableInfo
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.AccountSubscription
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.SummitServerClient
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.search.SearchTabbedViewModel.CommunityFilter
import com.idunnololz.summit.lemmy.search.SearchTabbedViewModel.PersonFilter
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.getStringOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

@HiltViewModel
class SearchHomeViewModel @Inject constructor(
    private val application: Application,
    @ApplicationContext private val context: Context,
    private val accountInfoManager: AccountInfoManager,
    private val userCommunitiesManager: UserCommunitiesManager,
    private val summitServerClient: SummitServerClient,
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

    companion object {
        private const val QUERY_LIMIT = 40
        private const val TAG = "SearchHomeViewModel"
    }

    val apiInstance: String
        get() = apiClient.instance
    val currentAccountView = MutableLiveData<AccountView?>()

    val showSearch = MutableLiveData<Boolean>(false)

    val currentQueryFlow = MutableStateFlow<String>("")
    val currentSortTypeFlow = MutableStateFlow<SortType>(SortType.Active)
    val currentQueryLiveData = currentQueryFlow.asLiveData()
    val nextPersonFilter = MutableLiveData<PersonFilter?>(null)
    val nextCommunityFilter = MutableLiveData<CommunityFilter?>(null)

    var subscriptionCommunities: List<AccountSubscription> = listOf()

    val model = StatefulLiveData<SearchHomeModel>()

    private var componentName: ComponentName? = null

    init {
        viewModelScope.launch {
            accountInfoManager.currentFullAccount.collect {
                withContext(Dispatchers.Main) {
                    if (it != null) {
                        currentAccountView.value =
                            accountInfoManager.getAccountViewForAccount(it.account)
                    } else {
                        currentAccountView.value = null
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            accountInfoManager.subscribedCommunities.collect {
                subscriptionCommunities = it

                withContext(Dispatchers.Main) {
                    generateModel()
                }
            }
        }
    }

    fun updateCurrentQuery(query: String) {
        viewModelScope.launch {
            currentQueryFlow.value = query
        }
    }

    fun generateModel(componentName: ComponentName? = null, force: Boolean = false) {
        if (componentName != null) {
            this.componentName = componentName
        }

        val componentName = this.componentName ?: return

        val currentModel = model.valueOrNull
        if (currentModel != null &&
            !currentModel.isCommunitySuggestionsLoading &&
            currentModel.errors.isEmpty() &&
            !force
        ) {
            viewModelScope.launch {
                val seen = mutableSetOf<String>()
                val searchSuggestions = ArrayList<String>()
                val errors = mutableListOf<Throwable>()

                val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager
                val searchableInfo: SearchableInfo? = searchManager.getSearchableInfo(
                    componentName,
                )

                var text1Col: Int = -1

                runInterruptible(Dispatchers.IO) {
                    // Query 2x the limit because there might be case sensitive duplicates...
                    getSearchManagerSuggestions(searchableInfo, "", QUERY_LIMIT).use { c ->
                        if (c != null) {
                            try {
                                text1Col = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
                            } catch (e: Exception) {
                                Log.e(TAG, "error changing cursor and caching columns", e)
                            }

                            while (c.moveToNext()) {
                                c.getStringOrNull(text1Col)?.let {
                                    if (seen.add(it.lowercase(Locale.US))) {
                                        searchSuggestions.add(it)
                                        Log.d(TAG, "Got suggestion $it")
                                    }
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    model.setValue(
                        currentModel.copy(
                            suggestions = searchSuggestions.take(4),
                            myCommunities = subscriptionCommunities.map {
                                MyCommunity(
                                    it.toCommunityRef(),
                                    it,
                                )
                            },
                        )
                    )
                }
            }

            return
        }

        model.setIsLoading()

        viewModelScope.launch {
            val seen = mutableSetOf<String>()
            val searchSuggestions = ArrayList<String>()
            val errors = mutableListOf<Throwable>()

            val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager
            val searchableInfo: SearchableInfo? = searchManager.getSearchableInfo(
                componentName,
            )

            var text1Col: Int = -1

            runInterruptible(Dispatchers.IO) {
                // Query 2x the limit because there might be case sensitive duplicates...
                getSearchManagerSuggestions(searchableInfo, "", QUERY_LIMIT).use { c ->
                    if (c != null) {
                        try {
                            text1Col = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
                        } catch (e: Exception) {
                            Log.e(TAG, "error changing cursor and caching columns", e)
                        }

                        while (c.moveToNext()) {
                            c.getStringOrNull(text1Col)?.let {
                                if (seen.add(it.lowercase(Locale.US))) {
                                    searchSuggestions.add(it)
                                    Log.d(TAG, "Got suggestion $it")
                                }
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                model.setValue(
                    SearchHomeModel(
                        suggestions = searchSuggestions.take(4),
                        myCommunities = subscriptionCommunities.map {
                            MyCommunity(
                                it.toCommunityRef(),
                                it,
                            )
                        },
                        communitySuggestionsDto = null,
                        isCommunitySuggestionsLoading = true,
                        errors = errors,
                    ),
                )
            }

            val communitySessions = summitServerClient.communitySuggestions(force)

            communitySessions
                .onFailure {
                    errors.add(it)
                }

            withContext(Dispatchers.Main) {
                model.setValue(
                    SearchHomeModel(
                        suggestions = searchSuggestions.take(4),
                        myCommunities = subscriptionCommunities.map {
                            MyCommunity(
                                it.toCommunityRef(),
                                it,
                            )
                        },
                        communitySuggestionsDto = communitySessions.getOrNull(),
                        isCommunitySuggestionsLoading = false,
                        errors = errors,
                    ),
                )
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

    fun deleteSuggestion(componentName: ComponentName?, suggestion: String) {
        viewModelScope.launch {
            val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as? SearchManager
            val searchableInfo: SearchableInfo? = searchManager?.getSearchableInfo(componentName)
            val searchable = searchableInfo ?: return@launch
            val authority = searchable.suggestAuthority ?: return@launch

            val uriBuilder = Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .query("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
                .fragment("") // TODO: Remove, workaround for a bug in Uri.writeToParcel()
                .appendEncodedPath("suggestions")

            val uri = uriBuilder.build()

            runInterruptible {
                context.contentResolver.delete(
                    uri,
                    "query = ?",
                    arrayOf(suggestion),
                )
            }

            generateModel(componentName)
        }
    }
}
