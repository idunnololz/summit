package com.idunnololz.summit.lemmy.personOptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonOptionsViewModel @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
) : ViewModel() {

    val personData = StatefulLiveData<PersonData>()

    fun loadPersonData(personRef: PersonRef, force: Boolean = false) {
        personData.setIsLoading()

        viewModelScope.launch(Dispatchers.Default) {
            apiClient
                .fetchPersonByNameWithRetry(
                    name = personRef.fullName,
                    sortType = SortType.New,
                    page = 1,
                    limit = 1,
                    force = force
                )
                .onSuccess {
//                    personData.postValue(
//                        PersonData(
//                            isBlocked = it.
//                        )
//                    )
                }
        }
    }

    data class PersonData(
        val isBlocked: Boolean
    )
}