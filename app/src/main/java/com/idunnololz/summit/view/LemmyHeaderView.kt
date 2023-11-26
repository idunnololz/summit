package com.idunnololz.summit.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.google.android.material.R
import com.idunnololz.summit.util.ext.getColorCompat
import com.idunnololz.summit.util.ext.getColorFromAttribute
import com.idunnololz.summit.R as R2

class LemmyHeaderView : LinearLayout {

    companion object {
        const val STATIC_VIEW_COUNT = 3
    }

    val textView1: TextView
    val textView2: TextView
    val textView3: TextView
    private val flairView: FlairView

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        textView1 = LinkifyTextView(context, null, R.attr.textAppearanceBodySmall)
            .style()
        textView2 = LinkifyTextView(context, null, R.attr.textAppearanceBodySmall)
            .style()
        textView3 = LinkifyTextView(context, null, R.attr.textAppearanceBodySmall)
            .style()
        flairView = FlairView(context)

        addView(textView1)
        addView(flairView)
        addView(textView2)
        addView(textView3)

        flairView.visibility = View.GONE
    }

    fun setTextFirstPart(text: CharSequence) {
        textView1.text = text
    }

    fun setTextSecondPart(text: CharSequence) {
        textView2.text = text
    }

    var textSize: Float = 0f
        set(value) {
            field = value

            textView1.textSize = value
            textView2.textSize = value
            textView3.textSize = value
        }

    fun getFlairView(): FlairView = flairView

    override fun setOrientation(orientation: Int) {
        if (orientation == this.orientation) {
            return
        }

        super.setOrientation(orientation)

        if (orientation == VERTICAL) {
            textView3.visibility = View.GONE
        } else {
            textView3.visibility = View.VISIBLE
        }
    }

    private fun LinkifyTextView.style(): LinkifyTextView {
        maxLines = 1
        isSingleLine = true
        setTextColor(context.getColorCompat(R2.color.colorTextFaint))
        TextViewCompat.setCompoundDrawableTintList(
            this,
            ColorStateList.valueOf(
                context.getColorFromAttribute(R.attr.colorControlNormal),
            ),
        )

        return this
    }
}
