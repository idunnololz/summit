package com.idunnololz.summit.api

interface ApiCompat {
  fun supportsFeature(apiFeature: ApiFeature): Boolean
}

/**
 * Refers to actions that can be performed by API. This is different than what is supported by the
 * site. Eg. PieFed, the website, supports user registrations but the API does not.
 */
enum class ApiFeature {
  // Eg. user/post reports
  Reports,
  Register,
  // Whether the server supports listing all posts/comments downvoted by this user.
  Downvoted,
}