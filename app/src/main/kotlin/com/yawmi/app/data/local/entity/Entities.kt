package com.yawmi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * قواعد عامة لكل الكيانات (مهمة للمزامنة في المرحلة 6):
 *  - المعرّف String (UUID) يُولَّد على الجهاز، فلا تتصادم المعرّفات بين جهازين.
 *  - createdAt / updatedAt بالميلي ثانية؛ deletedAt != null يعني «محذوف» (حذف ناعم) كي ينتقل الحذف بالمزامنة.
 *  - التواريخ تُخزَّن نصاً ISO (yyyy-MM-dd) والأوقات دقائق منذ منتصف الليل (Int) — بلا محوّلات أنواع.
 *  - القيم المعدودة (enum) تُخزَّن نصاً باسمها.
 *  - لا نخزّن «رقم المرحلة الحالية»: الحالة تُشتق من سجل LessonActionEntity (انظر :sequence-engine).
 */

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String? = null,
    /** معرّف Firebase Auth بعد ربط الحساب (المرحلة 6). */
    val authUid: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "teacher_profiles",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class TeacherProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fullName: String = "",
    /** PRIMARY | MIDDLE | SECONDARY */
    val educationLevel: String = "MIDDLE",
    val specialty: String = "",
    val wilaya: String = "",
    /** أيام العمل كأسماء java.time.DayOfWeek مفصولة بفواصل. */
    val workDays: String = "SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY",
    /** مدة الاستعداد قبل أول نشاط (لمنبه الاستيقاظ). */
    val prepMinutes: Int = 60,
    val activeDurationProfileId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "institutions")
data class InstitutionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val wilaya: String = "",
    val educationLevel: String = "MIDDLE",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** جدول أسبوعي واحد (يمكن تعدد الجداول عند العمل في أكثر من مؤسسة). */
@Entity(
    tableName = "schedule_profiles",
    foreignKeys = [ForeignKey(InstitutionEntity::class, ["id"], ["institutionId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("institutionId"), Index("isActive"), Index("workProfileId")],
)
data class ScheduleProfileEntity(
    @PrimaryKey val id: String,
    val institutionId: String? = null,
    /** ملف العمل الذي يتبعه هذا الجدول (v2). */
    val workProfileId: String? = null,
    val name: String,
    val isActive: Boolean = true,
    val validFromIso: String? = null,
    val validToIso: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * نظام مدة الحصة (عادي 60 / خاص 45 / مخصص). غير مرتبط بشهر أو مناسبة.
 * بدايته ونهايته اختياريتان، ويمكن تفعيله في أي وقت من السنة.
 */
@Entity(tableName = "duration_profiles")
data class DurationProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val minutes: Int,
    val startDateIso: String? = null,
    val endDateIso: String? = null,
    val isActive: Boolean = false,
    val isBuiltIn: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** حصة/استراحة/اجتماع/نشاط متكرر أسبوعياً. dayOfWeek وفق java.time (الاثنين=1 … الأحد=7). */
@Entity(
    tableName = "schedule_items",
    foreignKeys = [ForeignKey(ScheduleProfileEntity::class, ["id"], ["scheduleProfileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("scheduleProfileId"), Index("dayOfWeek")],
)
data class ScheduleItemEntity(
    @PrimaryKey val id: String,
    val scheduleProfileId: String,
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    /** LESSON | BREAK | MEETING | SEMINAR | ACTIVITY | OTHER (قيم AgendaKind) */
    val kind: String = "LESSON",
    val title: String? = null,
    val subject: String? = null,
    val classGroupId: String? = null,
    val lessonTypeId: String? = null,
    /** true = موعد ثابت لا يُعاد حسابه عند تغيير نظام مدة الحصة (اجتماعات/ندوات). */
    val durationLocked: Boolean = false,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** تعديل حصة واحدة في تاريخ محدد دون المساس بالجدول المتكرر. */
@Entity(
    tableName = "schedule_overrides",
    foreignKeys = [ForeignKey(ScheduleItemEntity::class, ["id"], ["scheduleItemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("scheduleItemId"), Index("dateIso")],
)
data class ScheduleOverrideEntity(
    @PrimaryKey val id: String,
    val scheduleItemId: String,
    val dateIso: String,
    /** MODIFIED | CANCELLED */
    val action: String,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** مكتبة أنواع الحصص (قابلة للتخصيص بلا حد). */
@Entity(tableName = "lesson_types")
data class LessonTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val colorHex: String = "#A9CDAE",
    val iconKey: String = "book",
    val defaultMinutes: Int? = null,
    /** EDUCATIONAL | EVALUATIVE | ADMINISTRATIVE */
    val category: String = "EDUCATIONAL",
    /** هل النوع جزء من التسلسل التعليمي؟ الفرض/الاختبار/الاجتماع: false دائماً افتراضياً. */
    val countsInSequence: Boolean = false,
    val needsAssessment: Boolean = false,
    val sortOrder: Int = 0,
    val isBuiltIn: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** قالب تسلسل (3/4/5 مراحل أو مخصص). المراحل JSON: [{"id":"..","name":".."}] */
@Entity(tableName = "sequence_templates")
data class SequenceTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stagesJson: String,
    val isBuiltIn: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "class_groups",
    foreignKeys = [ForeignKey(InstitutionEntity::class, ["id"], ["institutionId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("institutionId")],
)
data class ClassGroupEntity(
    @PrimaryKey val id: String,
    val institutionId: String? = null,
    val name: String,
    val level: String = "",
    val studentCount: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * تسلسل بيداغوجي مستقل = (قسم × مادة). يحتفظ بنسخة (Snapshot) من مراحل القالب
 * فلا يتأثر تقدمه إذا عُدّل القالب لاحقاً.
 */
@Entity(
    tableName = "subject_sequences",
    foreignKeys = [ForeignKey(ClassGroupEntity::class, ["id"], ["classGroupId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("classGroupId"), Index(value = ["classGroupId", "subject"], unique = true)],
)
data class SubjectSequenceEntity(
    @PrimaryKey val id: String,
    val classGroupId: String,
    val subject: String,
    val templateId: String? = null,
    val stagesSnapshotJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** الحصة المنفذة فعلياً — منفصلة عن الحصة المجدولة. */
@Entity(
    tableName = "lesson_sessions",
    indices = [Index("dateIso"), Index("classGroupId"), Index("scheduleItemId")],
)
data class LessonSessionEntity(
    @PrimaryKey val id: String,
    val scheduleItemId: String? = null,
    val classGroupId: String? = null,
    val subject: String? = null,
    val dateIso: String,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    /** PLANNED | HELD | NOT_HELD */
    val status: String = "PLANNED",
    val actualMinutes: Int? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * سجل عمليات التسلسل (مصدر الحقيقة). للإلحاق فقط: لا تعديل ولا حذف.
 * التراجع حدث جديد (type = UNDO) يشير إلى هدفه داخل payloadJson.
 * المعرّف الفريد + OnConflict.IGNORE = منع التكرار عند المزامنة بين جهازين.
 */
@Entity(
    tableName = "lesson_actions",
    foreignKeys = [ForeignKey(SubjectSequenceEntity::class, ["id"], ["subjectSequenceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("subjectSequenceId"), Index("timestampMillis")],
)
data class LessonActionEntity(
    @PrimaryKey val id: String,
    val subjectSequenceId: String,
    val sessionId: String? = null,
    val deviceId: String,
    val timestampMillis: Long,
    /** COMPLETE | MERGE | REPEAT | EXTEND | SKIP | NOT_HELD | ASSESSMENT | REORDER | NEW_CYCLE | UNDO */
    val type: String,
    val payloadJson: String = "{}",
    val createdAt: Long,
)

/** فرض/اختبار — منفصل عن مراحل التسلسل؛ يرتبط بإجراء فقط إذا اختار الأستاذ أن يستهلك مرحلة. */
@Entity(
    tableName = "assessment_events",
    indices = [Index("subjectSequenceId"), Index("dateIso")],
)
data class AssessmentEventEntity(
    @PrimaryKey val id: String,
    val subjectSequenceId: String? = null,
    val classGroupId: String? = null,
    /** ASSIGNMENT | EXAM | OTHER */
    val kind: String,
    val title: String = "",
    val dateIso: String,
    val consumesStage: Boolean = false,
    val actionId: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "appointments", indices = [Index("dateIso")])
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** قيم AppointmentType */
    val type: String,
    val dateIso: String,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val location: String? = null,
    val notes: String? = null,
    val attachmentsJson: String = "[]",
    /** دقائق التذكير المسبق مفصولة بفواصل، مثل "60,15". */
    val reminderMinutes: String = "",
    /** PENDING | DONE */
    val status: String = "PENDING",
    /** MANUAL | OCR */
    val source: String = "MANUAL",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "holidays", indices = [Index("startDateIso"), Index("endDateIso")])
data class HolidayEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** OFFICIAL | RELIGIOUS | SCHOOL | SUMMER | EXCEPTIONAL | PERSONAL */
    val category: String,
    val startDateIso: String,
    val endDateIso: String,
    /** false = تاريخ تقريبي ينتظر الإعلان الرسمي (خصوصاً المناسبات الدينية). */
    val isConfirmed: Boolean = false,
    val source: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "tasks", indices = [Index("isDone"), Index("dueDateIso")])
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val dueDateIso: String? = null,
    val dueMinutes: Int? = null,
    /** 0 عادية، 1 مهمة، 2 عاجلة */
    val priority: Int = 0,
    val category: String? = null,
    val isDone: Boolean = false,
    val reminderMinutes: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "notes", indices = [Index("linkedType", "linkedId")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String? = null,
    val body: String,
    /** APPOINTMENT | CLASS_GROUP | SESSION | null */
    val linkedType: String? = null,
    val linkedId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** تعريف منبه/تنبيه (التنفيذ الفعلي بـ AlarmManager في المرحلة 5). */
@Entity(tableName = "notifications", indices = [Index("triggerAtMillis"), Index("targetType", "targetId")])
data class NotificationEntity(
    @PrimaryKey val id: String,
    /** WAKE_UP | BEFORE_WORK | BEFORE_LESSON | LESSON_END | NEXT_LESSON | MEETING | SEMINAR | ASSESSMENT | TASK */
    val kind: String,
    val targetType: String? = null,
    val targetId: String? = null,
    val label: String? = null,
    val triggerAtMillis: Long? = null,
    val timeMinutes: Int? = null,
    val repeatDays: String = "",
    val leadMinutes: Int = 0,
    val soundUri: String? = null,
    val snoozeMinutes: Int = 5,
    val enabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(tableName = "backup_records")
data class BackupRecordEntity(
    @PrimaryKey val id: String,
    /** AUTO | MANUAL */
    val kind: String,
    val filePath: String? = null,
    val sizeBytes: Long = 0,
    val appVersion: String,
    val dbVersion: Int,
    val checksum: String? = null,
    val createdAt: Long,
)
