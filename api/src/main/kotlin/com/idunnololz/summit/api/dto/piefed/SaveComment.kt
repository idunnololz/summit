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
 * @param save
 */

data class SaveComment(

  @SerializedName("comment_id")
  val commentId: kotlin.Int,

  @SerializedName("save")
  val save: kotlin.Boolean,

)
