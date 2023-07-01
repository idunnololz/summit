package com.idunnololz.summit.account_ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import com.idunnololz.summit.CommunityDirections
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.databinding.AccountItemBinding
import com.idunnololz.summit.databinding.AddAccountItemBinding
import com.idunnololz.summit.databinding.CurrentAccountItemBinding
import com.idunnololz.summit.databinding.DialogFragmentAccountsBinding
import com.idunnololz.summit.util.BaseDialogFragment
import com.idunnololz.summit.util.StatefulData
import com.idunnololz.summit.util.ext.navigateSafe
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountsAndSettingsDialogFragment : BaseDialogFragment<DialogFragmentAccountsBinding>() {

    companion object {
        fun newInstance() =
            AccountsAndSettingsDialogFragment()
    }

    private val viewModel: AccountsViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT

            val window = checkNotNull(dialog.window)
            window.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(DialogFragmentAccountsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        with(binding) {
            val adapter = AccountAdapter(
                context,
                signOut = {
                    viewModel.signOut(it.account)
                },
                onAccountClick = {
                    viewModel.switchAccount(it.account)
                },
                onAddAccountClick = {
                    val direction = CommunityDirections.actionGlobalLogin()
                    findNavController().navigateSafe(direction)
                },
            )

            recyclerView.setHasFixedSize(false)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            settings.setOnClickListener {
                requireMainActivity().openSettings()
            }

            viewModel.accounts.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {}
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                        adapter.setAccounts(it.data)
                    }
                }
            }
            viewModel.signOut.observe(viewLifecycleOwner) {
                when (it) {
                    is StatefulData.Error -> {}
                    is StatefulData.Loading -> {
                        loadingView.showProgressBar()
                    }
                    is StatefulData.NotStarted -> {}
                    is StatefulData.Success -> {
                        loadingView.hideAll()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.refreshAccounts()
    }

    private class AccountAdapter(
        private val context: Context,
        private val signOut: (AccountView) -> Unit,
        private val onAccountClick: (AccountView) -> Unit,
        private val onAddAccountClick: () -> Unit,
    ) : RecyclerView.Adapter<ViewHolder>() {

        private sealed interface Item {
            data class CurrentAccountItem(
                val accountView: AccountView
            ) : Item
            data class AccountItem(
                val accountView: AccountView
            ) : Item
            data class AddAccountItem(
                val hasAccounts: Boolean
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(areItemsTheSame = { old, new ->
            old::class == new::class && when (old) {
                is Item.CurrentAccountItem ->
                    old.accountView.account.id == (new as Item.CurrentAccountItem).accountView.account.id
                is Item.AccountItem ->
                    old.accountView.account.id == (new as Item.AccountItem).accountView.account.id
                is Item.AddAccountItem -> true
            }
        }).apply {
            addItemType(
                clazz = Item.CurrentAccountItem::class,
                inflateFn = CurrentAccountItemBinding::inflate
            ) { item, b, _ ->
                b.image.load(item.accountView.profileImage)
                b.name.text = item.accountView.account.name
                b.instance.text = item.accountView.account.instance

                b.signOut.setOnClickListener {
                    signOut(item.accountView)
                }
            }
            addItemType(Item.AccountItem::class, AccountItemBinding::inflate) { item, b, _ ->
                b.image.load(item.accountView.profileImage)
                b.name.text = item.accountView.account.name
                b.instance.text = item.accountView.account.instance

                b.root.setOnClickListener {
                    onAccountClick(item.accountView)
                }
            }
            addItemType(Item.AddAccountItem::class, AddAccountItemBinding::inflate) { item, b, _ ->
                b.title.text = if (item.hasAccounts) {
                    context.getString(R.string.add_another_account)
                } else {
                    context.getString(R.string.add_account)
                }
                b.root.setOnClickListener {
                    onAddAccountClick()
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

        fun setAccounts(accounts: List<AccountView>) {
            val currentAccount = accounts.firstOrNull { it.account.current }
            val newItems = mutableListOf<Item>()

            if (currentAccount != null) {
                newItems.add(Item.CurrentAccountItem(currentAccount))
            }

            accounts.mapNotNullTo(newItems) {
                if (it.account.id != currentAccount?.account?.id) {
                    Item.AccountItem(it)
                } else {
                    null
                }
            }

            newItems.add(Item.AddAccountItem(hasAccounts = accounts.isNotEmpty()))

            adapterHelper.setItems(newItems, this)
        }
    }
}