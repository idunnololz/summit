package com.idunnololz.summit.util.colorPicker

import androidx.viewpager.widget.PagerAdapter
import com.idunnololz.summit.util.colorPicker.view.HeightableViewPager

abstract class HeightablePagerAdapter : PagerAdapter(), HeightableViewPager.Heightable {
    private var position = -1

    override fun setPrimaryItem(container: android.view.ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)

        if (position != this.position) {
            this.position = position
            container.requestLayout()
        }
    }
}