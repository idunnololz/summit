package com.idunnololz.summit.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.SavedStateHandle
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.api.utils.getImageUrl
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.lemmy.CommentsSortOrder
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.DefaultSortOrder
import com.idunnololz.summit.lemmy.LocalPostView
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.PostsRepository
import com.idunnololz.summit.lemmy.multicommunity.instance
import com.idunnololz.summit.lemmy.toApiSortOrder
import com.idunnololz.summit.lemmy.utils.getSortOrderForCommunity
import com.idunnololz.summit.preferences.PreferenceManager
import com.idunnololz.summit.preferences.perCommunity.PerCommunityPreferences
import com.idunnololz.summit.prefetcher.PostFeedPrefetcher
import com.idunnololz.summit.prefetcher.PostPrefetcher
import com.idunnololz.summit.util.ext.getParcelable
import com.idunnololz.summit.util.ext.toByteArray
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

@HiltWorker
class OfflinePostFeedWork @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val postsRepositoryFactory: PostsRepository.Factory,
    private val postFeedPrefetcher: PostFeedPrefetcher,
    private val postPrefetcher: PostPrefetcher,
    private val accountInfoManager: AccountInfoManager,
    private val preferenceManager: PreferenceManager,
    private val perCommunityPreferences: PerCommunityPreferences,
    private val offlineManager: OfflineManager,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "OfflinePostFeedWork"

        private const val NOTIFICATION_CHANNEL_ID = "offline_post_feed"
        private const val NOTIFICATION_CHANNEL_NAME = "Offline post feed"
        private const val NOTIFICATION_ID = 1337

        private const val ARG_COMMUNITY_REF = "ARG_COMMUNITY_REF"

        fun makeInputData(communityRef: CommunityRef): Data =
            workDataOf(ARG_COMMUNITY_REF to BoxedCommunityRef(communityRef).toByteArray())
    }

    @Parcelize
    enum class ProgressPhase(val index: Int) : Parcelable {
        Start(0),
        FetchingPostFeed(1),
        FetchingPosts(2),
        FetchingExtras(3),
        Complete(4),
    }

    @Parcelize
    private class BoxedCommunityRef(
        val communityRef: CommunityRef,
    ) : Parcelable

    private val postsRepository = postsRepositoryFactory.create(SavedStateHandle())

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun doWork(): Result {
        val progressTracker = ProgressTracker()

        setProgress(progressTracker.getProgressData())

        val communityRef =
            inputData.getParcelable<BoxedCommunityRef>(ARG_COMMUNITY_REF)?.communityRef

        val fullAccount = accountInfoManager.currentFullAccount.value
        val preferences = preferenceManager.getComposedPreferencesForAccount(
            fullAccount?.account,
        )

        val sortOrder = getSortOrderForCommunity(
            communityRef,
            preferences,
            perCommunityPreferences,
            fullAccount,
        ) ?: DefaultSortOrder

        postsRepository.setCommunity(communityRef)
        postsRepository.setSortOrder(sortOrder)

        val allPosts = mutableListOf<LocalPostView>()
        val offlinePostCount = preferenceManager.currentPreferences.getOfflinePostCount()
        var postsLoaded = 0
        var index = 0

        setProgress(
            progressTracker.apply {
                currentPhase = ProgressPhase.FetchingPostFeed
                subMax = offlinePostCount.toDouble()
            }.getProgressData(),
        )

        while (true) {
            val result = postFeedPrefetcher.suspendPrefetchPage(index, postsRepository)

            val posts = result.getOrNull()?.posts
            val postCount = posts?.size ?: 0

            if (posts != null) {
                allPosts += posts
            }

            if (postCount == 0) {
                break
            }

            postsLoaded += postCount

            setProgress(
                progressTracker.apply {
                    subCount = postsLoaded.toDouble()
                }.getProgressData(),
            )

            if (postsLoaded >= offlinePostCount) {
                break
            }

            index++
        }

        setProgress(
            progressTracker.apply {
                currentPhase = ProgressPhase.FetchingPosts
                subMax = allPosts.size.toDouble()
            }.getProgressData(),
        )

        postPrefetcher.prefetchPosts(
            postOrCommentRefs = allPosts.map { post ->
                Either.Left(
                    PostRef(
                        post.fetchedPost.source.instance
                            ?: post.fetchedPost.postView.instance,
                        post.fetchedPost.postView.post.id,
                    ),
                )
            },
            sortOrder = (preferences.defaultCommentsSortOrder ?: CommentsSortOrder.Top).toApiSortOrder(),
            maxDepth = if (preferences.collapseChildCommentsByDefault) {
                1
            } else {
                null
            },
            account = fullAccount?.account,
            onProgress = { count: Int, maxCount: Int ->
                setProgress(
                    progressTracker.apply {
                        subCount = count.toDouble()
                    }.getProgressData(),
                )
            },
        )

        setProgress(
            progressTracker.apply {
                currentPhase = ProgressPhase.FetchingExtras
                subMax = allPosts.size.toDouble()
            }.getProgressData(),
        )
        coroutineScope {
            var doneCount = 0
            val fetchImageJobs = mutableListOf<Job>()

            for (post in allPosts) {
                fetchImageJobs += launch {
                    val postView = post.fetchedPost.postView
                    val imageUrl = postView.getImageUrl(false)
                    if (imageUrl != null) {
                        withContext(Dispatchers.Main) {
                            suspendCancellableCoroutine<Unit> { cont ->
                                val registration = offlineManager.fetchImage(
                                    imageUrl,
                                    {
                                        cont.resume(Unit) {}
                                    },
                                    {
                                        Log.d(TAG, "Error downloading image for post $post. Url: $imageUrl", it)
                                        cont.resume(Unit) {}
                                    },
                                )

                                cont.invokeOnCancellation {
                                    registration.cancel(offlineManager)
                                }
                            }
                        }
                    }

                    doneCount++
                    setProgress(
                        progressTracker.apply {
                            subCount = doneCount.toDouble()
                        }.getProgressData(),
                    )
                }
            }

            setProgress(
                progressTracker.apply {
                    currentPhase = ProgressPhase.Complete
                }.getProgressData(),
            )

            fetchImageJobs.joinAll()
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_download_for_offline_24)
            .setOngoing(true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentTitle(context.getString(R.string.app_name))
            .setLocalOnly(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentText("Updating widget")
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    class ProgressTracker {

        companion object {
            private const val PROGRESS_PHASE = "PROGRESS_1"
            private const val PROGRESS_SUB_COUNT = "PROGRESS_2"
            private const val PROGRESS_SUB_MAX = "PROGRESS_3"

            fun fromData(data: Data) = ProgressTracker().apply {
                currentPhase = ProgressPhase.entries[data.getInt(PROGRESS_PHASE, 0)]
                subCount = data.getDouble(PROGRESS_SUB_COUNT, 0.0)
                subMax = data.getDouble(PROGRESS_SUB_MAX, 0.0)
            }
        }

        var currentPhase: ProgressPhase = ProgressPhase.Start
            set(value) {
                field = value
                subCount = 0.0
                subMax = 1.0
            }
        var subCount: Double = 0.0
        var subMax: Double = 0.0

        val progressPercent
            get() = (currentPhase.index / ((ProgressPhase.entries.size - 1).toDouble()))
        val subProgressPercent
            get() = subCount / subMax

        fun getProgressData() = workDataOf(
            PROGRESS_PHASE to currentPhase.ordinal,
            PROGRESS_SUB_COUNT to subCount,
            PROGRESS_SUB_MAX to subMax,
        )
    }
}
