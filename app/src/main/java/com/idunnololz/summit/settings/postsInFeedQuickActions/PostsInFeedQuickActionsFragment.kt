package com.idunnololz.summit.settings.postsInFeedQuickActions

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.idunnololz.summit.R
import com.idunnololz.summit.alert.newAlertDialogLauncher
import com.idunnololz.summit.databinding.FragmentCustomQuickActionsBinding
import com.idunnololz.summit.databinding.InactiveActionsTitleBinding
import com.idunnololz.summit.databinding.QuickActionBinding
import com.idunnololz.summit.databinding.QuickActionsTitleBinding
import com.idunnololz.summit.databinding.SettingItemOnOffMasterBinding
import com.idunnololz.summit.preferences.PostQuickActionId
import com.idunnololz.summit.preferences.PostQuickActionIds
import com.idunnololz.summit.preferences.PostsInFeedQuickActionsSettings
import com.idunnololz.summit.settings.SettingsFragment
import com.idunnololz.summit.util.AnimationsHelper
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.ext.setup
import com.idunnololz.summit.util.insetViewExceptBottomAutomaticallyByMargins
import com.idunnololz.summit.util.insetViewExceptTopAutomaticallyByPadding
import com.idunnololz.summit.util.recyclerView.AdapterHelper
import com.idunnololz.summit.util.setupForFragment
import com.idunnololz.summit.util.setupToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class PostsInFeedQuickActionsFragment :
    BaseFragment<FragmentCustomQuickActionsBinding>() {

    private val viewModel: PostsInFeedQuickActionsViewModel by viewModels()

    @Inject
    lateinit var animationsHelper: AnimationsHelper

    private val resetSettingsDialogLauncher = newAlertDialogLauncher("reset_settings") {
        if (it.isOk) {
            viewModel.resetSettings()
        }
    }

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

        requireSummitActivity().apply {
            setupForFragment<SettingsFragment>()
            insetViewExceptTopAutomaticallyByPadding(viewLifecycleOwner, binding.contentContainer)
            insetViewExceptBottomAutomaticallyByMargins(viewLifecycleOwner, binding.toolbar)

            setupToolbar(binding.toolbar, getString(R.string.customize_post_in_post_feed_quick_actions))
            binding.toolbar.addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(R.menu.menu_custom_quick_actions, menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                        when (menuItem.itemId) {
                            R.id.reset_settings -> {
                                resetSettingsDialogLauncher.launchDialog {
                                    this.messageResId = R.string.warn_reset_settings
                                    this.positionButtonResId = R.string.reset_settings
                                    this.negativeButtonResId = R.string.cancel
                                }
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
                    viewModel.updatePostsInFeedQuickActions(it)
                },
                onEnableChanged = {
                    viewModel.setEnabled(it)
                },
            )
            recyclerView.setup(animationsHelper)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter

            adapter.setRecyclerView(recyclerView)

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.postsInFeedQuickActions.collect {
                    adapter.setData(it)
                }
            }
        }
    }

    private class QuickActionsAdapter(
        private val context: Context,
        private val onQuickActionsChanged: (List<PostQuickActionId>) -> Unit,
        private val onEnableChanged: (Boolean) -> Unit,
    ) : Adapter<ViewHolder>() {

        private sealed interface Item {
            data class EnableItem(val isEnabled: Boolean) : Item
            data object QuickActionsTitle : Item
            data object InactiveActionsTitle : Item
            data class QuickAction(
                val actionId: Int,
                @DrawableRes val icon: Int,
                val name: String,
            ) : Item
        }

        private val adapterHelper = AdapterHelper<Item>(
            { old, new ->
                old::class == new::class && when (old) {
                    Item.InactiveActionsTitle -> true
                    Item.QuickActionsTitle -> true
                    is Item.QuickAction ->
                        old.actionId == (new as Item.QuickAction).actionId
                    is Item.EnableItem -> true
                }
            },
        ).apply {
            addItemType(
                clazz = Item.EnableItem::class,
                inflateFn = SettingItemOnOffMasterBinding::inflate,
            ) { item, b, h ->
                b.title.text = context.getString(R.string.custom_post_in_feed_quick_actions)
                b.desc.visibility = View.GONE

                b.switchView.setOnCheckedChangeListener(null)
                b.switchView.isChecked = item.isEnabled
                b.switchView.setOnCheckedChangeListener { buttonView, isChecked ->
                    onEnableChanged(isChecked)
                }
            }
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

        private var data: PostsInFeedQuickActionsSettings? = null

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
            val mutableItems = adapterHelper.items.toMutableList()

            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(mutableItems, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(mutableItems, i, i - 1)
                }
            }
            adapterHelper.setItems(mutableItems, this)

            updateQuickActions()
        }

        fun setRecyclerView(v: RecyclerView) {
            ith.attachToRecyclerView(v)
        }

        fun refreshItems() {
            val data = data ?: return

            fun toQuickAction(actionId: Int): Item.QuickAction? {
                val icon: Int
                val name: String
                when (actionId) {
                    PostQuickActionIds.Voting -> {
                        icon = R.drawable.baseline_swap_vert_24
                        name = context.getString(R.string.vote_actions)
                    }
                    PostQuickActionIds.Reply -> {
                        icon = R.drawable.baseline_reply_24
                        name = context.getString(R.string.reply)
                    }
                    PostQuickActionIds.Save -> {
                        icon = R.drawable.baseline_bookmark_24
                        name = context.getString(R.string.save)
                    }
                    PostQuickActionIds.Share -> {
                        icon = R.drawable.baseline_share_24
                        name = context.getString(R.string.share)
                    }
                    PostQuickActionIds.TakeScreenshot -> {
                        icon = R.drawable.baseline_screenshot_24
                        name = context.getString(R.string.take_screenshot)
                    }
                    PostQuickActionIds.CrossPost -> {
                        icon = R.drawable.baseline_content_copy_24
                        name = context.getString(R.string.cross_post)
                    }
                    PostQuickActionIds.ShareSourceLink -> {
                        icon = R.drawable.ic_fediverse_24
                        name = context.getString(R.string.share_source_link)
                    }
                    PostQuickActionIds.CommunityInfo -> {
                        icon = R.drawable.ic_community_24
                        name = context.getString(R.string.community_info)
                    }
                    PostQuickActionIds.ViewSource -> {
                        icon = R.drawable.baseline_code_24
                        name = context.getString(R.string.view_raw)
                    }
                    PostQuickActionIds.DetailedView -> {
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
            items.add(Item.EnableItem(data.enabled))
            items.add(Item.QuickActionsTitle)
            quickActions.mapNotNullTo(items) {
                toQuickAction(it)
            }

            items.add(Item.InactiveActionsTitle)
            unusedActions.mapNotNullTo(items) {
                toQuickAction(it)
            }

            adapterHelper.setItems(items, this)
        }

        fun getQuickActions(): List<Int> {
            val quickActions = mutableListOf<Int>()
            for (item in adapterHelper.items) {
                when (item) {
                    Item.InactiveActionsTitle ->
                        break
                    is Item.QuickAction ->
                        quickActions.add(item.actionId)
                    Item.QuickActionsTitle ->
                        continue
                    is Item.EnableItem ->
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

        fun setData(data: PostsInFeedQuickActionsSettings) {
            this.data = data
            this.quickActions = data.actions

            val allActions = PostQuickActionIds.AllActions.filter {
                it != PostQuickActionIds.More
            }

            unusedActions = allActions.filter { !quickActions.contains(it) }
            this.quickActions = quickActions
            refreshItems()
        }
    }
}
