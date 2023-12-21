package com.idunnololz.summit.util.color

import android.content.Context
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorManager @Inject constructor() {

    var textColor: Int = 0
        private set
    var controlColor: Int = 0
        private set

    fun updateColors(context: Context) {
        textColor = context.getColorCompat(R.color.colorText)
        controlColor = context.getColorFromAttribute(androidx.appcompat.R.attr.colorControlNormal)
    }
}
