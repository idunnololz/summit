package com.idunnololz.summit.offline

data class RecurringEvent(
    val daysOfWeek: List<Int>,
    val hourOfDay: Int,
    val minuteOfHour: Int,
) {
    fun serializeToString(): String = buildString {
        append(hourOfDay)
        append(',')
        append(minuteOfHour)
        append(',')
        daysOfWeek.forEach {
            append(it)
            append(',')
        }
        setLength(length - 1)
    }

    companion object {
        fun fromString(str: String): RecurringEvent = str.split(',').let { tokens ->
            RecurringEvent(
                daysOfWeek = tokens.drop(2).map { it.toInt() },
                hourOfDay = tokens[0].toInt(),
                minuteOfHour = tokens[1].toInt(),
            )
        }
    }
}
