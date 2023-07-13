package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.idunnololz.summit.R
import com.idunnololz.summit.databinding.LemmyHeaderViewBinding

class LemmyHeaderView : LinearLayout {

    companion object {
        const val STATIC_VIEW_COUNT = 3
    }

    private val textView1: TextView
    val textView2: TextView
    private val flairView: FlairView

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

        val binding = LemmyHeaderViewBinding.inflate(LayoutInflater.from(context), this)

        textView1 = binding.textView1
        textView2 = binding.textView2
        flairView = binding.flairView

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
        }

    fun getFlairView(): FlairView = flairView
}