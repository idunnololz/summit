package com.idunnololz.summit.lemmy.multicommunity

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PersonId
import com.idunnololz.summit.api.dto.PostView
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Parcelize
@Serializable
data class FetchedPost(
    val postView: PostView,
    val source: Source,
) : Parcelable

fun PostView.toFetchedPost() = FetchedPost(
    postView = this,
    source = Source.StandardSource(),
)

@Serializable
@JsonClassDiscriminator("t")
sealed interface Source : Parcelable {
    @Serializable
    @SerialName("1")
    @Parcelize
    class StandardSource : Source

    @Serializable
    @SerialName("2")
    @Parcelize
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
