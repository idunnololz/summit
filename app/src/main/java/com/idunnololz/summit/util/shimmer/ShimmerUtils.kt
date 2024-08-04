package com.idunnololz.summit.util.shimmer

import android.content.Context
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils
import com.idunnololz.summit.util.ext.getColorCompat

fun newShimmerDrawable16to9(context: Context): ShimmerDrawable =
    newShimmerDrawable(context, 9f / 16f)

fun newShimmerDrawableSquare(context: Context): ShimmerDrawable = newShimmerDrawable(context, 1f)

fun newShimmerDrawable(context: Context, ratio: Float): ShimmerDrawable {
    val size = Utils.convertDpToPixel(24f)
    val shimmer = Shimmer.ColorHighlightBuilder()
        .setBaseColor(0)
        .setHighlightColor(context.getColorCompat(R.color.colorText))
        .setFixedWidth(size.toInt())
        .setFixedHeight((size * ratio).toInt())
        .setHighlightAlpha(0.15f)
        .build()

    return ShimmerDrawable().apply {
        this.shimmer = shimmer
    }
}
