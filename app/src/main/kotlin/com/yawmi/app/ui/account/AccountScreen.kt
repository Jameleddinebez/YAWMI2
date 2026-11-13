package com.yawmi.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yawmi.app.YawmiApplication
import com.yawmi.app.data.cloud.CloudStatus
import com.yawmi.app.ui.common.SectionTitle
import com.yawmi.app.ui.common.YawmiLogo

private const val ABOUT_MESSAGE =
    "زملائي وزميلاتي،\n\n" +
        "لكل أستاذ يحمل رسالة العلم، ولكل من يبذل جهده في سبيل بناء جيل واعٍ ومتعلم، " +
        "أقدم لكم هذا العمل المتواضع، راجياً أن يكون عوناً لكم في تنظيم أوقاتكم وتيسير مهامكم اليومية.\n\n" +
        "وفقكم الله في رسالتكم التربوية، وبارك في جهودكم، وجعل النجاح والتوفيق حليفكم دائماً.\n\n" +
        "من زميلكم: بزوقي جمال الدين\n\n" +
        "هذا التطبيق من أستاذ إلى أستاذ."

/** «حسابي»: إعدادات العرض + حالة السحابة + «عن التطبيق». الملف الشخصي الكامل في المرحلة 2. */
@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as YawmiApplication).container
    val hijriEnabled by container.userPrefs.hijriEnabled.collectAsStateWithLifecycle()
    val cloud by container.authRepository.status.collectAsStateWithLifecycle(initialValue = CloudStatus.NotConfigured)
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("حسابي", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                ListItem(
                    headlineContent = { Text("عرض التاريخ الهجري") },
                    supportingContent = {
                        Text("حساب تقويمي تقريبي (أم القرى). المناسبات الدينية الرسمية تُعتمد بالإعلان الرسمي.")
                    },
                    trailingContent = {
                        Switch(
                            checked = hijriEnabled,
                            onCheckedChange = { container.userPrefs.setHijriEnabled(it) },
                        )
                    },
                )
                ListItem(
                    headlineContent = { Text("الحساب والمزامنة السحابية") },
                    supportingContent = {
                        Text(
                            when (cloud) {
                                is CloudStatus.NotConfigured ->
                                    "غير مُعدّة — تُفعَّل في المرحلة 6 بعد ربط Firebase. بياناتك الآن محفوظة على هذا الجهاز فقط."
                                is CloudStatus.SignedOut -> "غير مسجّل الدخول"
                                is CloudStatus.SignedIn -> "مسجّل الدخول"
                            },
                        )
                    },
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YawmiLogo(size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        SectionTitle("عن التطبيق")
                        Text("يومي | YAWMI — المساعد اليومي للأستاذ", style = MaterialTheme.typography.bodyMedium)
                        if (versionName.isNotBlank()) {
                            Text("الإصدار $versionName", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(ABOUT_MESSAGE, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
