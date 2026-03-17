package com.idunnololz.summit.models.processed

import android.util.Log
import com.idunnololz.summit.api.dto.lemmy.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.lemmy.GetPostResponse
import com.idunnololz.summit.api.dto.lemmy.GetPostsResponse
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.api.dto.lemmy.SearchResponse
import com.idunnololz.summit.preferences.Preferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DtoConverter @Inject constructor(
  private val preferences: Preferences,
) {

  fun convertPostView(postView: PostView): com.idunnololz.summit.models.PostView {
    val parsedTagsResult =
      if (preferences.parseTagsFromPostTitles) {
        TagParser.parseTags(postView.post.name)
      } else {
        null
      }

    return com.idunnololz.summit.models.PostView(
      post = postView.post,
      creator = postView.creator,
      community = postView.community,
      creator_banned_from_community = postView.creator_banned_from_community,
      creator_is_moderator = postView.creator_is_moderator,
      creator_is_admin = postView.creator_is_admin,
      counts = postView.counts,
      subscribed = postView.subscribed,
      saved = postView.saved,
      read = postView.read,
      creator_blocked = postView.creator_blocked,
      my_vote = postView.my_vote,
      unread_comments = postView.unread_comments,
      tags = parsedTagsResult?.tags
    )
  }

  fun convertGetPostsResponse(
    getPostsResponse: GetPostsResponse
  ): com.idunnololz.summit.models.GetPostsResponse {
    return com.idunnololz.summit.models.GetPostsResponse(
      getPostsResponse.posts.map { convertPostView(it) },
      getPostsResponse.next_page,
    )
  }

  fun convertGetPostResponse(
    getPostResponse: GetPostResponse
  ): com.idunnololz.summit.models.GetPostResponse {
    return com.idunnololz.summit.models.GetPostResponse(
      convertPostView(getPostResponse.post_view),
      getPostResponse.community_view,
      getPostResponse.moderators,
      getPostResponse.cross_posts.map { convertPostView(it) },
      getPostResponse.online,
    )
  }

  fun convertGetPersonDetailsResponse(
    getPersonDetailsResponse: GetPersonDetailsResponse
  ): com.idunnololz.summit.models.GetPersonDetailsResponse {
    return com.idunnololz.summit.models.GetPersonDetailsResponse(
      getPersonDetailsResponse.person_view,
      getPersonDetailsResponse.comments,
      getPersonDetailsResponse.posts.map { convertPostView(it) },
      getPersonDetailsResponse.moderates,
    )
  }

  fun convertSearchResponse(
    searchResponse: SearchResponse
  ): com.idunnololz.summit.models.SearchResponse {
    return com.idunnololz.summit.models.SearchResponse(
      searchResponse.type_,
      searchResponse.comments,
      searchResponse.posts.map { convertPostView(it) },
      searchResponse.communities,
      searchResponse.users,
    )
  }
}