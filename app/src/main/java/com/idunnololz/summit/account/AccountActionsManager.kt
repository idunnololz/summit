package com.idunnololz.summit.account

import android.content.Context
import android.util.Log
import android.view.View
import androidx.core.view.doOnDetach
import com.idunnololz.summit.R
import com.idunnololz.summit.api.AccountInstanceMismatchException
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PendingActionsManager
import com.idunnololz.summit.lemmy.VotesManager
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.lemmy.utils.VoteUiHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountActionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountManager: AccountManager,
    private val pendingActionsManager: PendingActionsManager,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    companion object {
        private const val TAG = "AccountActionsManager"
    }

    private val votesManager = VotesManager(context)
    private var nextId: Long = 1

    interface Registration {
        fun voteCurrent(score: Int)
        fun voteSuccess(newScore: Int)
        fun votePending(pendingScore: Int)
        fun voteFailed(score: Int, e: Throwable)
    }

    class VoteHandlerRegistration(
        val ref: VotableRef,
        val registration: Registration,
    )

    private val regIdToRegistration = mutableMapOf<Long, VoteHandlerRegistration>()
    private val voteRefToRegistrations = mutableMapOf<VotableRef, MutableList<Registration>>()
    private val coroutineScope = coroutineScopeFactory.create()

    val voteUiHandler = object : VoteUiHandler {
        override fun bindVoteUi(
            currentScore: Int,
            instance: String,
            ref: VotableRef,
            upVoteView: View,
            downVoteView: View,
            registration: Registration
        ) {
            votesManager.setVoteIfNoneSet(ref, currentScore)

            val existingRegId = upVoteView.getTag(R.id.account_actions_manager_reg_id)
            if (existingRegId != null) {
                unregisterVoteHandler(existingRegId as Long)
            }

            val account = accountManager.currentAccount.value

            upVoteView.setOnClickListener {
                val curVote = votesManager.getVote(ref) ?: 0
                val newScore = if (curVote == 1) {
                    0
                } else {
                    1
                }

                voteOn(instance, ref, newScore, account)
                    .onFailure {
                        registration.voteFailed(curVote, it)
                    }
            }
            downVoteView.setOnClickListener {
                val curVote = votesManager.getVote(ref) ?: 0
                val newScore = if (curVote == -1) {
                    0
                } else {
                    -1
                }

                voteOn(instance, ref, newScore, account)
                    .onFailure {
                        registration.voteFailed(curVote, it)
                    }
            }

            val regId = nextId
            upVoteView.setTag(R.id.account_actions_manager_reg_id, regId)
            nextId++

            registerVoteHandler(regId, ref, registration)

            registration.voteCurrent(votesManager.getVote(ref) ?: 0)

            Log.d(TAG, "Binding vote handler - ${ref}")
            upVoteView.post {
                upVoteView.doOnDetach {
                    unbindVoteUi(it)
                    Log.d(TAG, "Auto unbinding vote handler - ${ref}")
                }
            }
        }

        override fun unbindVoteUi(upVoteView: View) {
            val existingRegId = upVoteView.getTag(R.id.account_actions_manager_reg_id)
            if (existingRegId != null) {
                unregisterVoteHandler(existingRegId as Long)
            }
        }
    }

    init {
        accountManager.addOnAccountChangedListener(object : AccountManager.OnAccountChangedListener {
            override suspend fun onAccountChanged(newAccount: Account?) {
                votesManager.reset()
            }
        })

        pendingActionsManager.addActionCompleteListener(object :
            PendingActionsManager.OnActionChangedListener {
            override fun onActionAdded(action: LemmyAction) {
                when (action.info) {
                    is ActionInfo.VoteActionInfo -> {
                        votesManager.setPendingVote(action.info.ref, action.info.dir)
                        voteRefToRegistrations[action.info.ref]?.forEach {
                            it.votePending(action.info.dir)
                        }
                    }
                    is ActionInfo.CommentActionInfo -> {}
                    is ActionInfo.DeleteCommentActionInfo -> {}
                    is ActionInfo.EditActionInfo -> {}
                    null -> {}
                }
            }

            override fun onActionFailed(action: LemmyAction, reason: Throwable) {
                when (action.info) {
                    is ActionInfo.VoteActionInfo -> {
                        votesManager.clearPendingVotes(action.info.ref)
                        voteRefToRegistrations[action.info.ref]?.forEach {
                            it.voteFailed(votesManager.getVote(action.info.ref) ?: 0, reason)
                        }
                    }
                    is ActionInfo.CommentActionInfo -> {}
                    is ActionInfo.DeleteCommentActionInfo -> {}
                    is ActionInfo.EditActionInfo -> {}
                    null -> {}
                }
            }

            override fun onActionComplete(action: LemmyAction) {
                when (action.info) {
                    is ActionInfo.VoteActionInfo -> {
                        votesManager.setVote(action.info.ref, action.info.dir)
                        voteRefToRegistrations[action.info.ref]?.forEach {
                            it.voteSuccess(action.info.dir)
                        }
                    }
                    is ActionInfo.CommentActionInfo -> {}
                    is ActionInfo.DeleteCommentActionInfo -> {}
                    is ActionInfo.EditActionInfo -> {}
                    null -> {}
                }
            }
        })
    }

    private fun registerVoteHandler(existingRegId: Long, ref: VotableRef, registration: Registration) {
        regIdToRegistration[existingRegId] = VoteHandlerRegistration(ref, registration)
        val list = voteRefToRegistrations.getOrPut(ref) { mutableListOf() }
        list.add(registration)
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
}