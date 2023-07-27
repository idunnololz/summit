package com.idunnololz.summit.lemmy.comment

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.drjacky.imagepicker.ImagePicker
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.accountUi.PreAuthDialogFragment
import com.idunnololz.summit.accountUi.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentAddOrEditCommentBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.utils.TextFormatterHelper
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize

@AndroidEntryPoint
class AddOrEditCommentFragment :
    BaseDialogFragment<FragmentAddOrEditCommentBinding>(),
    FullscreenDialogFragment,
    SignInNavigator,
    BackPressHandler {

    companion object {
        const val REQUEST_KEY = "AddOrEditCommentFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        private const val SIS_TEXT = "SIS_TEXT"
    }

    private val args by navArgs<AddOrEditCommentFragmentArgs>()

    private val viewModel: AddOrEditCommentViewModel by viewModels()

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

    @Parcelize
    enum class Result : Parcelable {
        CommentSent,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
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
            insetViewExceptTopAutomaticallyByMargins(viewLifecycleOwner, binding.coordinatorLayout)
        }

        viewModel.currentAccount.observe(viewLifecycleOwner) {
            if (it != null) {
                setup(it, savedInstanceState)

                requireActivity().invalidateOptionsMenu()
            }
        }

        viewModel.commentSentEvent.observe(viewLifecycleOwner) {
            val result = it.contentIfNotHandled ?: return@observe

            result
                .onSuccess {
                    setFragmentResult(REQUEST_KEY, bundleOf(REQUEST_KEY_RESULT to Result.CommentSent))

                    dismiss()
                }
                .onFailure {
                    ErrorDialogFragment.show(
                        getString(R.string.error_unable_to_send_message),
                        it,
                        childFragmentManager,
                    )
                }
        }

        binding.toolbar.title = getString(R.string.comment)
        if (isEdit()) {
            binding.toolbar.inflateMenu(R.menu.menu_edit_comment)
        } else {
            binding.toolbar.inflateMenu(R.menu.menu_add_comment)
        }
        binding.toolbar.setNavigationIcon(R.drawable.baseline_close_24)
        binding.toolbar.setNavigationIconTint(context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal))
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.send_comment -> {
                    val account = viewModel.currentAccount.value
                    if (account == null) {
                        PreAuthDialogFragment()
                            .showAllowingStateLoss(childFragmentManager, "AS")
                        return@setOnMenuItemClickListener true
                    }

                    val inboxItem = args.inboxItem
                    val personId = args.personId
                    if (personId != 0) {
                        viewModel.sendComment(
                            personId,
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
                    true
                }
                R.id.update_comment -> {
                    if (viewModel.currentAccount.value == null) {
                        PreAuthDialogFragment()
                            .showAllowingStateLoss(childFragmentManager, "DF")
                        return@setOnMenuItemClickListener true
                    }

                    viewModel.updateComment(
                        PostRef(
                            args.instance,
                            requireNotNull(args.editCommentView?.post?.id) {
                                "editCommentView were null!"
                            },
                        ),
                        requireNotNull(args.editCommentView?.comment?.id),
                        binding.commentEditor.editText?.text.toString(),
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun setup(currentAccount: Account, savedInstanceState: Bundle?) {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        val postView = args.postView
        val commentView = args.commentView
        val inboxItem = args.inboxItem
        val personId = args.personId

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
            binding.replyingTo.text = postView.post.body
        } else if (inboxItem != null) {
            binding.replyingTo.text = inboxItem.content
        } else if (personId != 0) {
            binding.scrollView.visibility = View.GONE
            binding.divider.visibility = View.GONE
        } else {
            dismiss()
            return
        }

        textFormatterHelper.setupTextFormatterToolbar(
            binding.textFormatToolbar,
            requireNotNull(commentEditor.editText),
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
            onPreviewClick = {
                PreviewCommentDialogFragment()
                    .apply {
                        arguments = PreviewCommentDialogFragmentArgs(
                            args.instance,
                            commentEditor.editText?.text.toString(),
                        ).toBundle()
                    }
                    .showAllowingStateLoss(childFragmentManager, "AA")
            },
        )
        viewModel.uploadImageEvent.observe(viewLifecycleOwner) {
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
                    viewModel.uploadImageEvent.clear()

                    textFormatterHelper.onImageUploaded(it.data.url)
                }
            }
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
