package com.goldenminute.catalyst.ui

import java.util.Calendar

object JalaliCalendarHelper {
    class JalaliDate(val year: Int, val month: Int, val day: Int) {
        companion object {
            private val MONTH_NAMES = arrayOf(
                "فروردین", "اردیبهشت", "خرداد",
                "تیر", "مرداد", "شهریور",
                "مهر", "آبان", "آذر",
                "دی", "بهمن", "اسفند"
            )
            fun getMonthName(month: Int): String {
                return if (month in 1..12) MONTH_NAMES[month - 1] else ""
            }
        }
    }

    class GregorianDate(val year: Int, val month: Int, val day: Int)

    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val g_days_in_month = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val j_days_in_month = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy = gYear - 1600
        val gm = gMonth - 1
        val gd = gDay - 1

        var g_day_no = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            g_day_no += g_days_in_month[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            g_day_no++
        }
        g_day_no += gd

        var j_day_no = g_day_no - 79

        val j_np = j_day_no / 12053
        j_day_no %= 12053

        var jy = 979 + 33 * j_np + 4 * (j_day_no / 1461)
        j_day_no %= 1461

        if (j_day_no >= 366) {
            jy += (j_day_no - 1) / 365
            j_day_no = (j_day_no - 1) % 365
        }

        var jm = 0
        for (i in 0 until 11) {
            if (j_day_no < j_days_in_month[i]) {
                jm = i
                break
            }
            j_day_no -= j_days_in_month[i]
            jm = 11
        }
        val jd = j_day_no + 1
        return JalaliDate(jy, jm + 1, jd)
    }

    fun jalaliToGregorian(jYear: Int, jMonth: Int, jDay: Int): GregorianDate {
        val jy = jYear - 979
        val jm = jMonth - 1
        val jd = jDay - 1

        var j_day_no = 365 * jy + (jy / 33) * 8 + (jy % 33 + 3) / 4
        for (i in 0 until jm) {
            j_day_no += if (i < 6) 31 else 30
        }
        j_day_no += jd

        var g_day_no = j_day_no + 79

        var gy = 1600 + 400 * (g_day_no / 146097)
        g_day_no %= 146097

        if (g_day_no >= 36525) {
            g_day_no--
            gy += 100 * (g_day_no / 36524)
            g_day_no %= 36524

            if (g_day_no >= 365) {
                g_day_no++
            }
        }

        gy += 4 * (g_day_no / 1461)
        g_day_no %= 1461

        if (g_day_no >= 366) {
            g_day_no--
            gy += g_day_no / 365
            g_day_no %= 365
        }

        var i = 0
        val g_days_in_month = intArrayOf(
            31,
            if (gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)) 29 else 28,
            31,
            30,
            31,
            30,
            31,
            31,
            30,
            31,
            30,
            31
        )
        while (i < 12 && g_day_no >= g_days_in_month[i]) {
            g_day_no -= g_days_in_month[i]
            i++
        }
        val gm = i + 1
        val gd = g_day_no + 1

        return GregorianDate(gy, gm, gd)
    }

    fun isJalaliLeapYear(jYear: Int): Boolean {
        val r = jYear % 33
        return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }

    fun getJalaliMonthDays(jYear: Int, jMonth: Int): Int {
        return when (jMonth) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isJalaliLeapYear(jYear)) 30 else 29
            else -> 30
        }
    }

    fun getJalaliWeekdayIndex(gregorianDayOfWeek: Int): Int {
        return when (gregorianDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun getPersianDayOfWeekName(gregorianDayOfWeek: Int): String {
        return when (gregorianDayOfWeek) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یک‌شنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    fun toPersianDigits(input: String): String {
        return input.map { char ->
            when (char) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> char
            }
        }.joinToString("")
    }

    fun formatJalali(
        timestamp: Long,
        includeTime: Boolean = false,
        includeYear: Boolean = true,
        includeWeekday: Boolean = false
    ): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val jd = gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val monthName = JalaliDate.getMonthName(jd.month)
        val dayStr = toPersianDigits(jd.day.toString())
        val yearStr = toPersianDigits(jd.year.toString())

        val datePart = buildString {
            if (includeWeekday) {
                val dayOfWeekName = getPersianDayOfWeekName(cal.get(Calendar.DAY_OF_WEEK))
                append("$dayOfWeekName، ")
            }
            append("$dayStr $monthName")
            if (includeYear) {
                append(" $yearStr")
            }
        }

        if (includeTime) {
            val timeSdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val timeStr = toPersianDigits(timeSdf.format(java.util.Date(timestamp)))
            return "$datePart، $timeStr"
        }
        return datePart
    }

    fun formatDateOrTime(
        timestamp: Long,
        isPersian: Boolean,
        defaultPattern: String = "MMM d, yyyy • h:mm a",
        includeTime: Boolean = true,
        includeYear: Boolean = true,
        includeWeekday: Boolean = false
    ): String {
        return if (isPersian) {
            formatJalali(timestamp, includeTime = includeTime, includeYear = includeYear, includeWeekday = includeWeekday)
        } else {
            val sdf = java.text.SimpleDateFormat(defaultPattern, java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}
