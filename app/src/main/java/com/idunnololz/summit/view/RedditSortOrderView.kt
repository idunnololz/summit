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
import com.idunnololz.summit.reddit.RedditSortOrder
import com.idunnololz.summit.reddit.RedditSortOrder.TimeFrame

class RedditSortOrderView : LinearLayout {

    interface OnSortOrderChangedListener {
        fun onSortOrderChanged(newSortOrder: RedditSortOrder)
    }

    private val sortOrderSpinner: Spinner
    private val topTimeFrameSpinner: Spinner

    private var sortOrderAdapter: ArrayAdapter<SortOrderItem>? = null
    private var timeFrameAdapter: ArrayAdapter<TimeFrameItem>? = null

    private var currentSelection: RedditSortOrder = RedditSortOrder.NewOrder

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
                SortOrderItem(RedditSortOrder.HotOrder, context.getString(R.string.sort_order_hot)),
                SortOrderItem(RedditSortOrder.NewOrder, context.getString(R.string.sort_order_new)),
                SortOrderItem(
                    RedditSortOrder.TopOrder(),
                    context.getString(R.string.sort_order_top)
                ),
                SortOrderItem(
                    RedditSortOrder.RisingOrder,
                    context.getString(R.string.sort_order_rising)
                )
            )
        ).apply {
            setDropDownViewResource(R.layout.reddit_sort_order_drop_down_view)
        }

        timeFrameAdapter = ArrayAdapter(
            context, R.layout.reddit_sort_order_option_view, listOf(
                TimeFrameItem(TimeFrame.NOW, context.getString(R.string.time_frame_now)),
                TimeFrameItem(TimeFrame.TODAY, context.getString(R.string.time_frame_today)),
                TimeFrameItem(
                    TimeFrame.THIS_WEEK,
                    context.getString(R.string.time_frame_this_week)
                ),
                TimeFrameItem(
                    TimeFrame.THIS_MONTH,
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
                currentSelection = RedditSortOrder.TopOrder(selected.timeFrame)

                onSortOrderChangedListeners.forEach { it.onSortOrderChanged(currentSelection) }
            }

        }
    }

    private fun onSelectionChanged() {
        val selected = sortOrderSpinner.selectedItem as SortOrderItem
        if (selected.order is RedditSortOrder.TopOrder) {
            topTimeFrameSpinner.visibility = View.VISIBLE
            topTimeFrameSpinner.isEnabled = true
        } else {
            topTimeFrameSpinner.visibility = View.GONE
            topTimeFrameSpinner.isEnabled = false
        }
        currentSelection = selected.order

        onSortOrderChangedListeners.forEach { it.onSortOrderChanged(currentSelection) }
    }

    fun getSelection(): RedditSortOrder = currentSelection

    fun setSelection(selection: RedditSortOrder) {
        currentSelection = selection

        sortOrderAdapter?.let {
            for (i in 0 until it.count) {
                if (requireNotNull(it.getItem(i)).order::class == selection::class) {
                    sortOrderSpinner.setSelection(i)
                    break
                }
            }
        }
        if (selection is RedditSortOrder.TopOrder) {
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
        val order: RedditSortOrder,
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