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
 * @param id
 * @param userId
 * @param postId
 * @param body
 * @param removed
 * @param published
 * @param deleted
 * @param apId
 * @param local
 * @param path
 * @param languageId
 * @param updated
 * @param distinguished
 */

data class Comment(

  @SerializedName("id")
  val id: kotlin.Int,

  @SerializedName("user_id")
  val userId: kotlin.Int,

  @SerializedName("post_id")
  val postId: kotlin.Int,

  @SerializedName("body")
  val body: kotlin.String,

  @SerializedName("removed")
  val removed: kotlin.Boolean,

  @SerializedName("published")
  val published: String,

  @SerializedName("deleted")
  val deleted: kotlin.Boolean,

  @SerializedName("ap_id")
  val apId: kotlin.String,

  @SerializedName("local")
  val local: kotlin.Boolean,

  @SerializedName("path")
  val path: kotlin.String,

  @SerializedName("language_id")
  val languageId: kotlin.Int,

  @SerializedName("updated")
  val updated: String? = null,

  @SerializedName("edited_at")
  val editedAt: String? = null,

  @SerializedName("distinguished")
  val distinguished: kotlin.Boolean? = null,

)
