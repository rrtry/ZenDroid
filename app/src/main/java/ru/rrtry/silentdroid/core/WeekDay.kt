package ru.rrtry.silentdroid.core

enum class WeekDay(val num: Int, val value: Int) {

    MONDAY(1, 0x2),

    TUESDAY(2, 0x4),

    WEDNESDAY(3, 0x8),

    THURSDAY(4, 0x10),

    FRIDAY(5, 0x20),

    SATURDAY(6, 0x40),

    SUNDAY(7, 0x1);

    companion object {

        const val WEEKENDS: Int = 0x41
        const val WEEKDAYS: Int = 0x3E
        const val ALL_DAYS: Int = 0x7F
        const val NONE: Int = 0x0

        val values: Array<WeekDay>
        get() = values()

        fun fromDay(day: Int): Int {
            return values[day - 1].value
        }
    }
}