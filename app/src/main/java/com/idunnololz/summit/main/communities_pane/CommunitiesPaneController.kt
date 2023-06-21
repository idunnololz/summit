package com.idunnololz.summit.main.communities_pane

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
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
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CommunitiesPaneController @AssistedInject constructor(
    private val offlineManager: OfflineManager,
    @Assisted private val viewModel: CommunitiesPaneViewModel,
    @Assisted private val binding: CommunitiesPaneBinding,
    @Assisted private val viewLifecycleOwner: LifecycleOwner,
) {

    @AssistedFactory
    interface Factory {
        fun create(
            viewModel: CommunitiesPaneViewModel,
            binding: CommunitiesPaneBinding,
            viewLifecycleOwner: LifecycleOwner,
        ): CommunitiesPaneController
    }

    init {
        viewModel.loadCommunities()

        val context = binding.root.context

        val adapter = UserCommunitiesAdapter(context, offlineManager)

        viewModel.communities.observe(viewLifecycleOwner) {
            if (it != null) {
                adapter.data = it
            }
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
    ) : Adapter<ViewHolder>() {

        private sealed interface Item {
            object BookmarkHeaderItem : Item
            data class HomeCommunityItem(
                val communityRef: CommunityRef
            ) : Item
            data class BookmarkedCommunityItem(
                val communityRef: CommunityRef,
                val iconUrl: String?,
            ) : Item
            object SubscriptionHeaderItem : Item
            data class SubscribedCommunityItem(
                val communityRef: CommunityRef,
                val iconUrl: String?,
            ) : Item
            object NoSubscriptionsItem : Item
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
                b.icon.load(R.drawable.baseline_home_18)
                b.textView.text = item.communityRef.getName(context)
            }
            addItemType(
                clazz = Item.BookmarkedCommunityItem::class,
                inflateFn = GenericCommunityItemBinding::inflate
            ) { item, b, h ->
                b.icon.setImageResource(R.drawable.ic_subreddit_default)
                offlineManager.fetchImage(h.itemView, item.iconUrl) {
                    b.icon.load(it)
                }
                b.textView.text = item.communityRef.getName(context)
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
                b.textView.text = item.communityRef.getName(context)

                b.root.setOnClickListener {
                    
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
                    Item.HomeCommunityItem(it.communityRef)
                } else {
                    Item.BookmarkedCommunityItem(it.communityRef, it.iconUrl)
                }
            }

            newItems.add(Item.SubscriptionHeaderItem)
            if (data.subscriptionCommunities.isNotEmpty()) {
                data.subscriptionCommunities.mapTo(newItems) {
                    Item.SubscribedCommunityItem(it.toCommunityRef(), it.icon)
                }
            } else {
                newItems.add(Item.NoSubscriptionsItem)
            }

            adapterHelper.setItems(newItems, this)
        }
    }
}