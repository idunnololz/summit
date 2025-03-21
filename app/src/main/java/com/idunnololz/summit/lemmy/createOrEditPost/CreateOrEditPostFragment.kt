package com.idunnololz.summit.lemmy.createOrEditPost

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import android.webkit.URLUtil
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentCreateOrEditPostBinding
import com.idunnololz.summit.drafts.DraftData
import com.idunnololz.summit.drafts.DraftEntry
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsDialogFragment
import com.idunnololz.summit.drafts.OriginalPostData
import com.idunnololz.summit.editTextToolbar.EditTextToolbarSettingsDialogFragment
import com.idunnololz.summit.editTextToolbar.TextFieldToolbarManager
import com.idunnololz.summit.editTextToolbar.TextFormatToolbarViewHolder
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.UploadImageViewModel
import com.idunnololz.summit.lemmy.comment.AddLinkDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.setMarkdown
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.mentions.MentionsHelper
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragment
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragmentArgs
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewAutomaticallyByMargins
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreateOrEditPostFragment :
    BaseDialogFragment<FragmentCreateOrEditPostBinding>(),
    FullscreenDialogFragment,
    OldAlertDialogFragment.AlertDialogFragmentListener {

    companion object {
        const val REQUEST_KEY = "CreateOrEditPostFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"
    }

    private val args by navArgs<CreateOrEditPostFragmentArgs>()

    private val viewModel: CreateOrEditPostViewModel by viewModels()
    private val uploadImageViewModel: UploadImageViewModel by viewModels()

    private var adapter: CommunitySearchResultsAdapter? = null

    @Inject
    lateinit var textFieldToolbarManager: TextFieldToolbarManager

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var mentionsHelper: MentionsHelper

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    @Inject
    lateinit var avatarHelper: AvatarHelper

    @Inject
    lateinit var lemmyTextHelper: LemmyTextHelper

    private var textFormatToolbar: TextFormatToolbarViewHolder? = null

    private val floatingLocation = Point()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    uploadImageViewModel.uploadImage(uri)
                }
            }
        }

    private val launcherForUrl =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    uploadImageViewModel.uploadImageForUrl(uri)
                }
            }
        }

    private val showSearchBackPressedHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.showSearch.value = false
        }
    }

    private var isSent: Boolean = false

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
                textFormatToolbar?.onLinkAdded(result.text, result.url)
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
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<ChooseSavedImageDialogFragment.Result>(
                ChooseSavedImageDialogFragment.REQUEST_RESULT,
            )
            if (result != null) {
                uploadImageViewModel.uploadImage(result.fileUri)
            }
        }
        childFragmentManager.setFragmentResultListener(
            "for_link",
            this,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<ChooseSavedImageDialogFragment.Result>(
                ChooseSavedImageDialogFragment.REQUEST_RESULT,
            )
            if (result != null) {
                uploadImageViewModel.uploadImageForUrl(result.fileUri)
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

        setBinding(FragmentCreateOrEditPostBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            requireMainActivity().apply {
                insetViewAutomaticallyByMargins(viewLifecycleOwner, contentOuter)
            }

            toolbar.inflateMenu(R.menu.menu_create_or_edit_post)

            if (isEdit()) {
                toolbar.title = getString(R.string.edit_post)
                toolbar.menu.findItem(R.id.create_post)?.isVisible = false
            } else {
                toolbar.title = getString(R.string.create_post)
                toolbar.menu.findItem(R.id.update_post)?.isVisible = false
            }
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            toolbar.setOnMenuItemClickListener a@{
                when (it.itemId) {
                    R.id.create_post -> {
                        if (uploadImageViewModel.isUploading) {
                            OldAlertDialogFragment.Builder()
                                .setMessage(R.string.warn_upload_in_progress)
                                .setPositiveButton(R.string.proceed_anyways)
                                .setNegativeButton(R.string.cancel)
                                .createAndShow(this@CreateOrEditPostFragment, "create_post")
                            return@a true
                        }

                        createPost()

                        if (preferences.hapticsOnActions) {
                            toolbar.performHapticFeedbackCompat(
                                HapticFeedbackConstantsCompat.CONFIRM,
                            )
                        }
                        true
                    }
                    R.id.update_post -> {
                        if (uploadImageViewModel.isUploading) {
                            OldAlertDialogFragment.Builder()
                                .setMessage(R.string.warn_upload_in_progress)
                                .setPositiveButton(R.string.proceed_anyways)
                                .setNegativeButton(R.string.cancel)
                                .createAndShow(this@CreateOrEditPostFragment, "update_post")
                            return@a true
                        }

                        updatePost()

                        if (preferences.hapticsOnActions) {
                            toolbar.performHapticFeedbackCompat(
                                HapticFeedbackConstantsCompat.CONFIRM,
                            )
                        }
                        true
                    }
                    R.id.save_draft -> {
                        saveDraft(overwriteExistingDraft = false)
                        true
                    }
                    R.id.drafts -> {
                        DraftsDialogFragment.show(childFragmentManager, DraftTypes.Post)
                        true
                    }
                    else -> false
                }
            }

            url.editText?.doOnTextChanged { text, _, _, _ ->
                viewModel.setUrl(text.toString())
            }

            mentionsHelper.installMentionsSupportOn(viewLifecycleOwner, binding.postEditText)
            postEditText.addTextChangedListener {
                root.postDelayed(
                    {
                        onScrollUpdated()
                    },
                    10,
                )
            }
            titleEditText.addTextChangedListener {
                root.postDelayed(
                    {
                        onScrollUpdated()
                    },
                    10,
                )
            }
            urlEditText.addTextChangedListener {
                root.postDelayed(
                    {
                        onScrollUpdated()
                    },
                    10,
                )
            }

            textFieldToolbarManager.textFieldToolbarSettings.observe(viewLifecycleOwner) {
                postBodyToolbar.removeAllViews()

                textFormatToolbar = textFieldToolbarManager.createTextFormatterToolbar(
                    context,
                    binding.postBodyToolbar,
                )

                val postEditor = postEditor
                textFormatToolbar?.setupTextFormatterToolbar(
                    editText = requireNotNull(postEditor.editText),
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
                                                arguments = ChooseSavedImageDialogFragmentArgs().toBundle()
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
                    },
                    onAddLinkClick = {
                        AddLinkDialogFragment.show(
                            binding.postEditText.getSelectedText(),
                            childFragmentManager,
                        )
                    },
                    onPreviewClick = {
                        val postStr = buildString {
                            appendLine("## ${binding.titleEditText.text}")
                            appendLine()
                            appendLine("![](${binding.urlEditText.text})")
                            appendLine()
                            appendLine(postEditor.editText?.text.toString())
                        }
                        PreviewCommentDialogFragment()
                            .apply {
                                arguments = PreviewCommentDialogFragmentArgs(
                                    args.instance,
                                    postStr,
                                ).toBundle()
                            }
                            .showAllowingStateLoss(childFragmentManager, "AA")
                    },
                    onDraftsClick = {
                        DraftsDialogFragment.show(childFragmentManager, DraftTypes.Post)
                    },
                    onSettingsClick = {
                        EditTextToolbarSettingsDialogFragment.show(childFragmentManager)
                    },
                )
            }

            loadingView.hideAll()

            url.setEndIconOnClickListener {
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
                                launcherForUrl.launch(intent)
                            }
                            R.id.from_gallery -> {
                                val intent = ImagePicker.with(requireActivity())
                                    .galleryOnly()
                                    .createIntent()
                                launcherForUrl.launch(intent)
                            }
                            R.id.from_camera_with_editor -> {
                                val intent = ImagePicker.with(requireActivity())
                                    .cameraOnly()
                                    .crop()
                                    .cropFreeStyle()
                                    .createIntent()
                                launcherForUrl.launch(intent)
                            }
                            R.id.from_gallery_with_editor -> {
                                val intent = ImagePicker.with(requireActivity())
                                    .galleryOnly()
                                    .crop()
                                    .cropFreeStyle()
                                    .createIntent()
                                launcherForUrl.launch(intent)
                            }
                            R.id.use_a_saved_image -> {
                                ChooseSavedImageDialogFragment()
                                    .apply {
                                        arguments = ChooseSavedImageDialogFragmentArgs(
                                            "for_link",
                                        ).toBundle()
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
                    bottomSheetContainer = root,
                    expandFully = true,
                    handleBackPress = true,
                    onBackPressedDispatcher = onBackPressedDispatcher,
                )
            }
            viewModel.createOrEditPostResult.observe(viewLifecycleOwner) {
                updateEnableState()

                when (it) {
                    is StatefulData.Error -> {
                        loadingView.hideAll()
                        if (it.error is CreateOrEditPostViewModel.NoTitleError) {
                            title.error = getString(R.string.required)
                        } else {
                            OldAlertDialogFragment.Builder()
                                .setMessage(
                                    getString(
                                        R.string.error_unable_to_send_post,
                                        it.error::class.qualifiedName,
                                        it.error.message,
                                    ),
                                )
                                .createAndShow(childFragmentManager, "ASDS")
                        }
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(
                                REQUEST_KEY_RESULT to it.data,
                            ),
                        )

                        isSent = true
                        dismiss()
                    }
                }
            }
            uploadImageViewModel.uploadImageResult.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.hideAll()

                        ErrorDialogFragment.show(
                            getString(R.string.error_unable_to_upload_image),
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
                        uploadImageViewModel.uploadImageResult.clear()

                        textFormatToolbar?.onImageUploaded(it.data.url)
                    }
                }
            }
            uploadImageViewModel.uploadImageForUrlResult.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        loadingView.hideAll()

                        ErrorDialogFragment.show(
                            getString(R.string.error_unable_to_upload_image),
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
                        uploadImageViewModel.uploadImageResult.clear()

                        url.editText?.setText(it.data.url)
                    }
                }
            }
            viewModel.linkMetadata.observe(viewLifecycleOwner) {
                onLinkMetadataChanged()
            }

            scrollView.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
                    onScrollUpdated()
                },
            )

            getMainActivity()?.insets?.observe(viewLifecycleOwner) { insets ->
                val isImeOpen = (insets?.imeHeight ?: 0) > 0

                root.post {
                    onImeChange(isImeOpen)
                }
            }
            root.viewTreeObserver.addOnPreDrawListener(
                object : OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        root.viewTreeObserver.removeOnPreDrawListener(this)

                        postBodyToolbarPlaceholder.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            height = postBodyToolbar.height
                        }
                        postBodyToolbarPlaceholder2.updateLayoutParams<LinearLayout.LayoutParams> {
                            height = postBodyToolbar.height
                        }

                        root.post {
                            onScrollUpdated()
                        }

                        return false // discard frame
                    }
                },
            )

            communityEditText.setOnClickListener {
                viewModel.showSearch.value = true
            }

            communityEditText.onFocusChangeListener = View.OnFocusChangeListener { _, _ ->
                viewModel.showSearch.value = communityEditText.hasFocus() &&
                    !communityEditText.text.isNullOrBlank()
            }

            adapter = CommunitySearchResultsAdapter(
                context,
                offlineManager,
                avatarHelper,
                onCommunitySelected = {
                    communityEditText.setText(it.community.toCommunityRef().fullName)
                    viewModel.showSearch.value = false
                },
            )
            communitySuggestionsRecyclerView.apply {
                adapter = this@CreateOrEditPostFragment.adapter
                setHasFixedSize(true)
                setup(animationsHelper)
                layoutManager = LinearLayoutManager(context)
            }

            viewModel.searchResults.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        adapter?.setQueryServerResults(listOf())
                    }
                    is StatefulData.Loading -> {
                        adapter?.setQueryServerResultsInProgress()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        adapter?.setQueryServerResults(it.data)
                    }
                }
            }

            viewModel.showSearchLiveData.observe(viewLifecycleOwner) { showSearch ->
                if (showSearch) {
                    showSearch()
                } else {
                    hideSearch()
                }
                updateToolbar()

                showSearchBackPressedHandler.isEnabled = showSearch
            }

            communityEditText.addTextChangedListener {
                val query = it?.toString() ?: ""
                adapter?.setQuery(query) {
                    communitySuggestionsRecyclerView.scrollToPosition(0)
                }
                viewModel.query.value = query.split("@").firstOrNull() ?: ""

                if (communityEditText.hasFocus()) {
                    viewModel.showSearch.value = query.isNotBlank()
                }
            }
            viewModel.currentDraftEntry.observe(viewLifecycleOwner) {
                if (it == null) return@observe

                val data = it.data
                if (data is DraftData.PostDraftData) {
                    titleEditText.setText(data.name)
                    postEditText.setText(data.body)
                    urlEditText.setText(data.url)
                    nsfwSwitch.isChecked = data.isNsfw
                    communityEditText.setText(data.targetCommunityFullName)
                }

                viewModel.currentDraftEntry.postValue(null)
            }

            if (savedInstanceState == null && !viewModel.postPrefilled) {
                viewModel.postPrefilled = true

                if (args.communityName != null) {
                    communityEditText.setText(
                        CommunityRef.CommunityRefByName(
                            name = requireNotNull(args.communityName),
                            instance = args.instance,
                        ).fullName,
                    )
                }

                val post = args.post
                val crossPost = args.crosspost
                val draft = args.draft
                if (crossPost != null) {
                    url.editText?.setText(crossPost.url)
                    title.editText?.setText(crossPost.name)
                    postEditor.editText?.setText(crossPost.getCrossPostContent())
                    nsfwSwitch.isChecked = crossPost.nsfw
                } else if (post != null) {
                    url.editText?.setText(post.url)
                    title.editText?.setText(post.name)
                    postEditor.editText?.setText(post.body)
                    nsfwSwitch.isChecked = post.nsfw
                } else if (draft != null) {
                    viewModel.currentDraftEntry.value = draft
                    viewModel.currentDraftId.value = draft.id
                }
            }

            if (savedInstanceState == null) {
                val extraText = args.extraText
                val extraImage = args.extraStream

                if (extraText != null) {
                    if (URLUtil.isValidUrl(extraText)) {
                        url.editText?.setText(extraText)
                    } else {
                        postEditor.editText?.setText(extraText)
                    }
                }

                if (extraImage != null) {
                    uploadImageViewModel.uploadImageForUrl(extraImage)
                }
            }

            hideSearch(animate = false)
            updateEnableState()

            onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                showSearchBackPressedHandler,
            )
        }
    }

    private var isImeOpen: Boolean = false
    private val outLocation = IntArray(2)

    private fun onImeChange(isImeOpen: Boolean) {
        this.isImeOpen = isImeOpen

        updateToolbar()
    }

    private fun updateToolbar() {
        if (viewModel.showSearch.value) {
            hidePostToolbar()
        } else if (isImeOpen) {
            binding.postBodyToolbarPlaceholder.visibility = View.GONE
            binding.postBodyToolbarPlaceholder2.visibility = View.VISIBLE
            binding.postTextDivider.visibility = View.VISIBLE
            binding.postBodyToolbar.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.BOTTOM
            }
            binding.postBodyToolbar.translationY = 0f

            showPostToolbar()
        } else {
            binding.postBodyToolbarPlaceholder.visibility = View.VISIBLE
            binding.postBodyToolbarPlaceholder2.visibility = View.GONE
            binding.postTextDivider.visibility = View.GONE
            binding.postBodyToolbar.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.TOP or Gravity.LEFT
            }

            onPositionChanged()
        }
    }

    private fun onPositionChanged() {
        if (isImeOpen || !isBindingAvailable()) {
            return
        }

        val scrollBounds = Rect()
        binding.scrollView.getHitRect(scrollBounds)
        val anyPartVisible = binding.postBodyToolbarPlaceholder.getLocalVisibleRect(scrollBounds)
        val visiblePercent = scrollBounds.height().toFloat() / binding.postBodyToolbarPlaceholder.height

        if (anyPartVisible && visiblePercent > 0.9f && !viewModel.showSearch.value) {
            showPostToolbar()
        } else {
            hidePostToolbar()
        }

        binding.postBodyToolbar.translationY = floatingLocation.y.toFloat() -
            (getMainActivity()?.insets?.value?.topInset ?: 0)
    }

    var hiding = false
    var showing = true
    private fun hidePostToolbar() {
        if (hiding) {
            return
        }

        hiding = true
        showing = false

        binding.postBodyToolbar.clearAnimation()
        binding.postBodyToolbar.animate()
            .alpha(0f)
    }

    private fun showPostToolbar() {
        if (showing) {
            return
        }

        hiding = false
        showing = true

        binding.postBodyToolbar.clearAnimation()
        binding.postBodyToolbar.animate()
            .alpha(1f)
    }

    private fun onScrollUpdated() {
        binding.postEditText.getLocationOnScreen(outLocation)

        floatingLocation.y = outLocation[1] +
            binding.postEditText.height

        onPositionChanged()
    }

    private fun createPost() {
        viewModel.createPost(
            communityFullName = binding.communityEditText.text.toString(),
            name = binding.title.editText?.text.toString(),
            body = binding.postEditor.editText?.text.toString(),
            url = binding.url.editText?.text.toString(),
            isNsfw = binding.nsfwSwitch.isChecked,
        )
    }

    private fun updatePost() {
        viewModel.updatePost(
            instance = args.instance,
            name = binding.title.editText?.text.toString(),
            body = binding.postEditor.editText?.text.toString(),
            url = binding.url.editText?.text.toString(),
            isNsfw = binding.nsfwSwitch.isChecked,
            postId = requireNotNull(args.post?.id) { "POST ID WAS NULL!" },
        )
    }

    private fun onLinkMetadataChanged() {
        when (val linkMetadata = viewModel.linkMetadata.value) {
            is StatefulData.Error -> {
                binding.titleSuggestionContainer.visibility = View.GONE
            }
            is StatefulData.Loading -> {
                binding.titleSuggestionContainer.visibility = View.GONE
            }
            is StatefulData.NotStarted -> {
                binding.titleSuggestionContainer.visibility = View.GONE
            }
            is StatefulData.Success -> {
                val title = linkMetadata.data.title

                if (linkMetadata.data.url == binding.urlEditText.text.toString() &&
                    !title.isNullOrBlank() &&
                    title != binding.titleEditText.text.toString()
                ) {
                    binding.titleSuggestionContainer.visibility = View.VISIBLE
                    binding.titleSuggestion.setMarkdown(
                        lemmyTextHelper,
                        getString(
                            R.string.use_suggested_title_format,
                            title,
                        ),
                    )
                    binding.titleSuggestionCard.setOnClickListener {
                        binding.titleEditText.setText(title)
                        onLinkMetadataChanged()
                    }
                } else {
                    binding.titleSuggestionContainer.visibility = View.GONE
                }

                binding.root.post {
                    onScrollUpdated()
                }
            }
        }
    }

    private fun updateEnableState() {
        val isLoading = viewModel.createOrEditPostResult.isLoading

        binding.community.isEnabled = !isLoading
        binding.url.isEnabled = !isLoading
        binding.title.isEnabled = !isLoading
        binding.postEditor.isEnabled = !isLoading
        binding.postBodyToolbar.isEnabled = !isLoading
        binding.nsfwSwitch.isEnabled = !isLoading
        textFormatToolbar?.isEnabled = !isLoading
    }

    private fun Post.getCrossPostContent(): String = buildString {
        appendLine(getString(R.string.cross_posted_from_format, ap_id))
        appendLine()

        body ?: return@buildString

        body.split("\n").forEach {
            appendLine("> $it")
        }
    }

    private fun showSearch() {
        if (!isBindingAvailable()) return

        binding.communitySuggestionsRecyclerView.animate().cancel()

        binding.communitySuggestionsRecyclerView.visibility = View.VISIBLE
        binding.communitySuggestionsRecyclerView.alpha = 0f
        binding.communitySuggestionsRecyclerView.animate()
            .alpha(1f)

        binding.postBodyToolbar.visibility = View.INVISIBLE
    }

    private fun hideSearch(animate: Boolean = true) {
        binding.communitySuggestionsRecyclerView.animate().cancel()

        if (animate) {
            binding.communitySuggestionsRecyclerView.animate()
                .alpha(0f)
                .withEndAction {
                    binding.communitySuggestionsRecyclerView.visibility = View.GONE
                    binding.communitySuggestionsRecyclerView.alpha = 1f
                }
        } else {
            binding.communitySuggestionsRecyclerView.visibility = View.GONE
            binding.communitySuggestionsRecyclerView.alpha = 1f
        }
        if (!binding.communityEditText.text.isNullOrBlank()) {
            Utils.hideKeyboard(requireMainActivity())
        }
        binding.postBodyToolbar.visibility = View.VISIBLE
    }

    private fun isEdit() = args.post != null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        if (!isSent) {
            if (preferences.saveDraftsAutomatically) {
                saveDraft()
            }
        }
    }

    private fun saveDraft(overwriteExistingDraft: Boolean = true) {
        val title = binding.titleEditText.text?.toString()
        val body = binding.postEditText.text?.toString()
        val url = binding.urlEditText.text?.toString()
        val isNsfw = binding.nsfwSwitch.isChecked

        val currentDraftId = viewModel.currentDraftId.value

        if (!title.isNullOrBlank() || !body.isNullOrBlank() || !url.isNullOrBlank()) {
            if (currentDraftId != null && overwriteExistingDraft) {
                viewModel.draftsManager.updateDraftAsync(
                    currentDraftId,
                    DraftData.PostDraftData(
                        originalPost = args.post?.toOriginalPostData(),
                        name = title,
                        body = body,
                        url = url,
                        isNsfw = isNsfw,
                        accountId = viewModel.currentAccount?.id ?: 0,
                        accountInstance = viewModel.currentAccount?.instance ?: "",
                        targetCommunityFullName = binding.communityEditText.text.toString(),
                    ),
                    showToast = true,
                )
            } else {
                viewModel.draftsManager.saveDraftAsync(
                    DraftData.PostDraftData(
                        originalPost = args.post?.toOriginalPostData(),
                        name = title,
                        body = body,
                        url = url,
                        isNsfw = isNsfw,
                        accountId = viewModel.currentAccount?.id ?: 0,
                        accountInstance = viewModel.currentAccount?.instance ?: "",
                        targetCommunityFullName = binding.communityEditText.text.toString(),
                    ),
                    showToast = true,
                )
            }
        }
    }

    private fun Post.toOriginalPostData(): OriginalPostData = OriginalPostData(
        this.name,
        this.body,
        this.url,
        this.nsfw,
    )

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        when (tag) {
            "create_post" -> {
                createPost()
            }
            "update_post" -> {
                updatePost()
            }
        }
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
