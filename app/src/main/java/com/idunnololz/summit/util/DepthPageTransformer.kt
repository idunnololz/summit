package com.idunnololz.summit.util

import android.view.View
import androidx.viewpager.widget.ViewPager

private const val MIN_SCALE = 0.75f

class DepthPageTransformer : ViewPager.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
//                    translationX = pageWidth * -position
                }
                position <= 0 -> { // [-1,0]
                    // Use the default slide transition when moving to the left page
                    alpha = 1f + (position * 0.66f)
                    translationX = pageWidth * -position
                }
                position <= 1 -> { // (0,1]
                    // Fade the page out.
                    alpha = 1f

                    // Counteract the default slide transition
                    translationX = 0f

                    // Scale the page down (between MIN_SCALE and 1)
//                    val scaleFactor = (MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position)))
//                    scaleX = scaleFactor
//                    scaleY = scaleFactor
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}
