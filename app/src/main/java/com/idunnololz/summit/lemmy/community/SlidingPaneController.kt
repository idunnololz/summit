package com.idunnololz.summit.lemmy.community

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.slidingpanelayout.widget.SlidingPaneLayout.PanelSlideListener
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentId
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.emptyScreen.EmptyScreenFragment
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.post.PostFragmentArgs
import com.idunnololz.summit.preferences.GlobalLayoutMode
import com.idunnololz.summit.preferences.GlobalLayoutModes
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.video.VideoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlidingPaneController(
    private val fragment: BaseFragment<*>,
    private val slidingPaneLayout: SlidingPaneLayout,
    private val childFragmentManager: FragmentManager,
    private val viewModel: PostViewPagerViewModel,
    private val globalLayoutMode: GlobalLayoutMode,
    /**
     * Used for tablets. The message is shown on the side pane when nothing is selected.
     */
    private val emptyScreenText: String,
    @IdRes private val fragmentContainerId: Int,
    val lockPanes: Boolean = false,
    private val retainClosedPosts: Boolean = false,
) {

    interface PostViewPagerViewModel {
        var lastSelectedItem: Either<PostRef, CommentRef>?
    }

    companion object {
        private const val TAG = "SlidingPaneController"
    }

    private var activeOpenPostJob: Job? = null
    private var activeClosePostJob: Job? = null
    private var lastPostFragment: PostFragment? = null
    var onPageSelectedListener: (isOpen: Boolean) -> Unit = {}
    var onPostOpen: (accountId: Long?, postView: PostView?) -> Unit = { _, _ -> }

    val isSlideable: Boolean
        get() = slidingPaneLayout.isSlideable

    val isOpen: Boolean
        get() = slidingPaneLayout.isOpen

    private val panelSlideListener =
        object : PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {
                Log.d(TAG, "onPanelSlide() - $slideOffset")

                if (slidingPaneLayout.isSlideable) {
                    fragment.getMainActivity()?.apply {
                        lockUiOpenness = false
                        setNavUiOpenPercent(1f - slideOffset)
                    }

                    slidingPaneLayout.getChildAt(0).alpha = 0.5f + (0.5f * slideOffset)
                }
            }

            override fun onPanelOpened(panel: View) {
                Log.d(TAG, "onPanelOpened()")

                fragment.requireMainActivity().apply {
                    lockUiOpenness = false
                }
                onPageSelectedListener(true)
                slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_UNLOCKED
            }

            override fun onPanelClosed(panel: View) {
                Log.d(TAG, "onPanelClosed()")

                // close post fragment
                val postFragment = childFragmentManager
                    .findFragmentById(R.id.post_fragment_container)
                if (postFragment != null) {
                    childFragmentManager.commit(allowStateLoss = true) {
                        if (retainClosedPosts) {
                            detach(postFragment)
                        } else {
                            remove(postFragment)
                        }
                    }

                    if (retainClosedPosts) {
                        lastPostFragment = postFragment as? PostFragment
                    }
                }
                if (slidingPaneLayout.isSlideable) {
                    fragment.getMainActivity()?.setNavUiOpenPercent(0f)
                }
                onPageSelectedListener(false)
                slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED
            }
        }

    fun init() {
        slidingPaneLayout.addPanelSlideListener(panelSlideListener)
        slidingPaneLayout.post {
            if (!slidingPaneLayout.isSlideable) {
                val firstChild = slidingPaneLayout.getChildAt(0) ?: return@post
                firstChild.findViewById<View>(R.id.pane_divider)?.visibility = View.VISIBLE

                val currentFragment = childFragmentManager.findFragmentById(fragmentContainerId)
                if (currentFragment == null) {
                    childFragmentManager.commit(allowStateLoss = true) {
                        setReorderingAllowed(true)
                        replace(
                            fragmentContainerId,
                            EmptyScreenFragment.newInstance(emptyScreenText),
                        )
                    }
                }
            } else {
                val firstChild = slidingPaneLayout.getChildAt(0) ?: return@post
                firstChild.findViewById<View>(R.id.pane_divider)?.visibility = View.GONE

                if (slidingPaneLayout.isOpen) {
                    slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_UNLOCKED
                } else {
                    slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED
                }
            }
        }

        if (globalLayoutMode == GlobalLayoutModes.SmallScreen) {
            slidingPaneLayout.getChildAt(0).updateLayoutParams<SlidingPaneLayout.LayoutParams> {
                width = SlidingPaneLayout.LayoutParams.MATCH_PARENT
            }
        }
    }

    fun openPost(
        instance: String,
        id: Int,
        currentCommunity: CommunityRef?,
        accountId: Long?,
        post: PostView? = null,
        jumpToComments: Boolean = false,
        reveal: Boolean = false,
        videoState: VideoState? = null,
    ) {
        try {
            // Best effort restore PostFragment

            val lastPostFragment = lastPostFragment

            if (lastPostFragment != null) {
                val args = PostFragmentArgs.fromBundle(requireNotNull(lastPostFragment.arguments))

                if (id == args.post?.post?.id) {
                    openPostInternal(
                        args = Bundle(),
                        itemRef = Either.Left(PostRef(instance, id)),
                        postFragmentOverride = lastPostFragment,
                    )
                    onPostOpen.invoke(accountId, post)
                    return
                } else {
                    this.lastPostFragment = null
                    childFragmentManager.commit(allowStateLoss = true) {
                        // Apparently we don't need to call attach before remove
                        // attach(lastPostFragment)
                        remove(lastPostFragment)
                    }
                }
            }
        } catch (e: Exception) {
            // do nothing
        }

        openPostInternal(
            args = PostFragmentArgs(
                instance = instance,
                id = id,
                reveal = reveal,
                post = post,
                jumpToComments = jumpToComments,
                currentCommunity = currentCommunity,
                videoState = videoState,
                accountId = accountId ?: 0L,
            ).toBundle(),
            itemRef = Either.Left(PostRef(instance, id)),
        )

        onPostOpen.invoke(accountId, post)
    }

    fun openComment(instance: String, commentId: CommentId) {
        openPostInternal(
            PostFragmentArgs(
                instance = instance,
                id = 0,
                commentId = commentId,
                currentCommunity = null,
                isSinglePage = false,
            ).toBundle(),
            Either.Right(CommentRef(instance, commentId)),
        )
    }

    private fun openPostInternal(
        args: Bundle,
        itemRef: Either<PostRef, CommentRef>? = null,
        postFragmentOverride: PostFragment? = null,
    ) {
        if (activeOpenPostJob != null) {
            Log.d(TAG, "Ignoring openPost() because it occurred too fast.")
            return
        }

        activeOpenPostJob = fragment.lifecycleScope.launch(Dispatchers.Main) {
            val fragment =
                postFragmentOverride
                    ?: PostFragment().apply {
                        arguments = args
                    }

            childFragmentManager.commit(allowStateLoss = true) {
                setReorderingAllowed(true)
                if (postFragmentOverride != null) {
                    attach(fragment)
                }
                replace(R.id.post_fragment_container, fragment)
            }

            if (postFragmentOverride != null) {
                withContext(Dispatchers.IO) {
                    // Restoring the last fragment is laggy. Delay for a bit to reduce stuttering.
                    delay(100)
                }
            }

            viewModel.lastSelectedItem = itemRef

            openPane()

            withContext(Dispatchers.IO) {
                delay(250)
            }
            activeOpenPostJob = null
        }
    }

    fun closePost(fragment: Fragment) {
        if (activeClosePostJob != null) {
            Log.d(TAG, "Ignoring closePost() because it occurred too fast.")
            return
        }

        activeClosePostJob = fragment.lifecycleScope.launch(Dispatchers.Main) {
            closePane()

            withContext(Dispatchers.IO) {
                delay(250)
            }
            activeClosePostJob = null
        }
    }

    fun callPageSelected() {
        onPageSelectedListener(slidingPaneLayout.isOpen)
    }

    private fun openPane() {
        slidingPaneLayout.openPane()
        if (!slidingPaneLayout.isSlideable) {
            panelSlideListener.onPanelOpened(slidingPaneLayout)
        }
    }

    private fun closePane() {
        slidingPaneLayout.closePane()
        if (!slidingPaneLayout.isSlideable) {
            panelSlideListener.onPanelClosed(slidingPaneLayout)
        }
    }
}
