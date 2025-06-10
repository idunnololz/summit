package com.idunnololz.summit.lemmy.modlogs

import android.os.Parcelable
import com.idunnololz.summit.api.dto.ModlogActionType
import com.idunnololz.summit.lemmy.PersonRef
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModLogsFilterConfig(
  val filterByActionType: ModlogActionType = ModlogActionType.All,
  val filterByMod: PersonRef.PersonRefComplete? = null,
  val filterByPerson: PersonRef.PersonRefComplete? = null,
): Parcelable {
  fun isFilterDefault() =
    filterByActionType == ModlogActionType.All &&
      filterByMod == null &&
      filterByPerson == null
}