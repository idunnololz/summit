/*
 * Designed and developed by 2017 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.idunnololz.summit.util.colorPicker.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import java.util.Locale

/** ColorUtils a util class for changing the form of colors.  */
internal object ColorUtils {
    /** changes color to string hex code.  */
    fun getHexCode(@ColorInt color: Int): String {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return String.format(Locale.getDefault(), "%02X%02X%02X%02X", a, r, g, b)
    }

    /** changes color to argb integer array.  */
    fun getColorARGB(@ColorInt color: Int): IntArray {
        val argb = IntArray(4)
        argb[0] = Color.alpha(color)
        argb[1] = Color.red(color)
        argb[2] = Color.green(color)
        argb[3] = Color.blue(color)
        return argb
    }
}
