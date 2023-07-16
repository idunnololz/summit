package com.idunnololz.summit.lemmy.comment

import android.os.Bundle
import android.os.Parcelable
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account_ui.PreAuthDialogFragment
import com.idunnololz.summit.account_ui.SignInNavigator
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.FragmentAddOrEditCommentBinding
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.utils.TextFormatterHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import java.lang.Integer.max
import java.lang.Integer.min


@AndroidEntryPoint
class AddOrEditCommentFragment : BaseDialogFragment<FragmentAddOrEditCommentBinding>(),
    FullscreenDialogFragment, SignInNavigator {

    companion object {
        const val REQUEST_KEY = "AddOrEditCommentFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"
    }

    private val args by navArgs<AddOrEditCommentFragmentArgs>()

    private val viewModel: AddOrEditCommentViewModel by viewModels()

    private val textFormatterHelper = TextFormatterHelper()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            viewModel.uploadImage(args.instance, uri)
        }
    }

    @Parcelize
    enum class Result : Parcelable {
        CommentSent,
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_Dialog_Fullscreen)
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
        savedInstanceState: Bundle?
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

        viewModel.currentAccount.observe(viewLifecycleOwner) {
            if (it != null) {
                setup(it)

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
                    AlertDialogFragment.Builder()
                        .setMessage(context.getString(
                            R.string.error_unable_to_send_message,
                            it::class.qualifiedName,
                            it.message))
                        .createAndShow(childFragmentManager, "errr")
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
                    if (viewModel.currentAccount.value == null) {
                        PreAuthDialogFragment()
                            .showAllowingStateLoss(childFragmentManager, "AS")
                        return@setOnMenuItemClickListener true
                    }

                    val inboxItem = args.inboxItem
                    val personId = args.personId
                    if (personId != 0) {
                        viewModel.sendComment(
                            personId,
                            binding.commentEditor.text.toString()
                        )
                    } else if (inboxItem != null) {
                        viewModel.sendComment(
                            args.instance,
                            inboxItem,
                            binding.commentEditor.text.toString()
                        )
                    } else {
                        viewModel.sendComment(
                            PostRef(args.instance,
                                requireNotNull(
                                    args.postView?.post?.id ?: args.commentView?.post?.id
                                ) {
                                    "Both postView and commentView were null!"
                                }),
                            args.commentView?.comment?.id,
                            binding.commentEditor.text.toString(),
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
                        PostRef(args.instance,
                            requireNotNull(args.editCommentView?.post?.id) {
                                "editCommentView were null!"
                            }),
                        requireNotNull(args.editCommentView?.comment?.id),
                        binding.commentEditor.text.toString(),
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun setup(currentAccount: Account) {
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

            commentEditor.setText(commentToEdit.comment.content)
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
            commentEditor,
            imagePickerLauncher,
            onPreviewClick = {
                PreviewCommentDialogFragment()
                    .apply {
                        arguments = PreviewCommentDialogFragmentArgs(
                            args.instance,
                            commentEditor.text.toString()
                        ).toBundle()
                    }
                    .showAllowingStateLoss(childFragmentManager, "AA")
            }
        )
        viewModel.uploadImageEvent.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()
                    AlertDialogFragment.Builder()
                        .setMessage(getString(
                            R.string.error_unable_to_send_post,
                            it.error::class.qualifiedName,
                            it.error.message))
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
}