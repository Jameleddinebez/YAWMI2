package com.yawmi.app.data.repository

import com.yawmi.app.data.local.YawmiDatabase
import com.yawmi.app.data.local.entity.DurationProfileEntity
import com.yawmi.app.data.local.entity.LessonTypeEntity
import com.yawmi.app.data.local.entity.SequenceTemplateEntity
import com.yawmi.app.data.local.entity.SubjectCatalogEntity
import com.yawmi.domain.SubjectCatalogSeed
import com.yawmi.sequence.SequenceTemplates
import com.yawmi.sequence.StageDef
import org.json.JSONArray
import org.json.JSONObject

/**
 * يزرع «المكتبات الافتراضية» عند أول تشغيل (أنواع الحصص، أنظمة مدة الحصة، قوالب التسلسل).
 * هذه ليست بيانات المستخدم: هي نقاط انطلاق يعدّلها الأستاذ أو يحذفها.
 * المعرّفات ثابتة + IGNORE = لا تكرار حتى لو زُرعت على جهازين ثم تزامنا.
 */
class DefaultDataSeeder(private val db: YawmiDatabase) {

    suspend fun seedIfEmpty(now: Long = System.currentTimeMillis()) {
        if (db.lessonTypeDao().count() == 0) db.lessonTypeDao().insertAllIgnore(lessonTypes(now))
        if (db.durationProfileDao().count() == 0) db.durationProfileDao().insertAllIgnore(durationProfiles(now))
        if (db.sequenceTemplateDao().count() == 0) db.sequenceTemplateDao().insertAllIgnore(templates(now))
        if (db.subjectCatalogDao().count() == 0) db.subjectCatalogDao().insertAllIgnore(subjectCatalog(now))
    }

    /** مسودة غير موثّقة رسمياً (انظر SubjectCatalogSeed): isVerified=false دائماً. */
    private fun subjectCatalog(now: Long): List<SubjectCatalogEntity> = SubjectCatalogSeed.all().map {
        SubjectCatalogEntity(
            id = it.id, nameAr = it.nameAr, nameFr = it.nameFr, educationLevel = it.level.name,
            academicYear = SubjectCatalogSeed.ACADEMIC_YEAR, sourceNote = SubjectCatalogSeed.SOURCE_NOTE,
            catalogVersion = SubjectCatalogSeed.CATALOG_VERSION, isVerified = false, isCustom = false,
            createdAt = now, updatedAt = now,
        )
    }

    private fun lessonTypes(now: Long): List<LessonTypeEntity> {
        // (id, الاسم، التصنيف، ضمن التسلسل؟، يحتاج تقييماً؟، اللون، الأيقونة)
        data class Seed(
            val id: String, val name: String, val category: String,
            val inSequence: Boolean, val needsAssessment: Boolean, val color: String, val icon: String,
        )
        val seeds = listOf(
            Seed("lt_learning", "وضعية تعلمية", "EDUCATIONAL", true, false, "#A9CDAE", "lightbulb"),
            Seed("lt_simple", "وضعية بسيطة", "EDUCATIONAL", true, false, "#DCEBDD", "edit"),
            Seed("lt_integration", "وضعية إدماجية", "EDUCATIONAL", true, false, "#E9D77D", "merge"),
            Seed("lt_evaluation", "وضعية تقويمية", "EVALUATIVE", true, true, "#F3DDA4", "fact_check"),
            Seed("lt_practical", "حصة تطبيقية", "EDUCATIONAL", false, false, "#B9D8ED", "build"),
            Seed("lt_theory", "حصة نظرية", "EDUCATIONAL", false, false, "#C5A47E", "book"),
            Seed("lt_revision", "حصة مراجعة", "EDUCATIONAL", false, false, "#A9CDAE", "refresh"),
            Seed("lt_correction", "حصة تصحيح", "EDUCATIONAL", false, false, "#DCEBDD", "check"),
            Seed("lt_assignment", "فرض", "EVALUATIVE", false, true, "#E9D77D", "assignment"),
            Seed("lt_exam", "اختبار", "EVALUATIVE", false, true, "#F3DDA4", "quiz"),
        )
        return seeds.mapIndexed { index, s ->
            LessonTypeEntity(
                id = s.id, name = s.name, category = s.category, colorHex = s.color, iconKey = s.icon,
                countsInSequence = s.inSequence, needsAssessment = s.needsAssessment,
                sortOrder = index, isBuiltIn = true, createdAt = now, updatedAt = now,
            )
        }
    }

    /** عادي 60 / خاص 45. غير مرتبطين بأي شهر أو مناسبة. «المخصص» ينشئه الأستاذ بالمدة التي يريد. */
    private fun durationProfiles(now: Long): List<DurationProfileEntity> = listOf(
        DurationProfileEntity("dp_normal", "عادي", 60, isActive = true, isBuiltIn = true, createdAt = now, updatedAt = now),
        DurationProfileEntity("dp_special", "خاص", 45, isActive = false, isBuiltIn = true, createdAt = now, updatedAt = now),
    )

    private fun templates(now: Long): List<SequenceTemplateEntity> = listOf(
        SequenceTemplateEntity("tpl_3", "تسلسل من 3 مراحل", stagesToJson(SequenceTemplates.threeStages()), true, now, now),
        SequenceTemplateEntity("tpl_4", "تسلسل من 4 مراحل", stagesToJson(SequenceTemplates.fourStages()), true, now, now),
        SequenceTemplateEntity("tpl_5", "تسلسل من 5 مراحل", stagesToJson(SequenceTemplates.fiveStages()), true, now, now),
    )

    companion object {
        fun stagesToJson(stages: List<StageDef>): String {
            val arr = JSONArray()
            stages.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name)) }
            return arr.toString()
        }

        fun stagesFromJson(json: String): List<StageDef> {
            val arr = JSONArray(json)
            return List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                StageDef(o.getString("id"), o.getString("name"))
            }
        }
    }
}
