package com.idunnololz.summit.lemmy

import android.os.Parcelable
import com.idunnololz.summit.api.dto.Person
import com.idunnololz.summit.api.utils.instance
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("t")
sealed interface PersonRef : Parcelable, PageRef {

    val instance: String

    @Serializable
    @SerialName("1")
    @Parcelize
    data class PersonRefByName(
        val name: String,
        /**
         * This should be the instance of the actual person.
         */
        override val instance: String,
    ) : PersonRef

    @Serializable
    @SerialName("2")
    @Parcelize
    data class PersonRefById(
        val id: Long,
        /**
         * This should be the instance of the actual person.
         */
        override val instance: String,
    ) : PersonRef

    val fullName: String
        get() =
            when (this) {
                is PersonRefByName -> "$name@$instance"
                is PersonRefById -> "id:$id@$instance"
            }
}

fun Person.toPersonRef() = PersonRef.PersonRefByName(name, instance)
