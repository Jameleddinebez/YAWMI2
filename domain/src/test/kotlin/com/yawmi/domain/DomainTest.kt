package com.yawmi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DomainTest {

    private fun lesson(id: String, s: Int, e: Int) =
        AgendaItem(id, "حصة $id", AgendaKind.LESSON, s, e, AgendaSource.SCHEDULE)

    private fun item(id: String, kind: AgendaKind, s: Int?, e: Int?) =
        AgendaItem(id, id, kind, s, e, AgendaSource.APPOINTMENT)

    // ---------- AgendaCalculator ----------

    @Test
    fun agendaIsSortedByStartAndUntimedItemsGoLast() {
        val sorted = AgendaCalculator.sortedAgenda(
            listOf(item("z", AgendaKind.OTHER, null, null), lesson("b", 600, 660), lesson("a", 480, 540)),
        )
        assertEquals(listOf("a", "b", "z"), sorted.map { it.id })
    }

    @Test
    fun scheduledMinutesCountsLessonsAndSkipsBreaksAndPersonal() {
        val items = listOf(
            lesson("1", 480, 540),
            item("br", AgendaKind.BREAK, 540, 555),
            lesson("2", 555, 615),
            item("p", AgendaKind.PERSONAL, 700, 760),
        )
        assertEquals(120, AgendaCalculator.scheduledWorkMinutes(items))
    }

    @Test
    fun overlappingItemsAreNotDoubleCounted() {
        val items = listOf(lesson("1", 480, 540), item("m", AgendaKind.MEETING, 510, 570))
        assertEquals(90, AgendaCalculator.scheduledWorkMinutes(items))
    }

    @Test
    fun remainingLessonsCountsRunningAndFutureOnly() {
        val items = listOf(lesson("1", 480, 540), lesson("2", 555, 615), lesson("3", 700, 760))
        // 09:30 = 570 دقيقة → الحصة 1 انتهت، 2 جارية، 3 قادمة
        assertEquals(2, AgendaCalculator.remainingLessons(items, 570 * 60))
        assertEquals(3, AgendaCalculator.remainingLessons(items, 0))
        assertEquals(0, AgendaCalculator.remainingLessons(items, 800 * 60))
    }

    @Test
    fun nextAndCurrentActivityIgnoreBreaks() {
        val items = listOf(
            lesson("1", 480, 540),
            item("br", AgendaKind.BREAK, 540, 555),
            lesson("2", 555, 615),
        )
        val now = 545 * 60 // أثناء الاستراحة
        assertNull(AgendaCalculator.currentActivity(items, now))
        assertEquals("2", AgendaCalculator.nextActivity(items, now)?.id)
        assertEquals(10 * 60, AgendaCalculator.secondsUntilStart(items[2], now))
        assertEquals("1", AgendaCalculator.currentActivity(items, 500 * 60)?.id)
        assertNull(AgendaCalculator.nextActivity(items, 600 * 60))
    }

    @Test
    fun untimedItemsAreNeverNextActivity() {
        val items = listOf(item("x", AgendaKind.MEETING, null, null))
        assertNull(AgendaCalculator.nextActivity(items, 0))
    }

    // ---------- DayTypeResolver ----------

    private val sunday = LocalDate.of(2026, 11, 8)   // الأحد
    private val friday = LocalDate.of(2026, 11, 13)  // الجمعة

    @Test
    fun holidayWinsOverEverything() {
        assertEquals(DayType.HOLIDAY, DayTypeResolver.resolve(sunday, listOf(lesson("1", 480, 540)), true))
    }

    @Test
    fun lessonsOnlyIsWorkDay() {
        assertEquals(DayType.WORK, DayTypeResolver.resolve(sunday, listOf(lesson("1", 480, 540)), false))
    }

    @Test
    fun lessonPlusMeetingIsMixed() {
        val items = listOf(lesson("1", 480, 540), item("m", AgendaKind.MEETING, 600, 660))
        assertEquals(DayType.MIXED, DayTypeResolver.resolve(sunday, items, false))
    }

    @Test
    fun meetingOnlyAndSeminarOnly() {
        assertEquals(DayType.MEETING, DayTypeResolver.resolve(sunday, listOf(item("m", AgendaKind.MEETING, 600, 660)), false))
        assertEquals(DayType.SEMINAR, DayTypeResolver.resolve(sunday, listOf(item("s", AgendaKind.SEMINAR, 600, 660)), false))
    }

    @Test
    fun emptyDayDependsOnWorkWeek() {
        assertEquals(DayType.REST, DayTypeResolver.resolve(friday, emptyList(), false))
        assertEquals(DayType.UNSPECIFIED, DayTypeResolver.resolve(sunday, emptyList(), false))
    }

    @Test
    fun personalOnlyOnWorkDayStaysUnspecified() {
        assertEquals(
            DayType.UNSPECIFIED,
            DayTypeResolver.resolve(sunday, listOf(item("p", AgendaKind.PERSONAL, 600, 660)), false),
        )
    }

    // ---------- TimeFormat ----------

    @Test
    fun formatsMinutesAndCountdown() {
        assertEquals("08:05", TimeFormat.minutes(485))
        assertEquals("00:41:12", TimeFormat.countdown(41 * 60 + 12))
        assertEquals("00:00:00", TimeFormat.countdown(-5))
        assertEquals("1 س 30 د", TimeFormat.durationLabel(90))
        assertEquals("45 د", TimeFormat.durationLabel(45))
        assertEquals("2 س", TimeFormat.durationLabel(120))
    }

    @Test
    fun parsesTimesIncludingArabicIndicDigits() {
        assertEquals(8 * 60 + 30, TimeFormat.parseMinutes("08:30"))
        assertEquals(8 * 60 + 30, TimeFormat.parseMinutes("8:30"))
        assertEquals(8 * 60 + 30, TimeFormat.parseMinutes("٠٨:٣٠"))
        assertEquals(13 * 60, TimeFormat.parseMinutes(" 13.00 "))
        assertNull(TimeFormat.parseMinutes("24:00"))
        assertNull(TimeFormat.parseMinutes("8:60"))
        assertNull(TimeFormat.parseMinutes("abc"))
        assertNull(TimeFormat.parseMinutes(""))
    }

    // ---------- ArabicDates ----------

    @Test
    fun arabicDayAndAlgerianMonthNames() {
        assertEquals("الأربعاء", ArabicDates.dayName(LocalDate.of(2026, 11, 11)))
        assertEquals("11 نوفمبر 2026", ArabicDates.gregorian(LocalDate.of(2026, 11, 11)))
        assertEquals("5 جويلية 2026", ArabicDates.gregorian(LocalDate.of(2026, 7, 5)))
        assertEquals("1 جانفي 2027", ArabicDates.gregorian(LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun hijriDateIsProducedForSupportedRange() {
        val h = ArabicDates.hijri(LocalDate.of(2025, 3, 1))
        assertNotNull(h)
        assertEquals("1 رمضان 1446 هـ", h)
        assertTrue(ArabicDates.hijri(LocalDate.of(2026, 11, 11))!!.endsWith("هـ"))
    }

    // ---------- AppointmentType ----------

    @Test
    fun appointmentTypesMapToAgendaKinds() {
        assertEquals(AgendaKind.MEETING, AppointmentType.fromName("MEETING").agendaKind)
        assertEquals(AgendaKind.SEMINAR, AppointmentType.fromName("FORUM").agendaKind)
        assertEquals(AgendaKind.ASSESSMENT, AppointmentType.fromName("EXAM").agendaKind)
        assertEquals(AppointmentType.OTHER, AppointmentType.fromName("nonsense"))
        assertEquals(AppointmentType.OTHER, AppointmentType.fromName(null))
    }
}
