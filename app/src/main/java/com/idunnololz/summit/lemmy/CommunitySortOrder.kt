package com.idunnololz.summit.lemmy

import com.idunnololz.summit.api.dto.SortType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface CommunitySortOrder {

    @TypeLabel("1")
    object Hot : CommunitySortOrder
    @TypeLabel("2")
    object Active : CommunitySortOrder
    @TypeLabel("3")
    object New : CommunitySortOrder
    @TypeLabel("4")
    object Old : CommunitySortOrder
    @TypeLabel("5")
    object MostComments : CommunitySortOrder
    @TypeLabel("6")
    object NewComments : CommunitySortOrder

    @TypeLabel("7")
    @JsonClass(generateAdapter = true)
    data class TopOrder(
        val timeFrame: TimeFrame = TimeFrame.Today
    ) : CommunitySortOrder

    enum class TimeFrame {
        Today,
        ThisWeek,
        ThisMonth,
        ThisYear,
        AllTime
    }
}

fun CommunitySortOrder.toApiSortOrder(): SortType =
    when (this) {
        CommunitySortOrder.Active -> SortType.Active
        CommunitySortOrder.Hot -> SortType.Hot
        CommunitySortOrder.MostComments -> SortType.MostComments
        CommunitySortOrder.New -> SortType.New
        CommunitySortOrder.NewComments -> SortType.NewComments
        CommunitySortOrder.Old -> SortType.Old
        is CommunitySortOrder.TopOrder -> {
            when (this.timeFrame) {
                CommunitySortOrder.TimeFrame.Today -> SortType.TopDay
                CommunitySortOrder.TimeFrame.ThisWeek -> SortType.TopWeek
                CommunitySortOrder.TimeFrame.ThisMonth -> SortType.TopMonth
                CommunitySortOrder.TimeFrame.ThisYear -> SortType.TopYear
                CommunitySortOrder.TimeFrame.AllTime -> SortType.TopAll
            }
        }
    }