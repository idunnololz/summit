package com.idunnololz.summit.lemmy.utils

import com.idunnololz.summit.R
import com.idunnololz.summit.lemmy.CommunitySortOrder
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu

fun BaseFragment<*>.showSortTypeMenu(
    getCurrentSortType: () -> CommunitySortOrder,
    onSortOrderSelected: (CommunitySortOrder) -> Unit,
) {
    val _sortByTopMenu: BottomMenu by lazy {
        BottomMenu(requireContext()).apply {
            addItem(R.id.sort_order_top_last_hour, R.string.time_frame_last_hour)
            addItem(
                R.id.sort_order_top_last_six_hour,
                getString(R.string.time_frame_last_hours_format, "6"),
            )
            addItem(
                R.id.sort_order_top_last_twelve_hour,
                getString(R.string.time_frame_last_hours_format, "12"),
            )
            addItem(R.id.sort_order_top_day, R.string.time_frame_today)
            addItem(R.id.sort_order_top_week, R.string.time_frame_this_week)
            addItem(R.id.sort_order_top_month, R.string.time_frame_this_month)
            addItem(
                R.id.sort_order_top_last_three_month,
                getString(R.string.time_frame_last_months_format, "3"),
            )
            addItem(
                R.id.sort_order_top_last_six_month,
                getString(R.string.time_frame_last_months_format, "6"),
            )
            addItem(
                R.id.sort_order_top_last_nine_month,
                getString(R.string.time_frame_last_months_format, "9"),
            )
            addItem(R.id.sort_order_top_year, R.string.time_frame_this_year)
            addItem(R.id.sort_order_top_all_time, R.string.time_frame_all_time)
            setTitle(R.string.sort_by_top)

            setOnMenuItemClickListener { menuItem ->
                idToSortOrder(menuItem.id)?.let {
                    onSortOrderSelected(it)
                }
            }
        }
    }
    fun getSortByTopMenu(): BottomMenu {
        when (val order = getCurrentSortType()) {
            is CommunitySortOrder.TopOrder -> {
                when (order.timeFrame) {
                    CommunitySortOrder.TimeFrame.Today ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_day)
                    CommunitySortOrder.TimeFrame.ThisWeek ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_week)
                    CommunitySortOrder.TimeFrame.ThisMonth ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_month)
                    CommunitySortOrder.TimeFrame.ThisYear ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_year)
                    CommunitySortOrder.TimeFrame.AllTime ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_all_time)
                    CommunitySortOrder.TimeFrame.LastHour ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_hour)
                    CommunitySortOrder.TimeFrame.LastSixHour ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_six_hour)
                    CommunitySortOrder.TimeFrame.LastTwelveHour ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_twelve_hour)
                    CommunitySortOrder.TimeFrame.LastThreeMonth ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_three_month)
                    CommunitySortOrder.TimeFrame.LastSixMonth ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_six_month)
                    CommunitySortOrder.TimeFrame.LastNineMonth ->
                        _sortByTopMenu.setChecked(R.id.sort_order_top_last_nine_month)
                }
            }
            else -> {}
        }

        return _sortByTopMenu
    }

    val _sortByMenu: BottomMenu by lazy {
        BottomMenu(requireContext()).apply {
            addItem(R.id.sort_order_active, R.string.sort_order_active)
            addItem(R.id.sort_order_hot, R.string.sort_order_hot)
            addItem(
                R.id.sort_order_top,
                R.string.sort_order_top,
                R.drawable.baseline_chevron_right_24,
            )
            addItem(R.id.sort_order_new, R.string.sort_order_new)
            addItem(R.id.sort_order_old, R.string.sort_order_old)
            addItem(R.id.sort_order_most_comments, R.string.sort_order_most_comments)
            addItem(R.id.sort_order_new_comments, R.string.sort_order_new_comments)
            addItem(R.id.sort_order_controversial, R.string.sort_order_controversial)
            addItem(R.id.sort_order_scaled, R.string.sort_order_scaled)
            setTitle(R.string.sort_by)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.id) {
                    R.id.sort_order_top ->
                        getMainActivity()?.showBottomMenu(getSortByTopMenu())
                    else ->
                        idToSortOrder(menuItem.id)?.let {
                            onSortOrderSelected(it)
                        }
                }
            }
        }
    }

    fun getSortByMenu(): BottomMenu {
        when (getCurrentSortType()) {
            CommunitySortOrder.Active -> _sortByMenu.setChecked(R.id.sort_order_active)
            CommunitySortOrder.Hot -> _sortByMenu.setChecked(R.id.sort_order_hot)
            CommunitySortOrder.New -> _sortByMenu.setChecked(R.id.sort_order_new)
            is CommunitySortOrder.TopOrder -> _sortByMenu.setChecked(R.id.sort_order_top)
            CommunitySortOrder.MostComments -> _sortByMenu.setChecked(R.id.sort_order_most_comments)
            CommunitySortOrder.NewComments -> _sortByMenu.setChecked(R.id.sort_order_new_comments)
            CommunitySortOrder.Old -> _sortByMenu.setChecked(R.id.sort_order_old)
            CommunitySortOrder.Controversial -> _sortByMenu.setChecked(
                R.id.sort_order_controversial,
            )
            CommunitySortOrder.Scaled -> _sortByMenu.setChecked(R.id.sort_order_scaled)
        }

        return _sortByMenu
    }

    getMainActivity()?.showBottomMenu(getSortByMenu())
}
