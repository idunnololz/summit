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
 * @param linked
 * @param allowed
 * @param blocked
 */

data class FederatedInstancesView(

  @SerializedName("linked")
  val linked: kotlin.collections.List<InstanceWithoutFederationState>,

  @SerializedName("allowed")
  val allowed: kotlin.collections.List<InstanceWithoutFederationState>,

  @SerializedName("blocked")
  val blocked: kotlin.collections.List<InstanceWithoutFederationState>,

)
