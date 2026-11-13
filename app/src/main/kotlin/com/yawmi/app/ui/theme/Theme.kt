package com.yawmi.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val YawmiLightColors = lightColorScheme(
    primary = YawmiColors.GreenDark,
    onPrimary = YawmiColors.GoldLight,
    primaryContainer = YawmiColors.GreenLight,
    onPrimaryContainer = YawmiColors.GreenDark,
    secondary = YawmiColors.SandBrown,
    onSecondary = YawmiColors.GreenDark,
    secondaryContainer = YawmiColors.GoldLight,
    onSecondaryContainer = YawmiColors.GreenDark,
    tertiary = YawmiColors.ComfortBlue,
    onTertiary = YawmiColors.GreenDark,
    tertiaryContainer = YawmiColors.ComfortBlue,
    onTertiaryContainer = YawmiColors.GreenDark,
    background = YawmiColors.Background,
    onBackground = YawmiColors.TextPrimary,
    surface = Color.White,
    onSurface = YawmiColors.TextPrimary,
    surfaceVariant = YawmiColors.Grey,
    onSurfaceVariant = YawmiColors.TextSecondary,
    outline = Color(0xFF8A958F),
    outlineVariant = YawmiColors.Border,
    error = YawmiColors.Error,
)

/**
 * الثيم الرئيسي. اتجاه التخطيط RTL مفروض دائماً (المنتج عربي أولاً).
 * خط النظام يدعم العربية؛ خط عربي مخصص يُضاف لاحقاً مع تصدير PDF (المرحلة 5).
 */
@Composable
fun YawmiTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = YawmiLightColors,
            typography = yawmiTypography(YawmiFonts.NotoSansArabic),
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(YawmiSpacing.cardRadius),
                large = RoundedCornerShape(20.dp),
            ),
            content = content,
        )
    }
}

/** سلّم الخطوط: عنوان صفحة 24، قسم 18، بطاقة 16، نص 14–16، ثانوي 12–13. */
private fun yawmiTypography(family: FontFamily): Typography {
    fun style(size: Int, weight: FontWeight = FontWeight.Normal, line: Int = (size * 1.5).toInt()) =
        TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp)
    return Typography(
        displayLarge = style(40, FontWeight.Bold), displayMedium = style(34, FontWeight.Bold), displaySmall = style(30, FontWeight.Bold),
        headlineLarge = style(28, FontWeight.Bold), headlineMedium = style(28, FontWeight.Bold), headlineSmall = style(24, FontWeight.Bold),
        titleLarge = style(20, FontWeight.Bold), titleMedium = style(18, FontWeight.Medium), titleSmall = style(16, FontWeight.Medium),
        bodyLarge = style(16), bodyMedium = style(14), bodySmall = style(13),
        labelLarge = style(14, FontWeight.Medium), labelMedium = style(13, FontWeight.Medium), labelSmall = style(12, FontWeight.Medium),
    )
}
