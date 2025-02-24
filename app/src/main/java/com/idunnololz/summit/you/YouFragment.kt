package com.idunnololz.summit.you

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.dispose
import com.idunnololz.summit.R
import com.idunnololz.summit.account.Account
import com.idunnololz.summit.account.info.AccountInfo
import com.idunnololz.summit.accountUi.AccountsAndSettingsDialogFragment
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentYouBinding
import com.idunnololz.summit.databinding.GenericSpaceFooterItemBinding
import com.idunnololz.summit.databinding.ItemGenericHeaderBinding
import com.idunnololz.summit.databinding.ItemYouDividerBinding
import com.idunnololz.summit.databinding.ItemYouMenuBinding
import com.idunnololz.summit.databinding.ItemYouProfileBinding
import com.idunnololz.summit.databinding.ItemYouSignInOrSignUpBinding
import com.idunnololz.summit.lemmy.LemmyUtils
import com.idunnololz.summit.lemmy.person.PersonTabbedFragment
import com.idunnololz.summit.saved.FilteredPostAndCommentsType
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.computeWindowMetrics
import com.idunnololz.summit.util.convertPixelToSp
import com.idunnololz.summit.util.dateStringToTs
import com.idunnololz.summit.util.excludeRegionFromSystemGestures
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getTextSizeFromTextAppearance
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import javax.inject.Inject

@AndroidEntryPoint
class YouFragment : BaseFragment<FragmentYouBinding>() {

    private val viewModel: YouViewModel by viewModels()

    @Inject
    lateinit var avatarHelper: AvatarHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentYouBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            requireMainActivity().apply {
                insetViewAutomaticallyByPaddingAndNavUi(
                    lifecycleOwner = viewLifecycleOwner,
                    rootView = binding.recyclerView,
                    applyTopInset = false,
                )
                insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            }

            toolbar.inflateMenu(R.menu.menu_you)
            toolbar.setOnMenuItemClickListener { item ->
                when (item?.itemId) {
                    R.id.settings -> {
                        val direction = YouFragmentDirections.actionGlobalSettingsFragment(null)
                        findNavController().navigateSafe(direction)
                    }
                }
                true
            }
            toolbar.excludeRegionFromSystemGestures()

            val adapter = YouAdapter(
                context = context,
                avatarHelper = avatarHelper,
                screenWidthDp = Utils.convertPixelsToDp(
                    requireActivity().computeWindowMetrics().bounds.width().toFloat(),
                ).toInt(),
                onSwitchAccountClick = {
                    AccountsAndSettingsDialogFragment.newInstance()
                        .showAllowingStateLoss(childFragmentManager, "AccountsDialogFragment")
                },
                onProfileClick = { account, profileIcon, url ->
                    if (url.isNullOrBlank()) {
                        OldAlertDialogFragment.Builder()
                            .setMessage(R.string.error_user_has_no_profile_image)
                            .createAndShow(
                                childFragmentManager,
                                "error_user_has_no_profile_image",
                            )
                    } else {
                        getMainActivity()?.openImage(
                            profileIcon,
                            toolbar,
                            account.name,
                            url,
                            null,
                        )
                    }
                },
                onPostsClick = {
                    val direction = YouFragmentDirections
                        .actionYouFragmentToPersonTabbedFragment2(
                            screen = PersonTabbedFragment.Screen.Posts,
                        )
                    findNavController().navigateSafe(direction)
                },
                onCommentsClick = {
                    val direction = YouFragmentDirections
                        .actionYouFragmentToPersonTabbedFragment2(
                            screen = PersonTabbedFragment.Screen.Comments,
                        )
                    findNavController().navigateSafe(direction)
                },
                onAccountAgeClick = {
                    val direction = YouFragmentDirections
                        .actionYouFragmentToPersonTabbedFragment2(
                            screen = PersonTabbedFragment.Screen.About,
                        )
                    findNavController().navigateSafe(direction)
                },
                onSignInOrSignUpClick = {
                    val direction = YouFragmentDirections.actionGlobalLogin()
                    findNavController().navigateSafe(direction)
                },
            ) {
                when (it) {
                    R.id.saved -> {
                        val direction = YouFragmentDirections.actionYouFragmentToSavedFragment()
                        findNavController().navigateSafe(direction)
                    }
                    R.id.history -> {
                        val direction = YouFragmentDirections.actionYouFragmentToHistoryFragment()
                        findNavController().navigateSafe(direction)
                    }
                    R.id.profile -> {
                        val direction = YouFragmentDirections
                            .actionYouFragmentToPersonTabbedFragment2()
                        findNavController().navigateSafe(direction)
                    }
                    R.id.hidden_posts -> {
                        val direction = YouFragmentDirections
                            .actionYouFragmentToHiddenPostsFragment2()
                        findNavController().navigateSafe(direction)
                    }
                    R.id.blocked -> {
                        val direction = YouFragmentDirections
                            .actionYouFragmentToSettingsAccountBlockListFragment2()
                        findNavController().navigateSafe(direction)
                    }
                    R.id.upvoted -> {
                        val direction = YouFragmentDirections.actionYouFragmentToSavedFragment(
                            type = FilteredPostAndCommentsType.Upvoted,
                        )
                        findNavController().navigateSafe(direction)
                    }
                    R.id.downvoted -> {
                        val direction = YouFragmentDirections.actionYouFragmentToSavedFragment(
                            type = FilteredPostAndCommentsType.Downvoted,
                        )
                        findNavController().navigateSafe(direction)
                    }
                    R.id.drafts -> {
                        val direction = YouFragmentDirections
                            .actionYouFragmentToDraftsTabbedFragment()
                        findNavController().navigateSafe(direction)
                    }
                    R.id.account_settings -> {
                        requireMainActivity().openAccountSettings()
                    }
                    R.id.user_tags -> {
                        val direction = YouFragmentDirections.actionYouFragmentToUserTagsFragment()
                        findNavController().navigateSafe(direction)
                    }
                    R.id.your_actions -> {
                        val direction = YouFragmentDirections
                            .actionYouFragmentToActions()
                        findNavController().navigateSafe(direction)
                    }
                }
            }

            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadModel(force = true)
            }

            viewModel.model.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading ->
                        loadingView.showProgressBar()
                    is StatefulData.NotStarted -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.hideAll()
                    }
                    is StatefulData.Success -> {
                        if (!it.data.isLoading) {
                            swipeRefreshLayout.isRefreshing = false
                            loadingView.hideAll()
                        }

                        adapter.updateModel(it.data)
                    }
                }
            }
            viewModel.newActionErrorsCount.observe(viewLifecycleOwner) {
                adapter.updateNewActionErrorsCount(it)
            }

            viewModel.loadModel(force = false)
        }
    }

    override fun onResume() {
        super.onResume()

        setupForFragment<YouFragment>()
    }

    private class YouAdapter(
        private val context: Context,
        private val avatarHelper: AvatarHelper,
        private val screenWidthDp: Int,
        private val onSwitchAccountClick: () -> Unit,
        private val onProfileClick: (Account, View, String?) -> Unit,
        private val onPostsClick: () -> Unit,
        private val onCommentsClick: () -> Unit,
        private val onAccountAgeClick: () -> Unit,
        private val onSignInOrSignUpClick: () -> Unit,
        private val onItemClick: (Int) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data object HeaderItem : Item
            data object FooterItem : Item
            data object SignInOrSignUpItem : Item
            data class Divider(val key: String) : Item
            data class ProfileItem(
                val name: String?,
                val account: Account?,
                val accountInfo: AccountInfo?,
                val person: PersonView?,
            ) : Item
            data class MenuItem(
                @IdRes val itemId: Int,
                @DrawableRes val iconRes: Int,
                @StringRes val text: Int,
                val number: Int? = null,
            ) : Item
        }

        private var actionErrorsCount = 0
        private var model: YouModel? = null

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    Item.HeaderItem -> true
                    is Item.MenuItem ->
                        old.itemId == (new as Item.MenuItem).itemId
                    is Item.ProfileItem -> true
                    Item.FooterItem -> true
                    Item.SignInOrSignUpItem -> true
                    is Item.Divider ->
                        old.key == (new as Item.Divider).key
                }
            },
        ).apply {
            addItemType(
                clazz = Item.HeaderItem::class,
                inflateFn = ItemGenericHeaderBinding::inflate,
            ) { _, _, _ -> }
            addItemType(
                clazz = Item.FooterItem::class,
                inflateFn = GenericSpaceFooterItemBinding::inflate,
            ) { _, _, _ -> }
            addItemType(
                clazz = Item.Divider::class,
                inflateFn = ItemYouDividerBinding::inflate,
            ) { _, _, _ ->
            }
            addItemType(
                clazz = Item.SignInOrSignUpItem::class,
                inflateFn = ItemYouSignInOrSignUpBinding::inflate,
            ) { _, b, _ ->
                b.button.setOnClickListener {
                    onSignInOrSignUpClick()
                }
            }
            addItemType(Item.ProfileItem::class, ItemYouProfileBinding::inflate) { item, b, h ->
                val account = item.account
                val profileUrl = item.accountInfo?.miscAccountInfo?.avatar

                if (account != null) {
                    b.profileIcon.imageTintList = null
                    avatarHelper.loadAvatar(
                        imageView = b.profileIcon,
                        imageUrl = profileUrl,
                        personName = account.name,
                        personId = account.id,
                        personInstance = account.instance,
                    )
                } else {
                    b.profileIcon.dispose()
                    b.profileIcon.imageTintList = ColorStateList.valueOf(
                        context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal),
                    )
                    b.profileIcon.setImageResource(R.drawable.outline_account_circle_24)
                }
                ViewCompat.setTransitionName(b.profileIcon, "profileIcon")

                if (account == null) {
                    b.profileIcon.setOnClickListener(null)
                } else {
                    b.profileIcon.setOnClickListener {
                        onProfileClick(account, b.profileIcon, profileUrl)
                    }
                }

                b.switchAccountButton.setOnClickListener {
                    onSwitchAccountClick()
                }

                b.title.text = account?.name ?: context.getString(R.string.guest)
                if (account != null) {
                    b.subtitle.visibility = View.VISIBLE
                    b.subtitle.text = "${account.name}@${account.instance}"
                } else {
                    b.subtitle.visibility = View.GONE
                }

                val mult = if (screenWidthDp > 400) {
                    1f
                } else if (screenWidthDp > 350) {
                    0.9f
                } else {
                    0.8f
                }
                val titleSize = convertPixelToSp(
                    context.getTextSizeFromTextAppearance(
                        com.google.android.material.R.attr.textAppearanceTitleLarge,
                    ),
                ) * mult
                val labelSize = convertPixelToSp(
                    context.getTextSizeFromTextAppearance(
                        com.google.android.material.R.attr.textAppearanceLabelSmall,
                    ),
                ) * mult

                b.posts.textSize = titleSize
                b.postsLabel.textSize = labelSize
                b.comments.textSize = titleSize
                b.commentsLabel.textSize = labelSize
                b.accountAge.textSize = titleSize
                b.accountAgeLabel.textSize = labelSize

                if (item.person != null) {
                    b.posts.text = LemmyUtils.abbrevNumber(item.person.counts.post_count.toLong())
                    b.comments.text = LemmyUtils.abbrevNumber(
                        item.person.counts.comment_count.toLong(),
                    )

                    val ts = dateStringToTs(item.person.person.published)
                    val accountCreationTime = LocalDateTime
                        .ofEpochSecond(ts / 1000, 0, ZoneOffset.UTC)
                        .toLocalDate()
                    val period = Period.between(accountCreationTime, LocalDate.now())

                    val years = period.years
                    val months = period.months
                    val days = period.days

                    b.accountAge.text = buildString {
                        if (years > 0) {
                            append(years)
                            append("y ")
                        }
                        if (months > 0) {
                            append(months)
                            append("m ")
                        }
                        if (days > 0 && years == 0) {
                            append(days)
                            append("d ")
                        }
                    }.trim()

                    b.statCard1.setOnClickListener {
                        onPostsClick()
                    }
                    b.statCard2.setOnClickListener {
                        onCommentsClick()
                    }
                    b.statCard3.setOnClickListener {
                        onAccountAgeClick()
                    }
                } else {
                    b.posts.text = "-"
                    b.comments.text = "-"
                    b.accountAge.text = "-"
                    b.statCard1.setOnClickListener(null)
                    b.statCard2.setOnClickListener(null)
                    b.statCard3.setOnClickListener(null)
                }
            }
            addItemType(Item.MenuItem::class, ItemYouMenuBinding::inflate) { item, b, h ->
                b.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    item.iconRes,
                    0,
                    0,
                    0,
                )
                b.text.setText(item.text)
                b.root.setOnClickListener {
                    onItemClick(item.itemId)
                }

                if (item.number == null) {
                    b.number.visibility = View.GONE
                } else {
                    b.number.visibility = View.VISIBLE
                    b.number.text = item.number.toString()
                }
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            val newItems = mutableListOf<Item>()

            val model = model
            if (model != null) {
                newItems += Item.HeaderItem
                newItems += Item.ProfileItem(
                    name = model.name,
                    account = model.account,
                    accountInfo = model.accountInfo,
                    person = model.personResult?.getOrNull()?.person_view,
                )

                if (model.account == null) {
                    newItems += Item.SignInOrSignUpItem
                }

                if (model.account != null) {
                    newItems += Item.MenuItem(
                        R.id.profile,
                        R.drawable.outline_account_circle_24,
                        R.string.user_profile,
                    )
                    newItems += Item.Divider("div1")
                }

                if (model.account != null) {
                    newItems += Item.MenuItem(
                        R.id.saved,
                        R.drawable.outline_bookmark_border_24,
                        R.string.saved,
                    )
                }
                newItems += Item.MenuItem(
                    R.id.history,
                    R.drawable.baseline_history_24,
                    R.string.history,
                )
                newItems += Item.MenuItem(
                    R.id.hidden_posts,
                    R.drawable.outline_visibility_off_24,
                    R.string.hidden_posts,
                )
                if (model.account != null) {
                    newItems += Item.MenuItem(
                        R.id.blocked,
                        R.drawable.baseline_block_24,
                        R.string.blocked,
                    )
                    newItems += Item.MenuItem(
                        R.id.upvoted,
                        R.drawable.baseline_arrow_upward_24,
                        R.string.upvoted,
                    )
                    newItems += Item.MenuItem(
                        R.id.downvoted,
                        R.drawable.baseline_arrow_downward_24,
                        R.string.downvoted,
                    )
                    newItems += Item.MenuItem(
                        R.id.drafts,
                        R.drawable.ic_draft_24,
                        R.string.drafts,
                    )
                    newItems += Item.Divider("div2")
                    newItems += Item.MenuItem(
                        R.id.account_settings,
                        R.drawable.ic_settings_account_box_24,
                        R.string.account_settings,
                    )
                }
                newItems += Item.MenuItem(
                    R.id.user_tags,
                    R.drawable.outline_sell_24,
                    R.string.user_tags,
                )
                newItems += Item.MenuItem(
                    itemId = R.id.your_actions,
                    iconRes = R.drawable.outline_play_arrow_24,
                    text = R.string.your_actions,
                    number = if (actionErrorsCount == 0) {
                        null
                    } else {
                        actionErrorsCount
                    },
                )
                newItems += Item.FooterItem
            }

            adapterHelper.setItems(newItems, this)
        }

        fun updateModel(model: YouModel) {
            this.model = model

            refreshItems()
        }

        fun updateNewActionErrorsCount(count: Int?) {
            count ?: return
            actionErrorsCount = count

            refreshItems()
        }
    }
}
