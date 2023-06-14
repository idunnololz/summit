package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.idunnololz.summit.R
import com.idunnololz.summit.util.ext.getColorCompat
import java.lang.RuntimeException

class DayPicker : ConstraintLayout {

    companion object {
        const val DAY_MONDAY = 1
        const val DAY_TUESDAY = 2
        const val DAY_WEDNESDAY = 3
        const val DAY_THURSDAY = 4
        const val DAY_FRIDAY = 5
        const val DAY_SATURDAY = 6
        const val DAY_SUNDAY = 7
    }

    private lateinit var dayViews: List<TextView>

    private val selectedDays = mutableListOf<Int>()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        View.inflate(context, R.layout.day_picker, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        dayViews = listOf(
            findViewById(R.id.day1),
            findViewById(R.id.day2),
            findViewById(R.id.day3),
            findViewById(R.id.day4),
            findViewById(R.id.day5),
            findViewById(R.id.day6),
            findViewById(R.id.day7)
        )
        refreshView()
    }

    private fun refreshView() {
        val days = listOf(
            DAY_SUNDAY,
            DAY_MONDAY,
            DAY_TUESDAY,
            DAY_WEDNESDAY,
            DAY_THURSDAY,
            DAY_FRIDAY,
            DAY_SATURDAY
        )
        dayViews.zip(days).forEach { (v, d) ->
            v.text = when (d) {
                DAY_MONDAY -> "M"
                DAY_TUESDAY -> "T"
                DAY_WEDNESDAY -> "W"
                DAY_THURSDAY -> "T"
                DAY_FRIDAY -> "F"
                DAY_SATURDAY -> "S"
                DAY_SUNDAY -> "S"
                else -> throw RuntimeException("Unknown day: $d")
            }
            if (selectedDays.contains(d)) {
                v.setBackgroundResource(R.drawable.day_selected_bg)
                v.setTextColor(context.getColorCompat(R.color.colorTextInverted))
            } else {
                v.setBackgroundResource(R.drawable.day_normal_bg)
                v.setTextColor(context.getColorCompat(R.color.colorTextTitle))
            }
            v.setOnClickListener {
                val removed = selectedDays.remove(d)
                if (!removed) {
                    selectedDays.add(d)
                }
                refreshView()
            }
        }
    }

    fun getSelectedDays(): List<Int> = selectedDays

    fun setSelectedDays(days: List<Int>) {
        selectedDays.clear()
        selectedDays.addAll(days)

        refreshView()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        dayViews.forEach {
            it.isEnabled = enabled
        }
    }
}