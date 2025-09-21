package com.idunnololz.summit.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
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
import com.idunnololz.summit.avatar.AvatarHelper
import com.idunnololz.summit.databinding.BottomMenuBinding
import com.idunnololz.summit.databinding.MenuItemBinding
import com.idunnololz.summit.databinding.MenuItemDividerBinding
import com.idunnololz.summit.databinding.MenuItemFooterBinding
import com.idunnololz.summit.databinding.MenuItemTitleBinding
import com.idunnololz.summit.databinding.MenuItemWithSwitchBinding
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.main.ActivityInsets
import com.idunnololz.summit.util.BottomMenu.Item.*
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.util.recyclerView.AdapterHelper

class BottomMenu(
  private val context: Context,
) {

  companion object {

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

  var bottomSheetView: View? = null

  private val onBackPressedCallback =
    newBottomSheetPredictiveBackBackPressHandler(
      context,
      { bottomSheetView },
    ) {
      close()
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

  fun addRawItem(item: MenuItem.ActionItem) {
    adapter.menuItems.add(item)
  }

  fun addItem(@IdRes id: Int, @StringRes title: Int) {
    adapter.menuItems.add(MenuItem.ActionItem(id, context.getString(title), null))
  }

  fun addItem(@IdRes id: Int, title: CharSequence) {
    adapter.menuItems.add(MenuItem.ActionItem(id, title, null))
  }

  fun addItem(@IdRes id: Int, @StringRes title: Int, @DrawableRes checkIcon: Int) {
    adapter.menuItems.add(
      MenuItem.ActionItem(id, context.getString(title), null, checkIcon = checkIcon),
    )
  }

  fun addItem(@IdRes id: Int, title: String, @DrawableRes icon: Int) {
    adapter.menuItems.add(MenuItem.ActionItem(id, title, null, checkIcon = icon))
  }

  fun addItemWithIcon(@IdRes id: Int, @StringRes title: Int, @DrawableRes icon: Int) {
    addItemWithIcon(id, context.getString(title), icon)
  }

  fun addItemWithIcon(@IdRes id: Int, title: CharSequence, @DrawableRes icon: Int) {
    adapter.menuItems.add(MenuItem.ActionItem(id, title, null, icon = MenuIcon.ResourceIcon(icon)))
  }

  fun addItemWithIcon(@IdRes id: Int, title: CharSequence, drawable: Drawable) {
    adapter.menuItems.add(
      MenuItem.ActionItem(id, title, null, icon = MenuIcon.DrawableIcon(drawable)),
    )
  }

  fun addDangerousItemWithIcon(@IdRes id: Int, @StringRes title: Int, @DrawableRes icon: Int) {
    adapter.menuItems.add(
      MenuItem.ActionItem(
        id,
        context.getString(title),
        null,
        icon = MenuIcon.ResourceIcon(icon),
        modifier = ModifierIds.DANGER,
      ),
    )
  }

  fun addItemWithSwitch(
    @IdRes id: Int,
    @IdRes idSwitchOn: Int,
    @IdRes idSwitchOff: Int,
    title: String,
    @DrawableRes icon: Int,
    isOn: Boolean,
  ) {
    adapter.menuItems.add(
      MenuItem.ActionWithSwitchItem(
        id = id,
        switchOnId = idSwitchOn,
        switchOffId = idSwitchOff,
        title = title,
        description = null,
        icon = MenuIcon.ResourceIcon(icon),
        isOn = isOn,
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
    onBackPressedDispatcher: OnBackPressedDispatcher =
      bottomMenuContainer.onBackPressedDispatcher,
    avatarHelper: AvatarHelper? = null,
  ) {
    if (handleInsets) {
      bottomMenuContainer.insets.observeForever(insetsObserver)
    }

    parent = bottomSheetContainer

    adapter.avatarHelper = avatarHelper
    adapter.title = title
    adapter.checked = checked
    adapter.onMenuItemClickListener = onMenuItemClickListener

    adapter.refreshItems()

    val binding = BottomMenuBinding.inflate(inflater, bottomSheetContainer, false)

    val rootView = binding.root
    val bottomSheet = binding.bottomSheet
    val recyclerView = binding.recyclerView

    bottomSheetView = bottomSheet

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

    fixBottomSheetFling(bottomSheetBehavior, recyclerView)

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
      onBackPressedDispatcher.addCallback(
        bottomMenuContainer,
        onBackPressedCallback,
      )
    }
  }

  private fun fixBottomSheetFling(
    bottomSheetBehavior: BottomSheetBehavior<FrameLayout>,
    recyclerView: RecyclerView,
  ) {
    recyclerView.isNestedScrollingEnabled = false
    recyclerView.overScrollMode = OVER_SCROLL_NEVER

    recyclerView.addOnScrollListener(
      object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          super.onScrolled(recyclerView, dx, dy)

          val position = (recyclerView.layoutManager as? LinearLayoutManager)
            ?.findFirstCompletelyVisibleItemPosition()

          bottomSheetBehavior.isDraggable = position == 0
        }
      },
    )
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

    data class MenuItemWithSwitchItem(
      val menuItem: MenuItem.ActionWithSwitchItem,
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
    var avatarHelper: AvatarHelper? = null,
  ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val checkedTextColor = context.getColorFromAttribute(
      androidx.appcompat.R.attr.colorPrimary,
    )
    private val dangerTextColor = context.getColorFromAttribute(
      androidx.appcompat.R.attr.colorError,
    )
    private val defaultTextColor = ContextCompat.getColor(context, R.color.colorTextTitle)

    private val adapterHelper = AdapterHelper<Item>(
      areItemsTheSame = { old, new ->
        old::class == new::class &&
          when (old) {
            FooterItem -> true
            is MenuItemItem ->
              old.menuItem.id == (new as MenuItemItem).menuItem.id
            is TitleItem -> true
            DividerItem -> true
            is MenuItemWithSwitchItem ->
              old.menuItem.id == (new as MenuItemWithSwitchItem).menuItem.id
          }
      },
    ).apply {
      addItemType(TitleItem::class, MenuItemTitleBinding::inflate) { item, b, _ ->
        b.title.text = item.title
        if (item.title == null) {
          b.title.visibility = View.GONE
        }
      }
      addItemType(DividerItem::class, MenuItemDividerBinding::inflate) { item, b, _ -> }
      addItemType(
        clazz = MenuItemItem::class,
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
                androidx.appcompat.R.attr.colorControlNormal,
              ),
            )
            b.checkbox.setImageResource(menuItem.checkIcon)
            b.checkbox.visibility = View.VISIBLE
          }
          menuItem.id == checked -> {
            b.checkbox.setColorFilter(
              context.getColorFromAttribute(
                androidx.appcompat.R.attr.colorPrimary,
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
          when (icon) {
            is MenuIcon.ResourceIcon -> {
              b.iconSpace.visibility = View.VISIBLE
              b.icon.visibility = View.VISIBLE
              b.richImage.visibility = View.GONE

              b.icon.setImageResource(icon.customIcon)
            }
            is MenuIcon.DrawableIcon -> {
              b.iconSpace.visibility = View.VISIBLE
              b.icon.visibility = View.VISIBLE
              b.richImage.visibility = View.GONE

              b.icon.setImageDrawable(icon.customIcon)
            }
            is MenuIcon.CommunityIcon -> {
              b.iconSpace.visibility = View.VISIBLE
              b.icon.visibility = View.GONE
              b.richImage.visibility = View.VISIBLE

              avatarHelper?.loadCommunityIcon(b.richImage, icon.communityRef, icon.url)
            }
          }
        } else {
          b.icon.visibility = View.GONE
          b.richImage.visibility = View.GONE
          b.iconSpace.visibility = View.GONE
        }

        if (item.menuItem.description == null) {
          b.description.visibility = View.GONE
        } else {
          b.description.visibility = View.VISIBLE
          b.description.text = item.menuItem.description
        }

        if (onMenuItemClickListener != null) {
          b.root.setOnClickListener {
            onMenuItemClickListener?.invoke(menuItem)
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
          }
        }
      }
      addItemType(
        clazz = MenuItemWithSwitchItem::class,
        inflateFn = MenuItemWithSwitchBinding::inflate,
        onViewCreated = {
          it.icon.setTag(R.id.icon_tint, ImageViewCompat.getImageTintList(it.icon))
        },
      ) { item, b, _ ->
        val menuItem = item.menuItem

        b.title.text = menuItem.title

        if (menuItem.id == checked) {
          b.title.setTextColor(checkedTextColor)
          b.title.setTypeface(b.title.typeface, Typeface.BOLD)
        } else {
          b.title.setTextColor(defaultTextColor)
          b.title.setTypeface(b.title.typeface, Typeface.NORMAL)
        }

        ImageViewCompat.setImageTintList(
          b.icon,
          b.icon.getTag(R.id.icon_tint) as? ColorStateList,
        )

        val icon = menuItem.icon
        if (icon != null) {
          when (icon) {
            is MenuIcon.ResourceIcon -> {
              b.iconSpace.visibility = View.VISIBLE
              b.icon.visibility = View.VISIBLE
              b.richImage.visibility = View.GONE

              b.icon.setImageResource(icon.customIcon)
            }
            is MenuIcon.DrawableIcon -> {
              b.iconSpace.visibility = View.VISIBLE
              b.icon.visibility = View.VISIBLE
              b.richImage.visibility = View.GONE

              b.icon.setImageDrawable(icon.customIcon)
            }
            is MenuIcon.CommunityIcon -> {
              b.iconSpace.visibility = View.VISIBLE
              b.icon.visibility = View.GONE
              b.richImage.visibility = View.VISIBLE

              avatarHelper?.loadCommunityIcon(b.richImage, icon.communityRef, icon.url)
            }
          }
        } else {
          b.icon.visibility = View.GONE
          b.richImage.visibility = View.GONE
          b.iconSpace.visibility = View.GONE
        }

        if (menuItem.description == null) {
          b.description.visibility = View.GONE
        } else {
          b.description.visibility = View.VISIBLE
          b.description.text = menuItem.description
        }

        // prevent the listener from being called
        b.onOffSwitch.setOnCheckedChangeListener(null)
        b.onOffSwitch.isChecked = menuItem.isOn

        if (onMenuItemClickListener != null) {
          b.root.setOnClickListener {
            onMenuItemClickListener?.invoke(
              MenuItem.ActionItem(
                id = menuItem.id,
                title = menuItem.title,
                description = menuItem.description,
                icon = menuItem.icon,
              ),
            )
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
          }

          b.onOffSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            onMenuItemClickListener?.invoke(
              MenuItem.ActionItem(
                id = if (isChecked) {
                  menuItem.switchOnId
                } else {
                  menuItem.switchOffId
                },
                title = menuItem.title,
                description = menuItem.description,
                icon = menuItem.icon,
              ),
            )
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
          }
        }
      }
      addItemType(FooterItem::class, MenuItemFooterBinding::inflate) { _, _, _ -> }
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
          description = null,
          icon = MenuIcon.ResourceIcon(icon),
          modifier = modifier,
        ),
      )
    }

    fun addDividerIfNeeded() {
      if (menuItems.lastOrNull() != MenuItem.DividerItem && menuItems.isNotEmpty()) {
        menuItems.add(MenuItem.DividerItem)
      }
    }

    fun refreshItems(cb: () -> Unit = {}) {
      val newItems = mutableListOf<Item>()

      newItems.add(TitleItem(title))
      menuItems.forEach {
        when (it) {
          is MenuItem.ActionItem ->
            newItems.add(MenuItemItem(it))

          MenuItem.DividerItem ->
            newItems.add(DividerItem)

          is MenuItem.ActionWithSwitchItem ->
            newItems.add(MenuItemWithSwitchItem(it))
        }
      }
      if (newItems.lastOrNull() is DividerItem) {
        newItems.removeAt(newItems.size - 1)
      }
      newItems.add(FooterItem)

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
      val title: CharSequence,
      val description: CharSequence?,
      val icon: MenuIcon? = null,
      @DrawableRes val checkIcon: Int = 0,
      val modifier: Int = ModifierIds.NONE,
    ) : MenuItem

    class ActionWithSwitchItem(
      @IdRes val id: Int,
      @IdRes val switchOnId: Int,
      @IdRes val switchOffId: Int,
      val title: CharSequence,
      val description: CharSequence?,
      val icon: MenuIcon? = null,
      val isOn: Boolean,
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
    data class CommunityIcon(
      val communityRef: CommunityRef,
      val url: String?,
    ) : MenuIcon
  }
}
