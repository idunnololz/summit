package com.idunnololz.summit.lemmy.post

import android.content.Context
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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.TabbedFragmentPostBinding
import com.idunnololz.summit.lemmy.community.CommunityFragment
import com.idunnololz.summit.lemmy.community.PostListEngineItem
import com.idunnololz.summit.lemmy.inbox.InboxTabbedFragment.InboxPagerAdapter
import com.idunnololz.summit.lemmy.multicommunity.FetchedPost
import com.idunnololz.summit.lemmy.multicommunity.accountId
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.nsfwMode.NsfwModeManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PageItem
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDrawableCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PostTabbedFragment :
    BaseFragment<TabbedFragmentPostBinding>() {

    private val args: PostTabbedFragmentArgs by navArgs()

    private var argumentsHandled = false

    @Inject
    lateinit var nsfwModeManager: NsfwModeManager

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

        val context = requireContext()
        val items = (parentFragment as? CommunityFragment)?.viewModel?.postListEngine?.items
            ?: listOf()

        val pagerAdapter = PostAdapter(
            context,
            this,
        ).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            updateNsfwMode(nsfwModeManager)
        }
        pagerAdapter.setPages(items)

        with(binding) {
            viewPager.adapter = pagerAdapter
            viewPager.offscreenPageLimit = 1
            viewPager.setPageTransformer(
                MarginPageTransformer(Utils.convertDpToPixel(16f).toInt()))

            if (savedInstanceState == null && !argumentsHandled) {
                argumentsHandled = true

                val index = pagerAdapter.findPageIndex(args.id.toLong())
                if (index >= 0) {
                    viewPager.setCurrentItem(index, false)
                }
            }
        }
    }

    fun closePost(postFragment: PostFragment) {
        (parentFragment as? CommunityFragment)?.closePost(postFragment)
    }

    class PostAdapter(
        private val context: Context,
        fragment: Fragment,
    ) : FragmentStateAdapter(fragment) {

        sealed interface Item {
            val id: Long

            class PostItem(
                override val id: Long,
                val fetchedPost: FetchedPost,
                val instance: String,
            ): Item
        }

        var items: List<Item> = listOf()
        private var nsfwMode: Boolean = false

        override fun getItemId(position: Int): Long {
            return when(val item = items[position]) {
                is Item.PostItem -> item.id
            }
        }

        override fun containsItem(itemId: Long): Boolean =
            items.any { it.id == itemId }

        override fun createFragment(position: Int): Fragment {
            return when(val item = items[position]) {
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
                        ).toBundle()
                    }
            }
        }

        override fun getItemCount(): Int = items.size

        fun setPages(data: List<PostListEngineItem>) {
            val oldItems = items
            val newItems = mutableListOf<Item>()

            data.forEach {
                when (it) {
                    is PostListEngineItem.AutoLoadItem,
                    PostListEngineItem.EndItem,
                    is PostListEngineItem.ErrorItem,
                    is PostListEngineItem.FooterItem,
                    PostListEngineItem.FooterSpacerItem,
                    PostListEngineItem.HeaderItem,
                    is PostListEngineItem.ManualLoadItem,
                    is PostListEngineItem.PageTitle,
                    is PostListEngineItem.PersistentErrorItem,
                    is PostListEngineItem.FilteredPostItem -> {}
                    is PostListEngineItem.VisiblePostItem -> {
                        newItems.add(Item.PostItem(
                            id = it.fetchedPost.postView.post.id.toLong(),
                            fetchedPost = it.fetchedPost,
                            instance = it.instance,
                        ))
                    }
                }
            }

            val diff = DiffUtil.calculateDiff(
                object : DiffUtil.Callback() {
                    override fun areItemsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int,
                    ): Boolean {
                        return oldItems[oldItemPosition].id == newItems[newItemPosition].id
                    }

                    override fun getOldListSize(): Int = oldItems.size

                    override fun getNewListSize(): Int = newItems.size

                    override fun areContentsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int,
                    ): Boolean {
                        return true
                    }
                },
            )
            this.items = newItems
            diff.dispatchUpdatesTo(this)
        }

        fun findPageIndex(id: Long): Int {
            return items.indexOfLast {
                when(it) {
                    is Item.PostItem -> it.id == id
                }
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