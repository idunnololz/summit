package com.idunnololz.summit.lemmy.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommentReplyView
import com.idunnololz.summit.api.dto.PersonMentionView
import com.idunnololz.summit.api.dto.PrivateMessageView
import com.idunnololz.summit.databinding.FragmentInboxBinding
import com.idunnololz.summit.databinding.InboxListItemBinding
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.video.VideoState

class InboxFragment : BaseFragment<FragmentInboxBinding>() {

    private val args by navArgs<InboxFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentInboxBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentFragment = parentFragment as InboxTabbedFragment

        requireMainActivity().apply {
            insetViewExceptTopAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.recyclerView)
        }

        when (args.pageType) {
            InboxViewModel.PageType.All -> {
                parentFragment.viewModel.replies.observe(viewLifecycleOwner) {
                    onUpdate()
                }
                parentFragment.viewModel.mentions.observe(viewLifecycleOwner) {
                    onUpdate()
                }
                parentFragment.viewModel.messages.observe(viewLifecycleOwner) {
                    onUpdate()
                }
            }
            InboxViewModel.PageType.Replies -> {
                parentFragment.viewModel.replies.observe(viewLifecycleOwner) {
                    onUpdate()
                }
            }
            InboxViewModel.PageType.Mentions -> {
                parentFragment.viewModel.mentions.observe(viewLifecycleOwner) {
                    onUpdate()
                }
            }
            InboxViewModel.PageType.Messages -> {
                parentFragment.viewModel.messages.observe(viewLifecycleOwner) {
                    onUpdate()
                }
            }
        }

        fun refresh() {
            when (args.pageType) {
                InboxViewModel.PageType.All -> {
                    parentFragment.viewModel.fetchInbox(force = true)
                }
                InboxViewModel.PageType.Replies -> {
                    parentFragment.viewModel.fetchReplies(force = true)
                }
                InboxViewModel.PageType.Mentions -> {
                    parentFragment.viewModel.fetchMentions(force = true)
                }
                InboxViewModel.PageType.Messages -> {
                    parentFragment.viewModel.fetchMessages(force = true)
                }
            }
        }

        binding.loadingView.setOnRefreshClickListener {
            refresh()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refresh()
        }

        val adapter = InboxItemAdapter(
            parentFragment.postAndCommentViewBuilder,
            parentFragment.viewModel.instance,
            onImageClick = { url ->
                getMainActivity()?.openImage(null, url, null)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
        )

        (getData() as? StatefulData.Success)?.data?.let {
            adapter.data = it
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
    }

    private fun getData(): StatefulData<List<Item>> {
        val parentFragment = parentFragment as InboxTabbedFragment
        val sources: List<StatefulData<out InboxViewModel.InboxResult>> = when (args.pageType) {
            InboxViewModel.PageType.All -> {
                listOf(
                    parentFragment.viewModel.replies.value,
                    parentFragment.viewModel.mentions.value,
                    parentFragment.viewModel.messages.value,
                )
            }
            InboxViewModel.PageType.Replies ->
                listOf(
                    parentFragment.viewModel.replies.value,
                )
            InboxViewModel.PageType.Mentions ->
                listOf(
                    parentFragment.viewModel.mentions.value,
                )
            InboxViewModel.PageType.Messages ->
                listOf(
                    parentFragment.viewModel.messages.value,
                )
        }

        val error = sources.firstOrNull { it is StatefulData.Error<*> }
        if (error != null) {
            return StatefulData.Error((error as StatefulData.Error<*>).error)
        }

        val loading = sources.firstOrNull { it is StatefulData.Loading }
        if (loading != null) {
            return StatefulData.Loading()
        }

        val notStarted = sources.firstOrNull { it is StatefulData.NotStarted }
        if (notStarted != null) {
            return StatefulData.NotStarted()
        }

        val data = sources.flatMap {
            it as StatefulData.Success<out InboxViewModel.InboxResult>

            when (it.data) {
                is InboxViewModel.InboxResult.MentionsResult -> {
                    it.data.mentions.map {
                        Item.MentionItem(it)
                    }
                }
                is InboxViewModel.InboxResult.MessagesResult -> {
                    it.data.messages.map {
                        Item.MessageItem(it)
                    }
                }
                is InboxViewModel.InboxResult.RepliesResult -> {
                    it.data.replies.map {
                        Item.ReplyItem(it)
                    }
                }
            }
        }

        return StatefulData.Success(data)
    }

    private fun onUpdate() {
        when (val data = getData()) {
            is StatefulData.Error -> {
                binding.loadingView.showDefaultErrorMessageFor(data.error)
            }
            is StatefulData.Loading -> {
                binding.loadingView.showProgressBar()
            }
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                binding.loadingView.hideAll()
                binding.swipeRefreshLayout.isRefreshing = false

                val sortedData = data.data.sortedByDescending {
                    when (it) {
                        is Item.MentionItem ->
                            dateStringToTs(it.mention.comment.updated ?: it.mention.comment.published)
                        is Item.MessageItem ->
                            dateStringToTs(it.message.private_message.updated ?: it.message.private_message.published)
                        is Item.ReplyItem ->
                            dateStringToTs(it.reply.comment.updated ?: it.reply.comment.published)
                    }
                }

                if (sortedData.isEmpty()) {
                    binding.loadingView.showErrorWithRetry(R.string.there_doesnt_seem_to_be_anything_here)
                } else {
                    (binding.recyclerView.adapter as? InboxItemAdapter)?.data = sortedData
                }
            }
        }
    }

    private sealed interface Item {
        data class ReplyItem (
            val reply: CommentReplyView
        ): Item
        data class MentionItem (
            val mention: PersonMentionView
        ): Item
        data class MessageItem (
            val message: PrivateMessageView
        ): Item
    }

    private class InboxItemAdapter(
        private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
        private val instance: String,
        private val onImageClick: (String) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
    ) : RecyclerView.Adapter<ViewHolder>() {

        var data: List<Item> = listOf()
            set(value) {
                field = value
                refreshItems()
            }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.MentionItem ->
                        old.mention.comment.id == (new as Item.MentionItem).mention.comment.id
                    is Item.MessageItem ->
                        old.message.private_message.id ==
                                (new as Item.MessageItem).message.private_message.id
                    is Item.ReplyItem ->
                        old.reply.comment.id == (new as Item.ReplyItem).reply.comment.id
                }
            }
        ).apply {
            addItemType(Item.MentionItem::class, InboxListItemBinding::inflate) { item, b, h ->
                postAndCommentViewBuilder.bindMessage(
                    b = b,
                    instance = instance,
                    message = item.mention,
                    onImageClick = onImageClick,
                    onPageClick = onPageClick
                )
            }
            addItemType(Item.MessageItem::class, InboxListItemBinding::inflate) { item, b, h ->
                postAndCommentViewBuilder.bindMessage(
                    b = b,
                    instance = instance,
                    message = item.message,
                    onImageClick = onImageClick,
                    onPageClick = onPageClick
                )
            }
            addItemType(Item.ReplyItem::class, InboxListItemBinding::inflate) { item, b, h ->
                postAndCommentViewBuilder.bindMessage(
                    b = b,
                    instance = instance,
                    message = item.reply,
                    onImageClick = onImageClick,
                    onPageClick = onPageClick
                )
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
            adapterHelper.setItems(data, this)
        }

    }
}