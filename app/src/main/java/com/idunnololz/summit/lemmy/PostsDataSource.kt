package com.idunnololz.summit.lemmy

import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.ListingType
import com.idunnololz.summit.api.dto.lemmy.SortType
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.Source
import com.idunnololz.summit.lemmy.utils.listSource.CursorBackedSingleDataSource
import com.idunnololz.summit.lemmy.utils.listSource.Page
import com.idunnololz.summit.lemmy.utils.listSource.SimpleDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class SinglePostsDataSource @AssistedInject constructor(
  @Assisted private val communityName: String?,
  @Assisted private val listingType: ListingType?,
  private val apiClient: AccountAwareLemmyClient,
) : SimpleDataSource<FetchedPost, SortType> {

  @AssistedFactory
  interface Factory {
    fun create(communityName: String?, listingType: ListingType?): SinglePostsDataSource
  }

  override suspend fun fetchItems(
    sortOrder: SortType?,
    page: Int,
    force: Boolean,
    limit: Int,
    showRead: Boolean?,
  ): Result<List<FetchedPost>> = apiClient
    .fetchPosts(
      communityIdOrName = if (communityName == null) {
        null
      } else {
        Either.Right(communityName)
      },
      sortType = sortOrder,
      listingType = listingType ?: ListingType.All,
      page = page,
      cursor = null,
      limit = limit,
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
) : CursorBackedSingleDataSource<FetchedPost, SortType>(
  fetchObjects = suspend {
      pageCursor: String?,
      sortOrder: SortType?,
      limit: Int,
      force: Boolean,
      showRead: Boolean?,
    ->

    apiClient
      .fetchPosts(
        communityIdOrName = if (communityName == null) {
          null
        } else {
          Either.Right(communityName)
        },
        sortType = sortOrder,
        listingType = listingType ?: ListingType.All,
        cursor = pageCursor,
        limit = limit,
        force = force,
        showRead = showRead,
        page = null,
      )
      .map {
        Page(
          items = it.posts.map { FetchedPost(it, Source.StandardSource()) },
          nextCursor = it.nextPage,
        )
      }
  },
) {

  @AssistedFactory
  interface Factory {
    fun create(communityName: String?, listingType: ListingType?): CursorBackedSinglePostsDataSource
  }
}
