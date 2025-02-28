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
package com.idunnololz.summit.util.colorPicker.listeners

import androidx.annotation.ColorInt

/**
 * ColorListener is invoked whenever [com.skydoves.colorpickerview.ColorPickerView] is
 * triggered.
 */
interface ColorListener : ColorPickerViewListener {
    /**
     * invoked by [com.skydoves.colorpickerview.ColorPickerView].
     *
     * @param color the last selected color.
     * @param fromUser triggered by the user(true) or not(false).
     */
    fun onColorSelected(@ColorInt color: Int, fromUser: Boolean)
}
