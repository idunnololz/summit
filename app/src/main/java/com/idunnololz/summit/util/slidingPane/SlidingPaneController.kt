package com.idunnololz.summit.util.slidingPane

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
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.actions.PostReadManager
import com.idunnololz.summit.api.dto.lemmy.CommentId
import com.idunnololz.summit.emptyScreen.EmptyScreenFragment
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.post.PostFragmentArgs
import com.idunnololz.summit.lemmy.post.PostTabbedFragment
import com.idunnololz.summit.lemmy.post.PostTabbedFragmentArgs
import com.idunnololz.summit.models.PostView
import com.idunnololz.summit.preferences.GlobalLayoutMode
import com.idunnololz.summit.preferences.GlobalLayoutModes
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.video.VideoState
import com.idunnololz.summit.view.FixedSlidingPaneLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlidingPaneController @AssistedInject constructor(
  private val animationsHelper: AnimationsHelper,
  @Assisted private val fragment: BaseFragment<*>,
  @Assisted private val slidingPaneLayout: FixedSlidingPaneLayout,
  @Assisted private val childFragmentManager: FragmentManager,
  @Assisted private val viewModel: PostViewPagerViewModel,
  @Assisted private val globalLayoutMode: GlobalLayoutMode,
  /**
   * Used for tablets. The message is shown on the side pane when nothing is selected.
   */
  @Assisted private val emptyScreenText: String,
  @Assisted("fragmentContainerId") @IdRes private val fragmentContainerId: Int,
  @Assisted("retainClosedPosts")
  private val retainClosedPosts: Boolean = false,
  @Assisted("useSwipeBetweenPosts")
  private val useSwipeBetweenPosts: Boolean = false,
  @Assisted("isPreview")
  private val isPreview: Boolean = false,
) {

  @AssistedFactory
  interface Factory {
    fun create(
      fragment: BaseFragment<*>,
      slidingPaneLayout: FixedSlidingPaneLayout,
      childFragmentManager: FragmentManager,
      viewModel: PostViewPagerViewModel,
      globalLayoutMode: GlobalLayoutMode,
      /**
       * Used for tablets. The message is shown on the side pane when nothing is selected.
       */
      emptyScreenText: String,
      @Assisted("fragmentContainerId")
      @IdRes fragmentContainerId: Int,
      @Assisted("retainClosedPosts")
      retainClosedPosts: Boolean = false,
      @Assisted("useSwipeBetweenPosts")
      useSwipeBetweenPosts: Boolean = false,
      @Assisted("isPreview")
      isPreview: Boolean = false,
    ): SlidingPaneController
  }

  interface PostViewPagerViewModel {
    val postReadManager: PostReadManager
    var lastSelectedItem: Either<PostRef, CommentRef>?
  }

  companion object {
    private const val TAG = "SlidingPaneController"
  }

  private var activeOpenPostJob: Job? = null
  private var activeClosePostJob: Job? = null
  private var lastPostFragment: PostFragment? = null
  var lockNavBar = false
  var onPageSelectedListener: (isOpen: Boolean) -> Unit = {}
  var onPostOpen: (accountId: Long?, postView: PostView?) -> Unit = { _, _ -> }
  var panelSlideListener: SlidingPaneLayout.PanelSlideListener? = null

  val isSlideable: Boolean
    get() = slidingPaneLayout.isSlideable

  val isOpen: Boolean
    get() = slidingPaneLayout.isOpen

  var panelClosedNavBarOpenPercent: Float = 1f
  var panelOpenNavBarOpenPercent: Float = 0f

  private val _panelSlideListener =
    object : SlidingPaneLayout.PanelSlideListener {
      override fun onPanelSlide(panel: View, slideOffset: Float) {
        Log.d(TAG, "onPanelSlide() - $slideOffset")

        if (slidingPaneLayout.isSlideable) {
          fragment.getMainActivity()?.apply {
            val delta = panelOpenNavBarOpenPercent - panelClosedNavBarOpenPercent
            if (!lockNavBar) {
              setNavUiOpenPercent(
                panelClosedNavBarOpenPercent + delta * (1f - slideOffset),
                force = isSlideable,
              )
            }
          }

          slidingPaneLayout.getChildAt(0).alpha = 0.5f + (0.5f * slideOffset)
        }

        panelSlideListener?.onPanelSlide(panel, slideOffset)
      }

      override fun onPanelOpened(panel: View) {
        Log.d(TAG, "onPanelOpened()")

        fragment.getMainActivity()?.apply {
          setNavUiOpenPercent(panelOpenNavBarOpenPercent, force = isSlideable)
        }

        onPageSelectedListener(true)
        slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_UNLOCKED

        panelSlideListener?.onPanelOpened(panel)
      }

      override fun onPanelClosed(panel: View) {
        Log.d(TAG, "onPanelClosed()")

        fragment.getMainActivity()?.apply {
          setNavUiOpenPercent(panelClosedNavBarOpenPercent, force = isSlideable)
        }

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
          fragment.getMainActivity()?.setNavUiOpenPercent(
            1f * panelClosedNavBarOpenPercent,
            force = isSlideable,
          )
        }
        onPageSelectedListener(false)
        slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED

        panelSlideListener?.onPanelClosed(panel)
      }
    }

  fun init() {
    slidingPaneLayout.addPanelSlideListener(_panelSlideListener)
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
              EmptyScreenFragment.Companion.newInstance(emptyScreenText),
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

      if (slidingPaneLayout.isOpen && slidingPaneLayout.isSlideable) {
        _panelSlideListener.onPanelOpened(slidingPaneLayout)
      }
    }

    if (globalLayoutMode == GlobalLayoutModes.SmallScreen) {
      slidingPaneLayout.getChildAt(0).updateLayoutParams<SlidingPaneLayout.LayoutParams> {
        width = SlidingPaneLayout.LayoutParams.MATCH_PARENT
      }
    }

    slidingPaneLayout.isSwipeEnabled = !useSwipeBetweenPosts
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

        if (id == args.id) {
          openPostInternal(
            args = null,
            removeLastPostFragment = false,
            itemRef = Either.Left(PostRef(instance, id)),
            postFragmentOverride = lastPostFragment,
          )
          onPostOpen.invoke(accountId, post)
          return
        }
      }
    } catch (_: Exception) {
      // do nothing
    }

    val postReadInfo = viewModel.postReadManager.getPostReadInfo(instance, id)

    openPostInternal(
      args = PostFragmentArgs(
        instance = instance,
        id = id,
        reveal = reveal,
        isPreview = isPreview,
        jumpToComments = jumpToComments,
        currentCommunity = currentCommunity,
        videoState = videoState,
        accountId = accountId ?: 0L,
        lastReadTs = if (postReadInfo?.read == true || post?.read == true) {
          postReadInfo?.ts ?: (System.currentTimeMillis() - (15 * 60 * 1000))
        } else {
          0L
        },
      ),
      removeLastPostFragment = true,
      itemRef = Either.Left(PostRef(instance, id)),
    )

    onPostOpen.invoke(accountId, post)
  }

  fun openComment(instance: String, commentId: CommentId) {
    openPostInternal(
      args = PostFragmentArgs(
        instance = instance,
        id = 0,
        isPreview = isPreview,
        commentId = commentId,
        currentCommunity = null,
        isSinglePage = false,
      ),
      removeLastPostFragment = false,
      itemRef = Either.Right(CommentRef(instance, commentId)),
    )
  }

  private fun openPostInternal(
    args: PostFragmentArgs?,
    removeLastPostFragment: Boolean,
    itemRef: Either<PostRef, CommentRef>? = null,
    postFragmentOverride: PostFragment? = null,
  ) {
    if (activeOpenPostJob != null) {
      Log.d(TAG, "Ignoring openPost() because it occurred too fast.")
      return
    }

    val lastPostFragment = lastPostFragment

    activeOpenPostJob = fragment.lifecycleScope.launch(Dispatchers.Main) {
      if (removeLastPostFragment && lastPostFragment != null) {
        this@SlidingPaneController.lastPostFragment = null
        childFragmentManager.commit(allowStateLoss = true) {
          // Apparently we don't need to call attach before remove
          // attach(lastPostFragment)
          remove(lastPostFragment)
        }
      }

      val fragment =
        postFragmentOverride
          ?: if (useSwipeBetweenPosts) {
            PostTabbedFragment().apply {
              arguments = PostTabbedFragmentArgs(
                id = args?.id ?: 0,
              ).toBundle()
            }
          } else {
            PostFragment().apply {
              arguments = args?.toBundle() ?: Bundle()
            }
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

      if (animationsHelper.shouldAnimate(AnimationsHelper.AnimationLevel.Navigation)) {
        withContext(Dispatchers.IO) {
          delay(250)
        }
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

      if (animationsHelper.shouldAnimate(AnimationsHelper.AnimationLevel.Navigation)) {
        withContext(Dispatchers.IO) {
          delay(250)
        }
      }
      activeClosePostJob = null
    }
  }

  fun callPageSelected() {
    onPageSelectedListener(slidingPaneLayout.isOpen)
  }

  private fun openPane() {
    if (animationsHelper.shouldAnimate(AnimationsHelper.AnimationLevel.Navigation)) {
      slidingPaneLayout.openPane()
    } else {
      slidingPaneLayout.openPaneNoAnimation()
    }
    if (!slidingPaneLayout.isSlideable) {
      _panelSlideListener.onPanelOpened(slidingPaneLayout)
    }
  }

  private fun closePane() {
    if (animationsHelper.shouldAnimate(AnimationsHelper.AnimationLevel.Navigation)) {
      slidingPaneLayout.closePane()
    } else {
      slidingPaneLayout.closePaneNoAnimation()
    }
    if (!slidingPaneLayout.isSlideable) {
      _panelSlideListener.onPanelClosed(slidingPaneLayout)
    }
  }

  fun setPanelOffset(offset: Float) {
    if (!fragment.isBindingAvailable()) return
    slidingPaneLayout.smoothSlideTo(offset)
  }

  fun lockNavBar() {
    lockNavBar = true
  }

  fun unlockNavBar() {
    lockNavBar = false
  }
}
