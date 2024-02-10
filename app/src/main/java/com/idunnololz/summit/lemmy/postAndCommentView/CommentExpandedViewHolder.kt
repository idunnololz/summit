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
    val root: ConstraintLayout,
    val highlightBg: View,
    val threadLinesSpacer: Space,
    val progressBar: ProgressBar,
    val headerView: LemmyHeaderView,
    val collapseSectionButton: View,
    val topHotspot: View,
    val leftHotspot: View,
    val mediaContainer: ConstraintLayout,
    val overlay: View,
    val text: TextView,
    val startGuideline: View,
    var scoreCount: TextView?,
    var upvoteCount: TextView?,
    var upvoteButton: View?,
    var downvoteCount: TextView?,
    var downvoteButton: View?,
    var quickActionsBar: ViewGroup?,
    var actionButtons: List<ImageView>,
    val actionsContainer: ViewGroup?,

    var actionsDivider1: View? = null,
    var actionsDivider2: View? = null,
    var scoreCount2: TextView? = null,
    var upvoteCount2: TextView? = null,
    var downvoteCount2: TextView? = null,
) : RecyclerView.ViewHolder(root) {

    data class State(
        var preferUpAndDownVotes: Boolean? = null,
    )

    var state = State()

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
                leftHotspot = binding.leftHotspot,
                mediaContainer = binding.mediaContainer,
                overlay = binding.overlay,
                text = binding.text,
                startGuideline = binding.startGuideline,
                scoreCount = null,
                upvoteCount = null,
                upvoteButton = null,
                downvoteCount = null,
                downvoteButton = null,
                actionButtons = listOf(),
                quickActionsBar = null,
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
                leftHotspot = binding.leftHotspot,
                mediaContainer = binding.mediaContainer,
                overlay = binding.overlay,
                text = binding.text,
                startGuideline = binding.startGuideline,
                scoreCount = binding.headerView.textView2,
                upvoteCount = null,
                upvoteButton = null,
                downvoteCount = null,
                downvoteButton = null,
                actionButtons = listOf(),
                quickActionsBar = null,
                actionsContainer = binding.actionsContainer,
            )
    }
}
