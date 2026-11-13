package com.yawmi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * كيانات الإصدار 2 من قاعدة البيانات (مضافة فوق v1 دون المساس بها، ويتصل بها Migration صريح).
 */

/**
 * «ملف عمل»: طور + مؤسسة + مواد + جدول + إعدادات عمل مستقلة.
 * يستطيع الأستاذ امتلاك عدة ملفات (مثلاً متوسطة وثانوية) دون خلط بياناتها.
 */
@Entity(
    tableName = "work_profiles",
    foreignKeys = [ForeignKey(InstitutionEntity::class, ["id"], ["institutionId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("institutionId")],
)
data class WorkProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** MIDDLE | SECONDARY */
    val educationLevel: String,
    val institutionId: String? = null,
    val durationProfileId: String? = null,
    val lessonMinutes: Int,
    val breakMinutes: Int,
    /** الأرشفة بدل الحذف: تبقى السجلات التاريخية. */
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * مكتبة المواد (مُصدَّرة بإصدار). المواد ليست ثابتة في الكود: الاستبدال بإصدار جديد لا يمس اختيارات الأساتذة
 * لأن teacher_subjects تشير إلى المعرّف الثابت. isVerified=false = مسودة غير مطابقة لنص رسمي.
 */
@Entity(tableName = "subject_catalog", indices = [Index("educationLevel")])
data class SubjectCatalogEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameFr: String? = null,
    val educationLevel: String,
    /** السنة الدراسية عند الحاجة (غير مقيّدة في المسودة). */
    val grade: String? = null,
    /** الشعبة عند الحاجة (غير مقيّدة في المسودة). */
    val stream: String? = null,
    val academicYear: String,
    val sourceNote: String,
    val catalogVersion: String,
    val isVerified: Boolean = false,
    /** مادة أضافها المستخدم بنفسه. */
    val isCustom: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

/** المواد التي يدرّسها الأستاذ داخل ملف عمل. حذف مادة من الكتالوج لا يمس السجلات (RESTRICT). */
@Entity(
    tableName = "teacher_subjects",
    foreignKeys = [
        ForeignKey(WorkProfileEntity::class, ["id"], ["workProfileId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(SubjectCatalogEntity::class, ["id"], ["subjectId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("workProfileId"),
        Index("subjectId"),
        Index(value = ["workProfileId", "subjectId"], unique = true),
    ],
)
data class TeacherSubjectEntity(
    @PrimaryKey val id: String,
    val workProfileId: String,
    val subjectId: String,
    /** لون المادة المخصص (اختياري) — يُستخدم لاحقاً في تصميم المادة. */
    val colorHex: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** إعداد عمل يوم من أيام الأسبوع داخل ملف عمل: راحة/صباحي/مسائي/الاثنان + بدايات وعدد الحصص. */
@Entity(
    tableName = "work_day_configs",
    foreignKeys = [ForeignKey(WorkProfileEntity::class, ["id"], ["workProfileId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("workProfileId"),
        Index(value = ["workProfileId", "dayOfWeek"], unique = true),
    ],
)
data class WorkDayConfigEntity(
    @PrimaryKey val id: String,
    val workProfileId: String,
    /** java.time.DayOfWeek.value (الاثنين=1 … الأحد=7) */
    val dayOfWeek: Int,
    /** OFF | MORNING | EVENING | BOTH */
    val mode: String,
    val morningStartMinutes: Int,
    val morningPeriods: Int,
    val eveningStartMinutes: Int,
    val eveningPeriods: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
