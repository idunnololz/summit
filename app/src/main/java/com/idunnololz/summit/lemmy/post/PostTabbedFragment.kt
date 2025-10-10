package com.idunnololz.summit.lemmy.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.idunnololz.summit.databinding.TabbedFragmentPostBinding
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.community.PostListEngineItem
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.accountId
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.nsfwMode.NsfwModeManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.runPredrawDiscardingFrame
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PostTabbedFragment : BaseFragment<TabbedFragmentPostBinding>() {

  companion object {
    private const val TAG = "PostTabbedFragment"
  }

  private val args: PostTabbedFragmentArgs by navArgs()

  private var argumentsHandled = false

  @Inject
  lateinit var nsfwModeManager: NsfwModeManager

  @Inject
  lateinit var moreActionsHelper: MoreActionsHelper

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    setBinding(TabbedFragmentPostBinding.inflate(inflater, container, false))

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val parentFragment = parentFragment as? CommunityFragment

    fun getViewModel() = parentFragment?.viewModel

    val context = requireContext()

    val pagerAdapter = PostAdapter(
      this,
    ).apply {
      stateRestorationPolicy =
        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

      updateNsfwMode(nsfwModeManager)
    }
    pagerAdapter.setPages(getViewModel()?.postListEngine?.items ?: listOf())

    with(binding) {
      viewPager.adapter = pagerAdapter
      viewPager.offscreenPageLimit = 1
      viewPager.setPageTransformer(
        MarginPageTransformer(Utils.convertDpToPixel(16f).toInt()),
      )
      viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)

          val item = pagerAdapter.items[position]

          if (item is PostAdapter.Item.PostItem) {
            moreActionsHelper.onPostRead(
              postView = item.fetchedPost.postView,
              delayMs = 500,
              read = true,
            )
            parentFragment?.updateLastSelectedItem(
              PostRef(parentFragment.viewModel.apiInstance, item.fetchedPost.postView.post.id),
            )
          }

          if (position + 1 == pagerAdapter.itemCount) {
            val pageToFetch = item as? PostAdapter.Item.AutoLoadItem

            // end reached; load more items
            if (pageToFetch != null) {
              getViewModel()?.fetchPage(pageToFetch.pageToLoad)
            }
          }
        }
      })

      if (savedInstanceState == null && !argumentsHandled) {
        argumentsHandled = true

        val index = pagerAdapter.findPageIndex(args.id.toLong())
        if (index >= 0) {
          viewPager.setCurrentItem(index, false)
        }
      }

      getViewModel()?.loadedPostsData?.observe(viewLifecycleOwner) {
        when (it) {
          is StatefulData.Loading -> {}
          is StatefulData.NotStarted -> {}
          is StatefulData.Error,
          is StatefulData.Success,
          -> {
            val position = viewPager.currentItem
            pagerAdapter.setPages(getViewModel()?.postListEngine?.items ?: listOf())

            if (position > 0) {
              // Guard again activity recreate, don't scroll if this is an activity recreate
              viewPager.runPredrawDiscardingFrame {
                viewPager.setCurrentItem(position, true)
              }
            }
          }
        }
      }
    }
  }

  fun closePost(postFragment: PostFragment) {
    (parentFragment as? CommunityFragment)?.closePost(postFragment)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

//    outState.remove("android:support:fragments")
  }

  class PostAdapter(
    fragment: Fragment,
  ) : FragmentStateAdapter(fragment) {

    sealed interface Item {
      val id: Long

      class PostItem(
        override val id: Long,
        val fetchedPost: FetchedPost,
        val instance: String,
      ) : Item

      data class AutoLoadItem(
        override val id: Long,
        val pageToLoad: Int,
      ) : Item
    }

    var items: List<Item> = listOf()
    private var nsfwMode: Boolean = false

    override fun getItemId(position: Int): Long = when (val item = items[position]) {
      is Item.PostItem -> item.id
      is Item.AutoLoadItem -> item.id
    }

    override fun containsItem(itemId: Long): Boolean = items.any { it.id == itemId }

    override fun createFragment(position: Int): Fragment = when (val item = items[position]) {
      is Item.PostItem ->
        PostFragment().apply {
          arguments = PostFragmentArgs(
            instance = item.instance,
            id = item.fetchedPost.postView.post.id,
            reveal = nsfwMode,
            post = item.fetchedPost.postView,
            jumpToComments = false,
            currentCommunity = item.fetchedPost.postView.community.toCommunityRef(),
            videoState = null,
            accountId = item.fetchedPost.source.accountId ?: 0L,
            isInViewPager = true,
          ).toBundle()
        }
      is Item.AutoLoadItem ->
        PostListLoadingPageFragment()
    }

    override fun getItemCount(): Int = items.size

    fun setPages(data: List<PostListEngineItem>) {
      val oldItems = items
      val newItems = mutableListOf<Item>()

      data.forEach {
        when (it) {
          PostListEngineItem.EndItem,
          is PostListEngineItem.ErrorItem,
          is PostListEngineItem.FooterItem,
          PostListEngineItem.FooterSpacerItem,
          PostListEngineItem.HeaderItem,
          is PostListEngineItem.ManualLoadItem,
          is PostListEngineItem.PageTitle,
          is PostListEngineItem.PersistentErrorItem,
          is PostListEngineItem.FilteredPostItem,
          -> {}
          is PostListEngineItem.AutoLoadItem -> {
            newItems.add(
              Item.AutoLoadItem(
                id = it.pageToLoad * -1L,
                pageToLoad = it.pageToLoad,
              ),
            )
          }
          is PostListEngineItem.VisiblePostItem -> {
            newItems.add(
              Item.PostItem(
                id = it.fetchedPost.postView.post.id.toLong(),
                fetchedPost = it.fetchedPost,
                instance = it.instance,
              ),
            )
          }
        }
      }

      val diff = DiffUtil.calculateDiff(
        object : DiffUtil.Callback() {
          override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldItems[oldItemPosition].id == newItems[newItemPosition].id

          override fun getOldListSize(): Int = oldItems.size

          override fun getNewListSize(): Int = newItems.size

          override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            true
        },
      )
      this.items = newItems
      diff.dispatchUpdatesTo(this)
    }

    fun findPageIndex(id: Long): Int = items.indexOfLast {
      when (it) {
        is Item.PostItem -> it.id == id
        is Item.AutoLoadItem -> false
      }
    }

    fun updateNsfwMode(nsfwModeManager: NsfwModeManager) {
      val newValue = nsfwModeManager.nsfwModeEnabled.value
      if (nsfwMode == newValue) {
        return
      }

      nsfwMode = newValue

      @Suppress("NotifyDataSetChanged")
      notifyDataSetChanged()
    }
  }
}
