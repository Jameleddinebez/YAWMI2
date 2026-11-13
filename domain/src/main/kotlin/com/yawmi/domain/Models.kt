package com.yawmi.domain

import java.time.DayOfWeek

/** حالة اليوم — تحدد لون بطاقة الرأس في الصفحة الرئيسية. */
enum class DayType(val arabicLabel: String) {
    WORK("يوم عمل"),
    REST("يوم راحة"),
    MEETING("اجتماع"),
    SEMINAR("ندوة / ملتقى"),
    MIXED("يوم مختلط"),
    HOLIDAY("عطلة"),
    UNSPECIFIED("غير محدد"),
}

enum class AgendaKind { LESSON, BREAK, MEETING, SEMINAR, ASSESSMENT, ACTIVITY, PERSONAL, OTHER }

enum class AgendaSource { SCHEDULE, APPOINTMENT }

/** عنصر في أجندة اليوم، سواء جاء من الجدول الأسبوعي أو من المواعيد. الأوقات بالدقائق منذ منتصف الليل. */
data class AgendaItem(
    val id: String,
    val title: String,
    val kind: AgendaKind,
    val startMinutes: Int?,
    val endMinutes: Int?,
    val source: AgendaSource,
    val subtitle: String? = null,
) {
    val isTimed: Boolean get() = startMinutes != null && endMinutes != null

    /** هل يُحتسب ضمن ساعات العمل؟ الاستراحات والمواعيد الشخصية لا تُحتسب. */
    val countsAsWork: Boolean get() = kind != AgendaKind.BREAK && kind != AgendaKind.PERSONAL
}

/** أنواع المواعيد (القائمة الأولية — قابلة للتوسعة بأنواع يضيفها المستخدم في مرحلة لاحقة). */
enum class AppointmentType(val arabicLabel: String, val agendaKind: AgendaKind) {
    HOMEWORK("فرض", AgendaKind.ASSESSMENT),
    EXAM("اختبار", AgendaKind.ASSESSMENT),
    GRADING("تصحيح", AgendaKind.ASSESSMENT),
    MEETING("اجتماع", AgendaKind.MEETING),
    SEMINAR("ندوة", AgendaKind.SEMINAR),
    CONFERENCE("ملتقى", AgendaKind.SEMINAR),
    FORUM("منتدى", AgendaKind.SEMINAR),
    SCHOOL_ACTIVITY("نشاط مدرسي", AgendaKind.ACTIVITY),
    TRIP("رحلة", AgendaKind.ACTIVITY),
    COMPETITION("مسابقة", AgendaKind.ACTIVITY),
    EXHIBITION("معرض", AgendaKind.ACTIVITY),
    PERSONAL("موعد شخصي", AgendaKind.PERSONAL),
    OTHER("أخرى", AgendaKind.OTHER);

    companion object {
        fun fromName(name: String?): AppointmentType =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

data class TaskItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val dueDateIso: String? = null,
    /** 0 = عادية، 1 = مهمة، 2 = عاجلة */
    val priority: Int = 0,
    val isDone: Boolean = false,
)

/** أيام العمل. الافتراضي في الجزائر: من الأحد إلى الخميس. قابل للتعديل من الإعدادات (المرحلة 2). */
object WorkWeek {
    val DEFAULT: Set<DayOfWeek> = setOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    )
}
