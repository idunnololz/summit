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
import com.idunnololz.summit.databinding.ListingItemFullWithCardsBinding
import com.idunnololz.summit.databinding.ListingItemLargeListBinding
import com.idunnololz.summit.databinding.ListingItemListBinding
import com.idunnololz.summit.databinding.ListingItemListWithCardsBinding
import com.idunnololz.summit.databinding.SearchResultPostItemBinding
import com.idunnololz.summit.preferences.PostsInFeedQuickActionsSettings
import com.idunnololz.summit.view.LemmyHeaderView

class ListingItemViewHolder(
  val rawBinding: ViewBinding,
  val root: View,
  val contentView: View,
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
  val fullContentContainerView: ViewGroup?,
  val highlightBg: View,
  val layoutShowsFullContent: Boolean,
  val themeColorBar: View,
  val createCommentButton: View?,
  val moreButton: View? = null,
  val linkText: TextView? = null,
  val linkIcon: View? = null,
  val linkOverlay: View? = null,
  var quickActionViews: List<View>? = null,
  var actionButtons: List<ImageView>? = null,
) : RecyclerView.ViewHolder(root) {

  data class State(
    var preferImagesAtEnd: Boolean = false,
    var preferFullSizeImages: Boolean = true,
    var preferTitleText: Boolean = false,
    var preferUpAndDownVotes: Boolean? = null,
    var postsInFeedQuickActions: PostsInFeedQuickActionsSettings? = null,
  )

  var state = State()

  companion object {
    fun fromBinding(binding: ListingItemListBinding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.contentView,
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
      fullContentContainerView = binding.fullContent,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = false,
      createCommentButton = null,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: SearchResultPostItemBinding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.contentView,
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
      fullContentContainerView = binding.fullContent,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = false,
      createCommentButton = null,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: ListingItemLargeListBinding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.contentView,
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
      fullContentContainerView = null,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = false,
      createCommentButton = null,
      moreButton = null,
      linkText = binding.linkText,
      linkIcon = binding.linkIcon,
      linkOverlay = binding.linkOverlay,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: ListingItemCardBinding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.cardView,
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
      fullContentContainerView = null,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = false,
      createCommentButton = null,
      moreButton = null,
      linkText = binding.linkText,
      linkIcon = binding.linkIcon,
      linkOverlay = binding.linkOverlay,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: ListingItemCard2Binding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.cardView,
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
      fullContentContainerView = null,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = false,
      createCommentButton = null,
      moreButton = null,
      linkText = binding.linkText,
      linkIcon = binding.linkIcon,
      linkOverlay = binding.linkOverlay,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: ListingItemCard3Binding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.cardView,
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
      fullContentContainerView = null,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = false,
      createCommentButton = null,
      moreButton = null,
      linkText = binding.linkText,
      linkIcon = binding.linkIcon,
      linkOverlay = binding.linkOverlay,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: ListingItemFullBinding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.contentView,
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
      fullContentContainerView = binding.fullContent,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = true,
      createCommentButton = null,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: ListingItemFullWithCardsBinding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.contentView,
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
      fullContentContainerView = binding.fullContent,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = true,
      createCommentButton = null,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: ListingItemCompactBinding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.contentView,
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
      fullContentContainerView = binding.fullContent,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = false,
      createCommentButton = null,
      moreButton = binding.moreButton,
      themeColorBar = binding.themeColorBar,
    )

    fun fromBinding(binding: ListingItemListWithCardsBinding) = ListingItemViewHolder(
      rawBinding = binding,
      root = binding.root,
      contentView = binding.cardView,
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
      fullContentContainerView = binding.fullContent,
      highlightBg = binding.highlightBg,
      layoutShowsFullContent = false,
      createCommentButton = null,
      themeColorBar = binding.themeColorBar,
    )
  }
}
