package com.yawmi.domain

enum class Gender(val arabicLabel: String) {
    MALE("ذكر"),
    FEMALE("أنثى");

    companion object {
        fun fromName(name: String?): Gender? = entries.firstOrNull { it.name == name }
    }
}

enum class OnboardingError(val arabicMessage: String) {
    NAME_REQUIRED("أدخل اسمك"),
    LEVEL_REQUIRED("اختر الطور التعليمي"),
    SUBJECTS_REQUIRED("اختر مادة واحدة على الأقل"),
    INVALID_EMAIL("صيغة البريد الإلكتروني غير صحيحة"),
    INVALID_AGE("العمر غير صالح"),
    NO_WORK_DAYS("حدّد يوم عمل واحداً على الأقل"),
    WORK_CONFIG_INVALID("في نظام العمل إعدادات تحتاج إلى تصحيح"),
}

/** مسودة الإعداد الأولي: تُحفظ بعد كل تعديل ليُستأنف الإعداد من حيث توقف المستخدم. */
data class OnboardingDraft(
    val name: String = "",
    val level: EducationLevel? = null,
    val subjectIds: Set<String> = emptySet(),
    val institutionName: String = "",
    val email: String = "",
    val gender: Gender? = null,
    val age: String = "",
    val lessonMinutes: Int = 60,
    val breakMinutes: Int = 10,
    val week: List<DayWorkConfig> = WorkSystem.defaultWeek(),
)

object OnboardingValidator {
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** الإلزامي: الاسم والطور. الاختياري يُفحص فقط إن أُدخل. */
    fun infoErrors(d: OnboardingDraft): List<OnboardingError> {
        val out = mutableListOf<OnboardingError>()
        if (d.name.isBlank()) out += OnboardingError.NAME_REQUIRED
        if (d.level == null) out += OnboardingError.LEVEL_REQUIRED
        if (d.email.isNotBlank() && !emailRegex.matches(d.email.trim())) out += OnboardingError.INVALID_EMAIL
        if (d.age.isNotBlank()) {
            val a = d.age.trim().toIntOrNull()
            if (a == null || a !in 18..80) out += OnboardingError.INVALID_AGE
        }
        return out
    }

    fun subjectErrors(d: OnboardingDraft): List<OnboardingError> =
        if (d.subjectIds.isEmpty()) listOf(OnboardingError.SUBJECTS_REQUIRED) else emptyList()

    fun workErrors(d: OnboardingDraft): List<OnboardingError> {
        val out = mutableListOf<OnboardingError>()
        if (d.week.none { it.mode != WorkMode.OFF }) out += OnboardingError.NO_WORK_DAYS
        if (!WorkSystem.durationValid(d.lessonMinutes, d.breakMinutes) ||
            d.week.any { WorkSystem.issues(it, d.lessonMinutes, d.breakMinutes).isNotEmpty() }
        ) {
            out += OnboardingError.WORK_CONFIG_INVALID
        }
        return out
    }
}
