package com.idunnololz.summit.feedback

import android.app.Activity
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.DialogFragmentPostFeedbackBinding
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsDialogFragment
import com.idunnololz.summit.editTextToolbar.EditTextToolbarSettingsDialogFragment
import com.idunnololz.summit.editTextToolbar.TextFieldToolbarManager
import com.idunnololz.summit.editTextToolbar.TextFormatToolbarViewHolder
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.UploadImageViewModel
import com.idunnololz.summit.lemmy.comment.AddLinkDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.utils.getFeedbackScreenshotFile
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragment
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragmentArgs
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.DirectoryHelper
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.launchChangelog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PostFeedbackDialogFragment :
    BaseDialogFragment<DialogFragmentPostFeedbackBinding>(),
    FullscreenDialogFragment {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            PostFeedbackDialogFragment()
                .show(fragmentManager, "PostFeedbackDialogFragment")
        }
    }

    private val viewModel: PostFeedbackViewModel by viewModels()
    private val uploadImageViewModel: UploadImageViewModel by viewModels()

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var textFieldToolbarManager: TextFieldToolbarManager

    @Inject
    lateinit var directoryHelper: DirectoryHelper

    private var textFormatterToolbar: TextFormatToolbarViewHolder? = null

    private var isSent: Boolean = false

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    uploadImageViewModel.uploadImage(uri)
                }
            }
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
                viewModel.currentDraftId.value = result.id
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

        setBinding(
            DialogFragmentPostFeedbackBinding.inflate(inflater, container, false),
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            requireMainActivity().apply {
                insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.content)
                insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            }

            toolbar.title = getString(R.string.post_feedback_to_community)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(android.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            viewModel.postFeedbackState.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.hideAll()

                        ErrorDialogFragment.show(
                            getString(R.string.error_unable_to_submit_feedback),
                            it.error,
                            childFragmentManager,
                        )
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        val fm = parentFragmentManager
                        for (i in 0 until fm.backStackEntryCount) {
                            fm.popBackStack()
                        }

                        getMainActivity()?.launchChangelog()
                        dismiss()
                    }
                }
            }

            textFieldToolbarManager.textFieldToolbarSettings.observe(viewLifecycleOwner) {
                binding.bottomBar.removeAllViews()

                val commentEditText = binding.commentEditText

                textFormatterToolbar = textFieldToolbarManager.createTextFormatterToolbar(
                    context,
                    binding.bottomBar,
                )

                textFormatterToolbar?.setupTextFormatterToolbar(
                    editText = commentEditText,
                    referenceTextView = null,
                    lifecycleOwner = viewLifecycleOwner,
                    fragmentManager = childFragmentManager,
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
                            handleBackPress = true,
                            onBackPressedDispatcher = onBackPressedDispatcher,
                        )
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
                                    viewModel.apiInstance,
                                    commentEditText.text.toString(),
                                ).toBundle()
                            }
                            .showAllowingStateLoss(
                                childFragmentManager,
                                "PreviewCommentDialogFragment",
                            )
                    },
                    onDraftsClick = {
                        DraftsDialogFragment.show(childFragmentManager, DraftTypes.Comment)
                    },
                    onSettingsClick = {
                        EditTextToolbarSettingsDialogFragment.show(childFragmentManager)
                    },
                )
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

            val feedbackScreenshotFile = directoryHelper.getFeedbackScreenshotFile()
            post.setOnClickListener {
                viewModel.postFeedbackToChangelogPost(
                    text = commentEditText.text.toString(),
                    screenshot = if (includeScreenshot.isChecked) {
                        feedbackScreenshotFile
                    } else {
                        null
                    },
                )
            }

            if (feedbackScreenshotFile.exists()) {
                includeScreenshot.visibility = View.VISIBLE
                previewScreenshot.visibility = View.VISIBLE

                previewScreenshot.setOnClickListener {
                    getMainActivity()?.openImage(
                        sharedElement = null,
                        appBar = null,
                        title = null,
                        url = Uri.fromFile(feedbackScreenshotFile).toString(),
                        mimeType = null,
                        urlAlt = null,
                        mimeTypeAlt = null,
                    )
                }
            } else {
                includeScreenshot.visibility = View.GONE
                previewScreenshot.visibility = View.GONE
            }

            if (savedInstanceState == null) {
                commentEditText.requestFocus()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        if (!isSent) {
            try {
                if (preferences.saveDraftsAutomatically) {
                    saveDraft()
                }
            } catch (e: IllegalStateException) {
                // do nothing... very rare
            }
        }
    }

    private fun saveDraft(overwriteExistingDraft: Boolean = true) {
        val content = binding.commentEditText.text?.toString()

        val currentDraftId = viewModel.currentDraftId.value

        if (!content.isNullOrBlank()) {
            val account = viewModel.currentAccount.value

            if (currentDraftId != null && overwriteExistingDraft) {
                viewModel.draftsManager.updateDraftAsync(
                    entryId = currentDraftId,
                    draftData = DraftData.CommentDraftData(
                        originalComment = null,
                        postRef = viewModel.changeLogPostRef,
                        parentCommentId = null,
                        content = content,
                        accountId = account?.id ?: 0L,
                        accountInstance = account?.instance ?: "",
                    ),
                    showToast = true,
                )
            } else {
                viewModel.draftsManager.saveDraftAsync(
                    DraftData.CommentDraftData(
                        originalComment = null,
                        postRef = viewModel.changeLogPostRef,
                        parentCommentId = null,
                        content = content,
                        accountId = account?.id ?: 0L,
                        accountInstance = account?.instance ?: "",
                    ),
                    showToast = true,
                )
            }
        }
    }
}
