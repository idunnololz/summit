package com.idunnololz.summit.lemmy.communityInfo

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.toFullAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.lemmy.CommunityId
import com.idunnololz.summit.api.dto.lemmy.CommunityResponse
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.DeleteCommunity
import com.idunnololz.summit.api.dto.lemmy.GetCommunityResponse
import com.idunnololz.summit.api.dto.lemmy.GetSiteResponse
import com.idunnololz.summit.api.dto.lemmy.SubscribedType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.util.Event
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.RuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

@HiltViewModel
class CommunityInfoViewModel @Inject constructor(
  private val apiClient: AccountAwareLemmyClient,
  private val noAuthApiClient: LemmyApiClient,
  private val apiClientFactor: LemmyApiClient.Factory,
  val accountManager: AccountManager,
  private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

  companion object {
    private const val TAG = "CommunityInfoViewModel"
  }

  private var communityRef: CommunityRef? = null
  private var communityRefInstanceUsed: String? = null

  val siteOrCommunity = StatefulLiveData<SiteOrCommunityResult>()
  val multiCommunity = StatefulLiveData<MultiCommunityData>()
  private val subscribeEvent = MutableLiveData<Event<Result<CommunityView>>>()
  val deleteCommunityResult = StatefulLiveData<DeleteCommunityResult>()

  private var pollCommunityInfoJob: Job? = null

  val instance
    get() = apiClient.instance

  init {
    siteOrCommunity.observeForever a@{
      if (it !is StatefulData.Success) {
        return@a
      }
      val result = it.data.response.getOrNull() ?: return@a
      if (result.community_view.subscribed == SubscribedType.Pending) {
        pollCommunityInfoForSubscribedStatus(
          result.community_view.community.toCommunityRef(),
        )
      }
    }
  }

  private fun fetchCommunityOrSiteInfo(communityRef: CommunityRef?, force: Boolean = false) {
    siteOrCommunity.setIsLoading()

    if (communityRef == null) {
      siteOrCommunity.setIdle()
      return
    }

    viewModelScope.launch {
      if (communityRef is CommunityRef.MultiCommunity) {
        siteOrCommunity.postError(RuntimeException())
        return@launch
      } else if (communityRef is CommunityRef.ModeratedCommunities) {
        siteOrCommunity.setIdle()
        fetchModeratedCommunities(communityRef)
        return@launch
      } else if (communityRef is CommunityRef.Subscribed) {
        siteOrCommunity.setIdle()
        fetchSubscribedCommunities(communityRef)
        return@launch
      } else if (communityRef is CommunityRef.AllSubscribed) {
        siteOrCommunity.setIdle()
        fetchAllSubscribedCommunities(communityRef)
        return@launch
      }

      val result = when (communityRef) {
        is CommunityRef.All -> {
          Either.Left(apiClient.fetchSiteWithRetry(force))
        }
        is CommunityRef.CommunityRefByName -> {
          Either.Right(
            apiClient.fetchCommunityWithRetry(
              Either.Right(communityRef.getServerId(instance)),
              force,
            ),
          )
        }
        is CommunityRef.Local -> {
          if (apiClient.instance != communityRef.instance && communityRef.instance != null) {
            noAuthApiClient.changeInstance(communityRef.instance)
            Either.Left(noAuthApiClient.fetchSiteWithRetry(null, force))
          } else {
            Either.Left(apiClient.fetchSiteWithRetry(force))
          }
        }
        is CommunityRef.Subscribed -> {
          error("unreachable code")
        }
        is CommunityRef.MultiCommunity -> error("unreachable code")
        is CommunityRef.ModeratedCommunities -> error("unreachable code")
        is CommunityRef.AllSubscribed -> error("unreachable code")
      }

      result
        .onLeft {
          it
            .onSuccess {
              siteOrCommunity.postValue(
                SiteOrCommunityResult(
                  response = Either.Left(it),
                  force = force,
                ),
              )
            }
            .onFailure {
              siteOrCommunity.postError(it)
            }
        }
        .onRight {
          it
            .onSuccess {
              siteOrCommunity.postValue(
                SiteOrCommunityResult(
                  response = Either.Right(it),
                  force = force,
                ),
              )
            }
            .onFailure {
              siteOrCommunity.postError(it)
            }
        }
    }
  }

  private fun pollCommunityInfoForSubscribedStatus(communityRef: CommunityRef.CommunityRefByName) {
    Log.d(TAG, "polling community info")

    pollCommunityInfoJob?.cancel()

    pollCommunityInfoJob = viewModelScope.launch {
      while (communityRef == this@CommunityInfoViewModel.communityRef) {
        delay(5_000)

        val result = apiClient.fetchCommunityWithRetry(
          Either.Right(communityRef.getServerId(instance)),
          force = true,
        ).getOrNull() ?: continue

        // We ignore failures here...
        if (result.community_view.subscribed != SubscribedType.Pending) {
          Log.d(TAG, "Community subscribed status updated!")

          refetchCommunityOrSite(force = false)
          break
        }
      }
    }
  }

  private suspend fun fetchModeratedCommunities(communityRef: CommunityRef) {
    multiCommunity.setIsLoading()
    val fullAccount = accountInfoManager.currentFullAccount.value

    if (fullAccount == null) {
      multiCommunity.setError(NotAuthenticatedException())
      return
    }

    val results = flow {
      val moderatedCommunityIds = fullAccount
        .accountInfo
        .miscAccountInfo
        ?.modCommunityIds
        ?: listOf()
      moderatedCommunityIds.forEach { community ->
        val result = apiClient
          .fetchCommunityWithRetry(Either.Left(community), false)
        emit(result)
      }
    }.flowOn(Dispatchers.Default).toList()

    val successResults = mutableListOf<GetCommunityResponse>()
    for (result in results) {
      if (result.isSuccess) {
        successResults.add(result.getOrThrow())
      } else {
        multiCommunity.setError(requireNotNull(result.exceptionOrNull()))
        return
      }
    }

    multiCommunity.setValue(
      MultiCommunityData(
        communityRef = communityRef,
        instance = instance,
        icon = R.drawable.outline_shield_24,
        communitiesData = successResults.map {
          CommunityInfo(
            it.community_view.community.toCommunityRef(),
            it.community_view.community.id,
            it.community_view.community.name,
            it.community_view.community.title,
            it.community_view.community.icon,
          )
        },
      ),
    )
  }

  private suspend fun fetchSubscribedCommunities(communityRef: CommunityRef) {
    multiCommunity.setIsLoading()
    val fullAccount = accountInfoManager.currentFullAccount.value

    if (fullAccount == null) {
      multiCommunity.setError(NotAuthenticatedException())
      return
    }
    val subscriptions = fullAccount
      .accountInfo
      .subscriptions
      ?: listOf()

    multiCommunity.setValue(
      MultiCommunityData(
        communityRef = communityRef,
        instance = instance,
        icon = R.drawable.baseline_subscriptions_24,
        communitiesData = subscriptions.map {
          CommunityInfo(
            it.toCommunityRef(),
            it.id,
            it.name,
            it.title,
            it.icon,
          )
        },
      ),
    )
  }

  private suspend fun fetchAllSubscribedCommunities(communityRef: CommunityRef) {
    multiCommunity.setIsLoading()

    val oneUseClient = apiClientFactor.create()
    val fullAccounts = accountManager.getAccounts()
      .mapNotNull {
        oneUseClient.changeInstance(it.instance)
        val site = oneUseClient.fetchSiteWithRetry(auth = it.jwt, force = false)
        site.getOrNull()?.toFullAccount(it)
      }
    val allSubscriptions = fullAccounts.flatMap { it.accountInfo.subscriptions ?: listOf() }

    multiCommunity.setValue(
      MultiCommunityData(
        communityRef = communityRef,
        instance = instance,
        icon = R.drawable.baseline_subscriptions_24,
        communitiesData = allSubscriptions.map {
          CommunityInfo(
            it.toCommunityRef(),
            it.id,
            it.name,
            it.title,
            it.icon,
          )
        },
      ),
    )
  }

  fun updateSubscriptionStatus(communityId: Int, subscribe: Boolean) {
    viewModelScope.launch {
      val result = apiClient.followCommunityWithRetry(communityId, subscribe)

      subscribeEvent.postValue(Event(result))
      result
        .onSuccess {
          if (communityRef == it.community.toCommunityRef()) {
            val communityResult = siteOrCommunity.valueOrNull
            val communityView = communityResult?.response?.getOrNull()

            if (communityView != null) {
              siteOrCommunity.postValue(
                communityResult.copy(
                  response = Either.Right(
                    communityView.copy(
                      community_view = it,
                    ),
                  ),
                ),
              )
            }
          }

          delay(1000)

          refetchCommunityOrSite(force = true)
          accountInfoManager.refreshAccountInfo(force = true)

          Log.d(TAG, "subscription status: " + it.subscribed)
        }
    }
  }

  fun onCommunityChanged(communityRef: CommunityRef?) {
    if (this.communityRef == communityRef && communityRefInstanceUsed == instance) {
      return
    }

    communityRefInstanceUsed = instance
    this.communityRef = communityRef

    fetchCommunityOrSiteInfo(communityRef)
  }

  fun refetchCommunityOrSite(force: Boolean) {
    val communityRef = communityRef
    fetchCommunityOrSiteInfo(communityRef, force)
  }

  fun deleteCommunity(communityId: CommunityId, delete: Boolean) {
    deleteCommunityResult.setIsLoading()
    viewModelScope.launch {
      apiClient.deleteCommunity(
        DeleteCommunity(
          community_id = communityId,
          deleted = delete,
          auth = "",
        ),
      )
        .onFailure {
          deleteCommunityResult.postError(it)
        }
        .onSuccess {
          deleteCommunityResult.postValue(DeleteCommunityResult(it))
        }
    }
  }

  data class DeleteCommunityResult(
    val communityResponse: CommunityResponse,
  )

  data class SiteOrCommunityResult(
    val response: Either<GetSiteResponse, GetCommunityResponse>,
    val force: Boolean,
  )

  data class CommunityInfo(
    val communityRef: CommunityRef.CommunityRefByName,
    val id: CommunityId,
    val name: String,
    val title: String,
    val icon: String?,
  )

  data class MultiCommunityData(
    val communityRef: CommunityRef,
    val instance: String,
    @DrawableRes val icon: Int,
    val communitiesData: List<CommunityInfo>,
  )
}
