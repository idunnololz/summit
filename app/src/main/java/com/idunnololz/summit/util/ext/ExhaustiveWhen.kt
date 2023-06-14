package com.idunnololz.summit.util.ext

object Do {
    inline infix fun <reified T> exhaustive(any: T?) = any
}