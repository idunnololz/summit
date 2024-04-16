package com.idunnololz.summit.util.ext

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import com.idunnololz.summit.R
import com.idunnololz.summit.spans.CenteredImageSpan

private val TAG = "PopupMenuExt"

@SuppressLint("RestrictedApi")
fun PopupMenu.forceShowIcons() {
    try {
        val fields = javaClass.declaredFields
        for (field in fields) {
            if ("mPopup" == field.name) {
                field.isAccessible = true
                val menuPopupHelper = field.get(this)
                val h = menuPopupHelper as MenuPopupHelper
                h.setForceShowIcon(true)
                break
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "", e)
    }
}

fun PopupMenu.insertMenuIcons(context: Context) {
    insertMenuItemIcons(context, this)
}

/**
 * Moves icons from the PopupMenu's MenuItems' icon fields into the menu title as a Spannable with the icon and title text.
 */
private fun insertMenuItemIcons(context: Context, popupMenu: PopupMenu) {
    val menu: Menu = popupMenu.menu
    if (hasIcon(menu)) {
        for (i in 0 until menu.size()) {
            menu.getItem(i)?.let {
                insertMenuItemIcon(context, it)
            }
        }
    }
}

/**
 * @return true if the menu has at least one MenuItem with an icon.
 */
private fun hasIcon(menu: Menu): Boolean {
    for (i in 0 until menu.size()) {
        if (menu.getItem(i).icon != null) return true
    }
    return false
}

/**
 * Converts the given MenuItem's title into a Spannable containing both its icon and title.
 */
private fun insertMenuItemIcon(context: Context, menuItem: MenuItem) {
    var icon: Drawable? = menuItem.icon

    // If there's no icon, we insert a transparent one to keep the title aligned with the items
    // which do have icons.
    if (icon == null) icon = ColorDrawable(Color.TRANSPARENT)
    val iconSize: Int = context.resources.getDimensionPixelSize(R.dimen.menu_item_icon_size)
    icon.setBounds(0, 0, iconSize, iconSize)
    val imageSpan = CenteredImageSpan(icon)

    // Add a space placeholder for the icon, before the title.
    val ssb = SpannableStringBuilder("       " + menuItem.title)

    // Replace the space placeholder with the icon.
    ssb.setSpan(imageSpan, 1, 2, 0)
    menuItem.title = ssb
    // Set the icon to null just in case, on some weird devices, they've customized Android to display
    // the icon in the menu... we don't want two icons to appear.
    menuItem.icon = null
}
