package com.yawmi.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** يستنتج حالة اليوم تلقائياً من بياناته. يستطيع المستخدم تجاوز النتيجة يدوياً. */
object DayTypeResolver {

    fun resolve(
        date: LocalDate,
        items: List<AgendaItem>,
        isHoliday: Boolean,
        workDays: Set<DayOfWeek> = WorkWeek.DEFAULT,
    ): DayType {
        if (isHoliday) return DayType.HOLIDAY

        val hasLesson = items.any { it.kind == AgendaKind.LESSON }
        val hasMeeting = items.any { it.kind == AgendaKind.MEETING }
        val hasSeminar = items.any { it.kind == AgendaKind.SEMINAR }
        val hasOtherWork = items.any {
            it.kind == AgendaKind.ASSESSMENT || it.kind == AgendaKind.ACTIVITY || it.kind == AgendaKind.OTHER
        }

        return when {
            hasLesson && (hasMeeting || hasSeminar) -> DayType.MIXED
            hasMeeting && hasSeminar -> DayType.MIXED
            hasLesson -> DayType.WORK
            hasSeminar -> DayType.SEMINAR
            hasMeeting -> DayType.MEETING
            hasOtherWork -> DayType.WORK
            date.dayOfWeek !in workDays -> DayType.REST
            else -> DayType.UNSPECIFIED
        }
    }
}
