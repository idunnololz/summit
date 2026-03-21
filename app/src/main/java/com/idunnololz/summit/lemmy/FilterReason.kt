package com.idunnololz.summit.lemmy

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface FilterReason : Parcelable {

  @Parcelize
  data object Nsfw : FilterReason

  @Parcelize
  data object Link : FilterReason

  @Parcelize
  data object Image : FilterReason

  @Parcelize
  data object Video : FilterReason

  @Parcelize
  data object Text : FilterReason

  @Parcelize
  data object Custom : FilterReason

  @Parcelize
  data object Duplicate : FilterReason
}
