package com.idunnololz.summit.actions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.actions.ActionsRunnerHelper
import com.idunnololz.summit.actions.PendingActionsManager
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.lemmy.actions.LemmyActionFailureException
import com.idunnololz.summit.lemmy.actions.OldLemmyCompletedAction
import com.idunnololz.summit.lemmy.actions.OldLemmyFailedAction
import com.idunnololz.summit.lemmy.actions.LemmyAction
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ActionDetailsViewModel @Inject constructor(
  private val apiClient: LemmyApiClient,
  private val accountManager: AccountManager,
  private val actionsRunnerHelper: ActionsRunnerHelper,
  private val pendingActionsManager: PendingActionsManager,
) : ViewModel() {

  val retryActionResult = StatefulLiveData<Unit>()
  val deleteActionResult = StatefulLiveData<Unit>()

  fun markActionAsSeen(action: Action) {
    if (!action.seen) {
      pendingActionsManager.markActionAsSeen(action.toLemmyAction())
    }
  }

  fun retryAction(action: Action) {
    retryActionResult.setIsLoading()

    viewModelScope.launch {
      val actionInfo = action.info

      if (actionInfo == null) {
        retryActionResult.setError(RuntimeException("Action info is null"))
        return@launch
      }

      val result = actionsRunnerHelper
        .executeAction(
          actionInfo = actionInfo,
          retries = 0,
        )

      when (result) {
        is PendingActionsManager.ActionExecutionResult.Success -> {
          retryActionResult.setValue(Unit)

          pendingActionsManager.completeActionSuccess(
            action = LemmyAction(
              action.id,
              action.ts,
              action.creationTs,
              actionInfo,
            ),
            result = result.result,
          )
          if (action.details is ActionDetails.FailureDetails) {
            pendingActionsManager.deleteAction(
              action = action.toLemmyAction(),
            )
          }
        }
        is PendingActionsManager.ActionExecutionResult.Failure -> {
          retryActionResult.setError(LemmyActionFailureException(result.failureReason))
        }
      }
    }
  }

  fun deleteAction(action: Action) {
    deleteActionResult.setIsLoading()

    viewModelScope.launch {
      val actionInfo = action.info

      if (actionInfo == null) {
        retryActionResult.setError(RuntimeException("Action info is null"))
        return@launch
      }

      pendingActionsManager.deleteAction(
        action = action.toLemmyAction(),
      )

      deleteActionResult.postValue(Unit)
    }
  }
}
