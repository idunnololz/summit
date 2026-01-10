package com.idunnololz.summit.lemmy

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.GetPostsResponse
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.Source
import com.idunnololz.summit.lemmy.utils.listSource.LemmyListSource.Companion.DEFAULT_PAGE_SIZE
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

interface PostsDataSource {
  suspend fun fetchPosts(
    sortType: SortType? = null,
    page: Int,
    force: Boolean,
    showRead: Boolean?,
  ): Result<List<FetchedPost>>
}

class SinglePostsDataSource @AssistedInject constructor(
  @Assisted private val communityName: String?,
  @Assisted private val listingType: ListingType?,
  private val apiClient: AccountAwareLemmyClient,
) : PostsDataSource {

  @AssistedFactory
  interface Factory {
    fun create(communityName: String?, listingType: ListingType?): SinglePostsDataSource
  }

  override suspend fun fetchPosts(sortType: SortType?, page: Int, force: Boolean, showRead: Boolean?) = apiClient
    .fetchPosts(
      communityIdOrName = if (communityName == null) {
        null
      } else {
        Either.Right(communityName)
      },
      sortType = sortType,
      listingType = listingType ?: ListingType.All,
      page = page.toLemmyPageIndex(),
      cursor = null,
      limit = DEFAULT_PAGE_SIZE,
      force = force,
      showRead = showRead,
    )
    .map {
      it.posts.map { FetchedPost(it, Source.StandardSource()) }
    }
}

class CursorBackedSinglePostsDataSource @AssistedInject constructor(
  @Assisted private val communityName: String?,
  @Assisted private val listingType: ListingType?,
  private val apiClient: AccountAwareLemmyClient,
) : PostsDataSource {

  companion object {
    private const val TAG = "CursorBackedSinglePostsDataSource"
  }

  @AssistedFactory
  interface Factory {
    fun create(
      communityName: String?,
      listingType: ListingType?
    ): CursorBackedSinglePostsDataSource
  }

  private var cursorIds = listOf<String?>("0")

  override suspend fun fetchPosts(
    sortType: SortType?,
    page: Int,
    force: Boolean,
    showRead: Boolean?,
  ): Result<List<FetchedPost>> {
    Log.d(TAG, "fetchPosts - page: $page cursors: ${cursorIds.size}")

    buildCursorIds(
      sortType = sortType,
      page = page,
      force = force,
      showRead = showRead,
    ).onFailure {
      return Result.failure(it)
    }

    return _fetchPosts(
        sortType = sortType,
        page = page,
        force = force,
        showRead = showRead,
      )
      .onSuccess {
        if (page + 1 == cursorIds.size) {
          cursorIds += it.next_page
        } else {
          if (cursorIds[page + 1] != it.next_page) {
            Log.d(TAG, "inconsistency cursor ids... wiping cached cursor ids")

            cursorIds = cursorIds.take(page + 1)
            cursorIds += it.next_page
          }
        }
      }
      .map {
        it.posts.map { FetchedPost(it, Source.StandardSource()) }
      }
  }

  private suspend fun buildCursorIds(
    sortType: SortType?,
    page: Int,
    force: Boolean,
    showRead: Boolean?,
  ): Result<Unit> {
    if (cursorIds.size > page) {
      return Result.success(Unit)
    }

    Log.d(TAG, "cursor index outside of current range, fetching more cursors...")

    while (cursorIds.size <= page) {
      _fetchPosts(
        sortType = sortType,
        page = cursorIds.lastIndex,
        force = force,
        showRead = showRead,
      ).fold(
        {
          cursorIds += it.next_page
        },
        {
          return Result.failure(it)
        }
      )
    }

    return Result.success(Unit)
  }

  private suspend fun _fetchPosts(
    sortType: SortType?,
    page: Int,
    force: Boolean,
    showRead: Boolean?,
  ): Result<GetPostsResponse> {
    if (page > 0 && cursorIds[page] == null) {
      return Result.success(GetPostsResponse(listOf(), null))
    }

    val pageIndex = if (page == 0) {
      page.toLemmyPageIndex()
    } else {
      null
    }
    val cursor = if (page == 0) {
      null
    } else {
      cursorIds[page]
    }

    Log.d(TAG, "_fetchPosts - ${pageIndex} ${cursor}")

    return apiClient
      .fetchPosts(
        communityIdOrName = if (communityName == null) {
          null
        } else {
          Either.Right(communityName)
        },
        sortType = sortType,
        listingType = listingType ?: ListingType.All,
        page = pageIndex,
        cursor = cursor,
        limit = DEFAULT_PAGE_SIZE,
        force = force,
        showRead = showRead,
      )
  }
}