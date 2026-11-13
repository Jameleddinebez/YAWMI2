package com.yawmi.app.ui.home

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yawmi.domain.AppointmentType
import com.yawmi.domain.TimeFormat

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (title: String, description: String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مهمة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المهمة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onConfirm(title.trim(), description.trim()) }) {
                Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/** إضافة سريعة لموعد اليوم. التقويم الكامل (تواريخ أخرى، مرفقات، تذكيرات) في المرحلة 3. */
@Composable
fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, type: AppointmentType, startMinutes: Int?, endMinutes: Int?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AppointmentType.MEETING) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }

    val startMin = TimeFormat.parseMinutes(start)
    val endMin = TimeFormat.parseMinutes(end)
    val startInvalid = start.isNotBlank() && startMin == null
    val endInvalid = end.isNotBlank() && endMin == null
    val endWithoutStart = endMin != null && startMin == null
    val endBeforeStart = startMin != null && endMin != null && endMin <= startMin
    val valid = title.isNotBlank() && !startInvalid && !endInvalid && !endWithoutStart && !endBeforeStart

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة موعد لليوم") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الموعد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppointmentType.entries.toList()) { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.arabicLabel) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("البداية 08:00") },
                        isError = startInvalid || endWithoutStart,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("النهاية 09:00") },
                        isError = endInvalid || endBeforeStart,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    when {
                        startInvalid || endInvalid -> "صيغة الوقت: ساعة:دقيقة، مثل 08:30"
                        endWithoutStart -> "أدخل وقت البداية أولاً"
                        endBeforeStart -> "وقت النهاية يجب أن يكون بعد البداية"
                        else -> "الوقت اختياري؛ الموعد بلا وقت لا يدخل في العد التنازلي"
                    },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(title.trim(), type, startMin, endMin) }) {
                Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
