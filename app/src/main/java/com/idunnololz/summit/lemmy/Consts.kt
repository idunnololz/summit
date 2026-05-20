package com.idunnololz.summit.lemmy

import com.idunnololz.summit.util.consts.LEMMY_INSTANCES

object Consts {
  const val INSTANCE_LEMMY_WORLD = "lemmy.world"

  const val DEFAULT_INSTANCE = INSTANCE_LEMMY_WORLD

  val DEFAULT_LEMMY_INSTANCES =
    (LEMMY_INSTANCES + listOf(
    INSTANCE_LEMMY_WORLD,

    // piefed
    "piefed.social",
    "piefed.world",
    "piefed.blahaj.zone",
    "piefed.ca",
    "piefed.zip",
    "feddit.online",
    "fedinsfw.app",
  )).toSet().toList()
}
