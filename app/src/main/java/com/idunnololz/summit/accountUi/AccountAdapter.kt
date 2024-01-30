package com.idunnololz.summit.accountUi

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.idunnololz.summit.R
import com.idunnololz.summit.account.AccountView
import com.idunnololz.summit.account.GuestAccountManager
import com.idunnololz.summit.account.GuestOrUserAccount
import com.idunnololz.summit.databinding.AccountItemBinding
import com.idunnololz.summit.databinding.AddAccountItemBinding
import com.idunnololz.summit.databinding.CurrentAccountItemBinding
import com.idunnololz.summit.databinding.GuestAccountItemBinding
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class AccountAdapter(
    private val context: Context,
    private val isSimple: Boolean,
    private val showGuestAccount: Boolean,
    private val guestAccountManager: GuestAccountManager,
    private val signOut: (AccountView) -> Unit,
    private val onAccountClick: (GuestOrUserAccount) -> Unit,
    private val onAddAccountClick: () -> Unit,
    private val onSettingClick: () -> Unit,
    private val onPersonClick: (AccountView) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Item {
        data class CurrentAccountItem(
            val accountView: AccountView,
        ) : Item
        data class AccountItem(
            val accountView: AccountView,
        ) : Item
        data class GuestAccountItem(
            val isSelected: Boolean,
        ) : Item
        data class AddAccountItem(
            val hasAccounts: Boolean,
        ) : Item
    }

    private val adapterHelper = AdapterHelper<Item>(areItemsTheSame = { old, new ->
        old::class == new::class && when (old) {
            is Item.CurrentAccountItem ->
                old.accountView.account.id == (new as Item.CurrentAccountItem).accountView.account.id
            is Item.AccountItem ->
                old.accountView.account.id == (new as Item.AccountItem).accountView.account.id
            is Item.AddAccountItem -> true
            is Item.GuestAccountItem -> true
        }
    },).apply {
        addItemType(
            clazz = Item.CurrentAccountItem::class,
            inflateFn = CurrentAccountItemBinding::inflate,
        ) { item, b, _ ->
            b.image.load(item.accountView.profileImage)
            b.name.text = item.accountView.account.name
            b.instance.text = item.accountView.account.instance

            if (isSimple) {
                b.signOut.visibility = View.GONE
                b.settings.setImageResource(R.drawable.baseline_check_24)

                b.settings.setOnClickListener {
                    onAccountClick(item.accountView.account)
                }
                b.signOut.setOnClickListener {}
                b.root.setOnClickListener {
                    onAccountClick(item.accountView.account)
                }
            } else {
                b.signOut.visibility = View.VISIBLE
                b.settings.setImageResource(R.drawable.baseline_settings_24)

                b.settings.setOnClickListener {
                    onSettingClick()
                }
                b.signOut.setOnClickListener {
                    signOut(item.accountView)
                }
                b.root.setOnClickListener {
                    onPersonClick(item.accountView)
                }
            }
        }
        addItemType(Item.AccountItem::class, AccountItemBinding::inflate) { item, b, _ ->
            b.image.load(item.accountView.profileImage)
            b.name.text = item.accountView.account.name
            b.instance.text = item.accountView.account.instance

            b.root.setOnClickListener {
                onAccountClick(item.accountView.account)
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
        addItemType(Item.GuestAccountItem::class, GuestAccountItemBinding::inflate) { item, b, h ->
            if (item.isSelected) {
                b.selected.visibility = View.VISIBLE
            } else {
                b.selected.visibility = View.GONE
            }

            b.root.setOnClickListener {
                onAccountClick(guestAccountManager.getGuestAccount())
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        adapterHelper.onCreateViewHolder(parent, viewType)

    override fun getItemCount(): Int = adapterHelper.itemCount

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        adapterHelper.onBindViewHolder(holder, position)

    fun setAccounts(accounts: List<AccountView>, cb: () -> Unit = {}) {
        val currentAccount = accounts.firstOrNull { it.account.current }
        val newItems = mutableListOf<Item>()
        val showGuestAccount = accounts.isNotEmpty() && showGuestAccount

        if (currentAccount != null) {
            newItems.add(Item.CurrentAccountItem(currentAccount))
        }

        if (currentAccount == null && showGuestAccount) {
            newItems.add(Item.GuestAccountItem(isSelected = true))
        }

        accounts.mapNotNullTo(newItems) {
            if (it.account.id != currentAccount?.account?.id) {
                Item.AccountItem(it)
            } else {
                null
            }
        }

        if (currentAccount != null && showGuestAccount) {
            newItems.add(Item.GuestAccountItem(isSelected = false))
        }

        if (!isSimple) {
            newItems.add(Item.AddAccountItem(hasAccounts = accounts.isNotEmpty()))
        }

        adapterHelper.setItems(newItems, this, cb)
    }
}
