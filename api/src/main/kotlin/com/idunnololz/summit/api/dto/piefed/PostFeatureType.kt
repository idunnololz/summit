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
 * Values: Local,Community
 */

enum class PostFeatureType(val value: kotlin.String) {

  @SerializedName(value = "Local")
  Local("Local"),

  @SerializedName(value = "Community")
  Community("Community"),
  ;

  /**
   * Override [toString()] to avoid using the enum variable name as the value, and instead use
   * the actual value defined in the API spec file.
   *
   * This solves a problem when the variable name and its value are different, and ensures that
   * the client sends the correct enum values to the server always.
   */
  override fun toString(): kotlin.String = value

  companion object {
    /**
     * Converts the provided [data] to a [String] on success, null otherwise.
     */
    fun encode(data: kotlin.Any?): kotlin.String? = if (data is PostFeatureType) "$data" else null

    /**
     * Returns a valid [PostFeatureType] for [data], null otherwise.
     */
    fun decode(data: kotlin.Any?): PostFeatureType? = data?.let {
      val normalizedData = "$it".lowercase()
      values().firstOrNull { value ->
        it == value || normalizedData == "$value".lowercase()
      }
    }
  }
}
