package com.idunnololz.summit.main.communities_pane

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import arrow.core.Either
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.BookmarkHeaderItemBinding
import com.idunnololz.summit.databinding.BookmarkedCommunityHeaderItemBinding
import com.idunnololz.summit.databinding.CommunitiesPaneBinding
import com.idunnololz.summit.databinding.GenericCommunityItemBinding
import com.idunnololz.summit.databinding.HomeCommunityItemBinding
import com.idunnololz.summit.databinding.NoSubscriptionsItemBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.tabs.TabsManager
import com.idunnololz.summit.tabs.hasTabId
import com.idunnololz.summit.tabs.isSubscribedCommunity
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.user.UserCommunityItem
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

typealias OnCommunitySelected = (Either<UserCommunityItem, CommunityRef>) -> Unit

class CommunitiesPaneController @AssistedInject constructor(
    private val offlineManager: OfflineManager,
    private val userCommunitiesManager: UserCommunitiesManager,
    private val tabsManager: TabsManager,
    @Assisted private val viewModel: CommunitiesPaneViewModel,
    @Assisted private val binding: CommunitiesPaneBinding,
    @Assisted private val viewLifecycleOwner: LifecycleOwner,
    @Assisted private val onCommunitySelected: OnCommunitySelected,
) {

    @AssistedFactory
    interface Factory {
        fun create(
            viewModel: CommunitiesPaneViewModel,
            binding: CommunitiesPaneBinding,
            viewLifecycleOwner: LifecycleOwner,
            onCommunitySelected: OnCommunitySelected,
        ): CommunitiesPaneController
    }

    init {
        viewModel.loadCommunities()

        val context = binding.root.context

        val adapter = UserCommunitiesAdapter(
            context = context,
            offlineManager = offlineManager,
            onCommunitySelected = onCommunitySelected, onDeleteUserCommunity = { id ->
                viewModel.deleteUserCommunity(id)
            }
        )

        viewModel.communities.observe(viewLifecycleOwner) {
            if (it != null) {
                adapter.data = it
            }
        }

        tabsManager.currentTab.observe(viewLifecycleOwner) {
            adapter.currentTab = it
        }

        binding.apply {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
        }
    }

    private class UserCommunitiesAdapter(
        private val context: Context,
        private val offlineManager: OfflineManager,
        private val onCommunitySelected: OnCommunitySelected,
        private val onDeleteUserCommunity: (Long) -> Unit,
    ) : Adapter<ViewHolder>() {

        private sealed interface Item {
            object BookmarkHeaderItem : Item
            data class HomeCommunityItem(
                val communityRef: CommunityRef,
                val userCommunityItem: UserCommunityItem,
                val isSelected: Boolean,
            ) : Item
            data class BookmarkedCommunityItem(
                val communityRef: CommunityRef,
                val iconUrl: String?,
                val userCommunityItem: UserCommunityItem,
                val isSelected: Boolean,
            ) : Item
            object SubscriptionHeaderItem : Item
            data class SubscribedCommunityItem(
                val communityRef: CommunityRef,
                val iconUrl: String?,
                val isSelected: Boolean,
            ) : Item
            object NoSubscriptionsItem : Item
        }

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
                    Item.SubscriptionHeaderItem -> true
                    Item.NoSubscriptionsItem -> true
                }
            }
        ).apply {
            addItemType(
                clazz = Item.BookmarkHeaderItem::class,
                inflateFn = BookmarkHeaderItemBinding::inflate
            ) { item, b, h ->

            }
            addItemType(
                clazz = Item.HomeCommunityItem::class,
                inflateFn = HomeCommunityItemBinding::inflate,
            ) { item, b, h ->
                b.textView.text = item.communityRef.getName(context)
                b.selectedIndicator.visibility = if (item.isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                b.root.setOnClickListener {
                    onCommunitySelected(Either.Left(item.userCommunityItem))
                }
            }
            addItemType(
                clazz = Item.BookmarkedCommunityItem::class,
                inflateFn = GenericCommunityItemBinding::inflate
            ) { item, b, h ->
                b.icon.setImageResource(R.drawable.ic_subreddit_default)
                b.selectedIndicator.visibility = if (item.isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                offlineManager.fetchImage(h.itemView, item.iconUrl) {
                    b.icon.load(it)
                }
                b.textView.text = item.communityRef.getName(context)
                b.root.setOnClickListener {
                    onCommunitySelected(Either.Left(item.userCommunityItem))
                }
                b.root.setOnLongClickListener {
                    PopupMenu(context, it).apply {
                        inflate(R.menu.menu_user_community_item)

                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.delete -> {
                                    onDeleteUserCommunity(item.userCommunityItem.id)
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
                inflateFn = BookmarkedCommunityHeaderItemBinding::inflate
            ) { item, b, h ->
            }
            addItemType(
                clazz = Item.SubscribedCommunityItem::class,
                inflateFn = GenericCommunityItemBinding::inflate
            ) { item, b, h ->
                b.icon.load(R.drawable.ic_subreddit_default)
                offlineManager.fetchImage(h.itemView, item.iconUrl) {
                    b.icon.load(it)
                }
                b.selectedIndicator.visibility = if (item.isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                b.textView.text = item.communityRef.getName(context)

                b.root.setOnClickListener {
                    onCommunitySelected(Either.Right(item.communityRef))
                }
            }
            addItemType(
                clazz = Item.NoSubscriptionsItem::class,
                inflateFn = NoSubscriptionsItemBinding::inflate
            ) { _, _, _ -> }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            val data = data ?: return

            val newItems = mutableListOf<Item>()
            newItems.add(Item.BookmarkHeaderItem)
            data.userCommunities.mapTo(newItems) {
                if (it.id == UserCommunitiesManager.FIRST_FRAGMENT_TAB_ID) {
                    Item.HomeCommunityItem(
                        communityRef = it.communityRef,
                        userCommunityItem = it,
                        isSelected = currentTab?.hasTabId(it.id) ?: false
                    )
                } else {
                    Item.BookmarkedCommunityItem(
                        communityRef = it.communityRef,
                        iconUrl = it.iconUrl,
                        userCommunityItem = it,
                        isSelected = currentTab?.hasTabId(it.id) ?: false
                    )
                }
            }

            newItems.add(Item.SubscriptionHeaderItem)
            if (data.subscriptionCommunities.isNotEmpty()) {
                data.subscriptionCommunities.mapTo(newItems) {
                    Item.SubscribedCommunityItem(
                        communityRef = it.toCommunityRef(),
                        iconUrl = it.icon,
                        isSelected = currentTab?.isSubscribedCommunity(it.toCommunityRef()) ?: false
                    )
                }
            } else {
                newItems.add(Item.NoSubscriptionsItem)
            }

            adapterHelper.setItems(newItems, this)
        }
    }
}
