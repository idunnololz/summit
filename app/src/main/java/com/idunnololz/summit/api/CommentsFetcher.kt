package com.idunnololz.summit.api

import arrow.core.Either
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.CommentSortType
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostId

class CommentsFetcher(
    private val apiClient: AccountAwareLemmyClient,
) {

    suspend fun fetchCommentsWithRetry(
        id: Either<PostId, CommentId>,
        sort: CommentSortType,
        maxDepth: Int?,
        force: Boolean,
    ): Result<List<CommentView>> =
        apiClient.fetchCommentsWithRetry(
            id = id,
            sort = sort,
            maxDepth = maxDepth,
            force = force,
        )

    suspend fun fetchCommentsWithRetry(
        id: Either<PostId, CommentId>,
        sort: CommentSortType,
        maxDepth: Int?,
        force: Boolean,
        account: Account?,
    ): Result<List<CommentView>> =
        apiClient.fetchCommentsWithRetry(
            id = id,
            sort = sort,
            maxDepth = maxDepth,
            force = force,
            account = account,
        )
}
