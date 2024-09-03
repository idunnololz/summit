package com.idunnololz.summit.util.imgur

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadResponse(
    @SerializedName("data")
    val upload: Upload,
    @SerializedName("status")
    val status: Int,
    @SerializedName("success")
    val success: Boolean
)

@JsonClass(generateAdapter = true)
data class Upload(
    @SerializedName("account_id")
    val accountId: Int?,
    @SerializedName("account_url")
    val accountUrl: String?,
    @SerializedName("ad_type")
    val adType: Int?,
    @SerializedName("ad_url")
    val adUrl: String?,
    @SerializedName("animated")
    val animated: Boolean,
    @SerializedName("bandwidth")
    val bandwidth: Int,
    @SerializedName("datetime")
    val datetime: Long,
    @SerializedName("deletehash")
    val deletehash: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("favorite")
    val favorite: Boolean,
    @SerializedName("has_sound")
    val hasSound: Boolean,
    @SerializedName("height")
    val height: Int,
    @SerializedName("hls")
    val hls: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("in_gallery")
    val inGallery: Boolean,
    @SerializedName("in_most_viral")
    val inMostViral: Boolean,
    @SerializedName("is_ad")
    val isAd: Boolean,
    @SerializedName("link")
    val link: String,
    @SerializedName("mp4")
    val mp4: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("size")
    val size: Int,
    @SerializedName("tags")
    val tags: List<String>,
    @SerializedName("title")
    val title: String?,
    @SerializedName("type")
    val type: String,
    @SerializedName("views")
    val views: Int,
    @SerializedName("width")
    val width: Int
)