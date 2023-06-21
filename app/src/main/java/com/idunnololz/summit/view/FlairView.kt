package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.TextViewCompat
import coil.load
import com.google.android.material.textview.MaterialTextView
import com.idunnololz.summit.R
import com.idunnololz.summit.util.Utils

class FlairView : LinearLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun setFlairRichText(flairRichText: Any) {
        removeAllViews()

        if (flairRichText is List<*> && flairRichText.isNotEmpty()) {
            flairRichText.forEach a@{
                if (it is Map<*, *>) {
                    if (it["e"] == "emoji") {
                        val url = it["u"] ?: return@a

                        val view = ImageView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }

                        addView(view)

                        val iconSize =
                            context.resources.getDimensionPixelSize(R.dimen.reward_icon_size)
                        view.load(url)
                    } else {
                        val view = MaterialTextView(context)
                        TextViewCompat.setTextAppearance(
                            view,
                            R.style.TextAppearance_MyTheme_Headline4
                        )
                        view.text = it["t"] as String
                        view.setPadding(
                            Utils.convertDpToPixel(4f).toInt(),
                            0,
                            Utils.convertDpToPixel(4f).toInt(),
                            0
                        )
                        addView(view)
                    }
                }
            }
        }
    }
}