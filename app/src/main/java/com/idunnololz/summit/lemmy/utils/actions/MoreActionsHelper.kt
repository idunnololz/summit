package com.idunnololz.summit.lemmy.utils.actions

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
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
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoViewModel
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.Event
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.video.VideoDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoreActionsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    var apiClient: AccountAwareLemmyClient,
    val accountManager: AccountManager,
    val accountInfoManager: AccountInfoManager,
    val accountActionsManager: AccountActionsManager,
    private val hiddenPostsManager: HiddenPostsManager,
    private val savedManager: SavedManager,
    private val videoDownloadManager: VideoDownloadManager,
    private val fileDownloadHelper: FileDownloadHelper,
    private val offlineManager: OfflineManager,
    coroutineScopeFactory: CoroutineScopeFactory,
) {

    private val coroutineScope = coroutineScopeFactory.create()

    val currentAccount: Account?
        get() = apiClient.accountForInstance()
    val fullAccount: FullAccount?
        get() = accountInfoManager.currentFullAccount.value
    val apiInstance: String
        get() = apiClient.instance

    val blockCommunityResult = StatefulLiveData<BlockCommunityResult>()
    val blockPersonResult = StatefulLiveData<BlockPersonResult>()
    val blockInstanceResult = StatefulLiveData<BlockInstanceResult>()
    val saveCommentResult = StatefulLiveData<CommentView>()
    val savePostResult = StatefulLiveData<PostView>()
    val deletePostResult = StatefulLiveData<PostView>()
    val subscribeResult = StatefulLiveData<SubscribeResult>()

    val downloadVideoResult = StatefulLiveData<FileDownloadHelper.DownloadResult>()
    val downloadAndShareFile = StatefulLiveData<Uri>()
    val downloadResult = StatefulLiveData<Result<FileDownloadHelper.DownloadResult>>()

    private var currentPageInstance: String? = null

    fun blockCommunity(communityRef: CommunityRef.CommunityRefByName, block: Boolean = true) {
        blockCommunityResult.setIsLoading()
        coroutineScope.launch {
            ensureRightInstance {
                apiClient.fetchCommunityWithRetry(
                    idOrName = Either.Right(communityRef.getServerId(apiInstance)),
                    force = false,
                )
            }
                .onSuccess {
                    blockCommunityInternal(it.community_view.community.id, block)
                }
                .onFailure {
                    blockCommunityResult.postError(it)
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
            ensureRightInstance {
                apiClient.fetchPersonByNameWithRetry(personRef.fullName, force = false)
            }
                .onFailure {
                    blockPersonResult.postError(it)
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
            ensureRightInstance { apiClient.blockInstance(id, block) }
                .onSuccess {
                    blockInstanceResult.postValue(
                        BlockInstanceResult(
                            blocked = block,
                            instanceId = id,
                        )
                    )
                }
                .onFailure {
                    blockInstanceResult.postError(it)
                }
        }
    }

    fun deletePost(postId: PostId) {
        deletePostResult.setIsLoading()
        coroutineScope.launch {
            ensureRightInstance { apiClient.deletePost(postId) }
                .onSuccess {
                    deletePostResult.postValue(it)
                }
                .onFailure {
                    deletePostResult.postError(it)
                }
        }
    }

    fun deleteComment(postRef: PostRef, commentId: Int) {
        coroutineScope.launch {
            accountActionsManager.deleteComment(postRef, commentId)
        }
    }

    fun vote(postView: PostView, dir: Int, toggle: Boolean = false) {
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
        accountActionsManager.vote(apiClient.instance, ref, finalDir)
    }

    fun vote(commentView: CommentView, dir: Int, toggle: Boolean = false): Result<Unit> {
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
        return accountActionsManager.vote(apiClient.instance, ref, finalDir)
    }

    fun savePost(id: PostId, save: Boolean) {
        savePostResult.setIsLoading()
        coroutineScope.launch {
            ensureRightInstance { apiClient.savePost(id, save) }
                .onSuccess {
                    savePostResult.postValue(it)
                    savedManager.onPostSaveChanged()
                }
                .onFailure {
                    savePostResult.postError(it)
                }
        }
    }

    fun saveComment(id: CommentId, save: Boolean) {
        coroutineScope.launch {
            ensureRightInstance { apiClient.saveComment(id, save) }
                .onSuccess {
                    saveCommentResult.postValue(it)
                    savedManager.onCommentSaveChanged()
                }
                .onFailure {
                    saveCommentResult.postError(it)
                }
        }
    }

    fun hidePost(id: PostId) {
        hiddenPostsManager.hidePost(id, apiClient.instance)
    }

    fun onPostRead(postView: PostView, delayMs: Long) {
        if (postView.read) {
            return
        }

        coroutineScope.launch {
            if (delayMs > 0) {
                delay(delayMs)
            }
            accountActionsManager.markPostAsRead(apiClient.instance, postView.post.id, read = true)
        }
    }

    fun setPageInstance(instance: String) {
        currentPageInstance = instance
    }

    fun downloadVideo(
        context: Context,
        url: String,
    ) {
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
                            downloadVideoResult.postValue(it)
                        }
                        .onFailure {
                            downloadVideoResult.postError(it)
                        }
                }
                .onFailure {
                    downloadVideoResult.postError(it)
                }
        }
    }

    fun downloadAndShareImage(url: String) {
        downloadAndShareFile.setIsLoading()

        offlineManager.fetchImage(
            url = url,
            listener = { file ->
                val fileUri = FileProviderHelper(context)
                    .openTempFile("img_${file.name}") { os ->
                        os.sink().buffer().use {
                            it.writeAll(file.source())
                        }
                    }
                downloadAndShareFile.postValue(fileUri)
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

                    downloadResult.postValue(result)
                }
            },
            errorListener = {
                downloadResult.postError(it)
            },
        )
    }

    fun updateSubscription(communityRef: CommunityRef.CommunityRefByName, subscribe: Boolean) {
        subscribeResult.setIsLoading()

        coroutineScope.launch {
            ensureRightInstance {
                apiClient.fetchCommunityWithRetry(
                    idOrName = Either.Right(communityRef.getServerId(apiInstance)),
                    force = false,
                )
            }
                .onSuccess {
                    updateSubscriptionInternal(it.community_view.community.id, subscribe)
                }
                .onFailure {
                    subscribeResult.postError(it)
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
        ensureRightInstance { apiClient.followCommunityWithRetry(communityId, subscribe) }
            .onSuccess {
                subscribeResult.postValue(
                    SubscribeResult(
                        subscribe = subscribe,
                        communityId = communityId,
                    )
                )

                accountInfoManager.refreshAccountInfo()
            }
            .onFailure {
                subscribeResult.postError(it)
            }
    }

    private suspend fun blockPersonInternal(id: PersonId, block: Boolean = true) {
        ensureRightInstance { apiClient.blockPerson(id, block) }
            .onFailure {
                blockPersonResult.postError(it)
            }
            .onSuccess {
                accountInfoManager.refreshAccountInfo()
                blockPersonResult.postValue(
                    BlockPersonResult(
                        blocked = block,
                        personFullName = it.person.toPersonRef().fullName,
                        personId = id,
                    )
                )
            }
    }

    private suspend fun blockCommunityInternal(id: CommunityId, block: Boolean = true) {
        ensureRightInstance { apiClient.blockCommunity(id, block) }
            .onSuccess {
                blockCommunityResult.postValue(
                    BlockCommunityResult(
                        blocked = block,
                        communityId = id,
                    )
                )
            }
            .onFailure {
                blockCommunityResult.postError(it)
            }
    }

    private suspend fun <T> ensureRightInstance(
        onCorrectInstance: suspend () -> Result<T>,
    ): Result<T> {
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