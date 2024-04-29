package com.idunnololz.summit.lemmy.comment

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import arrow.core.Either
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.ErrorMessageOldReplyTargetBinding
import com.idunnololz.summit.databinding.FragmentAddOrEditCommentBinding
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsDialogFragment
import com.idunnololz.summit.drafts.OriginalCommentData
import com.idunnololz.summit.editTextToolbar.EditTextToolbarSettingsDialogFragment
import com.idunnololz.summit.editTextToolbar.TextFieldToolbarManager
import com.idunnololz.summit.editTextToolbar.TextFormatToolbarViewHolder
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.UploadImageViewModel
import com.idunnololz.summit.lemmy.utils.mentions.MentionsHelper
import com.idunnololz.summit.preferences.GlobalSettings
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragment
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragmentArgs
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.dateStringToPretty
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class AddOrEditCommentFragment :
    BaseDialogFragment<FragmentAddOrEditCommentBinding>(),
    FullscreenDialogFragment,
    SignInNavigator,
    BackPressHandler,
    AlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        const val REQUEST_KEY = "AddOrEditCommentFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        /**
         * @param accountId The account to use for sending the reply/message.
         * Null to use the current account.
         */
        fun showReplyDialog(
            instance: String,
            postOrCommentView: Either<PostView, CommentView>,
            fragmentManager: FragmentManager,
            accountId: Long?,
        ) {
            AddOrEditCommentFragment().apply {
                arguments = AddOrEditCommentFragmentArgs(
                    instance = instance,
                    commentView = postOrCommentView.getOrNull(),
                    postView = postOrCommentView.leftOrNull(),
                    editCommentView = null,
                    accountId = accountId ?: 0L,
                ).toBundle()
            }.showAllowingStateLoss(fragmentManager, "AddOrEditCommentFragment")
        }
    }

    private val args by navArgs<AddOrEditCommentFragmentArgs>()

    private val viewModel: AddOrEditCommentViewModel by viewModels()
    private val uploadImageViewModel: UploadImageViewModel by viewModels()

    private var currentBottomMenu: BottomMenu? = null

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    uploadImageViewModel.uploadImage(uri)
                }
            }
        }

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var textFieldToolbarManager: TextFieldToolbarManager

    @Inject
    lateinit var mentionsHelper: MentionsHelper

    private var textFormatterToolbar: TextFormatToolbarViewHolder? = null

    @Parcelize
    enum class Result : Parcelable {
        CommentSent,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)

        childFragmentManager.setFragmentResultListener(
            AddLinkDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<AddLinkDialogFragment.AddLinkResult>(
                AddLinkDialogFragment.REQUEST_KEY_RESULT,
            )
            if (result != null) {
                textFormatterToolbar?.onLinkAdded(result.text, result.url)
            }
        }
        childFragmentManager.setFragmentResultListener(
            DraftsDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<DraftEntry>(
                DraftsDialogFragment.REQUEST_KEY_RESULT,
            )
            if (result != null) {
                viewModel.currentDraftEntry.value = result
            }
        }
        childFragmentManager.setFragmentResultListener(
            ChooseSavedImageDialogFragment.REQUEST_KEY,
            this,
        ) { key, bundle ->
            val result = bundle.getParcelableCompat<ChooseSavedImageDialogFragment.Result>(
                ChooseSavedImageDialogFragment.REQUEST_RESULT,
            )
            if (result != null) {
                uploadImageViewModel.uploadImage(result.fileUri)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentAddOrEditCommentBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.bottomBar)
        }

        textFieldToolbarManager.textFieldToolbarSettings.observe(viewLifecycleOwner) {
            binding.bottomBar.removeAllViews()

            val commentEditText = binding.commentEditText

            textFormatterToolbar = textFieldToolbarManager.createTextFormatterToolbar(
                context,
                binding.bottomBar,
            )

            textFormatterToolbar?.setupTextFormatterToolbar(
                commentEditText,
                onChooseImageClick = {
                    val bottomMenu = BottomMenu(context).apply {
                        setTitle(R.string.insert_image)
                        addItemWithIcon(
                            R.id.from_camera,
                            R.string.take_a_photo,
                            R.drawable.baseline_photo_camera_24,
                        )
                        addItemWithIcon(
                            R.id.from_gallery,
                            R.string.choose_from_gallery,
                            R.drawable.baseline_image_24,
                        )
                        addItemWithIcon(
                            R.id.from_camera_with_editor,
                            R.string.take_a_photo_with_editor,
                            R.drawable.baseline_photo_camera_24,
                        )
                        addItemWithIcon(
                            R.id.from_gallery_with_editor,
                            R.string.choose_from_gallery_with_editor,
                            R.drawable.baseline_image_24,
                        )
                        addItemWithIcon(
                            R.id.use_a_saved_image,
                            R.string.use_a_saved_image,
                            R.drawable.baseline_save_24,
                        )

                        setOnMenuItemClickListener {
                            when (it.id) {
                                R.id.from_camera -> {
                                    val intent = ImagePicker.with(requireActivity())
                                        .cameraOnly()
                                        .createIntent()
                                    launcher.launch(intent)
                                }

                                R.id.from_gallery -> {
                                    val intent = ImagePicker.with(requireActivity())
                                        .galleryOnly()
                                        .createIntent()
                                    launcher.launch(intent)
                                }

                                R.id.from_camera_with_editor -> {
                                    val intent = ImagePicker.with(requireActivity())
                                        .cameraOnly()
                                        .crop()
                                        .cropFreeStyle()
                                        .createIntent()
                                    launcher.launch(intent)
                                }

                                R.id.from_gallery_with_editor -> {
                                    val intent = ImagePicker.with(requireActivity())
                                        .galleryOnly()
                                        .crop()
                                        .cropFreeStyle()
                                        .createIntent()
                                    launcher.launch(intent)
                                }

                                R.id.use_a_saved_image -> {
                                    ChooseSavedImageDialogFragment()
                                        .apply {
                                            arguments =
                                                ChooseSavedImageDialogFragmentArgs().toBundle()
                                        }
                                        .showAllowingStateLoss(
                                            childFragmentManager,
                                            "ChooseSavedImageDialogFragment",
                                        )
                                }
                            }
                        }
                    }

                    bottomMenu.show(
                        bottomMenuContainer = requireMainActivity(),
                        bottomSheetContainer = binding.root,
                        expandFully = true,
                        handleBackPress = false,
                    )

                    currentBottomMenu = bottomMenu
                },
                onAddLinkClick = {
                    AddLinkDialogFragment.show(
                        binding.commentEditText.getSelectedText(),
                        childFragmentManager,
                    )
                },
                onPreviewClick = {
                    PreviewCommentDialogFragment()
                        .apply {
                            arguments = PreviewCommentDialogFragmentArgs(
                                args.instance,
                                commentEditText.text.toString(),
                            ).toBundle()
                        }
                        .showAllowingStateLoss(childFragmentManager, "AA")
                },
                onDraftsClick = {
                    DraftsDialogFragment.show(childFragmentManager, DraftTypes.Comment)
                },
                onSettingsClick = {
                    EditTextToolbarSettingsDialogFragment.show(childFragmentManager)
                },
            )
        }

        viewModel.currentAccount.observe(viewLifecycleOwner) {
            if (it != null) {
                requireActivity().invalidateOptionsMenu()
            }
        }

        viewModel.commentSentEvent.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    viewModel.commentSentEvent.setIdle()

                    ErrorDialogFragment.show(
                        getString(R.string.error_unable_to_send_message),
                        it.error,
                        childFragmentManager,
                    )
                }
                is StatefulData.Loading -> {}
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    viewModel.commentSentEvent.setIdle()

                    setFragmentResult(
                        requestKey = REQUEST_KEY,
                        result = bundleOf(REQUEST_KEY_RESULT to Result.CommentSent),
                    )

                    dismiss()
                }
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) {
            refreshMessage()
        }

        binding.toolbar.inflateMenu(R.menu.menu_add_or_edit_comment)

        if (isDm) {
            binding.toolbar.title = getString(R.string.send_message)
        } else {
            binding.toolbar.title = getString(R.string.comment)
        }
        if (isEdit()) {
            binding.toolbar.menu.findItem(R.id.send_comment)?.isVisible = false
        } else {
            binding.toolbar.menu.findItem(R.id.update_comment)?.isVisible = false
        }
        binding.toolbar.setNavigationIcon(R.drawable.baseline_close_24)
        binding.toolbar.setNavigationIconTint(
            context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
        )
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        binding.toolbar.setOnMenuItemClickListener a@{
            when (it.itemId) {
                R.id.send_comment -> {
                    if (uploadImageViewModel.isUploading) {
                        AlertDialogFragment.Builder()
                            .setMessage(R.string.warn_upload_in_progress)
                            .setPositiveButton(R.string.proceed_anyways)
                            .setNegativeButton(R.string.cancel)
                            .createAndShow(this, "send_comment")
                        return@a true
                    }
                    sendComment()
                    true
                }
                R.id.update_comment -> {
                    if (uploadImageViewModel.isUploading) {
                        AlertDialogFragment.Builder()
                            .setMessage(R.string.warn_upload_in_progress)
                            .setPositiveButton(R.string.proceed_anyways)
                            .setNegativeButton(R.string.cancel)
                            .createAndShow(this, "update_comment")
                        return@a true
                    }
                    updateComment()
                    true
                }
                R.id.save_draft -> {
                    saveDraft(overwriteExistingDraft = false)
                    true
                }
                R.id.drafts -> {
                    DraftsDialogFragment.show(childFragmentManager, DraftTypes.Comment)
                    true
                }
                else -> false
            }
        }

        mentionsHelper.installMentionsSupportOn(viewLifecycleOwner, binding.commentEditText)

        if (savedInstanceState == null) {
            val replyTargetTs =
                args.commentView?.let {
                    dateStringToTs(it.comment.updated ?: it.comment.published)
                } ?: args.postView?.let {
                    dateStringToTs(it.post.updated ?: it.post.published)
                }
            val thresholdMs = GlobalSettings.warnReplyToOldContentThresholdMs ?: Long.MAX_VALUE

            if (replyTargetTs != null &&
                (System.currentTimeMillis() - replyTargetTs) > thresholdMs
            ) {
                viewModel.addMessage(
                    AddOrEditCommentViewModel.Message.ReplyTargetTooOld(replyTargetTs),
                )
            }

            binding.commentEditText.requestFocus()
        }
        setup(savedInstanceState)
    }

    private fun updateComment() {
        val accountId = accountId
        val account = if (accountId == null) {
            viewModel.currentAccount.value
        } else {
            viewModel.accountManager.getAccountByIdBlocking(accountId)
        }

        if (account == null) {
            PreAuthDialogFragment()
                .showAllowingStateLoss(childFragmentManager, "DF")
            return
        }

        viewModel.updateComment(
            account,
            PostRef(
                args.instance,
                requireNotNull(args.editCommentView?.post?.id) {
                    "editCommentView were null!"
                },
            ),
            requireNotNull(args.editCommentView?.comment?.id),
            binding.commentEditor.editText?.text.toString(),
        )
    }

    private fun sendComment() {
        val accountId = accountId
        val account = if (accountId == null) {
            viewModel.currentAccount.value
        } else {
            viewModel.accountManager.getAccountByIdBlocking(accountId)
        }

        if (account == null) {
            PreAuthDialogFragment()
                .showAllowingStateLoss(childFragmentManager, "AS")
            return
        }

        val inboxItem = args.inboxItem
        val personId = args.personId
        val personRef = args.personRef
        if (personId != 0L) {
            viewModel.sendComment(
                account,
                personId,
                binding.commentEditor.editText?.text.toString(),
            )
        } else if (personRef != null) {
            viewModel.sendComment(
                account,
                personRef,
                binding.commentEditor.editText?.text.toString(),
            )
        } else if (inboxItem != null) {
            viewModel.sendComment(
                account,
                args.instance,
                inboxItem,
                binding.commentEditor.editText?.text.toString(),
            )
        } else {
            viewModel.sendComment(
                account,
                PostRef(
                    args.instance,
                    requireNotNull(
                        args.postView?.post?.id ?: args.commentView?.post?.id,
                    ) {
                        "Both postView and commentView were null!"
                    },
                ),
                args.commentView?.comment?.id,
                binding.commentEditor.editText?.text.toString(),
            )
        }
    }

    private fun refreshMessage() {
        if (!isBindingAvailable()) {
            return
        }

        TransitionManager.beginDelayedTransition(binding.root)

        val context = requireContext()
        val messageView = when (val message = viewModel.messages.value?.firstOrNull()) {
            is AddOrEditCommentViewModel.Message.ReplyTargetTooOld -> {
                val b = ErrorMessageOldReplyTargetBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    binding.messageContainer,
                    false,
                )
                b.message.text = getString(
                    R.string.error_retry_target_too_old_format,
                    dateStringToPretty(context, message.replyTargetTs),
                )
                b.close.setOnClickListener {
                    viewModel.dismissMessage(message)
                }
                b.root
            }
            null -> null
        }
        binding.messageContainer.removeAllViews()

        if (messageView != null) {
            binding.messageContainer.addView(messageView)
        }
    }

    private fun setup(savedInstanceState: Bundle?) {
        if (!isBindingAvailable()) {
            return
        }

        val postView = args.postView
        val commentView = args.commentView
        val inboxItem = args.inboxItem

        val commentEditor = binding.commentEditor
        if (isEdit()) {
            val commentToEdit = requireNotNull(args.editCommentView)
            binding.scrollView.visibility = View.GONE
            binding.divider.visibility = View.GONE

            if (savedInstanceState == null) {
                commentEditor.editText?.setText(commentToEdit.comment.content)
            }
        } else if (commentView != null) {
            binding.replyingTo.text = commentView.comment.content
        } else if (postView != null) {
            binding.replyingTo.text = SpannableStringBuilder().apply {
                append(postView.post.name, StyleSpan(Typeface.BOLD), 0)
                if (!postView.post.body.isNullOrBlank()) {
                    appendLine()
                    appendLine()
                    appendLine(postView.post.body)
                }
            }
        } else if (inboxItem != null) {
            binding.replyingTo.text = inboxItem.content
        } else if (isDm) {
            binding.scrollView.visibility = View.GONE
            binding.divider.visibility = View.GONE
        } else {
            dismiss()
            return
        }
        uploadImageViewModel.uploadImageResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()

                    ErrorDialogFragment.show(
                        getString(R.string.error_unable_to_upload_image),
                        it.error,
                        childFragmentManager,
                    )
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingView.hideAll()
                    uploadImageViewModel.uploadImageResult.clear()

                    textFormatterToolbar?.onImageUploaded(it.data.url)
                }
            }
        }
        viewModel.currentDraftEntry.observe(viewLifecycleOwner) {
            if (it == null) return@observe

            val data = it.data
            if (data is DraftData.CommentDraftData) {
                binding.commentEditText.setText(data.content)
                binding.commentEditText.setSelection(binding.commentEditText.length())
            }

            viewModel.currentDraftEntry.postValue(null)
        }
    }

    private fun isEdit(): Boolean {
        return args.editCommentView != null
    }

    override fun navigateToSignInScreen() {
        (parentFragment as? SignInNavigator)?.navigateToSignInScreen()
        dismiss()
    }

    override fun proceedAnyways(tag: Int) {
    }

    override fun onBackPressed(): Boolean {
        if (isBindingAvailable()) {
            if (currentBottomMenu?.close() == true) {
                currentBottomMenu = null
                return true
            }
        }

        try {
            if (preferences.saveDraftsAutomatically) {
                saveDraft()
            }

            dismiss()
        } catch (e: IllegalStateException) {
            // do nothing... very rare
        }
        return true
    }

    private fun saveDraft(overwriteExistingDraft: Boolean = true) {
        val content = binding.commentEditText.text?.toString()

        val currentDraftEntry = viewModel.currentDraftEntry.value
        if (!content.isNullOrBlank()) {
            if (currentDraftEntry?.data != null &&
                currentDraftEntry.data is DraftData.CommentDraftData &&
                overwriteExistingDraft
            ) {
                viewModel.draftsManager.updateDraftAsync(
                    currentDraftEntry.id,
                    currentDraftEntry.data.copy(
                        content = content,
                    ),
                    showToast = true,
                )
            } else {
                val account = viewModel.currentAccount.value
                viewModel.draftsManager.saveDraftAsync(
                    DraftData.CommentDraftData(
                        originalComment = args.editCommentView?.toOriginalCommentData(),
                        postRef = PostRef(
                            args.instance,
                            args.postView?.post?.id
                                ?: args.commentView?.post?.id
                                ?: args.editCommentView?.post?.id ?: 0,
                        ),
                        parentCommentId = args.commentView?.comment?.id,
                        content = content,
                        accountId = account?.id ?: 0L,
                        accountInstance = account?.instance ?: "",
                    ),
                    showToast = true,
                )
            }
        }
    }

    private fun CommentView.toOriginalCommentData(): OriginalCommentData = OriginalCommentData(
        postRef = PostRef(args.instance, this.post.id),
        commentId = this.comment.id,
        content = this.comment.content,
        parentCommentId = args.commentView?.comment?.id,
    )

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        when (tag) {
            "send_comment" -> {
                sendComment()
            }
            "update_comment" -> {
                updateComment()
            }
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {
    }

    private val isDm: Boolean
        get() = args.personId != 0L || args.personRef != null

    private val accountId
        get() =
            if (args.accountId != 0L) {
                args.accountId
            } else {
                null
            }
}
