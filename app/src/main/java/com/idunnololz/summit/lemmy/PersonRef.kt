package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.utils.instance
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true, generator = "sealed:t")
sealed interface PersonRef : Parcelable, PageRef {

    val instance: String

    @Parcelize
    @TypeLabel("1")
    @JsonClass(generateAdapter = true)
    data class PersonRefByName(
        val name: String,
        /**
         * This should be the instance of the actual person.
         */
        override val instance: String,
    ) : PersonRef

    val fullName: String
        get() =
            when (this) {
                is PersonRefByName -> "$name@$instance"
            }
}

fun Person.toPersonRef() = PersonRef.PersonRefByName(name, instance)
