package com.idunnololz.summit.util.imgur

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    @SerialName("data")
    val upload: Upload,
    @SerialName("status")
    val status: Int,
    @SerialName("success")
    val success: Boolean,
)

@Serializable
data class Upload(
    @SerialName("account_id")
    val accountId: Int?,
    @SerialName("account_url")
    val accountUrl: String?,
    @SerialName("ad_type")
    val adType: Int?,
    @SerialName("ad_url")
    val adUrl: String?,
    @SerialName("animated")
    val animated: Boolean,
    @SerialName("bandwidth")
    val bandwidth: Int,
    @SerialName("datetime")
    val datetime: Long,
    @SerialName("deletehash")
    val deletehash: String,
    @SerialName("description")
    val description: String?,
    @SerialName("favorite")
    val favorite: Boolean,
    @SerialName("has_sound")
    val hasSound: Boolean,
    @SerialName("height")
    val height: Int,
    @SerialName("hls")
    val hls: String,
    @SerialName("id")
    val id: String,
    @SerialName("in_gallery")
    val inGallery: Boolean,
    @SerialName("in_most_viral")
    val inMostViral: Boolean,
    @SerialName("is_ad")
    val isAd: Boolean,
    @SerialName("link")
    val link: String?,
    @SerialName("mp4")
    val mp4: String,
    @SerialName("name")
    val name: String,
    @SerialName("size")
    val size: Int,
    @SerialName("tags")
    val tags: List<String>,
    @SerialName("title")
    val title: String?,
    @SerialName("type")
    val type: String,
    @SerialName("views")
    val views: Int,
    @SerialName("width")
    val width: Int,
)
