package com.yawmi.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yawmi.app.ui.theme.YawmiColors

/** شعار YAWMI: حرف Y مدمج مع ساعة (ذراعا الحرف تلتقيان عند قمة الساعة)، بالأخضر الداكن والذهبي. */
@Composable
fun YawmiLogo(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.085f
        drawRoundRect(
            color = YawmiColors.GreenDark,
            cornerRadius = CornerRadius(s * 0.22f, s * 0.22f),
        )
        // ذراعا Y
        drawLine(YawmiColors.GoldLight, Offset(s * 0.30f, s * 0.20f), Offset(s * 0.50f, s * 0.46f), stroke, StrokeCap.Round)
        drawLine(YawmiColors.GoldLight, Offset(s * 0.70f, s * 0.20f), Offset(s * 0.50f, s * 0.46f), stroke, StrokeCap.Round)
        // الساعة = ساق Y
        drawCircle(
            color = YawmiColors.GoldLight,
            radius = s * 0.19f,
            center = Offset(s * 0.50f, s * 0.66f),
            style = Stroke(width = stroke * 0.8f),
        )
        drawLine(YawmiColors.GoldLight, Offset(s * 0.50f, s * 0.66f), Offset(s * 0.50f, s * 0.56f), stroke * 0.6f, StrokeCap.Round)
        drawLine(YawmiColors.GoldLight, Offset(s * 0.50f, s * 0.66f), Offset(s * 0.58f, s * 0.70f), stroke * 0.6f, StrokeCap.Round)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
fun EmptyCard(title: String, message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * شاشة قسم لم يُنفَّذ بعد. تقول ذلك بوضوح ولا تتظاهر بأنها ميزة مكتملة.
 */
@Composable
fun PhasePlaceholderScreen(title: String, phase: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        YawmiLogo(size = 64.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "هذا القسم قيد البناء — $phase",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
