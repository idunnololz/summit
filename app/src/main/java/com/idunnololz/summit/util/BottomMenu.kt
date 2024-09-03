package com.idunnololz.summit.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.BottomMenuBinding
import com.idunnololz.summit.databinding.MenuItemBinding
import com.idunnololz.summit.databinding.MenuItemDividerBinding
import com.idunnololz.summit.databinding.MenuItemFooterBinding
import com.idunnololz.summit.databinding.MenuItemTitleBinding
import com.idunnololz.summit.main.ActivityInsets
import com.idunnololz.summit.preferences.Preferences
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class BottomMenu(
    private val context: Context,
) {

    companion object {

        @Suppress("unused")
        private val TAG = BottomMenu::class.java.simpleName
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val adapter: BottomMenuAdapter = BottomMenuAdapter(context)
    private var checked: Int? = null
    private var onMenuItemClickListener: ((menuItem: MenuItem.ActionItem) -> Unit)? = null
    private var title: String? = null

    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    private var parent: ViewGroup? = null

    private var topInset = MutableLiveData(0)
    private var bottomInset = MutableLiveData(0)

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            close()
        }
    }

    private val insetsObserver = Observer<ActivityInsets> {
        setInsets(it.topInset, it.bottomInset)
    }

    var onClose: (() -> Unit)? = null

    fun setTitle(@StringRes title: Int) {
        this.title = context.getString(title)
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun addItem(@IdRes id: Int, @StringRes title: Int) {
        adapter.menuItems.add(MenuItem.ActionItem(id, context.getString(title)))
    }

    fun addItem(@IdRes id: Int, title: String) {
        adapter.menuItems.add(MenuItem.ActionItem(id, title))
    }

    fun addItem(@IdRes id: Int, @StringRes title: Int, @DrawableRes checkIcon: Int) {
        adapter.menuItems.add(
            MenuItem.ActionItem(id, context.getString(title), checkIcon = checkIcon),
        )
    }

    fun addItem(@IdRes id: Int, title: String, @DrawableRes icon: Int) {
        adapter.menuItems.add(MenuItem.ActionItem(id, title, checkIcon = icon))
    }

    fun addItemWithIcon(@IdRes id: Int, @StringRes title: Int, @DrawableRes icon: Int) {
        addItemWithIcon(id, context.getString(title), icon)
    }

    fun addItemWithIcon(@IdRes id: Int, title: String, @DrawableRes icon: Int) {
        adapter.menuItems.add(MenuItem.ActionItem(id, title, icon = MenuIcon.ResourceIcon(icon)))
    }

    fun addItemWithIcon(@IdRes id: Int, title: String, drawable: Drawable) {
        adapter.menuItems.add(
            MenuItem.ActionItem(id, title, icon = MenuIcon.DrawableIcon(drawable)),
        )
    }

    fun addDangerousItemWithIcon(@IdRes id: Int, @StringRes title: Int, @DrawableRes icon: Int) {
        adapter.menuItems.add(
            MenuItem.ActionItem(
                id,
                context.getString(title),
                icon = MenuIcon.ResourceIcon(icon),
                modifier = ModifierIds.DANGER,
            ),
        )
    }

    fun addDivider() {
        adapter.addDividerIfNeeded()
    }

    fun itemsCount() = adapter.menuItems.size

    fun setChecked(@IdRes checked: Int) {
        this.checked = checked
    }

    fun setOnMenuItemClickListener(
        onMenuItemClickListener: (menuItem: MenuItem.ActionItem) -> Unit,
    ) {
        this.onMenuItemClickListener = onMenuItemClickListener
    }

    fun show(
        bottomMenuContainer: BottomMenuContainer,
        bottomSheetContainer: ViewGroup,
        expandFully: Boolean,
        handleBackPress: Boolean = true,
        handleInsets: Boolean = true,
    ) {
        if (handleInsets) {
            bottomMenuContainer.insets.observeForever(insetsObserver)
        }

        parent = bottomSheetContainer

        adapter.title = title
        adapter.checked = checked
        adapter.onMenuItemClickListener = onMenuItemClickListener

        adapter.refreshItems()

        val binding = BottomMenuBinding.inflate(inflater, bottomSheetContainer, false)

        val rootView = binding.root
        val bottomSheet = binding.bottomSheet
        val recyclerView = binding.recyclerView

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val overlay = binding.overlay
        overlay.setOnClickListener {
            bottomSheetBehavior?.setState(
                BottomSheetBehavior.STATE_HIDDEN,
            )
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            isGestureInsetBottomIgnored = true

            if (expandFully) {
                skipCollapsed = true
            }
        }.also {
            bottomSheetBehavior = it
        }
        adapter.bottomSheetBehavior = bottomSheetBehavior

        bottomInset.observeForever {
            recyclerView.updatePadding(bottom = it)
        }
        topInset.observeForever { topInset ->
            binding.bottomSheetContainerInner.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topInset
            }
        }

        bottomSheetContainer.addView(rootView)

        rootView.postDelayed(
            {
                if (bottomSheetContainer.width > bottomSheetBehavior.maxWidth) {
                    bottomSheetBehavior.skipCollapsed = true
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else if (expandFully) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }

                bottomSheetBehavior.addBottomSheetCallback(
                    object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet1: View, newState: Int) {
                            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                                parent = null
                                onBackPressedCallback.remove()
                                bottomSheetContainer.removeView(rootView)
                                bottomMenuContainer.insets.removeObserver(insetsObserver)

                                onClose?.invoke()
                            }

                            Log.d(TAG, "bottom sheet state: $newState")
                        }

                        override fun onSlide(bottomSheet1: View, slideOffset: Float) {
                            if (java.lang.Float.isNaN(slideOffset)) {
                                overlay.alpha = 1f
                            } else {
                                overlay.alpha = 1 + slideOffset
                            }
                        }
                    },
                )
            },
            100,
        )

        if (handleBackPress) {
            bottomMenuContainer.onBackPressedDispatcher.addCallback(
                bottomMenuContainer,
                onBackPressedCallback,
            )
        }
    }

    fun close(): Boolean {
        if (parent != null) {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_HIDDEN
            return true
        }
        return false
    }

    fun setInsets(topInset: Int, bottomInset: Int) {
        this.topInset.value = topInset
        this.bottomInset.value = bottomInset
    }

    private sealed interface Item {
        data class TitleItem(
            val title: String?,
        ) : Item

        data class MenuItemItem(
            val menuItem: MenuItem.ActionItem,
        ) : Item

        data object DividerItem : Item

        data object FooterItem : Item
    }

    class BottomMenuAdapter(
        private val context: Context,
        var title: String? = null,
        var checked: Int? = null,
        val menuItems: MutableList<MenuItem> = ArrayList<MenuItem>(),
        var onMenuItemClickListener: ((menuItem: MenuItem.ActionItem) -> Unit)? = null,
        var bottomSheetBehavior: BottomSheetBehavior<*>? = null,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val checkedTextColor = context.getColorFromAttribute(
            com.google.android.material.R.attr.colorPrimary,
        )
        private val dangerTextColor = context.getColorFromAttribute(
            com.google.android.material.R.attr.colorError,
        )
        private val defaultTextColor = ContextCompat.getColor(context, R.color.colorTextTitle)

        private val adapterHelper = AdapterHelper<Item>(
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    Item.FooterItem -> true
                    is Item.MenuItemItem ->
                        old.menuItem.id == (new as Item.MenuItemItem).menuItem.id
                    is Item.TitleItem -> true
                    Item.DividerItem -> true
                }
            },
        ).apply {
            addItemType(Item.TitleItem::class, MenuItemTitleBinding::inflate) { item, b, _ ->
                b.title.text = item.title
                if (item.title == null) {
                    b.title.visibility = View.GONE
                }
            }
            addItemType(Item.DividerItem::class, MenuItemDividerBinding::inflate) { item, b, _ ->
            }
            addItemType(
                clazz = Item.MenuItemItem::class,
                inflateFn = MenuItemBinding::inflate,
                onViewCreated = {
                    it.icon.setTag(R.id.icon_tint, ImageViewCompat.getImageTintList(it.icon))
                },
            ) { item, b, _ ->
                val menuItem = item.menuItem
                b.title.text = menuItem.title

                when {
                    menuItem.checkIcon != 0 -> {
                        b.checkbox.setColorFilter(
                            context.getColorFromAttribute(
                                com.google.android.material.R.attr.colorControlNormal,
                            ),
                        )
                        b.checkbox.setImageResource(menuItem.checkIcon)
                        b.checkbox.visibility = View.VISIBLE
                    }
                    menuItem.id == checked -> {
                        b.checkbox.setColorFilter(
                            context.getColorFromAttribute(
                                com.google.android.material.R.attr.colorPrimary,
                            ),
                        )
                        b.checkbox.setImageResource(R.drawable.baseline_done_24)
                        b.checkbox.visibility = View.VISIBLE
                    }
                    else -> b.checkbox.visibility = View.GONE
                }

                if (menuItem.id == checked) {
                    b.title.setTextColor(checkedTextColor)
                    b.title.setTypeface(b.title.typeface, Typeface.BOLD)
                } else if (menuItem.modifier == ModifierIds.DANGER) {
                    b.title.setTextColor(dangerTextColor)
                    b.title.setTypeface(b.title.typeface, Typeface.NORMAL)
                } else {
                    b.title.setTextColor(defaultTextColor)
                    b.title.setTypeface(b.title.typeface, Typeface.NORMAL)
                }

                if (menuItem.modifier == ModifierIds.DANGER) {
                    ImageViewCompat.setImageTintList(
                        b.icon,
                        ColorStateList.valueOf(dangerTextColor),
                    )
                } else {
                    ImageViewCompat.setImageTintList(
                        b.icon,
                        b.icon.getTag(R.id.icon_tint) as? ColorStateList,
                    )
                }

                val icon = menuItem.icon
                if (icon != null) {
                    b.icon.visibility = View.VISIBLE

                    when (icon) {
                        is MenuIcon.ResourceIcon ->
                            b.icon.setImageResource(icon.customIcon)
                        is MenuIcon.DrawableIcon ->
                            b.icon.setImageDrawable(icon.customIcon)
                    }
                } else {
                    b.icon.visibility = View.GONE
                }

                if (onMenuItemClickListener != null) {
                    b.root.setOnClickListener {
                        onMenuItemClickListener?.invoke(menuItem)
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            }
            addItemType(Item.FooterItem::class, MenuItemFooterBinding::inflate) { _, _, _ -> }
        }

        fun addItem(@IdRes id: Int, @StringRes title: Int) {
            menuItems.add(MenuItem.ActionItem(id, context.getString(title)))
        }

        fun addItem(@IdRes id: Int, title: String) {
            menuItems.add(MenuItem.ActionItem(id, title))
        }

        fun addItem(@IdRes id: Int, @StringRes title: Int, @DrawableRes checkIcon: Int) {
            menuItems.add(MenuItem.ActionItem(id, context.getString(title), checkIcon = checkIcon))
        }

        fun addItem(@IdRes id: Int, title: String, @DrawableRes icon: Int) {
            menuItems.add(MenuItem.ActionItem(id, title, checkIcon = icon))
        }

        fun addItemWithIcon(
            @IdRes id: Int,
            @StringRes title: Int,
            @DrawableRes icon: Int,
            modifier: Int = ModifierIds.NONE,
        ) {
            addItemWithIcon(id, context.getString(title), icon, modifier)
        }

        fun addItemWithIcon(
            @IdRes id: Int,
            title: String,
            @DrawableRes icon: Int,
            modifier: Int = ModifierIds.NONE,
        ) {
            menuItems.add(
                MenuItem.ActionItem(
                    id = id,
                    title = title,
                    icon = MenuIcon.ResourceIcon(icon),
                    modifier = modifier,
                ),
            )
        }

        fun addDividerIfNeeded() {
            if (menuItems.lastOrNull() != MenuItem.DividerItem) {
                menuItems.add(MenuItem.DividerItem)
            }
        }

        fun refreshItems(cb: () -> Unit = {}) {
            val newItems = mutableListOf<Item>()

            newItems.add(Item.TitleItem(title))
            menuItems.forEach {
                when (it) {
                    is MenuItem.ActionItem ->
                        newItems.add(Item.MenuItemItem(it))

                    MenuItem.DividerItem ->
                        newItems.add(Item.DividerItem)
                }
            }
            newItems.add(Item.FooterItem)

            adapterHelper.setItems(newItems, this, cb)
        }

        override fun getItemViewType(position: Int): Int = adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount
    }

    object ModifierIds {
        const val NONE = 0
        const val DANGER = 1
    }

    sealed interface MenuItem {
        class ActionItem(
            @IdRes val id: Int,
            val title: String,
            val icon: MenuIcon? = null,
            @DrawableRes val checkIcon: Int = 0,
            val modifier: Int = ModifierIds.NONE,
        ) : MenuItem
        data object DividerItem : MenuItem
    }

    sealed interface MenuIcon {
        data class ResourceIcon(
            @DrawableRes val customIcon: Int = 0,
        ) : MenuIcon
        data class DrawableIcon(
            val customIcon: Drawable,
        ) : MenuIcon
    }
}
