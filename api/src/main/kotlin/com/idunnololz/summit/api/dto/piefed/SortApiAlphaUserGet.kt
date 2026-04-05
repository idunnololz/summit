package com.idunnololz.summit.api.dto.piefed

import com.google.gson.annotations.SerializedName

enum class SortApiAlphaUserGet(
  val value: kotlin.String,
) {
  @SerializedName(value = "Active")
  Active("Active"),

  @SerializedName(value = "Hot")
  Hot("Hot"),

  @SerializedName(value = "New")
  New("New"),

  @SerializedName(value = "Top")
  Top("Top"),

  @SerializedName(value = "TopHour")
  TopHour("TopHour"),

  @SerializedName(value = "TopSixHour")
  TopSixHour("TopSixHour"),

  @SerializedName(value = "TopTwelveHour")
  TopTwelveHour("TopTwelveHour"),

  @SerializedName(value = "TopDay")
  TopDay("TopDay"),

  @SerializedName(value = "TopWeek")
  TopWeek("TopWeek"),

  @SerializedName(value = "TopMonth")
  TopMonth("TopMonth"),

  @SerializedName(value = "TopThreeMonths")
  TopThreeMonths("TopThreeMonths"),

  @SerializedName(value = "TopSixMonths")
  TopSixMonths("TopSixMonths"),

  @SerializedName(value = "TopNineMonths")
  TopNineMonths("TopNineMonths"),

  @SerializedName(value = "TopYear")
  TopYear("TopYear"),

  @SerializedName(value = "TopAll")
  TopAll("TopAll"),

  @SerializedName(value = "Scaled")
  Scaled("Scaled"),

  @SerializedName(value = "Old")
  Old("Old"),

  @SerializedName(value = "Relevance")
  Relevance("Relevance"),
  ;

  /**
   * Override [toString()] to avoid using the enum variable name as the value, and instead use
   * the actual value defined in the API spec file.
   *
   * This solves a problem when the variable name and its value are different, and ensures that
   * the client sends the correct enum values to the server always.
   */
  override fun toString(): kotlin.String = "$value"
}
