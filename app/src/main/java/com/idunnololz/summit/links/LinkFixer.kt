package com.idunnololz.summit.links

import android.util.Log
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.consts.LEMMY_INSTANCES
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class LinkFixer @Inject constructor(
  private val siteBackendHelper: SiteBackendHelper,
  private val accountAwareLemmyClient: AccountAwareLemmyClient,
  private val coroutineScopeFactory: CoroutineScopeFactory,
) {

  companion object {
    private const val TAG = "LinkFixer"
  }

  private var knownInstances: Set<String> = initialKnownInstances()
  @Volatile private var notAnInstance: Set<String> = setOf()
  @Volatile private var hostToInstance: Map<String, String> = mapOf()

  private val coroutineScope = coroutineScopeFactory.create()

  sealed interface FixPageRefResult {
    val pageRef: PageRef

    class Success(override val pageRef: PageRef): FixPageRefResult
    class InvalidFedInstance(override val pageRef: PageRef): FixPageRefResult
    class NoInformation(override val pageRef: PageRef): FixPageRefResult
  }

  init {
    coroutineScope.launch {
      accountAwareLemmyClient.instanceFlow.collect {
        knownInstances = initialKnownInstances()
        notAnInstance = setOf()
        hostToInstance = mapOf()
      }
    }
  }

  fun fixPageRefSync(pageRef: PageRef): FixPageRefResult {
    val instance = when (pageRef) {
      is CommentRef -> pageRef.instance
      is CommunityRef.All -> pageRef.instance
      is CommunityRef.CommunityRefByName -> pageRef.instance
      is CommunityRef.Local -> pageRef.instance
      is CommunityRef.ModeratedCommunities -> pageRef.instance
      is CommunityRef.MultiCommunity -> null
      is CommunityRef.AllSubscribed -> null
      is CommunityRef.Subscribed -> pageRef.instance
      is PersonRef -> pageRef.instance
      is PostRef -> pageRef.instance
    }

    if (instance == null) {
      return FixPageRefResult.Success(pageRef)
    }

    if (knownInstances.contains(instance)) {
      return FixPageRefResult.Success(pageRef)
    }

    if (notAnInstance.contains(instance)) {
      return FixPageRefResult.InvalidFedInstance(pageRef)
    }

    val newInstance = hostToInstance[instance]
    if (!newInstance.isNullOrBlank()) {
      return FixPageRefResult.Success(pageRef.updateInstance(newInstance))
    }

    return FixPageRefResult.NoInformation(pageRef)
  }

  suspend fun fixPageRef(pageRef: PageRef): FixPageRefResult = withContext(Dispatchers.Default) a@{
    val instance = when (pageRef) {
      is CommentRef -> pageRef.instance
      is CommunityRef.All -> pageRef.instance
      is CommunityRef.CommunityRefByName -> pageRef.instance
      is CommunityRef.Local -> pageRef.instance
      is CommunityRef.ModeratedCommunities -> pageRef.instance
      is CommunityRef.MultiCommunity -> null
      is CommunityRef.AllSubscribed -> null
      is CommunityRef.Subscribed -> pageRef.instance
      is PersonRef -> pageRef.instance
      is PostRef -> pageRef.instance
    } ?: return@a FixPageRefResult.Success(pageRef)

    if (knownInstances.contains(instance)) {
      return@a FixPageRefResult.Success(pageRef)
    }

    return@a when (pageRef) {
      is CommentRef,
      is PostRef -> {
        FixPageRefResult.Success(pageRef)
      }
      is CommunityRef.CommunityRefByName -> {
        accountAwareLemmyClient
          .fetchCommunityWithRetry(
            Either.Right(pageRef.fullName),
            force = false,
          ).fold(
            {
              knownInstances += instance
              FixPageRefResult.Success(pageRef)
            },
            {
              // Do not blacklist instance just in case the mention has a typo or something...
              // notAnInstance += instance
              FixPageRefResult.InvalidFedInstance(pageRef)
            }
          )
      }
      is PersonRef -> {
        when (pageRef) {
          is PersonRef.PersonRefByName -> {
            accountAwareLemmyClient
              .fetchPersonByNameWithRetry(
                name = pageRef.fullName,
                force = false,
              ).fold(
                {
                  knownInstances += instance
                  FixPageRefResult.Success(pageRef)
                },
                {
                  // Do not blacklist instance just in case the mention has a typo or something...
                  // notAnInstance += instance
                  FixPageRefResult.InvalidFedInstance(pageRef)
                }
              )
          }
          is PersonRef.PersonRefById,
          is PersonRef.PersonRefComplete -> {
            FixPageRefResult.Success(pageRef)
          }
        }
      }
      is CommunityRef.Local,
      is CommunityRef.All,
      is CommunityRef.ModeratedCommunities,
      is CommunityRef.Subscribed -> {
        checkIfValidInstance(instance)
        if (knownInstances.contains(hostToInstance[instance] ?: instance)) {
          FixPageRefResult.Success(pageRef)
        } else {
          FixPageRefResult.InvalidFedInstance(pageRef)
        }
      }
      is CommunityRef.MultiCommunity,
      is CommunityRef.AllSubscribed ->
        FixPageRefResult.Success(pageRef)
    }
  }

  private suspend fun checkIfValidInstance(possibleInstance: String) {
    var tokens = possibleInstance.split(".")
    while (tokens.size >= 2) {
      val possibleInstance = tokens.joinToString(separator = ".")

      val result = siteBackendHelper.fetchApiInfo(possibleInstance)

      Log.d(TAG, "Evaluating $possibleInstance")

      if (result.getOrNull()?.backendType?.isLemmy == true) {
        Log.d(TAG, "$possibleInstance is a lemmy instance!")
        knownInstances = knownInstances + possibleInstance

        hostToInstance = hostToInstance + (possibleInstance to possibleInstance)
      } else {
        val exception = result.exceptionOrNull()
        if (exception !is ClientApiException) {
          break
        }

        tokens = tokens.drop(1)
      }
    }

    notAnInstance = notAnInstance + possibleInstance
  }

  private fun PageRef.updateInstance(newInstance: String): PageRef = when (this) {
    is CommentRef -> this.copy(instance = newInstance)
    is CommunityRef.All -> this.copy(instance = newInstance)
    is CommunityRef.CommunityRefByName -> this.copy(instance = newInstance)
    is CommunityRef.Local -> this.copy(instance = newInstance)
    is CommunityRef.ModeratedCommunities -> this.copy(instance = newInstance)
    is CommunityRef.MultiCommunity -> this
    is CommunityRef.AllSubscribed -> this
    is CommunityRef.Subscribed -> this.copy(instance = newInstance)
    is PersonRef.PersonRefByName -> this.copy(instance = newInstance)
    is PersonRef.PersonRefById -> this.copy(instance = newInstance)
    is PersonRef.PersonRefComplete -> this.copy(instance = newInstance)
    is PostRef -> this.copy(instance = newInstance)
  }

  private fun initialKnownInstances() =
    LEMMY_INSTANCES.toSet() +
    setOf(
      // piefed
      "piefed.social",
      "piefed.world",
      "piefed.blahaj.zone",
      "piefed.ca",
      "piefed.zip",
      "feddit.online",
      "fedinsfw.app",
    )
}
