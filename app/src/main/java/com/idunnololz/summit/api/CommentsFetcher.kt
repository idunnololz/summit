package com.idunnololz.summit.api

import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountActionsManager
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostId
import com.idunnololz.summit.lemmy.utils.toVotableRef
import com.idunnololz.summit.util.retry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentsFetcher @Inject constructor(
    private val apiClient: AccountAwareLemmyClient,
    private val accountActionsManager: AccountActionsManager,
) {

    suspend fun fetchCommentsWithRetry(
        id: Either<PostId, CommentId>,
        sort: CommentSortType,
        force: Boolean,
    ): Result<List<CommentView>> =
        apiClient.fetchCommentsWithRetry(id, sort, force)
            .onSuccess {
                if (force) {
                    it.forEach {
                        accountActionsManager.setScore(it.toVotableRef(), it.counts.score)
                    }
                }
            }
}