package com.idunnololz.summit.localTracking.person

import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.localTracking.DerivedTracker
import com.idunnololz.summit.localTracking.LocalTracker
import com.idunnololz.summit.localTracking.SimpleOnTrackingEventListener
import com.idunnololz.summit.localTracking.TrackedAction
import com.idunnololz.summit.localTracking.TrackingEvent
import com.idunnololz.summit.localTracking.TrackingEventEntry
import com.idunnololz.summit.localTracking.TrackingEventsDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.getOrPut
import kotlin.collections.set
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@Singleton
class PersonTracker @Inject constructor(
  private val coroutineScopeFactory: CoroutineScopeFactory,
  private val localTracker: LocalTracker,
  private val accountManager: AccountManager,
  private val globalStateStorage: GlobalStateStorage,
  private val trackingEventsDao: TrackingEventsDao,
  private val personTrackerDao: PersonTrackerDao,
) : DerivedTracker {

  private val coroutineScope = coroutineScopeFactory.create()

  private var scoresByPerson: Map<Long, Map<Long, Int>> = mapOf()

  val onChangedFlow = MutableSharedFlow<Unit>()

  @OptIn(ExperimentalSerializationApi::class)
  override fun init() {
    coroutineScope.launch {
      if (globalStateStorage.migratedTrackingDataToPersonData) {
        val scoresByPerson: MutableMap<Long, MutableMap<Long, Int>> = mutableMapOf()
        val allEntries = personTrackerDao.getAllSimpleEntries()

        allEntries.forEach { entry ->
          scoresByPerson.getOrPut(entry.userId) { mutableMapOf() }.apply {
            put(entry.targetPersonId, entry.totalScore)
          }
        }

        this@PersonTracker.scoresByPerson = scoresByPerson
      } else {
        val allEvents = trackingEventsDao.getEventsWithActions(
          listOf(
            TrackedAction.UPVOTE,
            TrackedAction.DOWNVOTE,
            TrackedAction.CLEAR_VOTE,
          ),
        ).sortedByDescending { it.ts }
        updatePersonScores(allEvents)
      }
    }

    localTracker.registerOnTrackingEventListener(object : SimpleOnTrackingEventListener() {
      override fun onDeleteAll() {
        coroutineScope.launch {
          personTrackerDao.deleteAll()
        }
      }

      override fun onVote(
        userId: Long,
        targetPersonId: Long,
        postId: Long?,
        commentId: Long?,
        direction: Int,
      ) {
        coroutineScope.launch {
          val key = if (commentId != null) {
            "c/$commentId"
          } else if (postId != null) {
            "p/$postId"
          } else {
            error("how can comment and post id be null? how???")
          }

          val entry = personTrackerDao.getEntry(userId, targetPersonId).firstOrNull()
          val intermediateStatsByPerson: MutableMap<Long, MutableMap<Long, PersonIntermediateStats>> =
            if (entry == null) {
              mutableMapOf()
            } else {
              mutableMapOf(
                entry.userId to
                  mutableMapOf(entry.targetPersonId to entry.getPersonIntermediateStats()),
              )
            }

          intermediateStatsByPerson.update(
            userId = userId,
            targetPersonId = targetPersonId,
            key = key,
            direction = direction,
          )
          updateFinalScores(intermediateStatsByPerson)

          onChangedFlow.emit(Unit)
        }
      }
    })
  }

  fun getPersonScore(
    userId: Long? = accountManager.currentAccount.asAccount?.id,
    targetPersonId: Long,
  ): Int {
    if (userId == null) {
      return 0
    }

    return scoresByPerson[userId]?.get(targetPersonId) ?: 0
  }

  @OptIn(ExperimentalSerializationApi::class)
  private suspend fun updatePersonScores(events: List<TrackingEventEntry>) {
    val seenPosts = mutableSetOf<Long>()
    val seenComments = mutableSetOf<Long>()
    val personScore: MutableMap<Long, MutableMap<Long, PersonIntermediateStats>> = mutableMapOf()

    for (event in events) {
      val trackingEvent: TrackingEvent = Cbor.decodeFromByteArray(event.trackingEventCbor)

      val direction = when (trackingEvent.action) {
        TrackedAction.UPVOTE -> 1
        TrackedAction.DOWNVOTE -> -1
        TrackedAction.CLEAR_VOTE -> 0
        TrackedAction.VIEW,
        TrackedAction.REPLY,
        TrackedAction.DELETE_REPLY,
        TrackedAction.POST,
        -> error("unreachable")
      }

      val key: String

      if (trackingEvent.userId == trackingEvent.targetUserId) {
        continue
      }

      if (trackingEvent.commentId != null) {
        if (!seenComments.add(trackingEvent.commentId)) {
          continue
        }
        key = "c/${trackingEvent.commentId}"
      } else if (trackingEvent.postId != null) {
        if (!seenPosts.add(trackingEvent.postId)) {
          continue
        }
        key = "p/${trackingEvent.postId}"
      } else {
        error("how can comment and post id be null?")
      }

      require(trackingEvent.targetUserId != null)
      require(trackingEvent.userId != null)

      personScore.update(
        trackingEvent.userId,
        trackingEvent.targetUserId,
        key,
        direction,
      )
    }

    calculateFinalScores(personScore)
  }

  @OptIn(ExperimentalSerializationApi::class)
  private suspend fun calculateFinalScores(
    intermediateStatsByPerson: Map<Long, Map<Long, PersonIntermediateStats>>,
  ) {
    val scoresByPerson: MutableMap<Long, MutableMap<Long, Int>> = mutableMapOf()

    intermediateStatsByPerson.forEach { (personId, targetToStats) ->
      targetToStats.forEach { (targetId, stats) ->
        scoresByPerson.getOrPut(personId) { mutableMapOf() }.apply {
          put(targetId, stats.upvoted.size - stats.downvoted.size)
        }

        val ts = System.currentTimeMillis()
        personTrackerDao.insert(
          PersonStatEntry(
            userId = personId,
            targetPersonId = targetId,
            lastUpdateTs = ts,
            createdTs = ts,
            totalScore = stats.upvoted.size - stats.downvoted.size,
            personIntermediateStatsCbor = Cbor.encodeToByteArray(stats),
          ),
        )
      }
    }

    this.scoresByPerson = scoresByPerson
  }

  @OptIn(ExperimentalSerializationApi::class)
  private suspend fun updateFinalScores(
    intermediateStatsByPerson: Map<Long, Map<Long, PersonIntermediateStats>>,
  ) {
    intermediateStatsByPerson.forEach { (personId, targetToStats) ->
      targetToStats.forEach { (targetPersonId, stats) ->
        val newScore = stats.upvoted.size - stats.downvoted.size

        val scores: Map<Long, Int> = scoresByPerson[personId]
          ?: mapOf()

        val updatedScores = scores.toMutableMap().apply {
          this[targetPersonId] = newScore
        }

        scoresByPerson = scoresByPerson.toMutableMap().apply {
          this[personId] = updatedScores
        }

        val ts = System.currentTimeMillis()
        personTrackerDao.insert(
          PersonStatEntry(
            userId = personId,
            targetPersonId = targetPersonId,
            lastUpdateTs = ts,
            createdTs = ts,
            totalScore = stats.upvoted.size - stats.downvoted.size,
            personIntermediateStatsCbor = Cbor.encodeToByteArray(stats),
          ),
        )
      }
    }
  }

  private fun MutableMap<Long, MutableMap<Long, PersonIntermediateStats>>.update(
    userId: Long,
    targetPersonId: Long,
    key: String,
    direction: Int,
  ) {
    getOrPut(userId) { mutableMapOf() }.apply {
      val stats = getOrPut(targetPersonId) {
        PersonIntermediateStats()
      }

      if (direction == 1) {
        stats.upvoted.add(key)
        stats.downvoted.remove(key)
      } else if (direction == -1) {
        stats.downvoted.add(key)
        stats.upvoted.remove(key)
      } else if (direction == 0) {
        stats.upvoted.remove(key)
        stats.downvoted.remove(key)
      }
    }
  }
}
