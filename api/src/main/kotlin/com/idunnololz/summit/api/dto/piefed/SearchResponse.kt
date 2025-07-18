/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
  "ArrayInDataClass",
  "EnumEntryName",
  "RemoveRedundantQualifierName",
  "UnusedImport",
)

package com.idunnololz.summit.api.dto.piefed

import com.google.gson.annotations.SerializedName

/**
 *
 *
 * @param type
 * @param posts
 * @param communities
 * @param users
 */

data class SearchResponse(

  @SerializedName("type_")
  val type: SearchType?,

  @SerializedName("posts")
  val posts: kotlin.collections.List<PostView>,

  @SerializedName("communities")
  val communities: kotlin.collections.List<CommunityView>,

  @SerializedName("users")
  val users: kotlin.collections.List<PersonView>,

)
