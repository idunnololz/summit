package com.idunnololz.summit.api.dto.lemmy

data class FederatedInstances(
  val linked: List<Instance>,
  val allowed: List<Instance>,
  val blocked: List<Instance>,
)
