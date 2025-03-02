package com.idunnololz.summit.util.colorPicker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.ColorpickerDialogColorPickerBinding
import com.idunnololz.summit.lemmy.utils.stateStorage.GlobalStateStorage
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.colorPicker.utils.ColorPicker
import com.idunnololz.summit.util.colorPicker.utils.ColorPickerContainer
import com.idunnololz.summit.util.colorPicker.view.ColorPickerHistoryView
import com.idunnololz.summit.util.colorPicker.view.ColorPickerView
import com.idunnololz.summit.util.colorPicker.view.HsvPickerView
import com.idunnololz.summit.util.colorPicker.view.RgbPickerView
import com.idunnololz.summit.util.colorPicker.view.SmoothColorView
import kotlin.math.min

class ColorPickerDialog(
    private val context: Context,
    private val title: String,
    color: Int,
    private val defaultColor: Int? = null,
    private val globalStateStorage: GlobalStateStorage,
) : AlertDialog(context),
    OnColorPickedListener {

    companion object {
        private const val INST_KEY_ALPHA = "me.jfenn.colorpickerdialog.INST_KEY_ALPHA"
        private const val INST_KEY_PRESETS = "me.jfenn.colorpickerdialog.INST_KEY_PRESETS"
        private const val INST_KEY_COLOR = "me.jfenn.colorpickerdialog.INST_KEY_COLOR"
        private const val INST_KEY_TITLE = "me.jfenn.colorpickerdialog.INST_KEY_TITLE"
    }

    private val colorView: SmoothColorView
    private val colorHex: AppCompatEditText
    private val tabLayout: TabLayout
    private val slidersPager: ViewPager
    private var slidersAdapter: ColorPickerPagerAdapter? = null

    private var pickers: List<ColorPickerContainer> = listOf()

    private var isAlphaEnabled = true
    private var presets = IntArray(0)

    private var shouldIgnoreNextHex = false

    /**
     * Get the current color int selected by the picker.
     *
     * @return                  The current color of the picker.
     */
    @get:ColorInt
    @ColorInt
    var color: Int = color
        private set

    private var listener: OnColorPickedListener? = null


    init {
        val inflater = LayoutInflater.from(
            context,
        )
        val b = ColorpickerDialogColorPickerBinding
            .inflate(inflater, null, false)

        b.title.text = title

        colorView = b.color
        colorHex = b.colorHex
        tabLayout = b.tabLayout
        slidersPager = b.slidersPager

        pickers = listOf(
            ColorPickerView(context),
            HsvPickerView(context),
            RgbPickerView(context),
            ColorPickerHistoryView(context, globalStateStorage),
        )

        for (p in pickers) {
            p.colorPicker.setColor(color)
        }

        val slidersAdapter = ColorPickerPagerAdapter(context, pickers, b.slidersPager)
        this.slidersAdapter = slidersAdapter
        slidersAdapter.setListener(this)
        slidersAdapter.setAlphaEnabled(isAlphaEnabled)
        slidersAdapter.setColor(color)

        slidersPager.setAdapter(slidersAdapter)
        slidersPager.addOnPageChangeListener(slidersAdapter)
        tabLayout.setupWithViewPager(slidersPager)

        colorHex.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    val editable = colorHex.getText()
                    if (editable != null && !shouldIgnoreNextHex) {
                        val str = editable.toString()

                        if (str.length == (if (isAlphaEnabled) 9 else 7)) {
                            try {
                                val color = Color.parseColor(str)
                                slidersAdapter.updateColor(color, true)
                                updateColorPicked(color, updateFromEditText = true)
                            } catch (ignored: Exception) {
                            }
                        }
                    } else shouldIgnoreNextHex = false
                }
            },
        )

        b.card.setOnClickListener {
            Utils.copyToClipboard(context, colorHex.text.toString())
            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
        }

        with(b.buttonBar) {
            button1.text = context.getString(android.R.string.ok)
            button1.setOnClickListener { confirm() }
            button2.text = context.getString(android.R.string.cancel)
            button2.setOnClickListener { dismiss() }

            if (defaultColor != null) {
                button3.text = context.getString(R.string._default)
                button3.setOnClickListener {
                    saveColorToHistory(defaultColor)
                    listener?.onColorPicked(null, defaultColor)

                    dismiss()
                }
            }
        }

        onColorPicked(null, color)

        this.setView(b.root)
        onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            color = savedInstanceState.getInt(INST_KEY_COLOR, color)
        }
        if (savedInstanceState != null) {
            isAlphaEnabled = savedInstanceState.getBoolean(INST_KEY_ALPHA, isAlphaEnabled)

            val presets = savedInstanceState.getIntArray(INST_KEY_PRESETS)
            if (presets != null) this.presets = presets
        }
    }

    /**
     * Specify whether alpha values should be enabled. This parameter
     * defaults to true.
     *
     * @param isAlphaEnabled Whether alpha values are enabled.
     * @return "This" dialog instance, for method chaining.
     */
    fun withAlphaEnabled(isAlphaEnabled: Boolean): ColorPickerDialog {
        this.isAlphaEnabled = isAlphaEnabled
        return this
    }


    override fun onColorPicked(pickerView: ColorPicker?, @ColorInt color: Int) {
        updateColorPicked(color, updateFromEditText = false)
    }

    private fun updateColorPicked(color: Int, updateFromEditText: Boolean) {
        this.color = color
        colorView.setColor(color, false)

        shouldIgnoreNextHex = true

        if (!updateFromEditText) {
            colorHex.setText(
                String.format(
                    if (isAlphaEnabled) "#%08X" else "#%06X",
                    if (isAlphaEnabled) color else (0xFFFFFF and color),
                ),
            )
            colorHex.clearFocus()
        }

        val textColor = if (ColorUtils.isColorDark(
                ColorUtils.withBackground(
                    color,
                    Color.WHITE,
                ),
            )
        ) Color.WHITE else Color.BLACK
        colorHex.setTextColor(textColor)
        colorHex.backgroundTintList = ColorStateList.valueOf(textColor)
    }

    fun onResume() {
        val window = window
        val displayMetrics = DisplayMetrics()
        val windowmanager = window!!.windowManager
        windowmanager.defaultDisplay.getMetrics(displayMetrics)

        window.setLayout(
            min(
                Utils.convertDpToPixel(
                    if (displayMetrics.widthPixels > displayMetrics.heightPixels) 800f else 500f,
                ),
                displayMetrics.widthPixels * 0.9f,
            ).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
        )

        window.setBackgroundDrawableResource(R.drawable.dialog_background)
    }

    /**
     * Specify a listener to receive updates when a new color is selected.
     *
     * @param listener         The listener to receive updates.
     * @return                "This" dialog instance, for method chaining.
     */
    fun withListener(listener: OnColorPickedListener?): ColorPickerDialog {
        this.listener = listener
        return this
    }

    fun confirm() {
        saveColorToHistory(color)
        listener?.onColorPicked(null, color)

        dismiss()
    }

    private fun saveColorToHistory(color: Int) {
        val currentHistory = globalStateStorage.colorPickerHistory ?: ""
        val colors = currentHistory.split(",")
        val colorStr = java.lang.String.format("#%08X", (0xFFFFFFFF and color.toLong()))
        val newColorsList = (listOf(colorStr) + colors)
        val orderedSet = LinkedHashSet<String>()
        orderedSet.addAll(newColorsList)

        globalStateStorage.colorPickerHistory =
            orderedSet.toList().take(50).joinToString(separator = ",")
    }

}