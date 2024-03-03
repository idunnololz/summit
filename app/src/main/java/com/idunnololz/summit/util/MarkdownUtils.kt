package com.idunnololz.summit.util

fun String.escapeMarkdown(): String {
    val that = this

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
