package com.idunnololz.summit.util

import android.util.Log

fun String.escapeMarkdown(): String {
    val that = this

    Log.d("HAHA", "input: $that")

    return buildString {
        for (c in that) {
            when (c) {
                '\\' ->
                    append("\\\\")
                '`' ->
                    append("\\`")
                '*' ->
                    append("\\*")
                '_' ->
                    append("\\_")
                '{' ->
                    append("\\{")
                '}' ->
                    append("\\}")
                '[' ->
                    append("\\[")
                ']' ->
                    append("\\]")
                '<' ->
                    append("\\<")
                '>' ->
                    append("\\>")
                '(' ->
                    append("\\(")
                ')' ->
                    append("\\)")
                '\n' -> {
                    // do nothing
                }
                '#' ->
                    append("\\#")
                '+' ->
                    append("\\+")
                '-' ->
                    append("\\-")
                '.' ->
                    append("\\.")
                '!' ->
                    append("\\!")
                '|' ->
                    append("\\|")
                else ->
                    append(c)
            }
        }
    }
}
