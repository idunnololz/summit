package com.idunnololz.summit.util

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? =
    BundleCompat.getParcelable(this, key, T::class.java)

inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(
    key: String,
): ArrayList<T>? = BundleCompat.getParcelableArrayList(this, key, T::class.java)
