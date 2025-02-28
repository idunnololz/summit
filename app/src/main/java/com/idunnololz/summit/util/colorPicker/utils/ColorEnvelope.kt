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

import androidx.annotation.ColorInt

/** ColorEnvelope is a wrapper class of colors for provide various forms of color.  */
@Suppress("unused")
class ColorEnvelope(
    @field:ColorInt
    /**
     * gets envelope's color.
     *
     * @return color.
     */
    @get:ColorInt @param:ColorInt val color: Int
) {
    /**
     * gets envelope's hex code value.
     *
     * @return hex code.
     */
    val hexCode: String = ColorUtils.getHexCode(color)

    /**
     * gets envelope's argb color.
     *
     * @return argb integer array.
     */
    val argb: IntArray = ColorUtils.getColorARGB(color)
}
