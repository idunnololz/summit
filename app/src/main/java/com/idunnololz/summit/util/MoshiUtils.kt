package com.idunnololz.summit.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.lemmy.Community
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.util.Date

val moshi: Moshi by lazy {
    Moshi.Builder()
        .add(Community.adapter())
        .build()
}