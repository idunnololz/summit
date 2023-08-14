package com.idunnololz.summit.lemmy.multicommunity

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.AlertDialogFragment
import com.idunnololz.summit.databinding.CommunityIconItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorGroupItemBinding
import com.idunnololz.summit.databinding.CommunitySelectorNoResultsItemBinding
import com.idunnololz.summit.databinding.DialogFragmentMultiCommunityEditorBinding
import com.idunnololz.summit.databinding.MultiCommunityHeaderItemBinding
import com.idunnololz.summit.databinding.MultiCommunityIconSelectorBinding
import com.idunnololz.summit.databinding.MultiCommunitySelectedCommunitiesItemBinding
import com.idunnololz.summit.databinding.MultiCommunitySelectedCommunityBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.offline.OfflineManager
import com.idunnololz.summit.settings.util.HorizontalSpaceItemDecoration
import com.idunnololz.summit.user.UserCommunitiesManager
import com.idunnololz.summit.util.BackPressHandler
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.FullscreenDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.focusAndShowKeyboard
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.ext.showAllowingStateLoss
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.recyclerView.getBinding
import com.idunnololz.summit.util.recyclerView.isBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MultiCommunityEditorDialogFragment :
    BaseDialogFragment<DialogFragmentMultiCommunityEditorBinding>(),
    FullscreenDialogFragment,
    BackPressHandler {

    companion object {
        const val REQUEST_KEY = "MultiCommunityEditorDialogFragment_req_key"
        const val REQUEST_KEY_RESULT = "result"

        fun show(
            fragmentManager: FragmentManager,
            multiCommunity: CommunityRef.MultiCommunity,
            dbId: Long = 0,
        ) =
            MultiCommunityEditorDialogFragment()
                .apply {
                    arguments = MultiCommunityEditorDialogFragmentArgs(
                        multiCommunity,
                        dbId,
                    ).toBundle()
                }
                .showAllowingStateLoss(fragmentManager, "MultiCommunityEditorDialogFragment")
    }

    private val args by navArgs<MultiCommunityEditorDialogFragmentArgs>()

    private val viewModel: MultiCommunityEditorViewModel by viewModels()

    private var adapter: CommunityAdapter? = null

    @Inject
    lateinit var offlineManager: OfflineManager

    @Inject
    lateinit var userCommunitiesManager: UserCommunitiesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_App_DialogFullscreen)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentMultiCommunityEditorBinding.inflate(
            inflater,
            container,
            false
        ))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val multiCommunityAdapter = MultiCommunityAdapter(
            context = context,
            offlineManager = offlineManager,
            onChooseCommunitiesClick = {
                viewModel.showSearch.value = true
            },
            onCommunityNameChanged = {
                viewModel.communityName.value = it
            },
            onIconSelected = {
                viewModel.selectedIcon.value = it
            },
            onCommunityClick = {
                getMainActivity()?.showCommunityInfo(it)
            }
        )

        requireMainActivity().apply {
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)
            insetViewAutomaticallyByPadding(viewLifecycleOwner, binding.communitySelectorContainer)
        }

        with(binding) {
            toolbar.title = getString(R.string.multi_community_editor)
            toolbar.setNavigationIcon(R.drawable.baseline_close_24)
            toolbar.setNavigationIconTint(context.getColorFromAttribute(io.noties.markwon.R.attr.colorControlNormal))
            toolbar.setNavigationOnClickListener {
                dismiss()
            }
            toolbar.inflateMenu(R.menu.menu_multi_community_editor)
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.ok -> {
                        val adapter = adapter
                        val selectedCommunities = adapter?.selectedCommunities?.toList() ?: listOf()

                        if (selectedCommunities.isEmpty()) {
                            AlertDialogFragment.Builder()
                                .setTitle(R.string.error_no_communities_selected)
                                .setMessage(R.string.error_no_communities_selected_desc)
                                .setPositiveButton(android.R.string.ok)
                                .createAndShow(childFragmentManager, "defsdaf")
                            return@setOnMenuItemClickListener true
                        }

                        val ref = CommunityRef.MultiCommunity(
                            viewModel.communityName.value ?: "",
                            viewModel.selectedIcon.value,
                            selectedCommunities,
                        )

                        userCommunitiesManager.addUserCommunity(
                            dbId = args.dbId,
                            communityRef = ref,
                            icon = ref.icon,
                        )

                        setFragmentResult(REQUEST_KEY, bundleOf(
                            REQUEST_KEY_RESULT to ref
                        ))
                        dismiss()

                        true
                    }
                    else -> false
                }
            }

            searchEditText.addTextChangedListener {
                val query = it?.toString() ?: ""
                adapter?.setQuery(query) {
                    recyclerView.scrollToPosition(0)
                }
                viewModel.doQuery(query)
            }

            adapter = CommunityAdapter(context, offlineManager)
            resultsRecyclerView.adapter = adapter
            resultsRecyclerView.setHasFixedSize(true)
            resultsRecyclerView.layoutManager = LinearLayoutManager(context)

            if (savedInstanceState == null) {
                viewModel.communityName.value = args.multiCommunity.name
                viewModel.selectedIcon.value = args.multiCommunity.icon
                adapter?.setSelectedCommunities(args.multiCommunity.communities)
                viewModel.setSelectedCommunities(args.multiCommunity.communities)
            }

            recyclerView.adapter = multiCommunityAdapter
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = LinearLayoutManager(context)
        }

        viewModel.showSearch.observe(viewLifecycleOwner) {
            if (it) {
                showSearch()
            } else {
                hideSearch()
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) {
            when (it) {
                is StatefulData.Error -> {
                    adapter?.setQueryServerResults(listOf())
                }
                is StatefulData.Loading -> {
                    adapter?.setQueryServerResultsInProgress()
                }
                is StatefulData.NotStarted -> {}
                is StatefulData.Success -> {
                    adapter?.setQueryServerResults(it.data)
                }
            }
        }

        fun updateAdapter() {
            val selectedCommunities = viewModel.selectedCommunitiesLiveData.value
            val communityIcons = viewModel.communityIcons.valueOrNull
            val communityName = viewModel.communityName.value
            val selectedIcon = viewModel.selectedIcon.value

            multiCommunityAdapter.setData(
                MultiCommunityAdapter.Data(
                    communityName ?: "",
                    selectedCommunities ?: listOf(),
                    communityIcons ?: listOf(),
                    selectedIcon,
                )
            )
        }

        viewModel.selectedCommunitiesLiveData.observe(viewLifecycleOwner) {
            updateAdapter()
        }
        viewModel.communityIcons.observe(viewLifecycleOwner) {
            updateAdapter()
        }
        viewModel.selectedIcon.observe(viewLifecycleOwner) {
            updateAdapter()
        }
        viewModel.communityName.observe(viewLifecycleOwner) {
            updateAdapter()
        }

        hideSearch(animate = false)
        updateAdapter()
    }

    override fun onBackPressed(): Boolean {
        if (viewModel.showSearch.value == true) {
            viewModel.showSearch.value = false
            return true
        }

        try {
            dismiss()
        } catch (e: IllegalStateException) {
            // do nothing... very rare
        }
        return true
    }

    private fun showSearch() {
        if (!isBindingAvailable()) return

        binding.communitySelectorContainer.visibility = View.VISIBLE
        binding.communitySelectorContainer.alpha = 0f
        binding.communitySelectorContainer.animate()
            .alpha(1f)
        binding.searchEditText.requestFocus()
        binding.root.findFocus()?.focusAndShowKeyboard()
    }

    private fun hideSearch(animate: Boolean = true) {
        Utils.hideKeyboard(requireMainActivity())

        if (animate) {
            binding.communitySelectorContainer.animate()
                .alpha(0f)
                .withEndAction {
                    binding.communitySelectorContainer.visibility = View.GONE
                    binding.communitySelectorContainer.alpha = 1f
                }
        } else {
            binding.communitySelectorContainer.visibility = View.GONE
            binding.communitySelectorContainer.alpha = 1f
        }
        adapter?.selectedCommunities?.let {
            viewModel.setSelectedCommunities(it.toList())
        }
    }

    private class MultiCommunityAdapter(
        private val context: Context,
        private val offlineManager: OfflineManager,
        private val onChooseCommunitiesClick: () -> Unit,
        private val onCommunityNameChanged: (String?) -> Unit,
        private val onIconSelected: (String?) -> Unit,
        private val onCommunityClick: (CommunityRef) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private sealed interface Item {

            data class HeaderItem(
                val communityName: String
            ) : Item

            data class GroupHeaderItem(
                val text: String,
                val stillLoading: Boolean = false,
            ) : Item

            object SelectedCommunitiesHeader : Item

            data class NoResultsItem(
                val text: String,
            ) : Item

            data class SelectedCommunityItem(
                val communityRef: CommunityRef.CommunityRefByName,
            ) : Item

            object IconsItem : Item
        }

        data class Data(
            val communityName: String,
            val communities: List<CommunityRef.CommunityRefByName>,
            val icons: List<String>,
            val selectedIcon: String?,
        )

        private var data: Data? = null

        private val iconAdapter = IconAdapter(context, offlineManager, onIconSelected)

        private val adapterHelper = AdapterHelper<Item> (
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    is Item.SelectedCommunityItem -> {
                        old == new
                    }
                    is Item.GroupHeaderItem -> {
                        old.text == (new as Item.GroupHeaderItem).text
                    }
                    is Item.NoResultsItem -> true
                    is Item.IconsItem -> true
                    is Item.HeaderItem -> true
                    is Item.SelectedCommunitiesHeader -> true
                }
            },
            getChangePayload = { old, new ->
                when (new) {
                    is Item.HeaderItem -> new.communityName
                    else -> null
                }
            }
        ).apply {
            addItemType(
                clazz = Item.HeaderItem::class,
                inflateFn = MultiCommunityHeaderItemBinding::inflate,
                onViewCreated = { b ->
                    b.nameEditText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            p0: CharSequence?,
                            p1: Int,
                            p2: Int,
                            p3: Int,
                        ) {
                        }

                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        }

                        override fun afterTextChanged(p0: Editable?) {
                            onCommunityNameChanged(p0?.toString())
                        }

                    })
                }
            ) { item, b, h ->
                b.nameEditText.setText(item.communityName)
            }
            addItemType(
                clazz = Item.SelectedCommunitiesHeader::class,
                inflateFn = MultiCommunitySelectedCommunitiesItemBinding::inflate
            ) { item, b, _ ->
                b.edit.setOnClickListener {
                    onChooseCommunitiesClick()
                }
            }
            addItemType(
                clazz = Item.GroupHeaderItem::class,
                inflateFn = CommunitySelectorGroupItemBinding::inflate,
            ) { item, b, _ ->
                b.titleTextView.text = item.text

                if (item.stillLoading) {
                    b.progressBar.visibility = View.VISIBLE
                } else {
                    b.progressBar.visibility = View.GONE
                }
            }

            addItemType(
                clazz = Item.SelectedCommunityItem::class,
                inflateFn = MultiCommunitySelectedCommunityBinding::inflate,
            ) { item, b, _ ->
                b.icon.load(R.drawable.ic_subreddit_default)

                b.title.text = item.communityRef.name

                @Suppress("SetTextI18n")
                b.monthlyActives.text = "(${item.communityRef.instance})"
                b.root.setOnClickListener {
                    onCommunityClick(item.communityRef)
                }
            }
            addItemType(
                clazz = Item.IconsItem::class,
                inflateFn = MultiCommunityIconSelectorBinding::inflate,
                onViewCreated = { b ->
                    val recyclerView = b.recyclerView

                    recyclerView.layoutManager = LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL,
                        false,
                    )
                    recyclerView.addItemDecoration(
                        HorizontalSpaceItemDecoration(
                            sizePx = context.resources.getDimensionPixelOffset(R.dimen.padding_half),
                            startAndEndSpacePx = 0,
                        ),
                    )
                    recyclerView.adapter = iconAdapter
                    recyclerView.isNestedScrollingEnabled = false
                },
            ) { _, _, _ ->
            }
            addItemType(
                clazz = Item.NoResultsItem::class,
                inflateFn = CommunitySelectorNoResultsItemBinding::inflate,
            ) { item, b, _ ->
                b.text.text = item.text
            }
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
            payloads: MutableList<Any>,
        ) {
            if (holder.isBinding<MultiCommunityHeaderItemBinding>() && payloads.isNotEmpty()) {
//                val currentValue = holder.getBinding<MultiCommunityHeaderItemBinding>().nameEditText.text.toString()
//                val newValue = (adapterHelper.items[position] as Item.HeaderItem).communityName
//                Log.d("HAHA", "cur: '$currentValue' new: '$newValue'")

                val nameEditText = holder.getBinding<MultiCommunityHeaderItemBinding>().nameEditText
                if (nameEditText.text.isNullOrBlank()) {
                    nameEditText
                        .setText(payloads[0] as String)
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            adapterHelper.onBindViewHolder(holder, position)
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            offlineManager.cancelFetch(holder.itemView)
        }

        override fun getItemCount(): Int = adapterHelper.itemCount

        fun setData(data: Data) {
            this.data = data

            refreshItems { }
        }

        private fun refreshItems(cb: () -> Unit) {
            val data = data ?: return

            val newItems = mutableListOf<Item>()

            newItems += Item.HeaderItem(
                data.communityName
            )

            newItems.add(Item.GroupHeaderItem(context.getString(R.string.multi_community_icon)))
            if (data.icons.isEmpty()) {
                newItems += Item.NoResultsItem(context.getString(R.string.no_icons))
            } else {
                newItems.add(Item.IconsItem)
            }

            newItems.add(Item.SelectedCommunitiesHeader)

            if (data.communities.isEmpty()) {
                newItems += Item.NoResultsItem(context.getString(R.string.no_communities_selected))
            } else {
                data.communities.forEach {
                    newItems += Item.SelectedCommunityItem(
                        it,
                    )
                }
            }

            adapterHelper.setItems(newItems, this, cb)
            iconAdapter.selectedIcon = data.selectedIcon
            iconAdapter.setIcons(data.icons) {}
        }
    }

    private class IconAdapter(
        private val context: Context,
        private val offlineManager: OfflineManager,
        private val onIconSelected: (String?) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val highlightBorderColor = context.getColorCompat(
            R.color.colorAccent,
        )
        private val normalBorderColor = context.getColorCompat(
            R.color.colorText,
        )

        private sealed interface Item {
            data class IconItem(
                val icon: String,
                val isSelected: Boolean,
            ) : Item
        }

        private var data: List<String> = listOf()

        var selectedIcon: String? = null
            set(value) {
                field = value

                refreshItems {}
            }

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                if (old::class != new::class) {
                    return@AdapterHelper false
                }

                when (old) {
                    is Item.IconItem ->
                        old.icon == (new as Item.IconItem).icon
                }
            },
        ).apply {
            addItemType(
                clazz = Item.IconItem::class,
                inflateFn = CommunityIconItemBinding::inflate,
            ) { item, b, _ ->
                offlineManager.fetchImage(b.image, item.icon) {
                    b.image.load(it)
                }

                if (item.isSelected) {
                    b.root.strokeColor = highlightBorderColor
                } else {
                    b.root.strokeColor = normalBorderColor
                }
                b.root.setOnClickListener {
                    selectedIcon = item.icon

                    onIconSelected(item.icon)
                }
            }
        }

        init {
            refreshItems {}
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            adapterHelper.onBindViewHolder(holder, position)
        }

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        fun setIcons(data: List<String>, cb: () -> Unit) {
            this.data = data

            refreshItems(cb)
        }

        private fun refreshItems(cb: () -> Unit) {
            adapterHelper.setItems(
                data.map {
                    Item.IconItem(
                        icon = it,
                        isSelected = it == selectedIcon,
                    )
                },
                this,
                cb,
            )
        }
    }
}