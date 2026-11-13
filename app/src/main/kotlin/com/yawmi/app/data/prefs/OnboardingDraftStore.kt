package com.yawmi.app.data.prefs

import android.content.Context
import com.yawmi.domain.DayWorkConfig
import com.yawmi.domain.EducationLevel
import com.yawmi.domain.Gender
import com.yawmi.domain.OnboardingDraft
import com.yawmi.domain.WorkMode
import com.yawmi.domain.WorkSystem
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

/** يحفظ تقدّم الإعداد الأولي (الخطوة + المسودة) ليُستأنف بعد إغلاق التطبيق. يُمسح عند الاكتمال. */
class OnboardingDraftStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("yawmi_onboarding", Context.MODE_PRIVATE)

    fun loadStep(): Int = sp.getInt(KEY_STEP, 0).coerceIn(0, 4)

    fun saveStep(step: Int) {
        sp.edit().putInt(KEY_STEP, step).apply()
    }

    fun save(d: OnboardingDraft) {
        val json = JSONObject()
            .put("name", d.name)
            .put("level", d.level?.name)
            .put("subjects", JSONArray(d.subjectIds.toList()))
            .put("institution", d.institutionName)
            .put("email", d.email)
            .put("gender", d.gender?.name)
            .put("age", d.age)
            .put("lesson", d.lessonMinutes)
            .put("break", d.breakMinutes)
            .put(
                "week",
                JSONArray().apply {
                    d.week.forEach { c ->
                        put(
                            JSONObject().put("day", c.day.value).put("mode", c.mode.name)
                                .put("ms", c.morningStart).put("mp", c.morningPeriods)
                                .put("es", c.eveningStart).put("ep", c.eveningPeriods),
                        )
                    }
                },
            )
        sp.edit().putString(KEY_DRAFT, json.toString()).apply()
    }

    fun load(): OnboardingDraft {
        val raw = sp.getString(KEY_DRAFT, null) ?: return OnboardingDraft()
        return try {
            val j = JSONObject(raw)
            val subjects = j.getJSONArray("subjects").let { a -> List(a.length()) { a.getString(it) }.toSet() }
            val weekArr = j.getJSONArray("week")
            val week = List(weekArr.length()) { i ->
                val o = weekArr.getJSONObject(i)
                DayWorkConfig(
                    day = DayOfWeek.of(o.getInt("day")), mode = WorkMode.fromName(o.getString("mode")),
                    morningStart = o.getInt("ms"), morningPeriods = o.getInt("mp"),
                    eveningStart = o.getInt("es"), eveningPeriods = o.getInt("ep"),
                )
            }
            OnboardingDraft(
                name = j.optString("name"),
                level = EducationLevel.fromName(j.optString("level").ifBlank { null }),
                subjectIds = subjects,
                institutionName = j.optString("institution"),
                email = j.optString("email"),
                gender = Gender.fromName(j.optString("gender").ifBlank { null }),
                age = j.optString("age"),
                lessonMinutes = j.optInt("lesson", 60),
                breakMinutes = j.optInt("break", 10),
                week = if (week.size == 7) week else WorkSystem.defaultWeek(),
            )
        } catch (e: Exception) {
            OnboardingDraft() // مسودة تالفة لا تُسقط التطبيق
        }
    }

    fun clear() {
        sp.edit().clear().apply()
    }

    private companion object {
        const val KEY_STEP = "step"
        const val KEY_DRAFT = "draft"
    }
}
