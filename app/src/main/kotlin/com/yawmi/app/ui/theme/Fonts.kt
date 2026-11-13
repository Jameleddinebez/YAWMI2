package com.yawmi.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.yawmi.app.R

object YawmiFonts {
    /** خط الواجهة: Noto Sans Arabic (رخصة OFL 1.1). */
    val NotoSansArabic = FontFamily(
        Font(R.font.noto_sans_arabic_regular, FontWeight.Normal),
        Font(R.font.noto_sans_arabic_medium, FontWeight.Medium),
        Font(R.font.noto_sans_arabic_bold, FontWeight.Bold),
    )
}

/**
 * الخط القرآني: إن وُجد الملف `res/font/quran_uthmanic.ttf` (مثلاً KFGQPC Uthmanic Script Hafs من مجمع الملك فهد)
 * يُستخدم تلقائياً؛ وإلا يُستخدم خط الواجهة مؤقتاً. الملف غير مضمَّن لأن مصدره الرسمي لا يمكن التحقق منه من بيئة البناء.
 * البحث بالاسم وقت التشغيل يجنّب كسر البناء عند غياب الملف.
 */
@Composable
fun rememberQuranFontFamily(): FontFamily {
    val context = LocalContext.current
    return remember {
        val id = context.resources.getIdentifier("quran_uthmanic", "font", context.packageName)
        if (id != 0) FontFamily(Font(id)) else YawmiFonts.NotoSansArabic
    }
}
