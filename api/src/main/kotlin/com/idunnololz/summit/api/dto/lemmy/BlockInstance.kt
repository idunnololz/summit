package com.idunnololz.summit.api.dto.lemmy

data class BlockInstance(
  val instance_id: InstanceId,
  val block: Boolean,
  val auth: String,
)
