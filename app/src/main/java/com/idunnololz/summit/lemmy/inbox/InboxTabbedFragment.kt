package com.idunnololz.summit.lemmy.inbox

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.TabbedFragmentInboxBinding
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.DepthPageTransformer2
import com.idunnololz.summit.util.PageItem
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InboxTabbedFragment : BaseFragment<TabbedFragmentInboxBinding>() {

    private val viewModel: InboxTabbedViewModel by viewModels()
    private val inboxViewModel: InboxViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(TabbedFragmentInboxBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        val pagerAdapter = InboxPagerAdapter(
            context,
            this,
        ).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        viewModel.pageItems.observe(viewLifecycleOwner) {
            it ?: return@observe

            pagerAdapter.setPages(it)

            binding.viewPager.isUserInputEnabled = it.size > 1
        }
        viewModel.pageItems.value?.let {
            pagerAdapter.setPages(it)
        }

        fun onPageChanged() {
            if (binding.viewPager.currentItem == 0) {
                viewModel.removeAllButFirst()

                getMainActivity()?.setNavUiOpenness(0f)
            } else {
                getMainActivity()?.setNavUiOpenness(1f)
            }
        }

        binding.viewPager.offscreenPageLimit = 99
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.setPageTransformer(DepthPageTransformer2())
        binding.viewPager.setCurrentItem(viewModel.pagePosition, false)
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    if (!binding.viewPager.isLaidOut) {
                        return
                    }
                    if (position == 0) {
                        getMainActivity()?.setNavUiOpenness(positionOffset)
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)

                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        onPageChanged()
                    }
                }
            },
        )

        onPageChanged()
        viewModel.updateUnreadCount()
    }

    override fun onResume() {
        super.onResume()
        inboxViewModel.pauseUnreadUpdates = true
    }

    override fun onPause() {
        inboxViewModel.pauseUnreadUpdates = false
        super.onPause()
    }

    override fun onDestroyView() {
        viewModel.pagePosition = binding.viewPager.currentItem
        binding.viewPager.adapter = null

        super.onDestroyView()
    }

    fun openMessage(item: InboxItem, instance: String) {
        viewModel.openMessage(item, instance)

        binding.viewPager.post {
            binding.viewPager.currentItem = 1
        }
    }

    fun closeMessage() {
        binding.viewPager.currentItem = 0
    }

    fun showLogin() {
        val direction = InboxTabbedFragmentDirections.actionGlobalLogin()
        findNavController().navigateSafe(direction)
    }

    class InboxPagerAdapter(
        private val context: Context,
        fragment: Fragment,
    ) : FragmentStateAdapter(fragment), TabLayoutMediator.TabConfigurationStrategy {

        var items: List<PageItem> = listOf()

        override fun getItemId(position: Int): Long {
            return items[position].id
        }

        override fun containsItem(itemId: Long): Boolean =
            items.any { it.id == itemId }

        override fun createFragment(position: Int): Fragment {
            val fragment = items[position].clazz.newInstance() as Fragment
            fragment.apply {
                arguments = items[position].args
            }

            return fragment
        }

        override fun getItemCount(): Int = items.size

        override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
            val item = items[position]
            tab.text = item.title
            if (item.drawable != null) {
                tab.icon = context.getDrawableCompat(item.drawable)?.apply {
                    setTint(context.getColorCompat(R.color.colorTextTitle))
                }
            }
        }

        fun setPages(newItems: List<PageItem>) {
            val oldItems = items

            val diff = DiffUtil.calculateDiff(
                object : DiffUtil.Callback() {
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
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
    }
}
