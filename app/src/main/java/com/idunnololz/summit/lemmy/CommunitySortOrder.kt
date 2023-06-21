package com.idunnololz.summit.lemmy

import com.idunnololz.summit.api.dto.SortType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface CommunitySortOrder {

//    companion object {
//        fun adapter(): PolymorphicJsonAdapterFactory<CommunitySortOrder> =
//            PolymorphicJsonAdapterFactory.of(CommunitySortOrder::class.java, "t")
//                .withSubtype(Hot::class.java, "1")
//                .withSubtype(Active::class.java, "2")
//                .withSubtype(New::class.java, "3")
//                .withSubtype(Old::class.java, "4")
//                .withSubtype(MostComments::class.java, "5")
//                .withSubtype(NewComments::class.java, "6")
//                .withSubtype(TopOrder::class.java, "7")
//    }

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