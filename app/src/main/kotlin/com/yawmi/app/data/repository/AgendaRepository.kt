package com.yawmi.app.data.repository

import com.yawmi.app.data.local.dao.AppointmentDao
import com.yawmi.app.data.local.dao.HolidayDao
import com.yawmi.app.data.local.dao.ScheduleItemDao
import com.yawmi.app.data.local.entity.AppointmentEntity
import com.yawmi.app.data.local.entity.ScheduleItemEntity
import com.yawmi.domain.AgendaCalculator
import com.yawmi.domain.AgendaItem
import com.yawmi.domain.AgendaKind
import com.yawmi.domain.AgendaSource
import com.yawmi.domain.AppointmentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.util.UUID

data class DayData(
    val date: LocalDate,
    val items: List<AgendaItem>,
    val holidayName: String?,
)

class AgendaRepository(
    private val appointmentDao: AppointmentDao,
    private val scheduleItemDao: ScheduleItemDao,
    private val holidayDao: HolidayDao,
) {
    /** أجندة يوم كاملة = الجدول الأسبوعي المتكرر + مواعيد ذلك التاريخ + العطلة إن وُجدت. */
    fun observeDay(date: LocalDate): Flow<DayData> = combine(
        appointmentDao.observeForDate(date.toString()),
        scheduleItemDao.observeActiveForDay(date.dayOfWeek.value),
        holidayDao.observeCovering(date.toString()),
    ) { appointments, scheduleItems, holidays ->
        val items = scheduleItems.map { it.toAgendaItem() } + appointments.map { it.toAgendaItem() }
        DayData(date, AgendaCalculator.sortedAgenda(items), holidays.firstOrNull()?.name)
    }

    suspend fun addAppointment(
        title: String,
        type: AppointmentType,
        date: LocalDate,
        startMinutes: Int?,
        endMinutes: Int?,
        now: Long = System.currentTimeMillis(),
    ) {
        appointmentDao.upsert(
            AppointmentEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                type = type.name,
                dateIso = date.toString(),
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}

internal fun ScheduleItemEntity.toAgendaItem(): AgendaItem {
    val agendaKind = AgendaKind.entries.firstOrNull { it.name == kind } ?: AgendaKind.OTHER
    val displayTitle = title?.takeIf { it.isNotBlank() }
        ?: subject?.takeIf { it.isNotBlank() }
        ?: when (agendaKind) {
            AgendaKind.LESSON -> "حصة"
            AgendaKind.BREAK -> "استراحة"
            AgendaKind.MEETING -> "اجتماع"
            else -> "نشاط"
        }
    return AgendaItem(
        id = "s-$id",
        title = displayTitle,
        kind = agendaKind,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        source = AgendaSource.SCHEDULE,
    )
}

internal fun AppointmentEntity.toAgendaItem(): AgendaItem {
    val t = AppointmentType.fromName(type)
    return AgendaItem(
        id = "a-$id",
        title = title,
        kind = t.agendaKind,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        source = AgendaSource.APPOINTMENT,
        subtitle = t.arabicLabel,
    )
}
