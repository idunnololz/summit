package com.idunnololz.summit.util.ext

fun String.count(element: String): Int {
    var count = 0

    // Check if the string contains the element at all
    var lastIndex = indexOf(element, 0)
    while (lastIndex >= 0) {
        count += 1

        // Find the next occurence
        lastIndex = indexOf(element, lastIndex + 1)
    }

    return count
}