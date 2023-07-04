package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.idunnololz.summit.api.dto.PersonId
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface PersonRef : Parcelable, PageRef {

    val instance: String

    @Parcelize
    @TypeLabel("1")
    data class PersonRefByName(
        val name: String,
        override val instance: String,
    ): PersonRef
}