package com.idunnololz.summit.api.dto.lemmy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PersonView(
  val person: Person,
  val counts: PersonAggregates,
) : Parcelable
