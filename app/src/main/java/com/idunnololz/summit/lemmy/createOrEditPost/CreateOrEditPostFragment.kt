package com.idunnololz.summit.lemmy.createOrEditPost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import arrow.core.Either
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.databinding.FragmentCreateOrEditPostBinding
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragment
import com.idunnololz.summit.lemmy.comment.AddOrEditCommentFragmentDirections
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragment
import com.idunnololz.summit.lemmy.comment.PreviewCommentDialogFragmentArgs
import com.idunnololz.summit.lemmy.utils.TextFormatterHelper
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateOrEditPostFragment : BaseDialogFragment<FragmentCreateOrEditPostBinding>() {

    companion object {
        const val REQUEST_KEY = "CreateOrEditPostFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"
    }

    private val args by navArgs<CreateOrEditPostFragmentArgs>()

    private val viewModel: CreateOrEditPostViewModel by viewModels()

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
                        instance = args.instance,
                        name = binding.title.text.toString(),
                        body = binding.postEditor.text.toString(),
                        url = binding.url.text.toString(),
                        isNsfw = binding.nsfwSwitch.isChecked,
                        communityNameOrId = if (args.communityName != null) {
                            Either.Left(requireNotNull(args.communityName))
                        } else {
                            Either.Right(args.communityId as CommunityId)
                        },
                    )
                    true
                }
                R.id.update_post -> {
                    viewModel.updatePost(
                        instance = args.instance,
                        name = binding.title.text.toString(),
                        body = binding.postEditor.text.toString(),
                        url = binding.url.text.toString(),
                        isNsfw = binding.nsfwSwitch.isChecked,
                        postId = requireNotNull(args.post?.id) { "POST ID WAS NULL!" },
                    )
                    true
                }
                else -> false
            }
        }

        val postEditor = binding.postEditor
        TextFormatterHelper.setupTextFormatterToolbar(
            binding.textFormatToolbar,
            postEditor,
            onPreviewClick = {
                PreviewCommentDialogFragment()
                    .apply {
                        arguments = PreviewCommentDialogFragmentArgs(
                            args.instance,
                            postEditor.text.toString()
                        ).toBundle()
                    }
                    .showAllowingStateLoss(childFragmentManager, "AA")
            }
        )

        binding.loadingView.hideAll()

        viewModel.createOrEditPostResult.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.hideAll()
                    if (it.error is CreateOrEditPostViewModel.NoTitleError) {
                        binding.title.error = getString(R.string.required)
                    } else {
                        AlertDialogFragment.Builder()
                            .setMessage(getString(
                                R.string.error_unable_to_send_post,
                                it.error::class.qualifiedName,
                                it.error.message))
                            .createAndShow(childFragmentManager, "ASDS")
                    }
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    requireActivity().supportFragmentManager.setFragmentResult(
                        REQUEST_KEY, bundleOf(
                            REQUEST_KEY_RESULT to it.data)
                    )

                    dismiss()
                }
            }
        }

        if (savedInstanceState == null && !viewModel.postPrefilled) {
            viewModel.postPrefilled = true

            val post = args.post
            if (post != null) {
                binding.url.setText(post.url)
                binding.title.setText(post.name)
                binding.postEditor.setText(post.body)
                binding.nsfwSwitch.isChecked = post.nsfw
            }
        }
    }

    private fun isEdit() =
        args.post != null

}