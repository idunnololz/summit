package com.idunnololz.summit.actions.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.api.LemmyApiClient
import com.idunnololz.summit.databinding.ActionDetailsItemFooterBinding
import com.idunnololz.summit.databinding.ActionDetailsItemHeaderBinding
import com.idunnololz.summit.databinding.ActionDetailsItemRichTextFieldBinding
import com.idunnololz.summit.databinding.ActionDetailsItemTextFieldBinding
import com.idunnololz.summit.databinding.DialogFragmentActionDetailsBinding
import com.idunnololz.summit.lemmy.CommentRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getColorWithAlpha
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showMoreLinkOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActionDetailsFragment :
    BaseFragment<DialogFragmentActionDetailsBinding>(),
    FullscreenDialogFragment {

    private val args: ActionDetailsFragmentArgs by navArgs()
    private val viewModel: ActionDetailsViewModel by viewModels()

    @Inject
    lateinit var apiClient: LemmyApiClient

    @Inject
    lateinit var lemmyTextHelper: LemmyTextHelper

    private val runActionAgainDialogLauncher = newAlertDialogLauncher("run_action_again") {
        if (it.isOk) {
            viewModel.retryAction(args.action)
        }
    }
    private val actionRunSuccessDialogLauncher = newAlertDialogLauncher("run_action_success") {
        if (it.isOk) {
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentActionDetailsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            requireMainActivity().apply {
                insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            }
            toolbar.apply {
                setNavigationIcon(R.drawable.baseline_close_24)
                setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
                setNavigationIconTint(
                    context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
                )
            }

            val context = requireContext()
            val action = args.action

            val adapter = ActionDetailsAdapter(
                context = context,
                currentInstance = apiClient.instance,
                lemmyTextHelper = lemmyTextHelper,
                onImageClick = { url ->
                    getMainActivity()?.openImage(
                        sharedElement = null,
                        appBar = null,
                        title = null,
                        url = url,
                        mimeType = null,
                    )
                },
                onVideoClick = { url ->
                    getMainActivity()?.openVideo(url, VideoType.Unknown, null)
                },
                onPageClick = {
                    getMainActivity()?.launchPage(it)
                },
                onLinkClick = { url, text, linkType ->
                    onLinkClick(url, text, linkType)
                },
                onLinkLongClick = { url, text ->
                    getMainActivity()?.showMoreLinkOptions(url, text)
                },
                onRetryClick = {
                    viewModel.retryAction(args.action)
                },
                onRunActionClick = {
                    runActionAgainDialogLauncher.launchDialog {
                        titleResId = R.string.warn_run_action_again_title
                        messageResId = R.string.warn_run_action_again_desc
                        positionButtonResId = R.string.run_action
                        negativeButtonResId = R.string.cancel
                    }
                },
                dismissAction = {
                    viewModel.deleteAction(args.action)
                },
                onViewRawClick = {
                    PreviewCommentDialogFragment()
                        .apply {
                            arguments = PreviewCommentDialogFragmentArgs(
                                "",
                                it,
                                true,
                            ).toBundle()
                        }
                        .showAllowingStateLoss(childFragmentManager, "PreviewCommentDialogFragment")
                },
            )

            viewModel.retryActionResult.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.hideAll()
                        adapter.disableActions = false
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                        adapter.disableActions = true
                    }
                    is StatefulData.NotStarted -> {
                        loadingView.hideAll()
                        adapter.disableActions = false
                    }
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                        adapter.disableActions = false

                        when (args.action.details) {
                            is ActionDetails.FailureDetails -> {
                                actionRunSuccessDialogLauncher.launchDialog {
                                    messageResId = R.string.retry_action_success_desc
                                    positionButtonResId = android.R.string.ok
                                }
                            }
                            ActionDetails.PendingDetails -> {}
                            ActionDetails.SuccessDetails -> {
                                actionRunSuccessDialogLauncher.launchDialog {
                                    messageResId = R.string.rerun_action_success_desc
                                    positionButtonResId = android.R.string.ok
                                }
                            }
                        }
                    }
                }
            }

            viewModel.deleteActionResult.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.hideAll()
                        adapter.disableActions = false
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                        adapter.disableActions = true
                    }
                    is StatefulData.NotStarted -> {
                        loadingView.hideAll()
                        adapter.disableActions = false
                    }
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                        adapter.disableActions = false

                        findNavController().popBackStack()
                    }
                }
            }

            adapter.setData(action)

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter

            if (!args.action.seen) {
                viewModel.markActionAsSeen(args.action)
            }
        }
    }

    private class ActionDetailsAdapter(
        private val context: Context,
        private val currentInstance: String,
        private val lemmyTextHelper: LemmyTextHelper,
        private val onImageClick: (url: String) -> Unit,
        private val onVideoClick: (url: String) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        private val onLinkLongClick: (url: String, text: String) -> Unit,
        private val onRetryClick: () -> Unit,
        private val onRunActionClick: () -> Unit,
        private val dismissAction: () -> Unit,
        private val onViewRawClick: (String) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {

            class HeaderItem(
                val actionDetails: ActionDetails,
                val title: String,
            ) : Item

            class TextFieldItem(
                val title: String,
                val value: String,
                val pageRef: PageRef?,
            ) : Item

            class RichTextFieldItem(
                val title: String,
                val value: String,
                val pageRef: PageRef?,
            ) : Item

            class FooterItem(
                val actionDetails: ActionDetails,
                val enableActions: Boolean,
            ) : Item
        }

        var disableActions: Boolean = false
            set(value) {
                field = value

                generateItems()
            }

        private var data: Action? = null
        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.HeaderItem ->
                        old.actionDetails == (new as Item.HeaderItem).actionDetails
                    is Item.TextFieldItem ->
                        old.title == (new as Item.TextFieldItem).title
                    is Item.RichTextFieldItem ->
                        old.title == (new as Item.RichTextFieldItem).title
                    is Item.FooterItem ->
                        true
                }
            },
        ).apply {
            addItemType(
                clazz = Item.HeaderItem::class,
                inflateFn = ActionDetailsItemHeaderBinding::inflate,
            ) { item, b, h ->
                val details = item.actionDetails
                when (details) {
                    is ActionDetails.FailureDetails -> {
                        b.status.chipBackgroundColor =
                            ColorStateList.valueOf(
                                getColorWithAlpha(
                                    yourColor = context.getColorCompat(R.color.style_red),
                                    alpha = 40,
                                ),
                            )
                        b.status.text = context.getString(R.string.failed)
                    }
                    ActionDetails.PendingDetails -> {
                        b.status.chipBackgroundColor =
                            ColorStateList.valueOf(
                                getColorWithAlpha(
                                    yourColor = context.getColorCompat(R.color.style_blue),
                                    alpha = 40,
                                ),
                            )
                        b.status.text = context.getString(R.string.pending)
                    }
                    ActionDetails.SuccessDetails -> {
                        b.status.chipBackgroundColor =
                            ColorStateList.valueOf(
                                getColorWithAlpha(
                                    yourColor = context.getColorCompat(R.color.style_green),
                                    alpha = 40,
                                ),
                            )
                        b.status.text = context.getString(R.string.success)
                    }
                }

                b.title.text = item.title
            }
            addItemType(
                clazz = Item.TextFieldItem::class,
                inflateFn = ActionDetailsItemTextFieldBinding::inflate,
            ) { item, b, h ->
                b.label.text = item.title

                if (item.pageRef == null) {
                    b.value.setTextColor(
                        context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorOnSurface,
                        ),
                    )
                    lemmyTextHelper.bindText(
                        textView = b.value,
                        text = item.value,
                        instance = currentInstance,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onPageClick = onPageClick,
                        onLinkClick = onLinkClick,
                        onLinkLongClick = onLinkLongClick,
                    )
                } else {
                    b.value.text = item.value
                    b.value.setTextColor(
                        context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorPrimary,
                        ),
                    )
                    b.root.setOnClickListener {
                        onPageClick(item.pageRef)
                    }
                }
            }
            addItemType(
                clazz = Item.RichTextFieldItem::class,
                inflateFn = ActionDetailsItemRichTextFieldBinding::inflate,
            ) { item, b, h ->
                b.label.text = item.title

                if (item.pageRef == null) {
                    lemmyTextHelper.bindText(
                        textView = b.value,
                        text = item.value,
                        instance = currentInstance,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onPageClick = onPageClick,
                        onLinkClick = onLinkClick,
                        onLinkLongClick = onLinkLongClick,
                    )
                } else {
                    b.value.text = item.value
                    b.root.setOnClickListener {
                        onPageClick(item.pageRef)
                    }
                }

                b.viewRaw.setOnClickListener {
                    onViewRawClick(item.value)
                }
            }
            addItemType(
                clazz = Item.FooterItem::class,
                inflateFn = ActionDetailsItemFooterBinding::inflate,
            ) { item, b, h ->
                b.dismissAction.setOnClickListener {
                    dismissAction()
                }

                b.retry.isEnabled = item.enableActions
                b.dismissAction.isEnabled = item.enableActions

                when (item.actionDetails) {
                    is ActionDetails.FailureDetails -> {
                        b.retry.visibility = View.VISIBLE
                        b.retry.text = context.getString(R.string.retry)
                        b.retry.setOnClickListener {
                            onRetryClick()
                        }
                    }
                    ActionDetails.PendingDetails -> {
                        b.retry.visibility = View.GONE
                    }
                    ActionDetails.SuccessDetails -> {
                        b.retry.visibility = View.VISIBLE
                        b.retry.text = context.getString(R.string.run_action)
                        b.retry.setOnClickListener {
                            onRunActionClick()
                        }
                    }
                }
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setData(data: Action) {
            this.data = data
            generateItems()
        }

        private fun generateItems() {
            val data = this.data ?: return
            val items = mutableListOf<Item>()
            val info = data.info

            items.add(
                Item.HeaderItem(
                    actionDetails = data.details,
                    title = info?.getActionName(context)
                        ?: context.getString(R.string.unknown),
                ),
            )
            items.add(
                Item.TextFieldItem(
                    title = context.getString(R.string.account),
                    value =
                    if (info?.accountInstance == null) {
                        "${info?.accountId}"
                    } else {
                        "${info.accountId}@${info.accountInstance}"
                    },
                    pageRef = if (info?.accountInstance != null &&
                        info.accountId != null &&
                        info.accountInstance != null
                    ) {
                        PersonRef.PersonRefById(
                            requireNotNull(info.accountId),
                            requireNotNull(info.accountInstance),
                        )
                    } else {
                        null
                    },
                ),
            )

            when (info) {
                is ActionInfo.CommentActionInfo -> {
                    items.add(
                        Item.TextFieldItem(
                            title = context.getString(R.string.post),
                            value = "${info.postRef.id}@${info.postRef.instance}",
                            pageRef = info.postRef,
                        ),
                    )
                    if (info.parentId != null) {
                        items.add(
                            Item.TextFieldItem(
                                title = context.getString(R.string.comment),
                                value = "${info.parentId}@${info.postRef.instance}",
                                pageRef = CommentRef(info.postRef.instance, info.parentId),
                            ),
                        )
                    }
                    items.add(
                        Item.RichTextFieldItem(
                            title = context.getString(R.string.comment_content),
                            value = info.content,
                            pageRef = null,
                        ),
                    )
                }
                is ActionInfo.DeleteCommentActionInfo -> {
                    items.add(
                        Item.TextFieldItem(
                            title = context.getString(R.string.object_reference),
                            value = "${info.postRef.id}@${info.postRef.instance}",
                            pageRef = info.postRef,
                        ),
                    )
                }
                is ActionInfo.EditCommentActionInfo -> {
                    items.add(
                        Item.TextFieldItem(
                            title = context.getString(R.string.object_reference),
                            value = "${info.postRef.id}@${info.postRef.instance}",
                            pageRef = info.postRef,
                        ),
                    )
                }
                is ActionInfo.MarkPostAsReadActionInfo -> {
                    items.add(
                        Item.TextFieldItem(
                            title = context.getString(R.string.object_reference),
                            value = "${info.postRef.id}@${info.postRef.instance}",
                            pageRef = info.postRef,
                        ),
                    )
                }
                is ActionInfo.VoteActionInfo -> {
                    items.add(
                        Item.TextFieldItem(
                            title = context.getString(R.string.object_reference),
                            value = when (info.ref) {
                                is VotableRef.CommentRef -> {
                                    "${info.ref.commentId}@${info.instance}"
                                }
                                is VotableRef.PostRef -> {
                                    "${info.ref.postId}@${info.instance}"
                                }
                            },
                            pageRef = when (info.ref) {
                                is VotableRef.CommentRef -> {
                                    CommentRef(info.instance, info.ref.commentId)
                                }
                                is VotableRef.PostRef -> {
                                    CommentRef(info.instance, info.ref.postId)
                                }
                            },
                        ),
                    )
                }
                null -> {}
            }

            items.add(
                Item.FooterItem(
                    actionDetails = data.details,
                    enableActions = !disableActions,
                ),
            )

            adapterHelper.setItems(items, this)
        }
    }
}
