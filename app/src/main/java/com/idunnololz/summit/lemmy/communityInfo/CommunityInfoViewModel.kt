package com.idunnololz.summit.lemmy.communityInfo

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.CommunityResponse
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.DeleteCommunity
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.SubscribedType
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.util.Event
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.lang.RuntimeException
import javax.inject.Inject

@HiltViewModel
class CommunityInfoViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val noAuthApiClient: LemmyApiClient,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

    companion object {
        private const val TAG = "CommunityInfoViewModel"
    }

    private var communityRef: CommunityRef? = null

    val siteOrCommunity = StatefulLiveData<SiteOrCommunityResult>()
    val multiCommunity = StatefulLiveData<List<GetCommunityResponse>>()
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
                pollCommunityInfoForSubscribedStatus(result.community_view.community.toCommunityRef())
            }
        }
    }

    private fun fetchCommunityOrSiteInfo(communityRef: CommunityRef, force: Boolean = false) {
        siteOrCommunity.setIsLoading()
        viewModelScope.launch {
            if (communityRef is CommunityRef.MultiCommunity) {
                siteOrCommunity.postError(RuntimeException())
                return@launch
            } else if (communityRef is CommunityRef.ModeratedCommunities) {
                siteOrCommunity.setIdle()
                fetchModeratedCommunities()
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
                    Either.Left(apiClient.fetchSiteWithRetry(force))
                }
                is CommunityRef.MultiCommunity -> error("unreachable code")
                is CommunityRef.ModeratedCommunities -> error("unreachable code")
            }

            result
                .onLeft {
                    it
                        .onSuccess {
                            siteOrCommunity.postValue(
                                SiteOrCommunityResult(
                                    response = Either.Left(it),
                                    force = force,
                                )
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
                                )
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

    private suspend fun fetchModeratedCommunities() {
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

        multiCommunity.setValue(successResults)
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
                                        )
                                    )
                                )
                            )
                        }
                    }

                    delay(1000)

                    refetchCommunityOrSite(force = true)
                    accountInfoManager.refreshAccountInfo()

                    Log.d(TAG, "subscription status: " + it.subscribed)
                }
        }
    }

    fun onCommunityChanged(communityRef: CommunityRef) {
        this.communityRef = communityRef

        fetchCommunityOrSiteInfo(communityRef)
    }

    fun refetchCommunityOrSite(force: Boolean) {
        val communityRef = communityRef ?: return
        fetchCommunityOrSiteInfo(communityRef, force)
    }

    fun deleteCommunity(communityId: CommunityId, delete: Boolean) {
        deleteCommunityResult.setIsLoading()
        viewModelScope.launch {
            apiClient.deleteCommunity(
                DeleteCommunity(
                    community_id = communityId,
                    deleted = delete,
                    auth = ""
                )
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
}
