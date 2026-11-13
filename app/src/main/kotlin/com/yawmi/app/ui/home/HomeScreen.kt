package com.yawmi.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmi.app.ui.common.EmptyCard
import com.yawmi.app.ui.common.SectionTitle
import com.yawmi.app.ui.common.YawmiLogo
import com.yawmi.app.ui.theme.YawmiColors
import com.yawmi.app.ui.theme.containerColor
import com.yawmi.domain.AgendaCalculator
import com.yawmi.domain.AgendaItem
import com.yawmi.domain.ArabicDates
import com.yawmi.domain.DayType
import com.yawmi.domain.TaskItem
import com.yawmi.domain.TimeFormat
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val ltrNumbers = TextStyle(textDirection = TextDirection.Ltr)

/** ساعة تتحدث كل ثانية (متزامنة مع حدود الثانية) دون إعادة فتح الشاشة. */
@Composable
private fun rememberNow(): State<LocalDateTime> = produceState(initialValue = LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(1000L - (System.currentTimeMillis() % 1000L))
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = rememberNow()

    // عند مرور منتصف الليل يتغير التاريخ فتُعاد الأجندة تلقائياً.
    LaunchedEffect(Unit) {
        snapshotFlow { now.value.toLocalDate() }.collect { viewModel.onDateChanged(it) }
    }

    var showAddTask by remember { mutableStateOf(false) }
    var showAddAppointment by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
    QuranHeader()
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AppTitleRow() }
        item { HeaderCard(state, onDayTypeChange = viewModel::setDayTypeOverride) }
        item { ClockCard(now) }
        item { NextActivityCard(state.items, now) }
        item { StatsRow(state.items, now) }
        item { WeatherPlaceholderCard() }
        item {
            QuickActions(
                onAddAppointment = { showAddAppointment = true },
                onAddTask = { showAddTask = true },
            )
        }
        item { SectionTitle("أنشطة اليوم") }
        if (state.items.isEmpty()) {
            item {
                EmptyCard(
                    title = "لا توجد أنشطة لهذا اليوم",
                    message = "أضف موعداً من الأعلى. الجدول الأسبوعي يُدخل في المرحلة 3.",
                )
            }
        } else {
            items(state.items, key = { it.id }) { AgendaRow(it) }
        }
        item {
            TasksCard(
                tasks = state.openTasks,
                totalOpen = state.openTaskCount,
                onComplete = viewModel::toggleTask,
            )
        }
    }
    }

    if (showAddTask) {
        AddTaskDialog(
            onDismiss = { showAddTask = false },
            onConfirm = { title, description ->
                viewModel.addTask(title, description)
                showAddTask = false
            },
        )
    }
    if (showAddAppointment) {
        AddAppointmentDialog(
            onDismiss = { showAddAppointment = false },
            onConfirm = { title, type, start, end ->
                viewModel.addAppointmentToday(title, type, start, end)
                showAddAppointment = false
            },
        )
    }
}

@Composable
private fun AppTitleRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        YawmiLogo(size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("يومي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "المساعد اليومي للأستاذ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeaderCard(state: HomeUiState, onDayTypeChange: (DayType?) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = state.dayType.containerColor(),
            contentColor = YawmiColors.GreenDark,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    ArabicDates.dayName(state.date),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Box {
                    AssistChip(
                        onClick = { menuOpen = true },
                        label = { Text(state.dayType.arabicLabel + if (state.isDayTypeManual) " (يدوي)" else "") },
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("تلقائي") },
                            onClick = { onDayTypeChange(null); menuOpen = false },
                        )
                        DayType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.arabicLabel) },
                                onClick = { onDayTypeChange(type); menuOpen = false },
                            )
                        }
                    }
                }
            }
            Text(ArabicDates.gregorian(state.date), style = MaterialTheme.typography.titleMedium)
            if (state.hijriEnabled) {
                ArabicDates.hijri(state.date)?.let {
                    Text("$it (حسابي)", style = MaterialTheme.typography.bodyMedium)
                }
            }
            state.holidayName?.let { Text("عطلة: $it", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ClockCard(now: State<LocalDateTime>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = YawmiColors.GreenDark, contentColor = YawmiColors.GoldLight),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
            Text(
                text = now.value.format(clockFormatter),
                style = TextStyle(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textDirection = TextDirection.Ltr,
                ),
            )
        }
    }
}

@Composable
private fun NextActivityCard(items: List<AgendaItem>, now: State<LocalDateTime>) {
    val nowSec = now.value.toLocalTime().toSecondOfDay()
    val current = AgendaCalculator.currentActivity(items, nowSec)
    val next = AgendaCalculator.nextActivity(items, nowSec)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (current != null) {
                CountdownRow(
                    label = "جارٍ الآن: ${current.title}",
                    suffix = "ينتهي بعد",
                    seconds = AgendaCalculator.secondsUntilEnd(current, nowSec),
                )
            }
            if (next != null) {
                CountdownRow(
                    label = "النشاط القادم: ${next.title}",
                    suffix = "يبدأ بعد",
                    seconds = AgendaCalculator.secondsUntilStart(next, nowSec),
                )
            }
            if (current == null && next == null) {
                Text("لا يوجد نشاط قادم اليوم", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun CountdownRow(label: String, suffix: String, seconds: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(suffix, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            Text(
                TimeFormat.countdown(seconds),
                style = ltrNumbers.merge(MaterialTheme.typography.titleLarge),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StatsRow(items: List<AgendaItem>, now: State<LocalDateTime>) {
    val nowSec = now.value.toLocalTime().toSecondOfDay()
    val workLabel = TimeFormat.durationLabel(AgendaCalculator.scheduledWorkMinutes(items))
    val remaining = AgendaCalculator.remainingLessons(items, nowSec)
    val total = AgendaCalculator.totalLessons(items)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("ساعات العمل المجدولة", workLabel, Modifier.weight(1f))
        StatCard("الحصص المتبقية", "$remaining من $total", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** بطاقة صادقة: الطقس غير منفَّذ بعد (المرحلة 5)، ولا نعرض أرقاماً وهمية. */
@Composable
private fun WeatherPlaceholderCard() {
    EmptyCard(
        title = "الطقس",
        message = "غير متاح بعد — يُضاف في المرحلة 5 عبر مزوّد قابل للاستبدال.",
    )
}

@Composable
private fun QuickActions(onAddAppointment: () -> Unit, onAddTask: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onAddAppointment, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("إضافة موعد")
        }
        Button(onClick = onAddTask, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("إضافة مهمة")
        }
    }
}

@Composable
private fun AgendaRow(item: AgendaItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val time = if (item.isTimed) {
                "${TimeFormat.minutes(item.startMinutes!!)} – ${TimeFormat.minutes(item.endMinutes!!)}"
            } else {
                "بلا وقت"
            }
            Text(
                time,
                style = ltrNumbers.merge(MaterialTheme.typography.bodyMedium),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                item.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TasksCard(tasks: List<TaskItem>, totalOpen: Int, onComplete: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionTitle("المهام غير المكتملة ($totalOpen)")
            if (tasks.isEmpty()) {
                Text("لا توجد مهام معلّقة", style = MaterialTheme.typography.bodyMedium)
            } else {
                tasks.forEach { task ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = false, onCheckedChange = { onComplete(task.id) })
                        Text(task.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (totalOpen > tasks.size) {
                    Text("و${totalOpen - tasks.size} مهام أخرى", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
