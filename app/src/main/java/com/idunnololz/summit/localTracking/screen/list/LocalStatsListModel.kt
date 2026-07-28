package com.idunnololz.summit.localTracking.screen.list

import com.idunnololz.summit.api.dto.lemmy.Person
import com.idunnololz.summit.lemmy.CommunityRef
import com.idunnololz.summit.lemmy.PersonRef

data class LocalStatsListModel(
  val items: List<Item>
) {

  sealed interface Item {
    class CommunityStatItem(
      val communityRef: CommunityRef?,
      val count: Int,
    ): Item

    data class PersonStatItem(
      val personId: Long?,
      val personResult: Result<Person>?,
      val count: Int,
    ): Item
  }
}