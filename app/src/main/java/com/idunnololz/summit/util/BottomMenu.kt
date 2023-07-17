package com.idunnololz.summit.util

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.BottomMenuBinding
import com.idunnololz.summit.databinding.MenuItemBinding
import com.idunnololz.summit.databinding.MenuItemFooterBinding
import com.idunnololz.summit.databinding.MenuItemTitleBinding
import com.idunnololz.summit.main.MainActivity
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class BottomMenu(private val context: Context) {

    companion object {

        @Suppress("unused")
        private val TAG = BottomMenu::class.java.simpleName
    }

    private val menuItems = ArrayList<MenuItem>()
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var adapter: BottomMenuAdapter? = null
    private var checked: Int? = null
    private var onMenuItemClickListener: ((menuItem: MenuItem) -> Unit)? = null
    private var title: String? = null

    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    private var parent: ViewGroup? = null

    private var topInset = MutableLiveData(0)
    private var bottomInset = MutableLiveData(0)

    private val checkedTextColor = context.getColorFromAttribute(
        com.google.android.material.R.attr.colorPrimary,
    )
    private val defaultTextColor = ContextCompat.getColor(context, R.color.colorTextTitle)

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            close()
        }
    }

    var onClose: (() -> Unit)? = null

    fun setTitle(@StringRes title: Int) {
        this.title = context.getString(title)
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun addItem(@IdRes id: Int, @StringRes title: Int) {
        menuItems.add(MenuItem(id, context.getString(title)))
    }

    fun addItem(@IdRes id: Int, title: String) {
        menuItems.add(MenuItem(id, title))
    }

    fun addItem(@IdRes id: Int, @StringRes title: Int, @DrawableRes checkIcon: Int) {
        menuItems.add(MenuItem(id, context.getString(title), checkIcon = checkIcon))
    }

    fun addItem(@IdRes id: Int, title: String, @DrawableRes icon: Int) {
        menuItems.add(MenuItem(id, title, checkIcon = icon))
    }

    fun addItemWithIcon(@IdRes id: Int, @StringRes title: Int, @DrawableRes icon: Int) {
        addItemWithIcon(id, context.getString(title), icon)
    }

    fun addItemWithIcon(@IdRes id: Int, title: String, @DrawableRes icon: Int) {
        menuItems.add(MenuItem(id, title, icon = MenuIcon.ResourceIcon(icon)))
    }

    fun itemsCount() = menuItems.size

    fun setChecked(@IdRes checked: Int) {
        this.checked = checked
    }

    fun setOnMenuItemClickListener(onMenuItemClickListener: (menuItem: MenuItem) -> Unit) {
        this.onMenuItemClickListener = onMenuItemClickListener
    }

    fun show(
        mainActivity: MainActivity,
        viewGroup: ViewGroup,
        expandFully: Boolean,
        handleBackPress: Boolean = true,
    ) {
        parent = viewGroup
        adapter = BottomMenuAdapter().apply {
            refreshItems()
        }

        val binding = BottomMenuBinding.inflate(inflater, viewGroup, false)

        val rootView = binding.root
        val bottomSheet = binding.bottomSheet
        val recyclerView = binding.recyclerView

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val overlay = View(context)
        viewGroup.addView(overlay)
        val layoutParams = overlay.layoutParams
        layoutParams.height = RecyclerView.LayoutParams.MATCH_PARENT
        layoutParams.width = RecyclerView.LayoutParams.MATCH_PARENT
        ViewCompat.setElevation(overlay, Utils.convertDpToPixel(16f))
        overlay.layoutParams = layoutParams
        overlay.setBackgroundColor(ContextCompat.getColor(context, R.color.black50))
        overlay.setBackgroundColor(ContextCompat.getColor(context, R.color.black50))
        overlay.alpha = 0f
        overlay.setOnClickListener {
            bottomSheetBehavior?.setState(
                BottomSheetBehavior.STATE_HIDDEN,
            )
        }

        ViewCompat.setElevation(rootView, Utils.convertDpToPixel(17f))
        viewGroup.addView(rootView)

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN

            if (expandFully) {
                skipCollapsed = true
            }
                
        }.also {
            bottomSheetBehavior = it
        }

        bottomInset.observeForever {
            recyclerView.updatePadding(bottom = it)
        }
        topInset.observeForever {
            bottomSheet.updateLayoutParams<MarginLayoutParams> {
                topMargin = it
            }
        }

        rootView.postDelayed({
            if (expandFully) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }

            bottomSheetBehavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet1: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            parent = null
                            onBackPressedCallback.remove()
                            viewGroup.removeView(rootView)
                            viewGroup.removeView(overlay)

                            onClose?.invoke()
                        }
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
        }, 100)

        if (handleBackPress) {
            mainActivity.onBackPressedDispatcher.addCallback(mainActivity, onBackPressedCallback)
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
            val title: String?
        ): Item

        data class MenuItemItem(
            val menuItem: MenuItem
        ): Item

        object FooterItem : Item
    }

    private inner class BottomMenuAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val adapterHelper = AdapterHelper<Item> (
            areItemsTheSame = { old, new ->
                old::class == new::class && when (old) {
                    Item.FooterItem -> true
                    is Item.MenuItemItem ->
                        old.menuItem.id == (new as Item.MenuItemItem).menuItem.id
                    is Item.TitleItem -> true
                }
            }
        ).apply {
            addItemType(Item.TitleItem::class, MenuItemTitleBinding::inflate) { item, b, _ ->
                b.title.text = item.title
                if (item.title == null) {
                    b.title.visibility = View.GONE
                }
            }
            addItemType(Item.MenuItemItem::class, MenuItemBinding::inflate) { item, b, _ ->
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
                    b.title.setTypeface(null, Typeface.BOLD)
                } else {
                    b.title.setTextColor(defaultTextColor)
                    b.title.setTypeface(null, Typeface.NORMAL)
                }

                val icon = menuItem.icon
                if (icon != null) {
                    b.icon.visibility = View.VISIBLE

                    when (icon) {
                        is MenuIcon.ResourceIcon ->
                            b.icon.setImageResource(icon.customIcon)
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

        fun refreshItems() {
            val newItems = mutableListOf<Item>()

            newItems.add(Item.TitleItem(title))
            menuItems.forEach {
                newItems.add(Item.MenuItemItem(it))
            }
            newItems.add(Item.FooterItem)

            adapterHelper.setItems(newItems, this)
        }

        override fun getItemViewType(position: Int): Int =
            adapterHelper.getItemViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            adapterHelper.onCreateViewHolder(parent, viewType)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            adapterHelper.onBindViewHolder(holder, position)

        override fun getItemCount(): Int = adapterHelper.itemCount
    }

    class MenuItem(
        @IdRes val id: Int,
        val title: String,
        val icon: MenuIcon? = null,
        @DrawableRes val checkIcon: Int = 0,
    )

    sealed interface MenuIcon {
        data class ResourceIcon(
            @DrawableRes val customIcon: Int = 0,
        ) : MenuIcon
    }
}
