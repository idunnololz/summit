package com.idunnololz.summit.lemmy.utils

import android.content.Context
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.idToSortOrder
import com.idunnololz.summit.util.BaseFragment
import com.idunnololz.summit.util.BottomMenu

class SortTypeMenuHelper(
    val context: Context,
    val fragment: BaseFragment<*>,
    val getCurrentSortType: () -> SortType,
    val onSortTypeSelected: (SortType) -> Unit,
) {

    private val _sortByMenu: BottomMenu by lazy {
        BottomMenu(context).apply {
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
                        fragment.getMainActivity()?.showBottomMenu(getSortByTopMenu())
                    else ->
                        idToSortOrder(menuItem.id)?.let {
                            onSortTypeSelected(it)
                        }
                }
            }
        }
    }
    private val _sortByTopMenu: BottomMenu by lazy {
        BottomMenu(context).apply {
            addItem(R.id.sort_order_top_last_hour, R.string.time_frame_last_hour)
            addItem(
                R.id.sort_order_top_last_six_hour,
                context.getString(R.string.time_frame_last_hours_format, "6"),
            )
            addItem(
                R.id.sort_order_top_last_twelve_hour,
                context.getString(R.string.time_frame_last_hours_format, "12"),
            )
            addItem(R.id.sort_order_top_day, R.string.time_frame_today)
            addItem(R.id.sort_order_top_week, R.string.time_frame_this_week)
            addItem(R.id.sort_order_top_month, R.string.time_frame_this_month)
            addItem(
                R.id.sort_order_top_last_three_month,
                context.getString(R.string.time_frame_last_months_format, "3"),
            )
            addItem(
                R.id.sort_order_top_last_six_month,
                context.getString(R.string.time_frame_last_months_format, "6"),
            )
            addItem(
                R.id.sort_order_top_last_nine_month,
                context.getString(R.string.time_frame_last_months_format, "9"),
            )
            addItem(R.id.sort_order_top_year, R.string.time_frame_this_year)
            addItem(R.id.sort_order_top_all_time, R.string.time_frame_all_time)
            setTitle(R.string.sort_by_top)

            setOnMenuItemClickListener { menuItem ->
                idToSortOrder(menuItem.id)?.let {
                    onSortTypeSelected(it)
                }
            }
        }
    }

    private fun getSortByMenu(): BottomMenu {
        when (getCurrentSortType()) {
            SortType.Active -> _sortByMenu.setChecked(R.id.sort_order_active)
            SortType.Hot -> _sortByMenu.setChecked(R.id.sort_order_hot)
            SortType.New -> _sortByMenu.setChecked(R.id.sort_order_new)
            SortType.Old -> _sortByMenu.setChecked(R.id.sort_order_old)
            SortType.MostComments -> _sortByMenu.setChecked(R.id.sort_order_most_comments)
            SortType.NewComments -> _sortByMenu.setChecked(R.id.sort_order_new_comments)
            SortType.Controversial -> _sortByMenu.setChecked(R.id.sort_order_controversial)
            SortType.Scaled -> _sortByMenu.setChecked(R.id.sort_order_scaled)

            SortType.TopDay,
            SortType.TopWeek,
            SortType.TopMonth,
            SortType.TopYear,
            SortType.TopAll,
            SortType.TopHour,
            SortType.TopSixHour,
            SortType.TopTwelveHour,
            SortType.TopThreeMonths,
            SortType.TopSixMonths,
            SortType.TopNineMonths,
            -> _sortByMenu.setChecked(R.id.sort_order_top)
        }

        return _sortByMenu
    }
    private fun getSortByTopMenu(): BottomMenu {
        when (getCurrentSortType()) {
            SortType.Active,
            SortType.Hot,
            SortType.New,
            SortType.Old,
            SortType.MostComments,
            SortType.NewComments,
            SortType.Controversial,
            SortType.Scaled,
            -> { /* do nothing */ }

            SortType.TopDay ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_day)
            SortType.TopWeek ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_week)
            SortType.TopMonth ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_month)
            SortType.TopYear ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_year)
            SortType.TopAll ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_all_time)
            SortType.TopHour ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_last_hour)
            SortType.TopSixHour ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_last_six_hour)
            SortType.TopTwelveHour ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_last_twelve_hour)
            SortType.TopThreeMonths ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_last_three_month)
            SortType.TopSixMonths ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_last_six_month)
            SortType.TopNineMonths ->
                _sortByTopMenu.setChecked(R.id.sort_order_top_last_nine_month)
        }

        return _sortByTopMenu
    }

    private fun idToSortOrder(id: Int): SortType? = when (id) {
        R.id.sort_order_active ->
            SortType.Active
        R.id.sort_order_hot ->
            SortType.Hot
        R.id.sort_order_new ->
            SortType.New
        R.id.sort_order_old ->
            SortType.Old
        R.id.sort_order_most_comments ->
            SortType.MostComments
        R.id.sort_order_new_comments ->
            SortType.NewComments
        R.id.sort_order_top_last_hour ->
            SortType.TopHour
        R.id.sort_order_top_last_six_hour ->
            SortType.TopSixHour
        R.id.sort_order_top_last_twelve_hour ->
            SortType.TopTwelveHour
        R.id.sort_order_top_day ->
            SortType.TopDay
        R.id.sort_order_top_week ->
            SortType.TopWeek
        R.id.sort_order_top_month ->
            SortType.TopMonth
        R.id.sort_order_top_last_three_month ->
            SortType.TopThreeMonths
        R.id.sort_order_top_last_six_month ->
            SortType.TopSixMonths
        R.id.sort_order_top_last_nine_month ->
            SortType.TopNineMonths
        R.id.sort_order_top_year ->
            SortType.TopYear
        R.id.sort_order_top_all_time ->
            SortType.TopAll
        R.id.sort_order_controversial ->
            SortType.Controversial
        R.id.sort_order_scaled ->
            SortType.Scaled
        else -> null
    }

    fun show() {
        fragment.getMainActivity()?.showBottomMenu(getSortByMenu())
    }
}
