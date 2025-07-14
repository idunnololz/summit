package com.idunnololz.summit.lemmy.mod.viewVotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.api.dto.lemmy.PostId
import com.idunnololz.summit.api.dto.lemmy.VoteView
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.util.StatefulLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@HiltViewModel
class ViewVotesViewModel @Inject constructor(
  private val apiClient: AccountAwareLemmyClient,
  private val votesRepositoryFactory: VotesRepository.Factory,
) : ViewModel() {

  val apiInstance: String
    get() = apiClient.instance

  private var postOrCommentId: Either<PostId, CommentId>? = null
  private var votesRepository: VotesRepository? = null

  val votesModel = StatefulLiveData<VotesModel>()

  private val pages = mutableListOf<LemmyListSource.PageResult<VoteView>>()
  private var loadingJob: Job? = null

  fun loadVotes(postId: PostId, commentId: CommentId) {
    val postOrCommentId = if (postId != 0) {
      Either.Left(postId)
    } else {
      Either.Right(commentId)
    }

    if (postOrCommentId == this.postOrCommentId) {
      return
    }

    this.postOrCommentId = postOrCommentId
    votesRepository = votesRepositoryFactory.create(postOrCommentId)

    reset()
    loadPage(0)
  }

  private fun reset() {
    pages.clear()
    loadingJob?.cancel()
  }

  fun loadMoreIfNeeded() {
    if (votesModel.isLoading) {
      return
    }

    val lastPage = pages.lastOrNull()
    val hasMore = lastPage?.hasMore ?: true

    if (!hasMore) {
      return
    }

    val nextPage = (lastPage?.pageIndex ?: 0) + 1
    loadPage(nextPage)
  }

  fun loadPage(page: Int) {
    val votesRepository = votesRepository ?: return

    votesModel.setIsLoading()

    loadingJob = viewModelScope.launch {
      val result = votesRepository.getPage(page, force = false)

      ensureActive()

      result
        .onSuccess {
          pages.add(it)

          votesModel.postValue(
            VotesModel(
              pages,
            ),
          )
        }
        .onFailure {
          votesModel.postError(it)
        }
    }
  }

  fun resetAndLoad() {
    reset()
    loadPage(0)
  }
}
