package com.idunnololz.summit.util

import com.idunnololz.summit.lemmy.actions.ActionInfo
import com.idunnololz.summit.lemmy.utils.VotableRef
import com.squareup.moshi.Moshi

val moshi: Moshi by lazy {
    Moshi.Builder()
        .build()
}