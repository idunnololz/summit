package com.idunnololz.summit.lemmy.utils.actions

import android.content.Context
import android.net.Uri
import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.FullAccount
import com.idunnololz.summit.actions.SavedManager
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.dto.InstanceId
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.nsfwMode.NsfwModeManager
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.video.VideoDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source

@Singleton
class MoreActionsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lemmyApiClientFactory: AccountAwareLemmyClient.Factory,
    val accountManager: AccountManager,
    val accountInfoManager: AccountInfoManager,
    val accountActionsManager: AccountActionsManager,
    private val hiddenPostsManager: HiddenPostsManager,
    private val savedManager: SavedManager,
    private val videoDownloadManager: VideoDownloadManager,
    private val fileDownloadHelper: FileDownloadHelper,
    private val offlineManager: OfflineManager,
    private val nsfwModeManager: NsfwModeManager,
    coroutineScopeFactory: CoroutineScopeFactory,
) {

    private val coroutineScope = coroutineScopeFactory.create()
    val apiClient = lemmyApiClientFactory.create()

    val currentAccount: Account?
        get() = apiClient.accountForInstance()
    val fullAccount: FullAccount?
        get() = accountInfoManager.currentFullAccount.value
    val apiInstance: String
        get() = apiClient.instance

    val blockCommunityResult = StatefulLiveData<BlockCommunityResult>()
    val blockPersonResult = StatefulLiveData<BlockPersonResult>()
    val blockInstanceResult = StatefulLiveData<BlockInstanceResult>()
    val saveCommentResult = StatefulLiveData<SaveCommentResult>()
    val savePostResult = StatefulLiveData<SavePostResult>()
    val deletePostResult = StatefulLiveData<DeletePostResult>()
    val subscribeResult = StatefulLiveData<SubscribeResult>()

    val downloadVideoResult = StatefulLiveData<FileDownloadHelper.DownloadResult>()
    val downloadAndShareFile = StatefulLiveData<Uri>()
    val downloadResult = StatefulLiveData<Result<FileDownloadHelper.DownloadResult>>()

    val nsfwModeEnabledFlow
        get() = nsfwModeManager.nsfwModeEnabled

    private var currentPageInstance: String? = null

    fun blockCommunity(communityRef: CommunityRef.CommunityRefByName, block: Boolean = true) {
        blockCommunityResult.setIsLoading()
        coroutineScope.launch {
            ensureRightInstance(apiClient) {
                apiClient.fetchCommunityWithRetry(
                    idOrName = Either.Right(communityRef.getServerId(apiInstance)),
                    force = false,
                )
            }
                .onSuccess {
                    blockCommunityInternal(it.community_view.community.id, block)
                }
                .onFailure {
                    blockCommunityResult.postErrorAndClear(it)
                }
        }
    }

    fun blockCommunity(id: CommunityId, block: Boolean = true) {
        blockCommunityResult.setIsLoading()
        coroutineScope.launch {
            blockCommunityInternal(id, block)
        }
    }

    fun blockPerson(personRef: PersonRef, block: Boolean = true) {
        blockPersonResult.setIsLoading()
        coroutineScope.launch {
            ensureRightInstance(apiClient) {
                apiClient.fetchPersonByNameWithRetry(personRef.fullName, force = false)
            }
                .onFailure {
                    blockPersonResult.postErrorAndClear(it)
                }
                .onSuccess {
                    blockPersonInternal(it.person_view.person.id, block)
                }
        }
    }

    fun blockPerson(id: PersonId, block: Boolean = true) {
        blockPersonResult.setIsLoading()
        coroutineScope.launch {
            blockPersonInternal(id, block)
        }
    }

    fun blockInstance(id: InstanceId, block: Boolean = true) {
        blockInstanceResult.setIsLoading()
        coroutineScope.launch {
            ensureRightInstance(apiClient) { apiClient.blockInstance(id, block) }
                .onSuccess {
                    blockInstanceResult.postValueAndClear(
                        BlockInstanceResult(
                            blocked = block,
                            instanceId = id,
                        ),
                    )
                }
                .onFailure {
                    blockInstanceResult.postErrorAndClear(it)
                }
        }
    }

    fun deletePost(postId: PostId, delete: Boolean, accountId: Long? = null) {
        deletePostResult.setIsLoading()
        coroutineScope.launch {
            ensureRightInstance(apiClient) { apiClient.deletePost(postId, delete) }
                .onSuccess {
                    deletePostResult.postValueAndClear(DeletePostResult(postId, delete, accountId))
                }
                .onFailure {
                    deletePostResult.postErrorAndClear(it)
                }
        }
    }

    fun deleteComment(postRef: PostRef, commentId: Int, accountId: Long? = null) {
        coroutineScope.launch {
            accountActionsManager.deleteComment(postRef, commentId, accountId)
        }
    }

    fun vote(postView: PostView, dir: Int, toggle: Boolean = false, accountId: Long? = null) {
        val ref = VotableRef.PostRef(postView.post.id)
        val finalDir = if (toggle) {
            val curScore = accountActionsManager.getVote(ref)
                ?: postView.my_vote
            if (curScore == dir) {
                0
            } else {
                dir
            }
        } else {
            dir
        }
        accountActionsManager.vote(
            instance = apiClient.instance,
            ref = ref,
            dir = finalDir,
            accountId = accountId,
        )
    }

    fun vote(
        commentView: CommentView,
        dir: Int,
        toggle: Boolean = false,
        accountId: Long? = null,
    ): Result<Unit> {
        val ref = VotableRef.CommentRef(commentView.comment.id)
        val finalDir = if (toggle) {
            val curScore = accountActionsManager.getVote(ref)
                ?: commentView.my_vote
            if (curScore == dir) {
                0
            } else {
                dir
            }
        } else {
            dir
        }
        return accountActionsManager.vote(
            instance = apiClient.instance,
            ref = ref,
            dir = finalDir,
            accountId = accountId,
        )
    }

    fun savePost(id: PostId, save: Boolean, accountId: Long? = null) {
        savePostResult.setIsLoading()
        coroutineScope.launch {
            val apiClient = lemmyApiClientFactory.create()
            val account = accountManager.getAccountByIdOrDefault(accountId)
            apiClient.setAccount(account, accountChanged = true)

            ensureRightInstance(apiClient) { apiClient.savePost(id, save) }
                .onSuccess {
                    savePostResult.postValueAndClear(
                        SavePostResult(
                            postId = id,
                            save = save,
                            accountId = accountId,
                        ),
                    )
                    savedManager.onPostSaveChanged()
                }
                .onFailure {
                    savePostResult.postErrorAndClear(it)
                }
        }
    }

    fun saveComment(id: CommentId, save: Boolean) {
        coroutineScope.launch {
            ensureRightInstance(apiClient) { apiClient.saveComment(id, save) }
                .onSuccess {
                    saveCommentResult.postValueAndClear(
                        SaveCommentResult(
                            commentId = id,
                            save = save,
                        ),
                    )
                    savedManager.onCommentSaveChanged()
                }
                .onFailure {
                    saveCommentResult.postErrorAndClear(it)
                }
        }
    }

    fun hidePost(id: PostId) {
        hiddenPostsManager.hidePost(id, apiClient.instance)
    }

    fun onPostRead(postView: PostView, delayMs: Long, read: Boolean, accountId: Long? = null) {
        if (postView.read == read) {
            return
        }

        coroutineScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            accountActionsManager.markPostAsRead(
                instance = apiClient.instance,
                id = postView.post.id,
                read = read,
                accountId = accountId,
            )
        }
    }

    fun togglePostRead(postView: PostView, delayMs: Long, accountId: Long? = null) {
        coroutineScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            accountActionsManager.markPostAsRead(
                instance = apiClient.instance,
                id = postView.post.id,
                read = !postView.read,
                accountId = accountId,
            )
        }
    }

    fun setPageInstance(instance: String) {
        currentPageInstance = instance
    }

    fun downloadVideo(context: Context, url: String) {
        downloadVideoResult.setIsLoading()

        coroutineScope.launch {
            videoDownloadManager.downloadVideo(url)
                .onSuccess { file ->
                    fileDownloadHelper
                        .downloadFile(
                            c = context,
                            destFileName = file.name,
                            url = url,
                            cacheFile = file,
                        )
                        .onSuccess {
                            downloadVideoResult.postValueAndClear(it)
                        }
                        .onFailure {
                            downloadVideoResult.postErrorAndClear(it)
                        }
                }
                .onFailure {
                    downloadVideoResult.postErrorAndClear(it)
                }
        }
    }

    fun downloadAndShareImage(url: String) {
        downloadAndShareFile.setIsLoading()

        offlineManager.fetchImage(
            url = url,
            listener = { file ->
                coroutineScope.launch {
                    val fileUri = FileProviderHelper(context)
                        .openTempFile("img_${file.name}") { os ->
                            os.sink().buffer().use {
                                it.writeAll(file.source())
                            }
                        }

                    downloadAndShareFile.postValueAndClear(fileUri)
                }
            },
        )
    }

    fun downloadFile(
        context: Context,
        destFileName: String,
        url: String,
        mimeType: String? = null,
    ) {
        offlineManager.fetchImage(
            url = url,
            listener = {
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        fileDownloadHelper
                            .downloadFile(
                                c = context,
                                destFileName = destFileName,
                                url = url,
                                cacheFile = it,
                                mimeType = mimeType,
                            )
                    }

                    downloadResult.postValueAndClear(result)
                }
            },
            errorListener = {
                coroutineScope.launch {
                    downloadResult.postErrorAndClear(it)
                }
            },
        )
    }

    fun updateSubscription(communityRef: CommunityRef.CommunityRefByName, subscribe: Boolean) {
        subscribeResult.setIsLoading()

        coroutineScope.launch {
            ensureRightInstance(apiClient) {
                apiClient.fetchCommunityWithRetry(
                    idOrName = Either.Right(communityRef.getServerId(apiInstance)),
                    force = false,
                )
            }
                .onSuccess {
                    updateSubscriptionInternal(it.community_view.community.id, subscribe)
                }
                .onFailure {
                    subscribeResult.postErrorAndClear(it)
                }
        }
    }

    fun updateSubscription(communityId: Int, subscribe: Boolean) {
        subscribeResult.setIsLoading()

        coroutineScope.launch {
            updateSubscriptionInternal(communityId, subscribe)
        }
    }

    private suspend fun updateSubscriptionInternal(communityId: Int, subscribe: Boolean) {
        ensureRightInstance(
            apiClient,
        ) { apiClient.followCommunityWithRetry(communityId, subscribe) }
            .onSuccess {
                subscribeResult.postValueAndClear(
                    SubscribeResult(
                        subscribe = subscribe,
                        communityId = communityId,
                    ),
                )

                accountInfoManager.refreshAccountInfo()
            }
            .onFailure {
                subscribeResult.postErrorAndClear(it)
            }
    }

    private suspend fun blockPersonInternal(id: PersonId, block: Boolean = true) {
        ensureRightInstance(apiClient) { apiClient.blockPerson(id, block) }
            .onFailure {
                blockPersonResult.postErrorAndClear(it)
            }
            .onSuccess {
                accountInfoManager.refreshAccountInfo()
                blockPersonResult.postValueAndClear(
                    BlockPersonResult(
                        blocked = block,
                        personFullName = it.person.toPersonRef().fullName,
                        personId = id,
                    ),
                )
            }
    }

    private suspend fun blockCommunityInternal(id: CommunityId, block: Boolean = true) {
        ensureRightInstance(apiClient) { apiClient.blockCommunity(id, block) }
            .onSuccess {
                blockCommunityResult.postValueAndClear(
                    BlockCommunityResult(
                        blocked = block,
                        communityId = id,
                    ),
                )
            }
            .onFailure {
                blockCommunityResult.postErrorAndClear(it)
            }
    }

    private suspend fun <T> ensureRightInstance(
        apiClient: AccountAwareLemmyClient,
        onCorrectInstance: suspend () -> Result<T>,
    ): Result<T> {
        val apiInstance = apiClient.instance
        val currentPageInstance = currentPageInstance
        return if (currentPageInstance != null && currentPageInstance != apiInstance) {
            Result.failure(
                AccountInstanceMismatchException(
                    apiInstance,
                    currentPageInstance,
                ),
            )
        } else {
            onCorrectInstance()
        }
    }

    private suspend fun <T> StatefulLiveData<T>.postValueAndClear(value: T) {
        withContext(Dispatchers.Main) {
            setValue(value)
            setIdle()
        }
    }

    private suspend fun <T> StatefulLiveData<T>.postErrorAndClear(error: Throwable) {
        withContext(Dispatchers.Main) {
            setError(error)
            setIdle()
        }
    }
}

data class BlockPersonResult(
    val blocked: Boolean,
    val personFullName: String,
    val personId: PersonId,
)

data class BlockInstanceResult(
    val blocked: Boolean,
    val instanceId: InstanceId,
)

data class BlockCommunityResult(
    val blocked: Boolean,
    val communityId: CommunityId,
)

data class SubscribeResult(
    val subscribe: Boolean,
    val communityId: CommunityId,
)

data class SaveCommentResult(
    val commentId: CommentId,
    val save: Boolean,
)

data class SavePostResult(
    val postId: PostId,
    val save: Boolean,
    val accountId: Long?,
)

data class DeletePostResult(
    val postId: PostId,
    val delete: Boolean,
    val accountId: Long?,
)
