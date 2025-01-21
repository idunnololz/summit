package com.idunnololz.summit.lemmy.duplicatePostsDetector

import android.content.SharedPreferences
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountManager
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.coroutine.CoroutineScopeFactory
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.utils.StableAccountId
import com.idunnololz.summit.lemmy.utils.toVotableRef
import com.idunnololz.summit.preferences.Preferences
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import javax.inject.Inject

class DuplicatePostsDetector @Inject constructor(
    private val preferences: Preferences,
    private val accountManager: AccountManager,
    private val factory: PerAccountDuplicatePostsDetector.Factory,
    private val coroutineScopeFactory: CoroutineScopeFactory,
) {

    private val coroutineScope = coroutineScopeFactory.create()
    private val detectors = mutableMapOf<StableAccountId, PerAccountDuplicatePostsDetector>()
    private var currentDuplicatePostsDetector: PerAccountDuplicatePostsDetector? = null
    private var isEnabled: Boolean

    init {
        isEnabled = preferences.isHiddenPostsEnabled

        coroutineScope.launch {
            preferences.onPreferenceChangeFlow.collect {
                isEnabled = preferences.isHiddenPostsEnabled
            }
        }
        coroutineScope.launch {
            accountManager.currentAccount.collect {
                currentDuplicatePostsDetector =
                    (it as? Account)?.getDuplicatePostsDetector()
            }
        }
    }

    fun addReadOrHiddenPost(postView: PostView) {
        if (!isEnabled) return

        currentDuplicatePostsDetector?.addReadOrHiddenPost(postView)
    }

    fun getPostDuplicates(postView: PostView): PostRef? {
        if (!isEnabled) return null

        return currentDuplicatePostsDetector?.getPostDuplicates(postView)
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

        val detector2 = factory.create()
        detectors[key] = detector2
        return detector2
    }
}

class PerAccountDuplicatePostsDetector @AssistedInject constructor() {

    @AssistedFactory
    interface Factory {
        fun create(): PerAccountDuplicatePostsDetector
    }

    private val readPosts = mutableMapOf<String, PostRef>()

    fun addReadOrHiddenPost(postView: PostView) {
        val key = generateKeyForPostView(postView)

        if (key != null) {
            readPosts[key] = PostRef(postView.instance, postView.post.id)
        }
    }

    fun getPostDuplicates(postView: PostView): PostRef? {
        val key = generateKeyForPostView(postView)
            ?: return null

        return readPosts[key]
    }

    fun isPostDuplicateOfRead(postView: PostView): Boolean {
        val key = generateKeyForPostView(postView)
            ?: return false

        val readPost = readPosts[key]

        return readPost != null && readPost != PostRef(postView.instance, postView.post.id)
    }

    private fun generateKeyForPostView(postView: PostView): String? {
        val key = buildString {
            append(postView.post.name)
            append("|")
            append(postView.post.url)
            append("|")
            append(postView.post.body)
        }

        if (key.length < 25) {
            return null
        } else {
            return key
        }
    }
}