package com.idunnololz.summit.lemmy.inbox.inbox

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.idunnololz.summit.R
import com.idunnololz.summit.account.info.AccountInfoManager
import com.idunnololz.summit.account.info.FullAccount
import com.idunnololz.summit.account.info.isAdmin
import com.idunnololz.summit.account.info.isMod
import com.idunnololz.summit.databinding.InboxPaneBinding
import com.idunnololz.summit.databinding.InboxPaneCategoryItemBinding
import com.idunnololz.summit.databinding.InboxPaneDividerItemBinding
import com.idunnololz.summit.databinding.InboxPaneTitleItemBinding
import com.idunnololz.summit.lemmy.inbox.PageType
import com.idunnololz.summit.util.PrettyPrintUtils
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

class InboxPaneController @AssistedInject constructor(
  private val accountInfoManager: AccountInfoManager,
  @Assisted private val viewModel: InboxViewModel,
  @Assisted private val binding: InboxPaneBinding,
  @Assisted private val viewLifecycleOwner: LifecycleOwner,
  @Assisted private val onCategoryClickListener: (InboxCategory) -> Unit,
) {

  @AssistedFactory
  interface Factory {
    fun create(
      viewModel: InboxViewModel,
      binding: InboxPaneBinding,
      viewLifecycleOwner: LifecycleOwner,
      onCategoryClickListener: (InboxCategory) -> Unit,
    ): InboxPaneController
  }

  init {
    with(binding) {
      val adapter = InboxPaneAdapter(
        onCategoryClickListener,
      )

      inboxPaneRecyclerView.layoutManager = LinearLayoutManager(inboxPaneRecyclerView.context)
      inboxPaneRecyclerView.setHasFixedSize(false)
      inboxPaneRecyclerView.adapter = adapter

      viewLifecycleOwner.lifecycleScope.launch {
        accountInfoManager.unreadCount.collect {
          adapter.unreadCounts = it
        }
      }
      viewLifecycleOwner.lifecycleScope.launch {
        accountInfoManager.currentFullAccount.collect {
          adapter.fullAccount = it
        }
      }
      viewLifecycleOwner.lifecycleScope.launch {
        viewModel.pageTypeFlow.collect {
          adapter.selectedCategory = when (it) {
            PageType.Unread -> InboxCategory.Unread
            PageType.All -> InboxCategory.All
            PageType.Replies -> InboxCategory.Replies
            PageType.Mentions -> InboxCategory.Mentions
            PageType.Messages -> InboxCategory.Messages
            PageType.Reports -> InboxCategory.Reports
            PageType.Conversation -> InboxCategory.Messages
            PageType.Applications -> InboxCategory.Applications
          }
        }
      }
    }
  }

  enum class InboxCategory {
    Unread,
    All,
    Replies,
    Mentions,
    Messages,
    Reports,
    Applications,
  }

  private class InboxPaneAdapter(
    private val onCategoryClickListener: (InboxCategory) -> Unit,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed interface Item {
      object TitleItem : Item
      class CategoryItem(
        val category: InboxCategory,
        val unreadCount: Int,
        val isSelected: Boolean,
      ) : Item
      class DividerItem(val name: String) : Item
    }

    var unreadCounts: AccountInfoManager.UnreadCount? = null
      set(value) {
        field = value

        refreshItems()
      }
    var fullAccount: FullAccount? = null
      set(value) {
        field = value

        refreshItems()
      }
    var selectedCategory: InboxCategory? = null
      set(value) {
        field = value

        refreshItems()
      }

    private val adapterHelper = AdapterHelper<Item>(
      { old, new ->
        old::class == new::class && when (old) {
          is Item.CategoryItem ->
            old.category == (new as Item.CategoryItem).category
          is Item.DividerItem ->
            old.name == (new as Item.DividerItem).name
          Item.TitleItem -> true
        }
      },
    ).apply {
      addItemType(
        Item.CategoryItem::class,
        InboxPaneCategoryItemBinding::inflate,
      ) { item, b, h ->
        when (item.category) {
          InboxCategory.Unread -> {
            b.text.setText(R.string.unread)
            b.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
              R.drawable.baseline_mark_email_unread_24,
              0,
              0,
              0,
            )
          }
          InboxCategory.All -> {
            b.text.setText(R.string.all)
            b.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
              R.drawable.baseline_inbox_24,
              0,
              0,
              0,
            )
          }
          InboxCategory.Replies -> {
            b.text.setText(R.string.replies)
            b.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
              R.drawable.baseline_reply_24,
              0,
              0,
              0,
            )
          }
          InboxCategory.Mentions -> {
            b.text.setText(R.string.mentions)
            b.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
              R.drawable.baseline_at_24,
              0,
              0,
              0,
            )
          }
          InboxCategory.Messages -> {
            b.text.setText(R.string.messages)
            b.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
              R.drawable.outline_email_24,
              0,
              0,
              0,
            )
          }
          InboxCategory.Reports -> {
            b.text.setText(R.string.reports)
            b.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
              R.drawable.outline_shield_24,
              0,
              0,
              0,
            )
          }
          InboxCategory.Applications -> {
            b.text.setText(R.string.registration_applications)
            b.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
              R.drawable.outline_assignment_24,
              0,
              0,
              0,
            )
          }
        }

        if (item.unreadCount == 0) {
          b.unreadCount.text = ""
        } else {
          b.unreadCount.text = PrettyPrintUtils.defaultDecimalFormat.format(item.unreadCount)
        }

        b.root.isSelected = item.isSelected
        b.root.setOnClickListener {
          onCategoryClickListener(item.category)
        }
      }
      addItemType(
        Item.TitleItem::class,
        InboxPaneTitleItemBinding::inflate,
      ) { item, b, h ->
      }
      addItemType(
        Item.DividerItem::class,
        InboxPaneDividerItemBinding::inflate,
      ) { item, b, h ->
      }
    }

    init {
      refreshItems()
    }

    override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
      adapterHelper.onCreateViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
      adapterHelper.onBindViewHolder(holder, position)

    override fun getItemCount(): Int = adapterHelper.itemCount

    private fun refreshItems() {
      val newItems = mutableListOf<Item>()
      val fullAccount = fullAccount
      val unreadCounts = unreadCounts

      fun newCategoryItem(category: InboxCategory, unreadCount: Int?) = Item.CategoryItem(
        category,
        unreadCount ?: 0,
        category == selectedCategory,
      )

      newItems.add(Item.TitleItem)
      newItems.add(newCategoryItem(InboxCategory.Unread, unreadCounts?.totalUnreadCount))
      newItems.add(newCategoryItem(InboxCategory.All, unreadCounts?.totalUnreadCount))
      newItems.add(newCategoryItem(InboxCategory.Replies, unreadCounts?.replies))
      newItems.add(newCategoryItem(InboxCategory.Mentions, unreadCounts?.mentions))
      newItems.add(newCategoryItem(InboxCategory.Messages, unreadCounts?.privateMessages))

      val isMod = fullAccount?.isMod() == true
      val isAdmin = fullAccount?.isAdmin() == true

      if (isMod || isAdmin) {
        newItems.add(Item.DividerItem("mod_admin_divider"))
        if (isMod) {
          newItems.add(
            newCategoryItem(
              InboxCategory.Reports,
              unreadCounts?.totalUnresolvedReportsCount,
            ),
          )
        }
        if (isAdmin) {
          newItems.add(
            newCategoryItem(
              InboxCategory.Applications,
              unreadCounts?.totalUnreadApplicationsCount,
            ),
          )
        }
      }

      adapterHelper.setItems(newItems, this)
    }
  }
}
