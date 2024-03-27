package com.idunnololz.summit.lemmy.createOrEditCommunity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.CommunityResponse
import com.idunnololz.summit.api.dto.EditCommunity
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.LanguageId
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CreateOrEditCommunityViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

    val getCommunityResult = StatefulLiveData<GetCommunityResponse>()
    val currentCommunityData = MutableLiveData<CommunityData>()
    val updateCommunityResult = StatefulLiveData<CommunityResponse>()

    val instance
        get() = apiClient.instance

    fun loadCommunityInfo(communityRef: CommunityRef.CommunityRefByName?) {
        if (communityRef == null) {
            setCommunityIfNotSet(null)
            return
        }

        getCommunityResult.setIsLoading()

        viewModelScope.launch(Dispatchers.Default) {
            val result = apiClient.fetchCommunityWithRetry(
                Either.Right(communityRef.getServerId(instance)),
                force = true,
            )

            result
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        setCommunityIfNotSet(it)
                        getCommunityResult.setValue(it)
                    }
                }
                .onFailure {
                    getCommunityResult.postError(it)
                }
        }

    }

    private fun setCommunityIfNotSet(community: GetCommunityResponse?) {
        if (currentCommunityData.isInitialized) {
            return
        }

        currentCommunityData.value = community?.let {
            CommunityData(
                it.community_view.community,
                it.discussion_languages,
            )
        } ?: CommunityData(
            Community(
                id = 0,
                name = "",
                title = "",
                description = null,
                removed = false,
                published = "",
                updated = null,
                deleted = false,
                nsfw = false,
                actor_id = "",
                local = true,
                icon = null,
                banner = null,
                hidden = false,
                posting_restricted_to_mods = false,
                instance_id = 0,
            ),
            null,
        )
    }

    fun update(updateFn: (Community) -> Community) {
        val curCommunityData = currentCommunityData.value ?: return

        currentCommunityData.value = curCommunityData.copy(
            community = updateFn(curCommunityData.community)
        )
    }

    fun saveChanges() {
        updateCommunityResult.setIsLoading()

        val curCommunityData = currentCommunityData.value ?: return
        val curCommunity = curCommunityData.community

        viewModelScope.launch(Dispatchers.Default) {
            val result = apiClient.updateCommunity(
                EditCommunity(
                    community_id = curCommunity.id,
                    title = curCommunity.title,
                    description = curCommunity.description,
                    icon = curCommunity.icon,
                    banner = curCommunity.banner,
                    nsfw = curCommunity.nsfw,
                    posting_restricted_to_mods = curCommunity.posting_restricted_to_mods,
                    discussion_languages = curCommunityData.discussionLanguages,
                    auth = "",
                )
            )

            result
                .onSuccess {
                    updateCommunityResult.postValue(it)
                }
                .onFailure {
                    updateCommunityResult.postError(it)
                }
        }
    }

    data class CommunityData(
        val community: Community,
        val discussionLanguages: List<LanguageId>? = null,
    )
}