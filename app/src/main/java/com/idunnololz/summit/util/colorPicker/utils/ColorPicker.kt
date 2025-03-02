package com.idunnololz.summit.util.colorPicker.utils

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
    val color: Int
    val name: String
    val view: View
}