package com.idunnololz.summit.util

import java.util.*

object NumberFormatUtil {
  private val suffixes: NavigableMap<Long, String> = TreeMap<Long, String>().apply {
    this[1_000L] = "k"
    this[1_000_000L] = "M"
    this[1_000_000_000L] = "G"
    this[1_000_000_000_000L] = "T"
    this[1_000_000_000_000_000L] = "P"
    this[1_000_000_000_000_000_000L] = "E"
  }

  fun format(value: Long): String {
    // Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
    if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1)
    if (value < 0) return "-" + format(-value)
    if (value < 1000) return value.toString() // deal with easy case
    val e = suffixes.floorEntry(value)
    val divideBy = e.key
    val suffix = e.value
    val truncated = value / (divideBy / 10) // the number part of the output times 10
    val hasDecimal =
      truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
    return if (hasDecimal) {
      (truncated / 10.0).toString() + suffix
    } else {
      (truncated / 10).toString() + suffix
    }
  }
}
