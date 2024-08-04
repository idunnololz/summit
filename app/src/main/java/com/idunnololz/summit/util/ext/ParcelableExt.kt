package com.idunnololz.summit.util.ext

import android.os.Parcel
import android.os.Parcelable
import androidx.work.Data

fun Parcelable.toByteArray(): ByteArray? {
    val parcel = Parcel.obtain()
    this.writeToParcel(parcel, 0)
    return parcel.marshall()
}

inline fun <reified T : Parcelable> Data.getParcelable(key: String): T? {
    val parcel = Parcel.obtain()
    try {
        val bytes = getByteArray(key) ?: return null
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        @Suppress("UNCHECKED_CAST")
        val creator = T::class.java.getField("CREATOR").get(null) as Parcelable.Creator<T>
        return creator.createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
}
