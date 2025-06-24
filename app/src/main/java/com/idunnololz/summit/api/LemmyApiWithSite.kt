package com.idunnololz.summit.api

class LemmyApiWithSite(
  private val lemmyApi: LemmyApi,
  val instance: String,
) : LemmyApi by lemmyApi