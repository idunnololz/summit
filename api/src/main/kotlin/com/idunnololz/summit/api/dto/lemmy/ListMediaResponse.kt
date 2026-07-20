package com.idunnololz.summit.api.dto.lemmy

data class ListMediaResponse(
  val images: List<LocalImageView>,
  val nextPage: String?,
  val prevPage: String?,
)
