package com.idunnololz.summit.util.resolver

import com.idunnololz.summit.api.AccountAwareLemmyClient
import com.idunnololz.summit.api.dto.lemmy.PostView
import com.idunnololz.summit.util.arrow.Either
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex

class PostResolverHelper @AssistedInject constructor(
    @Assisted private val coroutineScope: CoroutineScope,
    private val accountAwareLemmyClient: AccountAwareLemmyClient,
) {

  @AssistedFactory
  interface Factory {
    fun create(coroutineScope: CoroutineScope): PostResolverHelper
  }

  private val resolverHelper = GenericResolverHelper(
    coroutineScope,
    { postId, force ->
      accountAwareLemmyClient.fetchPostWithRetry(Either.Left(postId.toInt()), force = force)
        .map { it.postView }
    }
  )

  val postDictionary
    get() = resolverHelper.objectDictionary
  val onPostDictionaryChanged
    get() = resolverHelper.onObjectDictionaryChanged

  fun fetchPost(
    postId: Long,
    force: Boolean,
  ) {
    resolverHelper.fetchObject(postId, force)
  }
}