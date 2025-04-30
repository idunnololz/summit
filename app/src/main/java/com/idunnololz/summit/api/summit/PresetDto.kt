package com.idunnololz.summit.api.summit

import kotlinx.serialization.Serializable

@Serializable
data class PresetDto(
  val id: String? = null,
  val presetName: String,
  val presetDescription: String,
  val presetData: String,
  val createTs: Long,
  val updateTs: Long,
  val isApproved: Boolean? = null,
  val hasPreview: Boolean? = null,
)
