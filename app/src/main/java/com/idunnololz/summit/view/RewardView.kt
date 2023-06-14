package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.idunnololz.summit.R
import com.idunnololz.summit.reddit_objects.AwardInfo

class RewardView : LinearLayout {

    private val textView: TextView
    private val rewardIcon: ImageView

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        orientation = HORIZONTAL
        inflate(context, R.layout.reward_view, this)

        textView = findViewById(R.id.text)
        rewardIcon = findViewById(R.id.rewardIcon)

        setPadding(
            context.resources.getDimensionPixelOffset(R.dimen.padding_quarter), 0,
            context.resources.getDimensionPixelOffset(R.dimen.padding_quarter), 0
        )
    }

    fun setAward(awardInfo: AwardInfo) {
        if (awardInfo.count == 1) {
            textView.visibility = View.GONE
        } else {
            textView.visibility = View.VISIBLE
            textView.text = context.getString(R.string.award_count_format, awardInfo.count)
        }
        Glide.with(this)
            .load(awardInfo.iconUrl)
            .into(rewardIcon)
    }

}