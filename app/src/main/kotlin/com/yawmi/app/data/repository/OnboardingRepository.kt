package com.yawmi.app.data.repository

import androidx.room.withTransaction
import com.yawmi.app.data.local.YawmiDatabase
import com.yawmi.app.data.local.entity.InstitutionEntity
import com.yawmi.app.data.local.entity.ScheduleProfileEntity
import com.yawmi.app.data.local.entity.SubjectCatalogEntity
import com.yawmi.app.data.local.entity.TeacherProfileEntity
import com.yawmi.app.data.local.entity.TeacherSubjectEntity
import com.yawmi.app.data.local.entity.UserEntity
import com.yawmi.app.data.local.entity.WorkDayConfigEntity
import com.yawmi.app.data.local.entity.WorkProfileEntity
import com.yawmi.domain.EducationLevel
import com.yawmi.domain.OnboardingDraft
import com.yawmi.domain.OnboardingValidator
import com.yawmi.domain.SubjectCatalogSeed
import com.yawmi.domain.WorkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OnboardingRepository(private val db: YawmiDatabase) {

    /** null = لم يُعرف بعد (أول قراءة)، false = يلزم الإعداد الأولي، true = اكتمل. */
    val isComplete: Flow<Boolean?> = db.workProfileDao().observeCount().map<Int, Boolean?> { it > 0 }

    fun subjectsFor(level: EducationLevel): Flow<List<SubjectCatalogEntity>> =
        db.subjectCatalogDao().observeByLevel(level.name)

    /** مادة يضيفها الأستاذ بنفسه (تُحفظ في الكتالوج بوسم isCustom). تعيد معرّفها أو null إن كان الاسم فارغاً. */
    suspend fun addCustomSubject(name: String, level: EducationLevel, now: Long = System.currentTimeMillis()): String? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val id = "sub_custom_" + UUID.randomUUID()
        db.subjectCatalogDao().insertIgnore(
            SubjectCatalogEntity(
                id = id, nameAr = clean, educationLevel = level.name,
                academicYear = SubjectCatalogSeed.ACADEMIC_YEAR, sourceNote = "أضافها المستخدم",
                catalogVersion = "user", isVerified = false, isCustom = true, createdAt = now, updatedAt = now,
            ),
        )
        return id
    }

    /**
     * ينشئ كل شيء في معاملة واحدة (ذرّياً): المستخدم، ملفه، المؤسسة (اختيارياً)، ملف العمل،
     * الجدول **الفارغ** (بلا حصص وهمية)، المواد، وإعدادات أيام العمل. إن فشل جزء لا يُحفظ شيء.
     */
    suspend fun complete(draft: OnboardingDraft, now: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
        val level = requireNotNull(draft.level) { "الطور غير محدد" }
        require(OnboardingValidator.infoErrors(draft).isEmpty()) { "بيانات المستخدم غير صالحة" }
        require(OnboardingValidator.subjectErrors(draft).isEmpty()) { "لم تُختر مواد" }
        require(OnboardingValidator.workErrors(draft).isEmpty()) { "نظام العمل غير صالح" }

        db.withTransaction {
            val userId = "local-user"
            db.userDao().upsert(
                UserEntity(userId, draft.name.trim(), draft.email.trim().ifBlank { null }, null, now, now),
            )
            val workDays = draft.week.filter { it.mode != WorkMode.OFF }.joinToString(",") { it.day.name }
            db.teacherProfileDao().upsert(
                TeacherProfileEntity(
                    id = "tp-local", userId = userId, fullName = draft.name.trim(), educationLevel = level.name,
                    workDays = workDays, createdAt = now, updatedAt = now,
                ),
            )

            val institutionId = draft.institutionName.trim().takeIf { it.isNotEmpty() }?.let { name ->
                val id = UUID.randomUUID().toString()
                db.institutionDao().upsert(
                    InstitutionEntity(id, name, educationLevel = level.name, createdAt = now, updatedAt = now),
                )
                id
            }

            val workProfileId = UUID.randomUUID().toString()
            val profileName = draft.institutionName.trim().ifBlank { "ملف ${level.arabicLabel}" }
            db.workProfileDao().upsert(
                WorkProfileEntity(
                    id = workProfileId, name = profileName, educationLevel = level.name, institutionId = institutionId,
                    lessonMinutes = draft.lessonMinutes, breakMinutes = draft.breakMinutes,
                    createdAt = now, updatedAt = now,
                ),
            )

            // الجدول الفارغ: صف الجدول فقط، لا ScheduleItems.
            db.scheduleProfileDao().upsert(
                ScheduleProfileEntity(
                    id = UUID.randomUUID().toString(), institutionId = institutionId, workProfileId = workProfileId,
                    name = "الجدول الأسبوعي", isActive = true, createdAt = now, updatedAt = now,
                ),
            )

            val single = draft.subjectIds.size == 1
            db.teacherSubjectDao().insertAllIgnore(
                draft.subjectIds.sorted().map { sid ->
                    TeacherSubjectEntity(
                        id = UUID.randomUUID().toString(), workProfileId = workProfileId, subjectId = sid,
                        isDefault = single, createdAt = now, updatedAt = now,
                    )
                },
            )

            db.workDayConfigDao().upsertAll(
                draft.week.map { c ->
                    WorkDayConfigEntity(
                        id = UUID.randomUUID().toString(), workProfileId = workProfileId, dayOfWeek = c.day.value,
                        mode = c.mode.name, morningStartMinutes = c.morningStart, morningPeriods = c.morningPeriods,
                        eveningStartMinutes = c.eveningStart, eveningPeriods = c.eveningPeriods,
                        createdAt = now, updatedAt = now,
                    )
                },
            )
        }
    }
}
