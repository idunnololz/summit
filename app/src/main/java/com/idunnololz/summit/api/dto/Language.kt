package com.idunnololz.summit.api.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Language(
    val id: LanguageId,
    val code: String,
    val name: String,
) : Parcelable
