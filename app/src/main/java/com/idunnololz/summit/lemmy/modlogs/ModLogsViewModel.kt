package com.idunnololz.summit.lemmy.modlogs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.utils.ListEngine
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModLogsViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val noAuthApiClient: LemmyApiClient,
    private val accountManager: AccountManager,
) : ViewModel() {

    companion object {

        private const val TAG = "ModLogsViewModel"

        private const val PAGE_ENTRIES_LIMIT = 50
    }

    private var communityRef: CommunityRef? = null
    private var communityView: CommunityView? = null

    val apiInstance: String
        get() = apiClient.instance
    private val modLogEngine = ListEngine<ModEvent>()

    val modLogData = StatefulLiveData<ModLogData>()

    private var modSource: MultiModEventDataSource? = null

    init {
        viewModelScope.launch {
            modLogEngine.items.collect {
                modLogData.postValue(ModLogData(it))
            }
        }
    }

    fun fetchModLogs(pageIndex: Int, force: Boolean = false) {
        Log.d(TAG, "fetchModLogs(): $pageIndex, $force")
        modLogData.setIsLoading()

        val communityRef = communityRef
        val communityView = communityView
        val account = accountManager.currentAccount.value

        viewModelScope.launch {
            val communityIdOrNull: Result<CommunityId?> =
                if (communityRef is CommunityRef.CommunityRefByName) {
                    if (communityView != null && communityView.community.name == communityRef.name) {
                        Result.success(communityView.community.id)
                    } else {
                        noAuthApiClient.getCommunity(
                            account = if (account?.instance == apiInstance) {
                                account
                            } else {
                                null
                            },
                            idOrName = Either.Right(communityRef.getServerId(apiClient.instance)),
                            force = force,
                        ).fold(
                            {
                                this@ModLogsViewModel.communityView = it.community_view

                                Result.success(it.community_view.community.id)
                            },
                            {
                                Result.failure(it)
                            },
                        )
                    }
                } else {
                    Result.success(null)
                }

            if (communityIdOrNull.isFailure) {
                modLogData.postError(communityIdOrNull.exceptionOrNull()!!)
                return@launch
            }

            val modSource = modSource ?: MultiModEventDataSource.create(
                noAuthApiClient,
                communityIdOrNull.getOrThrow(),
                noAuthApiClient.instance,
                PAGE_ENTRIES_LIMIT,
            ).also {
                modSource = it
            }

            val result = modSource.fetchModEvents(pageIndex, force)

            val modEvents = result.fold(
                onSuccess = {
                    it.sortedByDescending { it.ts }
                },
                onFailure = {
                    null
                },
            )

            modLogEngine.addPage(
                page = pageIndex,
                communities = result.fold(
                    onSuccess = {
                        Result.success(modEvents!!)
                    },
                    onFailure = {
                        Result.failure(it)
                    },
                ),
                hasMore = result.fold(
                    onSuccess = {
                        it.isNotEmpty()
                    },
                    onFailure = {
                        true
                    },
                ),
            )
        }
    }

    fun setArguments(instance: String, communityRef: CommunityRef?) {
        noAuthApiClient.changeInstance(instance)
        this.communityRef = communityRef
    }

    fun reset() {
        viewModelScope.launch {
            modLogData.clear()
            fetchModLogs(0, force = true)
        }
    }

    data class ModLogData(
        val data: List<ListEngine.Item<ModEvent>>,
    )
}
