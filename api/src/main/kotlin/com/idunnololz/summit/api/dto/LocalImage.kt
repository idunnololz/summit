package com.idunnololz.summit.api.dto

data class LocalImage(
  val local_user_id: LocalUserId? = null,
  val pictrs_alias: String,
  val pictrs_delete_token: String,
  val published: String,
)
