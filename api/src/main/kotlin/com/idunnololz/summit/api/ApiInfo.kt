package com.idunnololz.summit.api

data class ApiInfo(
  val backendType: ApiType?,
  val downvoteAllowed: Boolean,
) {

  fun supportsFeature(apiFeature: ApiFeature): Boolean =
    when (backendType) {
      ApiType.LemmyV3 ->
        when (apiFeature) {
          ApiFeature.Reports -> true
          ApiFeature.Register -> true
          ApiFeature.Downvoted -> true
          ApiFeature.UploadsList -> true
          ApiFeature.Downvote -> downvoteAllowed
          ApiFeature.SearchAll -> true
          ApiFeature.SearchComments -> true
        }
      ApiType.PieFedAlpha ->
        when (apiFeature) {
          ApiFeature.Reports -> false
          ApiFeature.Register -> false
          ApiFeature.Downvoted -> false
          ApiFeature.UploadsList -> false
          ApiFeature.Downvote -> downvoteAllowed
          ApiFeature.SearchAll -> false
          ApiFeature.SearchComments -> false
        }
      null -> false
    }
}