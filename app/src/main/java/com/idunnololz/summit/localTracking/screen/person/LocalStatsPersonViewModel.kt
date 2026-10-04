package com.idunnololz.summit.localTracking.screen.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.Source
import com.idunnololz.summit.localTracking.TrackedAction
import com.idunnololz.summit.localTracking.TrackingEvent
import com.idunnololz.summit.localTracking.TrackingEventsDao
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.resolver.PostResolverHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.collections.get
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.launch
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray

@HiltViewModel
class LocalStatsPersonViewModel @Inject constructor(
  private val accountManager: AccountManager,
  private val trackingEventsDao: TrackingEventsDao,
  private val postResolverHelperFactory: PostResolverHelper.Factory,
  private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

  data class Model(
    val items: List<Item>,
  ) {

    sealed interface Item

    data class PostModelItem(
      val postId: Long?,
      val fetchedPost: Result<FetchedPost>?,
    ) : Item

    data class PersonStatsSummaryItem(
      val totalScore: Int,
      val upvotes: Int,
      val downvotes: Int,
      val recentTotalScore: Int,
      val recentUpvotes: Int,
      val recentDownvotes: Int,
      val recentInteractionsCount: Int,
      val totalInteractionsCount: Int,
      val recentViewsCount: Int,
      val totalViewsCount: Int,
    ) : Item

    data object NoPostsModelItem : Item
  }

  val data = StatefulLiveData<Model>()

  private val postResolverHelper = postResolverHelperFactory.create(viewModelScope)

  init {
    viewModelScope.launch {
      postResolverHelper.onPostDictionaryChanged.collect {
        val model = data.valueOrNull ?: return@collect
        data.postValue(
          model.copy(
            items = model.items.map {
              when (it) {
                is Model.PostModelItem ->
                  it.copy(
                    fetchedPost = postResolverHelper.postDictionary[it.postId]?.map {
                      FetchedPost(
                        postView = it,
                        source = Source.StandardSource(),
                      )
                    },
                  )
                else -> it
              }
            },
          ),
        )
      }
    }
  }

  fun generateModel(personRef: PersonRef, force: Boolean) {
    val currentAccountId = accountManager.currentAccount.asAccount?.id ?: return

    if (!force && data.isLoaded) {
      return
    }

    data.setIsLoading()

    viewModelScope.launch {
      val events = trackingEventsDao.getAll()
        .filter {
          it.userId == currentAccountId
        }
        .sortedBy { it.ts }

      val personId = when (personRef) {
        is PersonRef.PersonRefById -> personRef.id
        is PersonRef.PersonRefByName -> apiClient.fetchPersonByNameWithRetry(
          personRef.fullName,
          force = false,
        ).fold(
          onSuccess = {
            it.personView.person.id
          },
          onFailure = {
            data.setError(it)
            null
          },
        )
        is PersonRef.PersonRefComplete -> personRef.id
      }

      if (personId == null) {
        return@launch
      }

      val allPosts = mutableListOf<Long?>()
      val seenPostId = mutableSetOf<Long?>()
      val postToScore = mutableMapOf<Long, Int>()
      val postToScoreRecent = mutableMapOf<Long, Int>()
      val recentCutOff = 7.days.inWholeMilliseconds
      val currentTime = System.currentTimeMillis()
      var recentInteractionsCount = 0
      var totalInteractionsCount = 0

      val allViewedPostIds = mutableSetOf<Long>()
      val recentViewedPostIds = mutableSetOf<Long>()
      events.forEach {
        val trackingEvent: TrackingEvent = Cbor.decodeFromByteArray(it.trackingEventCbor)

        when (trackingEvent.action) {
          TrackedAction.UPVOTE -> {
            if (trackingEvent.targetUserId == personId) {
              if (trackingEvent.commentId == null) {
                if (seenPostId.add(trackingEvent.postId)) {
                  allPosts.add(trackingEvent.postId)
                }
                if (trackingEvent.postId != null) {
                  postToScore[trackingEvent.postId] = 1
                }
              }
              if (currentTime - it.ts < recentCutOff) {
                recentInteractionsCount++

                if (trackingEvent.commentId == null) {
                  if (trackingEvent.postId != null) {
                    postToScoreRecent[trackingEvent.postId] = 1
                  }
                }
              }
              totalInteractionsCount++
            }
          }
          TrackedAction.DOWNVOTE -> {
            if (trackingEvent.targetUserId == personId) {
              if (trackingEvent.commentId == null) {
                if (seenPostId.add(trackingEvent.postId)) {
                  allPosts.add(trackingEvent.postId)
                }
                if (trackingEvent.postId != null) {
                  postToScore[trackingEvent.postId] = -1
                }
              }
              if (currentTime - it.ts < recentCutOff) {
                recentInteractionsCount++

                if (trackingEvent.commentId == null) {
                  if (trackingEvent.postId != null) {
                    postToScoreRecent[trackingEvent.postId] = -1
                  }
                }
              }
              totalInteractionsCount++
            }
          }
          TrackedAction.CLEAR_VOTE -> {
            if (trackingEvent.targetUserId == personId) {
              if (trackingEvent.commentId == null) {
                if (seenPostId.add(trackingEvent.postId)) {
                  allPosts.add(trackingEvent.postId)
                }
                if (trackingEvent.postId != null) {
                  postToScore[trackingEvent.postId] = 0
                }
              }
              if (currentTime - it.ts < recentCutOff) {
                recentInteractionsCount++

                if (trackingEvent.commentId == null) {
                  if (trackingEvent.postId != null) {
                    postToScoreRecent[trackingEvent.postId] = 0
                  }
                }
              }
              totalInteractionsCount++
            }
          }
          TrackedAction.VIEW -> {
            if (trackingEvent.targetUserId == personId) {
              if (trackingEvent.commentId == null) {
                if (seenPostId.add(trackingEvent.postId)) {
                  allPosts.add(trackingEvent.postId)
                }
                if (trackingEvent.postId != null) {
                  if (currentTime - it.ts < recentCutOff) {
                    recentViewedPostIds.add(trackingEvent.postId)
                  }
                  allViewedPostIds.add(trackingEvent.postId)
                }
              }
            }
          }
          TrackedAction.REPLY -> {
//            if (trackingEvent.communityRef == communityRef && seenPostId.add(trackingEvent.postId)) {
//              if (currentTime - it.ts < recentCutOff) {
//                recentInteractionsCount++
//              }
//              totalInteractionsCount++
//            }
          }
          TrackedAction.DELETE_REPLY -> {}
          TrackedAction.POST -> {
//            if (trackingEvent.communityRef == communityRef && seenPostId.add(trackingEvent.postId)) {
//              if (currentTime - it.ts < recentCutOff) {
//                recentInteractionsCount++
//              }
//              totalInteractionsCount++
//            }
          }
        }
      }

      var upvotes = 0
      var downvotes = 0
      for ((_, score) in postToScore) {
        if (score == 1) {
          upvotes++
        } else if (score == -1) {
          downvotes++
        }
      }

      var upvotesRecent = 0
      var downvotesRecent = 0
      for ((_, score) in postToScoreRecent) {
        if (score == 1) {
          upvotesRecent++
        } else if (score == -1) {
          downvotesRecent++
        }
      }

      data.postValue(
        Model(
          items = buildList {
            add(
              Model.PersonStatsSummaryItem(
                totalScore = upvotes - downvotes,
                upvotes = upvotes,
                downvotes = downvotes,
                recentTotalScore = upvotesRecent - downvotesRecent,
                recentUpvotes = upvotesRecent,
                recentDownvotes = downvotesRecent,
                recentInteractionsCount = recentInteractionsCount,
                totalInteractionsCount = totalInteractionsCount,
                recentViewsCount = recentViewedPostIds.count(),
                totalViewsCount = allViewedPostIds.count(),
              ),
            )

            if (allPosts.isEmpty()) {
              add(Model.NoPostsModelItem)
            } else {
              allPosts.forEach {
                add(
                  Model.PostModelItem(
                    postId = it,
                    fetchedPost = postResolverHelper.postDictionary[it]?.map {
                      FetchedPost(
                        postView = it,
                        source = Source.StandardSource(),
                      )
                    },
                  ),
                )
              }
            }
          },
        ),
      )
    }
  }

  fun fetchPost(postId: Long, force: Boolean) {
    postResolverHelper.fetchPost(postId, force = force)
  }
}
