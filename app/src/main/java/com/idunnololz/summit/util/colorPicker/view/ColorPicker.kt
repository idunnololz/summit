package com.idunnololz.summit.util.colorPicker.view

import android.view.View
import androidx.annotation.ColorInt
import com.idunnololz.summit.util.colorPicker.OnColorPickedListener

interface ColorPickerContainer {
    val colorPicker: ColorPicker
    // To avoid naming conflict with View.getRootView()
    val rootView2: View
}

interface ColorPicker {
    fun setColor(@ColorInt color: Int, animate: Boolean = false)
    fun setListener(listener: OnColorPickedListener?)
    var isAlphaEnabled: Boolean
    val color: Int
    val name: String
    val isTrackingTouch: Boolean
    val view: View
}