package com.idunnololz.summit.util

const val GIF_HEADER_1 = "GIF87a"
const val GIF_HEADER_2 = "GIF89a"

fun isGif(s: String): Boolean = s.startsWith(GIF_HEADER_1) || s.startsWith(GIF_HEADER_2)
