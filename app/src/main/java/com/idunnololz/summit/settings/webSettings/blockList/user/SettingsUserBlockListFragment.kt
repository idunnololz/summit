package com.idunnololz.summit.settings.webSettings.blockList.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.PersonBlockView
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.utils.fullName
import com.idunnololz.summit.databinding.BlockListUserItemBinding
import com.idunnololz.summit.databinding.FragmentSettingsCommunityBlockListBinding
import com.idunnololz.summit.databinding.FragmentSettingsUserBlockListBinding
import com.idunnololz.summit.error.ErrorDialogFragment
import com.idunnololz.summit.lemmy.MoreActionsViewModel
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsUserBlockListFragment : BaseFragment<FragmentSettingsUserBlockListBinding>() {

    private val viewModel: SettingsAccountBlockListViewModel by viewModels()
    private val actionsViewModel: MoreActionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentSettingsUserBlockListBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            val adapter = UserBlockListAdapter() {
                actionsViewModel.blockPerson(it, false)
            }

            actionsViewModel.blockPersonResult.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {
                        ErrorDialogFragment.show(
                            getString(R.string.error_unblock_failed),
                            it.error,
                            childFragmentManager,
                        )
                    }
                    is StatefulData.Loading -> {

                    }
                    is StatefulData.NotStarted -> TODO()
                    is StatefulData.Success -> TODO()
                }
            }

            viewModel.userBlockList.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> loadingView.showDefaultErrorMessageFor(it.error)
                    is StatefulData.Loading -> loadingView.showProgressBar()
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()

                        setupViews()
                    }
                }
            }
        }
    }

    private fun setupViews() {
        if (!isBindingAvailable()) return

        val data = viewModel.userBlockList.valueOrNull ?: return



    }

    private class UserBlockListAdapter(
        val onRemoveUser: (PersonId) -> Unit
    ) : Adapter<ViewHolder>() {

        var data: List<PersonBlockView> = listOf()
            set(value) {
                field = value

                refreshItems()
            }

        private val adapterHelper = AdapterHelper<PersonBlockView>(
            areItemsTheSame = { old, new ->
                old.person.id == new.person.id
            }
        ).apply {
            addItemType(PersonBlockView::class, BlockListUserItemBinding::inflate) { item, b, h ->
                b.icon.load(item.person.avatar)
                b.title.text = item.person.fullName
                b.delete.setOnClickListener {
//                    onRemoveUser(item)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int  =
            adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun refreshItems() {
            adapterHelper.setItems(data, this)
        }

    }
}