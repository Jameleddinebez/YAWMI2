package com.yawmi.domain

import java.time.DayOfWeek

enum class WorkMode(val arabicLabel: String) {
    OFF("راحة"),
    MORNING("صباحاً"),
    EVENING("مساءً"),
    BOTH("صباحاً ومساءً");

    val hasMorning: Boolean get() = this == MORNING || this == BOTH
    val hasEvening: Boolean get() = this == EVENING || this == BOTH

    companion object {
        fun fromName(name: String?): WorkMode = entries.firstOrNull { it.name == name } ?: OFF
    }
}

/** إعداد عمل يوم واحد. الأوقات بالدقائق منذ منتصف الليل. لكل يوم إعداده المستقل (لا جدول موحّد مفروض). */
data class DayWorkConfig(
    val day: DayOfWeek,
    val mode: WorkMode,
    val morningStart: Int = 8 * 60,
    val morningPeriods: Int = 0,
    val eveningStart: Int = 13 * 60,
    val eveningPeriods: Int = 0,
)

data class Period(val index: Int, val startMinutes: Int, val endMinutes: Int)

enum class WorkConfigIssue(val arabicMessage: String) {
    INVALID_DURATION("مدة الحصة أو الاستراحة غير صالحة"),
    MORNING_PERIODS_REQUIRED("حدّد عدد الحصص الصباحية (1 على الأقل)"),
    EVENING_PERIODS_REQUIRED("حدّد عدد الحصص المسائية (1 على الأقل)"),
    MORNING_EXCEEDS_DAY("الفترة الصباحية تتجاوز منتصف الليل"),
    EVENING_EXCEEDS_DAY("الفترة المسائية تتجاوز منتصف الليل"),
    SESSIONS_OVERLAP("الفترة الصباحية تتداخل مع المسائية"),
}

object WorkSystem {
    const val MIN_LESSON = 10
    const val MAX_LESSON = 240
    const val MAX_BREAK = 120
    const val MAX_PERIODS = 12
    private const val DAY_END = 24 * 60

    fun durationValid(lessonMinutes: Int, breakMinutes: Int): Boolean =
        lessonMinutes in MIN_LESSON..MAX_LESSON && breakMinutes in 0..MAX_BREAK

    /** أوقات حصص فترة واحدة: البداية، ثم حصة + استراحة، وهكذا (بلا استراحة بعد الأخيرة). */
    fun periods(startMinutes: Int, count: Int, lessonMinutes: Int, breakMinutes: Int): List<Period> =
        List(count.coerceAtLeast(0)) { i ->
            val s = startMinutes + i * (lessonMinutes + breakMinutes)
            Period(i + 1, s, s + lessonMinutes)
        }

    fun morningPeriods(cfg: DayWorkConfig, lesson: Int, brk: Int): List<Period> =
        if (cfg.mode.hasMorning) periods(cfg.morningStart, cfg.morningPeriods, lesson, brk) else emptyList()

    fun eveningPeriods(cfg: DayWorkConfig, lesson: Int, brk: Int): List<Period> =
        if (cfg.mode.hasEvening) periods(cfg.eveningStart, cfg.eveningPeriods, lesson, brk) else emptyList()

    fun issues(cfg: DayWorkConfig, lessonMinutes: Int, breakMinutes: Int): List<WorkConfigIssue> {
        if (cfg.mode == WorkMode.OFF) return emptyList()
        if (!durationValid(lessonMinutes, breakMinutes)) return listOf(WorkConfigIssue.INVALID_DURATION)
        val out = mutableListOf<WorkConfigIssue>()
        val morning = morningPeriods(cfg, lessonMinutes, breakMinutes)
        val evening = eveningPeriods(cfg, lessonMinutes, breakMinutes)
        if (cfg.mode.hasMorning) {
            if (cfg.morningPeriods !in 1..MAX_PERIODS) out += WorkConfigIssue.MORNING_PERIODS_REQUIRED
            else if (morning.last().endMinutes > DAY_END) out += WorkConfigIssue.MORNING_EXCEEDS_DAY
        }
        if (cfg.mode.hasEvening) {
            if (cfg.eveningPeriods !in 1..MAX_PERIODS) out += WorkConfigIssue.EVENING_PERIODS_REQUIRED
            else if (evening.last().endMinutes > DAY_END) out += WorkConfigIssue.EVENING_EXCEEDS_DAY
        }
        if (cfg.mode == WorkMode.BOTH && morning.isNotEmpty() && evening.isNotEmpty() &&
            morning.last().endMinutes > evening.first().startMinutes
        ) {
            out += WorkConfigIssue.SESSIONS_OVERLAP
        }
        return out
    }

    /** أسبوع افتراضي قابل للتعديل: الأحد–الخميس صباحاً (5 حصص من 08:00)، والجمعة والسبت راحة. */
    fun defaultWeek(): List<DayWorkConfig> = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
    ).map { d ->
        if (d in WorkWeek.DEFAULT) DayWorkConfig(d, WorkMode.MORNING, morningPeriods = 5)
        else DayWorkConfig(d, WorkMode.OFF)
    }
}
