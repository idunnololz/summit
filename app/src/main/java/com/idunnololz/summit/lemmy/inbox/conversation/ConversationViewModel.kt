package com.idunnololz.summit.lemmy.inbox.conversation

import android.content.Context
import androidx.lifecycle.ViewModel
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.InboxRepository
import com.idunnololz.summit.lemmy.inbox.repository.InboxSource
import com.idunnololz.summit.util.dateStringToTs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: AccountAwareLemmyClient,
    private val inboxRepositoryFactory: InboxRepository.Factory
) : ViewModel() {

    fun setup(personId: PersonId) {
        inboxRepositoryFactory.create(
            InboxRepository.InboxMultiDataSource(listOf(
                InboxSource(
                    context,
                    CommentSortType.New,
                ) { page: Int, sortOrder: CommentSortType, limit: Int, force: Boolean ->
                    apiClient.fetchPrivateMessages(
                        page = page,
                        limit = limit,
                        senderId = personId,
                        unreadOnly = false,
                        force = force,
                    ).fold(
                        onSuccess = {
                            Result.success(it.map {
                                it.toMessageItem()
                            })
                        },
                        onFailure = {
                            Result.failure(it)
                        },
                    )
                }
            ))
        )
    }
}
