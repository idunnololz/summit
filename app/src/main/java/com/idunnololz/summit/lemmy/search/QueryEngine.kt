package com.idunnololz.summit.lemmy.search

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.ApiFeature
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.PersonView
import com.idunnololz.summit.api.dto.lemmy.SearchType
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.api.toLemmyPageIndex
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentHeaderInfo
import com.idunnololz.summit.lemmy.PostHeaderInfo
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.Source.*
import com.idunnololz.summit.lemmy.search.QueryEngine.QueryResultsPage.*
import com.idunnololz.summit.lemmy.search.QueryEngine.SearchResultView.*
import com.idunnololz.summit.lemmy.toCommentHeaderInfo
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.lemmy.utils.listSource.CursorBackedSingleDataSource
import com.idunnololz.summit.lemmy.utils.listSource.LemmyListSource
import com.idunnololz.summit.lemmy.utils.listSource.Page
import com.idunnololz.summit.lemmy.utils.listSource.SimpleDataSource
import com.idunnololz.summit.lemmy.utils.listSource.fold
import com.idunnololz.summit.models.PostView
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.qualifiers.ApplicationContext
import info.debatty.java.stringsimilarity.NGram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Item {
  data class PostItem(
    val fetchedPost: FetchedPost,
    val instance: String,
    val pageIndex: Int,
    val postHeaderInfo: PostHeaderInfo,
  ) : Item()
  data class CommentItem(
    val commentView: CommentView,
    val instance: String,
    val pageIndex: Int,
    val commentHeaderInfo: CommentHeaderInfo,
  ) : Item()
  data class CommunityItem(
    val communityView: CommunityView,
    val instance: String,
    val pageIndex: Int,
  ) : Item()
  data class UserItem(
    val personView: PersonView,
    val instance: String,
    val pageIndex: Int,
  ) : Item()

  data class AutoLoadItem(
    val pageToLoad: Int,
  ) : Item()

  data class ErrorItem(
    val error: Throwable,
    val pageToLoad: Int,
  ) : Item()

  data object EndItem : Item()
  data object FooterSpacerItem : Item()
}

class QueryEngine(
  @ApplicationContext private val context: Context,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val apiClient: AccountAwareLemmyClient,
  private val type: SearchType,
) {

  class SearchSource(
    context: Context,
    simpleDataSource: SimpleDataSource<SearchResultView, SortType>
  ) : LemmyListSource<SearchResultView, SortType, Long>(
    context,
    { id },
    SortType.New,
    {
      pageIndex: Int,
      sortOrder: SortType,
      limit: Int,
      force: Boolean ->

      simpleDataSource.fetchItems(sortOrder, pageIndex, force, limit = limit, showRead = true)
    },
  )

  class PagedSearchSource(
    private val apiClient: AccountAwareLemmyClient,
    private val trigram: NGram,
    private val communityIdFilter: Int?,
    private val currentSortType: SortType,
    private val listingTypeFilter: ListingType?,
    private val type: SearchType,
    private val currentQuery: String,
    private val personIdFilter: Long?,
  ) : SimpleDataSource<SearchResultView, SortType> {
    override suspend fun fetchItems(
      sortOrder: SortType?,
      page: Int,
      force: Boolean,
      limit: Int,
      showRead: Boolean?,
    ): Result<List<SearchResultView>> {
      return apiClient
        .searchWithRetry(
          communityId = communityIdFilter,
          communityName = null,
          sortType = currentSortType,
          listingType = listingTypeFilter ?: ListingType.All,
          searchType = type,
          page = page.toLemmyPageIndex(),
          query = currentQuery,
          limit = limit,
          creatorId = personIdFilter,
          force = force,
        )
        .map {
          val items = mutableListOf<SearchResultView>()

          it.comments.mapTo(items) { CommentResultView(it) }
          it.posts.mapTo(items) { PostResultView(FetchedPost(it, StandardSource())) }
          it.communities.mapTo(items) { CommunityResultView(it) }
          it.users.mapTo(items) { UserResultView(it) }

          val sortedItems =
            if (currentSortType == SortType.Active) {
              items.sortedBy {
                when (it) {
                  is CommentResultView ->
                    trigram.distance(
                      it.commentView.comment.content,
                      currentQuery,
                    )
                  is CommunityResultView -> {
                    trigram.distance(
                      it.communityView.community.name,
                      currentQuery,
                    )
                  }
                  is PostResultView -> {
                    val post = it.fetchedPost.postView.post
                    val toMatch = post.name + " " + post.body
                    trigram.distance(toMatch, currentQuery)
                  }
                  is UserResultView ->
                    trigram.distance(
                      it.personView.person.name,
                      currentQuery,
                    )
                }
              }
            } else {
              items
            }

          sortedItems
        }
    }
  }

  class CursorSearchSource(
    private val apiClient: AccountAwareLemmyClient,
    private val trigram: NGram,
    private val communityIdFilter: Int?,
    private val currentSortType: SortType,
    private val listingTypeFilter: ListingType?,
    private val type: SearchType,
    private val currentQuery: String,
    private val personIdFilter: Long?,
  ) : CursorBackedSingleDataSource<SearchResultView, SortType>(
    fetchObjects = {
      pageCursor: String?,
      sortOrder: SortType?,
      limit: Int,
      force: Boolean,
      showRead: Boolean?, ->

      apiClient
        .searchWithRetry(
          communityId = communityIdFilter,
          communityName = null,
          sortType = currentSortType,
          listingType = listingTypeFilter ?: ListingType.All,
          searchType = type,
          query = currentQuery,
          limit = limit,
          creatorId = personIdFilter,
          pageCursor = pageCursor,
          force = force,
        )
        .map {
          val items = mutableListOf<SearchResultView>()

          it.comments.mapTo(items) { CommentResultView(it) }
          it.posts.mapTo(items) { PostResultView(FetchedPost(it, StandardSource())) }
          it.communities.mapTo(items) { CommunityResultView(it) }
          it.users.mapTo(items) { UserResultView(it) }

          val sortedItems =
            if (currentSortType == SortType.Active) {
              items.sortedBy {
                when (it) {
                  is CommentResultView ->
                    trigram.distance(
                      it.commentView.comment.content,
                      currentQuery,
                    )
                  is CommunityResultView -> {
                    trigram.distance(
                      it.communityView.community.name,
                      currentQuery,
                    )
                  }
                  is PostResultView -> {
                    val post = it.fetchedPost.postView.post
                    val toMatch = post.name + " " + post.body
                    trigram.distance(toMatch, currentQuery)
                  }
                  is UserResultView ->
                    trigram.distance(
                      it.personView.person.name,
                      currentQuery,
                    )
                }
              }
            } else {
              items
            }

          Page(
            items = sortedItems,
            nextCursor = it.nextCursor,
          )
        }
    }
  )

  companion object {
    private const val TAG = "QueryEngine"

    private const val MAX_QUERY_PAGE_LIMIT = 20
  }

  sealed interface SearchResultView {

    val id: Long

    data class PostResultView(
      val fetchedPost: FetchedPost,
    ) : SearchResultView {
      override val id: Long
        get() = fetchedPost.postView.post.id.toLong()
    }

    data class CommentResultView(
      val commentView: CommentView,
    ) : SearchResultView {
      override val id: Long
        get() = commentView.comment.id.toLong()
    }

    data class CommunityResultView(
      val communityView: CommunityView,
    ) : SearchResultView {
      override val id: Long
        get() = communityView.community.id.toLong()
    }

    data class UserResultView(
      val personView: PersonView,
    ) : SearchResultView {
      override val id: Long
        get() = personView.person.id
    }

  }

  sealed interface QueryResultsPage {

    val pageIndex: Int
    val hasMore: Boolean

    data class AllResultsPage(
      val results: List<SearchResultView>,
      override val pageIndex: Int,
      override val hasMore: Boolean,
    ) : QueryResultsPage

    data class UrlResultsPage(
      val results: List<SearchResultView>,
      override val pageIndex: Int,
      override val hasMore: Boolean,
    ) : QueryResultsPage

    data class PostResultsPage(
      val results: List<PostView>,
      override val pageIndex: Int,
      override val hasMore: Boolean,
    ) : QueryResultsPage

    data class CommentResultsPage(
      val results: List<CommentView>,
      override val pageIndex: Int,
      override val hasMore: Boolean,
    ) : QueryResultsPage

    data class CommunityResultsPage(
      val results: List<CommunityView>,
      override val pageIndex: Int,
      override val hasMore: Boolean,
    ) : QueryResultsPage

    data class UserResultsPage(
      val results: List<PersonView>,
      override val pageIndex: Int,
      override val hasMore: Boolean,
    ) : QueryResultsPage

    data class ErrorPage(
      val error: Throwable,
      override val pageIndex: Int,
      override val hasMore: Boolean,
    ) : QueryResultsPage
  }

  val pageCount
    get() = pages.size
  val currentState = MutableStateFlow<StatefulData<Unit>>(StatefulData.NotStarted())

  private var currentQuery: String? = null
  private var currentSortType: SortType = SortType.Active
  private var currentInstance: String = apiClient.instance

  private val coroutineScope = coroutineScopeFactory.create()

  private val activePageQueries = mutableSetOf<Int>()
  private var pages: List<QueryResultsPage> = listOf()
  private var _items: List<Item> = listOf()

  private var personIdFilter: Long? = null
  private var communityIdFilter: Int? = null
  private var listingTypeFilter: ListingType? = null

  private val trigram = NGram(3)

  private var searchSource: SearchSource? = null

  val onItemsChangeFlow = MutableSharedFlow<Unit>()

  fun setPersonFilter(personId: Long?) {
    if (personIdFilter == personId) {
      return
    }
    personIdFilter = personId

    reset()

    performQuery(0, force = false)
  }

  fun setCommunityFilter(communityId: Int?) {
    if (communityIdFilter == communityId) {
      return
    }
    communityIdFilter = communityId

    reset()

    performQuery(0, force = false)
  }

  fun setListingTypeFilter(listingType: ListingType?) {
    if (listingTypeFilter == listingType) {
      return
    }
    listingTypeFilter = listingType

    reset()

    performQuery(0, force = false)
  }

  fun setQuery(query: String?) {
    if (currentQuery == query) {
      return
    }

    currentQuery = query

    reset()

    performQuery(0, force = false)
  }

  fun setSortType(sortType: SortType) {
    if (currentSortType == sortType) {
      return
    }

    currentSortType = sortType

    reset()

    performQuery(0, force = false)
  }

  fun setInstance(instance: String) {
    if (currentInstance == instance) {
      return
    }

    currentInstance = instance

    reset()
    performQuery(0, force = false)
  }

  fun performQuery(pageIndex: Int, force: Boolean) {
    if (activePageQueries.contains(pageIndex)) {
      return
    }

    activePageQueries.add(pageIndex)

    val currentQuery = currentQuery ?: return

    coroutineScope.launch {
      currentState.value = StatefulData.Loading()

      pages = pages.filter { it.pageIndex != pageIndex }

      generateItems()

      if (force) {
        reset()
      }


      if (searchSource == null) {
        searchSource = SearchSource(
          context,
          if (apiClient.supportsFeature(ApiFeature.ListByCursorRequired).getOrNull() == true) {
            CursorSearchSource(
              apiClient,
              trigram,
              communityIdFilter,
              currentSortType,
              listingTypeFilter,
              type,
              currentQuery,
              personIdFilter,
            )
          } else {
            PagedSearchSource(
              apiClient,
              trigram,
              communityIdFilter,
              currentSortType,
              listingTypeFilter,
              type,
              currentQuery,
              personIdFilter,
            )
          }
        )
      }

      val pageResult = searchSource?.getPage(pageIndex, force)
        ?: return@launch

      pageResult.fold(
        {
          val result: QueryResultsPage? = when (type) {
            SearchType.All ->
              AllResultsPage(
                it.items.also {
                  Log.d("HAHA", "items: ${it.size}")
                },
                pageIndex,
                it.hasMore,
              )
            SearchType.Url ->
              AllResultsPage(
                it.items,
                pageIndex,
                it.hasMore,
              )
            SearchType.Comments ->
              CommentResultsPage(
                it.items.filterIsInstance<CommentResultView>().map { it.commentView },
                pageIndex,
                it.hasMore,
              )
            SearchType.Posts ->
              PostResultsPage(
                it.items.filterIsInstance<PostResultView>().map { it.fetchedPost.postView },
                pageIndex,
                it.hasMore,
              )
            SearchType.Communities ->
              CommunityResultsPage(
                it.items.filterIsInstance<CommunityResultView>().map { it.communityView },
                pageIndex,
                it.hasMore,
              )
            SearchType.Users ->
              UserResultsPage(
                it.items.filterIsInstance<UserResultView>().map { it.personView },
                pageIndex,
                it.hasMore,
              )
          }

          if (result == null) {
            return@launch
          }

          val newPages = pages.toMutableList().apply {
            add(result)
          }
          withContext(Dispatchers.Main) {
            pages = newPages
          }
          currentState.value = StatefulData.Success(Unit)
          generateItems()

          withContext(Dispatchers.Main) {
            activePageQueries.remove(pageIndex)
          }
        },
        {
          val newPages = pages.toMutableList().apply {
            val existingItemIndex = indexOfFirst { it.pageIndex == pageIndex }
            val errorPage = ErrorPage(it, pageIndex, false)

            if (existingItemIndex == -1) {
              add(errorPage)
            } else {
              this[existingItemIndex] = errorPage
            }
          }
          withContext(Dispatchers.Main) {
            pages = newPages
          }
          currentState.value = StatefulData.Error(it)
          generateItems()

          withContext(Dispatchers.Main) {
            activePageQueries.remove(pageIndex)
          }
        }
      )

//      apiClient
//        .searchWithRetry(
//          communityId = communityIdFilter,
//          communityName = null,
//          sortType = currentSortType,
//          listingType = listingTypeFilter ?: ListingType.All,
//          searchType = type,
//          page = pageIndex.toLemmyPageIndex(),
//          query = currentQuery,
//          limit = MAX_QUERY_PAGE_LIMIT,
//          creatorId = personIdFilter,
//          force = force,
//        )
//        .onSuccess {
//        }
//        .onFailure {
//        }
    }
  }

  private suspend fun generateItems() {
    // regenerate items on page change

    if (pages.isEmpty()) {
      if (currentState.value is StatefulData.Loading) {
        _items = listOf()
      } else {
        _items = listOf(
          Item.EndItem,
          Item.FooterSpacerItem,
        )
      }
      onItemsChangeFlow.emit(Unit)

      return
    }

    val context = ContextCompat.getContextForLanguage(context)
    val newItems = mutableListOf<Item>()
    val pages = pages
    val instance = currentInstance

    pages.first()
    val lastPage = pages.last()

    for (page in pages) {
      when (page) {
        is AllResultsPage -> {
          page.results.mapTo(newItems) {
            when (it) {
              is CommentResultView ->
                Item.CommentItem(
                  commentView = it.commentView,
                  instance = instance,
                  pageIndex = page.pageIndex,
                  commentHeaderInfo = it.commentView.toCommentHeaderInfo(context),
                )
              is CommunityResultView ->
                Item.CommunityItem(it.communityView, instance, page.pageIndex)
              is PostResultView ->
                Item.PostItem(
                  fetchedPost = it.fetchedPost,
                  instance = instance,
                  pageIndex = page.pageIndex,
                  postHeaderInfo = it.fetchedPost.postView.toPostHeaderInfo(context),
                )
              is UserResultView ->
                Item.UserItem(it.personView, instance, page.pageIndex)
            }
          }
        }
        is UrlResultsPage ->
          page.results.mapTo(newItems) {
            when (it) {
              is CommentResultView ->
                Item.CommentItem(
                  commentView = it.commentView,
                  instance = instance,
                  pageIndex = page.pageIndex,
                  commentHeaderInfo = it.commentView.toCommentHeaderInfo(context),
                )
              is CommunityResultView ->
                Item.CommunityItem(it.communityView, instance, page.pageIndex)
              is PostResultView ->
                Item.PostItem(
                  fetchedPost = it.fetchedPost,
                  instance = instance,
                  pageIndex = page.pageIndex,
                  postHeaderInfo = it.fetchedPost.postView.toPostHeaderInfo(context),
                )
              is UserResultView ->
                Item.UserItem(it.personView, instance, page.pageIndex)
            }
          }
        is CommentResultsPage ->
          page.results.mapTo(newItems) {
            Item.CommentItem(
              commentView = it,
              instance = instance,
              pageIndex = page.pageIndex,
              commentHeaderInfo = it.toCommentHeaderInfo(context),
            )
          }
        is CommunityResultsPage ->
          page.results.mapTo(newItems) {
            Item.CommunityItem(it, instance, page.pageIndex)
          }
        is PostResultsPage ->
          page.results.mapTo(newItems) {
            Item.PostItem(
              fetchedPost = FetchedPost(
                it,
                StandardSource(),
              ),
              instance = instance,
              pageIndex = page.pageIndex,
              postHeaderInfo = it.toPostHeaderInfo(context),
            )
          }
        is UserResultsPage ->
          page.results.mapTo(newItems) {
            Item.UserItem(it, instance, page.pageIndex)
          }
        is ErrorPage ->
          newItems.add(Item.ErrorItem(page.error, page.pageIndex))
      }
    }

    if (lastPage is ErrorPage) {
      // add nothing!
    } else if (lastPage.hasMore) {
      newItems.add(Item.AutoLoadItem(lastPage.pageIndex + 1))
      newItems += Item.FooterSpacerItem
    } else {
      newItems.add(Item.EndItem)
      newItems += Item.FooterSpacerItem
    }

    _items = newItems

    Log.d(TAG, "items regenerated. length: ${newItems.size}")

    onItemsChangeFlow.emit(Unit)
  }

  private fun reset() {
    pages = listOf()
    activePageQueries.clear()
    searchSource = null
  }

  fun getItems() = _items
}
