package com.idunnololz.summit.settings.commentQuickActions

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.OldAlertDialogFragment
import com.idunnololz.summit.databinding.FragmentCustomQuickActionsBinding
import com.idunnololz.summit.databinding.InactiveActionsTitleBinding
import com.idunnololz.summit.databinding.QuickActionBinding
import com.idunnololz.summit.databinding.QuickActionsTitleBinding
import com.idunnololz.summit.preferences.CommentQuickActionId
import com.idunnololz.summit.preferences.CommentQuickActionIds
import com.idunnololz.summit.preferences.CommentQuickActionsSettings
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class CustomQuickActionsFragment :
    BaseFragment<FragmentCustomQuickActionsBinding>(),
    OldAlertDialogFragment.AlertDialogFragmentListener {

    private val viewModel: CustomQuickActionsViewModel by viewModels()

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        setBinding(FragmentCustomQuickActionsBinding.inflate(inflater, container, false))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        requireMainActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.contentContainer)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setSupportActionBar(binding.toolbar)

            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.customize_comment_quick_actions)

            addMenuProvider2(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.menu_custom_quick_actions, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                        when (menuItem.itemId) {
                            R.id.reset_settings -> {
                                OldAlertDialogFragment.Builder()
                                    .setMessage(R.string.warn_reset_settings)
                                    .setPositiveButton(R.string.reset_settings)
                                    .setNegativeButton(R.string.cancel)
                                    .createAndShow(childFragmentManager, "reset_settings")
                                true
                            }

                            else -> false
                        }
                },
            )
        }

        with(binding) {
            val adapter = QuickActionsAdapter(
                context = context,
                onQuickActionsChanged = {
                    viewModel.updateCommentQuickActions(it)
                },
            ).apply {
                setItems(
                    (
                        viewModel.preferences.commentQuickActions
                            ?: CommentQuickActionsSettings()
                        )
                        .actions,
                )
            }
            recyclerView.setup(animationsHelper)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter

            adapter.setRecyclerView(recyclerView)

            viewModel.settingsChangedLiveData.observe(viewLifecycleOwner) {
                adapter.setItems(
                    (
                        viewModel.preferences.commentQuickActions
                            ?: CommentQuickActionsSettings()
                        )
                        .actions,
                )
            }
        }
    }

    private class QuickActionsAdapter(
        private val context: Context,
        private val onQuickActionsChanged: (List<CommentQuickActionId>) -> Unit,
    ) : Adapter<ViewHolder>() {

        private sealed class Item {
            data object QuickActionsTitle : Item()
            data object InactiveActionsTitle : Item()
            data class QuickAction(
                val actionId: Int,
                @DrawableRes val icon: Int,
                val name: String,
            ) : Item()
        }

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    Item.InactiveActionsTitle -> true
                    Item.QuickActionsTitle -> true
                    is Item.QuickAction ->
                        old.actionId == (new as Item.QuickAction).actionId
                }
            },
        ).apply {
            addItemType(
                clazz = Item.QuickActionsTitle::class,
                inflateFn = QuickActionsTitleBinding::inflate,
            ) { item, b, h ->
            }
            addItemType(
                clazz = Item.InactiveActionsTitle::class,
                inflateFn = InactiveActionsTitleBinding::inflate,
            ) { item, b, h ->
            }
        }
        private val ith: ItemTouchHelper

        private var quickActions: List<Int> = listOf()
        private var unusedActions: List<Int> = listOf()

        private var items: List<Item> = listOf()

        init {
            val callback = object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0,
            ) {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: ViewHolder,
                    target: ViewHolder,
                ): Boolean {
                    swap(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                    return true
                }

                override fun canDropOver(
                    recyclerView: RecyclerView,
                    current: ViewHolder,
                    target: ViewHolder,
                ): Boolean {
                    val item = adapterHelper.items[target.bindingAdapterPosition]

                    return item is Item.QuickAction || item is Item.InactiveActionsTitle
                }

                override fun isLongPressDragEnabled(): Boolean {
                    return true
                }

                override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}

                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: ViewHolder,
                ): Int {
                    val item = adapterHelper.items[viewHolder.bindingAdapterPosition]
                    return if (item is Item.QuickAction) {
                        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                        makeMovementFlags(dragFlags, 0)
                    } else {
                        0
                    }
                }
            }

            ith = ItemTouchHelper(callback)

            adapterHelper.addItemType(
                clazz = Item.QuickAction::class,
                inflateFn = QuickActionBinding::inflate,
            ) { item, b, h ->
                b.icon.setImageResource(item.icon)
                b.text.text = item.name

                b.handle.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        ith.startDrag(h)
                    }
                    false
                }
            }
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun getItemCount(): Int = adapterHelper.itemCount

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        private fun swap(fromPosition: Int, toPosition: Int) {
            val mutableItems = items.toMutableList()

            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(mutableItems, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(mutableItems, i, i - 1)
                }
            }
            items = mutableItems
            adapterHelper.setItems(items, this)

            updateQuickActions()
        }

        fun setRecyclerView(v: RecyclerView) {
            ith.attachToRecyclerView(v)
        }

        fun setItems(quickActions: List<Int>) {
            val allActions = CommentQuickActionIds.AllActions.filter {
                it != CommentQuickActionIds.More
            }

            unusedActions = allActions.filter { !quickActions.contains(it) }
            this.quickActions = quickActions

            refreshItems()
        }

        fun refreshItems() {
            fun toQuickAction(actionId: Int): Item.QuickAction? {
                val icon: Int
                val name: String
                when (actionId) {
                    CommentQuickActionIds.Voting -> {
                        icon = R.drawable.baseline_swap_vert_24
                        name = context.getString(R.string.vote_actions)
                    }
                    CommentQuickActionIds.Reply -> {
                        icon = R.drawable.baseline_reply_24
                        name = context.getString(R.string.reply)
                    }
                    CommentQuickActionIds.Save -> {
                        icon = R.drawable.baseline_bookmark_24
                        name = context.getString(R.string.save)
                    }
                    CommentQuickActionIds.Share -> {
                        icon = R.drawable.baseline_share_24
                        name = context.getString(R.string.share)
                    }
                    CommentQuickActionIds.TakeScreenshot -> {
                        icon = R.drawable.baseline_screenshot_24
                        name = context.getString(R.string.take_screenshot)
                    }
                    CommentQuickActionIds.ShareSource -> {
                        icon = R.drawable.ic_fediverse_24
                        name = context.getString(R.string.share_source_link)
                    }
                    CommentQuickActionIds.OpenComment -> {
                        icon = R.drawable.baseline_open_in_new_24
                        name = context.getString(R.string.open_comment)
                    }
                    CommentQuickActionIds.ViewSource -> {
                        icon = R.drawable.baseline_code_24
                        name = context.getString(R.string.view_raw)
                    }
                    CommentQuickActionIds.DetailedView -> {
                        icon = R.drawable.baseline_open_in_full_24
                        name = context.getString(R.string.detailed_view)
                    }
                    else -> return null
                }

                return Item.QuickAction(
                    actionId,
                    icon,
                    name,
                )
            }

            val items = mutableListOf<Item>()
            items.add(Item.QuickActionsTitle)
            quickActions.mapNotNullTo(items) {
                toQuickAction(it)
            }

            items.add(Item.InactiveActionsTitle)
            unusedActions.mapNotNullTo(items) {
                toQuickAction(it)
            }

            this.items = items

            adapterHelper.setItems(items, this)
        }

        fun getQuickActions(): List<Int> {
            val quickActions = mutableListOf<Int>()
            for (item in items) {
                when (item) {
                    Item.InactiveActionsTitle ->
                        break
                    is Item.QuickAction ->
                        quickActions.add(item.actionId)
                    Item.QuickActionsTitle ->
                        continue
                }
            }

            return quickActions
        }

        fun updateQuickActions() {
            val newQuickActions = getQuickActions()
            if (newQuickActions == quickActions) {
                return
            }

            quickActions = newQuickActions
            onQuickActionsChanged(newQuickActions)
        }
    }

    override fun onPositiveClick(dialog: OldAlertDialogFragment, tag: String?) {
        viewModel.resetSettings()
    }

    override fun onNegativeClick(dialog: OldAlertDialogFragment, tag: String?) {
    }
}
