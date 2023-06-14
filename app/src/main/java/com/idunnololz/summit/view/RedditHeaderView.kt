package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.idunnololz.summit.R

class RedditHeaderView : LinearLayout {

    companion object {
        const val STATIC_VIEW_COUNT = 3
    }

    private val textView1: TextView
    private val textView2: TextView
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

        View.inflate(context, R.layout.reddit_header_view, this)

        textView1 = findViewById(R.id.textView1)
        textView2 = findViewById(R.id.textView2)
        flairView = findViewById(R.id.flairView)

        flairView.visibility = View.GONE
    }

    fun setTextFirstPart(text: CharSequence) {
        textView1.text = text
    }

    fun setTextSecondPart(text: CharSequence) {
        textView2.text = text
    }

    fun getFlairView(): FlairView = flairView
}