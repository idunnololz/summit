package com.idunnololz.summit.lemmy

import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
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
