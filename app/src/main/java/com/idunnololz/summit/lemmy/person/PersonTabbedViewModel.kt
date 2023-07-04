package com.idunnololz.summit.lemmy.person

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.R
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityModeratorView
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class PersonTabbedViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

    val personData = StatefulLiveData<PersonDetailsData>()

    fun fetchPersonIfNotDone(personRef: PersonRef) {
        if (personData.valueOrNull != null) return

        personData.setIsLoading()

        viewModelScope.launch {
            apiClient.changeInstance(personRef.instance)

            val result = when (personRef) {
                is PersonRef.PersonRefByName -> {
                    apiClient.fetchPersonByNameWithRetry(personRef.name)
                }
            }

            result
                .onSuccess {
                    personData.postValue(
                        PersonDetailsData(
                            it.person_view,
                            it.comments,
                            it.posts,
                            it.moderates,
                        )
                    )
                }
                .onFailure {
                    personData.postError(it)
                }
        }
    }

    data class PersonDetailsData(
        val personView: PersonView,
        val comments: List<CommentView>,
        val posts: List<PostView>,
        val moderates: List<CommunityModeratorView>,
    )
}