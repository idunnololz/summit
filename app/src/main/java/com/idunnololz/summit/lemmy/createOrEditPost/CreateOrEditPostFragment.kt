package com.idunnololz.summit.lemmy.createOrEditPost

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentCreateOrEditPostBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.comment.AddLinkDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.utils.TextFormatterHelper
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateOrEditPostFragment :
    BaseDialogFragment<FragmentCreateOrEditPostBinding>(),
    FullscreenDialogFragment,
    BackPressHandler {

    companion object {
        const val REQUEST_KEY = "CreateOrEditPostFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"
    }

    private val args by navArgs<CreateOrEditPostFragmentArgs>()

    private val viewModel: CreateOrEditPostViewModel by viewModels()

    private val textFormatterHelper = TextFormatterHelper()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    viewModel.uploadImage(args.instance, uri)
                }
            }
        }

    private val launcherForUrl =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data

                if (uri != null) {
                    viewModel.uploadImageForUrl(args.instance, uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)

        childFragmentManager.setFragmentResultListener(AddLinkDialogFragment.REQUEST_KEY, this) { key, bundle ->
            val result = bundle.getParcelableCompat<AddLinkDialogFragment.AddLinkResult>(
                AddLinkDialogFragment.REQUEST_KEY_RESULT)
            if (result != null) {
                textFormatterHelper.onLinkAdded(result.text, result.url)
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

        requireMainActivity().apply {
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.content)
        }

        if (isEdit()) {
            binding.toolbar.title = getString(R.string.edit_post)
            binding.toolbar.inflateMenu(R.menu.menu_edit_post)
        } else {
            binding.toolbar.title = getString(R.string.create_post)
            binding.toolbar.inflateMenu(R.menu.menu_add_post)
        }
        binding.toolbar.setNavigationIcon(R.drawable.baseline_close_24)
        binding.toolbar.setNavigationIconTint(context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal))
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.create_post -> {
                    viewModel.createPost(
                        communityRef = CommunityRef.CommunityRefByName(
                            name = requireNotNull(args.communityName),
                            instance = args.instance,
                        ),
                        name = binding.title.editText?.text.toString(),
                        body = binding.postEditor.editText?.text.toString(),
                        url = binding.url.editText?.text.toString(),
                        isNsfw = binding.nsfwSwitch.isChecked,
                    )
                    true
                }
                R.id.update_post -> {
                    viewModel.updatePost(
                        instance = args.instance,
                        name = binding.title.editText?.text.toString(),
                        body = binding.postEditor.editText?.text.toString(),
                        url = binding.url.editText?.text.toString(),
                        isNsfw = binding.nsfwSwitch.isChecked,
                        postId = requireNotNull(args.post?.id) { "POST ID WAS NULL!" },
                    )
                    true
                }
                else -> false
            }
        }

        val postEditor = binding.postEditor
        textFormatterHelper.setupTextFormatterToolbar(
            binding.textFormatToolbar,
            requireNotNull(postEditor.editText),
            onChooseImageClick = {
                val bottomMenu = BottomMenu(context).apply {
                    setTitle(R.string.insert_image)
                    addItemWithIcon(R.id.from_camera, R.string.take_a_photo, R.drawable.baseline_photo_camera_24)
                    addItemWithIcon(R.id.from_gallery, R.string.choose_from_gallery, R.drawable.baseline_image_24)
                    addItemWithIcon(R.id.from_camera_with_editor, R.string.take_a_photo_with_editor, R.drawable.baseline_photo_camera_24)
                    addItemWithIcon(R.id.from_gallery_with_editor, R.string.choose_from_gallery_with_editor, R.drawable.baseline_image_24)

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
                        }
                    }
                }

                bottomMenu.show(
                    mainActivity = requireMainActivity(),
                    viewGroup = binding.coordinatorLayout,
                    expandFully = true,
                    handleBackPress = false,
                )
            },
            onAddLinkClick = {
                AddLinkDialogFragment()
                    .showAllowingStateLoss(childFragmentManager, "asdf")
            },
            onPreviewClick = {
                PreviewCommentDialogFragment()
                    .apply {
                        arguments = PreviewCommentDialogFragmentArgs(
                            args.instance,
                            postEditor.editText?.text.toString(),
                        ).toBundle()
                    }
                    .showAllowingStateLoss(childFragmentManager, "AA")
            },
        )

        binding.loadingView.hideAll()

        binding.uploadImage.setOnClickListener {
            val bottomMenu = BottomMenu(context).apply {
                setTitle(R.string.insert_image)
                addItemWithIcon(R.id.from_camera, R.string.take_a_photo, R.drawable.baseline_photo_camera_24)
                addItemWithIcon(R.id.from_gallery, R.string.choose_from_gallery, R.drawable.baseline_image_24)
                addItemWithIcon(R.id.from_camera_with_editor, R.string.take_a_photo_with_editor, R.drawable.baseline_photo_camera_24)
                addItemWithIcon(R.id.from_gallery_with_editor, R.string.choose_from_gallery_with_editor, R.drawable.baseline_image_24)

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
                    }
                }
            }

            bottomMenu.show(
                mainActivity = requireMainActivity(),
                viewGroup = binding.coordinatorLayout,
                expandFully = true,
                handleBackPress = false,
            )
        }
        viewModel.createOrEditPostResult.observe(viewLifecycleOwner) {
            updateEnableState()

            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()
                    if (it.error is CreateOrEditPostViewModel.NoTitleError) {
                        binding.title.error = getString(R.string.required)
                    } else {
                        AlertDialogFragment.Builder()
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
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    requireActivity().supportFragmentManager.setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(
                            REQUEST_KEY_RESULT to it.data,
                        ),
                    )

                    dismiss()
                }
            }
        }
        viewModel.uploadImageResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()
                    AlertDialogFragment.Builder()
                        .setMessage(
                            getString(
                                R.string.error_unable_to_send_post,
                                it.error::class.qualifiedName,
                                it.error.message,
                            ),
                        )
                        .createAndShow(childFragmentManager, "ASDS")
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingView.hideAll()
                    viewModel.uploadImageResult.clear()

                    textFormatterHelper.onImageUploaded(it.data.url)
                }
            }
        }
        viewModel.uploadImageForUrlResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()
                    AlertDialogFragment.Builder()
                        .setMessage(
                            getString(
                                R.string.error_unable_to_send_post,
                                it.error::class.qualifiedName,
                                it.error.message,
                            ),
                        )
                        .createAndShow(childFragmentManager, "ASDS")
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    binding.loadingView.hideAll()
                    viewModel.uploadImageResult.clear()

                    binding.url.editText?.setText(it.data.url)
                }
            }
        }

        if (savedInstanceState == null && !viewModel.postPrefilled) {
            viewModel.postPrefilled = true

            val post = args.post
            if (post != null) {
                binding.url.editText?.setText(post.url)
                binding.title.editText?.setText(post.name)
                binding.postEditor.editText?.setText(post.body)
                binding.nsfwSwitch.isChecked = post.nsfw
            }
        }
        updateEnableState()
    }

    private fun updateEnableState() {
        val isLoading = viewModel.createOrEditPostResult.isLoading

        binding.url.isEnabled = !isLoading
        binding.uploadImage.isEnabled = !isLoading
        binding.title.isEnabled = !isLoading
        binding.postEditor.isEnabled = !isLoading
        binding.postBodyToolbar.isEnabled = !isLoading
        binding.nsfwSwitch.isEnabled = !isLoading
        binding.textFormatToolbar.root.children.forEach {
            it.isEnabled = !isLoading
        }
    }

    private fun isEdit() =
        args.post != null

    override fun onBackPressed(): Boolean {
        if (isBindingAvailable()) {
            if (binding.coordinatorLayout.childCount > 0) {
                binding.coordinatorLayout.removeAllViews()
                return true
            }
        }

        try {
            dismiss()
        } catch (e: IllegalStateException) {
            // do nothing... very rare
        }
        return true
    }
}
