package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.idunnololz.summit.R
import com.idunnololz.summit.api.dto.SortType
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface CommunitySortOrder : Parcelable {

    @Parcelize
    @TypeLabel("1")
    data object Hot : CommunitySortOrder

    @Parcelize
    @TypeLabel("2")
    data object Active : CommunitySortOrder

    @Parcelize
    @TypeLabel("3")
    data object New : CommunitySortOrder

    @Parcelize
    @TypeLabel("4")
    data object Old : CommunitySortOrder

    @Parcelize
    @TypeLabel("5")
    data object MostComments : CommunitySortOrder

    @Parcelize
    @TypeLabel("6")
    data object NewComments : CommunitySortOrder

    @Parcelize
    @TypeLabel("7")
    @JsonClass(generateAdapter = true)
    data class TopOrder(
        val timeFrame: TimeFrame = TimeFrame.Today,
    ) : CommunitySortOrder

    @Parcelize
    @TypeLabel("8")
    data object Controversial : CommunitySortOrder

    @Parcelize
    @TypeLabel("9")
    data object Scaled : CommunitySortOrder

    enum class TimeFrame {
        LastHour,
        LastSixHour,
        LastTwelveHour,
        Today,
        ThisWeek,
        ThisMonth,
        LastThreeMonth,
        LastSixMonth,
        LastNineMonth,
        ThisYear,
        AllTime,
    }
}

val DefaultSortOrder = CommunitySortOrder.Active

fun idToSortOrder(id: Int) = when (id) {
    R.id.sort_order_active ->
        CommunitySortOrder.Active
    R.id.sort_order_hot ->
        CommunitySortOrder.Hot
    R.id.sort_order_new ->
        CommunitySortOrder.New
    R.id.sort_order_old ->
        CommunitySortOrder.Old
    R.id.sort_order_most_comments ->
        CommunitySortOrder.MostComments
    R.id.sort_order_new_comments ->
        CommunitySortOrder.NewComments
    R.id.sort_order_top_last_hour ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastHour)
    R.id.sort_order_top_last_six_hour ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastSixHour)
    R.id.sort_order_top_last_twelve_hour ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastTwelveHour)
    R.id.sort_order_top_day ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.Today)
    R.id.sort_order_top_week ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisWeek)
    R.id.sort_order_top_month ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisMonth)
    R.id.sort_order_top_last_three_month ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastThreeMonth)
    R.id.sort_order_top_last_six_month ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastSixMonth)
    R.id.sort_order_top_last_nine_month ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastNineMonth)
    R.id.sort_order_top_year ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisYear)
    R.id.sort_order_top_all_time ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.AllTime)
    R.id.sort_order_controversial ->
        CommunitySortOrder.Controversial
    R.id.sort_order_scaled ->
        CommunitySortOrder.Scaled
    else -> null
}

fun CommunitySortOrder.toApiSortOrder(): SortType = when (this) {
    CommunitySortOrder.Active -> SortType.Active
    CommunitySortOrder.Hot -> SortType.Hot
    CommunitySortOrder.MostComments -> SortType.MostComments
    CommunitySortOrder.New -> SortType.New
    CommunitySortOrder.NewComments -> SortType.NewComments
    CommunitySortOrder.Old -> SortType.Old
    is CommunitySortOrder.TopOrder -> {
        when (this.timeFrame) {
            CommunitySortOrder.TimeFrame.LastHour -> SortType.TopHour
            CommunitySortOrder.TimeFrame.LastSixHour -> SortType.TopSixHour
            CommunitySortOrder.TimeFrame.LastTwelveHour -> SortType.TopTwelveHour
            CommunitySortOrder.TimeFrame.Today -> SortType.TopDay
            CommunitySortOrder.TimeFrame.ThisWeek -> SortType.TopWeek
            CommunitySortOrder.TimeFrame.ThisMonth -> SortType.TopMonth
            CommunitySortOrder.TimeFrame.LastThreeMonth -> SortType.TopThreeMonths
            CommunitySortOrder.TimeFrame.LastSixMonth -> SortType.TopSixMonths
            CommunitySortOrder.TimeFrame.LastNineMonth -> SortType.TopNineMonths
            CommunitySortOrder.TimeFrame.ThisYear -> SortType.TopYear
            CommunitySortOrder.TimeFrame.AllTime -> SortType.TopAll
        }
    }
    CommunitySortOrder.Controversial -> SortType.Controversial
    CommunitySortOrder.Scaled -> SortType.Scaled
}

fun SortType.toId() = when (this) {
    SortType.Active -> R.id.sort_order_active
    SortType.Hot -> R.id.sort_order_hot
    SortType.New -> R.id.sort_order_new
    SortType.Old -> R.id.sort_order_old
    SortType.TopDay -> R.id.sort_order_top_day
    SortType.TopWeek -> R.id.sort_order_top_week
    SortType.TopMonth -> R.id.sort_order_top_month
    SortType.TopYear -> R.id.sort_order_top_year
    SortType.TopAll -> R.id.sort_order_top_all_time
    SortType.MostComments -> R.id.sort_order_most_comments
    SortType.NewComments -> R.id.sort_order_new_comments
    SortType.TopHour -> R.id.sort_order_top_last_hour
    SortType.TopSixHour -> R.id.sort_order_top_last_six_hour
    SortType.TopTwelveHour -> R.id.sort_order_top_last_twelve_hour
    SortType.TopThreeMonths -> R.id.sort_order_top_last_three_month
    SortType.TopSixMonths -> R.id.sort_order_top_last_six_month
    SortType.TopNineMonths -> R.id.sort_order_top_last_nine_month
    SortType.Controversial -> R.id.sort_order_controversial
    SortType.Scaled -> R.id.sort_order_scaled
}

fun SortType.toSortOrder(): CommunitySortOrder = when (this) {
    SortType.Active -> CommunitySortOrder.Active
    SortType.Hot -> CommunitySortOrder.Hot
    SortType.New -> CommunitySortOrder.New
    SortType.Old -> CommunitySortOrder.Old
    SortType.MostComments -> CommunitySortOrder.MostComments
    SortType.NewComments -> CommunitySortOrder.NewComments
    SortType.TopDay -> CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.Today)
    SortType.TopWeek -> CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisWeek)
    SortType.TopMonth -> CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisMonth)
    SortType.TopYear -> CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.ThisYear)
    SortType.TopAll -> CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.AllTime)
    SortType.TopHour -> CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastHour)
    SortType.TopSixHour ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastSixHour)
    SortType.TopTwelveHour ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastTwelveHour)
    SortType.TopThreeMonths ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastThreeMonth)
    SortType.TopSixMonths ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastSixMonth)
    SortType.TopNineMonths ->
        CommunitySortOrder.TopOrder(CommunitySortOrder.TimeFrame.LastNineMonth)
    SortType.Controversial -> CommunitySortOrder.Controversial
    SortType.Scaled -> CommunitySortOrder.Scaled
}
