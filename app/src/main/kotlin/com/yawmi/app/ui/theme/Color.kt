package com.yawmi.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.yawmi.domain.DayType

/** الألوان المعتمدة في هوية YAWMI. */
object YawmiColors {
    val GreenDark = Color(0xFF183D35)
    val GoldLight = Color(0xFFF3DDA4)
    val GreenLight = Color(0xFFDCEBDD)
    val GreenMedium = Color(0xFFA9CDAE)
    val WarmYellow = Color(0xFFE9D77D)
    val ComfortBlue = Color(0xFFB9D8ED)
    val SandBrown = Color(0xFFC5A47E)
    val Grey = Color(0xFFE8E8E8)
    val Background = Color(0xFFF7F8F5)
    val TextPrimary = Color(0xFF1F2D29)
    val TextSecondary = Color(0xFF69746F)
    val Border = Color(0xFFE6E8E2)
    val Error = Color(0xFFB3261E)
    val Success = Color(0xFF2E7D52)
    val Warning = Color(0xFFA56A13)
}

/**
 * لون بطاقة رأس الصفحة الرئيسية بحسب حالة اليوم فقط.
 * باقي عناصر الواجهة (التنقل، الأزرار، الشعار) ثابتة كي لا تتغير الهوية عشوائياً.
 */
fun DayType.containerColor(): Color = when (this) {
    DayType.WORK -> YawmiColors.GreenLight
    DayType.REST -> YawmiColors.ComfortBlue
    DayType.MEETING -> YawmiColors.GoldLight
    DayType.SEMINAR -> YawmiColors.WarmYellow
    DayType.MIXED -> YawmiColors.GreenMedium
    DayType.HOLIDAY -> YawmiColors.SandBrown
    DayType.UNSPECIFIED -> YawmiColors.Grey
}
