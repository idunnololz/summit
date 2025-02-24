package com.idunnololz.summit.main.communitiesPane

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import coil.dispose
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.BookmarkHeaderItemBinding
import com.idunnololz.summit.databinding.BookmarkedCommunityHeaderItemBinding
import com.idunnololz.summit.databinding.CommunitiesPaneBinding
import com.idunnololz.summit.databinding.GenericCommunityItemBinding
import com.idunnololz.summit.databinding.HomeCommunityItemBinding
import com.idunnololz.summit.databinding.NoSubscriptionsItemBinding
import com.idunnololz.summit.databinding.TabStateItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.tabs.hasTabId
import com.idunnololz.summit.tabs.isSubscribedCommunity
import com.idunnololz.summit.tabs.toTab
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.getDrawableCompat
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.ext.tint
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

typealias OnCommunitySelected = (Either<UserCommunityItem, CommunityRef>, resetTab: Boolean) -> Unit

class CommunitiesPaneController @AssistedInject constructor(
    private val offlineManager: OfflineManager,
    private val tabsManager: TabsManager,
    private val animationsHelper: AnimationsHelper,
    private val avatarHelper: AvatarHelper,
    @Assisted private val viewModel: CommunitiesPaneViewModel,
    @Assisted private val binding: CommunitiesPaneBinding,
    @Assisted private val viewLifecycleOwner: LifecycleOwner,
    @Assisted private val onCommunitySelected: OnCommunitySelected,
    @Assisted private val onEditMultiCommunity: (UserCommunityItem) -> Unit,
    @Assisted private val onAddBookmarkClick: () -> Unit,
) {

    @AssistedFactory
    interface Factory {
        fun create(
            viewModel: CommunitiesPaneViewModel,
            binding: CommunitiesPaneBinding,
            viewLifecycleOwner: LifecycleOwner,
            onCommunitySelected: OnCommunitySelected,
            onEditMultiCommunity: (UserCommunityItem) -> Unit,
            onAddBookmarkClick: () -> Unit,
        ): CommunitiesPaneController
    }

    init {
        viewModel.loadCommunities()

        val context = binding.root.context

        val adapter = UserCommunitiesAdapter(
            context = context,
            onCommunitySelected = onCommunitySelected,
            avatarHelper = avatarHelper,
            onDeleteUserCommunity = { id ->
                viewModel.deleteUserCommunity(id)
            },
            onEditMultiCommunity = { ref ->
                onEditMultiCommunity(ref)
            },
            onAddBookmarkClick = onAddBookmarkClick,
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            viewModel.loadCommunities()
        }

        binding.titleEditText.addTextChangedListener {
            adapter.filter(it?.toString()) {
                binding.recyclerView.scrollToPosition(0)
            }
        }

        viewModel.communities.observe(viewLifecycleOwner) {
            if (it != null) {
                adapter.data = it
            }
        }

        tabsManager.currentTab.observe(viewLifecycleOwner) {
            adapter.currentTab = it
        }

        binding.apply {
            recyclerView.setup(animationsHelper)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
        }
    }

    fun onShown() {
    }

    private class UserCommunitiesAdapter(
        private val context: Context,
        private val onCommunitySelected: OnCommunitySelected,
        private val avatarHelper: AvatarHelper,
        private val onDeleteUserCommunity: (Long) -> Unit,
        private val onEditMultiCommunity: (UserCommunityItem) -> Unit,
        private val onAddBookmarkClick: () -> Unit,
    ) : Adapter<ViewHolder>() {

        private sealed interface Item {
            data object BookmarkHeaderItem : Item
            data class HomeCommunityItem(
                val communityRef: CommunityRef,
                val userCommunityItem: UserCommunityItem,
                val isSelected: Boolean,
                val resetTabOnClick: Boolean,
            ) : Item
            data class BookmarkedCommunityItem(
                val communityRef: CommunityRef,
                val iconUrl: String?,
                val userCommunityItem: UserCommunityItem,
                val isSelected: Boolean,
                val resetTabOnClick: Boolean,
            ) : Item
            data class SubscriptionHeaderItem(
                val isRefreshing: Boolean,
            ) : Item
            data class SubscribedCommunityItem(
                val communityRef: CommunityRef,
                val iconUrl: String?,
                val isSelected: Boolean,
                val resetTabOnClick: Boolean,
            ) : Item
            data object NoSubscriptionsItem : Item

            data class TabStateItem(
                val parentKey: String,
                val tab: TabsManager.Tab,
                val tabState: TabsManager.TabState,
                val isSelected: Boolean,
            ) : Item
        }

        private var filter: String? = null
        var currentTab: TabsManager.Tab? = null
            set(value) {
                field = value

                refreshItems()
            }
        var data: CommunitiesPaneViewModel.CommunityData? = null
            set(value) {
                field = value

                refreshItems()
            }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                if (old::class != new::class) {
                    return@AdapterHelper false
                }

                when (old) {
                    Item.BookmarkHeaderItem -> true
                    is Item.BookmarkedCommunityItem ->
                        old.communityRef.getKey() ==
                            (new as Item.BookmarkedCommunityItem).communityRef.getKey()
                    is Item.HomeCommunityItem ->
                        old.communityRef.getKey() ==
                            (new as Item.HomeCommunityItem).communityRef.getKey()
                    is Item.SubscribedCommunityItem ->
                        old.communityRef.getKey() ==
                            (new as Item.SubscribedCommunityItem).communityRef.getKey()
                    is Item.SubscriptionHeaderItem -> true
                    Item.NoSubscriptionsItem -> true
                    is Item.TabStateItem ->
                        old.parentKey == (new as Item.TabStateItem).parentKey
                }
            },
        ).apply {
            addItemType(
                clazz = Item.BookmarkHeaderItem::class,
                inflateFn = BookmarkHeaderItemBinding::inflate,
            ) { _, b, _ ->
                b.add.setOnClickListener {
                    onAddBookmarkClick()
                }
            }
            addItemType(
                clazz = Item.HomeCommunityItem::class,
                inflateFn = HomeCommunityItemBinding::inflate,
            ) { item, b, _ ->
                b.title.text = context.getString(R.string.home)
                b.subtitle.text = item.communityRef.getLocalizedFullName(context)
                b.selectedIndicator.visibility = if (item.isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                b.root.setOnClickListener {
                    onCommunitySelected(Either.Left(item.userCommunityItem), item.resetTabOnClick)
                }
            }
            addItemType(
                clazz = Item.BookmarkedCommunityItem::class,
                inflateFn = GenericCommunityItemBinding::inflate,
            ) { item, b, h ->
                b.selectedIndicator.visibility = if (item.isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                fun loadCommunityIcon() {
                    b.icon.dispose()
                    b.icon.scaleType = ImageView.ScaleType.CENTER_CROP
                    b.icon.background = null
                    b.icon.imageTintList = null
                    avatarHelper.loadCommunityIcon(b.icon, item.communityRef, item.iconUrl)
                }

                if (item.iconUrl == null) {
                    fun loadIcon(d: Int) {
                        b.icon.dispose()
                        b.icon.setImageResource(d)
                        b.icon.scaleType = ImageView.ScaleType.CENTER
                        b.icon.setBackgroundColor(context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorSurfaceContainerHighest))
                        b.icon.imageTintList = ColorStateList.valueOf(context.getColorFromAttribute(
                            com.google.android.material.R.attr.colorOnSurface))
//                        b.icon.load(
//                            context.getDrawableCompat(d)
//                                ?.tint(
//                                    context.getColorFromAttribute(
//                                        androidx.appcompat.R.attr.colorControlNormal,
//                                    ),
//                                ),
//                        ) {}
                    }

                    when (item.communityRef) {
                        is CommunityRef.All ->
                            loadIcon(R.drawable.ic_feed_all)
                        is CommunityRef.CommunityRefByName ->
                            loadCommunityIcon()
                        is CommunityRef.Local ->
                            loadIcon(R.drawable.ic_feed_home)
                        is CommunityRef.MultiCommunity ->
                            loadIcon(R.drawable.baseline_dynamic_feed_24)
                        is CommunityRef.AllSubscribed ->
                            loadIcon(R.drawable.baseline_subscriptions_24)
                        is CommunityRef.Subscribed ->
                            loadIcon(R.drawable.baseline_subscriptions_24)
                        is CommunityRef.ModeratedCommunities ->
                            loadIcon(R.drawable.outline_shield_24)
                    }
                } else {
                    loadCommunityIcon()
                }

                if (item.communityRef is CommunityRef.MultiCommunity) {
                    b.typeIcon.visibility = View.VISIBLE
                    b.typeIcon.setImageResource(R.drawable.baseline_dynamic_feed_24)
                } else {
                    b.typeIcon.visibility = View.GONE
                }

                b.title.text = item.communityRef.getName(context)
                b.subtitle.text = item.communityRef.getLocalizedFullNameSpannable(context)
                b.root.setOnClickListener {
                    onCommunitySelected(Either.Left(item.userCommunityItem), item.resetTabOnClick)
                }
                b.root.setOnLongClickListener {
                    PopupMenu(context, it).apply {
                        inflate(R.menu.menu_user_community_item)

                        val multiCommunity = item.communityRef as? CommunityRef.MultiCommunity

                        menu.findItem(R.id.edit).isVisible = multiCommunity != null

                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.delete -> {
                                    onDeleteUserCommunity(item.userCommunityItem.id)
                                    true
                                }
                                R.id.edit -> {
                                    if (multiCommunity != null) {
                                        onEditMultiCommunity(item.userCommunityItem)
                                    }
                                    true
                                }
                                else -> false
                            }
                        }

                        show()
                    }
                    true
                }
            }
            addItemType(
                clazz = Item.SubscriptionHeaderItem::class,
                inflateFn = BookmarkedCommunityHeaderItemBinding::inflate,
            ) { item, b, h ->
                if (item.isRefreshing) {
                    b.progressBar.visibility = View.VISIBLE
                } else {
                    b.progressBar.visibility = View.GONE
                }
            }
            addItemType(
                clazz = Item.SubscribedCommunityItem::class,
                inflateFn = GenericCommunityItemBinding::inflate,
            ) { item, b, h ->
                avatarHelper.loadCommunityIcon(b.icon, item.communityRef, item.iconUrl)

                b.selectedIndicator.visibility = if (item.isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                b.title.text = item.communityRef.getName(context)
                b.subtitle.text = item.communityRef.getLocalizedFullNameSpannable(context)

                b.root.setOnClickListener {
                    onCommunitySelected(Either.Right(item.communityRef), item.resetTabOnClick)
                }
            }
            addItemType(
                clazz = Item.NoSubscriptionsItem::class,
                inflateFn = NoSubscriptionsItemBinding::inflate,
            ) { _, _, _ -> }
            addItemType(
                clazz = Item.TabStateItem::class,
                inflateFn = TabStateItemBinding::inflate,
            ) { item, b, h ->
                b.textView.text = item.tabState.currentCommunity.getName(context)
                b.selectedIndicator.visibility = if (item.isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                b.root.setOnClickListener {
                    when (val tab = item.tab) {
                        is TabsManager.Tab.SubscribedCommunityTab ->
                            onCommunitySelected(Either.Right(tab.subscribedCommunity), false)
                        is TabsManager.Tab.UserCommunityTab ->
                            onCommunitySelected(Either.Left(tab.userCommunityItem), false)
                    }
                }
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems(cb: (() -> Unit)? = null) {
            val data = data ?: return
            val filter = filter

            val newItems = mutableListOf<Item>()

            if (filter.isNullOrBlank()) {
                newItems.add(Item.BookmarkHeaderItem)

                for (userCommunity in data.userCommunities) {
                    val isSelected = currentTab?.hasTabId(userCommunity.id) ?: false
                    val tab = userCommunity.toTab()

                    val tabStateItem = data.tabsState[tab]?.let {
                        if (it.currentCommunity == userCommunity.communityRef) {
                            null
                        } else {
                            Item.TabStateItem(
                                "tabid:${userCommunity.id}",
                                tab,
                                it,
                                isSelected,
                            )
                        }
                    }

                    if (userCommunity.id == UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID) {
                        newItems += Item.HomeCommunityItem(
                            communityRef = userCommunity.communityRef,
                            userCommunityItem = userCommunity,
                            isSelected = isSelected && tabStateItem == null,
                            resetTabOnClick = tabStateItem != null,
                        )
                    } else {
                        newItems += Item.BookmarkedCommunityItem(
                            communityRef = userCommunity.communityRef,
                            iconUrl = userCommunity.iconUrl,
                            userCommunityItem = userCommunity,
                            isSelected = isSelected && tabStateItem == null,
                            resetTabOnClick = tabStateItem != null,
                        )
                    }

                    if (tabStateItem != null) {
                        newItems += tabStateItem
                    }
                }

                newItems.add(
                    Item.SubscriptionHeaderItem(
                        data.accountInfoUpdateState is StatefulData.Loading,
                    ),
                )
                if (data.subscriptionCommunities.isNotEmpty()) {
                    for (subscriptionCommunity in data.subscriptionCommunities) {
                        val isSelected = currentTab
                            ?.isSubscribedCommunity(subscriptionCommunity.toCommunityRef())
                            ?: false
                        val tab = subscriptionCommunity.toCommunityRef().toTab()

                        val tabStateItem = data.tabsState[tab]?.let {
                            if (it.currentCommunity == subscriptionCommunity.toCommunityRef()) {
                                null
                            } else {
                                Item.TabStateItem(
                                    subscriptionCommunity.name,
                                    tab,
                                    tabState = it,
                                    isSelected = isSelected,
                                )
                            }
                        }

                        newItems += Item.SubscribedCommunityItem(
                            communityRef = subscriptionCommunity.toCommunityRef(),
                            iconUrl = subscriptionCommunity.icon,
                            isSelected = isSelected && tabStateItem == null,
                            resetTabOnClick = tabStateItem != null,
                        )
                        if (tabStateItem != null) {
                            newItems += tabStateItem
                        }
                    }
                } else {
                    newItems.add(Item.NoSubscriptionsItem)
                }
            } else {
                for (userCommunity in data.userCommunities) {
                    val isSelected = currentTab?.hasTabId(userCommunity.id) ?: false
                    val tab = userCommunity.toTab()
                    val tabState = data.tabsState[tab]
                    val tabStateName = tabState?.currentCommunity?.getName(context)

                    if (tabStateName?.contains(filter, ignoreCase = true) != true &&
                        !userCommunity.communityRef.getName(context).contains(filter, ignoreCase = true)
                    ) {
                        continue
                    }

                    val tabStateItem = tabState?.let {
                        if (it.currentCommunity == userCommunity.communityRef) {
                            null
                        } else {
                            Item.TabStateItem(
                                "tabid:${userCommunity.id}",
                                tab,
                                it,
                                isSelected,
                            )
                        }
                    }

                    if (userCommunity.id == UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID) {
                        newItems += Item.HomeCommunityItem(
                            communityRef = userCommunity.communityRef,
                            userCommunityItem = userCommunity,
                            isSelected = isSelected && tabStateItem == null,
                            resetTabOnClick = tabStateItem != null,
                        )
                    } else {
                        newItems += Item.BookmarkedCommunityItem(
                            communityRef = userCommunity.communityRef,
                            iconUrl = userCommunity.iconUrl,
                            userCommunityItem = userCommunity,
                            isSelected = isSelected && tabStateItem == null,
                            resetTabOnClick = tabStateItem != null,
                        )
                    }

                    if (tabStateItem != null) {
                        newItems += tabStateItem
                    }
                }
                for (subscriptionCommunity in data.subscriptionCommunities) {
                    val communityRef = subscriptionCommunity.toCommunityRef()
                    val isSelected = currentTab
                        ?.isSubscribedCommunity(communityRef)
                        ?: false
                    val tab = communityRef.toTab()
                    val tabState = data.tabsState[tab]
                    val tabStateName = tabState?.currentCommunity?.getName(context)

                    if (tabStateName?.contains(filter, ignoreCase = true) != true &&
                        !communityRef.getName(context).contains(filter, ignoreCase = true)
                    ) {
                        continue
                    }

                    val tabStateItem = tabState?.let {
                        if (it.currentCommunity == communityRef) {
                            null
                        } else {
                            Item.TabStateItem(
                                subscriptionCommunity.name,
                                tab,
                                tabState = it,
                                isSelected = isSelected,
                            )
                        }
                    }

                    newItems += Item.SubscribedCommunityItem(
                        communityRef = communityRef,
                        iconUrl = subscriptionCommunity.icon,
                        isSelected = isSelected && tabStateItem == null,
                        resetTabOnClick = tabStateItem != null,
                    )
                    if (tabStateItem != null) {
                        newItems += tabStateItem
                    }
                }
            }

            adapterHelper.setItems(newItems, this, cb)
        }

        fun filter(f: String?, cb: () -> Unit) {
            filter = f

            refreshItems(cb)
        }
    }
}
