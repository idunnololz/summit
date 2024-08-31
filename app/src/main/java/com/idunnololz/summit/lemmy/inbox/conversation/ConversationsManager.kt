package com.idunnololz.summit.lemmy.inbox.conversation

import android.content.Context
import android.util.Log
import com.idunnololz.summit.BuildConfig
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.account.stableId
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsManager
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.db.ConversationEntriesDao
import com.idunnololz.summit.lemmy.utils.stateStorage.AccountStateStorage
import com.idunnololz.summit.lemmy.utils.stateStorage.StateStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class ConversationsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: AccountAwareLemmyClient,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val accountManager: AccountManager,
    private val conversationEntriesDao: ConversationEntriesDao,
    private val stateStorageManager: StateStorageManager,
    private val draftsManager: DraftsManager,
) {

    companion object {
        private const val TAG = "ConversationsManager"

        private const val PAGE_SIZE = 50

        /**
         * When loading conversations, the maximum number of messages to fetch. Any messages above
         * this limit will not be fetched.
         */
        const val CONVERSATION_MAX_MESSAGE_REFRESH_LIMIT = 1000
    }

    private val coroutineScope = coroutineScopeFactory.create()
    val conversationsFlow = MutableStateFlow(ConversationsModel(isLoaded = false))

    private val pageSize = PAGE_SIZE

    private var stateStorage: AccountStateStorage? = null
    private var conversationsByPersonId = mutableMapOf<PersonId, Conversation>()
    private var draftsByPersonId = mapOf<PersonId, DbMessageDraft>()
    private var currentAccount: Account? = null
    private var conversationsLoadedFromDb: Boolean = false
    private var conversationEarliestMessageTs: Long? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val conversationContext = Dispatchers.Default.limitedParallelism(1)

    fun init() {
        currentAccount = accountManager.currentAccount.asAccount
        setupForAccount()

        if (BuildConfig.DEBUG) {
//            runBlocking {
//                conversationEntriesDao.deleteAll()
//            }
//            stateStorage?.lastConversationRefreshTs = 0

            stateStorage?.conversationEarliestMessageTs = null
        }

        accountManager.addOnAccountChangedListener(object : AccountManager.OnAccountChangedListener {
            override suspend fun onAccountChanged(newAccount: Account?) {
                invalidate()
                currentAccount = newAccount
                setupForAccount()
            }

            override suspend fun onAccountSigningOut(account: Account) {
                conversationEntriesDao.deleteConversationsForAccount(account.stableId)
            }
        })
    }

    suspend fun refreshConversations(senderId: Long? = null): Result<ConversationsModel> {
        loadConversationsFromDbIfNeeded()

        val refreshTime = System.currentTimeMillis()
        val stateStorage = stateStorage
            ?: return Result.failure(RuntimeException("stateStorage is null!"))
        val lastConversationRefreshTs = stateStorage.lastConversationRefreshTs
        val earliestConversationRefreshTs = stateStorage.conversationEarliestMessageTs

        val allMessages = mutableListOf<InboxItem.MessageInboxItem>()
        var oldestMessageTs = System.currentTimeMillis()

        var page = 1
        var messagesLoaded = 0
        var earliestMessage: InboxItem.MessageInboxItem? = null
        var messageLimitTriggered = false

        while (true) {
            val result = apiClient.fetchPrivateMessages(
                page = page,
                limit = pageSize,
                senderId = senderId,
                unreadOnly = false,
                force = true,
            ).fold(
                onSuccess = {
                    Result.success(
                        it.map { InboxItem.MessageInboxItem(it) },
                    )
                },
                onFailure = {
                    Result.failure(it)
                },
            )

            result.exceptionOrNull()?.let {
                return Result.failure(it)
            }

            val messagesResult = result.getOrThrow()
            val hasMore = messagesResult.size >= pageSize

            for (message in messagesResult) {
                allMessages.add(message)

                oldestMessageTs = min(message.lastUpdateTs, oldestMessageTs)

                if (earliestMessage == null) {
                    earliestMessage = message
                } else if (earliestMessage.lastUpdateTs > message.lastUpdateTs) {
                    earliestMessage = message
                }
            }

            messagesLoaded += messagesResult.size

            if (oldestMessageTs <= lastConversationRefreshTs || !hasMore) {
                break
            }

            if (messagesLoaded >= CONVERSATION_MAX_MESSAGE_REFRESH_LIMIT) {
                messageLimitTriggered = true
                break
            }

            page++
        }

        Log.d(
            TAG,
            "lastConversationRefreshTs: $lastConversationRefreshTs " +
                "earliestMessageTs: ${earliestMessage?.lastUpdateTs} " +
                "messagesLoaded: $messagesLoaded " +
                "messageLimitTriggered: $messageLimitTriggered",
        )

        withContext(conversationContext) {
            allMessages.forEach {
                processMessage(it)
            }

            commitToDb()
        }

        stateStorage.lastConversationRefreshTs = refreshTime

        if (messageLimitTriggered && earliestMessage != null) {
            stateStorage.conversationEarliestMessageTs = earliestMessage.lastUpdateTs
        }

        if (stateStorage.conversationEarliestMessageTs == 0L) {
            conversationEarliestMessageTs = null
        } else {
            conversationEarliestMessageTs = stateStorage.conversationEarliestMessageTs
        }

        loadDrafts()

        publishModel()

        return Result.success(conversationsFlow.value)
    }

    suspend fun updateConversation(otherPersonId: Long) {
        refreshConversations(senderId = otherPersonId)
    }

    fun saveDraftAsync(otherPersonId: Long, draftContent: String?) {
        coroutineScope.launch {
            saveDraft(otherPersonId, draftContent)
        }
    }

    suspend fun saveDraft(otherPersonId: Long, draftContent: String?) {
        loadConversationsFromDbIfNeeded()

        val currentAccount = currentAccount
            ?: return
        val conversation = conversationsByPersonId[otherPersonId]
            ?: return

        val existingDraft = draftsByPersonId[conversation.personId]
        var didDbChange = false

        if (draftContent.isNullOrBlank()) {
            existingDraft?.entryId?.let {
                draftsManager.deleteDraftWithId(it)
                didDbChange = true
            }
        } else {
            val draftData = DraftData.MessageDraftData(
                targetAccountId = conversation.personId,
                targetInstance = conversation.personInstance,
                content = draftContent,
                accountId = currentAccount.id,
                accountInstance = currentAccount.instance,
            )

            draftsManager.updateDraft(
                entryId = existingDraft?.entryId ?: 0L,
                draftData = draftData,
                showToast = false,
            )
            didDbChange = true
        }

        if (didDbChange) {
            loadDrafts()

            publishModel()
        }
    }

    suspend fun getDraft(otherPersonId: Long): DraftData.MessageDraftData? {
        loadConversationsFromDbIfNeeded()

        val conversation = conversationsByPersonId[otherPersonId]
            ?: return null

        val existingDraft = draftsByPersonId[conversation.personId]

        return existingDraft?.draftData
    }

    private fun publishModel() {
        val conversationsModel = ConversationsModel(
            conversations = conversationsByPersonId.values.toList().sortedByDescending { it.ts },
            drafts = draftsByPersonId,
            conversationEarliestMessageTs = conversationEarliestMessageTs,
        )
        conversationsFlow.value = conversationsModel
    }

    private fun processMessage(message: InboxItem.MessageInboxItem) {
        val currentAccount = currentAccount ?: return

        val isMessageAuthor = message.authorId == currentAccount.id
        val otherPersonId = if (isMessageAuthor) {
            message.targetAccountId
        } else {
            message.authorId
        } ?: return
        val otherPersonInstance = if (isMessageAuthor) {
            message.targetInstance
        } else {
            message.authorInstance
        } ?: return
        val otherPersonName = if (isMessageAuthor) {
            message.targetUserName
        } else {
            message.authorName
        }

        val existingConversation = conversationsByPersonId[otherPersonId]

        val iconUrl = if (isMessageAuthor) {
            message.targetAccountAvatar
        } else {
            message.authorAvatar
        }
        val isRead = message.isRead || isMessageAuthor
        val messageId = message.id.toLong()

        if (existingConversation == null) {
            conversationsByPersonId[otherPersonId] = Conversation(
                id = 0,
                ts = message.lastUpdateTs,
                content = message.content,
                iconUrl = iconUrl,
                accountStableId = currentAccount.stableId,
                personId = otherPersonId,
                personInstance = otherPersonInstance,
                personName = otherPersonName,
                title = otherPersonName ?: context.getString(R.string.unknown),
                isRead = isRead,
                mostRecentMessageId = messageId,
            )
        } else if (existingConversation.mostRecentMessageId == messageId) {
            conversationsByPersonId[existingConversation.personId] = existingConversation.copy(
                ts = message.lastUpdateTs,
                content = message.content,
                iconUrl = iconUrl,
                isRead = isRead,
                mostRecentMessageId = messageId,
            )
        } else {
            if (existingConversation.ts > message.lastUpdateTs) {
                return
            }

            conversationsByPersonId[existingConversation.personId] = existingConversation.copy(
                ts = message.lastUpdateTs,
                content = message.content,
                iconUrl = iconUrl,
                isRead = isRead,
                mostRecentMessageId = messageId,
            )
        }
    }

    private fun invalidate() {
        coroutineScope.launch(conversationContext) {
            conversationsByPersonId = mutableMapOf()
            draftsByPersonId = mapOf()
            conversationsLoadedFromDb = false
            conversationEarliestMessageTs = null
            publishModel()
        }
    }

    private suspend fun loadConversationsFromDbIfNeeded() {
        if (conversationsLoadedFromDb) {
            return
        }

        withContext(conversationContext) {
            if (conversationsLoadedFromDb) {
                return@withContext
            }

            val currentAccount = currentAccount ?: return@withContext

            val entries = conversationEntriesDao.getAllEntriesForAccount(currentAccount.stableId)

            for (entry in entries) {
                conversationsByPersonId[entry.personId] = entry.toConversation()
            }

            loadDrafts()

            conversationsLoadedFromDb = true
        }
    }

    private suspend fun loadDrafts() {
        val currentAccount = currentAccount ?: return

        val allDrafts = draftsManager.getAllDraftsByType(
            DraftTypes.Message,
            currentAccount.id,
            currentAccount.instance,
        )
        val sortedDrafts = allDrafts
            .sortedBy { it.updatedTs }

        val draftsByPersonId = mutableMapOf<PersonId, DbMessageDraft>()

        for (draft in sortedDrafts) {
            val messageDraftData = draft.data as? DraftData.MessageDraftData
                ?: continue

            draftsByPersonId[messageDraftData.targetAccountId] =
                DbMessageDraft(draft.id, messageDraftData)
        }

        this.draftsByPersonId = draftsByPersonId
    }

    private suspend fun commitToDb() {
        if (!conversationsLoadedFromDb) {
            return
        }

        withContext(conversationContext) {
            if (!conversationsLoadedFromDb) {
                return@withContext
            }

            for (conversation in conversationsByPersonId.values) {
                conversationEntriesDao.insertEntry(conversation.toEntry())
            }
        }
    }

    private fun setupForAccount() {
        val currentAccount = currentAccount
            ?: return

        stateStorage = stateStorageManager.getAccountStateStorage(
            currentAccount.id, currentAccount.instance,
        )
    }
}
