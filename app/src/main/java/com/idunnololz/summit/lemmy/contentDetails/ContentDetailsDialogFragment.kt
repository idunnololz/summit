package com.idunnololz.summit.lemmy.contentDetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.Comment
import com.idunnololz.summit.api.dto.CommentAggregates
import com.idunnololz.summit.api.dto.CommentView
import com.idunnololz.summit.api.dto.Community
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.Post
import com.idunnololz.summit.api.dto.PostAggregates
import com.idunnololz.summit.api.dto.PostView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.DialogFragmentCommentDetailsBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.postListView.PostListViewBuilder
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.dateStringToFullDateTime
import com.idunnololz.summit.util.ext.getColorFromAttribute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ContentDetailsDialogFragment : BaseDialogFragment<DialogFragmentCommentDetailsBinding>() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            instance: String,
            commentView: CommentView,
        ) {
            ContentDetailsDialogFragment().apply {
                arguments = ContentDetailsDialogFragmentArgs(
                    instance = instance,
                    commentView = commentView,
                    postView = null,
                ).toBundle()
            }.show(fragmentManager, "CommentDetailsDialogFragment")
        }

        fun show(
            fragmentManager: FragmentManager,
            instance: String,
            postView: PostView,
        ) {
            ContentDetailsDialogFragment().apply {
                arguments = ContentDetailsDialogFragmentArgs(
                    instance = instance,
                    commentView = null,
                    postView = postView,
                ).toBundle()
            }.show(fragmentManager, "CommentDetailsDialogFragment")
        }
    }

    private val args by navArgs<ContentDetailsDialogFragmentArgs>()

    @Inject
    lateinit var postListViewBuilder: PostListViewBuilder

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            val window = checkNotNull(dialog.window)
            window.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentCommentDetailsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val commentView = args.commentView
        val postView = args.postView

        if (commentView != null) {
            val objectView = ObjectView(
                counts = commentView.counts.toCounts(),
                creator = commentView.creator,
                content = commentView.comment.toContent(),
                fullDetails = buildString {
                    appendLine(
                        "Published on ${
                        dateStringToFullDateTime(commentView.comment.published)}",
                    )
                    if (commentView.comment.updated != null) {
                        appendLine(
                            "Updated on ${
                            dateStringToFullDateTime(commentView.comment.updated)}",
                        )
                    }

                    appendLine()

                    appendCommunityInfo(commentView.community)

                    appendLine()

                    appendAuthorInfo(
                        commentView.creator,
                        commentView.creator_is_moderator == true,
                        commentView.creator_is_admin == true,
                        commentView.creator_banned_from_community,
                    )

                    appendLine()

                    appendLine("Raw content")
                    appendLine(commentView.comment.content)
                },
            )

            showObject(objectView)
        } else if (postView != null) {
            val objectView = ObjectView(
                counts = postView.counts.toCounts(),
                creator = postView.creator,
                content = postView.post.toContent(),
                fullDetails = buildString {
                    appendLine(
                        "Published on ${
                        dateStringToFullDateTime(postView.post.published)}",
                    )
                    if (postView.post.updated != null) {
                        appendLine(
                            "Updated on ${
                            dateStringToFullDateTime(postView.post.updated)}",
                        )
                    }

                    appendLine()

                    appendCommunityInfo(postView.community)

                    appendLine()

                    appendAuthorInfo(
                        postView.creator,
                        postView.creator_is_moderator == true,
                        postView.creator_is_admin == true,
                        postView.creator_banned_from_community,
                    )

                    appendLine()

                    appendLine("Raw content")
                    appendLine(postView.post.name)
                    if (!postView.post.body.isNullOrBlank()) {
                        appendLine()
                        appendLine(postView.post.body)
                    }
                    if (!postView.post.url.isNullOrBlank()) {
                        appendLine()
                        appendLine(postView.post.url)
                    }
                },
            )

            showObject(objectView)
        } else {
            dismiss()
        }
    }

    private fun showObject(o: ObjectView) {
        val context = requireContext()

        with(binding) {
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(
                context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
            )
            toolbar.setNavigationOnClickListener {
                dismiss()
            }

            if (o.creator.avatar != null) {
                icon.load(o.creator.avatar)
            } else {
                icon.visibility = View.GONE
            }

            name.text = o.creator.name

            if (o.content.imageUrl == null) {
                image.visibility = View.GONE
            } else {
                image.visibility = View.VISIBLE

                image.viewTreeObserver.addOnGlobalLayoutListener(
                    object : OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            image.viewTreeObserver.removeOnGlobalLayoutListener(this)

                            postListViewBuilder.loadImage(
                                root,
                                image,
                                o.content.imageUrl,
                                true,
                                binding.image.width,
                                false,
                            ) {}
                        }
                    },
                )
            }

            if (o.content.content.isNullOrBlank()) {
                text.visibility = View.GONE
            } else {
                text.visibility = View.VISIBLE
                LemmyTextHelper.bindText(
                    text,
                    o.content.content,
                    args.instance,
                    onImageClick = { url ->
//                    getMainActivity()?.openImage(null, binding.appBar, null, url, null)
                    },
                    onVideoClick = { url ->
//                    getMainActivity()?.openVideo(url, VideoType.UNKNOWN, null)
                    },
                    onPageClick = {
//                    getMainActivity()?.launchPage(it)
                    },
                    onLinkClick = { url, text, linkType ->
//                    onLinkClick(url, text, linkType)
                    },
                    onLinkLongClick = { url, text ->
//                    getMainActivity()?.showBottomMenuForLink(url, text)
                    },
                )
            }

            val voteCount = o.counts.upvotes + o.counts.downvotes

            upvotes.text = PrettyPrintUtils.defaultDecimalFormat.format(o.counts.upvotes)
            downvotes.text = PrettyPrintUtils.defaultDecimalFormat.format(o.counts.downvotes)
            upvoted.text =
                if (voteCount > 0) {
                    PrettyPrintUtils.defaultPercentFormat.format(
                        o.counts.upvotes / voteCount.toDouble(),
                    )
                } else {
                    getString(R.string.na)
                }

            showMoreDetails.setOnClickListener {
                TransitionManager.beginDelayedTransition(root)

                fullDetails.setTextIsSelectable(true)
                fullDetails.text = o.fullDetails.trim()
                fullDetailsContainer.visibility = View.VISIBLE

                showMoreDetails.visibility = View.GONE
            }
        }
    }

    private fun StringBuilder.appendCommunityInfo(community: Community) {
        appendLine("Posted on ${community.instance}")
    }

    private fun StringBuilder.appendAuthorInfo(
        person: Person,
        isMod: Boolean,
        isAdmin: Boolean,
        isBanned: Boolean,
    ) {
        appendLine("Posted from ${person.instance}")
        if (isMod) {
            appendLine("Posted by a moderator.")
        }
        if (isAdmin) {
            appendLine("Posted by an admin.")
        }
        if (isBanned) {
            appendLine("Poster is banned from this community.")
        }
    }

    private fun CommentAggregates.toCounts(): Counts =
        Counts(
            id = id,
            score = score,
            upvotes = upvotes,
            downvotes = downvotes,
            published = published,
            childCount = child_count,
        )

    private fun PostAggregates.toCounts(): Counts =
        Counts(
            id = id,
            score = score,
            upvotes = upvotes,
            downvotes = downvotes,
            published = published,
            childCount = comments,
        )

    private fun Comment.toContent(): Content =
        Content(
            content = content,
            imageUrl = null,
        )

    private fun Post.toContent(): Content =
        Content(
            content = body,
            imageUrl = this.thumbnail_url ?: this.url,
        )

    data class ObjectView(
        val counts: Counts,
        val creator: Person,
        val content: Content,
        val fullDetails: String,
    )

    data class Counts(
        val id: Int,
        val score: Int,
        val upvotes: Int,
        val downvotes: Int,
        val published: String,
        val childCount: Int,
    )

    data class Content(
        val content: String?,
        val imageUrl: String?,
    )
}
