package com.idunnololz.summit.util

import java.text.DecimalFormat

object FileSizeUtils {
    private val K: Long = 1024
    private val M = K * K
    private val G = M * K
    private val T = G * K

    fun convertToStringRepresentation(value: Long): String? {
        val dividers = longArrayOf(T, G, M, K, 1)
        val units = arrayOf("TB", "GB", "MB", "KB", "B")
        require(value >= 1) { "Invalid file size: $value" }
        var result: String? = null
        for (i in dividers.indices) {
            val divider = dividers[i]
            if (value >= divider) {
                result = format(value, divider, units[i])
                break
            }
        }
        return result
    }

    private fun format(value: Long, divider: Long, unit: String): String {
        val result = if (divider > 1) value.toDouble() / divider.toDouble() else value.toDouble()
        return DecimalFormat("#,##0.##").format(result) + " " + unit
    }
}
