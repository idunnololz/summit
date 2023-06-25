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
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.databinding.FragmentAddOrEditCommentBinding
import com.idunnololz.summit.lemmy.PostRef
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.navigateSafe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import java.lang.Integer.max
import java.lang.Integer.min


@AndroidEntryPoint
class AddOrEditCommentFragment : BaseFragment<FragmentAddOrEditCommentBinding>() {

    companion object {
        const val REQUEST_KEY = "AddOrEditCommentFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        private val TEXT_EMOJIS = listOf(
            "( ͡° ͜ʖ ͡° )",
            "ಠ_ಠ",
            "(╯°□°）╯︵ ┻━┻",
            "┬─┬ノ( º _ ºノ)",
            "¯\\_(ツ)_/¯",
            "༼ つ ◕_◕ ༽つ",
            "ᕕ( ᐛ )ᕗ",
            "(•_•) ( •_•)>⌐■-■ (⌐■_■)"
        )
    }

    private val args by navArgs<AddOrEditCommentFragmentArgs>()

    private val viewModel: AddOrEditCommentViewModel by viewModels()

    @Parcelize
    enum class Result : Parcelable {
        CommentSent,
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

            setupActionBar(
                "",
                showUp = false,
                animateActionBarIn = false,
            )

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.comment)
        }

        viewModel.currentAccount.observe(viewLifecycleOwner) {
            if (it != null) {
                setup(it)

                requireActivity().invalidateOptionsMenu()
            }
        }

        viewModel.commentSentEvent.observe(viewLifecycleOwner) {
            it.contentIfNotHandled ?: return@observe

            setFragmentResult(REQUEST_KEY, bundleOf(REQUEST_KEY_RESULT to Result.CommentSent))

            findNavController().navigateUp()
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                val account = viewModel.currentAccount.value ?: return

                if (isEdit()) {
                    menuInflater.inflate(R.menu.menu_edit_comment, menu)
                } else {
                    menuInflater.inflate(R.menu.menu_add_comment, menu)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.send_comment -> {
                        viewModel.sendComment(
                            PostRef(args.instance,
                            requireNotNull(args.postView?.post?.id ?: args.commentView?.post?.id) {
                                "Both postView and commentView were null!"
                            }),
                            args.commentView?.comment?.id,
                            binding.commentEditor.text.toString(),
                        )
                        true
                    }
                    R.id.update_comment -> {
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

        })
    }

    private fun setup(currentAccount: Account) {
        if (!isBindingAvailable()) {
            return
        }

        val context = requireContext()

        val postView = args.postView
        val commentView = args.commentView

        binding.replyingTo.movementMethod = ScrollingMovementMethod()

        if (isEdit()) {
            val commentToEdit = requireNotNull(args.editCommentView)
            binding.replyingTo.visibility = View.GONE
            binding.divider.visibility = View.GONE

            binding.commentEditor.setText(commentToEdit.comment.content)
        } else if (commentView != null) {
            binding.replyingTo.text = commentView.comment.content
        } else if (postView != null) {
            binding.replyingTo.text = postView.post.body
        } else {
            findNavController().navigateUp()
            return
        }

        with(binding) {
            preview.setOnClickListener {
                val direction = AddOrEditCommentFragmentDirections
                    .actionAddOrEditCommentFragmentToPreviewCommentDialogFragment(
                        args.instance,
                        binding.commentEditor.text.toString()
                    )
                findNavController().navigateSafe(direction)
            }

            textEmojis.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    menu.apply {
                        TEXT_EMOJIS.withIndex().forEach { (index, str) ->
                            add(0, index, 0, str)
                        }
                    }
                    setOnMenuItemClickListener {
                        val emoji = TEXT_EMOJIS[it.itemId]

                        replaceTextAtCursor(emoji)

                        true
                    }
                }.show()
            }

            spoiler.setOnClickListener {
                wrapTextAtCursor(
                    startText = "::: spoiler spoiler\n",
                    endText = "\n:::"
                )
            }
            bold.setOnClickListener {
                wrapTextAtCursor(
                    startText = "**",
                    endText = "**"
                )
            }
            italic.setOnClickListener {
                wrapTextAtCursor(
                    startText = "*",
                    endText = "*"
                )
            }
            strikethrough.setOnClickListener {
                wrapTextAtCursor(
                    startText = "~~",
                    endText = "~~"
                )
            }
            quote.setOnClickListener {
                wrapTextAtCursor(
                    startText = "> ",
                    endText = ""
                )
            }
            bulletedList.setOnClickListener {
                wrapTextAtCursor(
                    startText = "* ",
                    endText = "",
                    autoLineBreak = true,
                )
            }
            numberedList.setOnClickListener {
                wrapTextAtCursor(
                    startText = "1. ",
                    endText = "",
                    autoLineBreak = true,
                )
            }

            linkApp.setOnClickListener {
                replaceTextAtCursor(
                    "Play store link: [Summit - Lemmy Reader](https://play.google.com/store/apps/details?id=com.idunnololz.summit)"
                )
            }
        }
    }

    private fun replaceTextAtCursor(text: String) {
        if (!isBindingAvailable()) return

        val editText = binding.commentEditor
        val start = editText.selectionStart.coerceAtLeast(0)
        val end = editText.selectionEnd.coerceAtLeast(0)
        editText.text.replace(
            start.coerceAtMost(end),
            start.coerceAtLeast(end),
            text,
            0,
            text.length
        )
    }

    private fun wrapTextAtCursor(
        startText: String,
        endText: String,
        autoLineBreak: Boolean = false
    ) {
        if (!isBindingAvailable()) return

        val editText = binding.commentEditor
        val start = editText.selectionStart.coerceAtLeast(0)
        val end = editText.selectionEnd.coerceAtLeast(0)

        val prevChar = editText.text.toString().getOrNull(min(start - 1, end - 1))

        var finalCursorPos = min(start, end) + startText.length

        editText.text.insert(max(start, end), endText)
        editText.text.insert(min(start, end), startText)

        if (prevChar != null && prevChar != '\n' && autoLineBreak) {
            editText.text.insert(min(start, end), "\n")
            finalCursorPos++
        }

        editText.setSelection(finalCursorPos)
    }

    private fun isEdit(): Boolean {
        return args.editCommentView != null
    }
}