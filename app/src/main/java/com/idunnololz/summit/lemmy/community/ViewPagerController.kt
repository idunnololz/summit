package com.idunnololz.summit.lemmy.community

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
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
    private val viewModel: CommunityViewModel,
    private val onPageSelected: (Int) -> Unit,
) {

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
                    Log.d(TAG, "onPageScrolled: $positionOffset")
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
        viewPager.setDurationScroll(350)
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

        childFragmentManager.commitNow {
            replace(R.id.post_fragment_container, fragment)
        }

        onPostOpen()
        viewPager.setCurrentItem(1, true)

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
        val mainFragment = fragment.requireParentFragment().requireParentFragment() as MainFragment
        mainFragment.setStartPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)

        viewPagerAdapter.onPostOpen()
    }

    private fun onPostClosed() {
        val mainFragment = fragment.requireParentFragment().requireParentFragment() as MainFragment
        mainFragment.setStartPanelLockState(OverlappingPanelsLayout.LockState.UNLOCKED)

        viewPagerAdapter.onPostClosed()
    }

    class ViewPagerAdapter() : PagerAdapter() {

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
}