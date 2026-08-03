package com.goldenminute.catalyst

import com.goldenminute.catalyst.ui.JalaliCalendarHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testJalaliCalendarHelperConversions() {
    // 1403-03-04 (Jalali) or 1403/03/04
    // 1403-03-04 corresponds to 2024-05-24 (Gregorian)
    val jalali = JalaliCalendarHelper.gregorianToJalali(2024, 5, 24)
    assertEquals(1403, jalali.year)
    assertEquals(3, jalali.month)
    assertEquals(4, jalali.day)

    val gregorian = JalaliCalendarHelper.jalaliToGregorian(1403, 3, 4)
    assertEquals(2024, gregorian.year)
    assertEquals(5, gregorian.month)
    assertEquals(24, gregorian.day)
  }

  @Test
  fun testJalaliLeapYears() {
    assertTrue(JalaliCalendarHelper.isJalaliLeapYear(1403))
    assertFalse(JalaliCalendarHelper.isJalaliLeapYear(1404))
    assertFalse(JalaliCalendarHelper.isJalaliLeapYear(1405))
  }

  @Test
  fun testJalaliMonthsAndDigits() {
    assertEquals(31, JalaliCalendarHelper.getJalaliMonthDays(1403, 1))
    assertEquals(31, JalaliCalendarHelper.getJalaliMonthDays(1403, 6))
    assertEquals(30, JalaliCalendarHelper.getJalaliMonthDays(1403, 7))
    assertEquals(30, JalaliCalendarHelper.getJalaliMonthDays(1403, 11))
    assertEquals(30, JalaliCalendarHelper.getJalaliMonthDays(1403, 12)) // Leap year
    assertEquals(29, JalaliCalendarHelper.getJalaliMonthDays(1404, 12)) // Non-Leap year

    assertEquals("۰۱۲۳۴۵۶۷۸۹", JalaliCalendarHelper.toPersianDigits("0123456789"))
  }
}



