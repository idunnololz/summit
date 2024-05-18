package com.idunnololz.summit.lemmy.multicommunity

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostView
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class FetchedPost(
    val postView: PostView,
    val source: Source,
) : Parcelable

fun PostView.toFetchedPost() = FetchedPost(
    postView = this,
    source = Source.StandardSource(),
)

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface Source : Parcelable {
    @Parcelize
    @TypeLabel("1")
    @JsonClass(generateAdapter = true)
    class StandardSource : Source

    @Parcelize
    @TypeLabel("2")
    @JsonClass(generateAdapter = true)
    class AccountSource(
        val name: String,
        val id: PersonId,
        val instance: String,
    ) : Source
}

val Source.accountId: Long?
    get() {
        return when (this) {
            is Source.AccountSource -> this.id
            is Source.StandardSource -> null
        }
    }

val Source.instance: String?
    get() {
        return when (this) {
            is Source.AccountSource -> this.instance
            is Source.StandardSource -> null
        }
    }
