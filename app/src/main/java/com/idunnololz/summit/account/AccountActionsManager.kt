package com.idunnololz.summit.account

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.idunnololz.summit.R
import com.idunnololz.summit.actions.PendingActionsManager
import com.idunnololz.summit.actions.PendingCommentView
import com.idunnololz.summit.actions.PendingCommentsManager
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.actions.VotesManager
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.ApiListenerManager
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.GetCommentsResponse
import com.idunnololz.summit.api.dto.GetPersonDetailsResponse
import com.idunnololz.summit.api.dto.GetPostResponse
import com.idunnololz.summit.api.dto.GetPostsResponse
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.dto.SearchResponse
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureException
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.lemmy.utils.VoteUiHandler
import com.idunnololz.summit.lemmy.utils.toVotableRef
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.util.color.ColorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Response

@Singleton
class AccountActionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: AccountManager,
    private val pendingActionsManager: PendingActionsManager,
    private val coroutineScopeFactory: CoroutineScopeFactory,
    private val postReadManager: PostReadManager,
    private val preferenceManager: PreferenceManager,
    private val apiListenerManager: ApiListenerManager,
    private val colorManager: ColorManager,
) {

    companion object {
        private const val TAG = "AccountActionsManager"
    }

    private var preferences = preferenceManager.currentPreferences
    private val votesManager = VotesManager(context, preferences)
    private val pendingCommentsManager = PendingCommentsManager()
    private var nextId: Long = 1

    interface Registration {
        fun voteCurrent(score: Int, totalScore: Int?, upvotes: Int?, downvotes: Int?)
        fun voteSuccess(newScore: Int, totalScore: Int?, upvotes: Int?, downvotes: Int?)
        fun votePending(pendingScore: Int, totalScore: Int?, upvotes: Int?, downvotes: Int?)
        fun voteFailed(score: Int, totalScore: Int?, upvotes: Int?, downvotes: Int?, e: Throwable)
    }

    class VoteHandlerRegistration(
        val ref: VotableRef,
        val registration: Registration,
    )

    private val regIdToRegistration = mutableMapOf<Long, VoteHandlerRegistration>()
    private val voteRefToRegistrations = mutableMapOf<VotableRef, MutableList<Registration>>()
    private val coroutineScope = coroutineScopeFactory.create()

    val onCommentActionChanged = MutableSharedFlow<Unit>()

    val voteUiHandler = object : VoteUiHandler {
        override fun bindVoteUi(
            lifecycleOwner: LifecycleOwner,
            currentVote: Int,
            currentScore: Int,
            upVotes: Int,
            downVotes: Int,
            instance: String,
            ref: VotableRef,
            upVoteView: View?,
            downVoteView: View?,
            scoreView: TextView,
            upvoteCount: TextView?,
            downvoteCount: TextView?,
            accountId: Long?,
            registration: Registration,
        ) {
            votesManager.setVoteIfNoneSet(ref, currentVote)
            votesManager.setScoreIfNoneSet(ref, currentScore)
            votesManager.setUpvotesIfNoneSet(ref, upVotes)
            votesManager.setDownvotesIfNoneSet(ref, downVotes)

            val existingRegId = scoreView.getTag(R.id.account_actions_manager_reg_id)
            if (existingRegId != null) {
                unregisterVoteHandler(existingRegId as Long)
            }

            fun getAccount() = if (accountId == null) {
                accountManager.currentAccount.value as? Account
            } else {
                accountManager.getAccountByIdBlocking(accountId)
            }

            upVoteView?.setOnClickListener {
                val curVote = votesManager.getVote(ref) ?: 0
                val curScore = votesManager.getScore(ref) ?: 0
                val curUpvotes = votesManager.getUpvotes(ref) ?: 0
                val curDownvotes = votesManager.getDownvotes(ref) ?: 0

                val newScore = if (curVote == 1) {
                    0
                } else {
                    1
                }

                voteOn(instance, ref, newScore, getAccount())
                    .onFailure {
                        registration.voteFailed(curVote, curScore, curUpvotes, curDownvotes, it)
                    }
            }
            downVoteView?.setOnClickListener {
                val curVote = votesManager.getVote(ref) ?: 0
                val curScore = votesManager.getScore(ref) ?: 0
                val curUpvotes = votesManager.getUpvotes(ref) ?: 0
                val curDownvotes = votesManager.getDownvotes(ref) ?: 0

                val newScore = if (curVote == -1) {
                    0
                } else {
                    -1
                }

                voteOn(instance, ref, newScore, getAccount())
                    .onFailure {
                        registration.voteFailed(curVote, curScore, curUpvotes, curDownvotes, it)
                    }
            }

            val regId = nextId
            scoreView.setTag(R.id.account_actions_manager_reg_id, regId)
            nextId++

            registerVoteHandler(regId, ref, registration)

            registration.voteCurrent(
                score = votesManager.getVote(ref) ?: 0,
                totalScore = votesManager.getScore(ref),
                upvotes = votesManager.getUpvotes(ref),
                downvotes = votesManager.getDownvotes(ref),
            )

            Log.d(TAG, "Binding vote handler - $ref")

            if (upvoteCount != null && downvoteCount != null) {
                upvoteCount.text = LemmyUtils.abbrevNumber(
                    votesManager.getUpvotes(ref)?.toLong(),
                )
                downvoteCount.text = LemmyUtils.abbrevNumber(
                    votesManager.getDownvotes(ref)?.toLong(),
                )
            } else {
                scoreView.text = LemmyUtils.abbrevNumber(
                    votesManager.getScore(ref)?.toLong(),
                )
            }

            lifecycleOwner.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        Log.d(TAG, "Lifecycle onDestroy. Unbinding - $ref")

                        lifecycleOwner.lifecycle.removeObserver(this)

                        unbindVoteUi(scoreView)
                        upVoteView?.setOnClickListener(null)
                        downVoteView?.setOnClickListener(null)
                    }
                },
            )
        }

        override fun unbindVoteUi(scoreView: View) {
            val existingRegId = scoreView.getTag(R.id.account_actions_manager_reg_id)
            if (existingRegId != null) {
                unregisterVoteHandler(existingRegId as Long)
            }
        }

        override val upvoteColor: Int
            get() = preferences.upvoteColor
        override val downvoteColor: Int
            get() = preferences.downvoteColor

        override fun neutralColor(context: Context): Int = colorManager.textColor

        override fun controlColor(context: Context): Int = colorManager.controlColor
    }

    private val onActionChangedListener = object : PendingActionsManager.OnActionChangedListener {
        override fun onActionAdded(action: LemmyAction) {
            when (action.info) {
                is ActionInfo.VoteActionInfo -> {
                    votesManager.setPendingVote(action.info.ref, action.info.dir)
                    voteRefToRegistrations[action.info.ref]?.forEach {
                        it.votePending(
                            pendingScore = action.info.dir,
                            totalScore = votesManager.getScore(action.info.ref),
                            upvotes = votesManager.getUpvotes(action.info.ref),
                            downvotes = votesManager.getDownvotes(action.info.ref),
                        )
                    }
                }
                is ActionInfo.CommentActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onCommentActionAdded(action.id, action.info)
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.DeleteCommentActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onDeleteCommentActionAdded(action.id, action.info)
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.EditActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onEditCommentActionAdded(action.id, action.info)
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.MarkPostAsReadActionInfo -> {
                    coroutineScope.launch {
                        postReadManager.markPostAsReadLocal(
                            action.info.postRef.instance,
                            action.info.postRef.id,
                            action.info.read,
                        )
                        onCommentActionChanged.emit(Unit)
                    }
                }
                null -> {}
            }
        }

        override fun onActionFailed(action: LemmyAction, reason: LemmyActionFailureReason) {
            Log.d(TAG, "onActionComplete(): $action, reason: $reason")
            when (action.info) {
                is ActionInfo.VoteActionInfo -> {
                    votesManager.clearPendingVotes(action.info.ref)
                    voteRefToRegistrations[action.info.ref]?.forEach {
                        it.voteFailed(
                            score = votesManager.getVote(action.info.ref) ?: 0,
                            totalScore = votesManager.getScore(action.info.ref) ?: 0,
                            upvotes = votesManager.getUpvotes(action.info.ref) ?: 0,
                            downvotes = votesManager.getDownvotes(action.info.ref) ?: 0,
                            e = LemmyActionFailureException(reason),
                        )
                    }
                }
                is ActionInfo.CommentActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onCommentActionFailed(action.id, action.info, reason)
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.DeleteCommentActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onDeleteCommentActionFailed(
                            id = action.id,
                            info = action.info,
                            reason = reason,
                        )
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.EditActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onEditCommentActionFailed(
                            action.id,
                            action.info,
                            reason,
                        )
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.MarkPostAsReadActionInfo -> {
                    coroutineScope.launch {
                        postReadManager.delete(
                            action.info.postRef.instance,
                            action.info.postRef.id,
                        )
                        onCommentActionChanged.emit(Unit)
                    }
                }
                null -> {}
            }
        }

        override fun onActionComplete(action: LemmyAction, result: LemmyActionResult<*, *>) {
            Log.d(TAG, "onActionComplete(): $action")
            when (action.info) {
                is ActionInfo.VoteActionInfo -> {
                    var score: Int? = 0
                    var upvotes: Int? = 0
                    var downvotes: Int? = 0

                    (result as LemmyActionResult.VoteLemmyActionResult).result
                        .fold(
                            {
                                val voteRef = VotableRef.PostRef(it.post.id)

                                votesManager.clearPendingVotes(action.info.ref)
                                votesManager.setScore(voteRef, it.counts.score)
                                votesManager.setUpvotes(voteRef, it.counts.upvotes)
                                votesManager.setDownvotes(voteRef, it.counts.downvotes)

                                if (votesManager.shouldShowScore(voteRef)) {
                                    score = it.counts.score
                                    upvotes = it.counts.upvotes
                                    downvotes = it.counts.downvotes
                                } else {
                                    score = null
                                    upvotes = null
                                    downvotes = null
                                }
                            },
                            {
                                val voteRef = VotableRef.CommentRef(it.comment.id)

                                votesManager.setScore(voteRef, it.counts.score)
                                votesManager.setUpvotes(voteRef, it.counts.upvotes)
                                votesManager.setDownvotes(voteRef, it.counts.downvotes)

                                if (votesManager.shouldShowScore(voteRef)) {
                                    score = it.counts.score
                                    upvotes = it.counts.upvotes
                                    downvotes = it.counts.downvotes
                                } else {
                                    score = null
                                    upvotes = null
                                    downvotes = null
                                }
                            },
                        )

                    votesManager.setVote(action.info.ref, action.info.dir)
                    voteRefToRegistrations[action.info.ref]?.forEach {
                        it.voteSuccess(action.info.dir, score, upvotes, downvotes)
                    }
                }
                is ActionInfo.CommentActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onCommentActionComplete(action.id, action.info)
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.DeleteCommentActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onDeleteCommentActionComplete(action.id, action.info)
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.EditActionInfo -> {
                    coroutineScope.launch {
                        pendingCommentsManager.onEditCommentActionComplete(action.id, action.info)
                        onCommentActionChanged.emit(Unit)
                    }
                }
                is ActionInfo.MarkPostAsReadActionInfo -> {
                    coroutineScope.launch {
                        postReadManager.markPostAsReadLocal(
                            action.info.postRef.instance,
                            action.info.postRef.id,
                            action.info.read,
                        )
                        onCommentActionChanged.emit(Unit)
                    }
                }
                null -> {}
            }
        }
    }

    init {
        accountManager.addOnAccountChangedListener(
            object : AccountManager.OnAccountChangedListener {
                override suspend fun onAccountChanged(newAccount: Account?) {
                    votesManager.onAccountChanged(preferenceManager.currentPreferences)
                }
            },
        )

        apiListenerManager.registerListener {
            handleApiResponse(it)
        }

        pendingActionsManager.addActionCompleteListener(onActionChangedListener)
    }

    suspend fun createComment(
        postRef: PostRef,
        parentId: CommentId?,
        content: String,
        accountId: Long? = null,
    ) {
        val finalAccountId = accountId
            ?: accountOrDefault(null)?.id
            ?: return
        pendingActionsManager.comment(
            postRef,
            parentId,
            content,
            finalAccountId,
        )
    }

    suspend fun editComment(
        postRef: PostRef,
        commentId: CommentId,
        content: String,
        accountId: Long? = null,
    ) {
        val finalAccountId = accountId
            ?: accountOrDefault(null)?.id
            ?: return
        pendingActionsManager.editComment(
            postRef,
            commentId,
            content,
            finalAccountId,
        )
    }

    suspend fun deleteComment(postRef: PostRef, commentId: CommentId, accountId: Long? = null) {
        val finalAccountId = accountId
            ?: accountOrDefault(null)?.id
            ?: return
        pendingActionsManager.deleteComment(
            postRef,
            commentId,
            finalAccountId,
        )
    }

    fun vote(instance: String, ref: VotableRef, dir: Int, accountId: Long? = null): Result<Unit> {
        val account = runBlocking { accountOrDefault(accountId) }
            ?: return Result.failure(NotAuthenticatedException())
        return voteOn(instance, ref, dir, account)
    }

    suspend fun markPostAsRead(
        instance: String,
        id: PostId,
        read: Boolean,
        accountId: Long? = null,
    ) {
        val account = accountOrDefault(accountId) ?: return
        pendingActionsManager.markPostAsRead(
            PostRef(instance, id),
            read,
            account.id,
        )
    }

    private fun registerVoteHandler(
        existingRegId: Long,
        ref: VotableRef,
        registration: Registration,
    ) {
        regIdToRegistration[existingRegId] = VoteHandlerRegistration(ref, registration)
        val list = voteRefToRegistrations.getOrPut(ref) { mutableListOf() }
        list.add(registration)

        Log.d(TAG, "Total registrations: ${regIdToRegistration.size}")
    }

    private fun unregisterVoteHandler(existingRegId: Long) {
        val regData = regIdToRegistration.remove(existingRegId) ?: return
        val list = voteRefToRegistrations.getOrPut(regData.ref) { mutableListOf() }
        list.remove(regData.registration)
    }

    private fun voteOn(
        instance: String,
        ref: VotableRef,
        dir: Int,
        account: Account?,
    ): Result<Unit> {
        if (account == null) {
            return Result.failure(NotAuthenticatedException())
        }
        if (account.instance != instance) {
            return Result.failure(AccountInstanceMismatchException(account.instance, instance))
        }

        pendingActionsManager.voteOn(instance, ref, dir, account.id)

        return Result.success(Unit)
    }

    fun getPendingComments(postRef: PostRef) = pendingCommentsManager.getPendingComments(postRef)

    fun removePendingComment(pendingComment: PendingCommentView) {
        pendingCommentsManager.removePendingComment(pendingComment)
    }

    fun setScore(ref: VotableRef, score: Int) {
        votesManager.setScore(ref, score)
    }

    fun getScore(ref: VotableRef) = votesManager.getScore(ref)

    fun getVote(ref: VotableRef) = votesManager.getVote(ref)

    private fun handleApiResponse(response: Response<*>) {
        if (!response.isSuccessful) return
        if (response.raw().networkResponse == null) return

        fun updateScore(item: Any?) {
            when (item) {
                is CommentView -> {
                    val votableRef = item.toVotableRef()
                    setScore(votableRef, item.counts.score)
                    votesManager.setUpvotes(votableRef, item.counts.upvotes)
                    votesManager.setDownvotes(votableRef, item.counts.downvotes)
                    votesManager.setVote(votableRef, item.my_vote ?: 0)
                }
                is PostView -> {
                    val votableRef = item.toVotableRef()
                    setScore(votableRef, item.counts.score)
                    votesManager.setUpvotes(votableRef, item.counts.upvotes)
                    votesManager.setDownvotes(votableRef, item.counts.downvotes)
                    votesManager.setVote(votableRef, item.my_vote ?: 0)
                }
            }
        }

        fun updateScores(result: List<*>) {
            for (item in result) {
                updateScore(item)
            }
        }

        when (val result = response.body()) {
            is List<*> -> {
                if (result.isNotEmpty()) {
                    when (result.first()) {
                        is CommentView -> {
                            updateScores(result)
                        }
                        is PostView -> {
                            updateScores(result)
                        }
                    }
                }
            }
            is GetPostResponse -> {
                updateScore(result.post_view)
            }
            is GetPostsResponse -> {
                for (post in result.posts) {
                    updateScore(post)
                }
            }
            is GetCommentsResponse -> {
                for (comment in result.comments) {
                    updateScore(comment)
                }
            }
            is GetPersonDetailsResponse -> {
                for (comment in result.comments) {
                    updateScore(comment)
                }
                for (post in result.posts) {
                    updateScore(post)
                }
            }
            is SearchResponse -> {
                for (comment in result.comments) {
                    updateScore(comment)
                }
                for (post in result.posts) {
                    updateScore(post)
                }
            }
            else -> {
//                Log.d("ASDF", "${result?.javaClass?.simpleName}")
                // do nothing
            }
        }
    }

    private suspend fun accountOrDefault(accountId: Long?) = if (accountId == null) {
        accountManager.currentAccount.asAccount
    } else {
        accountManager.getAccountById(accountId)
    }
}
