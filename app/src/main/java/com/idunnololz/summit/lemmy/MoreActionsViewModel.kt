package com.idunnololz.summit.lemmy

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.info.AccountInfoManager
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
import com.idunnololz.summit.fileprovider.FileProviderHelper
import com.idunnololz.summit.hidePosts.HiddenPostsManager
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.FileDownloadHelper
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.video.VideoDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import javax.inject.Inject

@HiltViewModel
class MoreActionsViewModel @Inject constructor(
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
) : ViewModel() {

    val currentAccount: Account?
        get() = apiClient.accountForInstance()
    val apiInstance: String
        get() = apiClient.instance

    val blockCommunityResult = StatefulLiveData<Unit>()
    val blockPersonResult = StatefulLiveData<BlockPersonResult>()
    val blockInstanceResult = StatefulLiveData<BlockInstanceResult>()
    val saveCommentResult = StatefulLiveData<CommentView>()
    val savePostResult = StatefulLiveData<PostView>()
    val deletePostResult = StatefulLiveData<PostView>()

    val downloadVideoResult = StatefulLiveData<FileDownloadHelper.DownloadResult>()
    val downloadAndShareFile = StatefulLiveData<Uri>()
    val downloadResult = StatefulLiveData<Result<FileDownloadHelper.DownloadResult>>()

    private var currentPageInstance: String? = null

    fun blockCommunity(id: CommunityId, block: Boolean = true) {
        blockCommunityResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance { apiClient.blockCommunity(id, block) }
                .onFailure {
                    blockCommunityResult.postError(it)
                }
                .onSuccess {
                    blockCommunityResult.postValue(Unit)
                }
        }
    }

    fun blockPerson(id: PersonId, block: Boolean = true) {
        blockPersonResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance { apiClient.blockPerson(id, block) }
                .onFailure {
                    blockPersonResult.postError(it)
                }
                .onSuccess {
                    blockPersonResult.postValue(BlockPersonResult(block))
                }
        }
    }

    fun blockInstance(id: InstanceId, block: Boolean = true) {
        blockInstanceResult.setIsLoading()
        viewModelScope.launch {
            ensureRightInstance { apiClient.blockInstance(id, block) }
                .onFailure {
                    blockInstanceResult.postError(it)
                }
                .onSuccess {
                    blockInstanceResult.postValue(BlockInstanceResult(block))
                }
        }
    }

    fun deletePost(postId: PostId) {
        deletePostResult.setIsLoading()
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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

        viewModelScope.launch {
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

        viewModelScope.launch {
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
                viewModelScope.launch {
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
}

data class BlockPersonResult(
    val blockedPerson: Boolean,
)

data class BlockInstanceResult(
    val blocked: Boolean,
)
