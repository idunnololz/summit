package com.idunnololz.summit.util.colorPicker

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.viewpager.widget.ViewPager
import com.idunnololz.summit.util.colorPicker.utils.ColorPicker
import com.idunnololz.summit.util.colorPicker.utils.ColorPickerContainer

class ColorPickerPagerAdapter(
    private val context: Context,
    private val pickers: List<ColorPickerContainer>,
    private val viewPager: ViewPager,
) : HeightablePagerAdapter(), OnColorPickedListener, ViewPager.OnPageChangeListener {

    private var listener: OnColorPickedListener? = null

    @ColorInt
    private var color = Color.BLACK
    private var isAlphaEnabled = true
    private var position = 0

    /**
     * Specify a listener to receive updates when a new color is selected.
     *
     * @param listener         The listener to receive updates.
     */
    fun setListener(listener: OnColorPickedListener?) {
        this.listener = listener
    }

    /**
     * Specify an initial color for the picker(s) to use.
     *
     * @param color             The initial color int.
     */
    fun setColor(@ColorInt color: Int) {
        this.color = color
        pickers[position].colorPicker.setColor(color)
    }

    /**
     * Specify whether alpha values should be enabled. This parameter
     * defaults to true.
     *
     * @param isAlphaEnabled    Whether alpha values are enabled.
     */
    fun setAlphaEnabled(isAlphaEnabled: Boolean) {
        this.isAlphaEnabled = isAlphaEnabled
    }

    /**
     * Update the color value used by the picker(s).
     *
     * @param color             The new color int.
     * @param animate           Whether to animate the change in values.
     */
    fun updateColor(@ColorInt color: Int, animate: Boolean) {
        pickers[position].colorPicker.setColor(color, animate)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view: View =
            if (position >= 0 && position < pickers.size) {
                val picker: ColorPicker = pickers[position].colorPicker
                picker.apply {
                    setListener(this@ColorPickerPagerAdapter)
                    isAlphaEnabled = this@ColorPickerPagerAdapter.isAlphaEnabled
                    setColor(this@ColorPickerPagerAdapter.color)
                }
                pickers[position].rootView2
            } else {
                View(context)
            }

        container.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return pickers.size
    }

    override fun getHeightAt(position: Int, widthMeasureSpec: Int, heightMeasureSpec: Int): Int {
        val picker = pickers[position].rootView2
        picker.measure(widthMeasureSpec, heightMeasureSpec)
        return picker.measuredHeight
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pickers[position].colorPicker.name
    }

    override fun onColorPicked(pickerView: ColorPicker?, @ColorInt color: Int) {
        this.color = color
        if (listener != null) listener!!.onColorPicked(pickerView, color)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        this.position = position
        pickers[position].colorPicker.setColor(color)
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            viewPager.requestLayout()
            viewPager.post {
                pickers[viewPager.currentItem].colorPicker.setColor(color)
            }
        }
    }
}
