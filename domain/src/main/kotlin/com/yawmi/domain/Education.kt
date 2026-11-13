package com.yawmi.domain

enum class EducationLevel(val arabicLabel: String) {
    MIDDLE("التعليم المتوسط"),
    SECONDARY("التعليم الثانوي");

    companion object {
        fun fromName(name: String?): EducationLevel? = entries.firstOrNull { it.name == name }
    }
}

data class SubjectSeed(
    val id: String,
    val nameAr: String,
    val nameFr: String?,
    val level: EducationLevel,
)

/**
 * مكتبة المواد الأولية.
 *
 * تنبيه مهم: هذه القائمة **مسودة غير موثّقة رسمياً**. لم يكن الوصول إلى موقع وزارة التربية ممكناً أثناء البناء
 * (الموقع يمنع الوصول الآلي)، ولم تُطابَق القائمة مع نص رسمي نافذ. لذلك:
 *  - كل مادة تُحفظ بـ isVerified = false ومصدر «مسودة»؛
 *  - الأستاذ يضيف ما ينقص ويعدّل ما يلزم؛
 *  - في الثانوي لا تُربَط المواد بسنة أو شعبة، لأن الشعب والمواد تغيّرت وتتغير؛ يُترك التقييد للمستخدم حتى توفر مصدر رسمي.
 * رقم الإصدار يتيح استبدال القائمة لاحقاً دون المساس بما اختاره الأساتذة.
 */
object SubjectCatalogSeed {
    const val CATALOG_VERSION = "draft-2026.1"
    const val ACADEMIC_YEAR = "2026-2027"
    const val SOURCE_NOTE = "مسودة أولية غير موثّقة رسمياً — راجعها وعدّلها"

    val middle: List<SubjectSeed> = listOf(
        SubjectSeed("sub_mid_arabic", "اللغة العربية", "Langue arabe", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_tamazight", "اللغة الأمازيغية", "Tamazight", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_french", "اللغة الفرنسية", "Français", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_english", "اللغة الإنجليزية", "Anglais", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_math", "الرياضيات", "Mathématiques", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_natural", "العلوم الطبيعية والحياة", "Sciences naturelles", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_physics", "العلوم الفيزيائية والتكنولوجيا", "Sciences physiques et technologie", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_it", "الإعلام الآلي", "Informatique", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_histgeo", "التاريخ والجغرافيا", "Histoire-Géographie", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_islamic", "التربية الإسلامية", "Éducation islamique", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_civic", "التربية المدنية", "Éducation civique", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_art", "التربية التشكيلية", "Éducation artistique", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_music", "التربية الموسيقية", "Éducation musicale", EducationLevel.MIDDLE),
        SubjectSeed("sub_mid_pe", "التربية البدنية والرياضية", "EPS", EducationLevel.MIDDLE),
    )

    val secondary: List<SubjectSeed> = listOf(
        SubjectSeed("sub_sec_arabic", "اللغة العربية وآدابها", "Langue et littérature arabes", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_tamazight", "اللغة الأمازيغية", "Tamazight", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_french", "اللغة الفرنسية", "Français", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_english", "اللغة الإنجليزية", "Anglais", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_german", "اللغة الألمانية", "Allemand", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_spanish", "اللغة الإسبانية", "Espagnol", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_italian", "اللغة الإيطالية", "Italien", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_math", "الرياضيات", "Mathématiques", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_physics", "العلوم الفيزيائية", "Sciences physiques", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_natural", "علوم الطبيعة والحياة", "Sciences naturelles", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_islamic", "العلوم الإسلامية", "Sciences islamiques", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_philosophy", "الفلسفة", "Philosophie", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_histgeo", "التاريخ والجغرافيا", "Histoire-Géographie", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_it", "الإعلام الآلي", "Informatique", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_civil", "الهندسة المدنية", "Génie civil", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_electrical", "الهندسة الكهربائية", "Génie électrique", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_mechanical", "الهندسة الميكانيكية", "Génie mécanique", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_process", "هندسة الطرائق", "Génie des procédés", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_economy", "الاقتصاد والمناجمنت", "Économie et management", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_accounting", "التسيير المحاسبي والمالي", "Gestion comptable et financière", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_law", "القانون", "Droit", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_pe", "التربية البدنية والرياضية", "EPS", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_art", "التربية التشكيلية", "Arts plastiques", EducationLevel.SECONDARY),
        SubjectSeed("sub_sec_music", "التربية الموسيقية", "Éducation musicale", EducationLevel.SECONDARY),
    )

    fun forLevel(level: EducationLevel): List<SubjectSeed> =
        if (level == EducationLevel.MIDDLE) middle else secondary

    fun all(): List<SubjectSeed> = middle + secondary
}
