package com.idunnololz.summit.lemmy.postListView

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.idunnololz.summit.databinding.ListingItemCard2Binding
import com.idunnololz.summit.databinding.ListingItemCard3Binding
import com.idunnololz.summit.databinding.ListingItemCardBinding
import com.idunnololz.summit.databinding.ListingItemCompactBinding
import com.idunnololz.summit.databinding.ListingItemFullBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.ListingItemListWithCardsBinding
import com.idunnololz.summit.databinding.SearchResultPostItemBinding
import com.idunnololz.summit.view.LemmyHeaderView

class ListingItemViewHolder(
    val rawBinding: ViewBinding,
    val root: View,
    val headerContainer: LemmyHeaderView,
    val imageView: ImageView?,
    val title: TextView,
    var commentText: TextView?,
    var commentButton: View?,
    var upvoteCount: TextView?,
    var upvoteButton: View?,
    var downvoteCount: TextView?,
    var downvoteButton: View?,
    val iconImage: ImageView?,
    val openLinkButton: View?,
    val fullContentContainerView: ViewGroup?,
    val highlightBg: View,
    val layoutShowsFullContent: Boolean,
    val createCommentButton: View?,
    val moreButton: View? = null,
    val linkText: TextView? = null,
    val linkIcon: View? = null,
    val linkOverlay: View? = null,
) : RecyclerView.ViewHolder(root) {

    data class State(
        var preferImagesAtEnd: Boolean = false,
        var preferFullSizeImages: Boolean = true,
        var preferTitleText: Boolean = false,
        var preferUpAndDownVotes: Boolean? = null,
    )

    var state = State()

    companion object {
        fun fromBinding(binding: ListingItemListBinding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = binding.image,
            title = binding.title,
            commentText = null,
            commentButton = null,
            upvoteCount = null,
            upvoteButton = null,
            downvoteCount = null,
            downvoteButton = null,
            iconImage = binding.iconImage,
            openLinkButton = null,
            fullContentContainerView = binding.fullContent,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = false,
            createCommentButton = null,
        )

        fun fromBinding(binding: SearchResultPostItemBinding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = binding.image,
            title = binding.title,
            commentText = null,
            commentButton = null,
            upvoteCount = null,
            upvoteButton = null,
            downvoteCount = null,
            downvoteButton = null,
            iconImage = binding.iconImage,
            openLinkButton = null,
            fullContentContainerView = binding.fullContent,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = false,
            createCommentButton = null,
        )

        fun fromBinding(binding: ListingItemLargeListBinding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = binding.image,
            title = binding.title,
            commentText = null,
            commentButton = null,
            upvoteCount = null,
            upvoteButton = null,
            downvoteCount = null,
            downvoteButton = null,
            iconImage = null,
            openLinkButton = null,
            fullContentContainerView = null,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = false,
            createCommentButton = null,
            moreButton = null,
            linkText = binding.linkText,
            linkIcon = binding.linkIcon,
            linkOverlay = binding.linkOverlay,
        )

        fun fromBinding(binding: ListingItemCardBinding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = binding.image,
            title = binding.title,
            commentText = null,
            commentButton = null,
            upvoteCount = null,
            upvoteButton = null,
            downvoteCount = null,
            downvoteButton = null,
            iconImage = null,
            openLinkButton = null,
            fullContentContainerView = null,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = false,
            createCommentButton = null,
            moreButton = null,
            linkText = binding.linkText,
            linkIcon = binding.linkIcon,
            linkOverlay = binding.linkOverlay,
        )

        fun fromBinding(binding: ListingItemCard2Binding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = binding.image,
            title = binding.title,
            commentText = null,
            commentButton = null,
            upvoteCount = null,
            upvoteButton = null,
            downvoteCount = null,
            downvoteButton = null,
            iconImage = null,
            openLinkButton = null,
            fullContentContainerView = null,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = false,
            createCommentButton = null,
            moreButton = null,
            linkText = binding.linkText,
            linkIcon = binding.linkIcon,
            linkOverlay = binding.linkOverlay,
        )

        fun fromBinding(binding: ListingItemCard3Binding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = binding.image,
            title = binding.title,
            commentText = null,
            commentButton = null,
            upvoteCount = null,
            upvoteButton = null,
            downvoteCount = null,
            downvoteButton = null,
            iconImage = null,
            openLinkButton = null,
            fullContentContainerView = null,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = false,
            createCommentButton = null,
            moreButton = null,
            linkText = binding.linkText,
            linkIcon = binding.linkIcon,
            linkOverlay = binding.linkOverlay,
        )

        fun fromBinding(binding: ListingItemFullBinding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = null,
            title = binding.title,
            commentText = null,
            commentButton = null,
            upvoteCount = null,
            upvoteButton = null,
            downvoteCount = null,
            downvoteButton = null,
            iconImage = null,
            openLinkButton = null,
            fullContentContainerView = binding.fullContent,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = true,
            createCommentButton = null,
        )

        fun fromBinding(binding: ListingItemCompactBinding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = binding.image,
            title = binding.title,
            commentText = binding.commentText,
            commentButton = null,
            upvoteCount = binding.scoreText,
            upvoteButton = binding.upvoteButton,
            downvoteCount = null,
            downvoteButton = binding.downvoteButton,
            iconImage = binding.iconImage,
            openLinkButton = null,
            fullContentContainerView = binding.fullContent,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = false,
            createCommentButton = null,
            moreButton = binding.moreButton,
        )

        fun fromBinding(binding: ListingItemListWithCardsBinding) = ListingItemViewHolder(
            rawBinding = binding,
            root = binding.root,
            headerContainer = binding.headerContainer,
            imageView = binding.image,
            title = binding.title,
            commentText = null,
            commentButton = null,
            upvoteCount = null,
            upvoteButton = null,
            downvoteCount = null,
            downvoteButton = null,
            iconImage = binding.iconImage,
            openLinkButton = null,
            fullContentContainerView = binding.fullContent,
            highlightBg = binding.highlightBg,
            layoutShowsFullContent = false,
            createCommentButton = null,
        )
    }
}
