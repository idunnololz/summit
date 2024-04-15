package com.idunnololz.summit.lemmy.inbox

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.TabbedFragmentInboxBinding
import com.idunnololz.summit.lemmy.community.SlidingPaneController
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PageItem
import com.idunnololz.summit.util.TwoPaneOnBackPressedCallback
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InboxTabbedFragment : BaseFragment<TabbedFragmentInboxBinding>() {

    private val args by navArgs<InboxTabbedFragmentArgs>()

    private val viewModel: InboxTabbedViewModel by viewModels()
    private val inboxViewModel: InboxViewModel by activityViewModels()

    @Inject
    lateinit var preferences: Preferences

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

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            TwoPaneOnBackPressedCallback(binding.slidingPaneLayout),
        )

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

//            binding.viewPager.isUserInputEnabled = it.size > 1
        }
        viewModel.pageItems.value?.let {
            pagerAdapter.setPages(it)
        }

        fun onPageChanged() {
        }

        val slidingPaneController = SlidingPaneController(
            fragment = this,
            slidingPaneLayout = binding.slidingPaneLayout,
            childFragmentManager = childFragmentManager,
            viewModel = viewModel,
            globalLayoutMode = preferences.globalLayoutMode,
            emptyScreenText = getString(R.string.select_a_message),
            fragmentContainerId = R.id.message_fragment_container,
        ).apply {
            onPageSelectedListener = a@{ isOpen ->
                if (!isBindingAvailable()) {
                    return@a
                }

                if (!isOpen) {
                    viewModel.removeAllButFirst()

                    getMainActivity()?.setNavUiOpenPercent(0f)

                    val fragment = childFragmentManager.findFragmentById(R.id.message_fragment_container)

                    if (fragment != null) {
                        childFragmentManager.commit {
                            setReorderingAllowed(true)
                            remove(fragment)
                        }
                    }

                    inboxViewModel.isUserOnInboxScreen.value = true
                } else {
                    getMainActivity()?.setNavUiOpenPercent(1f)

                    inboxViewModel.isUserOnInboxScreen.value = false
                }
            }

            init()
        }

        binding.root.post {
            if (!slidingPaneController.isOpen) {
                inboxViewModel.isUserOnInboxScreen.value = true
            }
        }

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                setReorderingAllowed(true)
                replace(
                    R.id.inbox_fragment_container,
                    InboxFragment::class.java,
                    InboxFragmentArgs(InboxViewModel.PageType.All).toBundle(),
                )
            }
        }

        viewModel.notificationInboxItem.observe(viewLifecycleOwner) {
            it ?: return@observe

            openMessage(it.inboxItem, it.instance)

            val inboxFragment = childFragmentManager.findFragmentById(R.id.inbox_fragment_container)
                as? InboxFragment

            inboxFragment?.viewModel?.markAsRead(it.inboxItem, read = true, delete = false)
        }

        if (args.notificationId > 0) {
            viewModel.findInboxItemFromNotificationId(args.notificationId)
        }

        onPageChanged()
        viewModel.updateUnreadCount()
    }

    override fun onResume() {
        super.onResume()
        inboxViewModel.pauseUnreadUpdates = true
    }

    override fun onPause() {
        inboxViewModel.isUserOnInboxScreen.value = false
        inboxViewModel.pauseUnreadUpdates = false
        super.onPause()
    }

    fun openMessage(item: InboxItem, instance: String) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.message_fragment_container, MessageFragment::class.java, MessageFragmentArgs(item, instance).toBundle())
            // If it's already open and the detail pane is visible, crossfade
            // between the fragments.
            if (binding.slidingPaneLayout.isOpen) {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
        }
        binding.slidingPaneLayout.open()
    }

    fun closeMessage() {
        binding.slidingPaneLayout.closePane()
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
