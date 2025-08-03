package com.idunnololz.summit.lemmy.communityInfo

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.window.layout.WindowMetricsCalculator
import coil3.load
import coil3.request.allowHardware
import coil3.svg.SvgDecoder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.lemmy.CommunityId
import com.idunnololz.summit.api.dto.lemmy.CommunityView
import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.api.dto.lemmy.PersonView
import com.idunnololz.summit.api.dto.lemmy.SiteView
import com.idunnololz.summit.api.dto.lemmy.SubscribedType
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.CommunityInfoCommunityItemBinding
import com.idunnololz.summit.databinding.CommunityInfoHeaderItemBinding
import com.idunnololz.summit.databinding.FragmentCommunityInfoBinding
import com.idunnololz.summit.databinding.PageDataAdminItemBinding
import com.idunnololz.summit.databinding.PageDataDescriptionItemBinding
import com.idunnololz.summit.databinding.PageDataModItemBinding
import com.idunnololz.summit.databinding.PageDataStatsItemBinding
import com.idunnololz.summit.databinding.PageDataTitleItemBinding
import com.idunnololz.summit.databinding.WarningItemBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.communityInfo.CommunityInfoViewModel.CommunityInfo
import com.idunnololz.summit.lemmy.createOrEditCommunity.CreateOrEditCommunityFragment
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.lemmy.toPersonRef
import com.idunnololz.summit.lemmy.utils.actions.MoreActionsHelper
import com.idunnololz.summit.lemmy.utils.setup
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu
import com.idunnololz.summit.util.LinkUtils
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDimenFromAttribute
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.performHapticFeedbackCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.getParcelableCompat
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.video.VideoState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CommunityInfoFragment : BaseFragment<FragmentCommunityInfoBinding>() {

  private val args by navArgs<CommunityInfoFragmentArgs>()

  private val viewModel: CommunityInfoViewModel by viewModels()

  @Inject
  lateinit var moreActionsHelper: MoreActionsHelper

  @Inject
  lateinit var offlineManager: OfflineManager

  @Inject
  lateinit var preferences: Preferences

  @Inject
  lateinit var animationsHelper: AnimationsHelper

  @Inject
  lateinit var avatarHelper: AvatarHelper

  @Inject
  lateinit var lemmyTextHelper: LemmyTextHelper

  private var isAnimatingTitleIn: Boolean = false
  private var isAnimatingTitleOut: Boolean = false

  private var wasLoadedBefore: Boolean = false

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

    isAnimatingTitleIn = false
    isAnimatingTitleOut = false

    val context = binding.root.context

    parentFragmentManager.setFragmentResultListener(
      CreateOrEditCommunityFragment.REQUEST_KEY,
      viewLifecycleOwner,
    ) { _, bundle ->
      val result = bundle.getParcelableCompat<CreateOrEditCommunityFragment.Result>(
        CreateOrEditCommunityFragment.REQUEST_RESULT,
      )

      if (result != null) {
        when (result.actionType) {
          CreateOrEditCommunityFragment.ActionType.Create -> {
            result.communityRef?.let {
              getMainActivity()?.launchPage(it)
            }
          }
          CreateOrEditCommunityFragment.ActionType.Update -> {
            viewModel.refetchCommunityOrSite(force = true)
          }
        }
      }
    }

    requireSummitActivity().apply {
      insetViewAutomaticallyByPaddingAndNavUi(
        viewLifecycleOwner,
        binding.coordinatorLayout,
        applyTopInset = false,
      )
      insets.observe(viewLifecycleOwner) {
        val newToolbarHeight =
          context.getDimenFromAttribute(androidx.appcompat.R.attr.actionBarSize).toInt() +
            it.topInset
        binding.toolbar.updatePadding(top = it.topInset)
        binding.collapsingToolbarLayout.scrimVisibleHeightTrigger =
          (newToolbarHeight + Utils.convertDpToPixel(16f)).toInt()
        binding.bannerGradient.updateLayoutParams<ViewGroup.LayoutParams> {
          height = newToolbarHeight
        }
      }
    }

    val adapter = PageDataAdapter(
      context = context,
      instance = viewModel.instance,
      offlineManager = offlineManager,
      preferences = preferences,
      lemmyTextHelper = lemmyTextHelper,
      avatarHelper = avatarHelper,
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
      onSubscribeClick = { communityId, subscribe ->
        viewModel.updateSubscriptionStatus(
          communityId = communityId,
          subscribe = subscribe,
        )
      },
      onInstanceInfoClick = { instance ->
        getMainActivity()?.showCommunityInfo(
          CommunityRef.Local(
            instance,
          ),
        )
      },
      onCommunitiesClick = { instance ->
        val directions = CommunityInfoFragmentDirections
          .actionCommunityInfoFragmentToCommunitiesFragment(
            instance,
          )
        findNavController().navigateSafe(directions)
      },
      onModLogsClick = { instance, communityRef ->
        val directions = CommunityInfoFragmentDirections
          .actionCommunityInfoFragmentToModLogsFragment(
            instance = instance,
            communityRef = communityRef,
          )
        findNavController().navigateSafe(directions)
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
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          binding.swipeRefreshLayout.isRefreshing = false
          binding.loadingView.hideAll()
          val pageData = it.data.response.fold(
            { it.toPageData() },
            { it.toPageData() },
          )

          adapter.setData(pageData) {
            if (it.data.force) {
              binding.recyclerView.scrollToPosition(0)
            }
          }
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
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          binding.swipeRefreshLayout.isRefreshing = false
          binding.loadingView.hideAll()

          adapter.multiCommunity = it.data
          binding.recyclerView.adapter = adapter

          val name = it.data.communityRef.getMultiCommunityName(context)
          binding.title.text = name
          binding.name.text = name
          binding.subtitle.text = it.data.instance
          binding.icon.setImageResource(it.data.icon)
          binding.icon.strokeWidth = 0f
          ImageViewCompat.setImageTintList(
            binding.icon,
            ColorStateList.valueOf(
              context.getColorFromAttribute(
                androidx.appcompat.R.attr.colorControlNormal,
              ),
            ),
          )

          binding.banner.load("file:///android_asset/banner_placeholder.svg") {
            decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }

            val windowMetrics = WindowMetricsCalculator.getOrCreate()
              .computeCurrentWindowMetrics(context)
            size(
              windowMetrics.bounds.width(),
              (windowMetrics.bounds.width() * (9 / 16.0)).toInt(),
            )
          }
          binding.banner.setOnClickListener(null)
        }
      }
    }
    viewModel.deleteCommunityResult.observe(viewLifecycleOwner) {
      when (it) {
        is StatefulData.Error -> {
          ErrorDialogFragment.show(
            getString(R.string.error_unable_to_delete_community),
            it.error,
            childFragmentManager,
          )
        }
        is StatefulData.Loading -> {
          binding.loadingView.showProgressBar()
        }
        is StatefulData.NotStarted -> {}
        is StatefulData.Success -> {
          binding.loadingView.hideAll()
          viewModel.refetchCommunityOrSite(force = true)
        }
      }
    }

    // setup initial view state so animations aren't messy
    with(binding) {
      banner.transitionName = "banner_image"

      if (!wasLoadedBefore && savedInstanceState == null) {
        title.alpha = 0f
      }

      recyclerView.setup(animationsHelper)

      toolbar.apply {
        setTitle("")
        setNavigationIcon(
          R.drawable.baseline_arrow_back_24,
        )
        setNavigationOnClickListener {
          findNavController().navigateUp()
        }
      }
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
      } else if (percentCollapsed < 0.5) {
        if (!isAnimatingTitleOut) {
          isAnimatingTitleOut = true
          isAnimatingTitleIn = false

          if (binding.title.isLaidOut) {
            binding.title.animate().alpha(0f)
          } else {
            binding.title.alpha = 0f
          }
        }
      }
    }

    binding.apply {
      recyclerView.layoutManager = LinearLayoutManager(context)
      recyclerView.setHasFixedSize(true)
    }

    viewModel.onCommunityChanged(args.communityRef)

    if (savedInstanceState == null && !wasLoadedBefore) {
      wasLoadedBefore = true
    }
  }

  override fun onResume() {
    super.onResume()

    setupForFragment<CommunityInfoFragment>()
  }

  private fun loadPage(data: CommunityInfoData) {
    if (!isBindingAvailable()) return

    val context = requireContext()

    val communityView = data.backingObject.leftOrNull()
    val siteView = data.backingObject.getOrNull()

    TransitionManager.beginDelayedTransition(binding.collapsingToolbarContent)

    with(binding) {
      if (data.iconUrl != null) {
        ViewCompat.setTransitionName(icon, "profileIcon")

        icon.setOnClickListener {
          getMainActivity()?.openImage(
            icon,
            toolbar,
            null,
            data.iconUrl,
            null,
          )
        }
      } else {
        icon.setOnClickListener(null)
      }

      offlineManager.fetchImage(banner, data.bannerUrl) {
        banner.load(it) {
          allowHardware(false)
        }
      }
      if (data.bannerUrl != null) {
        banner.updateLayoutParams<ConstraintLayout.LayoutParams> {
          dimensionRatio = "H,16:9"
        }
        bannerDummy.updateLayoutParams<ConstraintLayout.LayoutParams> {
          dimensionRatio = "H,16:9"
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
        banner.load("file:///android_asset/banner_placeholder.svg") {
          decoderFactory { result, options, _ -> SvgDecoder(result.source, options) }

          val windowMetrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(context)
          size(
            windowMetrics.bounds.width(),
            (windowMetrics.bounds.width() * (9 / 16.0)).toInt(),
          )
        }
        banner.setOnClickListener(null)
      }

      if (communityView != null) {
        avatarHelper.loadCommunityIcon(icon, communityView.community)
        title.text = communityView.community.title
        name.text = communityView.community.title
        subtitle.text = communityView.community.fullName

        binding.fab.visibility = View.VISIBLE
        binding.fab.setOnClickListener {
          showOverflowMenu(communityView, data.mods)
        }
      } else if (siteView != null) {
        avatarHelper.loadInstanceIcon(icon, siteView)
        title.text = data.name
        name.text = data.name
        subtitle.text = data.fullName

        binding.fab.visibility = View.VISIBLE
        binding.fab.setOnClickListener {
          showOverflowMenu(siteView)
        }
      } else {
        avatarHelper.loadInstanceIcon(icon, null)
        title.text = data.name
        name.text = data.name
        subtitle.text = data.fullName

        binding.fab.visibility = View.GONE
      }
      binding.fab.setup(preferences)
    }
  }

  private fun showOverflowMenu(communityView: CommunityView, mods: List<Person>) {
    if (!isBindingAvailable()) return

    val context = requireContext()

    val bottomMenu = BottomMenu(requireContext()).apply {
      val fullAccount = moreActionsHelper.accountInfoManager.currentFullAccount.value
      val isMod = mods.firstOrNull { it.id == fullAccount?.accountId } != null

      if (isMod) {
        addItemWithIcon(
          id = R.id.edit_community,
          title = getString(R.string.edit_community),
          icon = R.drawable.baseline_edit_24,
        )
        addDivider()
      }

      addItemWithIcon(
        id = R.id.share,
        title = R.string.share,
        icon = R.drawable.baseline_share_24,
      )

      addItemWithIcon(
        id = R.id.block_community,
        title = getString(
          R.string.block_this_community_format,
          communityView.community.name,
        ),
        icon = R.drawable.baseline_block_24,
      )
      addItemWithIcon(
        id = R.id.unblock_community,
        title = getString(
          R.string.unblock_this_community_format,
          communityView.community.name,
        ),
        icon = R.drawable.ic_default_community,
      )
      addItemWithIcon(
        id = R.id.block_instance,
        title = getString(
          R.string.block_this_instance_format,
          communityView.community.instance,
        ),
        icon = R.drawable.baseline_public_off_24,
      )
      addItemWithIcon(
        id = R.id.unblock_instance,
        title = getString(
          R.string.unblock_this_instance_format,
          communityView.community.instance,
        ),
        icon = R.drawable.baseline_public_24,
      )
      addDivider()
      addItemWithIcon(
        id = R.id.view_mod_log,
        title = R.string.view_mod_logs,
        icon = R.drawable.baseline_notes_24,
      )

      if (isMod) {
        addDivider()
        if (communityView.community.deleted) {
          addDangerousItemWithIcon(
            id = R.id.restore_community,
            title = R.string.undo_delete_community,
            icon = R.drawable.baseline_restore_24,
          )
        } else {
          addDangerousItemWithIcon(
            id = R.id.delete_community,
            title = R.string.delete_community,
            icon = R.drawable.baseline_delete_24,
          )
        }
      }

      setOnMenuItemClickListener {
        when (it.id) {
          R.id.edit_community -> {
            val directions = CommunityInfoFragmentDirections
              .actionCommunityInfoFragmentToCreateOrEditCommunityFragment(
                communityView.community,
              )
            findNavController().navigateSafe(directions)
          }
          R.id.share -> {
            val url = LinkUtils.getLinkForCommunity(
              communityView.community.toCommunityRef(),
            )
            Utils.shareLink(context, url)
          }
          R.id.block_community -> {
            moreActionsHelper.blockCommunity(communityView.community.id, true)
          }
          R.id.unblock_community -> {
            moreActionsHelper.blockCommunity(communityView.community.id, false)
          }
          R.id.block_instance -> {
            moreActionsHelper.blockInstance(communityView.community.instance_id, true)
          }
          R.id.unblock_instance -> {
            moreActionsHelper.blockInstance(communityView.community.instance_id, false)
          }
          R.id.view_mod_log -> {
            val directions = CommunityInfoFragmentDirections
              .actionCommunityInfoFragmentToModLogsFragment(
                instance = viewModel.instance,
                communityRef = communityView.community.toCommunityRef(),
              )
            findNavController().navigateSafe(directions)
          }
          R.id.delete_community -> {
            viewModel.deleteCommunity(communityView.community.id, true)
          }
          R.id.restore_community -> {
            viewModel.deleteCommunity(communityView.community.id, false)
          }
        }
      }
    }

    requireSummitActivity().showBottomMenu(bottomMenu)
  }

  private fun showOverflowMenu(siteView: SiteView) {
    if (!isBindingAvailable()) return

    val context = requireContext()

    val bottomMenu = BottomMenu(requireContext()).apply {
      if (siteView.site.instance == viewModel.instance) {
        addItemWithIcon(
          id = R.id.create_community,
          title = getString(R.string.create_community),
          icon = R.drawable.baseline_add_24,
        )

        addItemWithIcon(
          id = R.id.share,
          title = R.string.share,
          icon = R.drawable.baseline_share_24,
        )

        addDivider()
      }

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
          R.id.create_community -> {
            val directions = CommunityInfoFragmentDirections
              .actionCommunityInfoFragmentToCreateOrEditCommunityFragment(
                null,
              )
            findNavController().navigateSafe(directions)
          }
          R.id.share -> {
            val url = LinkUtils.getLinkForInstance(siteView.site.instance)
            Utils.shareLink(context, url)
          }
          R.id.block_instance -> {
            moreActionsHelper.blockInstance(requireNotNull(siteView.site.instance_id), true)
          }
          R.id.unblock_instance -> {
            moreActionsHelper.blockInstance(requireNotNull(siteView.site.instance_id), false)
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

    requireSummitActivity().showBottomMenu(bottomMenu)
  }

  private class PageDataAdapter(
    private val context: Context,
    private val instance: String,
    private val offlineManager: OfflineManager,
    private val preferences: Preferences,
    private val lemmyTextHelper: LemmyTextHelper,
    private val avatarHelper: AvatarHelper,
    private val onImageClick: (String, View?, String) -> Unit,
    private val onVideoClick: (String, VideoType, VideoState?) -> Unit,
    private val onPageClick: (PageRef) -> Unit,
    private val onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
    private val onLinkLongClick: (url: String, text: String) -> Unit,
    private val onCommunityInfoClick: (communityRef: CommunityRef) -> Unit,
    private val onSubscribeClick: (communityId: CommunityId, subscribe: Boolean) -> Unit,
    private val onInstanceInfoClick: (instance: String) -> Unit,
    private val onCommunitiesClick: (instance: String) -> Unit,
    private val onModLogsClick: (instance: String, communityRef: CommunityRef?) -> Unit,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Item {
      data class CommunityHeaderItem(
        val communityView: CommunityView,
        val subscribedStatus: SubscribedType,
      ) : Item
      data class InstanceHeaderItem(
        val siteView: SiteView,
        val title: String,
        val subtitle: String,
      ) : Item
      data class MultiCommunityHeaderItem(
        val title: String,
        val subtitle: String,
        val instance: String,
      ) : Item
      data class WarningItem(
        val message: String,
      ) : Item
      data class StatsItem(
        val postCount: Int?,
        val commentCount: Int?,
        val userCount: Int?,

        val usersPerDay: Int?,
        val usersPerWeek: Int?,
        val usersPerMonth: Int?,
        val usersPerSixMonth: Int?,
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
        val communityInfo: CommunityInfo,
      ) : Item
    }

    private var data: CommunityInfoData? = null
    var multiCommunity: CommunityInfoViewModel.MultiCommunityData? = null
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
            old.communityInfo.communityRef ==
              (new as Item.CommunityItem).communityInfo.communityRef

          is Item.CommunityHeaderItem -> true
          is Item.InstanceHeaderItem -> true
          is Item.MultiCommunityHeaderItem -> true
        }
      },
    ).apply {
      addItemType(
        clazz = Item.MultiCommunityHeaderItem::class,
        inflateFn = CommunityInfoHeaderItemBinding::inflate,
      ) { item, b, _ ->
        b.button1.visibility = View.VISIBLE
        b.button2.visibility = View.GONE

        b.button1.setOnClickListener {
          onInstanceInfoClick(item.instance)
        }
      }
      addItemType(
        clazz = Item.InstanceHeaderItem::class,
        inflateFn = CommunityInfoHeaderItemBinding::inflate,
      ) { item, b, _ ->
        b.button1.visibility = View.VISIBLE
        b.button1.text = context.getString(R.string.communities)
        b.button1.setOnClickListener {
          onCommunitiesClick(item.siteView.site.instance)
        }

        b.button2.visibility = View.VISIBLE
        b.button2.text = context.getString(R.string.mod_logs)
        b.button2.setOnClickListener {
          onModLogsClick(item.siteView.site.instance, null)
        }
      }
      addItemType(
        clazz = Item.CommunityHeaderItem::class,
        inflateFn = CommunityInfoHeaderItemBinding::inflate,
      ) { item, b, _ ->
        b.button1.visibility = View.VISIBLE

        when (item.subscribedStatus) {
          SubscribedType.Subscribed -> {
            b.button1.text = context.getString(R.string.unsubscribe)
          }
          SubscribedType.NotSubscribed -> {
            b.button1.text = context.getString(R.string.subscribe)
          }
          SubscribedType.Pending -> {
            b.button1.text = context.getString(R.string.subscription_pending)
          }
        }

        b.button1.setOnClickListener {
          onSubscribeClick(
            item.communityView.community.id,
            item.subscribedStatus == SubscribedType.NotSubscribed,
          )
          if (preferences.hapticsEnabled) {
            it.performHapticFeedbackCompat(
              HapticFeedbackConstantsCompat.CONFIRM,
            )
          }
        }

        b.button2.visibility = View.VISIBLE
        b.button2.text = context.getString(R.string.instance_info)
        b.button2.setOnClickListener {
          onInstanceInfoClick(item.communityView.community.instance)
        }
      }
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
        lemmyTextHelper.bindText(
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
        fun Int?.format() = if (this == null) {
          "-"
        } else {
          nf.format(this)
        }

        b.posts.text = item.postCount.format()
        b.comments.text = item.commentCount.format()
        b.users.text = item.userCount.format()
        b.usersPerDay.text = item.usersPerDay.format()
        b.usersPerWeek.text = item.usersPerWeek.format()
        b.usersPerMonth.text = item.usersPerMonth.format()
        b.usersPerSixMonth.text = item.usersPerSixMonth.format()
      }
      addItemType(
        Item.AdminItem::class,
        PageDataAdminItemBinding::inflate,
      ) { item, b, _ ->
        avatarHelper.loadAvatar(b.icon, item.admin.person)
        b.name.text = item.admin.person.fullName

        b.root.setOnClickListener {
          onPageClick(item.admin.person.toPersonRef())
        }
      }
      addItemType(
        Item.ModItem::class,
        PageDataModItemBinding::inflate,
      ) { item, b, _ ->
        avatarHelper.loadAvatar(b.icon, item.mod)
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
        avatarHelper.loadCommunityIcon(
          imageView = b.icon,
          communityRef = item.communityInfo.communityRef,
          iconUrl = item.communityInfo.icon,
        )

        b.title.text = item.communityInfo.name

        b.root.setOnClickListener {
          onCommunityInfoClick(item.communityInfo.communityRef)
        }
      }
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    private fun refreshItems(cb: () -> Unit = {}) {
      val data = data
      val multiCommunityData = multiCommunity
      val newItems = mutableListOf<Item>()

      if (data != null) {
        data.backingObject
          .onLeft {
            newItems += Item.CommunityHeaderItem(it, data.subscribedStatus)
          }
          .onRight {
            newItems += Item.InstanceHeaderItem(it, data.name, data.fullName)
          }

        if (data.isRemoved) {
          newItems.add(
            Item.WarningItem(context.getString(R.string.warn_community_removed)),
          )
        } else if (data.isHidden) {
          newItems.add(
            Item.WarningItem(context.getString(R.string.warn_community_hidden)),
          )
        } else if (data.isDeleted) {
          newItems.add(
            Item.WarningItem(context.getString(R.string.warn_community_deleted)),
          )
        }
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
      } else if (multiCommunityData != null) {
        newItems += Item.MultiCommunityHeaderItem(
          title = multiCommunityData.communityRef.getMultiCommunityName(context),
          subtitle = multiCommunityData.instance,
          instance = multiCommunityData.instance,
        )

        newItems += Item.TitleItem(context.getString(R.string.communities))

        for (community in multiCommunityData.communitiesData) {
          newItems.add(
            Item.CommunityItem(
              community,
            ),
          )
        }
      }

      adapterHelper.setItems(newItems, this, cb)
    }

    fun setData(data: CommunityInfoData, cb: () -> Unit) {
      this.data = data
      refreshItems(cb)
    }
  }
}

private fun CommunityRef.getMultiCommunityName(context: Context) = when (this) {
  is CommunityRef.All -> TODO()
  is CommunityRef.CommunityRefByName -> TODO()
  is CommunityRef.Local -> TODO()
  is CommunityRef.ModeratedCommunities ->
    context.getString(R.string.moderated_communities)
  is CommunityRef.MultiCommunity -> TODO()
  is CommunityRef.Subscribed ->
    context.getString(R.string.subscribed_communities)
  is CommunityRef.AllSubscribed ->
    context.getString(R.string.all_subscribed)
}
