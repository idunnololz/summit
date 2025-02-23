package com.idunnololz.summit.main

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import coil.load
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.loadProfileImageOrDefault
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.CustomAppBarLargeBinding
import com.idunnololz.summit.databinding.CustomAppBarSmallBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.community.CommunityFragmentDirections
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoViewModel
import com.idunnololz.summit.lemmy.instancePicker.InstancePickerDialogFragment
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toUrl
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getDimenFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.relativeTimeToConcise
import com.idunnololz.summit.util.shimmer.newShimmerDrawableSquare
import com.idunnololz.summit.util.showMoreLinkOptions

class LemmyAppBarController(
    private val mainActivity: MainActivity,
    private val parentContainer: CoordinatorLayout,
    private val accountInfoManager: AccountInfoManager,
    private val communityInfoViewModel: CommunityInfoViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val avatarHelper: AvatarHelper,
    useHeader: Boolean,
    state: State? = null,
) {

    companion object {

        private const val TAG = "LemmyAppBarController"
    }

    data class State(
        var currentCommunity: CommunityRef? = null,
        var defaultCommunity: CommunityRef? = null,
    )

    private val context = mainActivity

    private var vh: ViewHolder

    private var isScrimListenerEnabled = false
    private var isToolbarElementsVisible = true
    private var currentOffset = 0
    private val onOffsetChangedListener = object : OnOffsetChangedListener {

        override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
            currentOffset = verticalOffset

            if (!isScrimListenerEnabled) {
                return
            }

            updateToolbarVisibility(animate = true)
        }
    }

    val state: State = state ?: State()
    val appBarRoot
        get() = vh.root
    val percentShown = MutableLiveData<Float>(0f)

    private sealed interface ViewHolder {
        val root: View
        val customActionBar: ViewGroup
        val communityTextView: TextView
        val pageTextView: TextView
        val customAppBar: AppBarLayout
        val accountImageView: ShapeableImageView
        val communitySortOrder: TextView
        val collapsingToolbarLayout: CollapsingToolbarLayout
        val toolbar: MaterialToolbar

        fun setSortOrderText(text: String)
        fun setToolbarTopPadding(padding: Int)

        class LargeAppBarViewHolder(
            override val root: View,
            override val customActionBar: ViewGroup,
            override val communityTextView: TextView,
            override val pageTextView: TextView,
            override val customAppBar: AppBarLayout,
            override val accountImageView: ShapeableImageView,
            override val communitySortOrder: TextView,
            override val collapsingToolbarLayout: CollapsingToolbarLayout,
            override val toolbar: MaterialToolbar,
            val title: TextView,
            val body: TextView,
            val banner: ImageView,
            val communitySortOrder2: TextView,
            val toolbarPlaceholder: View,
            val icon: ImageView,
        ): ViewHolder {
            override fun setSortOrderText(text: String) {
                communitySortOrder.text = text
                communitySortOrder2.text = text
            }

            override fun setToolbarTopPadding(padding: Int) {
                toolbar.updatePadding(top = padding)
                toolbarPlaceholder.updateLayoutParams<MarginLayoutParams> {
                    topMargin = padding
                }
            }
        }

        class SmallAppBarViewHolder(
            override val root: View,
            override val customActionBar: ViewGroup,
            override val communityTextView: TextView,
            override val pageTextView: TextView,
            override val customAppBar: AppBarLayout,
            override val accountImageView: ShapeableImageView,
            override val communitySortOrder: TextView,
            override val collapsingToolbarLayout: CollapsingToolbarLayout,
            override val toolbar: MaterialToolbar,
        ): ViewHolder {
            override fun setSortOrderText(text: String) {
                communitySortOrder.text = text
            }

            override fun setToolbarTopPadding(padding: Int) {
                toolbar.updatePadding(top = padding)
            }
        }
    }
    
    init {
        vh = ensureViewHolder(useHeader, force = true)

        mainActivity.apply {
            insets.observe(viewLifecycleOwner) {
                val toolbarHeight = context.getDimenFromAttribute(
                    androidx.appcompat.R.attr.actionBarSize).toInt()
                val newToolbarHeight = toolbarHeight + it.topInset
                vh.setToolbarTopPadding(it.topInset)
                vh.collapsingToolbarLayout.scrimVisibleHeightTrigger =
                    (newToolbarHeight + Utils.convertDpToPixel(16f)).toInt()
            }
        }
        vh.customAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            val toolbarHeight = vh.toolbar.height
            val restOfAppBarHeight = vh.root.height - toolbarHeight
            val realOffset = verticalOffset + restOfAppBarHeight
            val percentShown = -realOffset.toFloat() / toolbarHeight

            this@LemmyAppBarController.percentShown.value = percentShown
        }
    }

    fun setup(
        communitySelectedListener: CommunitySelectedListener,
        onAccountClick: (currentAccount: Account?) -> Unit,
        onSortOrderClick: () -> Unit,
        onChangeInstanceClick: () -> Unit,
        onCommunityLongClick: (currentCommunity: CommunityRef?, text: String?) -> Boolean,
    ) {
        val vh = vh
        fun showCommunitySelectorInternal() {
            val controller = mainActivity.showCommunitySelector(state.currentCommunity)
            controller.onCommunitySelectedListener = communitySelectedListener
            controller.onChangeInstanceClick = onChangeInstanceClick
        }

        vh.customAppBar.addOnOffsetChangedListener(onOffsetChangedListener)
        vh.accountImageView.setOnClickListener {
            val account = it.tag as? Account
            onAccountClick(account)
        }
        vh.communityTextView.setOnClickListener {
            showCommunitySelectorInternal()
        }
        vh.communityTextView.setOnLongClickListener {
            onCommunityLongClick(state.currentCommunity, vh.communityTextView.text?.toString())
        }

        if (vh is ViewHolder.LargeAppBarViewHolder) {
            vh.title.setOnClickListener {
                showCommunitySelectorInternal()
            }
            vh.title.setOnLongClickListener {
                onCommunityLongClick(state.currentCommunity, vh.communityTextView.text?.toString())
            }
            vh.communitySortOrder2.setOnClickListener {
                onSortOrderClick()
            }
        }
        vh.customActionBar.setOnClickListener {
            showCommunitySelectorInternal()
        }
        vh.communitySortOrder.setOnClickListener {
            onSortOrderClick()
        }
        communityInfoViewModel.siteOrCommunity.observe(viewLifecycleOwner) {
            updateCommunityButton()
        }
    }

    fun showCommunitySelector() {
        vh.communityTextView.performClick()
    }

    fun setCommunity(communityRef: CommunityRef?) {
        if (state.currentCommunity == communityRef) {
            return
        }

        state.currentCommunity = communityRef

        updateCommunityButton()
    }

    fun setDefaultCommunity(defaultCommunity: CommunityRef?) {
        if (state.defaultCommunity == defaultCommunity) {
            return
        }

        state.defaultCommunity = defaultCommunity

        updateCommunityButton()
    }

    private fun updateCommunityButton() {
        val vh = vh
        val currentCommunity = state.currentCommunity
        val communityName = currentCommunity?.getName(context) ?: ""
        vh.communityTextView.text = communityName
        val isHome = currentCommunity == state.defaultCommunity
        val fullAccount = accountInfoManager.currentFullAccount.value
        val isSubscribed = fullAccount?.accountInfo?.subscriptions?.any {
            it.toCommunityRef() == currentCommunity
        } == true

        if (isHome) {
            vh.communityTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.outline_home_18,
                0,
                0,
                0,
            )
        } else if (currentCommunity is CommunityRef.MultiCommunity) {
            vh.communityTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.outline_dynamic_feed_18,
                0,
                0,
                0,
            )
        } else if (isSubscribed) {
            vh.communityTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.outline_mail_18,
                0,
                0,
                0,
            )
        } else {
            vh.communityTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                0,
                0,
            )
        }

        if (vh is ViewHolder.LargeAppBarViewHolder) {
            vh.title.text = communityName
            when (val value = communityInfoViewModel.siteOrCommunity.value) {
                is StatefulData.Error -> {}
                is StatefulData.Loading -> {
                    vh.banner.load("file:///android_asset/banner_placeholder.svg") {
                        allowHardware(false)
                    }
                    vh.icon.load(newShimmerDrawableSquare(context))
                    vh.body.visibility = View.GONE
                }

                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    val bannerUrl = value.data.response
                        .fold(
                            {
                                avatarHelper.loadInstanceIcon(vh.icon, it.site_view)
                                it.site_view.site.banner
                            },
                            {
                                avatarHelper.loadCommunityIcon(vh.icon, it.community_view.community)
                                it.community_view.community.banner
                            }
                        )
                    val body = value.data.response
                        .fold(
                            { it.site_view.site.description },
                            { it.community_view.community.description }
                        )

                    if (body.isNullOrBlank()) {
                        vh.body.visibility = View.GONE
                    } else {
                        vh.body.visibility = View.VISIBLE
                        vh.body.text = body
                    }

                    if (bannerUrl == null) {
                        vh.banner.load("file:///android_asset/banner_placeholder.svg") {
                            allowHardware(false)
                        }
                    } else {
                        vh.banner.load(bannerUrl) {
                            allowHardware(false)
                        }
                    }
                }
            }
        }

        if (currentCommunity != null) {
            communityInfoViewModel.onCommunityChanged(currentCommunity)
        }
    }

    fun setPageIndex(pageIndex: Int, onPageSelectedListener: (pageIndex: Int) -> Unit) {
        vh.communitySortOrder.visibility = View.GONE

        vh.pageTextView.text = context.getString(R.string.page_format, (pageIndex + 1).toString())

        vh.pageTextView.setOnClickListener {
            PopupMenu(context, it).apply {
                menu.apply {
                    for (i in 0..pageIndex) {
                        add(0, i, 0, context.getString(R.string.page_format, (i + 1).toString()))
                    }
                }
                setOnMenuItemClickListener {
                    Log.d(TAG, "Page selected: ${it.itemId}")
                    onPageSelectedListener(it.itemId)
                    true
                }
            }.show()
        }
    }

    fun setIsInfinity(isInfinity: Boolean) {
        if (isInfinity) {
            vh.communitySortOrder.visibility = View.VISIBLE
            vh.pageTextView.visibility = View.GONE
        } else {
            vh.communitySortOrder.visibility = View.GONE
            vh.pageTextView.visibility = View.VISIBLE
        }
    }

    fun setUseHeader(useHeader: Boolean) {
        ensureViewHolder(useHeader)
        if (useHeader) {
            updateToolbarVisibility(animate = false)
            isScrimListenerEnabled = true
        } else {
            setToolbarElementsVisibility(visible = true, animate = false)
            isScrimListenerEnabled = false
        }
    }

    fun setSortOrder(communitySortOrder: CommunitySortOrder) {
        val sortOrderText =
            when (communitySortOrder) {
                CommunitySortOrder.Active -> context.getString(R.string.sort_order_active)
                CommunitySortOrder.Hot -> context.getString(R.string.sort_order_hot)
                CommunitySortOrder.MostComments -> context.getString(R.string.sort_order_most_comments)
                CommunitySortOrder.New -> context.getString(R.string.sort_order_new)
                CommunitySortOrder.NewComments -> context.getString(R.string.sort_order_new_comments)
                CommunitySortOrder.Old -> context.getString(R.string.sort_order_old)
                is CommunitySortOrder.TopOrder ->
                    context.getString(
                        R.string.sort_order_top_format,
                        when (communitySortOrder.timeFrame) {
                            CommunitySortOrder.TimeFrame.LastHour ->
                                relativeTimeToConcise(context, 3_600_000)
                            CommunitySortOrder.TimeFrame.LastSixHour ->
                                relativeTimeToConcise(context, 3_600_000 * 6)
                            CommunitySortOrder.TimeFrame.LastTwelveHour ->
                                relativeTimeToConcise(context, 3_600_000 * 12)
                            CommunitySortOrder.TimeFrame.Today ->
                                relativeTimeToConcise(context, 86_400_000)
                            CommunitySortOrder.TimeFrame.ThisWeek ->
                                relativeTimeToConcise(context, 604_800_000)
                            CommunitySortOrder.TimeFrame.ThisMonth ->
                                relativeTimeToConcise(context, 2_592_000_000)
                            CommunitySortOrder.TimeFrame.LastThreeMonth ->
                                relativeTimeToConcise(context, 2_592_000_000 * 3)
                            CommunitySortOrder.TimeFrame.LastSixMonth ->
                                relativeTimeToConcise(context, 2_592_000_000 * 6)
                            CommunitySortOrder.TimeFrame.LastNineMonth ->
                                relativeTimeToConcise(context, 2_592_000_000 * 9)
                            CommunitySortOrder.TimeFrame.ThisYear ->
                                relativeTimeToConcise(context, 31_104_000_000)
                            CommunitySortOrder.TimeFrame.AllTime ->
                                context.getString(R.string.time_frame_all_time)
                        },
                    )
                CommunitySortOrder.Controversial -> context.getString(R.string.sort_order_controversial)
                CommunitySortOrder.Scaled -> context.getString(R.string.sort_order_scaled_short)
            }
        vh.setSortOrderText(sortOrderText)
    }

    fun clearPageIndex() {
        vh.pageTextView.text = ""
        vh.pageTextView.setOnClickListener(null)
    }

    fun onAccountChanged(it: AccountView?) {
        vh.accountImageView.tag = it?.account

        it.loadProfileImageOrDefault(vh.accountImageView)
    }

    fun setExpanded(b: Boolean) {
        vh.customAppBar.setExpanded(b)
    }

    private fun updateToolbarVisibility(animate: Boolean) {
        val vh = vh

        if (vh is ViewHolder.LargeAppBarViewHolder) {
            with(vh.collapsingToolbarLayout) {
                setToolbarElementsVisibility(
                    visible = this.isLaidOut && height + currentOffset < scrimVisibleHeightTrigger,
                    animate = animate
                )
            }
        }
    }

    private fun setToolbarElementsVisibility(visible: Boolean, animate: Boolean) {
        if (visible == isToolbarElementsVisible) {
            return
        }
        isToolbarElementsVisible = visible

        if (animate) {
            if (isToolbarElementsVisible) {
                vh.communityTextView.animate()
                    .alpha(1f)
                vh.communitySortOrder.animate()
                    .alpha(1f)
                vh.pageTextView.animate()
                    .alpha(1f)
            } else {
                vh.communityTextView.animate()
                    .alpha(0f)
                vh.communitySortOrder.animate()
                    .alpha(0f)
                vh.pageTextView.animate()
                    .alpha(0f)
            }
        } else {
            if (isToolbarElementsVisible) {
                vh.communityTextView.alpha = 1f
                vh.communitySortOrder.alpha = 1f
                vh.pageTextView.alpha = 1f
            } else {
                vh.communityTextView.alpha = 0f
                vh.communitySortOrder.alpha = 0f
                vh.pageTextView.alpha = 0f
            }
        }
    }

    private fun ensureViewHolder(useHeader: Boolean, force: Boolean = false): ViewHolder {
        if (!force) {
            if (useHeader && vh is ViewHolder.LargeAppBarViewHolder) {
                return vh
            }
            if (!useHeader && vh is ViewHolder.SmallAppBarViewHolder) {
                return vh
            }
        }

        if (!force) {
            parentContainer.removeView(vh.root)
        }
        vh = createViewHolder(useHeader)
        parentContainer.addView(vh.root, 0)
        return vh
    }

    private fun createViewHolder(useHeader: Boolean): ViewHolder {
        val inflater = LayoutInflater.from(mainActivity)
        return if (useHeader) {
            val b = CustomAppBarLargeBinding.inflate(inflater, parentContainer, false)
            ViewHolder.LargeAppBarViewHolder(
                b.root,
                b.customActionBar,
                b.communityTextView,
                b.pageTextView,
                b.customAppBar,
                b.accountImageView,
                b.communitySortOrder,
                b.collapsingToolbarLayout,
                b.toolbar,
                b.title,
                b.body,
                b.banner,
                b.communitySortOrder2,
                b.toolbarPlaceholder,
                b.icon,
            )
        } else {
            val b = CustomAppBarSmallBinding.inflate(inflater, parentContainer, false)
            ViewHolder.SmallAppBarViewHolder(
                b.root,
                b.customActionBar,
                b.communityTextView,
                b.pageTextView,
                b.customAppBar,
                b.accountImageView,
                b.communitySortOrder,
                b.collapsingToolbarLayout,
                b.toolbar,
            )
        }
    }
}
