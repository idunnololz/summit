package com.idunnololz.summit.lemmy.inbox

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.discord.panels.OverlappingPanelsLayout
import com.discord.panels.PanelState
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.loadProfileImageOrDefault
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.databinding.FragmentInboxBinding
import com.idunnololz.summit.databinding.InboxListItemBinding
import com.idunnololz.summit.databinding.InboxListLoaderItemBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentArgs
import com.idunnololz.summit.lemmy.inbox.repository.LemmyListSource
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.links.LinkType
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.CustomDividerItemDecoration
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showBottomMenuForLink
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class InboxFragment :
    BaseFragment<FragmentInboxBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        private val TAG = "InboxFragment"
    }

    private val args by navArgs<InboxFragmentArgs>()

    val viewModel: InboxViewModel by activityViewModels()

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    @Inject
    lateinit var accountInfoManager: AccountInfoManager

    @Inject
    lateinit var preferences: Preferences

    private var adapter: InboxItemAdapter? = null

    private val paneOnBackPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!isBindingAvailable()) return

            binding.paneLayout.closePanels()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            requireMainActivity().apply {
                setupForFragment<InboxTabbedFragment>()
            }
        }

        setFragmentResultListener(AddOrEditCommentFragment.REQUEST_KEY) { _, _ ->
//            val result = bundle.getParcelableCompat<AddOrEditCommentFragment.Result>(AddOrEditCommentFragment.REQUEST_KEY_RESULT)
//
//            if (result != null) {
//                viewModel.fetchPostData(args.instance, args.id)
//            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentInboxBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val parentFragment = parentFragment as InboxTabbedFragment

        requireMainActivity().apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)

            insetViewAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.coordinatorLayout)
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.startPane)
        }

        viewModel.pageType.observe(viewLifecycleOwner) {
            it ?: return@observe

            getMainActivity()?.supportActionBar?.title = it.getName(context)
            binding.itemHighlighter.updateLayoutParams<ConstraintLayout.LayoutParams> {
                when (it) {
                    InboxViewModel.PageType.Unread -> {
                        topToTop = R.id.unread
                        bottomToBottom = R.id.unread
                    }
                    InboxViewModel.PageType.All -> {
                        topToTop = R.id.all
                        bottomToBottom = R.id.all
                    }
                    InboxViewModel.PageType.Replies -> {
                        topToTop = R.id.replies
                        bottomToBottom = R.id.replies
                    }
                    InboxViewModel.PageType.Mentions -> {
                        topToTop = R.id.mentions
                        bottomToBottom = R.id.mentions
                    }
                    InboxViewModel.PageType.Messages -> {
                        topToTop = R.id.messages
                        bottomToBottom = R.id.messages
                    }
                    InboxViewModel.PageType.Reports -> {
                        topToTop = R.id.reports
                        bottomToBottom = R.id.reports
                    }
                }
            }

            binding.recyclerView.postDelayed({
                if (isBindingAvailable()) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }, 100,)
        }

        viewModel.inboxData.observe(viewLifecycleOwner) {
            onUpdate()
        }
        viewModel.currentFullAccount.observe(viewLifecycleOwner) {
            if (it?.accountInfo?.miscAccountInfo?.modCommunityIds?.isNotEmpty() == true) {
                setModReportsVisibility(isVisible = true)
            } else {
                setModReportsVisibility(isVisible = false)
            }
        }

        val adapter = createAdapter()
        this.adapter = adapter

        fun refresh() {
            viewModel.pageIndex = 0
            viewModel.fetchInbox(force = true)
        }

        binding.loadingView.setOnRefreshClickListener {
            if (accountInfoManager.currentFullAccount.value == null) {
                parentFragment.showLogin()
            } else {
                refresh()
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refresh()
        }

        (viewModel.inboxData.value as? StatefulData.Success)?.data?.let {
            adapter.setData(it)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addOnScrollListener(
            object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
                        ?: return

                    if (layoutManager.findLastVisibleItemPosition() == adapter.itemCount - 1) {
                        if (!viewModel.inboxData.isLoading && adapter.hasMore()) {
                            viewModel.pageIndex++
                            viewModel.fetchInbox()
                        }
                    }
                }
            },
        )
        binding.recyclerView.addItemDecoration(
            CustomDividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL,
            ).apply {
                setDrawable(
                    checkNotNull(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.vertical_divider,
                        ),
                    ),
                )
            },
        )
        binding.fab.setOnClickListener {
            viewModel.markAllAsRead()
        }
        ItemTouchHelper(
            InboxSwipeToActionCallback(
                context,
                context.getColorCompat(R.color.style_green),
                R.drawable.baseline_check_24,
                binding.recyclerView,
            ) { viewHolder, _ ->
                val inboxItem = adapter.getItemAt(viewHolder.absoluteAdapterPosition)
                if (inboxItem != null) {
                    viewModel.markAsRead(
                        inboxItem = inboxItem,
                        read = true,
                        delete = viewModel.pageType.value == InboxViewModel.PageType.Unread,
                    )
                }
            },
        ).attachToRecyclerView(binding.recyclerView)

        fun updatePaneBackPressHandler() {
            if (binding.paneLayout.getSelectedPanel() != OverlappingPanelsLayout.Panel.CENTER) {
                paneOnBackPressHandler.remove()
                requireMainActivity().onBackPressedDispatcher.addCallback(paneOnBackPressHandler)
            } else {
                paneOnBackPressHandler.remove()
            }
        }

        binding.paneLayout.setEndPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)
        binding.paneLayout
            .registerStartPanelStateListeners(
                object : OverlappingPanelsLayout.PanelStateListener {
                    override fun onPanelStateChange(panelState: PanelState) {
                        when (panelState) {
                            PanelState.Closed -> {
                                getMainActivity()?.setNavUiOpenPercent(0f)

                                updatePaneBackPressHandler()
                            }
                            is PanelState.Closing -> {
                                getMainActivity()?.setNavUiOpenPercent(panelState.progress)
                            }
                            PanelState.Opened -> {
                                getMainActivity()?.setNavUiOpenPercent(100f)
                                updatePaneBackPressHandler()
                            }
                            is PanelState.Opening -> {
                                getMainActivity()?.setNavUiOpenPercent(panelState.progress)
                            }
                        }
                    }
                },
            )

        binding.toolbar.setNavigationIcon(R.drawable.baseline_menu_24)
        binding.toolbar.setNavigationOnClickListener {
            binding.paneLayout.openStartPanel()
        }

        binding.accountImageView.setOnClickListener {
            AccountsAndSettingsDialogFragment.newInstance()
                .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
        }

        with(binding) {
            unread.setOnClickListener {
                viewModel.pageTypeFlow.value = InboxViewModel.PageType.Unread
                paneLayout.closePanels()
            }
            all.setOnClickListener {
                viewModel.pageTypeFlow.value = InboxViewModel.PageType.All
                paneLayout.closePanels()
            }
            replies.setOnClickListener {
                viewModel.pageTypeFlow.value = InboxViewModel.PageType.Replies
                paneLayout.closePanels()
            }
            mentions.setOnClickListener {
                viewModel.pageTypeFlow.value = InboxViewModel.PageType.Mentions
                paneLayout.closePanels()
            }
            messages.setOnClickListener {
                viewModel.pageTypeFlow.value = InboxViewModel.PageType.Messages
                paneLayout.closePanels()
            }
            reports.setOnClickListener {
                viewModel.pageTypeFlow.value = InboxViewModel.PageType.Reports
                paneLayout.closePanels()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            accountInfoManager.currentFullAccountOnChange.collect {
                withContext(Dispatchers.Main) {
                    if (!isBindingAvailable()) return@withContext

                    viewModel.pageIndex = 0
                    viewModel.fetchInbox()
                }
            }
        }

        viewModel.currentAccountView.observe(viewLifecycleOwner) {
            it.loadProfileImageOrDefault(binding.accountImageView)
        }
        viewModel.currentAccount.observe(viewLifecycleOwner) {
            adapter.accountId = it?.id
        }

        viewModel.markAsReadResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error ->
                    ErrorDialogFragment.show(
                        message = getString(R.string.error_unable_to_mark_message_as_read),
                        error = it.error,
                        fm = childFragmentManager,
                    )
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {}
            }
        }

        if (args.pageType == InboxViewModel.PageType.Reports) {
            binding.fab.visibility = View.GONE
        }
        binding.fab.setup(preferences)

        viewModel.fetchInbox()
    }

    private fun setModReportsVisibility(isVisible: Boolean) {
        if (!isBindingAvailable()) return

        with(binding) {
            val visibility = if (isVisible) View.VISIBLE else View.GONE
            divider.visibility = visibility
            reports.visibility = visibility
        }
    }

    private fun markAsRead(inboxItem: InboxItem, read: Boolean) {
        viewModel.markAsRead(inboxItem, read)
    }

    private fun createAdapter(): InboxItemAdapter {
        return InboxItemAdapter(
            accountId = viewModel.currentAccount.value?.id,
            postAndCommentViewBuilder,
            viewModel.instance,
            viewLifecycleOwner,
            onImageClick = { url ->
                getMainActivity()?.openImage(null, binding.appBar, null, url, null)
            },
            onVideoClick = { url, videoType, state ->
                getMainActivity()?.openVideo(url, videoType, state)
            },
            onMarkAsRead = { inboxItem, read ->
                markAsRead(inboxItem, read)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onMessageClick = {
                if (!it.isRead && it !is ReportItem) {
                    markAsRead(it, read = true)
                }
                (parentFragment as? InboxTabbedFragment)?.openMessage(it, viewModel.instance)
            },
            onAddCommentClick = { inboxItem ->
                if (accountInfoManager.currentFullAccount.value == null) {
                    PreAuthDialogFragment.newInstance(R.id.action_add_comment)
                        .show(childFragmentManager, "asdf")
                    return@InboxItemAdapter
                }

                AddOrEditCommentFragment().apply {
                    arguments =
                        AddOrEditCommentFragmentArgs(
                            viewModel.instance,
                            null,
                            null,
                            null,
                            inboxItem,
                        ).toBundle()
                }.show(childFragmentManager, "asdf")
            },
            onOverflowMenuClick = {
                getMainActivity()?.showBottomMenu(
                    BottomMenu(requireContext()).apply {
                        setTitle(R.string.message_actions)
                        addItem(io.noties.markwon.R.id.none, R.string.no_options)
                    },
                )
            },
            onSignInRequired = {
                PreAuthDialogFragment.newInstance()
                    .show(childFragmentManager, "asdf")
            },
            onInstanceMismatch = { accountInstance, apiInstance ->
                AlertDialogFragment.Builder()
                    .setTitle(R.string.error_account_instance_mismatch_title)
                    .setMessage(
                        getString(
                            R.string.error_account_instance_mismatch,
                            accountInstance,
                            apiInstance,
                        ),
                    )
                    .createAndShow(childFragmentManager, "aa")
            },
            onLinkClick = { url, text, linkType ->
                onLinkClick(url, text, linkType)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showBottomMenuForLink(url, text)
            },
        )
    }

    private fun onUpdate() {
        when (val data = viewModel.inboxData.value) {
            is StatefulData.Error -> {
                binding.swipeRefreshLayout.isRefreshing = false
                if (data.error is NotAuthenticatedException) {
                    binding.loadingView.showErrorWithRetry(
                        getString(R.string.please_sign_in_to_view_your_inbox),
                        getString(R.string.sign_in),
                    )
                } else {
                    binding.loadingView.showDefaultErrorMessageFor(data.error)
                }
            }
            is StatefulData.Loading -> {
                if (adapter?.isEmpty() == true) {
                    binding.loadingView.showProgressBar()
                }
            }
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                binding.loadingView.hideAll()
                binding.swipeRefreshLayout.isRefreshing = false

                val itemCount = data.data.sumOf { it.items.size }
                if (itemCount == 0 && (adapter?.itemCount ?: 0) == 0) {
                    binding.loadingView.showErrorWithRetry(
                        getString(R.string.there_doesnt_seem_to_be_anything_here),
                        getString(R.string.refresh),
                    )
                } else {
                    Log.d(TAG, "onUpdate. Got ${data.data.sumOf { it.items.size }} items!")
                    (binding.recyclerView.adapter as? InboxItemAdapter)?.setData(data.data)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        binding.paneLayout.isEnabled = false
    }

    override fun onResume() {
        binding.paneLayout.isEnabled = true

        super.onResume()
    }

    private class InboxItemAdapter(
        var accountId: Long?,
        private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
        private val instance: String,
        private val lifecycleOwner: LifecycleOwner,
        private val onImageClick: (String) -> Unit,
        private val onVideoClick: (url: String, videoType: VideoType, videoState: VideoState?) -> Unit,
        private val onMarkAsRead: (InboxItem, Boolean) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onMessageClick: (InboxItem) -> Unit,
        private val onAddCommentClick: (InboxItem) -> Unit,
        private val onOverflowMenuClick: (InboxItem) -> Unit,
        private val onSignInRequired: () -> Unit,
        private val onInstanceMismatch: (String, String) -> Unit,
        private val onLinkClick: (url: String, text: String, linkType: LinkType) -> Unit,
        private val onLinkLongClick: (String, String) -> Unit,
    ) : RecyclerView.Adapter<ViewHolder>() {

        private sealed interface Item {
            data class InboxListItem(
                val inboxItem: InboxItem,
            ) : Item

            data class LoaderItem(
                val state: StatefulData<Unit>,
            ) : Item
        }

        private var allData: List<LemmyListSource.PageResult<InboxItem>> = listOf()

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.InboxListItem ->
                        old.inboxItem.id == (new as Item.InboxListItem).inboxItem.id
                    is Item.LoaderItem -> true
                }
            },
        ).apply {
            addItemType(Item.InboxListItem::class, InboxListItemBinding::inflate) { item, b, _ ->
                postAndCommentViewBuilder.bindMessage(
                    b = b,
                    instance = instance,
                    accountId = accountId,
                    viewLifecycleOwner = lifecycleOwner,
                    item = item.inboxItem,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onMarkAsRead = onMarkAsRead,
                    onPageClick = onPageClick,
                    onMessageClick = onMessageClick,
                    onAddCommentClick = onAddCommentClick,
                    onOverflowMenuClick = onOverflowMenuClick,
                    onSignInRequired = onSignInRequired,
                    onInstanceMismatch = onInstanceMismatch,
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                )
            }
            addItemType(Item.LoaderItem::class, InboxListLoaderItemBinding::inflate) { item, b, _ ->
                when (val state = item.state) {
                    is StatefulData.Error -> {
                        b.loadingView.showDefaultErrorMessageFor(state.error)
                    }
                    is StatefulData.Loading ->
                        b.loadingView.showProgressBar()
                    is StatefulData.NotStarted ->
                        b.loadingView.showProgressBar()
                    is StatefulData.Success ->
                        b.loadingView.showErrorText(R.string.no_more_items)
                }
                b.root.setTag(R.id.swipe_enabled, false)
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            val newItems = mutableListOf<Item>()

            allData.forEach { data ->
                data.items.mapTo(newItems) {
                    Item.InboxListItem(it)
                }
            }

            if (allData.isNotEmpty()) {
                if (allData.last().hasMore) {
                    newItems.add(Item.LoaderItem(StatefulData.NotStarted()))
                } else {
                    newItems.add(Item.LoaderItem(StatefulData.Success(Unit)))
                }
            }

            adapterHelper.setItems(newItems, this)
        }

        fun hasMore(): Boolean = allData.lastOrNull()?.hasMore ?: true

        fun isEmpty(): Boolean = allData.isEmpty() || allData.sumOf { it.items.size } == 0

        fun getItemAt(position: Int): InboxItem? =
            when (val item = adapterHelper.items.getOrNull(position)) {
                is Item.InboxListItem -> item.inboxItem
                is Item.LoaderItem -> null
                null -> null
            }

        fun setData(allData: List<LemmyListSource.PageResult<InboxItem>>) {
            this.allData = allData
            refreshItems()
        }
    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }
}
