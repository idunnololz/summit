package com.idunnololz.summit.api.dto

data class Register(
    val username: String,
    val password: String,
    val password_verify: String,
    val show_nsfw: Boolean,

    /**
     * email is mandatory if email verification is enabled on the server
     */
    val email: String? = null,
    /**
     * The UUID of the captcha item.
     */
    val captcha_uuid: String? = null,
    /**
     * Your captcha answer.
     */
    val captcha_answer: String? = null,
    /**
     * A form field to trick signup bots. Should be None.
     */
    val honeypot: String? = null,
    /**
     * An answer is mandatory if require application is enabled on the server
     */
    val answer: String? = null,
)
