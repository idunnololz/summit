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
 * @param commentId
 * @param removed
 * @param reason
 */

data class RemoveComment(

  @SerializedName("comment_id")
  val commentId: kotlin.Int,

  @SerializedName("removed")
  val removed: kotlin.Boolean,

  @SerializedName("reason")
  val reason: kotlin.String? = null,

)
