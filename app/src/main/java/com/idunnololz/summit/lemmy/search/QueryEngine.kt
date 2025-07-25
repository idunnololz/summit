package com.idunnololz.summit.lemmy.search

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.CommentView
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.PersonView
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.api.dto.lemmy.SearchType
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentHeaderInfo
import com.idunnololz.summit.lemmy.PostHeaderInfo
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.Source
import com.idunnololz.summit.lemmy.toCommentHeaderInfo
import com.idunnololz.summit.lemmy.toPostHeaderInfo
import com.idunnololz.summit.util.StatefulData
import dagger.hilt.android.qualifiers.ApplicationContext
import info.debatty.java.stringsimilarity.NGram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

  data class AutoLoadItem(val pageToLoad: Int) : Item()

  data class ErrorItem(val error: Throwable, val pageToLoad: Int) : Item()

  data object EndItem : Item()
  data object FooterSpacerItem : Item()
}

class QueryEngine(
  @ApplicationContext private val context: Context,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val apiClient: AccountAwareLemmyClient,
  private val type: SearchType,
) {

  companion object {
    private const val TAG = "QueryEngine"

    private const val MAX_QUERY_PAGE_LIMIT = 20
  }

  sealed interface SearchResultView {
    val isUrlResult: Boolean
    data class PostResultView(
      val fetchedPost: FetchedPost,
      override val isUrlResult: Boolean,
    ) : SearchResultView

    data class CommentResultView(
      val commentView: CommentView,
      override val isUrlResult: Boolean,
    ) : SearchResultView

    data class CommunityResultView(
      val communityView: CommunityView,
      override val isUrlResult: Boolean,
    ) : SearchResultView

    data class UserResultView(
      val personView: PersonView,
      override val isUrlResult: Boolean,
    ) : SearchResultView
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

  private var currentQuery: String = ""
  private var currentSortType: SortType = SortType.Active
  private var currentInstance: String = apiClient.instance

  private val coroutineScope = coroutineScopeFactory.create()

  private val activePageQueries = mutableSetOf<Int>()
  private var pages: List<QueryResultsPage> = listOf()
  private var _items: List<Item> = listOf()

  private var personIdFilter: Long? = null
  private var communityIdFilter: Int? = null

  private val trigram = NGram(3)

  val onItemsChangeFlow = MutableSharedFlow<Unit>()

  init {
    runBlocking {
      // should be ok because we are generating a blank page.
      generateItems()
    }
  }

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

  fun setQuery(query: String) {
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

    val currentQuery = currentQuery

    if (currentQuery.isBlank()) {
      return
    }
    coroutineScope.launch {
      currentState.value = StatefulData.Loading()
      generateItems()

      if (force) {
        reset()
      }

      apiClient
        .searchWithRetry(
          communityId = communityIdFilter,
          communityName = null,
          sortType = currentSortType,
          listingType = ListingType.All,
          searchType = type,
          page = pageIndex.toLemmyPageIndex(),
          query = currentQuery,
          limit = MAX_QUERY_PAGE_LIMIT,
          creatorId = personIdFilter,
          force = force,
        )
        .onSuccess {
          val result: QueryResultsPage = when (it.type_) {
            SearchType.All -> {
              val items = mutableListOf<SearchResultView>()

              it.posts.mapTo(items) {
                SearchResultView.PostResultView(
                  FetchedPost(
                    it,
                    Source.StandardSource(),
                  ),
                  false,
                )
              }
              it.comments.mapTo(items) {
                SearchResultView.CommentResultView(it, false)
              }
              it.communities.mapTo(items) {
                SearchResultView.CommunityResultView(it, false)
              }
              it.users.mapTo(items) {
                SearchResultView.UserResultView(it, false)
              }

              val sortedItems =
                if (currentSortType == SortType.Active) {
                  items.sortedBy {
                    when (it) {
                      is SearchResultView.CommentResultView ->
                        trigram.distance(
                          it.commentView.comment.content,
                          currentQuery,
                        )
                      is SearchResultView.CommunityResultView -> {
                        trigram.distance(
                          it.communityView.community.name,
                          currentQuery,
                        )
                      }
                      is SearchResultView.PostResultView -> {
                        val post = it.fetchedPost.postView.post
                        val toMatch = post.name + " " + post.body
                        trigram.distance(toMatch, currentQuery)
                      }
                      is SearchResultView.UserResultView ->
                        trigram.distance(
                          it.personView.person.name,
                          currentQuery,
                        )
                    }
                  }
                } else {
                  items
                }

              QueryResultsPage.AllResultsPage(
                sortedItems,
                pageIndex,
                sortedItems.size >= MAX_QUERY_PAGE_LIMIT,
              )
            }
            SearchType.Comments ->
              QueryResultsPage.CommentResultsPage(
                if (currentSortType == SortType.Active) {
                  it.comments.sortedBy {
                    trigram.distance(it.comment.content, currentQuery)
                  }
                } else {
                  it.comments
                },
                pageIndex,
                it.comments.size >= MAX_QUERY_PAGE_LIMIT,
              )
            SearchType.Posts ->
              QueryResultsPage.PostResultsPage(
                if (currentSortType == SortType.Active) {
                  it.posts.sortedBy {
                    trigram.distance(
                      it.post.name + " " + it.post.body,
                      currentQuery,
                    )
                  }
                } else {
                  it.posts
                },
                pageIndex,
                it.posts.size >= MAX_QUERY_PAGE_LIMIT,
              )
            SearchType.Communities ->
              QueryResultsPage.CommunityResultsPage(
                if (currentSortType == SortType.Active) {
                  it.communities.sortedBy {
                    trigram.distance(it.community.fullName, currentQuery)
                  }
                } else {
                  it.communities
                },
                pageIndex,
                it.communities.size >= MAX_QUERY_PAGE_LIMIT,
              )
            SearchType.Users ->
              QueryResultsPage.UserResultsPage(
                if (currentSortType == SortType.Active) {
                  it.users.sortedBy {
                    trigram.distance(it.person.name, currentQuery)
                  }
                } else {
                  it.users
                },
                pageIndex,
                it.users.size >= MAX_QUERY_PAGE_LIMIT,
              )
            SearchType.Url -> {
              val items = mutableListOf<SearchResultView>()

              it.posts.mapTo(items) {
                SearchResultView.PostResultView(
                  FetchedPost(
                    it,
                    Source.StandardSource(),
                  ),
                  true,
                )
              }
              it.comments.mapTo(items) {
                SearchResultView.CommentResultView(it, true)
              }
              it.communities.mapTo(items) {
                SearchResultView.CommunityResultView(it, true)
              }
              it.users.mapTo(items) {
                SearchResultView.UserResultView(it, true)
              }

              val sortedItems =
                if (currentSortType == SortType.Active) {
                  items.sortedBy { view ->
                    when (view) {
                      is SearchResultView.CommentResultView ->
                        trigram.distance(
                          view.commentView.comment.ap_id,
                          currentQuery,
                        )
                      is SearchResultView.CommunityResultView ->
                        trigram.distance(
                          view.communityView.community.actor_id,
                          currentQuery,
                        )
                      is SearchResultView.PostResultView ->
                        trigram.distance(
                          view.fetchedPost.postView.post.ap_id,
                          currentQuery,
                        )
                      is SearchResultView.UserResultView ->
                        trigram.distance(
                          view.personView.person.actor_id,
                          currentQuery,
                        )
                    }
                  }
                } else {
                  items
                }

              QueryResultsPage.UrlResultsPage(
                sortedItems,
                pageIndex,
                sortedItems.size >= MAX_QUERY_PAGE_LIMIT,
              )
            }
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
        }
        .onFailure {
          val newPages = pages.toMutableList().apply {
            add(QueryResultsPage.ErrorPage(it, pageIndex, false))
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

      return
    }

    val context = ContextCompat.getContextForLanguage(context)
    val newItems = mutableListOf<Item>()
    val pages = pages
    val instance = currentInstance

    val firstPage = pages.first()
    val lastPage = pages.last()

    for (page in pages) {
      when (page) {
        is QueryResultsPage.AllResultsPage -> {
          page.results.mapTo(newItems) {
            when (it) {
              is SearchResultView.CommentResultView ->
                Item.CommentItem(
                  commentView = it.commentView,
                  instance = instance,
                  pageIndex = page.pageIndex,
                  commentHeaderInfo = it.commentView.toCommentHeaderInfo(context),
                )
              is SearchResultView.CommunityResultView ->
                Item.CommunityItem(it.communityView, instance, page.pageIndex)
              is SearchResultView.PostResultView ->
                Item.PostItem(
                  fetchedPost = it.fetchedPost,
                  instance = instance,
                  pageIndex = page.pageIndex,
                  postHeaderInfo = it.fetchedPost.postView.toPostHeaderInfo(context),
                )
              is SearchResultView.UserResultView ->
                Item.UserItem(it.personView, instance, page.pageIndex)
            }
          }
        }
        is QueryResultsPage.UrlResultsPage ->
          page.results.mapTo(newItems) {
            when (it) {
              is SearchResultView.CommentResultView ->
                Item.CommentItem(
                  commentView = it.commentView,
                  instance = instance,
                  pageIndex = page.pageIndex,
                  commentHeaderInfo = it.commentView.toCommentHeaderInfo(context),
                )
              is SearchResultView.CommunityResultView ->
                Item.CommunityItem(it.communityView, instance, page.pageIndex)
              is SearchResultView.PostResultView ->
                Item.PostItem(
                  fetchedPost = it.fetchedPost,
                  instance = instance,
                  pageIndex = page.pageIndex,
                  postHeaderInfo = it.fetchedPost.postView.toPostHeaderInfo(context),
                )
              is SearchResultView.UserResultView ->
                Item.UserItem(it.personView, instance, page.pageIndex)
            }
          }
        is QueryResultsPage.CommentResultsPage ->
          page.results.mapTo(newItems) {
            Item.CommentItem(
              commentView = it,
              instance = instance,
              pageIndex = page.pageIndex,
              commentHeaderInfo = it.toCommentHeaderInfo(context),
            )
          }
        is QueryResultsPage.CommunityResultsPage ->
          page.results.mapTo(newItems) {
            Item.CommunityItem(it, instance, page.pageIndex)
          }
        is QueryResultsPage.PostResultsPage ->
          page.results.mapTo(newItems) {
            Item.PostItem(
              fetchedPost = FetchedPost(
                it,
                Source.StandardSource(),
              ),
              instance = instance,
              pageIndex = page.pageIndex,
              postHeaderInfo = it.toPostHeaderInfo(context),
            )
          }
        is QueryResultsPage.UserResultsPage ->
          page.results.mapTo(newItems) {
            Item.UserItem(it, instance, page.pageIndex)
          }
        is QueryResultsPage.ErrorPage ->
          newItems.add(Item.ErrorItem(page.error, page.pageIndex))
      }
    }

    if (lastPage is QueryResultsPage.ErrorPage) {
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
  }

  private fun Int.toLemmyPageIndex() = this + 1 // lemmy pages are 1 indexed

  fun getItems() = _items
}
