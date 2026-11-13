package com.yawmi.app.data.prefs

import android.content.Context
import com.yawmi.domain.DayType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** تفضيلات بسيطة محلية (SharedPreferences). تنتقل إلى DataStore/Room مع الإعدادات الكاملة في المرحلة 2. */
class UserPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("yawmi_prefs", Context.MODE_PRIVATE)

    private val _hijriEnabled = MutableStateFlow(sp.getBoolean(KEY_HIJRI, true))
    val hijriEnabled: StateFlow<Boolean> = _hijriEnabled.asStateFlow()

    fun setHijriEnabled(enabled: Boolean) {
        sp.edit().putBoolean(KEY_HIJRI, enabled).apply()
        _hijriEnabled.value = enabled
    }

    /** حقول اختيارية من الإعداد الأولي (تنتقل إلى Room مع شاشة الملف الشخصي لاحقاً). */
    fun saveProfileExtras(gender: String?, age: String?) {
        sp.edit().putString("profile_gender", gender).putString("profile_age", age).apply()
    }

    private val _dayTypeOverrides = MutableStateFlow(loadOverrides())

    /** تجاوزات يدوية لحالة اليوم، المفتاح تاريخ ISO. */
    val dayTypeOverrides: StateFlow<Map<String, DayType>> = _dayTypeOverrides.asStateFlow()

    fun setDayTypeOverride(date: LocalDate, type: DayType?) {
        val key = OVERRIDE_PREFIX + date.toString()
        val editor = sp.edit()
        if (type == null) editor.remove(key) else editor.putString(key, type.name)
        editor.apply()
        _dayTypeOverrides.value = loadOverrides()
    }

    private fun loadOverrides(): Map<String, DayType> {
        val result = mutableMapOf<String, DayType>()
        for ((key, value) in sp.all) {
            if (!key.startsWith(OVERRIDE_PREFIX) || value !is String) continue
            val type = DayType.entries.firstOrNull { it.name == value } ?: continue
            result[key.removePrefix(OVERRIDE_PREFIX)] = type
        }
        return result
    }

    private companion object {
        const val KEY_HIJRI = "hijri_enabled"
        const val OVERRIDE_PREFIX = "day_type_override_"
    }
}
