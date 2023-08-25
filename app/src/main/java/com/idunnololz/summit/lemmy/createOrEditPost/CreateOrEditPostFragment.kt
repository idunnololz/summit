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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.databinding.FragmentCreateOrEditPostBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.comment.AddLinkDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.TextFormatterHelper
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getSelectedText
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.getParcelableCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

    private var adapter: CommunitySearchResultsAdapter? = null

    @Inject
    lateinit var offlineManager: OfflineManager

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

        childFragmentManager.setFragmentResultListener(
            AddLinkDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val result = bundle.getParcelableCompat<AddLinkDialogFragment.AddLinkResult>(
                AddLinkDialogFragment.REQUEST_KEY_RESULT,
            )
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
                        communityFullName = binding.communityEditText.text.toString(),
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
                AddLinkDialogFragment.show(
                    binding.postEditText.getSelectedText(),
                    childFragmentManager
                )
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

        binding.url.setEndIconOnClickListener {
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
                    setFragmentResult(
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

        binding.communityEditText.setOnClickListener {
            viewModel.showSearch.value = true
        }

        binding.communityEditText.onFocusChangeListener = View.OnFocusChangeListener { editTextView, hasFocus ->
            viewModel.showSearch.value =
                binding.communityEditText.hasFocus() &&
                    !binding.communityEditText.text.isNullOrBlank()
        }

        adapter = CommunitySearchResultsAdapter(
            context,
            offlineManager,
            onCommunitySelected = {
                binding.communityEditText.setText(it.community.toCommunityRef().fullName)
                viewModel.showSearch.value = false
            },
        )
        binding.communitySuggestionsRecyclerView.apply {
            adapter = this@CreateOrEditPostFragment.adapter
            setHasFixedSize(true)
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

        viewModel.showSearchLiveData.observe(viewLifecycleOwner) {
            if (it) {
                showSearch()
            } else {
                hideSearch()
            }
        }

        binding.communityEditText.addTextChangedListener {
            val query = it?.toString() ?: ""
            adapter?.setQuery(query) {
                binding.communitySuggestionsRecyclerView.scrollToPosition(0)
            }
            viewModel.query.value = query.split("@").firstOrNull() ?: ""

            if (binding.communityEditText.hasFocus()) {
                viewModel.showSearch.value = query.isNotBlank()
            }
        }

        if (savedInstanceState == null && !viewModel.postPrefilled) {
            viewModel.postPrefilled = true

            if (args.communityName != null) {
                binding.communityEditText.setText(
                    CommunityRef.CommunityRefByName(
                        name = requireNotNull(args.communityName),
                        instance = args.instance,
                    ).fullName,
                )
            }

            val post = args.post
            val crossPost = args.crosspost
            if (crossPost != null) {
                binding.url.editText?.setText(crossPost.url)
                binding.title.editText?.setText(crossPost.name)
                binding.postEditor.editText?.setText(crossPost.getCrossPostContent())
                binding.nsfwSwitch.isChecked = crossPost.nsfw
            } else if (post != null) {
                binding.url.editText?.setText(post.url)
                binding.title.editText?.setText(post.name)
                binding.postEditor.editText?.setText(post.body)
                binding.nsfwSwitch.isChecked = post.nsfw
            }
        }

        hideSearch(animate = false)
        updateEnableState()
    }

    private fun updateEnableState() {
        val isLoading = viewModel.createOrEditPostResult.isLoading

        binding.community.isEnabled = !isLoading
        binding.url.isEnabled = !isLoading
        binding.title.isEnabled = !isLoading
        binding.postEditor.isEnabled = !isLoading
        binding.postBodyToolbar.isEnabled = !isLoading
        binding.nsfwSwitch.isEnabled = !isLoading
        binding.textFormatToolbar.root.children.forEach {
            it.isEnabled = !isLoading
        }
    }

    private fun Post.getCrossPostContent(): String =
        buildString {
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
        if (viewModel.showSearch.value) {
            viewModel.showSearch.value = false
            return true
        }

        try {
            dismiss()
        } catch (e: IllegalStateException) {
            // do nothing... very rare
        }
        return true
    }
}
