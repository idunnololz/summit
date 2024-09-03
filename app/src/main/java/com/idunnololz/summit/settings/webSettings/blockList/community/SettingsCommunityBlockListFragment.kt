package com.idunnololz.summit.settings.webSettings.blockList.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.CommunityId
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.databinding.BlockListCommunityItemBinding
import com.idunnololz.summit.databinding.FragmentSettingsCommunityBlockListBinding
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.webSettings.blockList.SettingsAccountBlockListViewModel
import com.idunnololz.summit.settings.webSettings.blockList.SettingsAccountBlockListViewModel.BlockedCommunityItem
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByMargins
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsCommunityBlockListFragment :
    BaseFragment<FragmentSettingsCommunityBlockListBinding>() {

    private val viewModel: SettingsAccountBlockListViewModel by viewModels()

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(
            FragmentSettingsCommunityBlockListBinding
                .inflate(inflater, container, false),
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByMargins(viewLifecycleOwner, binding.recyclerView)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = context.getString(R.string.blocked_communities)
        }

        with(binding) {
            val adapter = CommunityBlockListAdapter(
                onRemoveCommunity = {
                    viewModel.unblockCommunity(it)
                },
            )
            binding.recyclerView.setup(animationsHelper)
            binding.recyclerView.setHasFixedSize(true)
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = LinearLayoutManager(context)

            viewModel.communityBlockList.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> loadingView.showDefaultErrorMessageFor(it.error)
                    is StatefulData.Loading -> loadingView.showProgressBar()
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        adapter.data = it.data

                        if (it.data.isEmpty()) {
                            loadingView.showErrorText(
                                R.string.there_doesnt_seem_to_be_anything_here,
                            )
                        }
                    }
                }
            }
        }
    }

    private class CommunityBlockListAdapter(
        val onRemoveCommunity: (CommunityId) -> Unit,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var data: List<BlockedCommunityItem> = listOf()
            set(value) {
                field = value

                refreshItems()
            }

        private val adapterHelper = AdapterHelper<BlockedCommunityItem>(
            areItemsTheSame = { old, new ->
                old.blockedCommunity.community.id == new.blockedCommunity.community.id
            },
        ).apply {
            addItemType(
                BlockedCommunityItem::class,
                BlockListCommunityItemBinding::inflate,
            ) { item, b, h ->
                b.icon.load(item.blockedCommunity.community.icon)
                b.title.text = item.blockedCommunity.community.fullName

                if (item.isRemoving) {
                    b.delete.visibility = View.GONE
                    b.progressBar.visibility = View.VISIBLE
                } else {
                    b.delete.visibility = View.VISIBLE
                    b.progressBar.visibility = View.GONE
                }

                b.delete.setOnClickListener {
                    onRemoveCommunity(item.blockedCommunity.community.id)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            adapterHelper.setItems(data, this)
        }
    }
}
