package com.idunnololz.summit.main

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.text.buildSpannedString
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import coil3.dispose
import coil3.load
import coil3.request.allowHardware
import com.discord.panels.PanelsChildGestureRegionObserver
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
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.CustomAppBarLargeBinding
import com.idunnololz.summit.databinding.CustomAppBarSmallBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.appendSeparator
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoViewModel
import com.idunnololz.summit.lemmy.instance
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.addEllipsizeToSpannedOnLayout
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimenFromAttribute
import com.idunnololz.summit.util.relativeTimeToConcise
import com.idunnololz.summit.util.shimmer.newShimmerDrawableSquare
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.toErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LemmyAppBarController(
    private val mainActivity: MainActivity,
    private val baseFragment: BaseFragment<*>,
    private val parentContainer: CoordinatorLayout,
    private val accountInfoManager: AccountInfoManager,
    private val communityInfoViewModel: CommunityInfoViewModel,
    private val viewLifecycleOwner: LifecycleOwner,
    private val avatarHelper: AvatarHelper,
    private val moreActionsHelper: MoreActionsHelper,
    private val userCommunitiesManager: UserCommunitiesManager,
    private val lemmyTextHelper: LemmyTextHelper,
    useHeader: Boolean,
    state: State? = null,
) {

    companion object {

        private const val TAG = "LemmyAppBarController"
    }

    data class State(
        var currentCommunity: CommunityRef? = null,
        var defaultCommunity: CommunityRef? = null,
        var instance: String? = null,
    )

    private val context = mainActivity

    private var vh: ViewHolder

    private var isScrimListenerEnabled = false
    private var isToolbarElementsVisible: Boolean? = null
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
    val percentShown = MutableLiveData(0f)
    val expandDescription = MutableStateFlow(false)
    private var isInfinity: Boolean = true

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
            val subtitle: TextView,
            val body: TextView,
            val banner: ImageView,
            val communitySortOrder2: TextView,
            val toolbarPlaceholder: View,
            val icon: ImageView,
            val subscribe: TextView,
            val info: TextView,
            val titleHotspot: View,
            val feedInfoText: TextView,
            val communities: TextView,
            val scrollView: View,
        ) : ViewHolder {
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
        ) : ViewHolder {
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
                    androidx.appcompat.R.attr.actionBarSize,
                ).toInt()
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
        viewLifecycleOwner.lifecycleScope.launch {
            accountInfoManager.subscribedCommunities.collect {
                updateSubscribeButton()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            communityInfoViewModel.accountManager.currentAccountOnChange.collect {
                this@LemmyAppBarController.state.currentCommunity?.let {
                    setCommunity(it)
                }
            }
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
            vh.titleHotspot.setOnClickListener {
                showCommunitySelectorInternal()
            }
            vh.titleHotspot.setOnLongClickListener {
                onCommunityLongClick(state.currentCommunity, vh.communityTextView.text?.toString())
            }
            vh.communitySortOrder2.setOnClickListener {
                onSortOrderClick()
            }
            registerOverlappingPanelsRegionIfNeeded()
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
        if (state.currentCommunity == communityRef &&
            state.instance == communityInfoViewModel.instance
        ) {
            return
        }

        state.instance = communityInfoViewModel.instance
        state.currentCommunity = communityRef
        expandDescription.value = false

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
        val isBookmarked: Boolean
        if (currentCommunity == null) {
            isBookmarked = false
        } else {
            isBookmarked = userCommunitiesManager.isCommunityBookmarked(currentCommunity)
        }

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
        } else if (isBookmarked) {
            vh.communityTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.outline_bookmark_check_18,
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
            loadLargeAppBar(vh)

            if (currentCommunity != null) {
                communityInfoViewModel.onCommunityChanged(currentCommunity)
            }
        }
    }

    private fun loadLargeAppBar(vh: ViewHolder.LargeAppBarViewHolder) {
        val currentCommunity = state.currentCommunity
        val communityName = currentCommunity?.getName(context) ?: ""

        vh.title.text = communityName
        if (currentCommunity == null) {
            vh.subtitle.visibility = View.GONE
        } else {
            val communityInstance = currentCommunity.instance ?: communityInfoViewModel.instance
            vh.subtitle.visibility = View.VISIBLE
            @Suppress("SetTextI18n")
            vh.subtitle.text = when (currentCommunity) {
                is CommunityRef.All -> context.getString(R.string.all_feed_desc)
                is CommunityRef.AllSubscribed -> context.getString(R.string.all_subscribed_feed_desc)
                is CommunityRef.CommunityRefByName -> "$communityName@$communityInstance"
                is CommunityRef.Local -> context.getString(R.string.local_feed_desc)
                is CommunityRef.ModeratedCommunities -> context.getString(R.string.moderated_feed_desc)
                is CommunityRef.MultiCommunity -> context.getString(R.string.multi_community_desc)
                is CommunityRef.Subscribed -> context.getString(R.string.subscribed_feed_desc)
            }
        }

        when (val value = communityInfoViewModel.siteOrCommunity.value) {
            is StatefulData.Error -> {
                vh.body.text = value.error.toErrorMessage(context)
                vh.feedInfoText.text = "-"
            }
            is StatefulData.Loading -> {
                vh.banner.load("file:///android_asset/banner_placeholder.svg") {
                    allowHardware(false)
                }
                vh.icon.forCoil()
                vh.icon.load(newShimmerDrawableSquare(context))
                vh.body.text = buildSpannedString {
                    appendLine(context.getString(R.string.loading))
                    appendLine(context.getString(R.string.loading))
                }
                vh.feedInfoText.text = context.getString(R.string.loading)
            }

            is StatefulData.NotStarted -> {}
            is StatefulData.Success -> {
                vh.icon.transitionName = "app_bar_icon"
                val bannerUrl = value.data.response
                    .fold(
                        {
                            avatarHelper.loadInstanceIcon(vh.icon, it.site_view)
                            it.site_view.site.banner
                        },
                        {
                            avatarHelper.loadCommunityIcon(vh.icon, it.community_view.community)
                            it.community_view.community.banner
                        },
                    )
                val iconUrl = value.data.response
                    .fold(
                        {
                            it.site_view.site.icon
                        },
                        {
                            it.community_view.community.icon
                        },
                    )

                updateDescription()

                if (bannerUrl == null) {
                    vh.banner.load("file:///android_asset/banner_placeholder.svg") {
                        allowHardware(false)
                    }
                    vh.banner.setOnClickListener(null)
                    vh.banner.isClickable = false
                } else {
                    vh.banner.transitionName = "app_bar_banner"
                    vh.banner.load(bannerUrl) {
                        allowHardware(false)
                    }
                    vh.banner.setOnClickListener {
                        mainActivity.openImage(
                            sharedElement = vh.banner,
                            appBar = appBarRoot,
                            title = null,
                            url = bannerUrl,
                            mimeType = null,
                        )
                    }
                }
                if (iconUrl == null) {
                    vh.icon.setOnClickListener(null)
                    vh.icon.isClickable = false
                } else {
                    vh.icon.setOnClickListener {
                        mainActivity.openImage(
                            sharedElement = vh.icon,
                            appBar = appBarRoot,
                            title = null,
                            url = iconUrl,
                            mimeType = null,
                        )
                    }
                }

                vh.feedInfoText.visibility = View.VISIBLE
                vh.feedInfoText.text = buildSpannedString {
                    val mau = value.data.response.fold(
                        { it.site_view.counts.users_active_month },
                        { it.community_view.counts.users_active_month },
                    )
                    val totalUsers = value.data.response.fold(
                        { it.site_view.counts.users },
                        { it.community_view.counts.subscribers },
                    )
                    val posts = value.data.response.fold(
                        { it.site_view.counts.posts },
                        { it.community_view.counts.posts },
                    )
                    val comments = value.data.response.fold(
                        { it.site_view.counts.comments },
                        { it.community_view.counts.comments },
                    )
                    append(
                        context.resources.getQuantityString(
                            R.plurals.users_format,
                            totalUsers,
                            LemmyUtils.abbrevNumber(totalUsers.toLong()),
                        ),
                    )
                    appendSeparator()
                    append(
                        context.getString(
                            R.string.mau_format, LemmyUtils.abbrevNumber(mau.toLong()),
                        ),
                    )
                    appendSeparator()
                    append(
                        context.resources.getQuantityString(
                            R.plurals.posts_format,
                            posts,
                            LemmyUtils.abbrevNumber(posts.toLong()),
                        ),
                    )
                    appendSeparator()
                    append(
                        context.resources.getQuantityString(
                            R.plurals.comments_format,
                            comments,
                            LemmyUtils.abbrevNumber(comments.toLong()),
                        ),
                    )
                }
            }
        }

        updateSubscribeButton()

        when (currentCommunity) {
            is CommunityRef.MultiCommunity -> {
                vh.banner.load("file:///android_asset/banner_placeholder.svg") {
                    allowHardware(false)
                }
                vh.icon.forCoil()
                vh.icon.load(currentCommunity.icon)
                vh.body.visibility = View.VISIBLE
                vh.body.text = currentCommunity.communities.joinToString {
                    it.getLocalizedFullName(context)
                }
                vh.body.maxLines = 2
                vh.body.addEllipsizeToSpannedOnLayout()
                vh.feedInfoText.visibility = View.GONE
            }
            is CommunityRef.Subscribed -> {
                vh.banner.load("file:///android_asset/banner_placeholder.svg") {
                    allowHardware(false)
                }
                vh.icon.forIcon()
                vh.icon.setImageResource(R.drawable.baseline_subscriptions_24)
                vh.body.visibility = View.GONE
                vh.feedInfoText.visibility = View.GONE
            }
            is CommunityRef.AllSubscribed -> {
                vh.banner.load("file:///android_asset/banner_placeholder.svg") {
                    allowHardware(false)
                }
                vh.icon.forIcon()
                vh.icon.setImageResource(R.drawable.outline_groups_24)
                vh.body.visibility = View.GONE
                vh.feedInfoText.visibility = View.GONE
            }
            is CommunityRef.ModeratedCommunities -> {
                vh.banner.load("file:///android_asset/banner_placeholder.svg") {
                    allowHardware(false)
                }
                vh.icon.forIcon()
                vh.icon.setImageResource(R.drawable.outline_shield_24)
                vh.body.visibility = View.GONE
                vh.feedInfoText.visibility = View.GONE
            }
            is CommunityRef.CommunityRefByName,
            is CommunityRef.All,
            is CommunityRef.Local,
            null,
            -> {
            }
        }

        when (currentCommunity) {
            is CommunityRef.All,
            is CommunityRef.Local,
            -> {
                vh.info.visibility = View.VISIBLE
                vh.info.text = context.getString(R.string.instance_info)
            }
            is CommunityRef.MultiCommunity -> {
                vh.info.visibility = View.VISIBLE
                vh.info.text = context.getString(R.string.multi_community_info)
            }
            is CommunityRef.Subscribed,
            is CommunityRef.AllSubscribed,
            is CommunityRef.ModeratedCommunities,
            -> {
                vh.info.visibility = View.VISIBLE
                vh.info.text = context.getString(R.string.feed_info)
            }
            is CommunityRef.CommunityRefByName -> {
                vh.info.visibility = View.VISIBLE
                vh.info.text = context.getString(R.string.community_info)
            }
            null -> {
                vh.info.visibility = View.GONE
            }
        }

        if (currentCommunity is CommunityRef.All || currentCommunity is CommunityRef.Local) {
            vh.communities.visibility = View.VISIBLE
            vh.communities.setOnClickListener {
                mainActivity.showCommunities(
                    currentCommunity.instance ?: communityInfoViewModel.instance,
                )
            }
        } else {
            vh.communities.visibility = View.GONE
        }

        vh.info.setOnClickListener {
            if (currentCommunity != null) {
                mainActivity.showCommunityInfo(currentCommunity)
            }
        }
    }

    private fun ImageView.forIcon() {
        dispose()
        scaleType = ImageView.ScaleType.CENTER
        setBackgroundColor(
            context.getColorFromAttribute(
                com.google.android.material.R.attr.colorSurfaceContainerHighest,
            ),
        )
        imageTintList = ColorStateList.valueOf(
            context.getColorFromAttribute(
                com.google.android.material.R.attr.colorOnSurface,
            ),
        )
    }

    private fun ImageView.forCoil() {
        dispose()
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(
            context.getColorFromAttribute(
                com.google.android.material.R.attr.backgroundColor,
            ),
        )
        imageTintList = null
    }

    private fun updateDescription() {
        val vh = vh
        if (vh !is ViewHolder.LargeAppBarViewHolder) {
            return
        }

        val siteOrCommunity = communityInfoViewModel.siteOrCommunity.valueOrNull
            ?: return
        val body = siteOrCommunity.response
            .fold(
                { it.site_view.site.description },
                { it.community_view.community.description },
            )

        if (body.isNullOrBlank()) {
            vh.body.visibility = View.GONE
            vh.body.setOnClickListener(null)
        } else {
            vh.body.visibility = View.VISIBLE
            lemmyTextHelper.bindText(
                textView = vh.body,
                text = body,
                instance = communityInfoViewModel.instance,
                onImageClick = { url ->
                    mainActivity.openImage(
                        sharedElement = null,
                        appBar = appBarRoot,
                        title = null,
                        url = url,
                        mimeType = null,
                    )
                },
                onVideoClick = { url ->
                    mainActivity.openVideo(url, VideoType.Unknown, null)
                },
                onPageClick = {
                    mainActivity.launchPage(it)
                },
                onLinkClick = { url, text, linkType ->
                    baseFragment.onLinkClick(url, text, linkType)
                },
                onLinkLongClick = { url, text ->
                    mainActivity.showMoreLinkOptions(url, text)
                },
            )
            if (expandDescription.value) {
                vh.body.maxLines = Integer.MAX_VALUE
            } else {
                vh.body.maxLines = 2
                vh.body.addEllipsizeToSpannedOnLayout()
            }
            vh.body.setOnClickListener {
                expandDescription.value = !expandDescription.value

                updateDescription()
            }
        }
    }

    private fun updateSubscribeButton() {
        val currentCommunity = state.currentCommunity
        val vh = vh
        val fullAccount = accountInfoManager.currentFullAccount.value
        val isSubscribed = fullAccount?.accountInfo?.subscriptions?.any {
            it.toCommunityRef() == currentCommunity
        } == true

        if (vh is ViewHolder.LargeAppBarViewHolder) {
            if (currentCommunity is CommunityRef.CommunityRefByName) {
                vh.subscribe.visibility = View.VISIBLE
                if (isSubscribed) {
                    vh.subscribe.text = context.getString(R.string.subscribed)
                    vh.subscribe.setOnClickListener {
                        moreActionsHelper.updateSubscription(currentCommunity, false)
                    }
                    vh.subscribe.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.baseline_notifications_18,
                        0,
                        0,
                        0,
                    )
                } else {
                    vh.subscribe.text = context.getString(R.string.subscribe)
                    vh.subscribe.setOnClickListener {
                        moreActionsHelper.updateSubscription(currentCommunity, true)
                    }
                    vh.subscribe.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.outline_notifications_18,
                        0,
                        0,
                        0,
                    )
                }
            } else {
                vh.subscribe.visibility = View.GONE
            }
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
        isToolbarElementsVisible = null
        this.isInfinity = isInfinity
        updateToolbarVisibility(animate = false)
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
                CommunitySortOrder.MostComments -> context.getString(
                    R.string.sort_order_most_comments,
                )
                CommunitySortOrder.New -> context.getString(R.string.sort_order_new)
                CommunitySortOrder.NewComments -> context.getString(
                    R.string.sort_order_new_comments,
                )
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
                CommunitySortOrder.Controversial -> context.getString(
                    R.string.sort_order_controversial,
                )
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
                    animate = animate,
                )
            }
        } else {
            setToolbarElementsVisibility(
                visible = true,
                animate = animate,
            )
        }
    }

    private fun View.hide(animate: Boolean) {
        if (animate) {
            this.animate()
                .alpha(0f)
                .withEndAction {
                    this.visibility = View.INVISIBLE
                }
        } else {
            this.alpha = 0f
            this.visibility = View.INVISIBLE
        }
    }

    private fun View.show(animate: Boolean) {
        if (animate) {
            this.visibility = View.VISIBLE
            this.animate()
                .alpha(1f)
        } else {
            this.alpha = 1f
            this.visibility = View.VISIBLE
        }
    }

    private fun setToolbarElementsVisibility(visible: Boolean, animate: Boolean) {
        if (visible == isToolbarElementsVisible) {
            return
        }
        isToolbarElementsVisible = visible

        var isSortOrderVisible = visible
        var isPageVisible = visible

        if (isInfinity) {
            isPageVisible = false
        } else {
            isSortOrderVisible = false
        }

        if (visible) {
            vh.communityTextView.show(animate = animate)
        } else {
            vh.communityTextView.hide(animate = animate)
        }

        if (isSortOrderVisible) {
            vh.communitySortOrder.show(animate = animate)
        } else {
            vh.communitySortOrder.hide(animate = animate)
        }

        if (isPageVisible) {
            vh.pageTextView.show(animate = animate)
        } else {
            vh.pageTextView.hide(animate = animate)
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

    private fun registerOverlappingPanelsRegionIfNeeded() {
        val vh = vh as ViewHolder.LargeAppBarViewHolder
        PanelsChildGestureRegionObserver.Provider.get().register(vh.scrollView)
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
                b.subtitle,
                b.body,
                b.banner,
                b.communitySortOrder2,
                b.toolbarPlaceholder,
                b.icon,
                b.subscribe,
                b.info,
                b.titleHotspot,
                b.feedInfoText,
                b.communities,
                b.scrollView,
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
