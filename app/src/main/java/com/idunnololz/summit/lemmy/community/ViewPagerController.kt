package com.idunnololz.summit.lemmy.community

import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.discord.panels.OverlappingPanelsLayout
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.post.PostFragment
import com.idunnololz.summit.lemmy.post.PostFragmentArgs
import com.idunnololz.summit.main.MainFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.CustomScrollViewPager
import com.idunnololz.summit.util.DepthPageTransformer
import com.idunnololz.summit.video.VideoState


class ViewPagerController(
    private val fragment: BaseFragment<*>,
    private val viewPager: CustomScrollViewPager,
    private val childFragmentManager: FragmentManager,
    private val viewModel: PostViewPagerViewModel,
    private val lockPanes: Boolean = false,
    private val onPageSelected: (Int) -> Unit,
) {

    interface PostViewPagerViewModel {
        val viewPagerAdapter: ViewPagerAdapter
        var lastSelectedPost: PostRef?
    }

    companion object {
        private const val TAG = "ViewPagerController"
    }

    private val viewPagerAdapter = viewModel.viewPagerAdapter

    fun init() {
        viewPager.adapter = viewPagerAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (!viewPager.isLaidOut) {
                    return
                }
                if (position == 0) {
                    fragment.getMainActivity()?.setNavUiOpenness(positionOffset)
                }

            }

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    Log.d(TAG, "REAL POSITION: ${viewPager.currentItem}")

                    val position = viewPager.currentItem

                    if (position == 0) {
                        // close post fragment
                        val postFragment = childFragmentManager
                            .findFragmentById(R.id.post_fragment_container)
                        if (postFragment != null) {
                            childFragmentManager.commit(allowStateLoss = true) {
                                remove(postFragment)
                            }
                        }
                        fragment.getMainActivity()?.setNavUiOpenness(0f)
                    }

                    onPageSelected()
                    this@ViewPagerController.onPageSelected(position)
                } else {

                    fragment.requireMainActivity().apply {
                        lockUiOpenness = false
                    }
                }
            }

        })
        viewPager.setPageTransformer(false, DepthPageTransformer())
    }

    fun openPost(
        instance: String,
        id: Int,
        currentCommunity: CommunityRef?,
        post: PostView? = null,
        jumpToComments: Boolean = false,
        reveal: Boolean = false,
        videoState: VideoState? = null,
    ) {
        val fragment = PostFragment()
            .apply {
                arguments = PostFragmentArgs(
                    instance = instance,
                    id =  id,
                    reveal = reveal,
                    post = post,
                    jumpToComments = jumpToComments,
                    currentCommunity = currentCommunity,
                    videoState = videoState,
                ).toBundle()
            }

        childFragmentManager.commitNow(allowStateLoss = true) {
            replace(R.id.post_fragment_container, fragment)
        }

        onPostOpen()
//        viewPager.setCurrentItem(1, true)
        animatePagerTransition(true)

        viewModel.lastSelectedPost = PostRef(instance, id)
    }

    fun closePost(fragment: Fragment) {
        viewPager.setCurrentItem(0, true)
    }

    fun onPageSelected() {
        if (viewPager.currentItem == 1) {
            fragment.requireMainActivity().apply {
                setupForFragment<PostFragment>()
                setNavUiOpenness(1f)
                lockUiOpenness = true
            }
            onPostOpen()
        } else if (viewPager.currentItem == 0) {
            fragment.requireMainActivity().apply {
                setupForFragment<CommunityFragment>()
                lockUiOpenness = false
            }
            onPostClosed()
        }
    }

    private fun onPostOpen() {
        val mainFragment = fragment.parentFragment?.parentFragment as? MainFragment
        mainFragment?.setStartPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)

        viewPagerAdapter.onPostOpen()
        viewPager.setPagingEnabled(true)
    }

    private fun onPostClosed() {
        val mainFragment = fragment.parentFragment?.parentFragment as? MainFragment
        if (!lockPanes) {
            mainFragment?.setStartPanelLockState(OverlappingPanelsLayout.LockState.UNLOCKED)
        }

        viewPagerAdapter.onPostClosed()
        viewPager.setPagingEnabled(false)
    }

    class ViewPagerAdapter : PagerAdapter() {

        private var count = 1

        fun onPostClosed() {
            count = 1
            notifyDataSetChanged()
        }

        fun onPostOpen() {
            count = 2
            notifyDataSetChanged()
        }

        override fun getCount(): Int = count

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return (`object` as View) === view
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any =
            when (position) {
                0 -> container.getChildAt(0)
                1 -> container.getChildAt(1)
                else -> error("ASDF")
            }
    }

    private fun animatePagerTransition(forward: Boolean) {
        val animator = ValueAnimator.ofInt(
            0,
            viewPager.width - if (forward) viewPager.paddingLeft else viewPager.paddingRight
        )
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                try {
                    viewPager.endFakeDrag()
                } catch (e: Exception) {
                    // do nothing
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                try {
                    viewPager.endFakeDrag()
                } catch (e: Exception) {
                    // do nothing
                }
            }

            override fun onAnimationRepeat(animation: Animator) {}
        })
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener(object : AnimatorUpdateListener {
            private var oldDragPosition = 0
            override fun onAnimationUpdate(animation: ValueAnimator) {
                val dragPosition = animation.animatedValue as Int
                val dragOffset = dragPosition - oldDragPosition
                oldDragPosition = dragPosition
                try {
                    viewPager.fakeDragBy((dragOffset * if (forward) -1 else 1).toFloat())
                } catch (e: Exception) {
                    // do nothing
                }
            }
        })
        animator.duration = 250
        viewPager.beginFakeDrag()
        animator.start()
    }
}