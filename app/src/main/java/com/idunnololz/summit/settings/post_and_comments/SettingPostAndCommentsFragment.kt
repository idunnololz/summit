package com.idunnololz.summit.settings.post_and_comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.databinding.FragmentSettingPostAndCommentsBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.databinding.PostHeaderItemBinding
import com.idunnololz.summit.lemmy.post.ThreadLinesDecoration
import com.idunnololz.summit.lemmy.postAndCommentView.PostAndCommentViewBuilder
import com.idunnololz.summit.settings.LemmyFakeModels
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.SliderSettingItem
import com.idunnololz.summit.settings.ui.bindTo
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingPostAndCommentsFragment : BaseFragment<FragmentSettingPostAndCommentsBinding>(),
    AlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: SettingPostAndCommentsViewModel by viewModels()

    @Inject
    lateinit var postAndCommentViewBuilder: PostAndCommentViewBuilder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingPostAndCommentsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.scrollView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.post_and_comments)
        }

        binding.root.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                binding.root.viewTreeObserver.removeOnPreDrawListener(this)

                setup()
                return false
            }
        })
    }

    fun setup() {
        if (!isBindingAvailable()) return

        val adapter = FakePostAndCommentsAdapter(
            binding.demoViewContainer,
            postAndCommentViewBuilder,
            binding.demoViewContainer.width,
            viewLifecycleOwner,
        )

        val context = requireContext()

        binding.resetPostStyles.setOnClickListener {
            AlertDialogFragment.Builder()
                .setMessage(R.string.reset_view_to_default_styles)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "reset_post_to_default_styles")
        }

        binding.resetCommentStyles.setOnClickListener {
            AlertDialogFragment.Builder()
                .setMessage(R.string.reset_view_to_default_styles)
                .setPositiveButton(android.R.string.ok)
                .setNegativeButton(R.string.cancel)
                .createAndShow(childFragmentManager, "reset_comment_to_default_styles")
        }

        binding.demoViewContainer.adapter = adapter
        binding.demoViewContainer.setHasFixedSize(true)
        binding.demoViewContainer.layoutManager = LinearLayoutManager(context)
        binding.demoViewContainer.addItemDecoration(ThreadLinesDecoration(context))

        viewModel.onPostUiChanged.observe(viewLifecycleOwner) {
            updateRendering(adapter)
            bindPostUiSettings(adapter)
        }
        bindPostUiSettings(adapter)
        updateRendering(adapter)
    }

    private fun bindPostUiSettings(adapter: FakePostAndCommentsAdapter) {
        SliderSettingItem(
            R.id.setting_post_text_scaling,
            getString(R.string.font_size),
            0.2f,
            3f,
        ).bindTo(
            binding.textScalingSetting1,
            { viewModel.currentPostAndCommentUiConfig.postUiConfig.textSizeMultiplier },
            {
                viewModel.currentPostAndCommentUiConfig =
                    viewModel.currentPostAndCommentUiConfig.copy(
                        postUiConfig = viewModel.currentPostAndCommentUiConfig
                            .postUiConfig
                            .updateTextSizeMultiplier(it)
                    )

                updateRendering(adapter)
            }
        )
        SliderSettingItem(
            R.id.setting_comment_text_scaling,
            getString(R.string.font_size),
            0.2f,
            3f,
        ).bindTo(
            binding.textScalingSetting2,
            { viewModel.currentPostAndCommentUiConfig.commentUiConfig.textSizeMultiplier },
            {
                viewModel.currentPostAndCommentUiConfig =
                    viewModel.currentPostAndCommentUiConfig.copy(
                        commentUiConfig = viewModel.currentPostAndCommentUiConfig
                            .commentUiConfig
                            .updateTextSizeMultiplier(it)
                    )

                updateRendering(adapter)
            }
        )
    }

    private fun updateRendering(adapter: FakePostAndCommentsAdapter) {
        if (!isBindingAvailable()) return

        val set: Transition = TransitionSet()
            .addTransition(Fade(Fade.IN or Fade.OUT))
            .addTransition(ChangeBounds())
            .addTransition(ChangeClipBounds())
            .addTransition(ChangeImageTransform())
            .setOrdering(TransitionSet.ORDERING_TOGETHER)
            .setDuration(Utils.ANIMATION_DURATION_MS)
        TransitionManager.beginDelayedTransition(binding.demoViewContainer, set)

        binding.demoViewContainer.itemAnimator = null
        postAndCommentViewBuilder.uiConfig = viewModel.currentPostAndCommentUiConfig
        adapter.notifyDataSetChanged()
    }

    private class FakePostAndCommentsAdapter(
        private val container: View,
        private val postAndCommentViewBuilder: PostAndCommentViewBuilder,
        private val contentMaxWidth: Int,
        private val viewLifecycleOwner: LifecycleOwner,
    ) : Adapter<ViewHolder>() {

        private val items = listOf(
            LemmyFakeModels.fakePostView,
            LemmyFakeModels.fakeCommentView1,
            LemmyFakeModels.fakeCommentView2,
        )

        private val adapterHelper = AdapterHelper<Any>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is PostView -> true
                    is CommentView ->
                        old.comment.id == (new as CommentView).comment.id
                    else ->
                        false
                }
            }
        ).apply {
            addItemType(PostView::class, PostHeaderItemBinding::inflate) { item, b, h ->
                postAndCommentViewBuilder.bindPostView(
                    b,
                    container = container,
                    postView = item,
                    instance = "https://fake.instance",
                    isRevealed = true,
                    contentMaxWidth = contentMaxWidth,
                    viewLifecycleOwner = viewLifecycleOwner,
                    updateContent = true,
                    onRevealContentClickedFn = {},
                    onImageClick = {},
                    onVideoClick = { _, _, _ -> },
                    onPageClick = {},
                    onSignInRequired = {},
                    onInstanceMismatch = { _, _ -> },
                    videoState = null,
                    onAddCommentClick = {},
                    onPostMoreClick = {},
                )
            }
            addItemType(CommentView::class, PostCommentExpandedItemBinding::inflate) { item, b, h ->
                postAndCommentViewBuilder.bindCommentViewExpanded(
                    h,
                    b,
                    0,
                    if (item.comment.id == LemmyFakeModels.fakeCommentView1.comment.id) {
                        0
                    } else {
                        1
                    },
                    item,
                    false,
                    item.comment.content,
                    instance = "https://fake.instance",
                    false,
                    false,
                    false,
                    viewLifecycleOwner,
                    item.creator.id,
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, _ -> }
                )
            }
        }

        init {
            adapterHelper.setItems(items, this)
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

    }

    override fun onPositiveClick(dialog: AlertDialogFragment, tag: String?) {
        if (tag == "reset_post_to_default_styles") {
            viewModel.resetPostUiConfig()
        } else if (tag == "reset_comment_to_default_styles") {
            viewModel.resetCommentUiConfig()
        }
    }

    override fun onNegativeClick(dialog: AlertDialogFragment, tag: String?) {

    }
}