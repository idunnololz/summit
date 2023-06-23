package com.idunnololz.summit.main.community_info_pane

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.idunnololz.summit.api.dto.CommunityView
import com.idunnololz.summit.api.dto.GetSiteResponse
import com.idunnololz.summit.api.dto.SubscribedType
import com.idunnololz.summit.databinding.CommunityHeaderItemBinding
import com.idunnololz.summit.databinding.CommunityInfoPaneBinding
import com.idunnololz.summit.databinding.SiteHeaderItemBinding
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CommunityInfoController @AssistedInject constructor(
    private val offlineManager: OfflineManager,
    private val userCommunitiesManager: UserCommunitiesManager,
    @Assisted private val viewModel: CommunityInfoViewModel,
    @Assisted private val binding: CommunityInfoPaneBinding,
    @Assisted private val viewLifecycleOwner: LifecycleOwner,
) {
    @AssistedFactory
    interface Factory {
        fun create(
            viewModel: CommunityInfoViewModel,
            binding: CommunityInfoPaneBinding,
            viewLifecycleOwner: LifecycleOwner,
        ): CommunityInfoController
    }

    init {
        val context = binding.root.context

        val siteAdapter = SiteAdapter()
        val communityAdapter = CommunityAdapter(
            context = context, updateSubscriptionStatus = { communityId, subscribe ->
                viewModel.updateSubscriptionStatus(communityId, subscribe)
            }
        )

        binding.loadingView.setOnRefreshClickListener {
            viewModel.refetchCommunityOrSite()
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
                    it.data
                        .onLeft {
                            siteAdapter.data = it
                            binding.recyclerView.adapter = siteAdapter
                        }
                        .onRight {
                            communityAdapter.data = it
                            binding.recyclerView.adapter = communityAdapter
                        }
                }
            }
        }

        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
        }
    }

    fun onShown() {
        viewModel.refetchCommunityOrSite()
    }

    private class SiteAdapter : Adapter<ViewHolder>() {

        private sealed interface Item {
            data class HeaderItem(
                val instance: String,
                val imageUrl: String?,
            ) : Item
        }

        var data: GetSiteResponse? = null
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
                    is Item.HeaderItem ->
                        old.instance == (new as Item.HeaderItem).instance
                }
            }
        ).apply {
            addItemType(Item.HeaderItem::class, SiteHeaderItemBinding::inflate) { item, b, h ->
                b.icon.load(item.imageUrl)
                b.title.text = item.instance
            }
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

            newItems.add(Item.HeaderItem(
                data.site_view.site.name,
                data.site_view.site.icon
            ))

            adapterHelper.setItems(newItems, this)
        }
    }

    private class CommunityAdapter(
        private val context: Context,
        private val updateSubscriptionStatus: (Int, Boolean) -> Unit,
    ) : Adapter<ViewHolder>() {

        private sealed interface Item {
            data class HeaderItem(
                val communityId: Int,
                val communityName: String,
                val imageUrl: String?,
                val subscribed: SubscribedType,
            ) : Item
        }

        var data: CommunityView? = null
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
                    is Item.HeaderItem ->
                        old.communityId == (new as Item.HeaderItem).communityId
                }
            }
        ).apply {
            addItemType(Item.HeaderItem::class, CommunityHeaderItemBinding::inflate) { item, b, h ->
                b.icon.load(item.imageUrl)
                b.title.text = item.communityName

                when (item.subscribed) {
                    SubscribedType.Subscribed -> {
                        b.subscribe.visibility = View.GONE
                        b.unsubscribe.visibility = View.VISIBLE
                    }
                    SubscribedType.NotSubscribed -> {
                        b.subscribe.visibility = View.VISIBLE
                        b.unsubscribe.visibility = View.GONE
                    }
                    SubscribedType.Pending -> {
                        b.subscribe.visibility = View.GONE
                        b.unsubscribe.visibility = View.VISIBLE
                    }
                }

                b.subscribe.setOnClickListener {
                    updateSubscriptionStatus(item.communityId, true)
                }
                b.unsubscribe.setOnClickListener {
                    updateSubscriptionStatus(item.communityId, false)
                }
            }
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

            newItems.add(
                Item.HeaderItem(
                    data.community.id,
                    data.community.name,
                    data.community.icon,
                    data.subscribed,
                )
            )

            adapterHelper.setItems(newItems, this)
        }
    }
}