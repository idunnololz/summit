package com.idunnololz.summit.lemmy.person

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunityModeratorView
import com.idunnololz.summit.api.dto.PersonView
import com.idunnololz.summit.api.utils.instance
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.FragmentPersonAboutBinding
import com.idunnololz.summit.databinding.PersonInfoItemBinding
import com.idunnololz.summit.databinding.PersonInfoModeratesItemBinding
import com.idunnololz.summit.databinding.PersonInfoTitleBinding
import com.idunnololz.summit.lemmy.LemmyTextHelper
import com.idunnololz.summit.lemmy.PageRef
import com.idunnololz.summit.lemmy.appendNameWithInstance
import com.idunnololz.summit.lemmy.toCommunityRef
import com.idunnololz.summit.links.LinkContext
import com.idunnololz.summit.links.onLinkClick
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.preview.VideoType
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.showMoreLinkOptions
import com.idunnololz.summit.util.tsToConcise
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PersonAboutFragment : BaseFragment<FragmentPersonAboutBinding>() {

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    @Inject
    lateinit var avatarHelper: AvatarHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentPersonAboutBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentFragment = parentFragment as PersonTabbedFragment

        with(binding) {
            parentFragment.viewModel.personData.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.showDefaultErrorMessageFor(it.error)
                    }
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        swipeRefreshLayout.isRefreshing = false
                        loadingView.hideAll()

                        setup(it.data)
                    }
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                parentFragment.viewModel.fetchPage(
                    pageIndex = 0,
                    isPeronInfoFetch = true,
                    force = true,
                )
            }
        }
    }

    private fun setup(data: PersonTabbedViewModel.PersonDetailsData) {
        if (!isBindingAvailable()) return

        val context = requireContext()

        val parentFragment = parentFragment as PersonTabbedFragment

        val adapter = PersonInfoAdapter(
            context = context,
            instance = parentFragment.viewModel.instance,
            offlineManager = offlineManager,
            avatarHelper = avatarHelper,
            onImageClick = { url ->
                getMainActivity()?.openImage(
                    sharedElement = null,
                    appBar = parentFragment.binding.appBar,
                    title = null,
                    url = url,
                    mimeType = null,
                )
            },
            onVideoClick = { url ->
                getMainActivity()?.openVideo(url, VideoType.Unknown, null)
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
        )

        adapter.setData(data)

        binding.recyclerView.apply {
            setup(animationsHelper)
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            this.adapter = adapter
        }
    }

    private class PersonInfoAdapter(
        private val context: Context,
        private val instance: String,
        private val offlineManager: OfflineManager,
        private val avatarHelper: AvatarHelper,
        private val onImageClick: (url: String) -> Unit,
        private val onVideoClick: (url: String) -> Unit,
        private val onPageClick: (PageRef) -> Unit,
        private val onLinkClick: (url: String, text: String, linkContext: LinkContext) -> Unit,
        private val onLinkLongClick: (url: String, text: String) -> Unit,
    ) : Adapter<ViewHolder>() {

        sealed interface Item {
            data class InfoItem(
                val personView: PersonView,
            ) : Item

            data class TitleItem(
                val title: String,
            ) : Item

            data class ModeratedCommunity(
                val community: CommunityModeratorView,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                when (old) {
                    is Item.InfoItem -> true
                    is Item.TitleItem ->
                        old.title == (new as Item.TitleItem).title
                    is Item.ModeratedCommunity ->
                        old.community.community.id ==
                            (new as Item.ModeratedCommunity).community.community.id
                }
            },
        ).apply {
            addItemType(Item.InfoItem::class, PersonInfoItemBinding::inflate) { item, b, h ->
                val personView = item.personView

                LemmyTextHelper.bindText(
                    b.bio,
                    personView.person.bio
                        ?: buildString {
                            append("*")
                            append(context.getString(R.string.blank))
                            append("*")
                        },
                    instance,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    onPageClick = onPageClick,
                    onLinkClick = onLinkClick,
                    onLinkLongClick = onLinkLongClick,
                )

                b.accountInfo.text = buildString {
                    append(
                        if (personView.person.admin) {
                            context.getString(R.string.admin)
                        } else if (personView.person.bot_account) {
                            context.getString(R.string.bot)
                        } else {
                            context.getString(R.string.normal)
                        },
                    )

                    val status =
                        if (personView.person.banned) {
                            if (personView.person.ban_expires != null) {
                                context.getString(
                                    R.string.banned_until_format,
                                    tsToConcise(context, personView.person.ban_expires),
                                )
                            } else {
                                context.getString(R.string.banned)
                            }
                        } else if (personView.person.deleted) {
                            context.getString(R.string.deleted)
                        } else {
                            null
                        }

                    if (status != null) {
                        append(" (")
                        append(status)
                        append(")")
                    }
                }
            }
            addItemType(Item.TitleItem::class, PersonInfoTitleBinding::inflate) { item, b, h ->
                b.title.text = item.title
            }
            addItemType(
                Item.ModeratedCommunity::class,
                PersonInfoModeratesItemBinding::inflate,
            ) { item, b, h ->
                val community = item.community

                avatarHelper.loadCommunityIcon(
                    b.icon,
                    community.community,
                )

                b.name.text = SpannableStringBuilder().apply {
                    appendNameWithInstance(
                        context = context,
                        name = community.community.name,
                        instance = community.community.instance,
                    )
                }
                b.root.setOnClickListener {
                    onPageClick(community.community.toCommunityRef())
                }
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        fun setData(data: PersonTabbedViewModel.PersonDetailsData) {
            val items = mutableListOf<Item>()

            items.add(Item.InfoItem(data.personView))

            if (data.moderates.isNotEmpty()) {
                items.add(Item.TitleItem(context.getString(R.string.moderated_communities)))

                for (community in data.moderates) {
                    items.add(Item.ModeratedCommunity(community))
                }
            }

            adapterHelper.setItems(items, this)
        }
    }
}
