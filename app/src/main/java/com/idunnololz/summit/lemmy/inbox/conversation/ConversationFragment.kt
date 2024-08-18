package com.idunnololz.summit.lemmy.inbox.conversation

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.imageLoader
import coil.request.ImageRequest
import com.idunnololz.summit.R
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.ConversationItemBinding
import com.idunnololz.summit.databinding.FragmentConversationBinding
import com.idunnololz.summit.databinding.ItemConversationHeaderBinding
import com.idunnololz.summit.databinding.ItemConversationLoadMoreBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.inbox.InboxItem
import com.idunnololz.summit.lemmy.inbox.InboxTabbedFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.PrettyPrintStyles
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimen
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ConversationFragment : BaseFragment<FragmentConversationBinding>() {
    private val args by navArgs<ConversationFragmentArgs>()
    private val viewModel: ConversationViewModel by viewModels()

    @Inject
    lateinit var avatarHelper: AvatarHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = viewModel.setup(
            args.accountId,
            args.inboxItem,
            args.conversationItem,
            args.newConversation,
        )
        result.onFailure {
            (parentFragment as? InboxTabbedFragment)?.closeMessage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentConversationBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        viewModel.loadNextPage(force = true)

        requireMainActivity().apply {
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.mainContainer)
        }

        with(binding) {
            toolbar.apply {
                toolbar.setNavigationIcon(
                    com.google.android.material.R.drawable.ic_arrow_back_black_24,
                )
                toolbar.setNavigationIconTint(
                    context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
                )
                toolbar.setNavigationOnClickListener {
                    (parentFragment as? InboxTabbedFragment)?.closeMessage()
                }
            }

            val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
            val adapter = MessagesAdapter(context)

            fun markMessagesAsReadIfNeeded() {
                val firstPos = layoutManager.findFirstCompletelyVisibleItemPosition()
                val lastPos = layoutManager.findLastCompletelyVisibleItemPosition()

                for (i in firstPos..lastPos) {
                    val conversationItem =
                        (adapter.items.getOrNull(i) as? MessagesAdapter.Item.ConversationItem)
                            ?: continue

                    if (!conversationItem.isMe && !conversationItem.isRead) {
                        viewModel.markAsRead(conversationItem.messageItem.id)
                        conversationItem.isRead = true
                    }
                }
            }

            fun checkIfLoadMoreOnScreen() {
                val firstPos = layoutManager.findFirstVisibleItemPosition()
                val lastPos = layoutManager.findLastVisibleItemPosition()

                for (i in (firstPos - 1)..(lastPos + 1)) {
                    (adapter.items.getOrNull(i) as? MessagesAdapter.Item.LoadMoreItem)
                        ?: continue

                    viewModel.loadNextPage()
                }
            }

            recyclerView.adapter = adapter
            recyclerView.layoutManager = layoutManager
            recyclerView.setHasFixedSize(true)
            recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        markMessagesAsReadIfNeeded()
                        checkIfLoadMoreOnScreen()
                    }
                },
            )

            sendButton.setOnClickListener {
                val text = commentEditText.text

                if (!text.isNullOrBlank()) {
                    viewModel.sendComment(args.accountId, commentEditText.text.toString())
                }
            }

            viewModel.conversationInfoModel.observe(viewLifecycleOwner) { conversationInfoModel ->
                avatarHelper.loadAvatar(icon,
                    conversationInfoModel.otherPersonAvatar,
                    conversationInfoModel.otherPersonName ?: "",
                    conversationInfoModel.otherPersonId ?: 0L,
                    conversationInfoModel.otherPersonInstance ?: "",)

                title.text = conversationInfoModel.otherPersonName
                subtitle.text = conversationInfoModel.otherPersonInstance

                toolbarContentContainer.setOnClickListener {
                    getMainActivity()?.launchPage(
                        page = PersonRef.PersonRefByName(
                            conversationInfoModel.otherPersonName ?: "",
                            conversationInfoModel.otherPersonInstance ?: "",
                        )
                    )
                }
            }
            viewModel.conversationModel.observe(viewLifecycleOwner) {
                adapter.setItems(it)
            }
            viewModel.loadConversationState.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                    }
                }
            }
            viewModel.commentSentEvent.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        binding.sendButton.isEnabled = true
                        binding.commentEditText.isEnabled = true

                        ErrorDialogFragment
                            .show(
                                getString(R.string.error_unable_to_send_message),
                                it.error,
                                childFragmentManager,
                            )
                    }
                    is StatefulData.Loading -> {
                        binding.sendButton.isEnabled = false
                        binding.commentEditText.isEnabled = false
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        binding.sendButton.isEnabled = true
                        binding.commentEditText.isEnabled = true

                        binding.commentEditText.setText("")
                    }
                }
            }
            viewModel.draftModel.observe(viewLifecycleOwner) {
                binding.commentEditText.setText(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        requireMainActivity().apply {
            if (!navBarController.useNavigationRail) {
                navBarController.hideNavBar(animate = true)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        viewModel.saveDraft(binding.commentEditText.text?.toString())
    }

    private class MessagesAdapter(
        private val context: Context,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data object HeaderItem : Item

            data class ConversationItem(
                val isMe: Boolean,
                val messageItem: MessageItem,
                var isRead: Boolean,
                var bottomMargin: Int,
            ) : Item

            data class LoadMoreItem(
                val nextPageIndex: Int,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.ConversationItem ->
                        old.messageItem.id == (new as Item.ConversationItem).messageItem.id
                    is Item.HeaderItem -> true
                    is Item.LoadMoreItem -> true
                }
            },
        ).apply {
            addItemType(
                Item.HeaderItem::class,
                ItemConversationHeaderBinding::inflate,
            ) { item, b, h -> }
            addItemType(
                Item.ConversationItem::class,
                ConversationItemBinding::inflate,
            ) { item, b, h ->
                b.text.text = item.messageItem.content
                b.info.text = dateStringToPretty(
                    context = context,
                    ts = item.messageItem.lastUpdateTs,
                    style = PrettyPrintStyles.SHORT_DYNAMIC
                )

                if (item.isMe) {
                    b.text.setTextColor(
                        context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorOnPrimary,
                        ),
                    )
                    b.info.setTextColor(
                        context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorOnPrimary,
                        ),
                    )
                    b.cardView.setCardBackgroundColor(
                        context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorPrimary,
                        ),
                    )

                    b.info.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        startToStart = ConstraintLayout.LayoutParams.UNSET
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    }
                    b.cardView.updateLayoutParams<FrameLayout.LayoutParams> {
                        this.marginStart = context.getDimen(R.dimen.conversation_bubble_padding)
                        this.marginEnd = context.getDimen(R.dimen.padding)
                        this.gravity = Gravity.END
                        this.bottomMargin = item.bottomMargin
                    }
                } else {
                    b.text.setTextColor(
                        context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorOnPrimaryContainer,
                        ),
                    )
                    b.info.setTextColor(
                        context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorOnPrimaryContainer,
                        ),
                    )
                    b.cardView.setCardBackgroundColor(
                        context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorPrimaryContainer,
                        ),
                    )

                    b.info.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.UNSET
                    }
                    b.cardView.updateLayoutParams<FrameLayout.LayoutParams> {
                        this.marginStart = context.getDimen(R.dimen.padding)
                        this.marginEnd = context.getDimen(R.dimen.conversation_bubble_padding)
                        this.gravity = Gravity.START
                        this.bottomMargin = item.bottomMargin
                    }
                }
            }
            addItemType(
                Item.LoadMoreItem::class,
                ItemConversationLoadMoreBinding::inflate,
            ) { item, b, h -> }
        }

        private var model: ConversationModel? = null

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount

        val items
            get() = adapterHelper.items

        fun setItems(model: ConversationModel, cb: () -> Unit = {}) {
            this.model = model

            refreshItems(cb)
        }

        private fun refreshItems(cb: () -> Unit) {
            val model = model
            if (model == null) {
                adapterHelper.setItems(
                    newItems = listOf(),
                    adapter = this,
                    cb = cb,
                )
                return
            }

            val newItems = mutableListOf<Item>()

            newItems.add(Item.HeaderItem)
            model.allMessages.withIndex().mapTo(newItems) { (index, message) ->
                Item.ConversationItem(
                    isMe = message.authorId == model.accountId,
                    messageItem = message,
                    isRead = message.isRead,
                    bottomMargin = if (index == 0) {
                        0
                    } else {
                        context.getDimen(R.dimen.padding_half)
                    },
                )
            }

            if (model.hasMore) {
                newItems.add(Item.LoadMoreItem(model.nextPageIndex))
            }

            adapterHelper.setItems(
                newItems = newItems,
                adapter = this,
                cb = cb,
            )
        }
    }
}
