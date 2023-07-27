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
    val upvoteCount: TextView,
    val upvoteButton: ImageView?,
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
                binding.highlightBg,
                binding.threadLinesSpacer,
                binding.progressBar,
                binding.headerView,
                binding.collapseSectionButton,
                binding.topHotspot,
                binding.mediaContainer,
                binding.overlay,
                binding.text,
                binding.upvoteCount,
                binding.upvoteButton,
                binding.downvoteButton,
                binding.commentButton,
                binding.moreButton,
                null,
            )
        fun fromBinding(binding: PostCommentExpandedCompactItemBinding) =
            CommentExpandedViewHolder(
                rawBinding = binding,
                root = binding.root,
                binding.highlightBg,
                binding.threadLinesSpacer,
                binding.progressBar,
                binding.headerView,
                binding.collapseSectionButton,
                binding.topHotspot,
                binding.mediaContainer,
                binding.overlay,
                binding.text,
                binding.headerView.textView2,
                null,
                null,
                null,
                null,
                binding.actionsContainer,
            )
    }
}
