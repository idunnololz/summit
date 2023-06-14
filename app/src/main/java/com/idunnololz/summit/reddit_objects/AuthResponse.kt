package com.idunnololz.summit.reddit_objects

class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: String,
    val scope: String,
    val refreshToken: String
)