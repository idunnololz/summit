package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.CommunitySortOrder.TimeFrame
import com.idunnololz.summit.lemmy.CommunitySortOrder.TimeFrame.*

class LemmySortOrderView : LinearLayout {

    interface OnSortOrderChangedListener {
        fun onSortOrderChanged(newSortOrder: CommunitySortOrder)
    }

    private val sortOrderSpinner: Spinner
    private val topTimeFrameSpinner: Spinner

    private var sortOrderAdapter: ArrayAdapter<SortOrderItem>? = null
    private var timeFrameAdapter: ArrayAdapter<TimeFrameItem>? = null

    private var currentSelection: CommunitySortOrder = CommunitySortOrder.Active

    private val onSortOrderChangedListeners = hashSetOf<OnSortOrderChangedListener>()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        View.inflate(context, R.layout.reddit_sort_order_view, this)

        sortOrderSpinner = findViewById(R.id.sortOrderSpinner)
        topTimeFrameSpinner = findViewById(R.id.topTimeFrameSpinner)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        sortOrderAdapter = ArrayAdapter(
            context, R.layout.reddit_sort_order_option_view, listOf(
                SortOrderItem(CommunitySortOrder.Hot, context.getString(R.string.sort_order_hot)),
                SortOrderItem(CommunitySortOrder.New, context.getString(R.string.sort_order_new)),
                SortOrderItem(
                    CommunitySortOrder.TopOrder(),
                    context.getString(R.string.sort_order_top)
                ),
            )
        ).apply {
            setDropDownViewResource(R.layout.reddit_sort_order_drop_down_view)
        }

        timeFrameAdapter = ArrayAdapter(
            context, R.layout.reddit_sort_order_option_view, listOf(
                TimeFrameItem(NOW, context.getString(R.string.time_frame_now)),
                TimeFrameItem(TODAY, context.getString(R.string.time_frame_today)),
                TimeFrameItem(
                    THIS_WEEK,
                    context.getString(R.string.time_frame_this_week)
                ),
                TimeFrameItem(
                    THIS_MONTH,
                    context.getString(R.string.time_frame_this_month)
                ),
                TimeFrameItem(
                    TimeFrame.THIS_YEAR,
                    context.getString(R.string.time_frame_this_year)
                ),
                TimeFrameItem(TimeFrame.ALL_TIME, context.getString(R.string.time_frame_all_time))
            )
        ).apply {
            setDropDownViewResource(R.layout.reddit_sort_order_drop_down_view)
        }

        sortOrderSpinner.adapter = sortOrderAdapter
        sortOrderSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onSelectionChanged()
            }
        }

        topTimeFrameSpinner.adapter = timeFrameAdapter
        topTimeFrameSpinner.isEnabled = false
        topTimeFrameSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!topTimeFrameSpinner.isEnabled) return

                val selected = topTimeFrameSpinner.selectedItem as TimeFrameItem
                currentSelection = CommunitySortOrder.TopOrder(selected.timeFrame)

                onSortOrderChangedListeners.forEach { it.onSortOrderChanged(currentSelection) }
            }

        }
    }

    private fun onSelectionChanged() {
        val selected = sortOrderSpinner.selectedItem as SortOrderItem
        if (selected.order is CommunitySortOrder.TopOrder) {
            topTimeFrameSpinner.visibility = View.VISIBLE
            topTimeFrameSpinner.isEnabled = true
        } else {
            topTimeFrameSpinner.visibility = View.GONE
            topTimeFrameSpinner.isEnabled = false
        }
        currentSelection = selected.order

        onSortOrderChangedListeners.forEach { it.onSortOrderChanged(currentSelection) }
    }

    fun getSelection(): CommunitySortOrder = currentSelection

    fun setSelection(selection: CommunitySortOrder) {
        currentSelection = selection

        sortOrderAdapter?.let {
            for (i in 0 until it.count) {
                if (requireNotNull(it.getItem(i)).order::class == selection::class) {
                    sortOrderSpinner.setSelection(i)
                    break
                }
            }
        }
        if (selection is CommunitySortOrder.TopOrder) {
            timeFrameAdapter?.let {
                for (i in 0 until it.count) {
                    if (requireNotNull(it.getItem(i)).timeFrame == selection.timeFrame) {
                        topTimeFrameSpinner.setSelection(i)
                        break
                    }
                }
            }
        }
    }

    fun registerOnSortOrderChangedListener(onSortOrderChangedListener: OnSortOrderChangedListener) {
        onSortOrderChangedListeners.add(onSortOrderChangedListener)
    }

    fun unregisterOnSortOrderChangedListener(onSortOrderChangedListener: OnSortOrderChangedListener) {
        onSortOrderChangedListeners.remove(onSortOrderChangedListener)
    }

    private class SortOrderItem(
        val order: CommunitySortOrder,
        private val text: String
    ) {
        override fun toString(): String = text
    }

    private class TimeFrameItem(
        val timeFrame: TimeFrame,
        private val text: String
    ) {
        override fun toString(): String = text
    }

}