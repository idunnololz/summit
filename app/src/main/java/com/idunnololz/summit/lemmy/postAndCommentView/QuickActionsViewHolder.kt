package com.idunnololz.summit.lemmy.postAndCommentView

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

interface QuickActionsViewHolder {
    var quickActionsBar: ViewGroup?
    val quickActionsStartBarrier: View?
    val quickActionsEndBarrier: View?
    val quickActionsTopBarrier: View

    var scoreCount2: TextView?
    var upvoteCount2: TextView?
    var downvoteCount2: TextView?
    var upvoteButton: View?
    var downvoteButton: View?
    var actionButtons: List<ImageView>
}

class GeneralQuickActionsViewHolder(
    val root: ViewGroup,
    override val quickActionsTopBarrier: View,
    override val quickActionsStartBarrier: View? = null,
    override val quickActionsEndBarrier: View? = null,
    override var upvoteButton: View? = null,
    override var downvoteButton: View? = null,
    override var quickActionsBar: ViewGroup? = null,
    override var scoreCount2: TextView? = null,
    override var upvoteCount2: TextView? = null,
    override var downvoteCount2: TextView? = null,
    override var actionButtons: List<ImageView> = listOf(),
) : QuickActionsViewHolder
