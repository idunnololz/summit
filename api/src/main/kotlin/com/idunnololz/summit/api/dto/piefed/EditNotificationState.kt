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
 * @param readState
 */

data class EditNotificationState(

  @SerializedName("notif_id")
  val notifId: kotlin.Int? = null,

  @SerializedName("read_state")
  val readState: kotlin.Boolean? = null,

)
