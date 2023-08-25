package com.idunnololz.summit.lemmy.postAndCommentView

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.databinding.PostCommentExpandedCompactItemBinding
import com.idunnololz.summit.databinding.PostCommentExpandedItemBinding
import com.idunnololz.summit.view.LemmyHeaderView

class CommentExpandedViewHolder(
    val rawBinding: ViewBinding,
    val root: View,
    val highlightBg: View,
    val threadLinesSpacer: Space,
    val progressBar: ProgressBar,
    val headerView: LemmyHeaderView,
    val collapseSectionButton: View,
    val topHotspot: View,
    val mediaContainer: ConstraintLayout,
    val overlay: View,
    val text: TextView,
    val scoreCount: TextView,
    var upvoteCount: TextView?,
    val upvoteButton: ImageView?,
    var downvoteCount: TextView?,
    val downvoteButton: ImageView?,
    val commentButton: View?,
    val moreButton: View?,
    val actionsContainer: ViewGroup?,
) : RecyclerView.ViewHolder(root) {

    companion object {
        fun fromBinding(binding: PostCommentExpandedItemBinding) =
            CommentExpandedViewHolder(
                rawBinding = binding,
                root = binding.root,
                highlightBg = binding.highlightBg,
                threadLinesSpacer = binding.threadLinesSpacer,
                progressBar = binding.progressBar,
                headerView = binding.headerView,
                collapseSectionButton = binding.collapseSectionButton,
                topHotspot = binding.topHotspot,
                mediaContainer = binding.mediaContainer,
                overlay = binding.overlay,
                text = binding.text,
                scoreCount = binding.upvoteCount,
                upvoteCount = null,
                upvoteButton = binding.upvoteButton,
                downvoteCount = null,
                downvoteButton = binding.downvoteButton,
                commentButton = binding.commentButton,
                moreButton = binding.moreButton,
                actionsContainer = null,
            )
        fun fromBinding(binding: PostCommentExpandedCompactItemBinding) =
            CommentExpandedViewHolder(
                rawBinding = binding,
                root = binding.root,
                highlightBg = binding.highlightBg,
                threadLinesSpacer = binding.threadLinesSpacer,
                progressBar = binding.progressBar,
                headerView = binding.headerView,
                collapseSectionButton = binding.collapseSectionButton,
                topHotspot = binding.topHotspot,
                mediaContainer = binding.mediaContainer,
                overlay = binding.overlay,
                text = binding.text,
                scoreCount = binding.headerView.textView2,
                upvoteCount = null,
                upvoteButton = null,
                downvoteCount = null,
                downvoteButton = null,
                commentButton = null,
                moreButton = null,
                actionsContainer = binding.actionsContainer,
            )
    }
}
