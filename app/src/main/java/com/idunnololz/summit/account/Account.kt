package com.idunnololz.summit.account

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.idunnololz.summit.api.dto.ListingType
import com.idunnololz.summit.api.dto.LocalUserView
import com.idunnololz.summit.api.dto.SortType
import com.idunnololz.summit.lemmy.PersonRef
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Account(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "current") val current: Boolean,
    @ColumnInfo(name = "instance") override val instance: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "jwt") val jwt: String,
    @ColumnInfo(
        name = "default_listing_type",
        defaultValue = "0",
    )
    val defaultListingType: Int,
    @ColumnInfo(
        name = "default_sort_type",
        defaultValue = "0",
    )
    val defaultSortType: Int,
) : Parcelable, GuestOrUserAccount {

    companion object {
        fun from(instance: String, localUserView: LocalUserView, jwt: String) = Account(
            id = localUserView.person.id,
            name = localUserView.person.name,
            current = true,
            instance = instance,
            jwt = jwt,
            defaultListingType = localUserView.local_user.default_listing_type?.ordinal
                ?: ListingType.All.ordinal,
            defaultSortType = localUserView.local_user.default_sort_type?.ordinal
                ?: SortType.Active.ordinal,
        )
    }
}

/**
 * Only use for display name. This value is not stable as the name of an account can be changed.
 */
val Account.fullName
    get() = "${this.name}@${this.instance}"

/**
 * [fullName] is not stable as the name of an account can be changed.
 */
val Account.stableId
    get() = "i%${this.id}@${this.instance}"

fun Account.toPersonRef() = PersonRef.PersonRefByName(name = name, instance = instance)
