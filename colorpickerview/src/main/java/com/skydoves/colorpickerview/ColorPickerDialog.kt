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
package com.skydoves.colorpickerview

import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ListAdapter
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.colorpickerview.databinding.ColorpickerviewDialogColorpickerBinding
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.listeners.ColorListener
import com.skydoves.colorpickerview.listeners.ColorPickerViewListener
import com.skydoves.colorpickerview.preference.ColorPickerPreferenceManager
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar

/**
 * ColorPickerDialog is a dialog what having [ColorPickerView], [AlphaSlideBar] and
 * [BrightnessSlideBar].
 */
@Suppress("unused")
class ColorPickerDialog(context: Context) : AlertDialog(context) {
    private val colorPickerView: ColorPickerView? = null

    /**
     * Builder class for create [ColorPickerDialog].
     */
    class Builder : MaterialAlertDialogBuilder {
        private val dialogBinding: ColorpickerviewDialogColorpickerBinding

        /**
         * gets [ColorPickerView] on [Builder].
         *
         * @return [ColorPickerView].
         */
        val colorPickerView: ColorPickerView
        private var shouldAttachAlphaSlideBar = true
        private var shouldAttachBrightnessSlideBar = true
        private var bottomSpace = SizeUtils.dp2Px(context, 10)

        init {
            val layoutInflater = LayoutInflater.from(
                context,
            )
            this.dialogBinding =
                ColorpickerviewDialogColorpickerBinding.inflate(layoutInflater, null, false)
            this.colorPickerView = dialogBinding.colorPickerView
            colorPickerView.attachAlphaSlider(dialogBinding.alphaSlideBar)
            colorPickerView.attachBrightnessSlider(dialogBinding.brightnessSlideBar)
            colorPickerView.setColorListener(
                ColorEnvelopeListener { envelope, fromUser ->
                    // no stubs
                },
            )
            super.setView(dialogBinding.root)
        }

        constructor(context: Context) : super(context)

        constructor(context: Context, themeResId: Int) : super(context, themeResId)

        /**
         * sets [ColorPickerView] manually.
         *
         * @param colorPickerView [ColorPickerView].
         * @return [Builder].
         */
        fun setColorPickerView(colorPickerView: ColorPickerView?): Builder {
            dialogBinding.colorPickerViewFrame.removeAllViews()
            dialogBinding.colorPickerViewFrame.addView(colorPickerView)
            return this
        }

        /**
         * if true, attaches a [AlphaSlideBar] on the [ColorPickerDialog].
         *
         * @param value true or false.
         * @return [Builder].
         */
        fun attachAlphaSlideBar(value: Boolean): Builder {
            this.shouldAttachAlphaSlideBar = value
            return this
        }

        /**
         * if true, attaches a [BrightnessSlideBar] on the [ColorPickerDialog].
         *
         * @param value true or false.
         * @return [Builder].
         */
        fun attachBrightnessSlideBar(value: Boolean): Builder {
            this.shouldAttachBrightnessSlideBar = value
            return this
        }

        /**
         * sets the preference name.
         *
         * @param preferenceName preference name.
         * @return [Builder].
         */
        fun setPreferenceName(preferenceName: String?): Builder {
            colorPickerView.preferenceName = preferenceName
            return this
        }

        /**
         * sets the margin of the bottom. this space visible when [AlphaSlideBar] or [ ] is attached.
         *
         * @param bottomSpace space of the bottom.
         * @return [Builder].
         */
        fun setBottomSpace(bottomSpace: Int): Builder {
            this.bottomSpace = SizeUtils.dp2Px(context, bottomSpace)
            return this
        }

        /**
         * sets positive button with [ColorPickerViewListener] on the [ColorPickerDialog].
         *
         * @param textId        string resource integer id.
         * @param colorListener [ColorListener].
         * @return [Builder].
         */
        fun setPositiveButton(textId: Int, colorListener: ColorPickerViewListener): Builder {
            super.setPositiveButton(textId, getOnClickListener(colorListener))
            return this
        }

        /**
         * sets positive button with [ColorPickerViewListener] on the [ColorPickerDialog].
         *
         * @param text          string text value.
         * @param colorListener [ColorListener].
         * @return [Builder].
         */
        fun setPositiveButton(
            text: CharSequence?,
            colorListener: ColorPickerViewListener,
        ): Builder {
            super.setPositiveButton(text, getOnClickListener(colorListener))
            return this
        }

        override fun setNegativeButton(
            textId: Int,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setNegativeButton(textId, listener)
            return this
        }

        override fun setNegativeButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setNegativeButton(text, listener)
            return this
        }

        private fun getOnClickListener(
            colorListener: ColorPickerViewListener,
        ): DialogInterface.OnClickListener {
            return object : DialogInterface.OnClickListener {
                override fun onClick(dialogInterface: DialogInterface, i: Int) {
                    if (colorListener is ColorListener) {
                        colorListener.onColorSelected(colorPickerView.getColor(), true)
                    } else if (colorListener is ColorEnvelopeListener) {
                        colorListener
                            .onColorSelected(colorPickerView.getColorEnvelope(), true)
                    }
                    ColorPickerPreferenceManager.getInstance(context)
                        .saveColorPickerData(colorPickerView)
                }
            }
        }

        /**
         * shows a created [ColorPickerDialog].
         *
         * @return [AlertDialog].
         */
        override fun create(): AlertDialog {
            dialogBinding.colorPickerViewFrame.removeAllViews()
            dialogBinding.colorPickerViewFrame.addView(colorPickerView)

            val alphaSlideBar = colorPickerView.alphaSlideBar
            if (shouldAttachAlphaSlideBar && alphaSlideBar != null) {
                dialogBinding.alphaSlideBarFrame.removeAllViews()
                dialogBinding.alphaSlideBarFrame.addView(alphaSlideBar)
                colorPickerView.attachAlphaSlider(alphaSlideBar)
            } else if (!shouldAttachAlphaSlideBar) {
                dialogBinding.alphaSlideBarFrame.removeAllViews()
            }

            val brightnessSlideBar = colorPickerView.brightnessSlider
            if (shouldAttachBrightnessSlideBar && brightnessSlideBar != null) {
                dialogBinding.brightnessSlideBarFrame.removeAllViews()
                dialogBinding.brightnessSlideBarFrame.addView(brightnessSlideBar)
                colorPickerView.attachBrightnessSlider(brightnessSlideBar)
            } else if (!shouldAttachBrightnessSlideBar) {
                dialogBinding.brightnessSlideBarFrame.removeAllViews()
            }

            if (!shouldAttachAlphaSlideBar && !shouldAttachBrightnessSlideBar) {
                dialogBinding.spaceBottom.visibility =
                    View.GONE
            } else {
                dialogBinding.spaceBottom.visibility =
                    View.VISIBLE
                dialogBinding.spaceBottom.layoutParams.height = bottomSpace
            }

            super.setView(dialogBinding.root)
            return super.create()
        }

        override fun setTitle(titleId: Int): Builder {
            super.setTitle(titleId)
            return this
        }

        override fun setTitle(title: CharSequence?): Builder {
            super.setTitle(title)
            return this
        }

        override fun setCustomTitle(customTitleView: View?): Builder {
            super.setCustomTitle(customTitleView)
            return this
        }

        override fun setMessage(messageId: Int): Builder {
            super.setMessage(context.getString(messageId))
            return this
        }

        override fun setMessage(message: CharSequence?): Builder {
            super.setMessage(message)
            return this
        }

        override fun setIcon(iconId: Int): Builder {
            super.setIcon(iconId)
            return this
        }

        override fun setIcon(icon: Drawable?): Builder {
            super.setIcon(icon)
            return this
        }

        override fun setIconAttribute(attrId: Int): Builder {
            super.setIconAttribute(attrId)
            return this
        }

        override fun setCancelable(cancelable: Boolean): Builder {
            super.setCancelable(cancelable)
            return this
        }

        override fun setOnCancelListener(
            onCancelListener: DialogInterface.OnCancelListener?,
        ): Builder {
            super.setOnCancelListener(onCancelListener)
            return this
        }

        override fun setOnDismissListener(
            onDismissListener: DialogInterface.OnDismissListener?,
        ): Builder {
            super.setOnDismissListener(onDismissListener)
            return this
        }

        override fun setOnKeyListener(onKeyListener: DialogInterface.OnKeyListener?): Builder {
            super.setOnKeyListener(onKeyListener)
            return this
        }

        override fun setPositiveButton(
            textId: Int,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setPositiveButton(textId, listener)
            return this
        }

        override fun setPositiveButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setPositiveButton(text, listener)
            return this
        }

        override fun setNeutralButton(
            textId: Int,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setNeutralButton(textId, listener)
            return this
        }

        override fun setNeutralButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setNeutralButton(text, listener)
            return this
        }

        override fun setItems(itemsId: Int, listener: DialogInterface.OnClickListener?): Builder {
            super.setItems(itemsId, listener)
            return this
        }

        override fun setItems(
            items: Array<CharSequence>?,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setItems(items, listener)
            return this
        }

        override fun setAdapter(
            adapter: ListAdapter?,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setAdapter(adapter, listener)
            return this
        }

        override fun setCursor(
            cursor: Cursor?,
            listener: DialogInterface.OnClickListener?,
            labelColumn: String,
        ): Builder {
            super.setCursor(cursor, listener, labelColumn)
            return this
        }

        override fun setMultiChoiceItems(
            itemsId: Int,
            checkedItems: BooleanArray?,
            listener: DialogInterface.OnMultiChoiceClickListener?,
        ): Builder {
            super.setMultiChoiceItems(itemsId, checkedItems, listener)
            return this
        }

        override fun setMultiChoiceItems(
            items: Array<CharSequence>?,
            checkedItems: BooleanArray?,
            listener: DialogInterface.OnMultiChoiceClickListener?,
        ): Builder {
            super.setMultiChoiceItems(items, checkedItems, listener)
            return this
        }

        override fun setMultiChoiceItems(
            cursor: Cursor?,
            isCheckedColumn: String,
            labelColumn: String,
            listener: DialogInterface.OnMultiChoiceClickListener?,
        ): Builder {
            super.setMultiChoiceItems(cursor, isCheckedColumn, labelColumn, listener)
            return this
        }

        override fun setSingleChoiceItems(
            itemsId: Int,
            checkedItem: Int,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setSingleChoiceItems(itemsId, checkedItem, listener)
            return this
        }

        override fun setSingleChoiceItems(
            cursor: Cursor?,
            checkedItem: Int,
            labelColumn: String,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setSingleChoiceItems(cursor, checkedItem, labelColumn, listener)
            return this
        }

        override fun setSingleChoiceItems(
            items: Array<CharSequence>?,
            checkedItem: Int,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setSingleChoiceItems(items, checkedItem, listener)
            return this
        }

        override fun setSingleChoiceItems(
            adapter: ListAdapter?,
            checkedItem: Int,
            listener: DialogInterface.OnClickListener?,
        ): Builder {
            super.setSingleChoiceItems(adapter, checkedItem, listener)
            return this
        }

        override fun setOnItemSelectedListener(
            listener: AdapterView.OnItemSelectedListener?,
        ): Builder {
            super.setOnItemSelectedListener(listener)
            return this
        }

        override fun setView(layoutResId: Int): Builder {
            super.setView(layoutResId)
            return this
        }

        override fun setView(view: View?): Builder {
            super.setView(view)
            return this
        }
    }
}
