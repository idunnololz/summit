package com.idunnololz.summit.api.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PersonAggregates(
    val id: Int,
    val person_id: PersonId,
    val post_count: Int,
    val post_score: Int,
    val comment_count: Int,
    val comment_score: Int,
): Parcelable
