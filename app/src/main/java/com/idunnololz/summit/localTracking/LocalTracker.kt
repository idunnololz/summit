package com.idunnololz.summit.localTracking

import android.util.Log
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingActionsManager
import com.idunnololz.summit.api.utils.parentId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.preferences.Preferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTracker @Inject constructor(
  private val accountManager: AccountManager,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val perAccountLocalTrackerFactory: PerAccountLocalTracker.Factory,
  private val pendingActionsManager: PendingActionsManager,
) {

  companion object {
    private const val TAG = "LocalTracker"
  }

  private val coroutineScope = coroutineScopeFactory.create()
  private var currentTracker: PerAccountLocalTracker? = null

  init {
    pendingActionsManager.addActionCompleteListener(object : PendingActionsManager.OnActionChangedListener {
      override fun onActionAdded(action: LemmyAction) {}

      override fun onActionFailed(
        action: LemmyAction,
        reason: LemmyActionFailureReason,
      ) {}

      override fun onActionComplete(
        action: LemmyAction,
        result: LemmyActionResult<*, *>,
      ) {
        when (result) {
          is LemmyActionResult.CommentLemmyActionResult -> {
            trackEvent(
              instanceId = result.result.community.instance_id.toLong(),
              communityRef = result.result.community.toCommunityRef(),
              postId = result.result.post.id.toLong(),
              commentId = result.parentId?.toLong(),
              targetUserId = null,
              action = TrackedAction.REPLY,
              nsfw = result.result.post.nsfw || result.result.community.nsfw,
            )
          }
          is LemmyActionResult.DeleteCommentLemmyActionResult -> {
            trackEvent(
              instanceId = result.result.community.instance_id.toLong(),
              communityRef = result.result.community.toCommunityRef(),
              postId = result.result.post.id.toLong(),
              commentId = result.result.comment.parentId(),
              targetUserId = null,
              action = TrackedAction.DELETE_REPLY,
              nsfw = result.result.post.nsfw || result.result.community.nsfw,
            )
          }
          is LemmyActionResult.EditLemmyActionResult -> {}
          is LemmyActionResult.MarkPostAsReadActionResult -> {}
          is LemmyActionResult.VoteLemmyActionResult -> {
            result.result.fold(
              {
                // post

                val myVote = it.my_vote

                trackEvent(
                  instanceId = it.community.instance_id.toLong(),
                  communityRef = it.community.toCommunityRef(),
                  postId = it.post.id.toLong(),
                  commentId = null,
                  targetUserId = it.post.creator_id,
                  action = if (myVote == null || myVote == 0) {
                    TrackedAction.CLEAR_VOTE
                  } else if (myVote > 0) {
                    TrackedAction.UPVOTE
                  } else {
                    TrackedAction.DOWNVOTE
                  },
                  nsfw = it.post.nsfw || it.community.nsfw,
                )
              },
              {
                // comment

                val myVote = it.my_vote

                trackEvent(
                  instanceId = it.community.instance_id.toLong(),
                  communityRef = it.community.toCommunityRef(),
                  postId = it.post.id.toLong(),
                  commentId = it.comment.id.toLong(),
                  targetUserId = it.comment.creator_id,
                  action = if (myVote == null || myVote == 0) {
                    TrackedAction.CLEAR_VOTE
                  } else if (myVote > 0) {
                    TrackedAction.UPVOTE
                  } else {
                    TrackedAction.DOWNVOTE
                  },
                  nsfw = it.post.nsfw || it.community.nsfw,
                )
              },
            )
          }
        }
      }

      override fun onActionDeleted(action: LemmyAction) {}

    })

    coroutineScope.apply {
      launch {
        accountManager.currentAccount.collect {
          currentTracker = perAccountLocalTrackerFactory.create(it.id)
        }
      }
    }
  }

  fun trackEvent(
    instanceId: Long?,
    communityRef: CommunityRef?,
    postId: Long?,
    commentId: Long?,
    targetUserId: Long?,
    action: TrackedAction,
    nsfw: Boolean,
  ) {
    Log.d(TAG, "event: ${action} ${instanceId}i $communityRef ${postId}p ${commentId}c ${targetUserId}t nsfw: ${nsfw}")

    currentTracker?.trackEvent(
      instanceId = instanceId,
      communityRef = communityRef,
      postId = postId,
      commentId = commentId,
      targetUserId = targetUserId,
      action = action,
      nsfw = nsfw,
    )
  }
}

class PerAccountLocalTracker @AssistedInject constructor(
  @Assisted private val userId: Long,
  private val trackingEventsDao: TrackingEventsDao,
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val preferences: Preferences,
) {

  @AssistedFactory
  interface Factory {
    fun create(userId: Long): PerAccountLocalTracker
  }

  private val coroutineScope = coroutineScopeFactory.create()

  @OptIn(ExperimentalSerializationApi::class)
  fun trackEvent(
    instanceId: Long?,
    communityRef: CommunityRef?,
    postId: Long?,
    commentId: Long?,
    targetUserId: Long?,
    action: TrackedAction,
    nsfw: Boolean,
  ) {
    if (!preferences.localTrackingEnabled) {
      return
    }

    coroutineScope.launch {
      val event = TrackingEvent(
        userId = userId,
        instanceId = instanceId,
        communityRef = communityRef,
        postId = postId,
        commentId = commentId,
        targetUserId = targetUserId,
        action = action,
        nsfw = nsfw,
      )

      val encoded = Cbor.encodeToByteArray(event)

      trackingEventsDao.insertTrackingEventEntry(
        TrackingEventEntry(
          id = 0,
          ts = System.currentTimeMillis(),
          userId = userId,
          trackingEventCbor = encoded,
        )
      )
    }
  }

}