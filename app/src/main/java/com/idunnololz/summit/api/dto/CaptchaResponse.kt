package com.idunnololz.summit.api.dto

data class CaptchaResponse(
    val png: String,
    val wav: String,
    val uuid: String,
)
