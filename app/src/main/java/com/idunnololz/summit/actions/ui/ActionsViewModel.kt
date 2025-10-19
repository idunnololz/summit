package com.idunnololz.summit.actions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.PendingActionsManager
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureReason
import com.idunnololz.summit.lemmy.actions.LemmyActionResult
import com.idunnololz.summit.lemmy.actions.OldLemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.OldLemmyFailedAction
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class ActionsViewModel @Inject constructor(
  private val pendingActionsManager: PendingActionsManager,
  private val accountManager: AccountManager,
) : ViewModel() {

  val actionsDataLiveData = StatefulLiveData<ActionsData>()
  private val actionsListener =
    object : PendingActionsManager.OnActionChangedListener {
      override fun onActionAdded(action: LemmyAction) {
        loadActions()
      }

      override fun onActionFailed(action: LemmyAction, reason: LemmyActionFailureReason) {
        loadActions()
      }

      override fun onActionComplete(action: LemmyAction, result: LemmyActionResult<*, *>) {
        loadActions()
      }

      override fun onActionDeleted(action: LemmyAction) {
        loadActions()
      }
    }

  init {
    loadActions()

    pendingActionsManager.addActionCompleteListener(actionsListener)
  }

  override fun onCleared() {
    super.onCleared()
    pendingActionsManager.removeActionCompleteListener(actionsListener)
  }

  fun loadActions() {
    actionsDataLiveData.setIsLoading()

    viewModelScope.launch(Dispatchers.Default) {
      val pendingActions = pendingActionsManager.getAllPendingActions()
      val completedActions = pendingActionsManager.getAllCompletedActions()
      val failedAccountInfo = pendingActionsManager.getAllFailedActions()

      val accountIds = mutableSetOf<Long>()

      pendingActions.mapNotNullTo(accountIds) { it.info?.accountId }
      completedActions.mapNotNullTo(accountIds) { it.info?.accountId }
      failedAccountInfo.mapNotNullTo(accountIds) { it.info?.accountId }

      val accountDictionary = accountIds.associateWith { accountManager.getAccountById(it) }

      val actionsData = ActionsData(
        pendingActions = pendingActions.pendingToActions()
          .sortedByDescending { it.ts },
        completedActions = completedActions.completedToActions()
          .sortedByDescending { it.ts },
        failedActions = failedAccountInfo.failedToActions()
          .sortedByDescending { it.ts },
        accountDictionary = accountDictionary,
      )

      actionsDataLiveData.postValue(actionsData)
    }
  }

  fun markActionAsSeen(action: Action) {
    val value = actionsDataLiveData.valueOrNull
      ?: return

    if (action.seen) {
      return
    }

    fun List<Action>.markActionAsSeen() =
      map {
        if (it.id == action.id) {
          it.copy(seen = true)
        } else {
          it
        }
      }

    val newValue = value.copy(
      pendingActions = value.pendingActions.markActionAsSeen(),
      completedActions = value.completedActions.markActionAsSeen(),
      failedActions = value.failedActions.markActionAsSeen(),
    )
    actionsDataLiveData.setValue(newValue)
  }

  private fun List<LemmyAction>.pendingToActions(): List<Action> = this.map { it.toAction() }

  private fun List<LemmyAction>.completedToActions(): List<Action> = this.map { it.toAction() }

  private fun List<LemmyAction>.failedToActions(): List<Action> = this.map { it.toAction() }

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
}
