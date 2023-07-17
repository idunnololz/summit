package com.idunnololz.summit.lemmy.postListView

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.button.MaterialButton
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.view.LemmyHeaderView

class ListingItemViewHolder(
    val rawBinding: ViewBinding,
    val root: View,
    val headerContainer: LemmyHeaderView,
    val image: ImageView?,
    val title: TextView,
    val commentText: TextView,
    val commentButton: MaterialButton?,
    val upvoteCount: TextView,
    val upvoteButton: ImageView?,
    val downvoteButton: ImageView?,
    val linkTypeImage: ImageView?,
    val iconImage: ImageView?,
    val openLinkButton: View?,
    val fullContentContainerView: ViewGroup?,
    val highlightBg: View,
    val layoutShowsFullContent: Boolean,
    val createCommentButton: View?,
    val moreButton: View? = null,
) : RecyclerView.ViewHolder(root) {

    data class State(
        var preferImagesAtEnd: Boolean = false,
        var preferFullSizeImages: Boolean = false,
    )

    var state = State()

    companion object {
        fun fromBinding(binding: ListingItemListBinding) =
            ListingItemViewHolder(
                rawBinding = binding,
                root = binding.root,
                headerContainer = binding.headerContainer,
                image = binding.image,
                title = binding.title,
                commentText = binding.commentButton,
                commentButton = binding.commentButton,
                upvoteCount = binding.upvoteCount,
                upvoteButton = binding.upvoteButton,
                downvoteButton = binding.downvoteButton,
                linkTypeImage = binding.linkTypeImage,
                iconImage = binding.iconImage,
                openLinkButton = null,
                fullContentContainerView = binding.fullContent,
                highlightBg = binding.highlightBg,
                layoutShowsFullContent = false,
                createCommentButton = null,
            )

        fun fromBinding(binding: ListingItemCardBinding) =
            ListingItemViewHolder(
                rawBinding = binding,
                root = binding.root,
                headerContainer = binding.headerContainer,
                image = binding.image,
                title = binding.title,
                commentText = binding.commentButton,
                commentButton = binding.commentButton,
                upvoteCount = binding.upvoteCount,
                upvoteButton = binding.upvoteButton,
                downvoteButton = binding.downvoteButton,
                linkTypeImage = null,
                iconImage = null,
                openLinkButton = binding.openLinkButton,
                fullContentContainerView = null,
                highlightBg = binding.highlightBg,
                layoutShowsFullContent = false,
                createCommentButton = null,
            )

        fun fromBinding(binding: ListingItemFullBinding) =
            ListingItemViewHolder(
                rawBinding = binding,
                root = binding.root,
                headerContainer = binding.headerContainer,
                image = null,
                title = binding.title,
                commentText = binding.commentButton,
                commentButton = binding.commentButton,
                upvoteCount = binding.upvoteCount,
                upvoteButton = binding.upvoteButton,
                downvoteButton = binding.downvoteButton,
                linkTypeImage = null,
                iconImage = null,
                openLinkButton = null,
                fullContentContainerView = binding.fullContent,
                highlightBg = binding.highlightBg,
                layoutShowsFullContent = true,
                createCommentButton = null,
            )

        fun fromBinding(binding: ListingItemCompactBinding) =
            ListingItemViewHolder(
                rawBinding = binding,
                root = binding.root,
                headerContainer = binding.headerContainer,
                image = binding.image,
                title = binding.title,
                commentText = binding.commentText,
                commentButton = null,
                upvoteCount = binding.scoreText,
                upvoteButton = binding.upvoteButton,
                downvoteButton = binding.downvoteButton,
                linkTypeImage = binding.linkTypeImage,
                iconImage = binding.iconImage,
                openLinkButton = null,
                fullContentContainerView = binding.fullContent,
                highlightBg = binding.highlightBg,
                layoutShowsFullContent = false,
                createCommentButton = null,
                moreButton = binding.moreButton,
            )
    }
}