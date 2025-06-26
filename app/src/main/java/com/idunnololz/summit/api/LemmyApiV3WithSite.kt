package com.idunnololz.summit.api

class LemmyApiV3WithSite(
  private val lemmyApiV3: LemmyApiV3,
  val instance: String,
) : LemmyApiV3 by lemmyApiV3