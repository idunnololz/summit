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
    override var upvoteButton: View?,
    var downvoteCount: TextView?,
    override var downvoteButton: View?,
    override var quickActionsBar: ViewGroup?,
    override var actionButtons: List<ImageView>,
    val actionsContainer: ViewGroup?,
    override val quickActionsStartBarrier: View,
    override val quickActionsEndBarrier: View? = null,
    override val quickActionsTopBarrier: View,

    override var qaScoreCount: TextView? = null,
    override var qaUpvoteCount: TextView? = null,
    override var qaDownvoteCount: TextView? = null,
) : RecyclerView.ViewHolder(root), QuickActionsViewHolder {

    data class State(
        var preferUpAndDownVotes: Boolean? = null,
    )

    var state = State()

    companion object {
        fun fromBinding(binding: PostCommentExpandedItemBinding) = CommentExpandedViewHolder(
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
            quickActionsStartBarrier = binding.startBarrier,
            quickActionsTopBarrier = binding.bottomBarrier,
        )
    }
}
