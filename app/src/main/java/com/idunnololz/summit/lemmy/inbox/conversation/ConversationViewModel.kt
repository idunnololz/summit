package com.idunnololz.summit.lemmy.inbox.conversation

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.PageType
import com.idunnololz.summit.lemmy.inbox.repository.InboxRepository
import com.idunnololz.summit.lemmy.inbox.repository.InboxSource
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ConversationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: AccountManager,
    private val apiClient: AccountAwareLemmyClient,
    private val inboxRepositoryFactory: InboxRepository.Factory,
    private val conversationsManager: ConversationsManager,
) : ViewModel() {

    private var inboxRepository: InboxRepository? = null

    private var allMessagesById = mutableMapOf<Int, MessageItem>()
    private var nextPageIndex = 0
    private var lastPageIndex = -1
    private var highestLoadedPageIndex = -1
    private var personId: Long? = null
    private var draftMessage: String? = null

    val conversationInfoModel = MutableLiveData<ConversationInfoModel>()
    val conversationModel = MutableLiveData<ConversationModel>()
    val draftModel = MutableLiveData<String>()
    val loadConversationState = StatefulLiveData<Unit>()
    val commentSentEvent = StatefulLiveData<Unit>()

    fun setup(
        accountId: Long,
        messageInboxItem: InboxItem.MessageInboxItem?,
        conversation: Conversation?,
        newConversation: NewConversation?,
    ): Result<Unit> {
        val currentAccount = accountManager.currentAccount.asAccount

        if (accountId != currentAccount?.id) {
            return Result.failure(RuntimeException())
        }

        val otherPersonId: Long?
        val otherPersonName: String?
        val otherPersonInstance: String?
        val otherPersonIcon: String?

        if (messageInboxItem != null) {
            val isAuthor = currentAccount.id == messageInboxItem.authorId

            otherPersonId = if (isAuthor) {
                messageInboxItem.targetAccountId
            } else {
                messageInboxItem.authorId
            }
            otherPersonIcon = if (isAuthor) {
                messageInboxItem.targetAccountAvatar
            } else {
                messageInboxItem.authorAvatar
            }
            otherPersonName = if (isAuthor) {
                messageInboxItem.targetUserName
            } else {
                messageInboxItem.authorName
            }
            otherPersonInstance = if (isAuthor) {
                messageInboxItem.targetInstance
            } else {
                messageInboxItem.authorInstance
            }
        } else if (conversation != null) {
            otherPersonId = conversation.personId
            otherPersonIcon = conversation.iconUrl
            otherPersonName = conversation.personName
            otherPersonInstance = conversation.personInstance
        } else if (newConversation != null) {
            otherPersonId = newConversation.personId
            otherPersonIcon = newConversation.personAvatar
            otherPersonName = newConversation.personName
            otherPersonInstance = newConversation.personInstance
        } else {
            return Result.failure(RuntimeException())
        }

        conversationInfoModel.postValue(ConversationInfoModel(
            otherPersonId = otherPersonId,
            otherPersonAvatar = otherPersonIcon,
            otherPersonName = otherPersonName,
            otherPersonInstance = otherPersonInstance,
        ))

        personId = otherPersonId

        inboxRepository = inboxRepositoryFactory.create(
            InboxRepository.InboxMultiDataSource(
                listOf(
                    InboxSource(
                        context,
                        CommentSortType.New,
                    ) { page: Int, sortOrder: CommentSortType, limit: Int, force: Boolean ->
                        apiClient.fetchPrivateMessages(
                            page = page,
                            limit = limit,
                            senderId = otherPersonId,
                            unreadOnly = false,
                            force = force,
                        ).fold(
                            onSuccess = {
                                Result.success(
                                    it.map {
                                        it.toMessageItem()
                                    },
                                )
                            },
                            onFailure = {
                                Result.failure(it)
                            },
                        )
                    },
                ),
            ),
        )

        viewModelScope.launch {
            if (otherPersonId != null) {
                val draftData = conversationsManager.getDraft(otherPersonId)

                draftData?.content?.let {
                    draftModel.postValue(it)
                }
            }
        }

        return Result.success(Unit)
    }

    fun loadFirstPage(force: Boolean = false) {
        loadConversationState.setIsLoading()

        viewModelScope.launch {
            loadPage(0, force)
                .onSuccess {
                    conversationModel.postValue(it)

                    loadConversationState.postValue(Unit)
                }
                .onFailure {
                    loadConversationState.postError(it)
                }
        }
    }

    fun loadNextPage(force: Boolean = false) {
        if (loadConversationState.isLoading) {
            return
        }

        loadConversationState.setIsLoading()

        viewModelScope.launch {
            loadPage(nextPageIndex, force)
                .onSuccess {
                    conversationModel.postValue(it)
                    nextPageIndex++

                    loadConversationState.postValue(Unit)
                }
                .onFailure {
                    loadConversationState.postError(it)
                }
        }
    }

    private suspend fun loadPage(
        pageIndex: Int,
        force: Boolean = false,
    ): Result<ConversationModel> = withContext(Dispatchers.Default) {
        val inboxItems = inboxRepository
            ?.getPage(pageIndex, PageType.Conversation, force)

        if (inboxItems == null) {
            return@withContext Result.failure(RuntimeException("Inbox repository is null!"))
        }

        inboxItems.fold(
            onSuccess = {
                it.items.filterIsInstance<MessageItem>()
                    .forEach { messageItem ->
                        allMessagesById[messageItem.id] = messageItem
                    }

                val allMessages = allMessagesById.values
                    .sortedByDescending { it.lastUpdateTs }
                val account = accountManager.currentAccount.asAccount

                if (!it.hasMore) {
                    lastPageIndex = pageIndex
                }

                highestLoadedPageIndex = max(highestLoadedPageIndex, pageIndex)

                Result.success(
                    ConversationModel(
                        accountId = account?.id,
                        allMessages = allMessages,
                        nextPageIndex = nextPageIndex,
                        hasMore =
                        if (lastPageIndex < 0) {
                            true
                        } else {
                            highestLoadedPageIndex < lastPageIndex
                        },
                    ),
                )
            },
            onFailure = {
                Result.failure(it)
            },
        )
    }

    fun markAsRead(id: Int) {
        viewModelScope.launch {
            apiClient.markPrivateMessageAsRead(id, true)
            conversationsManager.refreshConversations()
            personId?.let {
                conversationsManager.updateConversation(it)
            }
        }
    }

    fun sendComment(accountId: Long, content: String) {
        commentSentEvent.setIsLoading()

        viewModelScope.launch {
            val account = accountManager.getAccountById(accountId)
                ?: return@launch
            val personId = personId
                ?: return@launch

            apiClient
                .createPrivateMessage(
                    content = content,
                    recipient = personId,
                    account = account,
                )
                .onFailure {
                    commentSentEvent.postError(it)
                }
                .onSuccess {
                    commentSentEvent.postValue(Unit)
                    loadFirstPage(force = true)
                    conversationsManager.updateConversation(personId)
                }
        }
    }

    fun saveDraft(text: String?) {
        val personId = personId ?: return

        conversationsManager.saveDraftAsync(personId, text)
    }
}
