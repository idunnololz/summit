package com.idunnololz.summit.preferences

import com.idunnololz.summit.preferences.PostQuickActionIds.Voting
import com.idunnololz.summit.preferences.PostQuickActionIds.VotingNoScores
import com.idunnololz.summit.preferences.PostQuickActionIds.VotingWithScores
import com.idunnololz.summit.preferences.PostQuickActionIds.VotingWithUpAndDownScores
import kotlinx.serialization.Serializable

@Serializable
data class PostsInFeedQuickActionsSettings(
  val enabled: Boolean = false,
  val actions: List<PostQuickActionId> = listOf(),
)

fun Preferences.generateQuickActions(): PostsInFeedQuickActionsSettings {
  val preferences = this
  val postsInFeedQuickActions = preferences.postsInFeedQuickActions
    ?: PostsInFeedQuickActionsSettings()

  fun List<Int>.updateWithPreferences() = this.map {
    if (it == Voting) {
      if (preferences.postShowUpAndDownVotes) {
        VotingWithUpAndDownScores
      } else if (preferences.hidePostScores) {
        VotingNoScores
      } else {
        VotingWithScores
      }
    } else {
      it
    }
  }

  if (postsInFeedQuickActions.enabled) {
    return postsInFeedQuickActions.copy(
      actions = postsInFeedQuickActions.actions.updateWithPreferences(),
    )
  } else {
    return postsInFeedQuickActions.copy(
      actions = listOf(Voting).updateWithPreferences(),
    )
  }
}