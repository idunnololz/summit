package com.idunnololz.summit.lemmy.communityInfo

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Either
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.isMod
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.SiteView
import com.idunnololz.summit.api.dto.SubscribedType
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.CommunityInfoCommunityItemBinding
import com.idunnololz.summit.databinding.FragmentCommunityInfoBinding
import com.idunnololz.summit.databinding.PageDataAdminItemBinding
import com.idunnololz.summit.databinding.PageDataDescriptionItemBinding
import com.idunnololz.summit.databinding.PageDataModItemBinding
import com.idunnololz.summit.databinding.PageDataStatsItemBinding
import com.idunnololz.summit.databinding.PageDataTitleItemBinding
import com.idunnololz.summit.databinding.WarningItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.links.LinkType
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimenFromAttribute
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommunityInfoFragment : BaseFragment<FragmentCommunityInfoBinding>() {

    private val args by navArgs<CommunityInfoFragmentArgs>()

    private val viewModel: CommunityInfoViewModel by viewModels()
    private val actionsViewModel: MoreActionsViewModel by viewModels()

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var preferences: Preferences

    private var isAnimatingTitleIn: Boolean = false
    private var isAnimatingTitleOut: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentCommunityInfoBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = binding.root.context

        requireMainActivity().apply {
            setupForFragment<CommunityInfoFragment>()

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = ""

            binding.title.text = args.communityRef.getName(context)
            binding.name.text = args.communityRef.getName(context)

            insetViewAutomaticallyByPaddingAndNavUi(viewLifecycleOwner, binding.coordinatorLayout)
        }

        val adapter = PageDataAdapter(
            context,
            viewModel.instance,
            offlineManager,
            onImageClick = { imageName, sharedElementView, url ->
                getMainActivity()?.openImage(
                    sharedElement = sharedElementView,
                    appBar = binding.appBar,
                    title = imageName,
                    url = url,
                    mimeType = null,
                )
            },
            onVideoClick = { url, videoType, state ->
                getMainActivity()?.openVideo(url, videoType, state)
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onLinkClick = { url, text, linkType ->
                onLinkClick(url, text, linkType)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showMoreLinkOptions(url, text)
            },
            onCommunityInfoClick = { communityRef ->
                getMainActivity()?.showCommunityInfo(communityRef)
            },
        )

        binding.loadingView.setOnRefreshClickListener {
            viewModel.refetchCommunityOrSite(force = true)
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refetchCommunityOrSite(force = true)
        }
        viewModel.siteOrCommunity.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()
                }
                is StatefulData.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()
                    val pageData = it.data.fold(
                        { it.toPageData() },
                        { it.toPageData() },
                    )

                    adapter.data = pageData
                    binding.recyclerView.adapter = adapter

                    loadPage(pageData)
                }
            }
        }
        viewModel.multiCommunity.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()
                }
                is StatefulData.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.loadingView.hideAll()

                    adapter.multiCommunity = it.data
                    binding.recyclerView.adapter = adapter

                    binding.subtitle.text = viewModel.instance
                    binding.icon.setImageResource(R.drawable.outline_shield_24)
                    binding.icon.strokeWidth = 0f
                    ImageViewCompat.setImageTintList(
                        binding.icon,
                        ColorStateList.valueOf(context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal)),
                    )
                }
            }
        }

        // setup initial view state so animations aren't messy
        with(binding) {
            when (args.communityRef) {
                is CommunityRef.All,
                is CommunityRef.Local,
                is CommunityRef.Subscribed,
                is CommunityRef.MultiCommunity,
                is CommunityRef.ModeratedCommunities,
                -> {
                    subscribe.visibility = View.GONE
                }
                is CommunityRef.CommunityRefByName -> {
                    subscribe.visibility = View.VISIBLE
                }
            }

            if (args.communityRef is CommunityRef.Local || args.communityRef is CommunityRef.All) {
                instanceInfo.visibility = View.GONE
                subscribe.visibility = View.VISIBLE
            } else {
                instanceInfo.visibility = View.VISIBLE
            }

            banner.transitionName = "banner_image"
            instanceInfo.visibility = View.GONE
        }

        val actionBarHeight = context.getDimenFromAttribute(androidx.appcompat.R.attr.actionBarSize)
        binding.appBar.addOnOffsetChangedListener { _, verticalOffset ->
            if (!isBindingAvailable()) {
                return@addOnOffsetChangedListener
            }

            val percentCollapsed =
                -verticalOffset / binding.collapsingToolbarLayout.height.toDouble()
            val absPixelsShowing = binding.collapsingToolbarLayout.height + verticalOffset

            if (absPixelsShowing <= actionBarHeight) {
                if (!isAnimatingTitleIn) {
                    isAnimatingTitleIn = true
                    isAnimatingTitleOut = false
                    binding.title.animate().alpha(1f)
                }
            } else if (percentCollapsed < 0.66) {
                if (!isAnimatingTitleOut) {
                    isAnimatingTitleOut = true
                    isAnimatingTitleIn = false
                    binding.title.animate().alpha(0f)
                }
            }
        }

        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
        }

        viewModel.onCommunityChanged(args.communityRef)
    }

    data class PageData(
        val backingObject: Either<CommunityView, SiteView>,
        val name: String,
        val fullName: String,
        val iconUrl: String?,
        val bannerUrl: String?,
        val instance: String,
        val publishTs: Int,
        val subscribedStatus: SubscribedType,
        val canSubscribe: Boolean,
        val content: String?,
        val isHidden: Boolean,
        val isRemoved: Boolean,

        val postCount: Int,
        val commentCount: Int,
        val userCount: Int,
        val usersPerDay: Int,
        val usersPerWeek: Int,
        val usersPerMonth: Int,
        val usersPerSixMonth: Int,

        val mods: List<Person>,
        val admins: List<PersonView>,
    )

    private fun GetCommunityResponse.toPageData(): PageData {
        val communityView = this.community_view
        val name = communityView.community.name
        val instance = communityView.community.instance

        return PageData(
            backingObject = Either.Left(communityView),
            name = name,
            fullName = "!$name@$instance",
            iconUrl = communityView.community.icon,
            bannerUrl = communityView.community.banner,
            instance = communityView.community.instance,
            publishTs = dateStringToTs(communityView.community.published).toInt(),
            subscribedStatus = communityView.subscribed,
            canSubscribe = true,
            content = communityView.community.description,
            isHidden = communityView.community.hidden,
            isRemoved = communityView.community.removed,

            postCount = communityView.counts.posts,
            commentCount = communityView.counts.comments,
            userCount = communityView.counts.subscribers,
            usersPerDay = communityView.counts.users_active_day,
            usersPerWeek = communityView.counts.users_active_week,
            usersPerMonth = communityView.counts.users_active_month,
            usersPerSixMonth = communityView.counts.users_active_half_year,

            mods = this.moderators.map { it.moderator },
            admins = listOf(),
        )
    }

    private fun GetSiteResponse.toPageData(): PageData {
        val siteView = this.site_view
        return PageData(
            backingObject = Either.Right(siteView),
            name = siteView.site.name,
            fullName = "!${siteView.site.instance}",
            iconUrl = siteView.site.icon,
            bannerUrl = siteView.site.banner,
            instance = siteView.site.instance,
            publishTs = dateStringToTs(siteView.site.published).toInt(),
            subscribedStatus = SubscribedType.NotSubscribed,
            canSubscribe = false,
            content = siteView.site.sidebar,
            isHidden = false,
            isRemoved = false,

            postCount = siteView.counts.posts,
            commentCount = siteView.counts.comments,
            userCount = siteView.counts.users,
            usersPerDay = siteView.counts.users_active_day,
            usersPerWeek = siteView.counts.users_active_week,
            usersPerMonth = siteView.counts.users_active_month,
            usersPerSixMonth = siteView.counts.users_active_half_year,

            mods = listOf(),
            admins = this.admins,
        )
    }

    private fun loadPage(data: PageData) {
        if (!isBindingAvailable()) return

        val context = requireContext()

        val communityView = data.backingObject.leftOrNull()
        val siteView = data.backingObject.getOrNull()

        TransitionManager.beginDelayedTransition(binding.collapsingToolbarContent)

        with(binding) {
            icon.load(data.iconUrl) {
                allowHardware(false)
                placeholder(R.drawable.ic_subreddit_default)
                fallback(R.drawable.ic_subreddit_default)
            }

            offlineManager.fetchImage(root, data.bannerUrl) {
                banner.load(it) {
                    allowHardware(false)
                }
            }
            if (data.bannerUrl != null) {
                icon.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    this.topToBottom = bannerDummy.id
                    this.topToTop = ConstraintLayout.LayoutParams.UNSET
                    this.topMargin = -Utils.convertDpToPixel(32f).toInt()
                }

                banner.setOnClickListener {
                    getMainActivity()?.openImage(
                        banner,
                        null,
                        args.communityRef.getName(context),
                        data.bannerUrl,
                        null,
                    )
                }
            } else {
                banner.setOnClickListener(null)
            }

            subtitle.text = data.fullName

            if (communityView != null) {
                subscribe.visibility = View.VISIBLE

                when (data.subscribedStatus) {
                    SubscribedType.Subscribed -> {
                        subscribe.text = getString(R.string.unsubscribe)
                        subscribe.isEnabled = true
                    }
                    SubscribedType.NotSubscribed -> {
                        subscribe.text = getString(R.string.subscribe)
                        subscribe.isEnabled = true
                    }
                    SubscribedType.Pending -> {
                        subscribe.text = getString(R.string.subscription_pending)
                        subscribe.isEnabled = false
                    }
                }

                subscribe.setOnClickListener {
                    viewModel.updateSubscriptionStatus(
                        communityId = communityView.community.id,
                        subscribe = data.subscribedStatus != SubscribedType.Subscribed,
                    )
                }

                binding.fab.visibility = View.VISIBLE
                binding.fab.setOnClickListener {
                    showOverflowMenu(communityView)
                }

                binding.instanceInfo.visibility = View.VISIBLE
                binding.instanceInfo.setOnClickListener {
                    getMainActivity()?.showCommunityInfo(
                        CommunityRef.Local(
                            communityView.community.instance,
                        ),
                    )
                }
            } else if (siteView != null) {
                binding.fab.visibility = View.VISIBLE
                binding.fab.setOnClickListener {
                    showOverflowMenu(siteView)
                }

                binding.instanceInfo.visibility = View.GONE

                subscribe.visibility = View.VISIBLE
                subscribe.text = getString(R.string.communities)
                subscribe.setOnClickListener {
                    val directions = CommunityInfoFragmentDirections
                        .actionCommunityInfoFragmentToCommunitiesFragment(
                            siteView.site.instance,
                        )
                    findNavController().navigateSafe(directions)
                }
            } else {
                binding.fab.visibility = View.GONE
                subscribe.visibility = View.GONE
                binding.instanceInfo.visibility = View.GONE
            }
            binding.fab.setup(preferences)
        }
    }

    private fun showOverflowMenu(communityView: CommunityView) {
        if (!isBindingAvailable()) return

        val bottomMenu = BottomMenu(requireContext()).apply {

            val fullAccount = actionsViewModel.accountInfoManager.currentFullAccount.value
            val isMod = fullAccount?.accountInfo?.isMod(communityView.community.id) == true

            if (isMod) {
                addItemWithIcon(
                    id = R.id.edit_community,
                    title = getString(R.string.edit_community),
                    icon = R.drawable.baseline_edit_24,
                )
                addDivider()
            }

            addItemWithIcon(
                id = R.id.block_community,
                title = getString(R.string.block_this_community_format, communityView.community.name),
                icon = R.drawable.baseline_block_24,
            )
            addItemWithIcon(
                id = R.id.unblock_community,
                title = getString(R.string.unblock_this_community_format, communityView.community.name),
                icon = R.drawable.ic_subreddit_default,
            )
            addItemWithIcon(
                id = R.id.block_instance,
                title = getString(R.string.block_this_instance_format, communityView.community.instance),
                icon = R.drawable.baseline_public_off_24,
            )
            addItemWithIcon(
                id = R.id.unblock_instance,
                title = getString(R.string.unblock_this_instance_format, communityView.community.instance),
                icon = R.drawable.baseline_public_24,
            )
            addDivider()
            addItemWithIcon(
                id = R.id.view_mod_log,
                title = R.string.view_mod_logs,
                icon = R.drawable.baseline_notes_24,
            )

            setOnMenuItemClickListener {
                when (it.id) {
                    R.id.edit_community -> {
                        val directions = CommunityInfoFragmentDirections
                            .actionCommunityInfoFragmentToCreateOrEditCommunityFragment(
                                communityView.community,
                            )
                        findNavController().navigateSafe(directions)
                    }
                    R.id.block_community -> {
                        actionsViewModel.blockCommunity(communityView.community.id, true)
                    }
                    R.id.unblock_community -> {
                        actionsViewModel.blockCommunity(communityView.community.id, false)
                    }
                    R.id.block_instance -> {
                        actionsViewModel.blockInstance(communityView.community.instance_id, true)
                    }
                    R.id.unblock_instance -> {
                        actionsViewModel.blockInstance(communityView.community.instance_id, false)
                    }
                    R.id.view_mod_log -> {
                        val directions = CommunityInfoFragmentDirections
                            .actionCommunityInfoFragmentToModLogsFragment(
                                communityView.community.instance,
                                communityView.community.toCommunityRef(),
                            )
                        findNavController().navigateSafe(directions)
                    }
                }
            }
        }

        requireMainActivity().showBottomMenu(bottomMenu)
    }

    private fun showOverflowMenu(siteView: SiteView) {
        if (!isBindingAvailable()) return

        val bottomMenu = BottomMenu(requireContext()).apply {
            addItemWithIcon(
                id = R.id.block_instance,
                title = getString(R.string.block_this_instance_format, siteView.site.instance),
                icon = R.drawable.baseline_public_off_24,
            )
            addItemWithIcon(
                id = R.id.unblock_instance,
                title = getString(R.string.unblock_this_instance_format, siteView.site.instance),
                icon = R.drawable.baseline_public_24,
            )
            addDivider()
            addItemWithIcon(
                id = R.id.view_mod_log,
                title = R.string.view_mod_logs,
                icon = R.drawable.baseline_notes_24,
            )

            setOnMenuItemClickListener {
                when (it.id) {
                    R.id.block_instance -> {
                        actionsViewModel.blockInstance(siteView.site.instance_id, true)
                    }
                    R.id.unblock_instance -> {
                        actionsViewModel.blockInstance(siteView.site.instance_id, false)
                    }
                    R.id.view_mod_log -> {
                        val directions = CommunityInfoFragmentDirections
                            .actionCommunityInfoFragmentToModLogsFragment(
                                siteView.site.instance,
                                null,
                            )
                        findNavController().navigateSafe(directions)
                    }
                }
            }
        }

        requireMainActivity().showBottomMenu(bottomMenu)
    }

    private class PageDataAdapter(
        private val context: Context,
        private val instance: String,
        private val offlineManager: OfflineManager,
        private val onImageClick: (String, View?, String) -> Unit,
        private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onLinkClick: (url: String, text: String, linkType: LinkType) -> Unit,
        private val onLinkLongClick: (url: String, text: String) -> Unit,
        private val onCommunityInfoClick: (communityRef: CommunityRef) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private sealed interface Item {
            data class WarningItem(
                val message: String,
            ) : Item
            data class StatsItem(
                val postCount: Int,
                val commentCount: Int,
                val userCount: Int,

                val usersPerDay: Int,
                val usersPerWeek: Int,
                val usersPerMonth: Int,
                val usersPerSixMonth: Int,
            ) : Item
            data class DescriptionItem(
                val content: String,
            ) : Item
            data class ModItem(
                val mod: Person,
            ) : Item
            data class AdminItem(
                val admin: PersonView,
            ) : Item
            data class TitleItem(
                val title: String,
            ) : Item
            data class CommunityItem(
                val communityView: CommunityView,
            ) : Item
        }

        var data: PageData? = null
            set(value) {
                field = value

                refreshItems()
            }
        var multiCommunity: List<GetCommunityResponse>? = null
            set(value) {
                field = value

                refreshItems()
            }

        val nf = PrettyPrintUtils.defaultDecimalFormat

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.WarningItem -> true
                    is Item.DescriptionItem ->
                        true
                    is Item.AdminItem ->
                        old.admin.person.id == (new as Item.AdminItem).admin.person.id

                    is Item.ModItem ->
                        old.mod.id == (new as Item.ModItem).mod.id
                    is Item.StatsItem ->
                        true
                    is Item.TitleItem ->
                        old.title == (new as Item.TitleItem).title
                    is Item.CommunityItem ->
                        old.communityView.community.id ==
                            (new as Item.CommunityItem).communityView.community.id
                }
            },
        ).apply {
            addItemType(
                clazz = Item.WarningItem::class,
                inflateFn = WarningItemBinding::inflate,
            ) { item, b, _ ->
                b.text.text = item.message
            }
            addItemType(
                clazz = Item.DescriptionItem::class,
                inflateFn = PageDataDescriptionItemBinding::inflate,
            ) { item, b, _ ->
                LemmyTextHelper.bindText(
                    textView = b.text,
                    text = item.content,
                    instance = instance,
                    onImageClick = {
                        onImageClick("", null, it)
                    },
                    onVideoClick = {
                        onVideoClick(it, VideoType.Unknown, null)
                    },
                    onPageClick = onPageClick,
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                )
            }
            addItemType(
                Item.StatsItem::class,
                PageDataStatsItemBinding::inflate,
            ) { item, b, _ ->
                b.posts.text = nf.format(item.postCount)
                b.comments.text = nf.format(item.commentCount)
                b.users.text = nf.format(item.userCount)
                b.usersPerDay.text = nf.format(item.usersPerDay)
                b.usersPerWeek.text = nf.format(item.usersPerWeek)
                b.usersPerMonth.text = nf.format(item.usersPerMonth)
                b.usersPerSixMonth.text = nf.format(item.usersPerSixMonth)
            }
            addItemType(
                Item.AdminItem::class,
                PageDataAdminItemBinding::inflate,
            ) { item, b, _ ->
                b.icon.load(item.admin.person.avatar)
                b.name.text = item.admin.person.fullName

                b.root.setOnClickListener {
                    onPageClick(item.admin.person.toPersonRef())
                }
            }
            addItemType(
                Item.ModItem::class,
                PageDataModItemBinding::inflate,
            ) { item, b, _ ->
                b.icon.load(item.mod.avatar)
                b.name.text = item.mod.fullName

                b.root.setOnClickListener {
                    onPageClick(item.mod.toPersonRef())
                }
            }
            addItemType(
                Item.TitleItem::class,
                PageDataTitleItemBinding::inflate,
            ) { item, b, _ ->
                b.title.text = item.title
            }
            addItemType(
                Item.CommunityItem::class,
                CommunityInfoCommunityItemBinding::inflate,
            ) { item, b, h ->
                b.icon.load(R.drawable.ic_subreddit_default)
                offlineManager.fetchImage(h.itemView, item.communityView.community.icon) {
                    b.icon.load(it)
                }

                b.title.text = item.communityView.community.name
                val mauString = LemmyUtils.abbrevNumber(item.communityView.counts.users_active_month.toLong())

                @Suppress("SetTextI18n")
                b.monthlyActives.text = "(${context.getString(R.string.mau_format, mauString)}) " +
                    "(${item.communityView.community.instance})"

                b.root.setOnClickListener {
                    onCommunityInfoClick(item.communityView.community.toCommunityRef())
                }
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            val data = data
            val multiCommunity = multiCommunity
            val newItems = mutableListOf<Item>()

            if (data != null) {
                if (data.isRemoved) {
                    newItems.add(Item.WarningItem(context.getString(R.string.warn_community_removed)))
                } else if (data.isHidden) {
                    newItems.add(Item.WarningItem(context.getString(R.string.warn_community_hidden)))
                }

                newItems.add(
                    Item.StatsItem(
                        data.postCount,
                        data.commentCount,
                        data.userCount,
                        data.usersPerDay,
                        data.usersPerWeek,
                        data.usersPerMonth,
                        data.usersPerSixMonth,
                    ),
                )
                newItems.add(Item.DescriptionItem(data.content ?: ""))

                if (data.admins.isNotEmpty()) {
                    newItems.add(Item.TitleItem(context.getString(R.string.admins)))
                    data.admins.mapTo(newItems) {
                        Item.AdminItem(it)
                    }
                }
                if (data.mods.isNotEmpty()) {
                    newItems.add(Item.TitleItem(context.getString(R.string.mods)))
                    data.mods.mapTo(newItems) {
                        Item.ModItem(it)
                    }
                }
            } else if (multiCommunity != null) {
                for (community in multiCommunity) {
                    newItems.add(
                        Item.CommunityItem(
                            community.community_view,
                        ),
                    )
                }
            }

            adapterHelper.setItems(newItems, this)
        }
    }
}
