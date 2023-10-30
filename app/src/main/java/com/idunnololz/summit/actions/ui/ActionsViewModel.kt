package com.idunnololz.summit.actions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingActionsManager
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.LemmyFailedAction
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActionsViewModel @Inject constructor(
    private val pendingActionsManager: PendingActionsManager,
    private val accountManager: AccountManager,
) : ViewModel() {

    val actionsDataLiveData = StatefulLiveData<ActionsData>()

    init {
        loadActions()
    }

    fun loadActions() {
        viewModelScope.launch {
            val pendingActions = pendingActionsManager.getAllPendingActions().sortedByDescending { it.ts }
            val completedActions = pendingActionsManager.getAllCompletedActions().sortedByDescending { it.ts }
            val failedAccountInfo = pendingActionsManager.getAllFailedActions().sortedByDescending { it.ts }

            val accountIds = mutableSetOf<Long>()

            pendingActions.mapNotNullTo(accountIds) { it.info?.accountId }
            completedActions.mapNotNullTo(accountIds) { it.info?.accountId }
            failedAccountInfo.mapNotNullTo(accountIds) { it.info?.accountId }

            val accountDictionary = accountIds.associateWith { accountManager.getAccountById(it) }

            val actionsData = ActionsData(
                pendingActions = pendingActions.pendingToActions(),
                completedActions = completedActions.completedToActions(),
                failedActions = failedAccountInfo.failedToActions(),
                accountDictionary = accountDictionary,
            )

            actionsDataLiveData.postValue(actionsData)
        }
    }

    private fun List<LemmyAction>.pendingToActions(): List<Action> =
        this.map {
            Action(
                id = it.id,
                info = it.info,
                ts = it.ts,
                details = ActionDetails.PendingDetails,
            )
        }

    private fun List<LemmyCompletedAction>.completedToActions(): List<Action> =
        this.map {
            Action(
                id = it.id,
                info = it.info,
                ts = it.ts,
                details = ActionDetails.SuccessDetails,
            )
        }

    private fun List<LemmyFailedAction>.failedToActions(): List<Action> =
        this.map {
            Action(
                id = it.id,
                info = it.info,
                ts = it.ts,
                details = ActionDetails.FailureDetails(
                    it.error,
                ),
            )
        }

    fun deleteCompletedActions() {
        viewModelScope.launch {
            pendingActionsManager.deleteCompletedActions()
            loadActions()
        }
    }

    fun deleteFailedActions() {
        viewModelScope.launch {
            pendingActionsManager.deleteFailedActions()
            loadActions()
        }
    }

    fun deletePendingActions() {
        viewModelScope.launch {
            pendingActionsManager.deleteAllPendingActions()
            loadActions()
        }
    }

    data class ActionsData(
        val pendingActions: List<Action>,
        val completedActions: List<Action>,
        val failedActions: List<Action>,
        val accountDictionary: Map<Long, Account?>,
    )

    data class Action(
        val id: Long,
        val info: ActionInfo?,
        val ts: Long,
        val details: ActionDetails,
    )

    sealed interface ActionDetails {
        data object SuccessDetails : ActionDetails
        data object PendingDetails : ActionDetails
        data class FailureDetails(
            val reason: LemmyActionFailureReason,
        ) : ActionDetails
    }
}
