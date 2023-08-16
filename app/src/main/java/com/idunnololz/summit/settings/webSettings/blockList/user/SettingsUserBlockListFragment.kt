package com.idunnololz.summit.settings.webSettings.blockList.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.databinding.BlockListUserItemBinding
import com.idunnololz.summit.databinding.FragmentSettingsUserBlockListBinding
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.settings.webSettings.blockList.SettingsAccountBlockListViewModel
import com.idunnololz.summit.settings.webSettings.blockList.SettingsAccountBlockListViewModel.BlockedPersonItem
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsUserBlockListFragment : BaseFragment<FragmentSettingsUserBlockListBinding>() {

    private val viewModel: SettingsAccountBlockListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsUserBlockListBinding.inflate(inflater, container, false))

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
            supportActionBar?.title = context.getString(R.string.blocked_users)
        }

        with(binding) {
//            val adapter = UserBlockListAdapter() {
//                actionsViewModel.blockPerson(it, false)
//            }
//
//            actionsViewModel.blockPersonResult.observe(viewLifecycleOwner) {
//                when (it) {
//                    is StatefulData.Error -> {
//                        ErrorDialogFragment.show(
//                            getString(R.string.error_unblock_failed),
//                            it.error,
//                            childFragmentManager,
//                        )
//                    }
//                    is StatefulData.Loading -> {
//
//                    }
//                    is StatefulData.NotStarted -> TODO()
//                    is StatefulData.Success -> TODO()
//                }
//            }

            val adapter = UserBlockListAdapter(
                onRemoveUser = {
                    viewModel.unblockPerson(it)
                },
            )
            binding.recyclerView.setHasFixedSize(true)
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = LinearLayoutManager(context)

            viewModel.userBlockList.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> loadingView.showDefaultErrorMessageFor(it.error)
                    is StatefulData.Loading -> loadingView.showProgressBar()
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        adapter.data = it.data

                        if (it.data.isEmpty()) {
                            loadingView.showErrorText(R.string.there_doesnt_seem_to_be_anything_here)
                        }
                    }
                }
            }
        }
    }

    private class UserBlockListAdapter(
        val onRemoveUser: (PersonId) -> Unit,
    ) : Adapter<ViewHolder>() {

        var data: List<BlockedPersonItem> = listOf()
            set(value) {
                field = value

                refreshItems()
            }

        private val adapterHelper = AdapterHelper<BlockedPersonItem>(
            areItemsTheSame = { old, new ->
                old.blockedPerson.target.id == new.blockedPerson.target.id
            },
        ).apply {
            addItemType(BlockedPersonItem::class, BlockListUserItemBinding::inflate) { item, b, _ ->
                b.icon.load(item.blockedPerson.target.avatar)
                b.title.text = item.blockedPerson.target.fullName

                if (item.isRemoving) {
                    b.delete.visibility = View.GONE
                    b.progressBar.visibility = View.VISIBLE
                } else {
                    b.delete.visibility = View.VISIBLE
                    b.progressBar.visibility = View.GONE
                }

                b.delete.setOnClickListener {
                    onRemoveUser(item.blockedPerson.target.id)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            adapterHelper.setItems(data, this)
        }
    }
}
