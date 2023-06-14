package com.idunnololz.summit.reddit_objects

import android.os.Parcel
import android.os.Parcelable
import com.idunnololz.summit.util.Utils
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

class ListingObject(
    kind: String = "",
    val data: ListingData? = null
) : RedditObject(kind)

class ListingData(
    val modHash: String,
    val dist: Int,
    val children: List<RedditObject>,
    val before: String? = null,
    val after: String? = null
) {
    inline fun <reified T> getChildrenAs(): List<T> = children.filterIsInstance<T>()
}

class ListingItemObject(
    val data: ListingItem?
) : RedditObject(kind = "t3")

@Parcelize
class ListingItem(
    val approvedAtUtc: String?,
    val subreddit: String,
    val selftext: String,
    val authorFullname: String,
    val saved: Boolean,
    val modReasonTitle: String?,
    val gilded: Int,
    val clicked: Boolean,
    val title: String,
    val subredditNamePrefixed: String,
    val hidden: Boolean,
    val pwls: Int,
    val linkFlairCssClass: String?,
    val downs: Int,
    val thumbnailHeight: Int?,
    val hideScore: Boolean,
    val name: String,
    val mediaMetadata: Map<String, MediaMetadata>?,
    val galleryData: GalleryData?,
    val quarantine: Boolean,
    val linkFlairTextColor: String,
    val authorFlairBackgroundColor: String,
    val subredditType: String,
    val ups: Int,
    val totalAwardsReceived: Int,
    val mediaEmbed: MediaEmbedInfo,
    val thumbnailWidth: Int?,
    val authorFlairTemplateId: String?,
    val isOriginalContent: Boolean,
    val userReports: List<String>,
    val secureMedia: @RawValue Any?,
    val isRedditMediaDomain: Boolean,
    val isMeta: Boolean,
    val category: String?,
    val secureMediaEmbed: SecureMediaEmbedInfo?,
    val linkFlairText: String?,
    val canModPost: Boolean,
    val score: Int,
    val approvedBy: String?,
    val authorPremium: Boolean,
    val thumbnail: String?,
    val edited: @RawValue Any, // Can be false or Double
    val authorFlairCssClass: String,
    val authorFlairRichtext: @RawValue Any?,
    val gildings: GildingInfo,
    val contentCategories: List<String>?,
    val isSelf: Boolean,
    val modNote: String?,
    val created: Double,
    val linkFlairType: String,
    val wls: Int,
    val removedByCategory: String?,
    val bannedBy: String?,
    val authorFlairType: String?,
    val domain: String,
    val allowLiveComments: String,
    val selftextHtml: String?,
    var likes: Boolean?,
    val suggestedSort: String,
    val bannedAtUtc: String?,
    val viewCount: String?,
    val archived: Boolean,
    val noFollow: Boolean,
    val isCrosspostable: Boolean,
    val pinned: Boolean,
    val over_18: Boolean,
    val allAwardings: List<AwardInfo>,
    val awarders: List<String>,
    val mediaOnly: Boolean,
    val canGild: Boolean,
    val spoiler: Boolean,
    val locked: Boolean,
    val authorFlairText: String,
    val visited: Boolean,
    val removedBy: String?,
    val numReports: Int?,
    val distinguished: String?,
    val subredditId: String,
    val modReasonBy: String?,
    val removalReason: String?,
    val linkFlairBackgroundColor: String,
    val id: String,
    val isRobotIndexable: Boolean,
    val reportReasons: List<UnknownObject>?,
    val author: String,
    val discussionType: String?,
    val numComments: Int,
    val sendReplies: Boolean,
    val whitelistStatus: String,
    val contestMode: Boolean,
    val modReports: List<String>,
    val authorPatreonFlair: Boolean,
    val authorFlairTextColor: String,
    val permalink: String,
    val parentWhitelistStatus: String,
    val stickied: Boolean,
    val url: String,
    val subredditSubscribers: Int,
    val createdUtc: Double,
    val numCrossposts: Int,
    val media: MediaInfo?,
    val isVideo: Boolean,
    val preview: AllPreviewInfo?,
    val crosspostParentList: List<ListingItem>?,
    val crosspostParent: String?
) : Parcelable {

    fun shouldHideItem(): Boolean = spoiler || over_18

    fun getThumbnailPreviewInfo(): PreviewInfo? = if (thumbnail != null &&
        thumbnailHeight != null &&
        thumbnailWidth != null
    ) {
        PreviewInfo(thumbnail, thumbnailWidth, thumbnailHeight)
    } else null

    fun getPreviewInfo(): PreviewInfo? = preview?.images?.first()?.source

    fun getLowestResHiddenPreviewInfo(): PreviewInfo? = preview?.images?.first()?.variants?.let {
        it.nsfw?.resolutions?.first() ?: it.obfuscated?.resolutions?.first()
    }

    fun getThumbnailUrl(reveal: Boolean): String? = if (thumbnail == "image") {
        preview?.images?.first()?.source?.getUrl()
    } else if (shouldHideItem()) {
        if (reveal) {
            preview?.images?.first()?.source?.getUrl()
        } else {
            getLowestResHiddenPreviewInfo()?.getUrl()
        }
    } else {
        thumbnail
    }
}

class AllPreviewInfo(
    val images: List<FullImagePreviewInfo>?
) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.createTypedArrayList(FullImagePreviewInfo)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(images)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AllPreviewInfo> {
        override fun createFromParcel(parcel: Parcel): AllPreviewInfo {
            return AllPreviewInfo(parcel)
        }

        override fun newArray(size: Int): Array<AllPreviewInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class FullImagePreviewInfo(
    val source: PreviewInfo,
    val resolutions: List<PreviewInfo>,
    val variants: ImageVariantsPreviewInfo,
    val id: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(PreviewInfo::class.java.classLoader)!!,
        parcel.createTypedArrayList(PreviewInfo)!!,
        parcel.readParcelable(ImageVariantsPreviewInfo::class.java.classLoader)!!,
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(source, flags)
        parcel.writeTypedList(resolutions)
        parcel.writeParcelable(variants, flags)
        parcel.writeString(id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FullImagePreviewInfo> {
        override fun createFromParcel(parcel: Parcel): FullImagePreviewInfo {
            return FullImagePreviewInfo(parcel)
        }

        override fun newArray(size: Int): Array<FullImagePreviewInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class ImageVariantsPreviewInfo(
    val obfuscated: ImagePreviewInfo?,
    val nsfw: ImagePreviewInfo?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(ImagePreviewInfo::class.java.classLoader),
        parcel.readParcelable(ImagePreviewInfo::class.java.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(obfuscated, flags)
        parcel.writeParcelable(nsfw, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ImageVariantsPreviewInfo> {
        override fun createFromParcel(parcel: Parcel): ImageVariantsPreviewInfo {
            return ImageVariantsPreviewInfo(parcel)
        }

        override fun newArray(size: Int): Array<ImageVariantsPreviewInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class ImagePreviewInfo(
    val source: PreviewInfo,
    val resolutions: List<PreviewInfo>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(PreviewInfo::class.java.classLoader)!!,
        parcel.createTypedArrayList(PreviewInfo)!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(source, flags)
        parcel.writeTypedList(resolutions)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ImagePreviewInfo> {
        override fun createFromParcel(parcel: Parcel): ImagePreviewInfo {
            return ImagePreviewInfo(parcel)
        }

        override fun newArray(size: Int): Array<ImagePreviewInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class PreviewInfo(
    private val url: String,
    val width: Int,
    val height: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    fun getUrl(): String = Utils.fromHtml(url).toString()
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeInt(width)
        parcel.writeInt(height)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PreviewInfo> {
        override fun createFromParcel(parcel: Parcel): PreviewInfo {
            return PreviewInfo(parcel)
        }

        override fun newArray(size: Int): Array<PreviewInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class MediaInfo(
    val redditVideo: VideoInfo?,
    val oembed: OembedInfo?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(VideoInfo::class.java.classLoader),
        parcel.readParcelable(OembedInfo::class.java.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(redditVideo, flags)
        parcel.writeParcelable(oembed, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaInfo> {
        override fun createFromParcel(parcel: Parcel): MediaInfo {
            return MediaInfo(parcel)
        }

        override fun newArray(size: Int): Array<MediaInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class OembedInfo(
    val providerUrl: String,
    val description: String,
    val title: String,
    val thumbnailWidth: Int,
    val width: Int,
    val height: Int,
    val html: String,
    val version: String,
    val providerName: String,
    val thumbnailUrl: String,
    val type: String,
    val thumbnailHeight: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(providerUrl)
        parcel.writeString(description)
        parcel.writeString(title)
        parcel.writeInt(thumbnailWidth)
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeString(html)
        parcel.writeString(version)
        parcel.writeString(providerName)
        parcel.writeString(thumbnailUrl)
        parcel.writeString(type)
        parcel.writeInt(thumbnailHeight)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OembedInfo> {
        override fun createFromParcel(parcel: Parcel): OembedInfo {
            return OembedInfo(parcel)
        }

        override fun newArray(size: Int): Array<OembedInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class VideoInfo(
    val fallbackurl: String,
    val height: Int,
    val width: Int,
    val scrubberMediaUrl: String,
    val dashUrl: String,
    val duration: Int,
    val hlsUrl: String,
    val isGif: Boolean,
    val transcodingStatus: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(fallbackurl)
        parcel.writeInt(height)
        parcel.writeInt(width)
        parcel.writeString(scrubberMediaUrl)
        parcel.writeString(dashUrl)
        parcel.writeInt(duration)
        parcel.writeString(hlsUrl)
        parcel.writeByte(if (isGif) 1 else 0)
        parcel.writeString(transcodingStatus)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VideoInfo> {
        override fun createFromParcel(parcel: Parcel): VideoInfo {
            return VideoInfo(parcel)
        }

        override fun newArray(size: Int): Array<VideoInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class AwardInfo(
    val count: Int,
    val isEnabled: Boolean,
    val subredditId: String,
    val description: String,
    val endDate: String?,
    val awardSubType: String,
    val coinReward: Int,
    val iconUrl: String,
    val daysOfPremium: Int,
    val isNew: Boolean,
    val id: String,
    val iconHeight: Int,
    val resizedIcons: List<IconInfo>,
    val daysOfDripExtension: Int,
    val awardType: String,
    val startDate: String?,
    val coinPrice: Int,
    val iconWidth: Int,
    val subredditCoinReward: Int,
    val name: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.createTypedArrayList(IconInfo) ?: listOf(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(count)
        parcel.writeByte(if (isEnabled) 1 else 0)
        parcel.writeString(subredditId)
        parcel.writeString(description)
        parcel.writeString(endDate)
        parcel.writeString(awardSubType)
        parcel.writeInt(coinReward)
        parcel.writeString(iconUrl)
        parcel.writeInt(daysOfPremium)
        parcel.writeByte(if (isNew) 1 else 0)
        parcel.writeString(id)
        parcel.writeInt(iconHeight)
        parcel.writeTypedList(resizedIcons)
        parcel.writeInt(daysOfDripExtension)
        parcel.writeString(awardType)
        parcel.writeString(startDate)
        parcel.writeInt(coinPrice)
        parcel.writeInt(iconWidth)
        parcel.writeInt(subredditCoinReward)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AwardInfo> {
        override fun createFromParcel(parcel: Parcel): AwardInfo {
            return AwardInfo(parcel)
        }

        override fun newArray(size: Int): Array<AwardInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class IconInfo(
    val url: String,
    val width: Int,
    val height: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeInt(width)
        parcel.writeInt(height)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<IconInfo> {
        override fun createFromParcel(parcel: Parcel): IconInfo {
            return IconInfo(parcel)
        }

        override fun newArray(size: Int): Array<IconInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class MediaEmbedInfo(
    val content: String,
    val width: Int,
    val scrolling: Boolean,
    val height: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(content)
        parcel.writeInt(width)
        parcel.writeByte(if (scrolling) 1 else 0)
        parcel.writeInt(height)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaEmbedInfo> {
        override fun createFromParcel(parcel: Parcel): MediaEmbedInfo {
            return MediaEmbedInfo(parcel)
        }

        override fun newArray(size: Int): Array<MediaEmbedInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class SecureMediaEmbedInfo(
    val content: String,
    val width: Int,
    val scrolling: Boolean,
    val height: Int,
    val mediaDomainUrl: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(content)
        parcel.writeInt(width)
        parcel.writeByte(if (scrolling) 1 else 0)
        parcel.writeInt(height)
        parcel.writeString(mediaDomainUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SecureMediaEmbedInfo> {
        override fun createFromParcel(parcel: Parcel): SecureMediaEmbedInfo {
            return SecureMediaEmbedInfo(parcel)
        }

        override fun newArray(size: Int): Array<SecureMediaEmbedInfo?> {
            return arrayOfNulls(size)
        }
    }
}

class GildingInfo(
    val gid_1: Int,
    val gid_2: Int,
    val gid_3: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(gid_1)
        parcel.writeInt(gid_2)
        parcel.writeInt(gid_3)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GildingInfo> {
        override fun createFromParcel(parcel: Parcel): GildingInfo {
            return GildingInfo(parcel)
        }

        override fun newArray(size: Int): Array<GildingInfo?> {
            return arrayOfNulls(size)
        }
    }
}

@Parcelize
data class GalleryData(
  val items: List<GalleryItem>
): Parcelable

@Parcelize
data class GalleryItem(
    val mediaId: String,
    val id: Long,
): Parcelable

data class MediaMetadata(
    val status: String?, // eg. "valid"
    val e: String?, // eg. "Image"
    val m: String?, //mimetype eg. "image/jpg"
    val p: List<PreviewMetadata>,
    val s: PreviewMetadata?,
    val id: String? // eg. "gil6s4xwi4g51"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(PreviewMetadata) ?: listOf(),
        parcel.readParcelable(PreviewMetadata::class.java.classLoader),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(status)
        parcel.writeString(e)
        parcel.writeString(m)
        parcel.writeTypedList(p)
        parcel.writeParcelable(s, flags)
        parcel.writeString(id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaMetadata> {
        override fun createFromParcel(parcel: Parcel): MediaMetadata {
            return MediaMetadata(parcel)
        }

        override fun newArray(size: Int): Array<MediaMetadata?> {
            return arrayOfNulls(size)
        }
    }
}

data class PreviewMetadata(
    val y: Int?, // eg. 780
    val x: Int?, // eg. 360
    val u: String? // eg."https://preview.redd.it/gil6s4xwi4g51.jpg?width=360&amp;format=pjpg&amp;auto=webp&amp;s=dfce79ae37bd468338653a414e995425ab6c836f"
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString()
    )

    fun getUrl(): String = Utils.fromHtml(u).toString()

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(y)
        parcel.writeValue(x)
        parcel.writeString(u)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PreviewMetadata> {
        override fun createFromParcel(parcel: Parcel): PreviewMetadata {
            return PreviewMetadata(parcel)
        }

        override fun newArray(size: Int): Array<PreviewMetadata?> {
            return arrayOfNulls(size)
        }
    }
}

class UnknownObject() : Parcelable {
    constructor(parcel: Parcel) : this() {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UnknownObject> {
        override fun createFromParcel(parcel: Parcel): UnknownObject {
            return UnknownObject(parcel)
        }

        override fun newArray(size: Int): Array<UnknownObject?> {
            return arrayOfNulls(size)
        }
    }

}

/*
{
    "approved_at_utc": null,
    "subreddit": "nfl",
    "selftext": "[San Francisco 49ers](/r/49ers#away \"lost\") [at](#at) [Kansas City Chiefs](/r/kansascitychiefs#home )\n\n\n[](/# \"GT-PRIMETIME\")\n\n\n----\n\n* Hard Rock Stadium\n* Miami Gardens, Florida\n\n\n\n----\n\n######[](#start-box-score)\n\n\n\n\n\n| | | | | | |\n| :-- | :-- | :-- | :-- | :-- |  :-- |\n|      |**First**|**Second**|**Third**|**Fourth**| **Final** |\n|**49ers**| 3 | 7 | 10 | 0 | 20 |\n|**Chiefs**| 7 | 3 | 0 | 21 | 31 |\n\n\n######[](#end-box-score)\n\n----\n\n* General information\n* \n\n----\n\n| | |\n| :-- | --: |\n| **Coverage** | **Odds** |\n| FOX | Kansas City -1.5 O/U 54.5 |\n\n \n| |\n|:---|\n| **Weather** |\n| [59\u00b0F/Wind 5mph/Clear sky/No precipitation expected](https://www.yr.no/place/United_States/Florida/Miami_Gardens#weather-01n \"Weather forecast from yr.no, delivered by the Norwegian Meteorological Institute and the NRK\") |\n \n\n\n\n----\n\n\n\n* Game Stats\n* \n\n----\n\n| | | | | | |\n| :-- | :-- | :-- | :-- | :-- | :-- |\n| **Passing** |  | **Cmp/Att** | **Yds** | **Tds** | **Ints** |\n| J.Garoppolo | [](/r/49ers) | 20/31 | 219| 1 | 2 |\n| P.Mahomes | [](/r/kansascitychiefs) | 26/42 | 286| 2 | 2 |\n| **Rushing** |  | **Car** | **Yds** | **Lng** | **Tds** |\n| R.Mostert | [](/r/49ers) | 12 | 58| 17 | 1 |\n| D.Samuel | [](/r/49ers) | 3 | 53| 32 | 0 |\n| Dam.Williams | [](/r/kansascitychiefs) | 17 | 104| 38 | 1 |\n| P.Mahomes | [](/r/kansascitychiefs) | 9 | 29| 13 | 1 |\n| **Receiving** |  | **Rec** | **Yds** | **Lng** | **Tds** |\n| K.Bourne | [](/r/49ers) | 2 | 42| 26 | 0 |\n| D.Samuel | [](/r/49ers) | 5 | 39| 16 | 0 |\n| K.Juszczyk | [](/r/49ers) | 3 | 39| 15 | 1 |\n| T.Hill | [](/r/kansascitychiefs) | 9 | 105| 44 | 0 |\n| S.Watkins | [](/r/kansascitychiefs) | 5 | 98| 38 | 0 |\n| T.Kelce | [](/r/kansascitychiefs) | 6 | 43| 11 | 1 |\n\n\n\n----\n\n* Scoring Summary\n*  \n\n----\n\n| | | | |\n| :--: | :--: | :-- | :-- |\n| **Team** | **Q** | **Type** | **Drive** |\n| [*SF*](/r/49ers) | 1 | FG | R.Gould 38 yd. Field Goal Drive: 10 plays, 62 yards in 5:58 |\n| [*KC*](/r/kansascitychiefs) | 1 | TD | P.Mahomes 1 yd. run (H.Butker kick is good) Drive: 15 plays, 75 yards in 7:26 |\n| [*KC*](/r/kansascitychiefs) | 2 | FG | H.Butker 31 yd. Field Goal Drive: 9 plays, 43 yards in 4:36 |\n| [*SF*](/r/49ers) | 2 | TD | K.Juszczyk 15 yd. pass from J.Garoppolo (R.Gould kick is good) Drive: 7 plays, 80 yards in 4:27 |\n| [*SF*](/r/49ers) | 3 | FG | R.Gould 42 yd. Field Goal Drive: 9 plays, 60 yards in 5:31 |\n| [*SF*](/r/49ers) | 3 | TD | R.Mostert 1 yd. run (R.Gould kick is good) Drive: 6 plays, 55 yards in 2:48 |\n| [*KC*](/r/kansascitychiefs) | 4 | TD | T.Kelce 1 yd. pass from P.Mahomes (H.Butker kick is good) Drive: 10 plays, 83 yards in 2:40 |\n| [*KC*](/r/kansascitychiefs) | 4 | TD | Dam.Williams 5 yd. pass from P.Mahomes (H.Butker kick is good) Drive: 7 plays, 65 yards in 2:26 |\n| [*KC*](/r/kansascitychiefs) | 4 | TD | Dam.Williams 38 yd. run (H.Butker kick is good) Drive: 2 plays, 42 yards in 0:13 |\n\n\n----\n\n* Thread Notes\n* [Message The Moderators](http://www.reddit.com/message/compose?to=%2Fr%2Fnfl)\n\n----\n\n| |\n| :-- | \n| Discuss whatever you wish. You can trash talk, but keep it civil. |\n| If you are experiencing problems with comment sorting in the official reddit app, we suggest using a third-party client instead ([Android](/r/Android/comments/7ctdf4/lets_settle_this_randroid_what_is_the_best_reddit/), [iOS](/r/ios/comments/68odw1/what_is_the_best_reddit_app_for_ios/)) |\n| Turning comment sort to ['new'](/r/nfl/comments/ey0e0e/super_bowl_liv_post_game_thread_san_francisco/?sort=new) will help you see the newest comments. |\n| Try [Tab Auto Refresh](https://mybrowseraddon.com/tab-auto-refresh.html) to auto-refresh this tab. |\n| Use [reddit-stream.com](http://reddit-stream.com/comments/ey0e0e) to get an autorefreshing version of this page |\n| Check in on the r/nfl chat: **#reddit-nfl** on FreeNode ([open in browser](http://webchat.freenode.net/?channels=reddit-nfl)). |\n| Show your team affiliation - pick your team's logo in the sidebar. |",
    "author_fullname": "t2_plq2w",
    "saved": false,
    "mod_reason_title": null,
    "gilded": 2,
    "clicked": false,
    "title": "Super Bowl LIV Post Game Thread: San Francisco 49ers (13-3) at Kansas City Chiefs (12-4)",
    "link_flair_richtext": [{
            "e": "text",
            "t": "Post Game Thread"
        }
    ],
    "subreddit_name_prefixed": "r/nfl",
    "hidden": false,
    "pwls": 6,
    "link_flair_css_class": "game-thread",
    "downs": 0,
    "thumbnail_height": null,
    "hide_score": false,
    "name": "t3_ey0e0e",
    "quarantine": false,
    "link_flair_text_color": "dark",
    "author_flair_background_color": "",
    "subreddit_type": "public",
    "ups": 10669,
    "total_awards_received": 34,
    "media_embed": {},
    "thumbnail_width": null,
    "author_flair_template_id": null,
    "is_original_content": false,
    "user_reports": [],
    "secure_media": null,
    "is_reddit_media_domain": false,
    "is_meta": false,
    "category": null,
    "secure_media_embed": {},
    "link_flair_text": "Post Game Thread",
    "can_mod_post": false,
    "score": 10669,
    "approved_by": null,
    "author_premium": true,
    "thumbnail": "self",
    "edited": 1580699588.0,
    "author_flair_css_class": "robot auto-approve",
    "author_flair_richtext": [{
            "e": "text",
            "t": "Game thread bot"
        }
    ],
    "gildings": {
        "gid_1": 7,
        "gid_2": 2,
        "gid_3": 1
    },
    "content_categories": null,
    "is_self": true,
    "mod_note": null,
    "created": 1580728317.0,
    "link_flair_type": "richtext",
    "wls": 6,
    "removed_by_category": null,
    "banned_by": null,
    "author_flair_type": "richtext",
    "domain": "self.nfl",
    "allow_live_comments": true,
    "selftext_html": "&lt;!-- SC_OFF --&gt;&lt;div class=\"md\"&gt;&lt;p&gt;&lt;a href=\"/r/49ers#away\" title=\"lost\"&gt;San Francisco 49ers&lt;/a&gt; &lt;a href=\"#at\"&gt;at&lt;/a&gt; &lt;a href=\"/r/kansascitychiefs#home\"&gt;Kansas City Chiefs&lt;/a&gt;&lt;/p&gt;\n\n&lt;p&gt;&lt;a href=\"/#\" title=\"GT-PRIMETIME\"&gt;&lt;/a&gt;&lt;/p&gt;\n\n&lt;hr/&gt;\n\n&lt;ul&gt;\n&lt;li&gt;Hard Rock Stadium&lt;/li&gt;\n&lt;li&gt;Miami Gardens, Florida&lt;/li&gt;\n&lt;/ul&gt;\n\n&lt;hr/&gt;\n\n&lt;h6&gt;&lt;a href=\"#start-box-score\"&gt;&lt;/a&gt;&lt;/h6&gt;\n\n&lt;table&gt;&lt;thead&gt;\n&lt;tr&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;/tr&gt;\n&lt;/thead&gt;&lt;tbody&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;First&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Second&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Third&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Fourth&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Final&lt;/strong&gt;&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;49ers&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;3&lt;/td&gt;\n&lt;td align=\"left\"&gt;7&lt;/td&gt;\n&lt;td align=\"left\"&gt;10&lt;/td&gt;\n&lt;td align=\"left\"&gt;0&lt;/td&gt;\n&lt;td align=\"left\"&gt;20&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Chiefs&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;7&lt;/td&gt;\n&lt;td align=\"left\"&gt;3&lt;/td&gt;\n&lt;td align=\"left\"&gt;0&lt;/td&gt;\n&lt;td align=\"left\"&gt;21&lt;/td&gt;\n&lt;td align=\"left\"&gt;31&lt;/td&gt;\n&lt;/tr&gt;\n&lt;/tbody&gt;&lt;/table&gt;\n\n&lt;h6&gt;&lt;a href=\"#end-box-score\"&gt;&lt;/a&gt;&lt;/h6&gt;\n\n&lt;hr/&gt;\n\n&lt;ul&gt;\n&lt;li&gt;General information&lt;/li&gt;\n&lt;li&gt;&lt;/li&gt;\n&lt;/ul&gt;\n\n&lt;hr/&gt;\n\n&lt;table&gt;&lt;thead&gt;\n&lt;tr&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"right\"&gt;&lt;/th&gt;\n&lt;/tr&gt;\n&lt;/thead&gt;&lt;tbody&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Coverage&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"right\"&gt;&lt;strong&gt;Odds&lt;/strong&gt;&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;FOX&lt;/td&gt;\n&lt;td align=\"right\"&gt;Kansas City -1.5 O/U 54.5&lt;/td&gt;\n&lt;/tr&gt;\n&lt;/tbody&gt;&lt;/table&gt;\n\n&lt;table&gt;&lt;thead&gt;\n&lt;tr&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;/tr&gt;\n&lt;/thead&gt;&lt;tbody&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Weather&lt;/strong&gt;&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"https://www.yr.no/place/United_States/Florida/Miami_Gardens#weather-01n\" title=\"Weather forecast from yr.no, delivered by the Norwegian Meteorological Institute and the NRK\"&gt;59\u00b0F/Wind 5mph/Clear sky/No precipitation expected&lt;/a&gt;&lt;/td&gt;\n&lt;/tr&gt;\n&lt;/tbody&gt;&lt;/table&gt;\n\n&lt;hr/&gt;\n\n&lt;ul&gt;\n&lt;li&gt;Game Stats&lt;/li&gt;\n&lt;li&gt;&lt;/li&gt;\n&lt;/ul&gt;\n\n&lt;hr/&gt;\n\n&lt;table&gt;&lt;thead&gt;\n&lt;tr&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;/tr&gt;\n&lt;/thead&gt;&lt;tbody&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Passing&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Cmp/Att&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Yds&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Tds&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Ints&lt;/strong&gt;&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;J.Garoppolo&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;20/31&lt;/td&gt;\n&lt;td align=\"left\"&gt;219&lt;/td&gt;\n&lt;td align=\"left\"&gt;1&lt;/td&gt;\n&lt;td align=\"left\"&gt;2&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;P.Mahomes&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;26/42&lt;/td&gt;\n&lt;td align=\"left\"&gt;286&lt;/td&gt;\n&lt;td align=\"left\"&gt;2&lt;/td&gt;\n&lt;td align=\"left\"&gt;2&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Rushing&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Car&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Yds&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Lng&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Tds&lt;/strong&gt;&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;R.Mostert&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;12&lt;/td&gt;\n&lt;td align=\"left\"&gt;58&lt;/td&gt;\n&lt;td align=\"left\"&gt;17&lt;/td&gt;\n&lt;td align=\"left\"&gt;1&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;D.Samuel&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;3&lt;/td&gt;\n&lt;td align=\"left\"&gt;53&lt;/td&gt;\n&lt;td align=\"left\"&gt;32&lt;/td&gt;\n&lt;td align=\"left\"&gt;0&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;Dam.Williams&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;17&lt;/td&gt;\n&lt;td align=\"left\"&gt;104&lt;/td&gt;\n&lt;td align=\"left\"&gt;38&lt;/td&gt;\n&lt;td align=\"left\"&gt;1&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;P.Mahomes&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;9&lt;/td&gt;\n&lt;td align=\"left\"&gt;29&lt;/td&gt;\n&lt;td align=\"left\"&gt;13&lt;/td&gt;\n&lt;td align=\"left\"&gt;1&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Receiving&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Rec&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Yds&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Lng&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Tds&lt;/strong&gt;&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;K.Bourne&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;2&lt;/td&gt;\n&lt;td align=\"left\"&gt;42&lt;/td&gt;\n&lt;td align=\"left\"&gt;26&lt;/td&gt;\n&lt;td align=\"left\"&gt;0&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;D.Samuel&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;5&lt;/td&gt;\n&lt;td align=\"left\"&gt;39&lt;/td&gt;\n&lt;td align=\"left\"&gt;16&lt;/td&gt;\n&lt;td align=\"left\"&gt;0&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;K.Juszczyk&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;3&lt;/td&gt;\n&lt;td align=\"left\"&gt;39&lt;/td&gt;\n&lt;td align=\"left\"&gt;15&lt;/td&gt;\n&lt;td align=\"left\"&gt;1&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;T.Hill&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;9&lt;/td&gt;\n&lt;td align=\"left\"&gt;105&lt;/td&gt;\n&lt;td align=\"left\"&gt;44&lt;/td&gt;\n&lt;td align=\"left\"&gt;0&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;S.Watkins&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;5&lt;/td&gt;\n&lt;td align=\"left\"&gt;98&lt;/td&gt;\n&lt;td align=\"left\"&gt;38&lt;/td&gt;\n&lt;td align=\"left\"&gt;0&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;T.Kelce&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;6&lt;/td&gt;\n&lt;td align=\"left\"&gt;43&lt;/td&gt;\n&lt;td align=\"left\"&gt;11&lt;/td&gt;\n&lt;td align=\"left\"&gt;1&lt;/td&gt;\n&lt;/tr&gt;\n&lt;/tbody&gt;&lt;/table&gt;\n\n&lt;hr/&gt;\n\n&lt;ul&gt;\n&lt;li&gt;Scoring Summary&lt;/li&gt;\n&lt;li&gt; &lt;/li&gt;\n&lt;/ul&gt;\n\n&lt;hr/&gt;\n\n&lt;table&gt;&lt;thead&gt;\n&lt;tr&gt;\n&lt;th align=\"center\"&gt;&lt;/th&gt;\n&lt;th align=\"center\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;/tr&gt;\n&lt;/thead&gt;&lt;tbody&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;strong&gt;Team&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;&lt;strong&gt;Q&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Type&lt;/strong&gt;&lt;/td&gt;\n&lt;td align=\"left\"&gt;&lt;strong&gt;Drive&lt;/strong&gt;&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;em&gt;SF&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;1&lt;/td&gt;\n&lt;td align=\"left\"&gt;FG&lt;/td&gt;\n&lt;td align=\"left\"&gt;R.Gould 38 yd. Field Goal Drive: 10 plays, 62 yards in 5:58&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;em&gt;KC&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;1&lt;/td&gt;\n&lt;td align=\"left\"&gt;TD&lt;/td&gt;\n&lt;td align=\"left\"&gt;P.Mahomes 1 yd. run (H.Butker kick is good) Drive: 15 plays, 75 yards in 7:26&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;em&gt;KC&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;2&lt;/td&gt;\n&lt;td align=\"left\"&gt;FG&lt;/td&gt;\n&lt;td align=\"left\"&gt;H.Butker 31 yd. Field Goal Drive: 9 plays, 43 yards in 4:36&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;em&gt;SF&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;2&lt;/td&gt;\n&lt;td align=\"left\"&gt;TD&lt;/td&gt;\n&lt;td align=\"left\"&gt;K.Juszczyk 15 yd. pass from J.Garoppolo (R.Gould kick is good) Drive: 7 plays, 80 yards in 4:27&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;em&gt;SF&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;3&lt;/td&gt;\n&lt;td align=\"left\"&gt;FG&lt;/td&gt;\n&lt;td align=\"left\"&gt;R.Gould 42 yd. Field Goal Drive: 9 plays, 60 yards in 5:31&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/49ers\"&gt;&lt;em&gt;SF&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;3&lt;/td&gt;\n&lt;td align=\"left\"&gt;TD&lt;/td&gt;\n&lt;td align=\"left\"&gt;R.Mostert 1 yd. run (R.Gould kick is good) Drive: 6 plays, 55 yards in 2:48&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;em&gt;KC&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;4&lt;/td&gt;\n&lt;td align=\"left\"&gt;TD&lt;/td&gt;\n&lt;td align=\"left\"&gt;T.Kelce 1 yd. pass from P.Mahomes (H.Butker kick is good) Drive: 10 plays, 83 yards in 2:40&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;em&gt;KC&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;4&lt;/td&gt;\n&lt;td align=\"left\"&gt;TD&lt;/td&gt;\n&lt;td align=\"left\"&gt;Dam.Williams 5 yd. pass from P.Mahomes (H.Butker kick is good) Drive: 7 plays, 65 yards in 2:26&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"center\"&gt;&lt;a href=\"/r/kansascitychiefs\"&gt;&lt;em&gt;KC&lt;/em&gt;&lt;/a&gt;&lt;/td&gt;\n&lt;td align=\"center\"&gt;4&lt;/td&gt;\n&lt;td align=\"left\"&gt;TD&lt;/td&gt;\n&lt;td align=\"left\"&gt;Dam.Williams 38 yd. run (H.Butker kick is good) Drive: 2 plays, 42 yards in 0:13&lt;/td&gt;\n&lt;/tr&gt;\n&lt;/tbody&gt;&lt;/table&gt;\n\n&lt;hr/&gt;\n\n&lt;ul&gt;\n&lt;li&gt;Thread Notes&lt;/li&gt;\n&lt;li&gt;&lt;a href=\"http://www.reddit.com/message/compose?to=%2Fr%2Fnfl\"&gt;Message The Moderators&lt;/a&gt;&lt;/li&gt;\n&lt;/ul&gt;\n\n&lt;hr/&gt;\n\n&lt;table&gt;&lt;thead&gt;\n&lt;tr&gt;\n&lt;th align=\"left\"&gt;&lt;/th&gt;\n&lt;/tr&gt;\n&lt;/thead&gt;&lt;tbody&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;Discuss whatever you wish. You can trash talk, but keep it civil.&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;If you are experiencing problems with comment sorting in the official reddit app, we suggest using a third-party client instead (&lt;a href=\"/r/Android/comments/7ctdf4/lets_settle_this_randroid_what_is_the_best_reddit/\"&gt;Android&lt;/a&gt;, &lt;a href=\"/r/ios/comments/68odw1/what_is_the_best_reddit_app_for_ios/\"&gt;iOS&lt;/a&gt;)&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;Turning comment sort to &lt;a href=\"/r/nfl/comments/ey0e0e/super_bowl_liv_post_game_thread_san_francisco/?sort=new\"&gt;&amp;#39;new&amp;#39;&lt;/a&gt; will help you see the newest comments.&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;Try &lt;a href=\"https://mybrowseraddon.com/tab-auto-refresh.html\"&gt;Tab Auto Refresh&lt;/a&gt; to auto-refresh this tab.&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;Use &lt;a href=\"http://reddit-stream.com/comments/ey0e0e\"&gt;reddit-stream.com&lt;/a&gt; to get an autorefreshing version of this page&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;Check in on the &lt;a href=\"/r/nfl\"&gt;r/nfl&lt;/a&gt; chat: &lt;strong&gt;#reddit-nfl&lt;/strong&gt; on FreeNode (&lt;a href=\"http://webchat.freenode.net/?channels=reddit-nfl\"&gt;open in browser&lt;/a&gt;).&lt;/td&gt;\n&lt;/tr&gt;\n&lt;tr&gt;\n&lt;td align=\"left\"&gt;Show your team affiliation - pick your team&amp;#39;s logo in the sidebar.&lt;/td&gt;\n&lt;/tr&gt;\n&lt;/tbody&gt;&lt;/table&gt;\n&lt;/div&gt;&lt;!-- SC_ON --&gt;",
    "likes": null,
    "suggested_sort": "confidence",
    "banned_at_utc": null,
    "view_count": null,
    "archived": false,
    "no_follow": false,
    "is_crosspostable": false,
    "pinned": false,
    "over_18": false,
    "all_awardings": [{
            "count": 1,
            "is_enabled": true,
            "subreddit_id": "t5_2qmg3",
            "description": "Shows the Mahomes Award and grants %{coin_symbol}200 Coins to the community. Exclusive to this community.",
            "end_date": null,
            "award_sub_type": "COMMUNITY",
            "coin_reward": 0,
            "icon_url": "https://i.redd.it/award_images/t5_2qmg3/pgt0be4qrje41_Mahomes.png",
            "days_of_premium": 0,
            "is_new": false,
            "id": "award_308de919-ebfd-467c-b7db-d99cedad1bed",
            "icon_height": 512,
            "resized_icons": [{
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/pgt0be4qrje41_Mahomes.png?width=16&amp;height=16&amp;auto=webp&amp;s=e110908fedb46d7942dd2ac562233c585276d90b",
                    "width": 16,
                    "height": 16
                }, {
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/pgt0be4qrje41_Mahomes.png?width=32&amp;height=32&amp;auto=webp&amp;s=a7f1826c2400c18a1d5173c95ee8c9161189e497",
                    "width": 32,
                    "height": 32
                }, {
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/pgt0be4qrje41_Mahomes.png?width=48&amp;height=48&amp;auto=webp&amp;s=2c762697531b2e27ce73af650d6857991ee8c258",
                    "width": 48,
                    "height": 48
                }, {
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/pgt0be4qrje41_Mahomes.png?width=64&amp;height=64&amp;auto=webp&amp;s=cbc7d734de612ca1b488701b0f46bd6a65c12a59",
                    "width": 64,
                    "height": 64
                }, {
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/pgt0be4qrje41_Mahomes.png?width=128&amp;height=128&amp;auto=webp&amp;s=733d7acb0245620433e042e514faeb3f209eed45",
                    "width": 128,
                    "height": 128
                }
            ],
            "days_of_drip_extension": 0,
            "award_type": "community",
            "start_date": null,
            "coin_price": 1000,
            "icon_width": 512,
            "subreddit_coin_reward": 200,
            "name": "Mahomes"
        }, {
            "count": 3,
            "is_enabled": true,
            "subreddit_id": "t5_2qmg3",
            "description": "Shows the Lombardi  Award and grants %{coin_symbol}100 Coins to the community. Exclusive to this community.",
            "end_date": null,
            "award_sub_type": "COMMUNITY",
            "coin_reward": 0,
            "icon_url": "https://i.redd.it/award_images/t5_2qmg3/hghx7l2jkug31_Lombardi.jpg",
            "days_of_premium": 0,
            "is_new": false,
            "id": "award_96302f04-c886-4031-81f9-0a4f317bb8ee",
            "icon_height": 700,
            "resized_icons": [{
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/hghx7l2jkug31_Lombardi.jpg?width=16&amp;height=16&amp;auto=webp&amp;s=30acd18eb18d0e735c4efdd9e083c633fdddf38e",
                    "width": 16,
                    "height": 16
                }, {
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/hghx7l2jkug31_Lombardi.jpg?width=32&amp;height=32&amp;auto=webp&amp;s=48c7ade58ca465abb9716cba17b1b346e3ed295c",
                    "width": 32,
                    "height": 32
                }, {
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/hghx7l2jkug31_Lombardi.jpg?width=48&amp;height=48&amp;auto=webp&amp;s=d456576578645693c2e3e39cb9c2031fff573da2",
                    "width": 48,
                    "height": 48
                }, {
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/hghx7l2jkug31_Lombardi.jpg?width=64&amp;height=64&amp;auto=webp&amp;s=442a8277f874ef9f55a1cf0f8d80d16915346bf7",
                    "width": 64,
                    "height": 64
                }, {
                    "url": "https://preview.redd.it/award_images/t5_2qmg3/hghx7l2jkug31_Lombardi.jpg?width=128&amp;height=128&amp;auto=webp&amp;s=355a94d85eec91751cd0b44d2d56a6d0ae0f9f2c",
                    "width": 128,
                    "height": 128
                }
            ],
            "days_of_drip_extension": 0,
            "award_type": "community",
            "start_date": null,
            "coin_price": 500,
            "icon_width": 700,
            "subreddit_coin_reward": 100,
            "name": "Lombardi "
        }, {
            "count": 1,
            "is_enabled": true,
            "subreddit_id": null,
            "description": "Gives the author a month of Reddit Premium, which includes %{coin_symbol}700 Coins for that month, and shows a Platinum Award.",
            "end_date": null,
            "award_sub_type": "GLOBAL",
            "coin_reward": 0,
            "icon_url": "https://www.redditstatic.com/gold/awards/icon/platinum_512.png",
            "days_of_premium": 31,
            "is_new": false,
            "id": "gid_3",
            "icon_height": 512,
            "resized_icons": [{
                    "url": "https://www.redditstatic.com/gold/awards/icon/platinum_16.png",
                    "width": 16,
                    "height": 16
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/platinum_32.png",
                    "width": 32,
                    "height": 32
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/platinum_48.png",
                    "width": 48,
                    "height": 48
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/platinum_64.png",
                    "width": 64,
                    "height": 64
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/platinum_128.png",
                    "width": 128,
                    "height": 128
                }
            ],
            "days_of_drip_extension": 31,
            "award_type": "global",
            "start_date": null,
            "coin_price": 1800,
            "icon_width": 512,
            "subreddit_coin_reward": 0,
            "name": "Platinum"
        }, {
            "count": 2,
            "is_enabled": true,
            "subreddit_id": null,
            "description": "Gives the author a week of Reddit Premium, %{coin_symbol}100 Coins to do with as they please, and shows a Gold Award.",
            "end_date": null,
            "award_sub_type": "GLOBAL",
            "coin_reward": 100,
            "icon_url": "https://www.redditstatic.com/gold/awards/icon/gold_512.png",
            "days_of_premium": 7,
            "is_new": false,
            "id": "gid_2",
            "icon_height": 512,
            "resized_icons": [{
                    "url": "https://www.redditstatic.com/gold/awards/icon/gold_16.png",
                    "width": 16,
                    "height": 16
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/gold_32.png",
                    "width": 32,
                    "height": 32
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/gold_48.png",
                    "width": 48,
                    "height": 48
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/gold_64.png",
                    "width": 64,
                    "height": 64
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/gold_128.png",
                    "width": 128,
                    "height": 128
                }
            ],
            "days_of_drip_extension": 0,
            "award_type": "global",
            "start_date": null,
            "coin_price": 500,
            "icon_width": 512,
            "subreddit_coin_reward": 0,
            "name": "Gold"
        }, {
            "count": 18,
            "is_enabled": true,
            "subreddit_id": null,
            "description": "Support your team!",
            "end_date": 1580716800,
            "award_sub_type": "GLOBAL",
            "coin_reward": 0,
            "icon_url": "https://i.redd.it/award_images/t5_22cerq/nwbihupzd7e41_KansasCity-1.png",
            "days_of_premium": 0,
            "is_new": false,
            "id": "award_214698f0-87fd-4e06-9c68-0c25621b4cec",
            "icon_height": 2048,
            "resized_icons": [{
                    "url": "https://preview.redd.it/award_images/t5_22cerq/nwbihupzd7e41_KansasCity-1.png?width=16&amp;height=16&amp;auto=webp&amp;s=04a10f7430466b70edb6646c70ad7bf6824b08eb",
                    "width": 16,
                    "height": 16
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/nwbihupzd7e41_KansasCity-1.png?width=32&amp;height=32&amp;auto=webp&amp;s=ae272f80f5b7fd5e95e858e1ae0d8272f7d6a1c1",
                    "width": 32,
                    "height": 32
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/nwbihupzd7e41_KansasCity-1.png?width=48&amp;height=48&amp;auto=webp&amp;s=e0d9de48ee75e222a97dfdf8ca69871c75ead070",
                    "width": 48,
                    "height": 48
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/nwbihupzd7e41_KansasCity-1.png?width=64&amp;height=64&amp;auto=webp&amp;s=d8117e3d410261b47abcc55bcb654060ab508ae7",
                    "width": 64,
                    "height": 64
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/nwbihupzd7e41_KansasCity-1.png?width=128&amp;height=128&amp;auto=webp&amp;s=1c1d8e2a7ad3a386519bf2c0660a6233af0cfe0e",
                    "width": 128,
                    "height": 128
                }
            ],
            "days_of_drip_extension": 0,
            "award_type": "global",
            "start_date": 1580517000,
            "coin_price": 250,
            "icon_width": 2048,
            "subreddit_coin_reward": 0,
            "name": "Kansas City"
        }, {
            "count": 1,
            "is_enabled": true,
            "subreddit_id": null,
            "description": "To pay respects.",
            "end_date": null,
            "award_sub_type": "GLOBAL",
            "coin_reward": 0,
            "icon_url": "https://i.redd.it/award_images/t5_22cerq/tcofsbf92md41_PressF.png",
            "days_of_premium": 0,
            "is_new": false,
            "id": "award_88fdcafc-57a0-48db-99cc-76276bfaf28b",
            "icon_height": 2048,
            "resized_icons": [{
                    "url": "https://preview.redd.it/award_images/t5_22cerq/tcofsbf92md41_PressF.png?width=16&amp;height=16&amp;auto=webp&amp;s=3481c2a89c2ebe653aae1b8d627c20c10abfc79e",
                    "width": 16,
                    "height": 16
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/tcofsbf92md41_PressF.png?width=32&amp;height=32&amp;auto=webp&amp;s=2bd2b8a9417e7cc18752927c11f98b242c133f2f",
                    "width": 32,
                    "height": 32
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/tcofsbf92md41_PressF.png?width=48&amp;height=48&amp;auto=webp&amp;s=a34e3d83c5dd9f6c731b1375500e4de8d4fee652",
                    "width": 48,
                    "height": 48
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/tcofsbf92md41_PressF.png?width=64&amp;height=64&amp;auto=webp&amp;s=6525899b9a01d5b0c4deea6c34cd8436ee1ff0c7",
                    "width": 64,
                    "height": 64
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/tcofsbf92md41_PressF.png?width=128&amp;height=128&amp;auto=webp&amp;s=c9e094023649693de991fff551a0c9561d11163a",
                    "width": 128,
                    "height": 128
                }
            ],
            "days_of_drip_extension": 0,
            "award_type": "global",
            "start_date": null,
            "coin_price": 150,
            "icon_width": 2048,
            "subreddit_coin_reward": 0,
            "name": "Press F"
        }, {
            "count": 1,
            "is_enabled": true,
            "subreddit_id": null,
            "description": "Prayers up for the blessed.",
            "end_date": null,
            "award_sub_type": "GLOBAL",
            "coin_reward": 0,
            "icon_url": "https://i.redd.it/award_images/t5_22cerq/trfv6ems1md41_BlessUp.png",
            "days_of_premium": 0,
            "is_new": false,
            "id": "award_77ba55a2-c33c-4351-ac49-807455a80148",
            "icon_height": 2048,
            "resized_icons": [{
                    "url": "https://preview.redd.it/award_images/t5_22cerq/trfv6ems1md41_BlessUp.png?width=16&amp;height=16&amp;auto=webp&amp;s=7a2f2b927be72d2b46ebd95bab8c072c3be0fbab",
                    "width": 16,
                    "height": 16
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/trfv6ems1md41_BlessUp.png?width=32&amp;height=32&amp;auto=webp&amp;s=6e42b7095bcc331e53202438613aa827addf70c3",
                    "width": 32,
                    "height": 32
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/trfv6ems1md41_BlessUp.png?width=48&amp;height=48&amp;auto=webp&amp;s=c740f7ef642fd2042d62c2bcba98734d08dfae6c",
                    "width": 48,
                    "height": 48
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/trfv6ems1md41_BlessUp.png?width=64&amp;height=64&amp;auto=webp&amp;s=74e630f1072bb2423034ae48aefa241d834d7186",
                    "width": 64,
                    "height": 64
                }, {
                    "url": "https://preview.redd.it/award_images/t5_22cerq/trfv6ems1md41_BlessUp.png?width=128&amp;height=128&amp;auto=webp&amp;s=0a89cd8011c8210315ee60441eefd77b973a0c82",
                    "width": 128,
                    "height": 128
                }
            ],
            "days_of_drip_extension": 0,
            "award_type": "global",
            "start_date": null,
            "coin_price": 150,
            "icon_width": 2048,
            "subreddit_coin_reward": 0,
            "name": "Bless Up"
        }, {
            "count": 7,
            "is_enabled": true,
            "subreddit_id": null,
            "description": "Shows the Silver Award... and that's it.",
            "end_date": null,
            "award_sub_type": "GLOBAL",
            "coin_reward": 0,
            "icon_url": "https://www.redditstatic.com/gold/awards/icon/silver_512.png",
            "days_of_premium": 0,
            "is_new": false,
            "id": "gid_1",
            "icon_height": 512,
            "resized_icons": [{
                    "url": "https://www.redditstatic.com/gold/awards/icon/silver_16.png",
                    "width": 16,
                    "height": 16
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/silver_32.png",
                    "width": 32,
                    "height": 32
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/silver_48.png",
                    "width": 48,
                    "height": 48
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/silver_64.png",
                    "width": 64,
                    "height": 64
                }, {
                    "url": "https://www.redditstatic.com/gold/awards/icon/silver_128.png",
                    "width": 128,
                    "height": 128
                }
            ],
            "days_of_drip_extension": 0,
            "award_type": "global",
            "start_date": null,
            "coin_price": 100,
            "icon_width": 512,
            "subreddit_coin_reward": 0,
            "name": "Silver"
        }
    ],
    "awarders": ["drdisrespectme", "noahdj1512", "flubberFuck", "Insertnicenamehere"],
    "media_only": false,
    "can_gild": false,
    "spoiler": false,
    "locked": false,
    "author_flair_text": "Game thread bot",
    "visited": false,
    "removed_by": null,
    "num_reports": null,
    "distinguished": null,
    "subreddit_id": "t5_2qmg3",
    "mod_reason_by": null,
    "removal_reason": null,
    "link_flair_background_color": "",
    "id": "ey0e0e",
    "is_robot_indexable": true,
    "report_reasons": null,
    "author": "nfl_gamethread",
    "discussion_type": null,
    "num_comments": 8392,
    "send_replies": false,
    "whitelist_status": "all_ads",
    "contest_mode": false,
    "mod_reports": [],
    "author_patreon_flair": false,
    "author_flair_text_color": "dark",
    "permalink": "/r/nfl/comments/ey0e0e/super_bowl_liv_post_game_thread_san_francisco/",
    "parent_whitelist_status": "all_ads",
    "stickied": true,
    "url": "https://www.reddit.com/r/nfl/comments/ey0e0e/super_bowl_liv_post_game_thread_san_francisco/",
    "subreddit_subscribers": 1836516,
    "created_utc": 1580699517.0,
    "num_crossposts": 0,
    "media": null,
    "is_video": false
}




*/