package com.idunnololz.summit.lemmy.communityInfo

import android.content.Context
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Either
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetCommunityResponse
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.dto.SiteView
import com.idunnololz.summit.api.dto.SubscribedType
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.databinding.FragmentCommunityInfoBinding
import com.idunnololz.summit.databinding.PageDataAdminItemBinding
import com.idunnololz.summit.databinding.PageDataDescriptionItemBinding
import com.idunnololz.summit.databinding.PageDataModItemBinding
import com.idunnololz.summit.databinding.PageDataStatsItemBinding
import com.idunnololz.summit.databinding.PageDataTitleItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.PersonRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.ext.getDimenFromAttribute
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showBottomMenuForLink
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommunityInfoFragment : BaseFragment<FragmentCommunityInfoBinding>() {

    private val args by navArgs<CommunityInfoFragmentArgs>()

    private val viewModel: CommunityInfoViewModel by viewModels()
    private val actionsViewModel: MoreActionsViewModel by viewModels()

    @Inject
    lateinit var offlineManager: OfflineManager

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
            onImageClick = { imageName, sharedElementView, url ->
                getMainActivity()?.openImage(
                    sharedElement = sharedElementView,
                    appBar = binding.appBar,
                    title = imageName,
                    url = url,
                    mimeType = null,
                )
            },
            onPageClick = {
                getMainActivity()?.launchPage(it)
            },
            onLinkLongClick = { url, text ->
                getMainActivity()?.showBottomMenuForLink(url, text)
            },
        )

        binding.loadingView.setOnRefreshClickListener {
            viewModel.refetchCommunityOrSite(force = true)
        }
        viewModel.siteOrCommunity.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    binding.loadingView.showDefaultErrorMessageFor(it.error)
                }
                is StatefulData.Loading -> {
                    binding.loadingView.showProgressBar()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
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

        // setup initial view state so animations aren't messy
        with(binding) {
            when (args.communityRef) {
                is CommunityRef.All,
                is CommunityRef.Local,
                is CommunityRef.Subscribed,
                is CommunityRef.MultiCommunity,
                -> {
                    subscribe.visibility = View.GONE
                }
                is CommunityRef.CommunityRefByName -> {
                    subscribe.visibility = View.VISIBLE
                }
            }

            banner.transitionName = "banner_image"
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
        val isSubscribed: Boolean,
        val canSubscribe: Boolean,
        val content: String?,

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
            Either.Left(communityView),
            name,
            "!$name@$instance",
            communityView.community.icon,
            communityView.community.banner,
            communityView.community.instance,
            dateStringToTs(communityView.community.published).toInt(),
            !(communityView.subscribed != SubscribedType.Subscribed),
            true,
            communityView.community.description,

            communityView.counts.posts,
            communityView.counts.comments,
            communityView.counts.subscribers,
            communityView.counts.users_active_day,
            communityView.counts.users_active_week,
            communityView.counts.users_active_month,
            communityView.counts.users_active_half_year,

            this.moderators.map { it.moderator },
            listOf(),
        )
    }

    private fun GetSiteResponse.toPageData(): PageData {
        val siteView = this.site_view
        return PageData(
            Either.Right(siteView),
            siteView.site.name,
            "!${siteView.site.instance}",
            siteView.site.icon,
            siteView.site.banner,
            siteView.site.instance,
            dateStringToTs(siteView.site.published).toInt(),
            false,
            false,
            siteView.site.description,

            siteView.counts.posts,
            siteView.counts.comments,
            siteView.counts.users,
            siteView.counts.users_active_day,
            siteView.counts.users_active_week,
            siteView.counts.users_active_month,
            siteView.counts.users_active_half_year,

            listOf(),
            this.admins,

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
                offlineManager.calculateImageMaxSizeIfNeeded(it)

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

                if (data.isSubscribed) {
                    subscribe.text = getString(R.string.unsubscribe)
                } else {
                    subscribe.text = getString(R.string.subscribe)
                }

                subscribe.setOnClickListener {
                    viewModel.updateSubscriptionStatus(communityView.community.id, !data.isSubscribed)
                }

                binding.fab.visibility = View.VISIBLE
                binding.fab.setOnClickListener {
                    showOverflowMenu(communityView)
                }
            } else {
                binding.fab.visibility = View.GONE
                subscribe.visibility = View.GONE
            }
        }
    }

    private fun showOverflowMenu(communityView: CommunityView) {
        if (!isBindingAvailable()) return

        val bottomMenu = BottomMenu(requireContext()).apply {
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

            setOnMenuItemClickListener {
                when (it.id) {
                    R.id.block_community -> {
                        actionsViewModel.blockCommunity(communityView.community.id, true)
                    }
                    R.id.unblock_community -> {
                        actionsViewModel.blockCommunity(communityView.community.id, false)
                    }
                }
            }
        }

        requireMainActivity().showBottomMenu(bottomMenu)
    }

    private class PageDataAdapter(
        private val context: Context,
        private val instance: String,
        private val onImageClick: (String, View?, String) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onLinkLongClick: (url: String, text: String) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private sealed interface Item {
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
        }

        var data: PageData? = null
            set(value) {
                field = value

                refreshItems()
            }

        val nf = PrettyPrintUtils.defaultDecimalFormat

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
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
                }
            },
        ).apply {
            addItemType(
                clazz = Item.DescriptionItem::class,
                inflateFn = PageDataDescriptionItemBinding::inflate,
            ) { item, b, h ->
                LemmyTextHelper.bindText(
                    textView = b.text,
                    text = item.content,
                    instance = instance,
                    onImageClick = {
                        onImageClick("", null, it)
                    },
                    onPageClick = onPageClick,
                    onLinkLongClick = onLinkLongClick,
                )
            }
            addItemType(
                Item.StatsItem::class,
                PageDataStatsItemBinding::inflate,
            ) { item, b, h ->
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
            ) { item, b, h ->
                b.icon.load(item.admin.person.avatar)
                b.name.text = item.admin.person.name

                b.root.setOnClickListener {
                    onPageClick(PersonRef.PersonRefByName(item.admin.person.name, instance))
                }
            }
            addItemType(
                Item.ModItem::class,
                PageDataModItemBinding::inflate,
            ) { item, b, h ->
                b.icon.load(item.mod.avatar)
                b.name.text = item.mod.name

                b.root.setOnClickListener {
                    onPageClick(PersonRef.PersonRefByName(item.mod.name, instance))
                }
            }
            addItemType(
                Item.TitleItem::class,
                PageDataTitleItemBinding::inflate,
            ) { item, b, h ->
                b.title.text = item.title
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
            val data = data ?: return
            val newItems = mutableListOf<Item>()

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

            adapterHelper.setItems(newItems, this)
        }
    }
}
