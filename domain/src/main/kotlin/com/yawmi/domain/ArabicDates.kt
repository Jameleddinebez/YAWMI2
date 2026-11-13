package com.yawmi.domain

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * تنسيق التواريخ بالعربية. أسماء الأشهر الميلادية بالصيغة الجزائرية المتداولة (جانفي، فيفري، ...).
 *
 * تنبيه: التاريخ الهجري هنا حسابي (تقويم أم القرى) ويُعرض للاستئناس فقط.
 * المناسبات الدينية الرسمية في الجزائر تُعتمد بالإعلان الرسمي — لا تُستنتج من هذا الحساب.
 */
object ArabicDates {

    private val dayNames = mapOf(
        DayOfWeek.SUNDAY to "الأحد",
        DayOfWeek.MONDAY to "الاثنين",
        DayOfWeek.TUESDAY to "الثلاثاء",
        DayOfWeek.WEDNESDAY to "الأربعاء",
        DayOfWeek.THURSDAY to "الخميس",
        DayOfWeek.FRIDAY to "الجمعة",
        DayOfWeek.SATURDAY to "السبت",
    )

    val gregorianMonths = listOf(
        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
    )

    val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الثانية",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )

    fun dayName(date: LocalDate): String = dayNames.getValue(date.dayOfWeek)

    fun dayName(day: DayOfWeek): String = dayNames.getValue(day)

    fun gregorian(date: LocalDate): String =
        "${date.dayOfMonth} ${gregorianMonths[date.monthValue - 1]} ${date.year}"

    /** null إذا كان التاريخ خارج نطاق تقويم أم القرى المدعوم. */
    fun hijri(date: LocalDate): String? = try {
        val h = HijrahDate.from(date)
        val day = h.get(ChronoField.DAY_OF_MONTH)
        val month = h.get(ChronoField.MONTH_OF_YEAR)
        val year = h.get(ChronoField.YEAR)
        "$day ${hijriMonths[month - 1]} $year هـ"
    } catch (e: DateTimeException) {
        null
    }
}
