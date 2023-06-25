package com.idunnololz.summit.main.community_info_pane

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountInfoManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.databinding.CommunityInfoPaneBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.util.Event
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityInfoViewModel @Inject constructor(
    private val communityInfoControllerFactory: CommunityInfoController.Factory,
    private val apiClient: AccountAwareLemmyClient,
    private val accountInfoManager: AccountInfoManager,
) : ViewModel() {

    companion object {
        private const val TAG = "CommunityInfoViewModel"
    }

    private var communityRef: CommunityRef? = null

    val siteOrCommunity = StatefulLiveData<Either<GetSiteResponse, CommunityView>>()
    private val subscribeEvent = MutableLiveData<Event<Result<CommunityView>>>()

    fun createController(
        binding: CommunityInfoPaneBinding,
        viewLifecycleOwner: LifecycleOwner,
    ) =
        communityInfoControllerFactory.create(
            this,
            binding,
            viewLifecycleOwner,
        )

    private fun fetchCommunityOrSiteInfo(communityRef: CommunityRef, force: Boolean = false) {
        siteOrCommunity.setIsLoading()
        viewModelScope.launch {
            val result = when (communityRef) {
                is CommunityRef.All -> {
                    Either.Left(apiClient.fetchSiteWithRetry(force))
                }
                is CommunityRef.CommunityRefByName -> {
                    Either.Right(apiClient.fetchCommunityWithRetry(Either.Right(communityRef.getServerId()), force))
                }
                is CommunityRef.CommunityRefByObj -> {
                    Either.Right(apiClient.fetchCommunityWithRetry(Either.Right(communityRef.getServerId()), force))
                }
                is CommunityRef.Local -> {
                    Either.Left(apiClient.fetchSiteWithRetry(force))
                }
                is CommunityRef.Subscribed -> {
                    Either.Left(apiClient.fetchSiteWithRetry(force))
                }
            }

            result
                .onLeft {
                    it
                        .onSuccess {
                            siteOrCommunity.postValue(Either.Left(it))
                        }
                        .onFailure {
                            siteOrCommunity.postError(it)
                        }
                }
                .onRight {
                    it
                        .onSuccess {
                            siteOrCommunity.postValue(Either.Right(it))
                        }
                        .onFailure {
                            siteOrCommunity.postError(it)
                        }
                }

        }
    }

    fun updateSubscriptionStatus(communityId: Int, subscribe: Boolean) {
        viewModelScope.launch {
            val result = apiClient.followCommunityWithRetry(communityId, subscribe)

            subscribeEvent.postValue(Event(result))
            result
                .onSuccess {
                    if (communityRef == it.community.toCommunityRef()) {
                        siteOrCommunity.postValue(Either.Right(it))
                    }

                    delay(1000)

                    refetchCommunityOrSite(force = true)
                    accountInfoManager.fetchAccountInfo()

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
}