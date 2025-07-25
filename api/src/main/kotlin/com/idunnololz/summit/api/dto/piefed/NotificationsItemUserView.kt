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
 * @param notifId
 * @param notifType
 * @param notifSubtype
 * @param author
 * @param post
 * @param postId
 * @param notifBody
 */

data class NotificationsItemUserView(

  @SerializedName("notif_id")
  val notifId: kotlin.Int? = null,

  @SerializedName("notif_type")
  val notifType: kotlin.Int? = null,

  @SerializedName("notif_subtype")
  val notifSubtype: kotlin.String? = null,

  @SerializedName("author")
  val author: Person? = null,

  @SerializedName("post")
  val post: PostView? = null,

  @SerializedName("post_id")
  val postId: kotlin.Int? = null,

  @SerializedName("notif_body")
  val notifBody: kotlin.String? = null,

)
