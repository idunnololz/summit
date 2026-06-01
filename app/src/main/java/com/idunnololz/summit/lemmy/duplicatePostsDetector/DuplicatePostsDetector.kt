package com.idunnololz.summit.lemmy.duplicatePostsDetector

import android.util.Log
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.account.AccountManager.OnAccountChangedListener
import com.idunnololz.summit.account.GuestOrUserAccount
import com.idunnololz.summit.account.asAccount
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.utils.StableAccountId
import com.idunnololz.summit.models.PostView
import com.idunnololz.summit.preferences.Preferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@Singleton
class DuplicatePostsDetector @Inject constructor(
  private val preferences: Preferences,
  private val accountManager: AccountManager,
  private val factory: PerAccountDuplicatePostsDetector.Factory,
  private val coroutineScopeFactory: CoroutineScopeFactory,
) {

  private val coroutineScope = coroutineScopeFactory.create()
  private val detectors = mutableMapOf<StableAccountId, PerAccountDuplicatePostsDetector>()
  private var currentDuplicatePostsDetector: PerAccountDuplicatePostsDetector? = null
  var isEnabled: Boolean
    private set

  init {
    isEnabled = preferences.isHiddenPostsEnabled

    currentDuplicatePostsDetector =
      accountManager.currentAccount.asAccount?.getDuplicatePostsDetector()

    coroutineScope.launch {
      preferences.onPreferenceChangeFlow.collect {
        isEnabled = preferences.isHiddenPostsEnabled
      }
    }
    coroutineScope.launch {
      accountManager.addOnAccountChangedListener(object : OnAccountChangedListener {
        override suspend fun onAccountChanged(newAccount: GuestOrUserAccount?) {
          currentDuplicatePostsDetector = newAccount?.asAccount?.getDuplicatePostsDetector()
        }
      })
    }
  }

  suspend fun addReadOrHiddenPost(postView: PostView) {
    if (!isEnabled) return

    currentDuplicatePostsDetector?.addReadOrHiddenPost(postView)
  }

  suspend fun removeReadOrHiddenPost(postView: PostView) {
    if (!isEnabled) return

    currentDuplicatePostsDetector?.removeReadOrHiddenPost(postView)
  }

  fun isPostDuplicateOfRead(postView: PostView): Boolean {
    if (!isEnabled) return false

    return currentDuplicatePostsDetector?.isPostDuplicateOfRead(postView) == true
  }

  private fun Account.getDuplicatePostsDetector(): PerAccountDuplicatePostsDetector {
    val key = StableAccountId(id, instance)
    val detector = detectors[key]

    if (detector != null) {
      return detector
    }

    val detector2 = factory.create(id, coroutineScope)
    detectors[key] = detector2
    return detector2
  }
}

class PerAccountDuplicatePostsDetector @AssistedInject constructor(
  @Assisted("userId") private val userId: Long,
  @Assisted private val coroutineScope: CoroutineScope,
  private val perAccountDuplicatePostsDao: PerAccountDuplicatePostsDao,
) {

  companion object {
    private const val TAG = "PerAccountDuplicatePostsDetector"

    /**
     * Number of duplicate posts saved by the app. Once the limit is hit, new posts will replace
     * old ones based on the other they were added. Can be changed to access order easily in
     * [newLinkedHashMap].
     */
    private const val MAX_DUPLICATE_POSTS_MEMORY_LIMIT = 500L
  }

  @AssistedFactory
  interface Factory {
    fun create(
      @Assisted("userId") userId: Long,
      coroutineScope: CoroutineScope,
    ): PerAccountDuplicatePostsDetector
  }

  private var readPosts = newLinkedHashMap()
  private val mutex = Mutex()

  init {
    coroutineScope.launch(Dispatchers.IO) {
      val entry = perAccountDuplicatePostsDao.getEntry(userId).firstOrNull() ?: return@launch
      val postsData: PerAccountDuplicatePostsData =
        Cbor.decodeFromByteArray(entry.trackingEventCbor)

      val readPosts = newLinkedHashMap()
      postsData.postKeys.forEach { (key, value) ->
        readPosts[key] = value
      }

      this@PerAccountDuplicatePostsDetector.readPosts = readPosts

      Log.d(TAG, "Initialization complete. Loaded ${readPosts.size} entries. User: $userId")
    }
  }

  private fun newLinkedHashMap() = object : LinkedHashMap<String, Long>(
    (MAX_DUPLICATE_POSTS_MEMORY_LIMIT * 1.5).toInt(),
    0.75f,
    false, /* true if ordered by access order, false if ordered by insertion order*/
  ) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean =
      size > MAX_DUPLICATE_POSTS_MEMORY_LIMIT
  }

  suspend fun addReadOrHiddenPost(postView: PostView) {
    val key = generateKeyForPostView(postView)
      ?: return

    val previousValue = mutex.withLock {
      readPosts.put(key, postView.post.id.toLong())
    }

    if (previousValue == null) {
      // ReadPosts modified. Update db.
      commitToDb()
    }
  }

  suspend fun removeReadOrHiddenPost(postView: PostView) {
    val key = generateKeyForPostView(postView)
      ?: return

    val removed = mutex.withLock {
      readPosts.remove(key)
    }

    if (removed != null) {
      // ReadPosts modified. Update db.
      commitToDb()
    }
  }

  private fun commitToDb() {
    coroutineScope.launch(Dispatchers.IO) {
      Log.d(TAG, "Updating db...")

      val copy = mutex.withLock {
        readPosts.toList()
      }

      perAccountDuplicatePostsDao.insert(
        PerAccountDuplicatePostsEntry(
          userId,
          System.currentTimeMillis(),
          Cbor.encodeToByteArray(
            PerAccountDuplicatePostsData(
              postKeys = copy,
            ),
          ),
        ),
      )
    }
  }

  fun isPostDuplicateOfRead(postView: PostView): Boolean {
    val key = generateKeyForPostView(postView)
      ?: return false

    val readPost = readPosts[key]

    return readPost != null && readPost != postView.post.id.toLong()
  }

  private fun generateKeyForPostView(postView: PostView): String? {
    val key = buildString {
      append(postView.post.name)
      append("|")
      append(postView.post.url)

      if (postView.post.name.length < 8) {
        // If the post title is not unique enough then we will need to use the body as well...
        append("|")
        append(cleanPostBody(postView.post.body))
      }
    }

    if (key.length < 8) {
      return null
    } else {
      return key.take(256)
    }
  }

  private fun cleanPostBody(body: String?): String {
    if (body.isNullOrEmpty()) {
      return ""
    }

    var lines = body.split("\n").filter { it.isNotBlank() }

    if (lines.isEmpty()) {
      return ""
    }

    lines = if (lines[0].startsWith("cross-posted from:", ignoreCase = true)) {
      lines.drop(1)
    } else {
      lines
    }

    return buildString {
      for (line in lines) {
        val cleanedLine = line.replace(">", "").trim()

        if (cleanedLine.isNotBlank()) {
          append(cleanedLine)
          append(" ")
        }
      }
    }.trim()
  }
}
