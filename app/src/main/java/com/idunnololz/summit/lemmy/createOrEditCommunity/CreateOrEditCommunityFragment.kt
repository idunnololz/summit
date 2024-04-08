package com.idunnololz.summit.lemmy.createOrEditCommunity

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.dispose
import coil.load
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.api.UploadImageResult
import com.idunnololz.summit.databinding.FragmentCreateOrEditCommunityBinding
import com.idunnololz.summit.drafts.DraftTypes
import com.idunnololz.summit.drafts.DraftsDialogFragment
import com.idunnololz.summit.editTextToolbar.EditTextToolbarSettingsDialogFragment
import com.idunnololz.summit.editTextToolbar.FloatingToolbarController
import com.idunnololz.summit.editTextToolbar.TextFieldToolbarManager
import com.idunnololz.summit.editTextToolbar.TextFormatToolbarViewHolder
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.UploadImageViewModel
import com.idunnololz.summit.lemmy.comment.AddLinkDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.languageSelect.LanguageSelectDialogFragment
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.showInsertImageMenu
import com.idunnololz.summit.lemmy.utils.showAdvancedLinkOptions
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragment
import com.idunnololz.summit.saveForLater.ChooseSavedImageDialogFragmentArgs
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.StatefulLiveData
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByMargins
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@AndroidEntryPoint
class CreateOrEditCommunityFragment : BaseFragment<FragmentCreateOrEditCommunityBinding>() {

    companion object {
        const val REQUEST_KEY = "CreateOrEditCommunityFragment_req"
        const val REQUEST_RESULT = "result"
    }

    @Parcelize
    data class Result(
        val communityRef: CommunityRef.CommunityRefByName?
    ): Parcelable

    private val args by navArgs<CreateOrEditCommunityFragmentArgs>()

    private val viewModel: CreateOrEditCommunityViewModel by viewModels()
    private val uploadImageViewModel: UploadImageViewModel by viewModels()

    @Inject
    lateinit var moreActionsHelper: MoreActionsHelper

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var textFieldToolbarManager: TextFieldToolbarManager

    private var textFormatToolbar: TextFormatToolbarViewHolder? = null

    private var floatingToolbarController: FloatingToolbarController? = null

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    uploadImageViewModel.uploadImage(uri)
                }
            }
        }
    private val setIconLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    uploadImageViewModel.uploadImageForCommunityIcon(uri)
                }
            }
        }
    private val setBannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    uploadImageViewModel.uploadImageForCommunityBanner(uri)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentCreateOrEditCommunityBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        viewModel.loadCommunityInfoIfNeeded(args.community?.toCommunityRef())

        childFragmentManager.setFragmentResultListener(
            LanguageSelectDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<LanguageSelectDialogFragment.Result>(
                LanguageSelectDialogFragment.REQUEST_RESULT,
            )
            if (result != null) {
                viewModel.updateLanguages(result.selectedLanguages)
            }
        }

        requireMainActivity().apply {
            setupForFragment<CreateOrEditCommunityFragment>()
            insetViewExceptTopAutomaticallyByMargins(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title =
                if (isCreateCommunity) {
                    getString(R.string.create_community)
                } else {
                    getString(R.string.edit_community)
                }

            hideBottomNav()
        }

        textFieldToolbarManager.textFieldToolbarSettings.observe(viewLifecycleOwner) {
            binding.postBodyToolbar.removeAllViews()

            textFormatToolbar = textFieldToolbarManager.createTextFormatterToolbar(
                context,
                binding.postBodyToolbar,
            )

            val postEditor = binding.descriptionInputLayout
            textFormatToolbar?.setupTextFormatterToolbar(
                requireNotNull(postEditor.editText),
                onChooseImageClick = {
                    val bottomMenu = BottomMenu(context).apply {
                        setTitle(R.string.insert_image)
                        addItemWithIcon(R.id.from_camera, R.string.take_a_photo, R.drawable.baseline_photo_camera_24)
                        addItemWithIcon(R.id.from_gallery, R.string.choose_from_gallery, R.drawable.baseline_image_24)
                        addItemWithIcon(R.id.from_camera_with_editor, R.string.take_a_photo_with_editor, R.drawable.baseline_photo_camera_24)
                        addItemWithIcon(R.id.from_gallery_with_editor, R.string.choose_from_gallery_with_editor, R.drawable.baseline_image_24)
                        addItemWithIcon(R.id.use_a_saved_image, R.string.use_a_saved_image, R.drawable.baseline_save_24)

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
                                        .showAllowingStateLoss(childFragmentManager, "ChooseSavedImageDialogFragment")
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
                        binding.descriptionEditText.getSelectedText(),
                        childFragmentManager,
                    )
                },
                onPreviewClick = {
                    val postStr = buildString {
                        appendLine("## ${binding.displayNameEditText.text}")
                        appendLine()
                        appendLine(postEditor.editText?.text.toString())
                    }
                    PreviewCommentDialogFragment()
                        .apply {
                            arguments = PreviewCommentDialogFragmentArgs(
                                viewModel.instance,
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

        floatingToolbarController = FloatingToolbarController(
            floatingPlaceholder = binding.textFieldToolbarPlaceholder,
            floatingViews = listOf(),
            anchoredPlaceholder = binding.textFieldToolbarPlaceholder2,
            anchoredViews = listOf(),
            toolbarContainer = binding.postBodyToolbar,
            floatingToolbarContainer = binding.scrollView,
            textField = binding.descriptionEditText,
            lifecycleOwner = viewLifecycleOwner,
            mainActivityProvider = { getMainActivity() },
        )
        floatingToolbarController?.setup(binding.root)

        viewModel.currentCommunityData.observe(viewLifecycleOwner) {
            updateFields(it)
        }
        viewModel.getCommunityResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.scrollView.visibility = View.INVISIBLE
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading -> {
                    binding.scrollView.visibility = View.INVISIBLE
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.scrollView.visibility = View.VISIBLE
                    binding.loadingView.hideAll()
                }
            }
        }
        viewModel.updateCommunityResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()
                    setFormEnabled(enabled = true)

                    ErrorDialogFragment.show(
                        getString(R.string.error_update_community),
                        it.error,
                        childFragmentManager,
                    )
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                    setFormEnabled(enabled = false)
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingView.hideAll()
                    setFormEnabled(enabled = true)
                    findNavController().popBackStack()
                }
            }
        }
        viewModel.createCommunityResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()
                    setFormEnabled(enabled = true)

                    ErrorDialogFragment.show(
                        getString(R.string.error_create_community),
                        it.error,
                        childFragmentManager,
                    )
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                    setFormEnabled(enabled = false)
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(
                            REQUEST_RESULT to Result(
                                viewModel.currentCommunityData.value
                                    ?.community
                                    ?.toCommunityRef()
                                    ?.copy(
                                        instance = viewModel.instance
                                    )
                            )
                        )
                    )

                    binding.loadingView.hideAll()
                    setFormEnabled(enabled = true)
                    findNavController().popBackStack()
                }
            }
        }

        listOf(
            uploadImageViewModel.uploadImageResult to UploadResultTarget.Description,
            uploadImageViewModel.uploadImageForCommunityIcon to UploadResultTarget.Icon,
            uploadImageViewModel.uploadImageForCommunityBanner to UploadResultTarget.Banner,
        ).forEach { (result, target) ->
            result.observe(viewLifecycleOwner) {
                onUploadComplete(result, it, target)
            }
        }

        addMenuProvider2(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_create_or_edit_community, menu)

                if (isCreateCommunity) {
                    menu.findItem(R.id.publish).isVisible = true
                    menu.findItem(R.id.save).isVisible = false
                } else {
                    menu.findItem(R.id.publish).isVisible = false
                    menu.findItem(R.id.save).isVisible = true
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.publish -> {
                        viewModel.update {
                            it.copy(
                                title = binding.displayNameEditText.text?.toString() ?: it.title,
                                description = binding.descriptionEditText.text?.toString(),
                            )
                        }
                        viewModel.createCommunity()
                        true
                    }
                    R.id.save -> {
                        viewModel.update {
                            it.copy(
                                title = binding.displayNameEditText.text?.toString() ?: it.title,
                                description = binding.descriptionEditText.text?.toString(),
                            )
                        }
                        viewModel.saveChanges()
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
        })

        with(binding) {
            nameEditText.doOnTextChanged a@{ text, start, before, count ->
                text ?: return@a

                viewModel.update {
                    it.copy(
                        name = text.toString()
                    )
                }
            }
            displayNameEditText.doOnTextChanged a@{ text, start, before, count ->
                text ?: return@a

                viewModel.update {
                    it.copy(
                        title = text.toString()
                    )
                }
            }
            descriptionEditText.doOnTextChanged a@{ text, start, before, count ->
                text ?: return@a

                viewModel.update {
                    it.copy(
                        description = text.toString()
                    )
                }
            }
        }
    }

    private fun onUploadComplete(
        result: StatefulLiveData<UploadImageResult>,
        uploadResult: StatefulData<UploadImageResult>,
        target: UploadResultTarget,
    ) {
        when (uploadResult) {
            is StatefulData.Error -> {
                binding.loadingView.hideAll()

                ErrorDialogFragment.show(
                    getString(R.string.error_unable_to_upload_image),
                    uploadResult.error,
                    childFragmentManager,
                )
            }
            is StatefulData.Loading -> {
                binding.loadingView.showProgressBar()
            }
            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                binding.loadingView.hideAll()
                result.clear()

                when (target) {
                    UploadResultTarget.Description ->
                        textFormatToolbar?.onImageUploaded(uploadResult.data.url)
                    UploadResultTarget.Icon ->
                        viewModel.update {
                            it.copy(
                                icon = uploadResult.data.url,
                            )
                        }
                    UploadResultTarget.Banner ->
                        viewModel.update {
                            it.copy(
                                banner = uploadResult.data.url,
                            )
                        }
                }
            }
        }
    }

    private fun setFormEnabled(enabled: Boolean) {
        with(binding) {
            displayNameEditText.isEnabled = enabled
            editIconButton.isEnabled = enabled
            clearIconButton.isEnabled = enabled
            editBannerButton.isEnabled = enabled
            clearBannerButton.isEnabled = enabled
            descriptionEditText.isEnabled = enabled
        }
    }

    private fun updateFields(communityData: CreateOrEditCommunityViewModel.CommunityData) {
        val context = requireContext()
        val community = communityData.community

        with(binding) {
            if (isCreateCommunity) {
                nameInputLayout.visibility = View.VISIBLE
            } else {
                nameInputLayout.visibility = View.GONE
            }

            if (nameEditText.text.isNullOrBlank() && community.name.isNotBlank()) {
                nameEditText.setText(community.name)
            }

            if (displayNameEditText.text.isNullOrBlank() && community.title.isNotBlank()) {
                displayNameEditText.setText(community.title)
            }

            if (community.icon == null) {
                icon.load(R.drawable.ic_community_default)
                icon.setOnClickListener(null)
            } else {
                icon.dispose()
                offlineManager.fetchImageWithError(
                    rootView = root,
                    url = community.icon,
                    listener = {
                        icon.load(it)
                    },
                    errorListener = {
                        icon.load(R.drawable.ic_community_default)
                    },
                )
                icon.setOnClickListener {
                    getMainActivity()?.let {
                        it.showAdvancedLinkOptions(
                            community.icon,
                            it.moreActionsHelper,
                            childFragmentManager,
                        )
                    }
                }
            }

            editIconButton.setOnClickListener {
                showInsertImageMenu(
                    context,
                    setIconLauncher,
                )
            }

            editBannerButton.setOnClickListener {
                showInsertImageMenu(
                    context,
                    setBannerLauncher,
                )
            }

            if (community.banner == null) {
                banner.load(null)
                banner.setOnClickListener(null)
            } else {
                banner.dispose()
                offlineManager.fetchImageWithError(
                    rootView = root,
                    url = community.banner,
                    listener = {
                        banner.load(it)
                    },
                    errorListener = {
                        banner.load(null)
                    },
                )
                banner.setOnClickListener {
                    getMainActivity()?.let {
                        it.showAdvancedLinkOptions(
                            community.banner,
                            it.moreActionsHelper,
                            childFragmentManager,
                        )
                    }
                }
            }

            if (descriptionEditText.text.isNullOrBlank() && !community.description.isNullOrBlank()) {
                descriptionEditText.setText(community.description)
            }

            nsfwCheckbox.isChecked = community.nsfw
            nsfwCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
                viewModel.update {
                    it.copy(
                        nsfw = isChecked
                    )
                }
            }
            onlyModCheckbox.isChecked = community.posting_restricted_to_mods
            onlyModCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
                viewModel.update {
                    it.copy(
                        posting_restricted_to_mods = isChecked
                    )
                }
            }
            if (communityData.discussionLanguages == null) {
                editLanguagesButton.visibility = View.GONE
            } else {
                editLanguagesButton.visibility = View.VISIBLE
                editLanguagesButton.setOnClickListener {
                    LanguageSelectDialogFragment.show(
                        languages = communityData.allLanguages,
                        selectedLanguages = communityData.discussionLanguages,
                        fragmentManager = childFragmentManager
                    )
                }
            }
        }
    }

    val isCreateCommunity: Boolean
        get() = args.community == null

    enum class UploadResultTarget {
        Description,
        Icon,
        Banner,
    }
}
