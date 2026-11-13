package com.yawmi.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yawmi.app.ui.theme.YawmiColors
import com.yawmi.app.ui.theme.rememberQuranFontFamily

/** النص كما ورد في المواصفات حرفياً (انظر docs/PHASE2_REPORT.md بخصوص الرسم العثماني). */
const val QURAN_VERSE = "وَمَا تَوْفِيقِيٓ إِلَّا بِاللَّهِ"
const val QURAN_REFERENCE = "سورة هود • الآية 88"

/**
 * إطار الآية: ثابت أعلى الصفحة (خارج منطقة التمرير)، مدمج الارتفاع، لا أنيميشن على النص.
 * أخضر داكن + حدود ذهبية رقيقة + نجمتان ثمانيتان هندسيتان على الطرفين.
 */
@Composable
fun QuranHeader(modifier: Modifier = Modifier) {
    val family = rememberQuranFontFamily()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(YawmiColors.GreenDark)
            .drawBehind { drawFrame() }
            .padding(horizontal = 44.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = QURAN_VERSE,
                style = TextStyle(
                    fontFamily = family, fontSize = 22.sp, lineHeight = 34.sp,
                    color = YawmiColors.GoldLight, textAlign = TextAlign.Center,
                ),
            )
            Text(
                text = QURAN_REFERENCE,
                style = TextStyle(fontSize = 12.sp, color = YawmiColors.GoldLight.copy(alpha = 0.85f)),
            )
        }
    }
}

private fun DrawScope.drawFrame() {
    val inset = 6.dp.toPx()
    drawRoundRect(
        color = YawmiColors.GoldLight.copy(alpha = 0.75f),
        topLeft = Offset(inset, inset),
        size = Size(size.width - 2 * inset, size.height - 2 * inset),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(width = 1.dp.toPx()),
    )
    val edge = inset + 20.dp.toPx()
    drawStar(Offset(edge, size.height / 2), 9.dp.toPx())
    drawStar(Offset(size.width - edge, size.height / 2), 9.dp.toPx())
}

/** نجمة ثمانية: مربعان متراكبان بزاوية 45°. */
private fun DrawScope.drawStar(center: Offset, radius: Float) {
    val side = radius * 1.5f
    for (angle in listOf(0f, 45f)) {
        rotate(degrees = angle, pivot = center) {
            drawRect(
                color = YawmiColors.GoldLight,
                topLeft = Offset(center.x - side / 2, center.y - side / 2),
                size = Size(side, side),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}
